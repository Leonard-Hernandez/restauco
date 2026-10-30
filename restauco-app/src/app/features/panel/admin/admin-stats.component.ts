import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, merge } from 'rxjs';
import { LucideAngularModule, TrendingDown, TrendingUp } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Breakdown, ReportPeriod, SalesReport } from '../../../core/api/models';
import { price } from '../../../shared/format';
import { PanelStream } from '../panel-stream.service';

const PERIODS: { value: ReportPeriod; label: string; previous: string }[] = [
  { value: 'TODAY', label: 'Hoy', previous: 'ayer a esta hora' },
  { value: 'WEEK', label: '7 días', previous: 'los 7 días anteriores' },
  { value: 'MONTH', label: '30 días', previous: 'los 30 días anteriores' }
];

const WEEKDAY = new Intl.DateTimeFormat('es-CO', { weekday: 'short', timeZone: 'UTC' });
const HOUR = new Intl.DateTimeFormat('es-CO', { hour: 'numeric', hour12: true, timeZone: 'UTC' });
const LONG_DAY = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long', timeZone: 'UTC' });

interface Bar { key: string; label: string; tip: string; sales: number; bills: number; height: number; showLabel: boolean }

/** "$45 mil", "$1,2 M": short amounts for tiles and axes; full ones go in tooltips. */
function compact(value: number): string {
  if (value >= 1_000_000) return `$${(value / 1_000_000).toLocaleString('es-CO', { maximumFractionDigits: 1 })} M`;
  if (value >= 1_000) return `$${Math.round(value / 1_000).toLocaleString('es-CO')} mil`;
  return price(value);
}

/** A restaurant-local "2026-10-20T18:00:00" as a Date to format in UTC (no shift). */
function wall(local: string): Date {
  const [d, t = '00:00'] = local.split('T');
  const [y, m, day] = d.split('-').map(Number);
  const [h, min] = t.split(':').map(Number);
  return new Date(Date.UTC(y, m - 1, day, h, min));
}

/** The owner's numbers: sales, best sellers, payment methods, waiters and kitchen times. */
@Component({
  selector: 'app-admin-stats',
  imports: [LucideAngularModule],
  template: `
    <main class="wrap stats">
      <header class="page-head">
        <h1 class="display">Estadísticas</h1>
        <div class="periods" role="group" aria-label="Período">
          @for (p of periods; track p.value) {
            <button type="button" class="chip" [attr.aria-pressed]="period() === p.value" (click)="select(p.value)">{{ p.label }}</button>
          }
        </div>
      </header>
      <p class="page-lead">{{ rangeLabel() }} · Ventas de las cuentas cerradas.</p>
      @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }

      @if (report(); as r) {
        <section class="tiles" [class.stale]="loading()" aria-label="Resumen">
          <div class="tile hero">
            <span class="tile-label">Ventas</span>
            <strong class="tile-value tabular">{{ price(r.current.sales) }}</strong>
            @if (change(r.current.sales, r.previous.sales); as c) {
              <span class="delta" [attr.data-dir]="c.dir">
                <lucide-icon [img]="c.dir === 'down' ? TrendingDown : TrendingUp" [size]="15" aria-hidden="true" />
                {{ c.text }} vs {{ previousLabel() }}
              </span>
            } @else {
              <span class="delta">{{ previousLabel() }}: {{ price(r.previous.sales) }}</span>
            }
          </div>
          <div class="tile">
            <span class="tile-label">Cuentas</span>
            <strong class="tile-value tabular">{{ r.current.bills }}</strong>
            <span class="delta">{{ r.previous.bills }} {{ previousLabel() }}</span>
          </div>
          <div class="tile">
            <span class="tile-label">Ticket promedio</span>
            <strong class="tile-value tabular">{{ r.current.bills ? compact(r.current.averageTicket) : '—' }}</strong>
            <span class="delta">por cuenta</span>
          </div>
        </section>

        <section class="panel" [class.stale]="loading()">
          <h2 class="label">{{ r.granularity === 'HOUR' ? 'Ventas por hora' : 'Ventas por día' }}</h2>
          @if (bars().length && r.current.sales > 0) {
            <div class="chart" (pointerleave)="$event.pointerType === 'mouse' && hovered.set(null)">
              <div class="tooltip" aria-hidden="true">
                @if (hoveredBar(); as b) {
                  <strong>{{ b.tip }}</strong>
                  <span class="tabular">{{ price(b.sales) }} · {{ b.bills }} {{ b.bills === 1 ? 'cuenta' : 'cuentas' }}</span>
                } @else {
                  <span class="hint">Toca una barra para ver el detalle</span>
                }
              </div>
              <div class="plot">
                <span class="max-line" aria-hidden="true"><span class="tabular">{{ compact(maxSales()) }}</span></span>
                <div class="columns" [style.--n]="bars().length">
                  @for (b of bars(); track b.key) {
                    <button type="button" class="col" [class.on]="hovered() === b.key"
                            (pointerenter)="$event.pointerType === 'mouse' && hovered.set(b.key)" (focus)="hovered.set(b.key)" (click)="hovered.set(b.key)"
                            [attr.aria-label]="b.tip + ': ' + price(b.sales) + ', ' + b.bills + ' cuentas'">
                      <span class="bar" [style.height.%]="b.height"></span>
                    </button>
                  }
                </div>
              </div>
              <div class="axis" [style.--n]="bars().length" aria-hidden="true">
                @for (b of bars(); track b.key) { <span>{{ b.showLabel ? b.label : '' }}</span> }
              </div>
            </div>
            <details class="as-table">
              <summary>Ver como tabla</summary>
              <table>
                <thead><tr><th scope="col">{{ r.granularity === 'HOUR' ? 'Hora' : 'Día' }}</th><th scope="col">Cuentas</th><th scope="col">Ventas</th></tr></thead>
                <tbody>
                  @for (b of bars(); track b.key) {
                    <tr><td>{{ b.tip }}</td><td class="tabular">{{ b.bills }}</td><td class="tabular">{{ price(b.sales) }}</td></tr>
                  }
                </tbody>
              </table>
            </details>
          } @else {
            <p class="none">Aún no hay cuentas cerradas {{ period() === 'TODAY' ? 'hoy' : 'en este período' }}.</p>
          }
        </section>

        <div class="split">
          <section class="panel" [class.stale]="loading()">
            <h2 class="label">Lo más pedido</h2>
            @if (r.topDishes.length) {
              <ol class="ranks">
                @for (d of r.topDishes; track d.dishId) {
                  <li>
                    <div class="rank-row"><span class="rank-name">{{ d.name }}</span><span class="rank-value tabular">{{ d.units }}</span></div>
                    <div class="track" aria-hidden="true"><span class="fill" [style.width.%]="share(d.units, r.topDishes[0].units)"></span></div>
                    <span class="rank-sub tabular">{{ price(d.sales) }}</span>
                  </li>
                }
              </ol>
            } @else {
              <p class="none">Sin pedidos enviados a cocina.</p>
            }
          </section>

          <section class="panel" [class.stale]="loading()">
            <h2 class="label">Cómo pagaron</h2>
            @if (r.byPaymentMethod.length) {
              <ol class="ranks">
                @for (m of r.byPaymentMethod; track m.label) {
                  <li>
                    <div class="rank-row"><span class="rank-name">{{ m.label }}</span><span class="rank-value tabular">{{ percent(m.sales, r.current.sales) }}</span></div>
                    <div class="track" aria-hidden="true"><span class="fill" [style.width.%]="share(m.sales, r.current.sales)"></span></div>
                    <span class="rank-sub tabular">{{ price(m.sales) }} · {{ m.bills }} {{ m.bills === 1 ? 'cuenta' : 'cuentas' }}</span>
                  </li>
                }
              </ol>
            } @else {
              <p class="none">Sin cuentas cerradas.</p>
            }

            <h2 class="label second">Por mesero</h2>
            @if (r.byWaiter.length) {
              <ol class="ranks">
                @for (w of r.byWaiter; track w.label) {
                  <li>
                    <div class="rank-row"><span class="rank-name">{{ w.label }}</span><span class="rank-value tabular">{{ compact(w.sales) }}</span></div>
                    <div class="track" aria-hidden="true"><span class="fill" [style.width.%]="share(w.sales, topSales(r.byWaiter))"></span></div>
                    <span class="rank-sub tabular">{{ w.bills }} {{ w.bills === 1 ? 'cuenta cerrada' : 'cuentas cerradas' }}</span>
                  </li>
                }
              </ol>
            } @else {
              <p class="none">Sin cuentas cerradas.</p>
            }
          </section>
        </div>

        <section class="panel" [class.stale]="loading()">
          <h2 class="label">Cocina y pedidos</h2>
          <dl class="facts">
            <div><dt>Rondas a cocina</dt><dd class="tabular">{{ r.kitchen.rounds }}</dd></div>
            <div><dt>Pedidas por QR</dt><dd class="tabular">{{ r.kitchen.rounds ? percent(r.kitchen.fromQr, r.kitchen.rounds) : '—' }}</dd></div>
            <div><dt>Preparación promedio</dt><dd class="tabular">{{ minutes(r.kitchen.prepMinutes) }}</dd></div>
            <div><dt>De “listo” a la mesa</dt><dd class="tabular">{{ minutes(r.kitchen.pickupMinutes) }}</dd></div>
            <div><dt>Canceladas</dt><dd class="tabular">{{ r.kitchen.cancelled }}</dd></div>
          </dl>
        </section>
      } @else if (!error()) {
        <p class="empty">Cargando…</p>
      }
    </main>
  `,
  styleUrl: './admin.css',
  styles: `
    .stats { max-width: 64rem; }
    .periods { display: flex; gap: 0.35rem; }
    /* Phones: the period switch sits at the bottom, under the thumb */
    @media (max-width: 720px) {
      .stats { padding-bottom: calc(6rem + env(safe-area-inset-bottom)); }
      .periods { position: fixed; z-index: 25; left: 0; right: 0; bottom: 0; gap: 0.4rem; padding: 0.6rem 0.9rem calc(0.6rem + env(safe-area-inset-bottom)); background: var(--ground-sunken); border-top: 1px solid var(--line); }
      .periods .chip { flex: 1; justify-content: center; min-height: 48px; font-size: 1rem; }
    }
    .stale { opacity: 0.55; transition: opacity 120ms; }

    .tiles { display: grid; gap: 0.6rem; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 0.4rem; }
    @media (min-width: 760px) { .tiles { grid-template-columns: 1.6fr repeat(2, minmax(0, 1fr)); } }
    .tile { display: flex; flex-direction: column; gap: 0.2rem; min-width: 0; padding: 0.9rem 1rem; border-radius: 16px; background: var(--ground-raised); border: 1px solid var(--line); }
    .tile.hero { grid-column: 1 / -1; border-color: color-mix(in oklab, var(--brand) 45%, transparent); }
    @media (min-width: 760px) { .tile.hero { grid-column: auto; } }
    .tile-label { font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.06em; font-size: 0.8rem; color: var(--ink-muted); }
    .tile-value { font-family: var(--font-label); font-weight: 600; font-size: 1.7rem; line-height: 1.1; color: var(--ink); overflow-wrap: anywhere; }
    .hero .tile-value { font-size: 2.3rem; color: var(--brand); }
    .delta { display: inline-flex; align-items: center; gap: 0.3rem; font-size: 0.82rem; color: var(--ink-faint); }
    .delta[data-dir="up"] { color: var(--ok); }
    .delta[data-dir="down"] { color: var(--danger); }

    .panel { margin-top: 1rem; padding: 1rem 1rem 1.1rem; border-radius: 16px; background: var(--ground-raised); border: 1px solid var(--line); min-width: 0; }
    .panel h2 { margin: 0 0 0.8rem; font-size: 1.05rem; color: var(--ink); }
    .panel h2.second { margin-top: 1.4rem; }
    .none { margin: 0; color: var(--ink-faint); }
    .split { display: grid; gap: 0 1rem; grid-template-columns: minmax(0, 1fr); }
    @media (min-width: 760px) { .split { grid-template-columns: repeat(2, minmax(0, 1fr)); } }

    .chart { position: relative; padding-top: 3.4rem; }
    .tooltip { position: absolute; top: 0; left: 0; right: 0; display: flex; flex-direction: column; gap: 0.1rem; font-size: 0.88rem; color: var(--ink-muted); }
    .hint { color: var(--ink-faint); }
    .tooltip strong { color: var(--ink); font-weight: 700; }
    .plot { position: relative; height: 11rem; border-bottom: 1px solid var(--line); }
    .max-line { position: absolute; left: 0; right: 0; top: 0; border-top: 1px dashed var(--line); }
    .max-line span { position: absolute; right: 0; top: -1.15rem; font-size: 0.72rem; color: var(--ink-faint); }
    .columns { position: absolute; inset: 0; display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); gap: 2px; }
    .col { position: relative; height: 100%; display: flex; align-items: flex-end; padding: 0; border: 0; background: none; cursor: pointer; -webkit-tap-highlight-color: transparent; }
    .col:focus-visible { outline: 2px solid var(--brand); outline-offset: 2px; border-radius: 4px; }
    .bar { width: 100%; max-width: 3rem; margin: 0 auto; min-height: 0; border-radius: 4px 4px 0 0; background: var(--brand); transition: background 100ms; }
    .col.on .bar { background: var(--brand-bright); }
    .columns:has(.col.on) .col:not(.on) .bar { opacity: 0.55; }
    .axis { display: grid; grid-template-columns: repeat(var(--n), minmax(0, 1fr)); gap: 2px; margin-top: 0.35rem; font-size: 0.72rem; color: var(--ink-faint); }
    .axis span { display: flex; justify-content: center; white-space: nowrap; overflow: visible; }
    /* Edge labels overflow inwards, never past the panel */
    .axis span:first-child { justify-content: flex-start; }
    .axis span:last-child { justify-content: flex-end; }
    .as-table { margin-top: 0.8rem; font-size: 0.88rem; color: var(--ink-muted); }
    .as-table summary { cursor: pointer; min-height: 32px; display: inline-flex; align-items: center; }
    .as-table table { width: 100%; border-collapse: collapse; margin-top: 0.4rem; }
    .as-table th, .as-table td { text-align: left; padding: 0.35rem 0.2rem; border-bottom: 1px solid var(--line); }
    .as-table th { font-weight: 700; color: var(--ink); }

    .ranks { list-style: none; margin: 0; padding: 0; display: grid; grid-template-columns: minmax(0, 1fr); gap: 0.75rem; }
    .rank-row { display: flex; justify-content: space-between; gap: 0.8rem; align-items: baseline; }
    .rank-name { font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.02em; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .rank-value { font-family: var(--font-label); font-weight: 600; color: var(--ink); }
    .track { height: 8px; margin: 0.3rem 0 0.2rem; border-radius: 4px; background: rgb(255 255 255 / 0.06); overflow: hidden; }
    .fill { display: block; height: 100%; border-radius: 4px; background: var(--brand); }
    .rank-sub { font-size: 0.8rem; color: var(--ink-faint); }

    .facts { display: grid; gap: 0.6rem 1rem; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 0; }
    @media (min-width: 760px) { .facts { grid-template-columns: repeat(5, minmax(0, 1fr)); } }
    .facts div { display: flex; flex-direction: column-reverse; gap: 0.1rem; }
    .facts dt { font-size: 0.82rem; color: var(--ink-muted); }
    .facts dd { margin: 0; font-family: var(--font-label); font-weight: 600; font-size: 1.4rem; color: var(--ink); }
  `
})
export default class AdminStatsComponent implements OnInit {
  private api = inject(ApiService);
  private stream = inject(PanelStream);
  private destroyRef = inject(DestroyRef);

  readonly periods = PERIODS;
  readonly period = signal<ReportPeriod>('TODAY');
  readonly report = signal<SalesReport | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly hovered = signal<string | null>(null);

  readonly price = price;
  readonly compact = compact;
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;

  readonly previousLabel = computed(() => PERIODS.find((p) => p.value === this.period())!.previous);

  readonly rangeLabel = computed(() => {
    const r = this.report();
    if (!r) return '';
    const to = LONG_DAY.format(wall(r.to));
    return r.from === r.to ? `Hoy, ${to}` : `Del ${LONG_DAY.format(wall(r.from))} al ${to}`;
  });

  readonly maxSales = computed(() => Math.max(0, ...(this.report()?.timeline ?? []).map((b) => b.sales)));

  readonly bars = computed<Bar[]>(() => {
    const r = this.report();
    if (!r) return [];
    const max = this.maxSales() || 1;
    const n = r.timeline.length;
    // Thin out axis labels so they never collide on a phone, counting back from the latest:
    // every day of a week, one per week (same weekday) over 30 days, ~5 over the hours
    const every = n <= 7 ? 1 : r.granularity === 'DAY' ? 7 : Math.ceil(n / 5);
    return r.timeline.map((b, i) => {
      const at = wall(b.start);
      const hourly = r.granularity === 'HOUR';
      return {
        key: b.start,
        label: hourly ? HOUR.format(at) : `${WEEKDAY.format(at).replace('.', '')} ${at.getUTCDate()}`,
        tip: hourly ? `${HOUR.format(at)} – ${HOUR.format(new Date(at.getTime() + 3_600_000))}` : LONG_DAY.format(at),
        sales: b.sales,
        bills: b.bills,
        height: (b.sales / max) * 100,
        showLabel: (n - 1 - i) % every === 0
      };
    });
  });

  readonly hoveredBar = computed(() => this.bars().find((b) => b.key === this.hovered()) ?? null);

  ngOnInit() {
    this.load();
    // A bill closed elsewhere: today's numbers move, so refresh them
    merge(this.stream.floor$, this.stream.opened$)
      .pipe(debounceTime(800), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.period() === 'TODAY') this.load();
      });
  }

  select(period: ReportPeriod) {
    if (period === this.period()) return;
    this.period.set(period);
    this.hovered.set(null);
    this.load();
  }

  load() {
    const period = this.period();
    this.loading.set(true);
    this.error.set(null);
    this.api.salesReport(period).subscribe({
      next: (r) => {
        if (period !== this.period()) return;
        this.report.set(r);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(errorMessage(e));
      }
    });
  }

  /** "+25 %" against the previous period; null when there is nothing to compare with. */
  change(current: number, previous: number): { text: string; dir: 'up' | 'down' | 'flat' } | null {
    if (previous <= 0) return null;
    const pct = Math.round(((current - previous) / previous) * 100);
    const dir = pct > 0 ? 'up' : pct < 0 ? 'down' : 'flat';
    return { text: `${pct > 0 ? '+' : pct < 0 ? '−' : ''}${Math.abs(pct)} %`, dir };
  }

  share(value: number, total: number): number {
    return total > 0 ? Math.max(2, (value / total) * 100) : 0;
  }

  percent(value: number, total: number): string {
    return total > 0 ? `${Math.round((value / total) * 100)} %` : '—';
  }

  topSales(list: Breakdown[]): number {
    return list[0]?.sales ?? 0;
  }

  minutes(value: number | null): string {
    if (value === null) return '—';
    return value < 1 ? '< 1 min' : `${Math.round(value)} min`;
  }
}
