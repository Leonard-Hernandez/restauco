import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, QrCode, Check, X, HandPlatter, Plus, Bike } from 'lucide-angular';
import { errorMessage } from '../../../core/api/api.service';
import { Ticket } from '../../../core/api/models';
import { elapsed, minutesSince, price } from '../../../shared/format';
import { TicketItemsComponent } from '../ticket-items.component';
import { FloorStore } from './floor-store.service';

/** The waiter's home: what needs action now (QR orders, pickups), then every table. */
@Component({
  selector: 'app-floor',
  imports: [RouterLink, LucideAngularModule, TicketItemsComponent],
  template: `
    <main class="wrap">
      @if (actionError()) { <p class="error-text banner" role="alert">{{ actionError() }}</p> }

      @if (store.pendingDeliveries(); as n) {
        <a routerLink="../domicilios" class="delivery-banner">
          <lucide-icon [img]="Bike" [size]="20" /> {{ n }} {{ n === 1 ? 'domicilio' : 'domicilios' }} por confirmar
        </a>
      }

      @if (pending().length) {
        <section aria-labelledby="pending-title">
          <h2 id="pending-title" class="section-title label"><lucide-icon [img]="QrCode" [size]="18" /> Pedidos por QR · por confirmar</h2>
          <div class="action-list">
            @for (t of pending(); track t.id) {
              <article class="action-card pending">
                <header>
                  <a class="table-link display" [routerLink]="[t.tableId]">Mesa {{ t.tableNumber }}</a>
                  <span class="age">{{ age(t.createdAt) }}</span>
                </header>
                <app-ticket-items [items]="t.items" />
                <footer>
                  <span class="tabular total">{{ price(t.total) }}</span>
                  <button type="button" class="btn btn-sm btn-danger" (click)="cancel(t)" [disabled]="busy() === t.id">
                    <lucide-icon [img]="X" [size]="16" /> Rechazar
                  </button>
                  <button type="button" class="btn btn-sm btn-brand" (click)="approve(t)" [disabled]="busy() === t.id">
                    <lucide-icon [img]="Check" [size]="16" /> A cocina
                  </button>
                </footer>
              </article>
            }
          </div>
        </section>
      }

      @if (ready().length) {
        <section aria-labelledby="ready-title">
          <h2 id="ready-title" class="section-title label ready-title"><lucide-icon [img]="HandPlatter" [size]="18" /> Listos para recoger</h2>
          <div class="action-list">
            @for (t of ready(); track t.id) {
              <article class="action-card ready">
                <header>
                  <a class="table-link display" [routerLink]="[t.tableId]">Mesa {{ t.tableNumber }}</a>
                  <span class="age">listo hace {{ age(t.readyAt) }}</span>
                </header>
                <app-ticket-items [items]="t.items" />
                <footer>
                  <button type="button" class="btn btn-ok grow" (click)="deliver(t)" [disabled]="busy() === t.id">
                    <lucide-icon [img]="Check" [size]="18" /> Entregado
                  </button>
                </footer>
              </article>
            }
          </div>
        </section>
      }

      <section aria-labelledby="tables-title">
        <h2 id="tables-title" class="section-title label">Mesas <span class="tip">· toca + para pedir sin entrar</span></h2>
        @if (store.tables(); as tables) {
          <ul class="tables">
            @for (t of tables; track t.id) {
              <li class="table-wrap">
                <a class="table" [routerLink]="[t.id]" [class.busy]="t.sessionId" [class.has-ready]="t.ready" [class.has-qr]="t.pendingApproval" [class.has-draft]="draftCount(t.id)">
                  <span class="num display">{{ t.number }}</span>
                  @if (t.name !== 'Mesa ' + t.number) { <span class="tname">{{ t.name }}</span> }
                  @if (t.sessionId) {
                    <span class="state tabular">{{ price(t.total) }} · {{ age(t.openedAt) }}</span>
                    <span class="badges">
                      @if (t.pendingApproval) { <span class="b qr">{{ t.pendingApproval }} QR</span> }
                      @if (t.inKitchen) { <span class="b kitchen">{{ t.inKitchen }} en cocina</span> }
                      @if (t.ready) { <span class="b ready">{{ t.ready }} listo</span> }
                    </span>
                  } @else {
                    <span class="state free">Libre</span>
                  }
                  @if (draftCount(t.id); as n) { <span class="b draft">{{ n }} sin enviar</span> }
                </a>
                <button type="button" class="quick" (click)="store.compose(t.id)" [disabled]="!store.menu()" [attr.aria-label]="'Tomar pedido para la mesa ' + t.number">
                  <lucide-icon [img]="Plus" [size]="22" />
                </button>
              </li>
            }
          </ul>
        } @else if (store.error()) {
          <div class="empty"><p>{{ store.error() }}</p><button type="button" class="btn btn-ghost btn-sm" (click)="store.refresh()">Reintentar</button></div>
        } @else {
          <p class="empty">Cargando mesas…</p>
        }
      </section>
    </main>
  `,
  styles: `
    .wrap { max-width: 76rem; margin: 0 auto; padding: 1.1rem 0.9rem 4rem; }
    .banner { margin: 0 0 1rem; }
    .delivery-banner { display: flex; align-items: center; gap: 0.6rem; margin: 0 0 1rem; padding: 0.9rem 1.1rem; border-radius: 16px; background: var(--warn); color: #1d1002; font-weight: 800; text-decoration: none; }
    .section-title { display: flex; align-items: center; gap: 0.5rem; margin: 0.6rem 0 0.75rem; font-size: 1.05rem; color: var(--ink-muted); }
    .ready-title { color: var(--ok); }
    section + section { margin-top: 1.6rem; }
    .action-list { display: grid; gap: 0.75rem; grid-template-columns: repeat(auto-fill, minmax(17rem, 1fr)); }
    .action-card { border-radius: 18px; padding: 0.9rem 1rem; background: var(--ground-raised); border: 2px solid var(--line); display: flex; flex-direction: column; gap: 0.6rem; }
    .action-card.pending { border-color: color-mix(in oklab, var(--warn) 70%, transparent); }
    .action-card.ready { border-color: var(--ok); background: color-mix(in oklab, var(--ok) 9%, var(--ground-raised)); }
    .action-card header { display: flex; justify-content: space-between; align-items: baseline; }
    .table-link { font-size: 1.6rem; color: var(--ink); text-decoration: none; }
    .age { color: var(--ink-faint); font-size: 0.88rem; }
    .action-card footer { display: flex; align-items: center; gap: 0.5rem; margin-top: auto; }
    .total { margin-right: auto; font-family: var(--font-label); font-weight: 600; color: var(--brand); }
    .grow { flex: 1; }

    .tip { font-family: var(--font-sans); text-transform: none; letter-spacing: 0; font-size: 0.85rem; color: var(--ink-faint); font-weight: 400; }
    .table-wrap { position: relative; }
    .quick { position: absolute; top: 0.5rem; right: 0.5rem; width: 48px; height: 48px; border-radius: 14px; border: 0; display: grid; place-items: center; background: var(--brand); color: var(--brand-ink); box-shadow: 0 6px 16px rgb(0 0 0 / 0.35); touch-action: manipulation; }
    .quick:active { transform: scale(0.94); }
    .quick:disabled { opacity: 0.4; }
    .table.has-draft { border-style: dashed; border-color: var(--brand); }
    .b.draft { align-self: flex-start; background: var(--brand); color: var(--brand-ink); }
    .tables { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.7rem; grid-template-columns: repeat(auto-fill, minmax(9.5rem, 1fr)); }
    .table { position: relative; display: flex; flex-direction: column; gap: 0.25rem; min-height: 8.5rem; padding: 0.85rem 0.95rem; border-radius: 20px; background: var(--ground-raised); border: 2px solid var(--line); color: var(--ink); text-decoration: none; transition: transform 120ms, border-color 160ms; }
    .table:active { transform: scale(0.98); }
    .table.busy { border-color: color-mix(in oklab, var(--brand) 60%, transparent); }
    .table.has-qr { border-color: var(--warn); }
    .table.has-ready { border-color: var(--ok); box-shadow: 0 0 0 3px color-mix(in oklab, var(--ok) 25%, transparent); }
    .num { font-size: 2.6rem; line-height: 1; }
    .busy .num { color: var(--brand); }
    .tname { font-size: 0.85rem; color: var(--ink-muted); }
    .state { margin-top: auto; font-size: 0.9rem; color: var(--ink-muted); }
    .state.free { color: var(--ink-faint); }
    .badges { display: flex; flex-wrap: wrap; gap: 0.3rem; }
    .b { font-size: 0.74rem; font-weight: 800; padding: 0.12rem 0.5rem; border-radius: 999px; }
    .b.qr { background: var(--warn); color: #1d1002; }
    .b.kitchen { background: rgb(255 255 255 / 0.1); color: var(--ink); }
    .b.ready { background: var(--ok); color: #04150c; }
  `
})
export default class FloorComponent implements OnInit, OnDestroy {
  readonly store = inject(FloorStore);

  readonly busy = signal<number | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly now = signal(Date.now());
  readonly price = price;
  readonly QrCode = QrCode;
  readonly Check = Check;
  readonly X = X;
  readonly HandPlatter = HandPlatter;
  readonly Plus = Plus;
  readonly Bike = Bike;

  // Deliveries have their own page
  readonly pending = computed(() => this.store.tickets().filter((t) => t.status === 'PENDING_APPROVAL' && t.deliveryId === null));
  readonly ready = computed(() => this.store.tickets().filter((t) => t.status === 'READY' && t.deliveryId === null));

  private clock = setInterval(() => this.now.set(Date.now()), 30_000);

  ngOnInit() {
    this.store.start();
  }

  draftCount(tableId: number): number {
    return this.store.draft(tableId).reduce((n, l) => n + l.qty, 0);
  }

  ngOnDestroy() {
    clearInterval(this.clock);
  }

  age(instant: string | null): string {
    return elapsed(minutesSince(instant, this.now()));
  }

  approve(t: Ticket) {
    this.run(t, this.store.approve(t.id));
  }

  deliver(t: Ticket) {
    this.run(t, this.store.deliver(t.id));
  }

  cancel(t: Ticket) {
    this.run(t, this.store.cancel(t.id, 'Rechazado por el mesero'));
  }

  private run(t: Ticket, action: ReturnType<FloorStore['approve']>) {
    this.busy.set(t.id);
    this.actionError.set(null);
    action.subscribe({
      next: () => this.busy.set(null),
      error: (e) => {
        this.busy.set(null);
        this.actionError.set(errorMessage(e));
        this.store.refresh();
      }
    });
  }
}
