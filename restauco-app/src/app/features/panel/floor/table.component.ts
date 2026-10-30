import { Component, computed, DestroyRef, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { LucideAngularModule, ArrowLeft, Check, Plus, X, Receipt, RotateCcw, Split } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { PaymentMethod, PaymentPart, TableDetail, Ticket, TicketStatus } from '../../../core/api/models';
import { addToCart, lineFromTicketItem } from '../../../shared/cart';
import { elapsed, minutesSince, price, time } from '../../../shared/format';
import { PanelStream } from '../panel-stream.service';
import { TicketItemsComponent } from '../ticket-items.component';
import { FloorStore } from './floor-store.service';

const STATUS: Record<TicketStatus, string> = {
  PENDING_APPROVAL: 'Por confirmar (QR)',
  SENT: 'En cocina',
  PREPARING: 'Preparando',
  READY: 'Listo para recoger',
  DELIVERED: 'Entregado',
  CANCELLED: 'Cancelado'
};

@Component({
  selector: 'app-table',
  imports: [RouterLink, LucideAngularModule, TicketItemsComponent],
  template: `
    <main class="wrap">
      <header class="head">
        <a routerLink="/panel/mesas" class="icon-btn" aria-label="Volver a las mesas"><lucide-icon [img]="ArrowLeft" [size]="20" /></a>
        @if (detail(); as d) {
          <div class="title">
            <h1 class="display">{{ d.table.name }}</h1>
            <p>
              @if (d.session) { Abierta hace {{ age(d.session.openedAt) }} · {{ d.tickets.length }} {{ d.tickets.length === 1 ? 'ronda' : 'rondas' }} }
              @else { Libre }
            </p>
          </div>
          <button type="button" class="btn btn-brand new" (click)="compose()" [disabled]="!store.menu()">
            <lucide-icon [img]="Plus" [size]="18" /> Pedido
            @if (draftCount(); as n) { <span class="draft-n tabular" [attr.aria-label]="n + ' sin enviar'">{{ n }}</span> }
          </button>
        }
      </header>

      @if (actionError()) { <p class="error-text" role="alert">{{ actionError() }}</p> }

      @if (detail(); as d) {
        @if (!d.tickets.length) {
          <div class="empty">
            <p>Sin pedidos todavía.</p>
            <button type="button" class="btn btn-brand" (click)="compose()" [disabled]="!store.menu()">Tomar pedido</button>
          </div>
        }
        <ol class="rounds">
          @for (t of rounds(); track t.id) {
            <li class="round" [attr.data-status]="t.status">
              <header>
                <span class="status label">{{ statusLabel(t.status) }}</span>
                <span class="meta">{{ t.source === 'QR' ? 'QR · ' : '' }}{{ time(t.createdAt) }}</span>
              </header>
              <app-ticket-items [items]="t.items" />
              @if (t.cancelReason) { <p class="reason">{{ t.cancelReason }}</p> }
              <footer>
                <span class="tabular amount" [class.void]="t.status === 'CANCELLED'">{{ price(t.total) }}</span>
                @switch (t.status) {
                  @case ('PENDING_APPROVAL') {
                    <button type="button" class="btn btn-sm btn-danger" (click)="cancel(t, 'Rechazado por el mesero')" [disabled]="busy() === t.id">Rechazar</button>
                    <button type="button" class="btn btn-sm btn-brand" (click)="act(t, 'approve')" [disabled]="busy() === t.id"><lucide-icon [img]="Check" [size]="16" /> A cocina</button>
                  }
                  @case ('SENT') {
                    @if (confirmCancel() === t.id) {
                      <span class="ask">¿Cancelar?</span>
                      <button type="button" class="btn btn-sm btn-ghost" (click)="confirmCancel.set(null)">No</button>
                      <button type="button" class="btn btn-sm btn-danger" (click)="cancel(t, null)" [disabled]="busy() === t.id">Sí, cancelar</button>
                    } @else {
                      <button type="button" class="btn btn-sm btn-ghost" (click)="confirmCancel.set(t.id)">Cancelar</button>
                    }
                  }
                  @case ('READY') {
                    <button type="button" class="btn btn-sm btn-ok" (click)="act(t, 'deliver')" [disabled]="busy() === t.id"><lucide-icon [img]="Check" [size]="16" /> Entregado</button>
                  }
                }
                @if (t.status !== 'CANCELLED' && t.status !== 'PENDING_APPROVAL') {
                  <button type="button" class="btn btn-sm btn-ghost" (click)="repeat(t)" [disabled]="!store.menu()" [attr.aria-label]="'Pedir otra vez la ronda de las ' + time(t.createdAt)">
                    <lucide-icon [img]="RotateCcw" [size]="15" /> Otra igual
                  </button>
                }
              </footer>
            </li>
          }
        </ol>

        @if (d.session) {
          <footer class="bill">
            <div>
              <span class="label">Total</span>
              <strong class="tabular">{{ price(d.total) }}</strong>
            </div>
            <button type="button" class="btn btn-ghost" (click)="openClose()"><lucide-icon [img]="Receipt" [size]="18" /> Cerrar cuenta</button>
          </footer>
        }
      } @else if (loadError()) {
        <div class="empty"><p>{{ loadError() }}</p><button type="button" class="btn btn-ghost btn-sm" (click)="load()">Reintentar</button></div>
      } @else {
        <p class="empty">Cargando…</p>
      }
    </main>

    @if (closing() && detail()?.session; as session) {
      <div class="scrim" (click)="closing.set(false)">
        <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="close-title" (click)="$event.stopPropagation()">
          <header class="sheet-head">
            <h2 id="close-title" class="display">Cerrar cuenta</h2>
            <button type="button" class="icon-btn" (click)="closing.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
          </header>
          <div class="sheet-body">
            <p class="bill-total"><span>{{ detail()!.table.name }}</span><strong class="tabular">{{ price(detail()!.total) }}</strong></p>
            @if (hasActive()) {
              <p class="warn">Hay pedidos sin entregar. Entrégalos o cancélalos antes de cerrar la cuenta.</p>
            } @else {
              <div class="pay-head">
                <h3 class="label">{{ splitting() ? 'Dividir el pago' : '¿Cómo pagó?' }}</h3>
                @if (methods().length > 1) {
                  <button type="button" class="btn btn-sm btn-ghost" [attr.aria-pressed]="splitting()" (click)="toggleSplit()">
                    <lucide-icon [img]="Split" [size]="15" /> {{ splitting() ? 'Un solo método' : 'Varios métodos' }}
                  </button>
                }
              </div>
              <div class="methods" [attr.role]="splitting() ? 'group' : 'radiogroup'" aria-label="Método de pago">
                @for (m of methods(); track m.id) {
                  <button type="button" class="method" [attr.role]="splitting() ? 'checkbox' : 'radio'" [attr.aria-checked]="picked().includes(m.id)" (click)="pick(m.id)">{{ m.name }}</button>
                } @empty {
                  <p class="warn">No hay métodos de pago activos. El dueño los configura en Pagos.</p>
                }
              </div>
              @if (splitting()) {
                @if (picked().length < 2) {
                  <p class="split-hint">Toca los métodos con que pagó (hasta {{ maxParts }}). Escribes cuánto va en cada uno y el último se completa solo.</p>
                } @else {
                  <ul class="parts">
                    @for (id of picked(); track id; let last = $last) {
                      <li>
                        <span class="part-name">{{ methodName(id) }}</span>
                        @if (last) {
                          <strong class="tabular rest" [class.bad]="rest() <= 0">{{ price(rest()) }}</strong>
                        } @else {
                          <label class="amount-field">
                            <span aria-hidden="true">$</span>
                            <input type="text" inputmode="numeric" autocomplete="off" [attr.aria-label]="'Monto en ' + methodName(id)"
                                   [value]="amountText(id)" (input)="setAmount(id, $any($event.target).value)" (focus)="$any($event.target).select()" />
                          </label>
                        }
                      </li>
                    }
                  </ul>
                  @if (rest() <= 0) { <p class="warn">Los montos ya cubren la cuenta: baja alguno para que {{ methodName(picked()[picked().length - 1]) }} pague algo, o quítalo.</p> }
                }
              }
            }
            @if (closeError()) { <p class="error-text" role="alert">{{ closeError() }}</p> }
          </div>
          <footer class="sheet-foot">
            <button type="button" class="btn btn-brand wide" (click)="close(session.id)" [disabled]="!payments() || hasActive() || closingBusy()">
              {{ closingBusy() ? 'Cerrando…' : 'Cobrar ' + price(detail()!.total) + (payments()?.length === 1 ? ' · ' + methodName(payments()![0].paymentMethodId) : '') }}
            </button>
          </footer>
        </section>
      </div>
    }
  `,
  styles: `
    .wrap { max-width: 46rem; margin: 0 auto; padding: 1rem 0.9rem 7rem; }
    .head { display: flex; align-items: center; gap: 0.8rem; margin-bottom: 1rem; }
    .title { flex: 1; min-width: 0; }
    .title h1 { margin: 0; font-size: 2.1rem; line-height: 1; color: var(--brand); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .draft-n { min-width: 1.5rem; height: 1.5rem; padding: 0 0.35rem; border-radius: 999px; display: inline-grid; place-items: center; font-size: 0.85rem; background: var(--brand-ink); color: var(--brand); }
    .title p { margin: 0.25rem 0 0; color: var(--ink-muted); font-size: 0.9rem; }
    .new { flex: none; white-space: nowrap; }
    .rounds { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.75rem; }
    .round { padding: 0.9rem 1rem; border-radius: 18px; background: var(--ground-raised); border: 2px solid var(--line); display: grid; gap: 0.6rem; }
    .round header { display: flex; justify-content: space-between; align-items: baseline; }
    .status { font-size: 1rem; }
    .meta { color: var(--ink-faint); font-size: 0.85rem; }
    .round[data-status="PENDING_APPROVAL"] { border-color: var(--warn); }
    .round[data-status="PENDING_APPROVAL"] .status { color: var(--warn); }
    .round[data-status="SENT"] .status, .round[data-status="PREPARING"] .status { color: var(--brand); }
    .round[data-status="READY"] { border-color: var(--ok); }
    .round[data-status="READY"] .status { color: var(--ok); }
    .round[data-status="DELIVERED"], .round[data-status="CANCELLED"] { opacity: 0.6; }
    .round footer { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
    .round footer .btn { min-height: 44px; }
    .amount { margin-right: auto; font-family: var(--font-label); font-weight: 600; }
    .amount.void { text-decoration: line-through; color: var(--ink-faint); }
    .ask { color: var(--ink-muted); font-size: 0.9rem; }
    .reason { margin: 0; color: var(--ink-faint); font-size: 0.88rem; }

    .bill { position: fixed; left: 0; right: 0; bottom: 0; z-index: 25; display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.85rem max(0.9rem, calc((100vw - 46rem) / 2 + 0.9rem)) calc(0.85rem + env(safe-area-inset-bottom)); background: var(--ground-sunken); border-top: 1px solid var(--line); }
    .bill div { display: flex; flex-direction: column; }
    .bill .label { font-size: 0.8rem; color: var(--ink-faint); }
    .bill strong { font-family: var(--font-label); font-size: 1.6rem; color: var(--brand); line-height: 1.1; }

    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.4rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .sheet-body { padding: 0 1.1rem 1rem; }
    .bill-total { display: flex; justify-content: space-between; align-items: baseline; margin: 0.4rem 0 0.8rem; font-size: 1.05rem; }
    .bill-total strong { font-family: var(--font-label); font-size: 1.8rem; color: var(--brand); }
    .sheet-body h3 { margin: 0.6rem 0 0.5rem; font-size: 0.95rem; color: var(--ink-muted); }
    .methods { display: grid; grid-template-columns: repeat(auto-fill, minmax(8.5rem, 1fr)); gap: 0.5rem; }
    .method { min-height: 56px; border-radius: 14px; border: 2px solid var(--line); background: var(--ground-sunken); color: var(--ink); font-family: var(--font-label); text-transform: uppercase; font-size: 1.05rem; letter-spacing: 0.03em; }
    .method[aria-checked="true"] { border-color: var(--brand); background: color-mix(in oklab, var(--brand) 16%, var(--ground-sunken)); color: var(--brand); }
    .warn { color: var(--warn); font-weight: 600; }
    .pay-head { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; margin: 0.6rem 0 0.5rem; }
    .pay-head h3 { margin: 0; }
    .split-hint { color: var(--ink-muted); font-size: 0.9rem; margin: 0.7rem 0 0; }
    .parts { list-style: none; margin: 0.8rem 0 0; padding: 0; display: grid; gap: 0.45rem; }
    .parts li { display: flex; align-items: center; justify-content: space-between; gap: 0.8rem; min-height: 56px; padding: 0.3rem 0.4rem 0.3rem 0.9rem; border-radius: 14px; background: var(--ground-sunken); border: 1px solid var(--line); }
    .part-name { font-family: var(--font-label); text-transform: uppercase; }
    .amount-field { display: flex; align-items: center; gap: 0.3rem; width: 9.5rem; min-height: 46px; padding: 0 0.7rem; border-radius: 12px; border: 2px solid var(--line); background: var(--ground-raised); color: var(--ink-muted); }
    .amount-field:focus-within { border-color: var(--brand); }
    .amount-field input { flex: 1; min-width: 0; border: 0; outline: none; background: none; color: var(--ink); font-family: var(--font-label); font-size: 1.2rem; text-align: right; }
    .rest { font-family: var(--font-label); font-size: 1.25rem; padding-right: 0.7rem; color: var(--ok); }
    .rest.bad { color: var(--danger); }
    .sheet-foot { padding: 0.9rem 1.1rem 1.1rem; border-top: 1px solid var(--line); }
    .wide { width: 100%; min-height: 54px; }
  `
})
export default class TableComponent implements OnInit, OnDestroy {
  readonly id = input.required<string>();

  private api = inject(ApiService);
  private stream = inject(PanelStream);
  readonly store = inject(FloorStore);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly detail = signal<TableDetail | null>(null);
  readonly methods = signal<PaymentMethod[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly busy = signal<number | null>(null);
  readonly confirmCancel = signal<number | null>(null);
  readonly closing = signal(false);
  /** Chosen methods in the order they were tapped; with a split, the last one takes the rest. */
  readonly picked = signal<number[]>([]);
  readonly splitting = signal(false);
  readonly amounts = signal<Record<number, number>>({});
  readonly maxParts = 5;
  readonly closingBusy = signal(false);
  readonly closeError = signal<string | null>(null);
  readonly now = signal(Date.now());

  readonly price = price;
  readonly time = time;
  readonly ArrowLeft = ArrowLeft;
  readonly Plus = Plus;
  readonly Check = Check;
  readonly X = X;
  readonly Receipt = Receipt;
  readonly RotateCcw = RotateCcw;
  readonly Split = Split;

  /** Newest round first: that is what the waiter is acting on. */
  readonly rounds = computed(() => [...(this.detail()?.tickets ?? [])].reverse());
  readonly hasActive = computed(() => (this.detail()?.tickets ?? [])
    .some((t) => t.status !== 'DELIVERED' && t.status !== 'CANCELLED'));

  readonly draftCount = computed(() => this.store.draft(Number(this.id())).reduce((n, l) => n + l.qty, 0));
  readonly total = computed(() => this.detail()?.total ?? 0);
  /** What the split leaves for the last method. */
  readonly rest = computed(() => {
    const ids = this.picked();
    const amounts = this.amounts();
    return this.total() - ids.slice(0, -1).reduce((sum, id) => sum + (amounts[id] ?? 0), 0);
  });
  /** The payment to send, or null while it is not complete. */
  readonly payments = computed<PaymentPart[] | null>(() => {
    const ids = this.picked();
    if (!ids.length) return null;
    if (ids.length === 1) return [{ paymentMethodId: ids[0], amount: this.total() }];
    const amounts = this.amounts();
    const parts = ids.map((id, i) => ({ paymentMethodId: id, amount: i === ids.length - 1 ? this.rest() : amounts[id] ?? 0 }));
    return parts.every((p) => p.amount > 0) ? parts : null;
  });

  private clock = setInterval(() => this.now.set(Date.now()), 30_000);

  ngOnInit() {
    this.store.start();
    this.load();
    const tableId = Number(this.id());
    this.stream.ticket$.pipe(filter((t) => t.tableId === tableId), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
    this.stream.floor$.pipe(filter((id) => id === tableId), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
    this.stream.opened$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
    this.store.sent$.pipe(filter((t) => t.tableId === tableId), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load());
  }

  ngOnDestroy() {
    clearInterval(this.clock);
  }

  load() {
    this.api.tableDetail(Number(this.id())).subscribe({
      next: (d) => {
        this.detail.set(d);
        this.loadError.set(null);
      },
      error: (e) => this.loadError.set(errorMessage(e))
    });
  }

  age(instant: string | null): string {
    return elapsed(minutesSince(instant, this.now()));
  }

  statusLabel(s: TicketStatus): string {
    return STATUS[s];
  }

  act(t: Ticket, action: 'approve' | 'deliver') {
    this.run(t, action === 'approve' ? this.store.approve(t.id) : this.store.deliver(t.id));
  }

  cancel(t: Ticket, reason: string | null) {
    this.run(t, this.store.cancel(t.id, reason));
  }

  compose() {
    this.store.compose(Number(this.id()));
  }

  /** Puts a past round back into this table's draft ("otra igual") and opens it. */
  repeat(t: Ticket) {
    const menu = this.store.menu();
    if (!menu) return;
    const tableId = Number(this.id());
    let lines = this.store.draft(tableId);
    let skipped = 0;
    for (const item of t.items) {
      const dish = menu.dishes.find((d) => d.id === item.dishId);
      if (dish?.orderable) lines = addToCart(lines, lineFromTicketItem(item, dish));
      else skipped++;
    }
    this.store.setDraft(tableId, lines);
    this.actionError.set(skipped ? `${skipped} ${skipped === 1 ? 'plato ya no está disponible' : 'platos ya no están disponibles'} y no se copió.` : null);
    this.compose();
  }

  openClose() {
    this.closeError.set(null);
    this.splitting.set(false);
    this.picked.set([]);
    this.amounts.set({});
    this.closing.set(true);
    this.api.activePaymentMethods().subscribe({
      next: (methods) => {
        this.methods.set(methods);
        if (methods.length === 1) this.picked.set([methods[0].id]);
      }
    });
  }

  methodName(id: number): string {
    return this.methods().find((m) => m.id === id)?.name ?? '';
  }

  toggleSplit() {
    const on = !this.splitting();
    this.splitting.set(on);
    this.amounts.set({});
    if (!on) this.picked.update((ids) => ids.slice(0, 1));
  }

  pick(id: number) {
    if (!this.splitting()) {
      this.picked.set([id]);
      return;
    }
    if (this.picked().includes(id)) {
      this.picked.update((ids) => ids.filter((x) => x !== id));
      this.amounts.update(({ [id]: _, ...rest }) => rest);
    } else if (this.picked().length < this.maxParts) {
      this.picked.update((ids) => [...ids, id]);
    }
  }

  amountText(id: number): string {
    const value = this.amounts()[id];
    return value ? value.toLocaleString('es-CO') : '';
  }

  /** Digits only: "5.000", "5000" and "$ 5 000" all mean 5000. */
  setAmount(id: number, text: string) {
    const value = Number(text.replace(/\D/g, '').slice(0, 9)) || 0;
    this.amounts.update((a) => ({ ...a, [id]: value }));
  }

  close(sessionId: number) {
    const payments = this.payments();
    if (!payments) return;
    this.closingBusy.set(true);
    this.closeError.set(null);
    this.api.closeSession(sessionId, payments).subscribe({
      next: () => {
        this.closingBusy.set(false);
        this.closing.set(false);
        this.router.navigate(['/panel/mesas']);
      },
      error: (e) => {
        this.closingBusy.set(false);
        this.closeError.set(errorMessage(e));
      }
    });
  }

  private run(t: Ticket, action: ReturnType<FloorStore['approve']>) {
    this.busy.set(t.id);
    this.actionError.set(null);
    action.subscribe({
      next: () => {
        this.busy.set(null);
        this.confirmCancel.set(null);
        this.load();
      },
      error: (e) => {
        this.busy.set(null);
        this.actionError.set(errorMessage(e));
        this.load();
      }
    });
  }
}
