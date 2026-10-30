import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideAngularModule, Bike, Crosshair, Minus, Plus, Search, ShoppingBag, Trash2, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { DeliveryInfo, DeliveryQuote, Dish, Menu } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { addToCart, cartCount, cartTotal, CartLine, describe, lineTotal, toOrderLine } from '../../shared/cart';
import { price } from '../../shared/format';
import { LatLng, PinMapComponent } from '../../shared/pin-map.component';
import { WordmarkComponent } from '../../shared/wordmark.component';
import { DishSheetComponent } from '../menu/dish-sheet.component';
import { MenuBrowserComponent } from '../menu/menu-browser.component';
import { rememberDelivery } from './delivery-history';

/**
 * Ordering for delivery, without an account. The customer finds their address on the map and
 * drags the pin to their door; the API checks that point is a real address inside the zone,
 * and a waiter confirms the order (calling if in doubt) before the kitchen sees it.
 */
@Component({
  selector: 'app-delivery-order',
  imports: [FormsModule, RouterLink, LucideAngularModule, WordmarkComponent, MenuBrowserComponent, DishSheetComponent, PinMapComponent],
  template: `
    <header class="top">
      <a routerLink="/" aria-label="Inicio"><app-wordmark size="sm" /></a>
      <span class="what label"><lucide-icon [img]="Bike" [size]="18" /> Domicilio</span>
    </header>

    @if (info() && !info()!.open) {
      <main class="gate">
        <h1 class="display">Ahora no estamos tomando domicilios</h1>
        <p>Vuelve más tarde o escríbenos.</p>
        @if (tenants.whatsappLink('Hola, quiero pedir a domicilio'); as wa) {
          <a class="btn btn-brand" [href]="wa" target="_blank" rel="noopener">Escribir por WhatsApp</a>
        }
      </main>
    } @else if (menu() && info()) {
      @if (toast(); as msg) { <div class="toast" role="status">{{ msg }}</div> }
      <p class="intro">
        {{ info()!.fee ? 'Domicilio ' + price(info()!.fee) : 'Domicilio gratis' }} · hasta {{ km(info()!.radiusKm * 1000) }}
        @if (info()!.minOrder) { · pedido mínimo {{ price(info()!.minOrder) }} }
      </p>
      <main class="wrap" [class.with-bar]="lines().length">
        <app-menu-browser [menu]="menu()!" [orderable]="true" stickyTop="60px" (open)="selected.set($event)" />
      </main>

      @if (selected(); as d) {
        <app-dish-sheet [dish]="d" [tags]="menu()!.tags" [orderable]="true" actionLabel="Agregar" (close)="selected.set(null)" (add)="add($event)" />
      }

      @if (lines().length && !checkout()) {
        <button type="button" class="cart-bar btn btn-brand" (click)="checkout.set(true)">
          <lucide-icon [img]="ShoppingBag" [size]="20" aria-hidden="true" />
          <span>Pedir · {{ count() }}</span>
          <span class="tabular">{{ price(total()) }}</span>
        </button>
      }

      @if (checkout()) {
        <div class="scrim" (click)="checkout.set(false)">
          <section class="sheet checkout" role="dialog" aria-modal="true" aria-labelledby="co-title" (click)="$event.stopPropagation()">
            <header class="sheet-head">
              <h2 id="co-title" class="display">Tu domicilio</h2>
              <button type="button" class="icon-btn" (click)="checkout.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
            </header>

            <div class="body">
              <ul class="lines">
                @for (l of lines(); track l.key) {
                  <li>
                    <div class="line-main">
                      <span class="line-name">{{ l.dish.name }}</span>
                      @if (describe(l); as d) { <span class="line-desc">{{ d }}</span> }
                    </div>
                    <div class="line-side">
                      <span class="tabular">{{ price(lineTotal(l)) }}</span>
                      <div class="stepper">
                        <button type="button" class="icon-btn sm" (click)="changeQty(l, -1)" [attr.aria-label]="l.qty === 1 ? 'Quitar' : 'Uno menos'"><lucide-icon [img]="l.qty === 1 ? Trash2 : Minus" [size]="16" /></button>
                        <span class="tabular qty">{{ l.qty }}</span>
                        <button type="button" class="icon-btn sm" (click)="changeQty(l, 1)" [disabled]="l.qty >= 20" aria-label="Uno más"><lucide-icon [img]="Plus" [size]="16" /></button>
                      </div>
                    </div>
                  </li>
                }
              </ul>

              <h3 class="step label">1 · ¿A dónde lo llevamos?</h3>
              <form class="search" (ngSubmit)="search()">
                <input class="field" name="address" [(ngModel)]="address" placeholder="Ej: Calle 5B # 36-20" maxlength="160" autocomplete="street-address" aria-label="Dirección" />
                <button type="submit" class="btn btn-ghost btn-sm" [disabled]="searching() || address.trim().length < 5" aria-label="Buscar en el mapa"><lucide-icon [img]="Search" [size]="18" /></button>
              </form>
              <button type="button" class="link gps" (click)="useGps()" [disabled]="searching()"><lucide-icon [img]="Crosshair" [size]="16" /> Usar mi ubicación actual</button>

              @if (results().length > 1 && !pin()) {
                <ul class="results" aria-label="Lugares encontrados">
                  @for (r of results(); track $index) {
                    <li><button type="button" class="result" [disabled]="!r.inZone" (click)="choose(r)">
                      <span>{{ r.label }}</span>
                      <small>{{ r.inZone ? km(r.distanceMeters) : 'Fuera de la zona' }}</small>
                    </button></li>
                  }
                </ul>
              }
              @if (locateError()) { <p class="error-text" role="alert">{{ locateError() }}</p> }

              @if (quote() || results().length) {
                <app-pin-map class="map" [pin]="pin()" [restaurant]="{ lat: info()!.lat!, lng: info()!.lng! }" [radiusKm]="info()!.radiusKm" (moved)="moveTo($event)" />
              }
              @if (quote(); as q) {
                <p class="found" [class.out]="!q.inZone">
                  @if (q.inZone) { El mapa dice: <strong>{{ q.label }}</strong> · {{ km(q.distanceMeters) }} }
                  @else { <strong>{{ q.label }}</strong> queda fuera de nuestra zona ({{ km(q.distanceMeters) }}). }
                </p>
                <p class="hint">Si el marcador no está en tu puerta, arrástralo o toca el mapa donde es.</p>
              }
              <input class="field" name="detail" [(ngModel)]="detail" placeholder="Apto, torre, casa, cómo encontrarte (opcional)" maxlength="120" aria-label="Detalles de la dirección" />

              <h3 class="step label">2 · Tus datos</h3>
              <input class="field" name="name" [(ngModel)]="name" placeholder="Tu nombre" maxlength="80" autocomplete="name" aria-label="Nombre" />
              <input class="field" name="phone" [(ngModel)]="phone" placeholder="Celular, ej: 300 123 4567" maxlength="20" inputmode="tel" autocomplete="tel" aria-label="Celular" />

              <h3 class="step label">3 · ¿Cómo pagas?</h3>
              <div class="methods" role="radiogroup" aria-label="Método de pago">
                @for (m of info()!.paymentMethods; track m.id) {
                  <button type="button" class="chip" role="radio" [attr.aria-checked]="methodId() === m.id" [class.on]="methodId() === m.id" (click)="methodId.set(m.id)">{{ m.name }}</button>
                }
              </div>
              @if (isCash()) {
                <input class="field" type="number" min="0" step="1000" name="cashFor" [(ngModel)]="cashFor" placeholder="¿Con cuánto pagas? (para llevarte las vueltas)" aria-label="Con cuánto pagas" />
              }
            </div>

            <footer class="sheet-foot">
              <dl class="money">
                <div><dt>Productos</dt><dd class="tabular">{{ price(total()) }}</dd></div>
                <div><dt>Domicilio</dt><dd class="tabular">{{ info()!.fee ? price(info()!.fee) : 'Gratis' }}</dd></div>
                <div class="grand"><dt>Total</dt><dd class="tabular">{{ price(total() + info()!.fee) }}</dd></div>
              </dl>
              <p class="foot-note">El restaurante revisa tu pedido y la dirección antes de prepararlo. Si hay dudas, te llamamos.</p>
              @if (missing(); as m) { <p class="missing">{{ m }}</p> }
              @if (sendError()) { <p class="error-text" role="alert">{{ sendError() }}</p> }
              <button type="button" class="btn btn-brand send" (click)="send()" [disabled]="sending() || !!missing()">
                {{ sending() ? 'Enviando…' : 'Pedir domicilio · ' + price(total() + info()!.fee) }}
              </button>
            </footer>
          </section>
        </div>
      }
    } @else if (fatal()) {
      <main class="gate"><h1 class="display">No pudimos abrir la carta</h1><p>{{ fatal() }}</p></main>
    } @else {
      <p class="empty">Cargando…</p>
    }
  `,
  styles: `
    :host { display: block; min-height: 100dvh; }
    .top { position: sticky; top: 0; z-index: 30; height: 60px; display: flex; align-items: center; gap: 0.8rem; padding: 0 1rem; background: var(--ground); border-bottom: 3px solid var(--brand); }
    .top a { display: inline-flex; text-decoration: none; }
    .what { display: inline-flex; align-items: center; gap: 0.35rem; padding-left: 0.8rem; border-left: 1px solid var(--line); color: var(--ink-muted); }
    .intro { margin: 0; padding: 0.7rem 1rem; text-align: center; color: var(--ink-muted); font-size: 0.92rem; background: var(--ground-sunken); }
    .wrap { max-width: 72rem; margin: 0 auto; padding-bottom: 2rem; }
    .wrap.with-bar { padding-bottom: 6.5rem; }
    .gate { max-width: 30rem; margin: 0 auto; padding: 4rem 1.25rem; display: grid; gap: 0.8rem; justify-items: start; }
    .gate h1 { font-size: 2rem; margin: 0; color: var(--brand); }
    .gate p { color: var(--ink-muted); margin: 0; }
    .cart-bar { position: fixed; z-index: 40; left: 1rem; right: 1rem; bottom: calc(1rem + env(safe-area-inset-bottom)); max-width: 34rem; margin: 0 auto; min-height: 58px; justify-content: space-between; box-shadow: 0 10px 30px rgb(0 0 0 / 0.5); }
    .cart-bar span:first-of-type { flex: 1; text-align: left; }
    .toast { position: fixed; z-index: 60; top: 72px; left: 50%; transform: translateX(-50%); max-width: calc(100% - 2rem); padding: 0.75rem 1.1rem; border-radius: 14px; background: var(--ink); color: var(--ground); font-weight: 700; box-shadow: 0 10px 30px rgb(0 0 0 / 0.5); }

    .checkout { max-height: 94dvh; }
    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.4rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .body { padding: 0 1.1rem 1rem; display: grid; gap: 0.6rem; }
    .lines { list-style: none; margin: 0; padding: 0; }
    .lines > li { display: flex; gap: 0.8rem; justify-content: space-between; padding: 0.7rem 0; border-bottom: 1px solid var(--line); }
    .line-main { display: flex; flex-direction: column; gap: 0.15rem; min-width: 0; }
    .line-name { font-family: var(--font-label); text-transform: uppercase; font-weight: 600; }
    .line-desc { color: var(--ink-muted); font-size: 0.88rem; }
    .line-side { display: flex; flex-direction: column; align-items: flex-end; gap: 0.3rem; }
    .stepper { display: flex; align-items: center; gap: 0.25rem; }
    .icon-btn.sm { width: 36px; height: 36px; }
    .qty { min-width: 1.4rem; text-align: center; font-weight: 700; }
    .step { margin: 1rem 0 0; font-size: 1.05rem; color: var(--brand); }
    .search { display: flex; gap: 0.4rem; }
    .search .field { flex: 1; min-width: 0; }
    .link { display: inline-flex; align-items: center; gap: 0.3rem; background: none; border: 0; padding: 0; min-height: 40px; color: var(--brand); font: inherit; font-weight: 700; justify-self: start; }
    .results { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.3rem; }
    .result { width: 100%; display: flex; justify-content: space-between; gap: 0.8rem; text-align: left; min-height: 48px; padding: 0.5rem 0.8rem; border-radius: 12px; border: 1px solid var(--line); background: var(--ground-sunken); color: var(--ink); font: inherit; }
    .result small { color: var(--ink-faint); white-space: nowrap; }
    .result:disabled { opacity: 0.5; }
    .map { height: 16rem; }
    .found { margin: 0; font-size: 0.93rem; color: var(--ink-muted); }
    .found.out { color: var(--danger); }
    .hint { margin: 0; font-size: 0.85rem; color: var(--ink-faint); }
    .methods { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .sheet-foot { position: sticky; bottom: 0; padding: 0.8rem 1.1rem 1.1rem; background: var(--ground-raised); border-top: 1px solid var(--line); display: grid; gap: 0.5rem; }
    .money { margin: 0; display: grid; gap: 0.15rem; font-size: 0.93rem; }
    .money div { display: flex; justify-content: space-between; }
    .money dt { color: var(--ink-faint); }
    .money dd { margin: 0; }
    .grand dd { font-family: var(--font-label); font-weight: 600; font-size: 1.2rem; color: var(--brand); }
    .foot-note { margin: 0; color: var(--ink-faint); font-size: 0.85rem; }
    .missing { margin: 0; color: var(--warn); font-size: 0.9rem; font-weight: 700; }
    .send { width: 100%; min-height: 54px; }
  `
})
export default class DeliveryOrderComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  readonly tenants = inject(TenantService);

  readonly menu = signal<Menu | null>(null);
  readonly info = signal<DeliveryInfo | null>(null);
  readonly fatal = signal<string | null>(null);
  readonly lines = signal<CartLine[]>([]);
  readonly selected = signal<Dish | null>(null);
  readonly checkout = signal(false);
  readonly toast = signal<string | null>(null);

  readonly results = signal<DeliveryQuote[]>([]);
  /** What the map says is at the pin, checked by the API */
  readonly quote = signal<DeliveryQuote | null>(null);
  readonly pin = signal<LatLng | null>(null);
  readonly searching = signal(false);
  readonly locateError = signal<string | null>(null);
  readonly methodId = signal<number | null>(null);
  readonly sending = signal(false);
  readonly sendError = signal<string | null>(null);

  address = '';
  detail = '';
  name = '';
  phone = '';
  cashFor: number | null = null;

  readonly price = price;
  readonly describe = describe;
  readonly lineTotal = lineTotal;
  readonly Bike = Bike;
  readonly ShoppingBag = ShoppingBag;
  readonly X = X;
  readonly Plus = Plus;
  readonly Minus = Minus;
  readonly Trash2 = Trash2;
  readonly Search = Search;
  readonly Crosshair = Crosshair;

  readonly count = computed(() => cartCount(this.lines()));
  readonly total = computed(() => cartTotal(this.lines()));
  readonly isCash = computed(() => /efectivo|cash/i.test(this.info()?.paymentMethods.find((m) => m.id === this.methodId())?.name ?? ''));

  private toastTimer: ReturnType<typeof setTimeout> | null = null;

  ngOnInit() {
    this.api.deliveryInfo().subscribe({ next: (i) => this.info.set(i), error: (e) => this.fatal.set(errorMessage(e)) });
    this.api.menu().subscribe({ next: (m) => this.menu.set(m), error: (e) => this.fatal.set(errorMessage(e)) });
  }

  /** What is still missing to order, in the order the form asks it. */
  missing(): string | null {
    const info = this.info();
    if (info && this.total() < info.minOrder) return `El pedido mínimo es ${price(info.minOrder)}.`;
    if (!this.quote()) return 'Busca tu dirección y ubica el marcador en tu puerta.';
    if (!this.quote()!.inZone) return 'Esa dirección queda fuera de la zona de domicilios.';
    if (this.address.trim().length < 5) return 'Escribe tu dirección (calle y número).';
    if (!this.name.trim()) return 'Escribe tu nombre.';
    if (this.phone.replace(/\D/g, '').length < 10) return 'Escribe tu celular.';
    if (!this.methodId()) return 'Escoge cómo pagas.';
    return null;
  }

  km(meters: number): string {
    return meters < 1000 ? `${Math.round(meters)} m` : `${(meters / 1000).toFixed(1).replace('.', ',')} km`;
  }

  add(line: CartLine) {
    this.lines.update((ls) => addToCart(ls, line));
    this.selected.set(null);
    this.showToast(`${line.qty} × ${line.dish.name} agregado`);
  }

  changeQty(line: CartLine, delta: number) {
    this.lines.update((ls) => ls.map((l) => (l.key === line.key ? { ...l, qty: l.qty + delta } : l)).filter((l) => l.qty > 0));
    if (!this.lines().length) this.checkout.set(false);
  }

  search() {
    this.locate(this.api.searchAddress(this.address.trim()), true);
  }

  useGps() {
    if (!('geolocation' in navigator)) {
      this.locateError.set('Tu navegador no comparte la ubicación. Escribe la dirección.');
      return;
    }
    this.searching.set(true);
    this.locateError.set(null);
    navigator.geolocation.getCurrentPosition(
      (pos) => this.moveTo({ lat: +pos.coords.latitude.toFixed(6), lng: +pos.coords.longitude.toFixed(6) }),
      () => {
        this.searching.set(false);
        this.locateError.set('No pudimos leer tu ubicación. Revisa el permiso o escribe la dirección.');
      },
      { enableHighAccuracy: true, timeout: 15000 }
    );
  }

  choose(r: DeliveryQuote) {
    this.results.set([r]);
    this.moveTo({ lat: r.lat, lng: r.lng });
  }

  /** The pin moved (search result, GPS, drag or tap): ask the map what is there. */
  moveTo(p: LatLng) {
    this.pin.set(p);
    this.locate(this.api.placeAt(p.lat, p.lng), false);
  }

  send() {
    const q = this.quote();
    const method = this.methodId();
    if (!q || !method || this.missing()) return;
    this.sending.set(true);
    this.sendError.set(null);
    this.api.orderDelivery({
      customerName: this.name.trim(),
      phone: this.phone.trim(),
      address: this.address.trim(),
      addressDetail: this.detail.trim() || null,
      lat: q.lat,
      lng: q.lng,
      paymentMethodId: method,
      cashFor: this.isCash() && this.cashFor ? Number(this.cashFor) : null,
      lines: this.lines().map(toOrderLine)
    }).subscribe({
      next: (track) => {
        rememberDelivery(track.token);
        this.router.navigate(['/domicilio', track.token]);
      },
      error: (e) => {
        this.sending.set(false);
        this.sendError.set(errorMessage(e));
      }
    });
  }

  private locate(call: ReturnType<ApiService['searchAddress']>, fromText: boolean) {
    this.searching.set(true);
    this.locateError.set(null);
    call.subscribe({
      next: (found) => {
        this.searching.set(false);
        if (fromText) {
          this.results.set(found);
          this.quote.set(null);
          this.pin.set(null);
          const first = found.find((r) => r.inZone);
          if (!found.length) this.locateError.set('No encontramos esa dirección. Revísala o usa tu ubicación y mueve el marcador.');
          else if (!first) this.locateError.set('Esa dirección queda fuera de nuestra zona de domicilios.');
          else if (found.length === 1 || found.filter((r) => r.inZone).length === 1) this.choose(first);
        } else {
          this.quote.set(found[0] ?? null);
          // The map gave a street but the customer has not written one yet: suggest it
          if (found[0]?.road && !this.address.trim()) this.address = found[0].road;
        }
      },
      error: (e) => {
        this.searching.set(false);
        if (!fromText) this.quote.set(null);
        this.locateError.set(errorMessage(e));
      }
    });
  }

  private showToast(message: string) {
    this.toast.set(message);
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => this.toast.set(null), 2600);
  }
}
