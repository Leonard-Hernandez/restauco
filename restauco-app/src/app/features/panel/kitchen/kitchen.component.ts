import { Component, computed, DestroyRef, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule, Ban, Flame, MonitorSmartphone, QrCode, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Dish, Ingredient, Menu, Ticket } from '../../../core/api/models';
import { chime } from '../../../shared/chime';
import { elapsed, minutesSince } from '../../../shared/format';
import { PanelStream } from '../panel-stream.service';
import { TicketItemsComponent } from '../ticket-items.component';

type Column = 'SENT' | 'PREPARING' | 'READY';

const KITCHEN = new Set(['SENT', 'PREPARING', 'READY']);
/** Minutes after which a ticket turns amber, then red */
const SLOW = 10;
const LATE = 20;

/**
 * The kitchen board, for a tablet or TV. New tickets arrive live with a chime; the cook
 * starts them and marks them ready, which alerts the waiter.
 */
@Component({
  selector: 'app-kitchen',
  imports: [LucideAngularModule, TicketItemsComponent],
  template: `
    <div class="toolbar">
      <div class="mobile-tabs" role="tablist" aria-label="Columnas">
        @for (c of columns; track c.key) {
          <button type="button" role="tab" class="chip" [class.on]="mobileColumn() === c.key" [attr.aria-selected]="mobileColumn() === c.key"
                  (click)="mobileColumn.set(c.key)">{{ c.title }} · {{ byStatus()[c.key].length }}</button>
        }
      </div>
      <span class="spacer"></span>
      @if (wakeSupported) {
        <button type="button" class="btn btn-sm" [class.btn-brand]="awake()" [class.btn-ghost]="!awake()" (click)="toggleWake()"
                [attr.aria-pressed]="awake()" title="Evita que la pantalla se apague">
          <lucide-icon [img]="MonitorSmartphone" [size]="16" /> {{ awake() ? 'Pantalla fija' : 'Fijar pantalla' }}
        </button>
      }
      <button type="button" class="btn btn-sm btn-ghost" (click)="openSoldOut()"><lucide-icon [img]="Ban" [size]="16" /> Agotados</button>
    </div>

    @if (error()) { <p class="error-text banner" role="alert">{{ error() }}</p> }

    <div class="board">
      @for (c of columns; track c.key) {
        <section class="column" [class.mobile-hidden]="mobileColumn() !== c.key" [attr.aria-labelledby]="'col-' + c.key" [attr.data-col]="c.key">
          <h2 class="col-title label" [id]="'col-' + c.key">{{ c.title }} <span class="badge">{{ byStatus()[c.key].length }}</span></h2>
          @for (t of byStatus()[c.key]; track t.id) {
            <article class="ticket" [attr.data-age]="ageClass(t)" [class.fresh]="fresh().has(t.id)">
              <header>
                <span class="table display">{{ t.deliveryId ? 'Domicilio ' + t.deliveryId : 'Mesa ' + t.tableNumber }}</span>
                <span class="age tabular">
                  @if (t.source === 'QR') { <lucide-icon [img]="QrCode" [size]="14" aria-label="Pedido por QR" /> }
                  {{ c.key === 'READY' ? 'listo hace ' + age(t.readyAt) : age(t.sentAt) }}
                </span>
              </header>
              <app-ticket-items [items]="t.items" size="lg" />
              <footer>
                @switch (t.status) {
                  @case ('SENT') {
                    <button type="button" class="btn btn-ghost" (click)="move(t, 'ready')" [disabled]="busy() === t.id">Listo ya</button>
                    <button type="button" class="btn btn-brand grow" (click)="move(t, 'start')" [disabled]="busy() === t.id">
                      <lucide-icon [img]="Flame" [size]="18" /> Empezar
                    </button>
                  }
                  @case ('PREPARING') {
                    <button type="button" class="btn btn-ok grow big" (click)="move(t, 'ready')" [disabled]="busy() === t.id">¡Listo!</button>
                  }
                  @case ('READY') {
                    <span class="waiting">Esperando al mesero</span>
                  }
                }
              </footer>
            </article>
          } @empty {
            <p class="col-empty">{{ c.empty }}</p>
          }
        </section>
      }
    </div>

    @if (soldOutOpen()) {
      <div class="scrim" (click)="soldOutOpen.set(false)">
        <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="soldout-title" (click)="$event.stopPropagation()">
          <header class="sheet-head">
            <h2 id="soldout-title" class="display">¿Qué se acabó?</h2>
            <button type="button" class="icon-btn" (click)="soldOutOpen.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
          </header>
          <div class="soldout-tabs" role="tablist" aria-label="Qué marcar">
            <button type="button" role="tab" class="chip" [class.on]="soldOutTab() === 'ingredients'" [attr.aria-selected]="soldOutTab() === 'ingredients'" (click)="soldOutTab.set('ingredients')">Ingredientes</button>
            <button type="button" role="tab" class="chip" [class.on]="soldOutTab() === 'dishes'" [attr.aria-selected]="soldOutTab() === 'dishes'" (click)="soldOutTab.set('dishes')">Platos</button>
          </div>
          @if (soldOutTab() === 'ingredients') {
            <p class="sheet-lead">Si se acaba un ingrediente, los platos que no pueden ir sin él se agotan solos, los que sí pueden salen sin él, y los rellenos hechos con él no se pueden escoger.</p>
          } @else {
            <p class="sheet-lead">Los platos agotados siguen en la carta pero no se pueden pedir.</p>
          }
          @if (menu(); as m) {
            <ul class="soldout-list">
              @if (soldOutTab() === 'ingredients') {
                @for (i of m.ingredients; track i.id) {
                  <li>
                    <span class="what">
                      <span>{{ i.name }}</span>
                      @if (!i.available && affected(i.id); as a) { <small>{{ a }}</small> }
                    </span>
                    <button type="button" class="switch" role="switch" [attr.aria-checked]="i.available" [disabled]="busyIngredient() === i.id"
                            [attr.aria-label]="i.name + (i.available ? ': hay' : ': se acabó')" (click)="toggleIngredient(i)">
                      {{ i.available ? 'Hay' : 'Se acabó' }}
                    </button>
                  </li>
                }
              } @else {
                @for (d of m.dishes; track d.id) {
                  <li>
                    <span class="what">
                      <span>{{ d.name }}</span>
                      @if (d.available && !d.orderable) { <small>Agotado: se acabó {{ d.soldOutReason?.toLowerCase() }}</small> }
                    </span>
                    <button type="button" class="switch" role="switch" [attr.aria-checked]="d.available"
                            [attr.aria-label]="d.name + (d.available ? ': disponible' : ': agotado')" (click)="toggleAvailable(d)">
                      {{ d.available ? 'Disponible' : 'Agotado' }}
                    </button>
                  </li>
                }
              }
            </ul>
          } @else { <p class="empty">Cargando…</p> }
        </section>
      </div>
    }
  `,
  styles: `
    :host { display: block; }
    .toolbar { display: flex; align-items: center; gap: 0.5rem; padding: 0.7rem 0.9rem 0; flex-wrap: wrap; }
    .spacer { flex: 1; }
    .mobile-tabs { display: flex; gap: 0.35rem; overflow-x: auto; scrollbar-width: none; }
    .banner { margin: 0.6rem 0.9rem 0; }
    .board { display: grid; gap: 0.9rem; padding: 0.8rem 0.9rem 3rem; }
    .column { display: flex; flex-direction: column; gap: 0.75rem; min-width: 0; }
    .col-title { display: none; align-items: center; gap: 0.5rem; margin: 0; font-size: 1.1rem; color: var(--ink-muted); }
    .column[data-col="READY"] .col-title { color: var(--ok); }
    @media (min-width: 900px) {
      .mobile-tabs { display: none; }
      .board { grid-template-columns: 1.15fr 1.15fr 0.8fr; align-items: start; }
      .column.mobile-hidden { display: flex; }
      .col-title { display: flex; }
    }
    @media (max-width: 899px) { .column.mobile-hidden { display: none; } }

    .ticket { border-radius: 18px; background: var(--ground-raised); border: 2px solid var(--line); border-top: 8px solid var(--ok); padding: 0.85rem 1rem 1rem; display: flex; flex-direction: column; gap: 0.8rem; }
    .ticket[data-age="slow"] { border-top-color: var(--warn); }
    .ticket[data-age="late"] { border-top-color: var(--danger); }
    .column[data-col="READY"] .ticket { border-top-color: var(--ok); opacity: 0.8; }
    .ticket.fresh { animation: pulse 1.2s ease-out 2; }
    @keyframes pulse { 0% { box-shadow: 0 0 0 0 rgb(255 194 26 / 0.7); } 100% { box-shadow: 0 0 0 18px rgb(255 194 26 / 0); } }
    .ticket header { display: flex; justify-content: space-between; align-items: baseline; gap: 0.5rem; }
    .table { font-size: 2rem; line-height: 1; }
    .age { display: inline-flex; align-items: center; gap: 0.3rem; font-family: var(--font-label); font-size: 1.05rem; color: var(--ink-muted); }
    .ticket[data-age="slow"] .age { color: var(--warn); }
    .ticket[data-age="late"] .age { color: var(--danger); font-weight: 700; }
    .ticket footer { display: flex; gap: 0.5rem; }
    .grow { flex: 1; }
    .big { min-height: 58px; font-size: 1.3rem; }
    .waiting { color: var(--ok); font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.05em; }
    .col-empty { margin: 0; padding: 1.5rem 1rem; border: 2px dashed var(--line); border-radius: 18px; text-align: center; color: var(--ink-faint); }

    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.2rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .sheet-lead { margin: 0 1.1rem 0.6rem; color: var(--ink-faint); font-size: 0.9rem; }
    .soldout-list { list-style: none; margin: 0; padding: 0 1.1rem 1.2rem; }
    .soldout-list li { display: flex; justify-content: space-between; align-items: center; gap: 1rem; min-height: 56px; border-bottom: 1px solid var(--line); }
    .soldout-tabs { display: flex; gap: 0.4rem; padding: 0 1.1rem 0.6rem; }
    .what { display: flex; flex-direction: column; gap: 0.1rem; padding: 0.4rem 0; }
    .what small { color: var(--warn); font-size: 0.82rem; }
    .switch:disabled { opacity: 0.5; }
    .switch { min-width: 7.5rem; min-height: 40px; border-radius: 999px; border: 0; font-family: var(--font-label); text-transform: uppercase; font-weight: 600; letter-spacing: 0.04em; background: color-mix(in oklab, var(--ok) 22%, transparent); color: var(--ok); }
    .switch[aria-checked="false"] { background: var(--danger); color: #fff; }
  `
})
export default class KitchenComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private stream = inject(PanelStream);
  private destroyRef = inject(DestroyRef);

  readonly tickets = signal<Ticket[]>([]);
  readonly error = signal<string | null>(null);
  readonly busy = signal<number | null>(null);
  readonly now = signal(Date.now());
  readonly fresh = signal<Set<number>>(new Set());
  readonly mobileColumn = signal<Column>('SENT');
  readonly soldOutOpen = signal(false);
  readonly soldOutTab = signal<'ingredients' | 'dishes'>('ingredients');
  readonly busyIngredient = signal<number | null>(null);
  readonly menu = signal<Menu | null>(null);
  readonly awake = signal(false);
  readonly wakeSupported = 'wakeLock' in navigator;

  readonly Flame = Flame;
  readonly QrCode = QrCode;
  readonly Ban = Ban;
  readonly X = X;
  readonly MonitorSmartphone = MonitorSmartphone;

  readonly columns: { key: Column; title: string; empty: string }[] = [
    { key: 'SENT', title: 'Nuevos', empty: 'Sin pedidos nuevos' },
    { key: 'PREPARING', title: 'Preparando', empty: 'Nada en el fuego' },
    { key: 'READY', title: 'Listos', empty: 'Nada esperando al mesero' }
  ];

  readonly byStatus = computed(() => {
    const groups: Record<Column, Ticket[]> = { SENT: [], PREPARING: [], READY: [] };
    for (const t of this.tickets()) groups[t.status as Column]?.push(t);
    return groups;
  });

  private clock = setInterval(() => this.now.set(Date.now()), 20_000);
  private wakeLock: WakeLockSentinel | null = null;
  private onVisible = () => {
    if (this.awake() && document.visibilityState === 'visible') void this.requestWake();
  };

  ngOnInit() {
    this.load();
    this.stream.opened$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
    this.stream.ticket$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((t) => this.onTicket(t));
    document.addEventListener('visibilitychange', this.onVisible);
  }

  ngOnDestroy() {
    clearInterval(this.clock);
    document.removeEventListener('visibilitychange', this.onVisible);
    void this.wakeLock?.release();
  }

  load() {
    this.api.kitchenTickets().subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.error.set(null);
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  age(instant: string | null): string {
    return elapsed(minutesSince(instant, this.now()));
  }

  ageClass(t: Ticket): 'ok' | 'slow' | 'late' {
    if (t.status === 'READY') return 'ok';
    const m = minutesSince(t.sentAt, this.now());
    return m >= LATE ? 'late' : m >= SLOW ? 'slow' : 'ok';
  }

  move(t: Ticket, action: 'start' | 'ready') {
    this.busy.set(t.id);
    const call = action === 'start' ? this.api.startTicket(t.id) : this.api.readyTicket(t.id);
    call.subscribe({
      next: (updated) => {
        this.busy.set(null);
        this.upsert(updated);
      },
      error: (e) => {
        this.busy.set(null);
        this.error.set(errorMessage(e));
        this.load();
      }
    });
  }

  openSoldOut() {
    this.soldOutOpen.set(true);
    this.api.menu().subscribe({ next: (m) => this.menu.set(m) });
  }

  toggleAvailable(d: Dish) {
    this.api.setAvailability(d.id, !d.available).subscribe({
      next: (updated) => this.menu.update((m) => m && { ...m, dishes: m.dishes.map((x) => (x.id === updated.id ? updated : x)) }),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  toggleIngredient(i: Ingredient) {
    this.busyIngredient.set(i.id);
    this.api.setIngredientAvailability(i.id, !i.available).subscribe({
      // Dishes and options change with it: reload the menu to show what it took with it
      next: () => this.api.menu().subscribe({
        next: (m) => {
          this.menu.set(m);
          this.busyIngredient.set(null);
        },
        error: () => this.busyIngredient.set(null)
      }),
      error: (e) => {
        this.busyIngredient.set(null);
        this.error.set(errorMessage(e));
      }
    });
  }

  /** "Agota: Arepa Pelúa · sin él: Sifrina · relleno: 3 arepas" */
  affected(ingredientId: number): string | null {
    const m = this.menu();
    if (!m) return null;
    const stopped: string[] = [];
    const without: string[] = [];
    let options = 0;
    for (const d of m.dishes) {
      const ing = d.ingredients.find((x) => x.id === ingredientId);
      if (ing) (ing.removable ? without : stopped).push(d.name);
      if (d.modifiers.some((g) => g.options.some((o) => o.ingredientIds.includes(ingredientId)))) options++;
    }
    const parts: string[] = [];
    if (stopped.length) parts.push(`agota ${stopped.join(', ')}`);
    if (without.length) parts.push(`salen sin él: ${without.join(', ')}`);
    if (options) parts.push(`no se puede escoger como relleno en ${options} ${options === 1 ? 'plato' : 'platos'}`);
    return parts.length ? parts.join(' · ') : null;
  }

  async toggleWake() {
    if (this.awake()) {
      this.awake.set(false);
      await this.wakeLock?.release();
      this.wakeLock = null;
    } else {
      this.awake.set(true);
      await this.requestWake();
    }
  }

  private async requestWake() {
    try {
      this.wakeLock = await navigator.wakeLock.request('screen');
    } catch {
      this.awake.set(false);
    }
  }

  private onTicket(ticket: Ticket) {
    const isNew = ticket.status === 'SENT' && !this.tickets().some((t) => t.id === ticket.id);
    this.upsert(ticket);
    if (isNew) {
      chime('new');
      this.fresh.update((s) => new Set(s).add(ticket.id));
      setTimeout(() => this.fresh.update((s) => {
        const next = new Set(s);
        next.delete(ticket.id);
        return next;
      }), 3000);
    }
  }

  private upsert(ticket: Ticket) {
    this.tickets.update((list) => {
      const rest = list.filter((t) => t.id !== ticket.id);
      return KITCHEN.has(ticket.status)
        ? [...rest, ticket].sort((a, b) => (a.sentAt ?? a.createdAt).localeCompare(b.sentAt ?? b.createdAt))
        : rest;
    });
  }
}
