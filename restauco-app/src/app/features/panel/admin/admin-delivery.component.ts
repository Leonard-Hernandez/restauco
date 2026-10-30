import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { DeliverySettings, PaymentMethod } from '../../../core/api/models';

/** The owner sets whether the restaurant delivers, from where, how far, the fee and the minimum. */
@Component({
  selector: 'app-admin-delivery',
  imports: [FormsModule],
  template: `
    <main class="wrap">
      <header class="page-head"><h1 class="display">Domicilios</h1></header>
      <p class="page-lead">Los clientes piden desde la página; cada pedido llega al mesero para que revise la dirección y lo confirme antes de pasar a cocina.</p>
      @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }
      @if (saved()) { <p class="ok-text" role="status">Guardado.</p> }

      @if (form(); as f) {
        <form class="form" (ngSubmit)="save()">
          <label class="row-check">
            <input type="checkbox" name="enabled" [(ngModel)]="f.enabled" /> Recibir pedidos a domicilio
          </label>

          <fieldset>
            <legend class="label">Dónde está el restaurante</legend>
            <div class="pair">
              <label>Latitud <input class="field" type="number" step="0.00001" name="lat" [(ngModel)]="f.lat" /></label>
              <label>Longitud <input class="field" type="number" step="0.00001" name="lng" [(ngModel)]="f.lng" /></label>
            </div>
            <button type="button" class="btn btn-ghost btn-sm" (click)="here()" [disabled]="locating()">
              {{ locating() ? 'Buscando…' : 'Estoy en el restaurante: usar mi ubicación' }}
            </button>
            @if (f.lat !== null && f.lng !== null) {
              <a class="maplink" [href]="'https://www.google.com/maps?q=' + f.lat + ',' + f.lng" target="_blank" rel="noopener">Ver el punto en el mapa</a>
            }
          </fieldset>

          <label>Hasta qué distancia llevan (km, en línea recta)
            <input class="field" type="number" min="0.5" max="30" step="0.5" name="radiusKm" [(ngModel)]="f.radiusKm" required />
          </label>
          <label>Valor del domicilio (0 = gratis)
            <input class="field" type="number" min="0" step="500" name="fee" [(ngModel)]="f.fee" required />
          </label>
          <fieldset>
            <legend class="label">Cómo pueden pagar los domicilios</legend>
            @for (m of methods(); track m.id) {
              <label class="row-check light">
                <input type="checkbox" [name]="'pm' + m.id" [checked]="accepts(m.id)" (change)="toggleMethod(m.id)" /> {{ m.name }}
              </label>
            } @empty { <p class="hint">Primero crea los métodos de pago en Pagos.</p> }
            <small class="hint">Los clientes solo ven los que marques. Ejemplo: quita Efectivo si el domiciliario no lleva vueltas.</small>
          </fieldset>

          <label>Pedido mínimo (0 = sin mínimo)
            <input class="field" type="number" min="0" step="1000" name="minOrder" [(ngModel)]="f.minOrder" required />
          </label>

          <button type="submit" class="btn btn-brand" [disabled]="saving()">{{ saving() ? 'Guardando…' : 'Guardar' }}</button>
        </form>
      } @else if (!error()) {
        <p class="empty">Cargando…</p>
      }
    </main>
  `,
  styleUrl: './admin.css',
  styles: `
    .form { display: grid; gap: 1.1rem; max-width: 32rem; }
    .form label { display: grid; gap: 0.35rem; color: var(--ink-muted); }
    .row-check { display: flex !important; align-items: center; gap: 0.6rem; color: var(--ink) !important; font-weight: 700; }
    .row-check input { width: 22px; height: 22px; }
    fieldset { border: 1px solid var(--line); border-radius: 14px; padding: 0.9rem; display: grid; gap: 0.7rem; margin: 0; }
    legend { padding: 0 0.4rem; }
    .pair { display: grid; grid-template-columns: 1fr 1fr; gap: 0.6rem; }
    .maplink { color: var(--brand); font-weight: 700; }
    .ok-text { color: var(--ok); font-weight: 700; }
    .row-check.light { font-weight: 400; }
    .hint { margin: 0; color: var(--ink-faint); font-size: 0.85rem; }
  `
})
export default class AdminDeliveryComponent implements OnInit {
  private api = inject(ApiService);

  readonly form = signal<DeliverySettings | null>(null);
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly saved = signal(false);
  readonly locating = signal(false);
  /** Active payment methods the owner can offer for delivery */
  readonly methods = signal<PaymentMethod[]>([]);
  /** Methods ticked while editing; null until the owner touches one (the saved settings rule until then) */
  private readonly picked = signal<number[] | null>(null);

  ngOnInit() {
    this.api.deliverySettings().subscribe({
      next: (s) => this.form.set({ ...s, radiusKm: s.radiusKm || 3 }),
      error: (e) => this.error.set(errorMessage(e))
    });
    this.api.paymentMethods().subscribe({ next: (m) => this.methods.set(m.filter((x) => x.active)) });
  }

  /** Active methods ticked. Saved [] means every active method; unticking all leaves none, not "all" */
  private chosen(): number[] {
    const picked = this.picked();
    if (picked) return picked;
    const active = this.methods().map((m) => m.id);
    const ids = this.form()?.paymentMethodIds ?? [];
    return ids.length ? active.filter((id) => ids.includes(id)) : active;
  }

  accepts(id: number): boolean {
    return this.chosen().includes(id);
  }

  toggleMethod(id: number) {
    const current = this.chosen();
    this.picked.set(current.includes(id) ? current.filter((x) => x !== id) : [...current, id]);
  }

  here() {
    const f = this.form();
    if (!f || !('geolocation' in navigator)) return;
    this.locating.set(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.locating.set(false);
        this.form.set({ ...f, lat: +pos.coords.latitude.toFixed(6), lng: +pos.coords.longitude.toFixed(6) });
      },
      () => {
        this.locating.set(false);
        this.error.set('No pudimos leer tu ubicación. Revisa el permiso de ubicación del navegador.');
      },
      { enableHighAccuracy: true, timeout: 15000 }
    );
  }

  save() {
    const f = this.form();
    if (!f) return;
    const chosen = this.chosen();
    if (this.methods().length && !chosen.length) {
      this.error.set('Escoge al menos un método de pago para los domicilios.');
      return;
    }
    // Every active one ticked is saved as [] (all), so methods created later are offered too.
    // Methods not loaded (yet, or the request failed): keep the saved choice, never widen it to all
    const loaded = this.methods().length > 0;
    const all = loaded && this.methods().every((m) => chosen.includes(m.id));
    const paymentMethodIds = !loaded ? f.paymentMethodIds : all ? [] : chosen;
    this.saving.set(true);
    this.saved.set(false);
    this.error.set(null);
    this.api.saveDeliverySettings({ ...f, paymentMethodIds }).subscribe({
      next: (s) => {
        this.form.set(s);
        this.picked.set(null);
        this.saving.set(false);
        this.saved.set(true);
      },
      error: (e) => {
        this.saving.set(false);
        this.error.set(errorMessage(e));
      }
    });
  }
}
