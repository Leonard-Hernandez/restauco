import { inject, Injectable, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs';
import { Ticket } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { EventStream } from '../../core/realtime/event-stream';

/**
 * The staff live feed (/api/panel/stream), one connection shared by every panel screen.
 * opened$ fires on each (re)connect: screens refetch, since events may have been missed.
 */
@Injectable()
export class PanelStream implements OnDestroy {

  private auth = inject(AuthService);

  readonly opened$ = new Subject<void>();
  readonly ticket$ = new Subject<Ticket>();
  /** A bill opened or closed on this table (null: a delivery) */
  readonly floor$ = new Subject<number | null>();
  /** Something ran out or came back */
  readonly menu$ = new Subject<void>();

  private stream = new EventStream({
    path: '/api/panel/stream',
    token: () => this.auth.token(),
    onOpen: () => this.opened$.next(),
    onUnauthorized: () => this.auth.restartLogin(),
    onEvent: (name, data) => {
      if (name === 'ticket') this.ticket$.next(data as Ticket);
      else if (name === 'floor') this.floor$.next((data as { tableId: number | null }).tableId);
      else if (name === 'menu') this.menu$.next();
    }
  });

  readonly status = this.stream.status;

  start() {
    this.stream.start();
  }

  ngOnDestroy() {
    this.stream.stop();
  }
}
