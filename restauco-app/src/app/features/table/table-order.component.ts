import { Component, computed, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, ChefHat, Minus, Plus, ShoppingBag, Trash2, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Dish, GuestTable, Menu, Ticket, TicketStatus } from '../../core/api/models';
import { EventStream } from '../../core/realtime/event-stream';
import { addToCart, cartCount, cartTotal, CartLine, describe, lineTotal, toOrderLine } from '../../shared/cart';
import { price, time } from '../../shared/format';
import { WordmarkComponent } from '../../shared/wordmark.component';
import { DishSheetComponent } from '../menu/dish-sheet.component';
import { MenuBrowserComponent } from '../menu/menu-browser.component';

const GUEST_STATUS: Record<TicketStatus, string> = {
  PENDING_APPROVAL: 'Esperando al mesero',
  SENT: 'En cocina',
  PREPARING: 'Preparando',
  READY: '¡Listo! Ya te lo llevan',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado'
};

/**
 * Ordering from the table QR, without an account. Orders wait for a waiter to confirm them,
 * then the guest follows them live through the kitchen.
 */
@Component({
  selector: 'app-table-order',
  imports: [FormsModule, LucideAngularModule, WordmarkComponent, MenuBrowserComponent, DishSheetComponent],
  template: `
    <header class="top">
      <app-wordmark size="sm" />
      @if (table(); as t) {
        <span class="table label">{{ t.name }}</span>
        <button type="button" class="orders-btn" (click)="ordersOpen.set(true)" [disabled]="!t.tickets.length">
          <lucide-icon [img]="ChefHat" [size]="18" aria-hidden="true" />
          <span class="label">Mis pedidos</span>
          @if (activeCount()) { <span class="badge">{{ activeCount() }}</span> }
        </button>
      }
    </header>

    @if (fatal()) {
      <main class="gate">
        <h1 class="display">Este código no funciona</h1>
        <p>{{ fatal() }}</p>
      </main>
    } @else if (menu() && table()) {
      @if (!table()!.active) {
        <p class="notice">Esta mesa no está recibiendo pedidos por QR. Llama al mesero.</p>
      }
      @if (toast(); as msg) { <div class="toast" role="status">{{ msg }}</div> }

      <main class="wrap" [class.with-bar]="lines().length">
        <app-menu-browser [menu]="menu()!" [orderable]="table()!.active" stickyTop="60px" (open)="selected.set($event)" />
      </main>

      @if (selected(); as d) {
        <app-dish-sheet [dish]="d" [tags]="menu()!.tags" [orderable]="table()!.active"
                        actionLabel="Agregar" (close)="selected.set(null)" (add)="add($event)" />
      }

      @if (lines().length && !cartOpen()) {
        <button type="button" class="cart-bar btn btn-brand" (click)="cartOpen.set(true)">
          <lucide-icon [img]="ShoppingBag" [size]="20" aria-hidden="true" />
          <span>Ver pedido · {{ count() }}</span>
          <span class="tabular">{{ price(total()) }}</span>
        </button>
      }

      @if (cartOpen()) {
        <div class="scrim" (click)="cartOpen.set(false)">
          <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="cart-title" (click)="$event.stopPropagation()">
            <header class="sheet-head">
              <h2 id="cart-title" class="display">Tu pedido</h2>
              <button type="button" class="icon-btn" (click)="cartOpen.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
            </header>
            <ul class="lines">
              @for (l of lines(); track l.key) {
                <li>
                  <div class="line-main">
                    <span class="line-name">{{ l.dish.name }}</span>
                    @if (describe(l); as d) { <span class="line-desc">{{ d }}</span> }
                    @if (l.note) { <span class="line-note">“{{ l.note }}”</span> }
                  </div>
                  <div class="line-side">
                    <span class="tabular">{{ price(lineTotal(l)) }}</span>
                    <div class="stepper">
                      <button type="button" class="icon-btn sm" (click)="changeQty(l, -1)" [attr.aria-label]="l.qty === 1 ? 'Quitar' : 'Uno menos'">
                        <lucide-icon [img]="l.qty === 1 ? Trash2 : Minus" [size]="16" />
                      </button>
                      <span class="tabular qty">{{ l.qty }}</span>
                      <button type="button" class="icon-btn sm" (click)="changeQty(l, 1)" [disabled]="l.qty >= 20" aria-label="Uno más"><lucide-icon [img]="Plus" [size]="16" /></button>
                    </div>
                  </div>
                </li>
              }
            </ul>
            <footer class="sheet-foot">
              <p class="foot-note">El mesero confirma tu pedido antes de pasarlo a la cocina.</p>
              @if (sendError()) { <p class="error-text" role="alert">{{ sendError() }}</p> }
              <button type="button" class="btn btn-brand send" (click)="send()" [disabled]="sending()">
                {{ sending() ? 'Enviando…' : 'Enviar pedido · ' + price(total()) }}
              </button>
            </footer>
          </section>
        </div>
      }

      @if (ordersOpen()) {
        <div class="scrim" (click)="ordersOpen.set(false)">
          <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="orders-title" (click)="$event.stopPropagation()">
            <header class="sheet-head">
              <h2 id="orders-title" class="display">Pedidos de la mesa</h2>
              <button type="button" class="icon-btn" (click)="ordersOpen.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
            </header>
            <ul class="tickets">
              @for (t of tickets(); track t.id) {
                <li class="ticket" [attr.data-status]="t.status">
                  <div class="ticket-head">
                    <span class="status label">{{ statusLabel(t.status) }}</span>
                    <span class="when">{{ time(t.createdAt) }}</span>
                  </div>
                  <ul>
                    @for (i of t.items; track $index) { <li>{{ i.qty }} × {{ i.name }}</li> }
                  </ul>
                  @if (t.status === 'CANCELLED' && t.cancelReason) { <p class="reason">{{ t.cancelReason }}</p> }
                </li>
              }
            </ul>
            <footer class="sheet-foot total-row">
              <span>Total hasta ahora</span>
              <strong class="tabular">{{ price(table()!.total) }}</strong>
            </footer>
          </section>
        </div>
      }
    } @else {
      <p class="empty">Cargando…</p>
    }
  `,
  styles: `
    :host { display: block; min-height: 100dvh; }
    .top { position: sticky; top: 0; z-index: 30; height: 60px; display: flex; align-items: center; gap: 0.8rem; padding: 0 1rem; background: var(--ground); border-bottom: 3px solid var(--brand); }
    .table { font-size: 1.05rem; color: var(--ink-muted); padding-left: 0.8rem; border-left: 1px solid var(--line); }
    .orders-btn { margin-left: auto; display: inline-flex; align-items: center; gap: 0.4rem; min-height: 42px; padding: 0 0.85rem; border-radius: 999px; border: 1.5px solid var(--line); background: none; color: var(--ink); }
    .orders-btn:disabled { opacity: 0.4; }
    .wrap { max-width: 72rem; margin: 0 auto; padding-bottom: 2rem; }
    .wrap.with-bar { padding-bottom: 6.5rem; }
    .notice { margin: 0; padding: 0.8rem 1rem; background: color-mix(in oklab, var(--warn) 18%, transparent); color: var(--ink); text-align: center; }
    .gate { max-width: 30rem; margin: 0 auto; padding: 4rem 1.25rem; }
    .gate h1 { font-size: 2.2rem; margin: 0 0 0.6rem; color: var(--brand); }
    .gate p { color: var(--ink-muted); }

    .cart-bar { position: fixed; z-index: 40; left: 1rem; right: 1rem; bottom: calc(1rem + env(safe-area-inset-bottom)); max-width: 34rem; margin: 0 auto; min-height: 58px; justify-content: space-between; box-shadow: 0 10px 30px rgb(0 0 0 / 0.5); }
    .cart-bar span:first-of-type { flex: 1; text-align: left; }

    .toast { position: fixed; z-index: 60; top: 72px; left: 50%; transform: translateX(-50%); max-width: calc(100% - 2rem); padding: 0.75rem 1.1rem; border-radius: 14px; background: var(--ink); color: var(--ground); font-weight: 700; box-shadow: 0 10px 30px rgb(0 0 0 / 0.5); animation: sheet-up 240ms var(--ease-out-expo); }

    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.4rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .lines, .tickets { list-style: none; margin: 0; padding: 0 1.1rem; }
    .lines > li { display: flex; gap: 0.8rem; justify-content: space-between; padding: 0.85rem 0; border-bottom: 1px solid var(--line); }
    .line-main { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
    .line-name { font-family: var(--font-label); text-transform: uppercase; font-weight: 600; }
    .line-desc { color: var(--ink-muted); font-size: 0.88rem; }
    .line-note { color: var(--brand-bright); font-size: 0.88rem; }
    .line-side { display: flex; flex-direction: column; align-items: flex-end; gap: 0.4rem; }
    .stepper { display: flex; align-items: center; gap: 0.25rem; }
    .icon-btn.sm { width: 36px; height: 36px; }
    .qty { min-width: 1.4rem; text-align: center; font-weight: 700; }
    .sheet-foot { position: sticky; bottom: 0; padding: 0.9rem 1.1rem 1.1rem; background: var(--ground-raised); border-top: 1px solid var(--line); }
    .foot-note { margin: 0 0 0.7rem; color: var(--ink-faint); font-size: 0.88rem; }
    .send { width: 100%; min-height: 54px; }
    .total-row { display: flex; justify-content: space-between; align-items: center; font-size: 1.05rem; }
    .total-row strong { font-family: var(--font-label); font-size: 1.3rem; color: var(--brand); }

    .ticket { padding: 0.9rem 0; border-bottom: 1px solid var(--line); }
    .ticket-head { display: flex; justify-content: space-between; align-items: center; }
    .status { font-size: 1.02rem; }
    .when { color: var(--ink-faint); font-size: 0.88rem; }
    .ticket ul { margin: 0.35rem 0 0; padding-left: 1.1rem; color: var(--ink-muted); }
    .ticket[data-status="PENDING_APPROVAL"] .status { color: var(--warn); }
    .ticket[data-status="SENT"] .status, .ticket[data-status="PREPARING"] .status { color: var(--brand); }
    .ticket[data-status="READY"] .status { color: var(--ok); }
    .ticket[data-status="DELIVERED"] .status, .ticket[data-status="CANCELLED"] .status { color: var(--ink-faint); }
    .reason { margin: 0.3rem 0 0; color: var(--ink-faint); font-size: 0.88rem; }
  `
})
export default class TableOrderComponent implements OnInit, OnDestroy {
  readonly token = input.required<string>();

  private api = inject(ApiService);

  readonly menu = signal<Menu | null>(null);
  readonly table = signal<GuestTable | null>(null);
  readonly lines = signal<CartLine[]>([]);
  readonly selected = signal<Dish | null>(null);
  readonly cartOpen = signal(false);
  readonly ordersOpen = signal(false);
  readonly sending = signal(false);
  readonly sendError = signal<string | null>(null);
  readonly fatal = signal<string | null>(null);
  readonly toast = signal<string | null>(null);

  readonly price = price;
  readonly time = time;
  readonly describe = describe;
  readonly lineTotal = lineTotal;
  readonly ShoppingBag = ShoppingBag;
  readonly ChefHat = ChefHat;
  readonly X = X;
  readonly Plus = Plus;
  readonly Minus = Minus;
  readonly Trash2 = Trash2;

  readonly count = computed(() => cartCount(this.lines()));
  readonly total = computed(() => cartTotal(this.lines()));
  readonly tickets = computed(() => [...(this.table()?.tickets ?? [])].reverse());
  readonly activeCount = computed(() => (this.table()?.tickets ?? [])
    .filter((t) => t.status !== 'DELIVERED' && t.status !== 'CANCELLED').length);

  private stream: EventStream | null = null;
  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit() {
    this.api.menu().subscribe({ next: (m) => this.menu.set(m), error: (e) => this.fatal.set(errorMessage(e)) });
    this.loadTable();
    this.stream = new EventStream({
      path: `/api/public/tables/${this.token()}/stream`,
      onOpen: () => this.loadTable(),
      onEvent: (name, data) => {
        if (name === 'ticket') this.onTicket(data as Ticket);
        else if (name === 'floor') this.loadTable();
      }
    });
    this.stream.start();
  }

  ngOnDestroy() {
    this.stream?.stop();
    if (this.toastTimer) clearTimeout(this.toastTimer);
  }

  statusLabel(s: TicketStatus): string {
    return GUEST_STATUS[s];
  }

  add(line: CartLine) {
    this.lines.update((ls) => addToCart(ls, line));
    this.selected.set(null);
    this.showToast(`${line.qty} × ${line.dish.name} agregado`);
  }

  changeQty(line: CartLine, delta: number) {
    this.lines.update((ls) => ls
      .map((l) => (l.key === line.key ? { ...l, qty: l.qty + delta } : l))
      .filter((l) => l.qty > 0));
    if (!this.lines().length) this.cartOpen.set(false);
  }

  send() {
    this.sending.set(true);
    this.sendError.set(null);
    this.api.guestOrder(this.token(), this.lines().map(toOrderLine)).subscribe({
      next: (ticket) => {
        this.sending.set(false);
        this.lines.set([]);
        this.cartOpen.set(false);
        this.onTicket(ticket);
        this.ordersOpen.set(true);
      },
      error: (e) => {
        this.sending.set(false);
        this.sendError.set(errorMessage(e));
      }
    });
  }

  private loadTable() {
    this.api.guestTable(this.token()).subscribe({
      next: (t) => this.table.set(t),
      error: (e) => this.fatal.set(errorMessage(e))
    });
  }

  private onTicket(ticket: Ticket) {
    const current = this.table();
    if (!current) return;
    const previous = current.tickets.find((t) => t.id === ticket.id);
    const tickets = previous
      ? current.tickets.map((t) => (t.id === ticket.id ? ticket : t))
      : [...current.tickets, ticket];
    const total = tickets.reduce((sum, t) => sum + (t.status === 'CANCELLED' ? 0 : t.total), 0);
    this.table.set({ ...current, tickets, total });
    if (previous && previous.status !== ticket.status) {
      this.showToast(ticket.status === 'READY' ? '¡Tu pedido está listo!' : this.statusLabel(ticket.status));
    }
  }

  private showToast(message: string) {
    this.toast.set(message);
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => this.toast.set(null), 2600);
  }
}
