import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Bike, Check, MapPin, Phone, TriangleAlert, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Delivery, DeliveryStatus, PaymentMethod } from '../../../core/api/models';
import { elapsed, minutesSince, price } from '../../../shared/format';
import { TicketItemsComponent } from '../ticket-items.component';
import { FloorStore } from './floor-store.service';

const STATUS: Record<DeliveryStatus, string> = {
  PENDING: 'Por confirmar',
  CONFIRMED: 'En cocina',
  PREPARING: 'Preparando',
  READY: 'Listo para despachar',
  ON_THE_WAY: 'En camino',
  DELIVERED: 'Entregado',
  CANCELLED: 'Rechazado'
};

/**
 * Deliveries from the website. The waiter checks each one before the kitchen sees it: the
 * address the customer wrote next to what the map found at their pin, and calls if in doubt.
 * Then: dispatch it with the courier, and charge it when the courier is back.
 */
@Component({
  selector: 'app-deliveries',
  imports: [FormsModule, LucideAngularModule, TicketItemsComponent],
  template: `
    <main class="wrap">
      <h1 class="title display"><lucide-icon [img]="Bike" [size]="26" /> Domicilios</h1>
      @if (actionError()) { <p class="error-text banner" role="alert">{{ actionError() }}</p> }

      <div class="list">
        @for (d of store.deliveries(); track d.id) {
          <article class="card" [attr.data-status]="d.status">
            <header>
              <span class="num display">#{{ d.id }}</span>
              <span class="status label">{{ statusLabel(d.status) }}</span>
              <span class="age">{{ age(d.createdAt) }}</span>
            </header>

            <div class="who">
              <strong>{{ d.customerName }}</strong>
              <span class="contact">
                <a class="btn btn-ghost btn-sm" [href]="'tel:+57' + d.phone"><lucide-icon [img]="Phone" [size]="15" /> {{ phoneLabel(d.phone) }}</a>
                <a class="btn btn-ghost btn-sm" [href]="'https://wa.me/57' + d.phone" target="_blank" rel="noopener">WhatsApp</a>
              </span>
            </div>

            <div class="where">
              <p class="written"><span class="k">Escribió</span> {{ d.address }}@if (d.addressDetail) { · {{ d.addressDetail }} }</p>
              <p class="map" [class.mismatch]="!d.matchesAddress">
                <span class="k">El mapa dice</span> {{ d.placeLabel }}
                <span class="dist">· {{ km(d.distanceMeters) }}</span>
              </p>
              @if (!d.matchesAddress) {
                <p class="warn"><lucide-icon [img]="TriangleAlert" [size]="15" /> La calle del punto en el mapa no coincide con la dirección escrita. Llama para confirmar antes de enviarlo a cocina.</p>
              }
              <a class="maplink" [href]="'https://www.google.com/maps?q=' + d.lat + ',' + d.lng" target="_blank" rel="noopener">
                <lucide-icon [img]="MapPin" [size]="15" /> Ver el punto en el mapa
              </a>
            </div>

            @for (t of d.tickets; track t.id) {
              @if (t.status !== 'CANCELLED') { <app-ticket-items [items]="t.items" /> }
            }

            <dl class="money">
              <div><dt>Productos</dt><dd class="tabular">{{ price(d.subtotal) }}</dd></div>
              <div><dt>Domicilio</dt><dd class="tabular">{{ d.fee ? price(d.fee) : 'Gratis' }}</dd></div>
              <div class="total"><dt>Total</dt><dd class="tabular">{{ price(d.total) }}</dd></div>
              <div><dt>Paga con</dt><dd>{{ d.paymentMethodName }}@if (d.cashFor) { · billete de {{ price(d.cashFor) }}, vueltas {{ price(d.cashFor - d.total) }} }</dd></div>
            </dl>

            <footer>
              @switch (d.status) {
                @case ('PENDING') {
                  <button type="button" class="btn btn-sm btn-danger" (click)="reject(d)" [disabled]="busy() === d.id">
                    <lucide-icon [img]="X" [size]="16" /> Rechazar
                  </button>
                  <button type="button" class="btn btn-brand grow" (click)="confirm(d)" [disabled]="busy() === d.id">
                    <lucide-icon [img]="Check" [size]="18" /> Confirmar y a cocina
                  </button>
                }
                @case ('READY') {
                  <button type="button" class="btn btn-ok grow" (click)="dispatch(d)" [disabled]="busy() === d.id">
                    <lucide-icon [img]="Bike" [size]="18" /> Despachar con el domiciliario
                  </button>
                }
                @case ('ON_THE_WAY') {
                  <select class="field method" [ngModel]="methodOf[d.id] ?? d.paymentMethodId" (ngModelChange)="methodOf[d.id] = $event" [attr.aria-label]="'Cómo pagó el domicilio ' + d.id">
                    @for (m of methods(); track m.id) { <option [ngValue]="m.id">{{ m.name }}</option> }
                  </select>
                  <button type="button" class="btn btn-ok grow" (click)="charge(d)" [disabled]="busy() === d.id">
                    Entregado y cobrado · {{ price(d.total) }}
                  </button>
                }
                @default { <span class="waiting">En cocina</span> }
              }
            </footer>
          </article>
        } @empty {
          <p class="empty">No hay domicilios pendientes. Los pedidos de la página llegan aquí para que los confirmes.</p>
        }
      </div>
    </main>
  `,
  styles: `
    .wrap { max-width: 76rem; margin: 0 auto; padding: 1.1rem 0.9rem 4rem; }
    .title { display: flex; align-items: center; gap: 0.5rem; margin: 0.3rem 0 1rem; font-size: 1.8rem; }
    .banner { margin: 0 0 1rem; }
    .list { display: grid; gap: 0.9rem; grid-template-columns: repeat(auto-fill, minmax(20rem, 1fr)); align-items: start; }
    .card { border-radius: 18px; padding: 0.95rem 1rem; background: var(--ground-raised); border: 2px solid var(--line); display: flex; flex-direction: column; gap: 0.75rem; min-width: 0; }
    .card[data-status="PENDING"] { border-color: var(--warn); }
    .card[data-status="READY"] { border-color: var(--ok); }
    .card header { display: flex; align-items: baseline; gap: 0.6rem; }
    .num { font-size: 1.5rem; }
    .status { color: var(--ink-muted); }
    .card[data-status="PENDING"] .status { color: var(--warn); }
    .card[data-status="READY"] .status { color: var(--ok); }
    .age { margin-left: auto; color: var(--ink-faint); font-size: 0.88rem; }
    .who { display: flex; flex-direction: column; gap: 0.4rem; }
    .who strong { font-size: 1.1rem; }
    .contact { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .where p { margin: 0 0 0.3rem; overflow-wrap: anywhere; }
    .k { display: block; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--ink-faint); }
    .map { color: var(--ink-muted); }
    .map.mismatch { color: var(--warn); }
    .dist { color: var(--ink-faint); }
    .warn { display: flex; gap: 0.4rem; align-items: flex-start; padding: 0.6rem 0.7rem; border-radius: 12px; background: color-mix(in oklab, var(--warn) 16%, transparent); font-size: 0.9rem; }
    .maplink { display: inline-flex; align-items: center; gap: 0.3rem; min-height: 40px; color: var(--brand); font-weight: 700; }
    .money { margin: 0; display: grid; gap: 0.2rem; font-size: 0.93rem; }
    .money div { display: flex; justify-content: space-between; gap: 1rem; }
    .money dt { color: var(--ink-faint); }
    .money dd { margin: 0; text-align: right; }
    .money .total dd { font-family: var(--font-label); font-weight: 600; color: var(--brand); font-size: 1.1rem; }
    .card footer { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: center; }
    .grow { flex: 1; }
    .method { width: auto; min-width: 8rem; flex: 1; }
    .waiting { color: var(--ink-muted); font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.05em; }
  `
})
export default class DeliveriesComponent implements OnInit, OnDestroy {
  readonly store = inject(FloorStore);
  private api = inject(ApiService);

  readonly busy = signal<number | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly methods = signal<PaymentMethod[]>([]);
  readonly now = signal(Date.now());
  /** delivery id -> method the waiter will charge (starts as the one the customer chose) */
  methodOf: Record<number, number | null> = {};

  readonly price = price;
  readonly Bike = Bike;
  readonly Check = Check;
  readonly X = X;
  readonly Phone = Phone;
  readonly MapPin = MapPin;
  readonly TriangleAlert = TriangleAlert;

  private clock = setInterval(() => this.now.set(Date.now()), 30_000);

  ngOnInit() {
    this.store.start();
    this.store.loadDeliveries();
    this.api.activePaymentMethods().subscribe({ next: (m) => this.methods.set(m) });
  }

  ngOnDestroy() {
    clearInterval(this.clock);
  }

  statusLabel(s: DeliveryStatus): string {
    return STATUS[s];
  }

  age(instant: string): string {
    return elapsed(minutesSince(instant, this.now()));
  }

  km(meters: number): string {
    return meters < 1000 ? `${meters} m` : `${(meters / 1000).toFixed(1).replace('.', ',')} km`;
  }

  phoneLabel(p: string): string {
    return `${p.slice(0, 3)} ${p.slice(3, 6)} ${p.slice(6)}`;
  }

  confirm(d: Delivery) {
    const t = this.current(d);
    if (t) this.run(d, this.store.approve(t.id));
  }

  reject(d: Delivery) {
    const t = this.current(d);
    if (t) this.run(d, this.store.cancel(t.id, 'Rechazado por el restaurante'));
  }

  dispatch(d: Delivery) {
    const t = this.current(d);
    if (t) this.run(d, this.store.deliver(t.id));
  }

  charge(d: Delivery) {
    const method = this.methodOf[d.id] ?? d.paymentMethodId ?? this.methods()[0]?.id;
    if (!method) {
      this.actionError.set('No hay métodos de pago activos.');
      return;
    }
    this.run(d, this.store.chargeDelivery(d, method));
  }

  private current(d: Delivery) {
    return d.tickets.filter((t) => t.status !== 'CANCELLED').at(-1) ?? d.tickets.at(-1);
  }

  private run(d: Delivery, action: ReturnType<FloorStore['chargeDelivery']>) {
    this.busy.set(d.id);
    this.actionError.set(null);
    action.subscribe({
      next: () => {
        this.busy.set(null);
        this.store.loadDeliveries();
      },
      error: (e) => {
        this.busy.set(null);
        this.actionError.set(errorMessage(e));
        this.store.loadDeliveries();
      }
    });
  }
}
