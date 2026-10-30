import { Component, computed, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Bike, Check, Phone } from 'lucide-angular';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { DeliveryStatus, DeliveryTrack } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { price, time } from '../../shared/format';
import { WordmarkComponent } from '../../shared/wordmark.component';

const STEPS: { status: DeliveryStatus[]; label: string }[] = [
  { status: ['PENDING'], label: 'Recibido: el restaurante lo está revisando' },
  { status: ['CONFIRMED', 'PREPARING'], label: 'Confirmado, en la cocina' },
  { status: ['READY'], label: 'Listo, saliendo' },
  { status: ['ON_THE_WAY'], label: 'En camino' },
  { status: ['DELIVERED'], label: 'Entregado' }
];
const POLL_MS = 10_000;

/** The customer follows their delivery with the private link they got when ordering. */
@Component({
  selector: 'app-delivery-track',
  imports: [RouterLink, LucideAngularModule, WordmarkComponent],
  template: `
    <header class="top">
      <a routerLink="/" aria-label="Inicio"><app-wordmark size="sm" /></a>
      <span class="what label"><lucide-icon [img]="Bike" [size]="18" /> Tu domicilio</span>
    </header>
    <main class="wrap">
      @if (track(); as t) {
        @if (t.status === 'CANCELLED') {
          <h1 class="display headline cancelled">El restaurante no pudo aceptar tu pedido</h1>
          @if (t.cancelReason) { <p class="lead">{{ t.cancelReason }}</p> }
          <p class="lead">No se cobró nada. Si quieres, escríbenos o llámanos.</p>
        } @else {
          <h1 class="display headline">{{ headline() }}</h1>
          <ol class="steps">
            @for (s of steps; track $index) {
              <li [class.done]="$index < stepIndex()" [class.now]="$index === stepIndex()">
                <span class="dot">@if ($index < stepIndex()) { <lucide-icon [img]="Check" [size]="14" /> }</span>
                {{ s.label }}
              </li>
            }
          </ol>
        }

        <section class="card">
          <h2 class="label">Pedido · {{ time(t.createdAt) }}</h2>
          <ul class="items">
            @for (i of t.items; track $index) {
              <li><span>{{ i.qty }} × {{ i.name }}@if (i.modifiers.length) { <small> · {{ options(i.modifiers) }}</small> }@if (i.removed.length) { <small> · sin {{ i.removed.join(', ').toLowerCase() }}</small> }</span><span class="tabular">{{ price(i.lineTotal) }}</span></li>
            }
          </ul>
          <dl class="money">
            <div><dt>Productos</dt><dd class="tabular">{{ price(t.subtotal) }}</dd></div>
            <div><dt>Domicilio</dt><dd class="tabular">{{ t.fee ? price(t.fee) : 'Gratis' }}</dd></div>
            <div class="grand"><dt>Total</dt><dd class="tabular">{{ price(t.total) }}</dd></div>
          </dl>
          <p class="meta">Pagas con {{ t.paymentMethodName }}@if (t.cashFor) { · billete de {{ price(t.cashFor) }} }</p>
          <p class="meta">Para {{ t.customerName }} · {{ t.address }}@if (t.addressDetail) { · {{ t.addressDetail }} }</p>
        </section>

        @if (callLink(); as tel) {
          <a class="btn btn-ghost call" [href]="tel"><lucide-icon [img]="Phone" [size]="18" /> Llamar al restaurante</a>
        }
      } @else if (error()) {
        <h1 class="display headline">No encontramos ese pedido</h1>
        <p class="lead">{{ error() }}</p>
        <a routerLink="/domicilio" class="btn btn-brand">Hacer un pedido</a>
      } @else {
        <p class="empty">Cargando…</p>
      }
    </main>
  `,
  styles: `
    :host { display: block; min-height: 100dvh; }
    .top { position: sticky; top: 0; z-index: 30; height: 60px; display: flex; align-items: center; gap: 0.8rem; padding: 0 1rem; background: var(--ground); border-bottom: 3px solid var(--brand); }
    .top a { display: inline-flex; text-decoration: none; }
    .what { display: inline-flex; align-items: center; gap: 0.35rem; padding-left: 0.8rem; border-left: 1px solid var(--line); color: var(--ink-muted); }
    .wrap { max-width: 34rem; margin: 0 auto; padding: 1.4rem 1rem 4rem; display: grid; gap: 1.1rem; }
    .headline { margin: 0; font-size: 2rem; line-height: 1.1; color: var(--brand); }
    .headline.cancelled { color: var(--danger); }
    .lead { margin: 0; color: var(--ink-muted); }
    .steps { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.2rem; }
    .steps li { display: flex; align-items: center; gap: 0.7rem; min-height: 44px; color: var(--ink-faint); }
    .steps li.done { color: var(--ink-muted); }
    .steps li.now { color: var(--ink); font-weight: 800; }
    .dot { flex: none; width: 24px; height: 24px; border-radius: 999px; border: 2px solid var(--line); display: grid; place-items: center; }
    .done .dot { background: var(--ok); border-color: var(--ok); color: #04150c; }
    .now .dot { border-color: var(--brand); box-shadow: 0 0 0 4px color-mix(in oklab, var(--brand) 25%, transparent); }
    .card { border-radius: 18px; padding: 1rem; background: var(--ground-raised); border: 1px solid var(--line); display: grid; gap: 0.6rem; }
    .card h2 { margin: 0; font-size: 1rem; color: var(--ink-muted); }
    .items { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.35rem; }
    .items li { display: flex; justify-content: space-between; gap: 1rem; }
    .items small { color: var(--ink-faint); }
    .money { margin: 0; display: grid; gap: 0.15rem; font-size: 0.93rem; border-top: 1px solid var(--line); padding-top: 0.6rem; }
    .money div { display: flex; justify-content: space-between; }
    .money dt { color: var(--ink-faint); }
    .money dd { margin: 0; }
    .grand dd { font-family: var(--font-label); font-weight: 600; font-size: 1.2rem; color: var(--brand); }
    .meta { margin: 0; color: var(--ink-muted); font-size: 0.9rem; overflow-wrap: anywhere; }
    .call { justify-self: start; }
  `
})
export default class DeliveryTrackComponent implements OnInit, OnDestroy {
  readonly token = input.required<string>();

  private api = inject(ApiService);
  private tenants = inject(TenantService);

  readonly track = signal<DeliveryTrack | null>(null);
  readonly error = signal<string | null>(null);
  readonly steps = STEPS;
  readonly price = price;
  readonly time = time;
  readonly Bike = Bike;
  readonly Check = Check;
  readonly Phone = Phone;

  readonly stepIndex = computed(() => {
    const status = this.track()?.status;
    return status ? STEPS.findIndex((s) => s.status.includes(status)) : 0;
  });
  readonly headline = computed(() => ({
    PENDING: '¡Recibimos tu pedido!',
    CONFIRMED: 'Tu pedido está confirmado',
    PREPARING: 'Estamos preparando tu pedido',
    READY: 'Tu pedido está listo',
    ON_THE_WAY: 'Tu pedido va en camino',
    DELIVERED: '¡Buen provecho!',
    CANCELLED: ''
  })[this.track()?.status ?? 'PENDING']);
  readonly callLink = computed(() => {
    const phone = this.tenants.tenant()?.contactPhone;
    return phone ? `tel:${phone.replace(/[^\d+]/g, '')}` : null;
  });

  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit() {
    this.load();
    this.timer = setInterval(() => {
      const s = this.track()?.status;
      if (document.visibilityState === 'visible' && s !== 'DELIVERED' && s !== 'CANCELLED') this.load();
    }, POLL_MS);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  options(modifiers: { name: string }[]): string {
    return modifiers.map((m) => m.name).join(', ');
  }

  private load() {
    this.api.trackDelivery(this.token()).subscribe({
      next: (t) => {
        this.track.set(t);
        this.error.set(null);
      },
      error: (e) => {
        if (!this.track()) this.error.set(errorMessage(e));
      }
    });
  }
}
