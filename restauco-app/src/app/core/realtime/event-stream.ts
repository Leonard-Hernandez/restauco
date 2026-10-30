import { signal } from '@angular/core';
import { appConfig } from '../config/app-config';

export type StreamStatus = 'connecting' | 'live' | 'offline';

export interface StreamOptions {
  /** Path under the API, e.g. /api/panel/stream */
  path: string;
  /** Bearer token for staff streams; null for public ones. */
  token?: () => string | null;
  /** Called on every (re)connect: refetch the screen, events may have been missed. */
  onOpen: () => void;
  onEvent: (name: string, data: unknown) => void;
  /** The API rejected the token. */
  onUnauthorized?: () => void;
}

const MAX_BACKOFF_MS = 15_000;
/** The server sends "ready" right away; a proxy that buffers the stream never lets it through. */
const READY_TIMEOUT_MS = 5_000;
const POLL_MS = 2_500;

interface EventBatch {
  cursor: number;
  reset: boolean;
  events: { name: string; data: unknown }[];
}

/**
 * Server-Sent Events over fetch. EventSource cannot send the Authorization and X-Tenant
 * headers, so this reads and parses the text/event-stream body itself, and reconnects with
 * backoff when the connection drops (the server also closes streams every 30 minutes).
 *
 * Some proxies buffer streams (Cloudflare quick tunnels do): when "ready" does not arrive,
 * it switches for good to polling the same events from the sibling /events endpoint.
 */
export class EventStream {

  readonly status = signal<StreamStatus>('connecting');

  private controller: AbortController | null = null;
  private stopped = false;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private polling = false;
  private cursor: number | null = null;

  constructor(private readonly options: StreamOptions) {}

  start() {
    this.stopped = false;
    if (this.polling) this.poll(0);
    else this.connect(0);
  }

  stop() {
    this.stopped = true;
    this.controller?.abort();
    if (this.retryTimer !== null) clearTimeout(this.retryTimer);
  }

  private headers(accept: string): Record<string, string> {
    const headers: Record<string, string> = { Accept: accept, 'X-Tenant': appConfig().tenant };
    const token = this.options.token?.();
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return headers;
  }

  private async connect(attempt: number) {
    if (this.stopped) return;
    this.status.set('connecting');
    const controller = this.controller = new AbortController();

    let nextAttempt = attempt + 1;
    const watchdog = setTimeout(() => {
      if (this.status() === 'live') return;
      this.polling = true;
      controller.abort();
    }, READY_TIMEOUT_MS);
    try {
      const response = await fetch(`${appConfig().apiUrl}${this.options.path}`, {
        headers: this.headers('text/event-stream'), signal: controller.signal, cache: 'no-store'
      });
      if (response.status === 401) {
        this.options.onUnauthorized?.();
      }
      if (!response.ok || !response.body) throw new Error(`HTTP ${response.status}`);
      nextAttempt = 0;
      await this.read(response.body);
    } catch {
      // aborted or network error: fall through to the reconnect
    } finally {
      clearTimeout(watchdog);
    }
    if (this.stopped) return;
    if (this.polling) {
      this.poll(0);
      return;
    }
    this.status.set('offline');
    const delay = Math.min(1000 * 2 ** Math.min(nextAttempt, 4), MAX_BACKOFF_MS);
    this.retryTimer = setTimeout(() => this.connect(nextAttempt), delay);
  }

  private async poll(attempt: number) {
    if (this.stopped) return;
    this.controller = new AbortController();
    const path = this.options.path.replace(/\/stream$/, '/events');
    const query = this.cursor === null ? '' : `?after=${this.cursor}`;

    let delay = POLL_MS;
    let nextAttempt = 0;
    try {
      const response = await fetch(`${appConfig().apiUrl}${path}${query}`, {
        headers: this.headers('application/json'), signal: this.controller.signal, cache: 'no-store'
      });
      if (response.status === 401) {
        this.options.onUnauthorized?.();
      }
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const batch = (await response.json()) as EventBatch;
      const missed = this.cursor === null || batch.reset;
      this.cursor = batch.cursor;
      this.status.set('live');
      if (missed) this.options.onOpen();
      else for (const e of batch.events) this.options.onEvent(e.name, e.data);
    } catch {
      if (this.stopped) return;
      this.status.set('offline');
      nextAttempt = attempt + 1;
      delay = Math.min(1000 * 2 ** Math.min(nextAttempt, 4), MAX_BACKOFF_MS);
    }
    if (this.stopped) return;
    this.retryTimer = setTimeout(() => this.poll(nextAttempt), delay);
  }

  private async read(body: ReadableStream<Uint8Array>) {
    const reader = body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let event = 'message';
    let data: string[] = [];

    for (;;) {
      const { value, done } = await reader.read();
      if (done) return;
      buffer += decoder.decode(value, { stream: true });
      let newline: number;
      while ((newline = buffer.indexOf('\n')) >= 0) {
        const line = buffer.slice(0, newline).replace(/\r$/, '');
        buffer = buffer.slice(newline + 1);
        if (line === '') {
          if (data.length) this.dispatch(event, data.join('\n'));
          event = 'message';
          data = [];
        } else if (line.startsWith(':')) {
          // comment (heartbeat)
        } else {
          const colon = line.indexOf(':');
          const field = colon < 0 ? line : line.slice(0, colon);
          const value = colon < 0 ? '' : line.slice(colon + 1).replace(/^ /, '');
          if (field === 'event') event = value;
          else if (field === 'data') data.push(value);
        }
      }
    }
  }

  private dispatch(name: string, raw: string) {
    if (name === 'ready') {
      this.status.set('live');
      this.options.onOpen();
      return;
    }
    try {
      this.options.onEvent(name, JSON.parse(raw));
    } catch {
      // ignore malformed payloads
    }
  }
}
