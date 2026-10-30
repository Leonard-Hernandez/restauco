import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, tap } from 'rxjs';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Delivery, Menu, TableSummary, Ticket, VoiceOrder } from '../../../core/api/models';
import { addToCart, CartLine, cartCount, newLine, toOrderLine } from '../../../shared/cart';
import { chime } from '../../../shared/chime';
import { PanelStream } from '../panel-stream.service';

export interface FloorAlert {
  id: number;
  kind: 'ready' | 'qr' | 'sent' | 'delivery';
  text: string;
  /** Where tapping it goes, relative to the panel */
  link: (string | number)[];
}

const ACTIVE = new Set(['PENDING_APPROVAL', 'SENT', 'PREPARING', 'READY']);
const ALERT_MS = 8000;
const DRAFTS_KEY = 'restauco.floor.drafts';

/** A table's order in progress, as stored between reloads (dishes by id). */
type StoredLine = { dishId: number; qty: number; note: string; removed: number[]; selections: Record<string, string[]> };

/**
 * The waiter's live view of the floor: tables, open tickets, and alerts when an order is
 * ready for pickup or a table orders by QR. Lives with the panel, so alerts follow the
 * waiter between the floor map and a table.
 *
 * It also keeps one draft order per table, so a waiter can start table 3, jump to table 5,
 * and come back to 3 without losing anything. Drafts survive a reload on this device.
 */
@Injectable()
export class FloorStore {

  private api = inject(ApiService);
  private stream = inject(PanelStream);
  private destroyRef = inject(DestroyRef);

  readonly tables = signal<TableSummary[] | null>(null);
  readonly tickets = signal<Ticket[]>([]);
  /** Deliveries not charged yet, oldest first */
  readonly deliveries = signal<Delivery[]>([]);
  readonly pendingDeliveries = computed(() => this.deliveries().filter((d) => d.status === 'PENDING').length);
  readonly error = signal<string | null>(null);
  readonly alerts = signal<FloorAlert[]>([]);
  readonly menu = signal<Menu | null>(null);
  /** tableId -> lines not sent yet, in the order the waiter started them */
  readonly drafts = signal<Record<number, CartLine[]>>({});
  /** The table whose order is open in the composer, or null when it is closed. */
  readonly composing = signal<number | null>(null);
  /** Rounds this waiter sent, for screens that show a table. */
  readonly sent$ = new Subject<Ticket>();

  readonly draftTables = computed(() => Object.entries(this.drafts())
    .filter(([, lines]) => lines.length)
    .map(([id, lines]) => ({ tableId: Number(id), count: cartCount(lines) })));

  private started = false;
  private tablesTimer: ReturnType<typeof setTimeout> | null = null;
  private deliveriesTimer: ReturnType<typeof setTimeout> | null = null;
  private alertSeq = 0;
  /** tableId -> dictated orders that went into its draft, linked to the round once it is sent */
  private voiceSamples = new Map<number, number[]>();

  start() {
    if (this.started) return;
    this.started = true;
    this.refresh();
    this.api.menu().subscribe({ next: (menu) => this.onMenu(menu) });
    this.stream.opened$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.refresh());
    this.stream.ticket$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((t) => this.onTicket(t, true));
    this.stream.floor$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((tableId) => {
      if (tableId === null) this.refreshDeliveriesSoon();
      else this.refreshTablesSoon();
    });
    // Something ran out: the composer must not offer it
    this.stream.menu$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() =>
      this.api.menu().subscribe({ next: (menu) => this.menu.set(menu) }));
  }

  refresh() {
    this.api.floor().subscribe({
      next: (tables) => {
        this.tables.set(tables);
        this.error.set(null);
      },
      error: (e) => this.error.set(errorMessage(e))
    });
    this.api.floorTickets().subscribe({ next: (tickets) => this.tickets.set(tickets) });
    this.loadDeliveries();
  }

  loadDeliveries() {
    this.api.deliveries().subscribe({ next: (list) => this.deliveries.set(list) });
  }

  tableNumber(tableId: number): number | null {
    return this.tables()?.find((t) => t.id === tableId)?.number ?? null;
  }

  draft(tableId: number): CartLine[] {
    return this.drafts()[tableId] ?? [];
  }

  setDraft(tableId: number, lines: CartLine[]) {
    this.drafts.update((all) => {
      const next = { ...all };
      if (lines.length) next[tableId] = lines;
      else {
        delete next[tableId];
        this.voiceSamples.delete(tableId);
      }
      return next;
    });
    this.saveDrafts();
  }

  /**
   * Puts a dictated order into a draft: the table it named ("mesa 3"), or the open one. Returns
   * the table it went to, and whether a table was named that does not exist.
   */
  applyVoice(order: VoiceOrder, currentTableId: number): { tableId: number; unknownTable: boolean; added: number } {
    const named = order.tableNumber !== null ? this.tables()?.find((t) => t.number === order.tableNumber) : undefined;
    const tableId = named?.id ?? currentTableId;
    const dishes = new Map((this.menu()?.dishes ?? []).map((d) => [d.id, d]));
    let lines = this.draft(tableId);
    let added = 0;
    for (const l of order.lines) {
      const dish = dishes.get(l.dishId);
      if (!dish) continue;
      lines = addToCart(lines, newLine(dish, { qty: l.qty, note: l.note ?? '', removed: l.removedIngredientIds, selections: l.selections }));
      added += l.qty;
    }
    this.setDraft(tableId, lines);
    if (added) this.voiceSamples.set(tableId, [...(this.voiceSamples.get(tableId) ?? []), order.sampleId]);
    return { tableId, unknownTable: order.tableNumber !== null && !named, added };
  }

  compose(tableId: number) {
    this.composing.set(tableId);
  }

  /** Sends a table's draft to the kitchen and forgets it. */
  sendDraft(tableId: number): Observable<Ticket> {
    return this.api.waiterOrder(tableId, this.draft(tableId).map(toOrderLine)).pipe(tap((ticket) => {
      // Examples for later training: best effort, the order is already in the kitchen
      for (const sampleId of this.voiceSamples.get(tableId) ?? []) {
        this.api.voiceSent(sampleId, ticket.id).subscribe({ error: () => undefined });
      }
      this.setDraft(tableId, []);
      this.onTicket(ticket, false);
      this.sent$.next(ticket);
      this.alert('sent', `Mesa ${ticket.tableNumber}: enviado a cocina`, ['mesas', tableId]);
    }));
  }

  ticketsOf(tableId: number): Ticket[] {
    return this.tickets().filter((t) => t.tableId === tableId);
  }

  /** Charges a delivery once the courier is back with the money. */
  chargeDelivery(d: Delivery, paymentMethodId: number): Observable<unknown> {
    return this.api.closeSession(d.sessionId, [{ paymentMethodId, amount: d.total }])
      .pipe(tap(() => this.deliveries.update((list) => list.filter((x) => x.id !== d.id))));
  }

  approve(id: number): Observable<Ticket> {
    return this.api.approve(id).pipe(tap((t) => this.onTicket(t, false)));
  }

  deliver(id: number): Observable<Ticket> {
    return this.api.deliver(id).pipe(tap((t) => this.onTicket(t, false)));
  }

  cancel(id: number, reason: string | null): Observable<Ticket> {
    return this.api.cancel(id, reason).pipe(tap((t) => this.onTicket(t, false)));
  }

  dismiss(id: number) {
    this.alerts.update((list) => list.filter((a) => a.id !== id));
  }

  /** Applies a ticket change (from the stream or our own action). */
  onTicket(ticket: Ticket, remote: boolean) {
    const previous = this.tickets().find((t) => t.id === ticket.id);
    this.tickets.update((list) => {
      const rest = list.filter((t) => t.id !== ticket.id);
      return ACTIVE.has(ticket.status)
        ? [...rest, ticket].sort((a, b) => a.createdAt.localeCompare(b.createdAt))
        : rest;
    });
    if (ticket.deliveryId !== null) {
      if (remote) {
        if (ticket.status === 'READY' && previous?.status !== 'READY') {
          this.alert('ready', `Domicilio ${ticket.deliveryId}: listo para despachar`, ['domicilios']);
        } else if (ticket.status === 'PENDING_APPROVAL' && !previous) {
          this.alert('delivery', `Nuevo domicilio: revisa la dirección y confírmalo`, ['domicilios']);
        }
      }
      this.refreshDeliveriesSoon();
      return;
    }
    if (remote) {
      if (ticket.status === 'READY' && previous?.status !== 'READY') {
        this.alert('ready', `Mesa ${ticket.tableNumber}: pedido listo para recoger`, ['mesas', ticket.tableId!]);
      } else if (ticket.status === 'PENDING_APPROVAL' && !previous) {
        this.alert('qr', `Mesa ${ticket.tableNumber} pidió por QR`, ['mesas', ticket.tableId!]);
      }
    }
    this.refreshTablesSoon();
  }

  private onMenu(menu: Menu) {
    this.menu.set(menu);
    if (Object.keys(this.drafts()).length) return;
    // Restore drafts against today's menu; dishes that are gone are dropped
    const dishes = new Map(menu.dishes.map((d) => [d.id, d]));
    const restored: Record<number, CartLine[]> = {};
    for (const [tableId, lines] of Object.entries(readStored())) {
      const kept = lines.flatMap((l) => {
        const dish = dishes.get(l.dishId);
        return dish ? [newLine(dish, { qty: l.qty, note: l.note, removed: l.removed, selections: l.selections })] : [];
      });
      if (kept.length) restored[Number(tableId)] = kept;
    }
    this.drafts.set(restored);
  }

  private saveDrafts() {
    const stored: Record<number, StoredLine[]> = {};
    for (const [tableId, lines] of Object.entries(this.drafts())) {
      stored[Number(tableId)] = lines.map((l) => ({ dishId: l.dish.id, qty: l.qty, note: l.note, removed: l.removed, selections: l.selections }));
    }
    try {
      localStorage.setItem(DRAFTS_KEY, JSON.stringify(stored));
    } catch {
      // private mode or storage full: drafts still live in memory
    }
  }

  private alert(kind: FloorAlert['kind'], text: string, link: FloorAlert['link']) {
    if (kind !== 'sent') chime(kind === 'ready' ? 'ready' : 'new');
    const id = ++this.alertSeq;
    this.alerts.update((list) => [...list.slice(-2), { id, kind, text, link }]);
    setTimeout(() => this.dismiss(id), kind === 'sent' ? 2500 : ALERT_MS);
  }

  private refreshDeliveriesSoon() {
    if (this.deliveriesTimer) clearTimeout(this.deliveriesTimer);
    this.deliveriesTimer = setTimeout(() => this.loadDeliveries(), 250);
  }

  // Several events often arrive together (a round, then its bill): refetch once.
  private refreshTablesSoon() {
    if (this.tablesTimer) clearTimeout(this.tablesTimer);
    this.tablesTimer = setTimeout(() => {
      this.api.floor().subscribe({ next: (tables) => this.tables.set(tables) });
    }, 250);
  }
}

function readStored(): Record<string, StoredLine[]> {
  try {
    return JSON.parse(localStorage.getItem(DRAFTS_KEY) ?? '{}') ?? {};
  } catch {
    return {};
  }
}
