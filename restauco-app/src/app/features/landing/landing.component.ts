import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, Bike, Clock, MapPin, MessageCircle, QrCode } from 'lucide-angular';
import { ApiService } from '../../core/api/api.service';
import { Menu } from '../../core/api/models';
import { TenantService } from '../../core/api/tenant.service';
import { DishPhotoComponent } from '../../shared/dish-photo.component';
import { phone, price } from '../../shared/format';
import { WordmarkComponent } from '../../shared/wordmark.component';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, LucideAngularModule, WordmarkComponent, DishPhotoComponent],
  template: `
    <div class="band top-band" aria-hidden="true"></div>
    <header class="hero">
      <div class="hero-inner">
        <h1 class="sr-only">{{ tenant()?.name }}</h1>
        <app-wordmark size="lg" />
        @if (b().tagline) { <p class="tagline label">{{ b().tagline }}</p> }
        <div class="flag-rule rule" aria-hidden="true"></div>
        <div class="ctas">
          <a routerLink="/menu" class="btn btn-brand">Ver la carta</a>
          @if (b().delivery) {
            <a routerLink="/domicilio" class="btn btn-ghost">
              <lucide-icon [img]="Bike" [size]="18" aria-hidden="true" /> Pedir a domicilio
            </a>
          } @else if (whatsapp()) {
            <a [href]="whatsapp()" class="btn btn-ghost" target="_blank" rel="noopener">
              <lucide-icon [img]="MessageCircle" [size]="18" aria-hidden="true" /> Pedir por WhatsApp
            </a>
          }
        </div>
        <ul class="facts">
          @if (b().hours) { <li><lucide-icon [img]="Clock" [size]="18" aria-hidden="true" /> {{ b().hours }}</li> }
          @if (tenant()?.address; as address) {
            <li><lucide-icon [img]="MapPin" [size]="18" aria-hidden="true" />
              @if (b().mapsUrl) { <a [href]="b().mapsUrl" target="_blank" rel="noopener">{{ address }}</a> } @else { {{ address }} }
            </li>
          }
          @if (b().delivery) { <li><lucide-icon [img]="Bike" [size]="18" aria-hidden="true" /> Servicio a domicilio: {{ phones() }}</li> }
        </ul>
      </div>
    </header>

    @if (house().length) {
      <section class="block">
        <div class="block-head">
          <h2 class="display">De la casa</h2>
          <a routerLink="/menu" class="more label">Toda la carta →</a>
        </div>
        <ul class="house">
          @for (d of house(); track d.id) {
            <li class="card house-card">
              <app-dish-photo class="house-photo" [url]="d.imageUrl" [alt]="d.name" [seed]="d.id" />
              <div class="house-body">
                <h3 class="label">{{ d.name }}</h3>
                @if (d.description) { <p>{{ d.description }}</p> }
                <span class="house-price tabular">{{ price(d.price) }}</span>
              </div>
            </li>
          }
        </ul>
      </section>
    }

    @if (build(); as bd) {
      <section class="band build">
        <div class="build-inner">
          <h2 class="display">{{ bd.title }}</h2>
          <ul class="sizes">
            @for (d of bd.dishes; track d.id) { <li><span>{{ d.name }}</span> <strong class="tabular">{{ price(d.price) }}</strong></li> }
          </ul>
          <p class="build-lead">Escoge entre:</p>
          <ul class="fillings">
            @for (f of bd.options; track f) { <li>{{ f }}</li> }
          </ul>
        </div>
      </section>
    }

    <section class="block qr">
      <lucide-icon [img]="QrCode" [size]="40" aria-hidden="true" />
      <div>
        <h2 class="display">¿Estás en el local?</h2>
        <p>Escanea el código QR de tu mesa para ver la carta y pedir sin esperar. El mesero confirma tu pedido y lo ves pasar a la cocina en tiempo real.</p>
        @if (b().paymentNote) { <p class="pay">{{ b().paymentNote }}.</p> }
      </div>
    </section>

    <footer class="foot">
      <div class="flag-rule" aria-hidden="true"></div>
      <div class="foot-inner">
        <span>{{ tenant()?.name }} · Cali</span>
        <a routerLink="/panel" class="staff">Equipo</a>
      </div>
    </footer>
  `,
  styles: `
    :host { display: block; }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
    .top-band { height: 22px; --band-size: 48px; }
    .hero { padding: clamp(2.5rem, 9vw, 5.5rem) 1.25rem clamp(2.5rem, 7vw, 4.5rem); background: radial-gradient(ellipse at 80% 0%, rgb(255 194 26 / 0.13), transparent 55%); }
    .hero-inner { max-width: 72rem; margin: 0 auto; }
    .tagline { margin: 1.1rem 0 0; font-size: clamp(1.1rem, 3.6vw, 1.5rem); color: var(--ink); }
    .rule { width: 9rem; margin: 1.2rem 0 1.6rem; }
    .ctas { display: flex; flex-wrap: wrap; gap: 0.7rem; }
    .facts { list-style: none; margin: 2rem 0 0; padding: 0; display: grid; gap: 0.6rem; color: var(--ink-muted); }
    .facts li { display: flex; align-items: center; gap: 0.6rem; }
    .facts lucide-icon { color: var(--brand); flex: none; }
    .facts a { color: inherit; }

    .block { max-width: 72rem; margin: 0 auto; padding: 2.6rem 1.25rem; }
    .block-head { display: flex; align-items: baseline; justify-content: space-between; gap: 1rem; margin-bottom: 1.1rem; }
    .block h2 { margin: 0; font-size: 2.1rem; color: var(--brand); }
    .more { color: var(--ink-muted); text-decoration: none; font-size: 0.95rem; }
    .more:hover { color: var(--brand); }
    .house { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.9rem; grid-template-columns: repeat(auto-fill, minmax(15rem, 1fr)); }
    .house-card { overflow: hidden; display: flex; flex-direction: column; }
    .house-photo { aspect-ratio: 4 / 3; }
    .house-body { padding: 0.9rem 1rem 1rem; display: flex; flex-direction: column; gap: 0.3rem; flex: 1; }
    .house-body h3 { margin: 0; font-size: 1.15rem; }
    .house-body p { margin: 0; color: var(--ink-muted); font-size: 0.93rem; }
    .house-price { margin-top: auto; font-family: var(--font-label); font-weight: 600; font-size: 1.15rem; color: var(--brand); }

    .build { padding: 3rem 1.25rem; }
    .build-inner { max-width: 72rem; margin: 0 auto; }
    .build h2 { margin: 0; font-size: clamp(2.2rem, 7vw, 3.2rem); }
    .build-lead { margin: 0.4rem 0 1.2rem; font-weight: 700; }
    .fillings { list-style: none; margin: 0 0 1.6rem; padding: 0; display: flex; flex-wrap: wrap; gap: 0.5rem; }
    .fillings li { padding: 0.4rem 0.85rem; border-radius: 999px; background: var(--brand-ink); color: var(--brand); font-family: var(--font-label); text-transform: uppercase; font-size: 0.92rem; letter-spacing: 0.03em; }
    .sizes { list-style: none; margin: 1rem 0 1.4rem; padding: 0; display: grid; gap: 0.35rem; max-width: 26rem; }
    .sizes li { display: flex; justify-content: space-between; gap: 1rem; padding-bottom: 0.35rem; border-bottom: 2px dotted rgb(22 16 5 / 0.35); font-family: var(--font-label); text-transform: uppercase; font-size: 1.1rem; }

    .qr { display: flex; gap: 1.1rem; align-items: flex-start; }
    .qr lucide-icon { color: var(--brand); flex: none; margin-top: 0.3rem; }
    .qr h2 { font-size: 1.7rem; color: var(--ink); }
    .qr p { margin: 0.5rem 0 0; color: var(--ink-muted); max-width: 40rem; }
    .qr .pay { color: var(--brand); font-weight: 700; }

    .foot-inner { max-width: 72rem; margin: 0 auto; padding: 1.4rem 1.25rem 2rem; display: flex; justify-content: space-between; color: var(--ink-faint); font-size: 0.9rem; }
    .staff { color: var(--ink-faint); }
  `
})
export default class LandingComponent implements OnInit {
  private api = inject(ApiService);
  private tenants = inject(TenantService);

  readonly tenant = this.tenants.tenant;
  readonly menu = signal<Menu | null>(null);
  readonly price = price;
  readonly MessageCircle = MessageCircle;
  readonly Clock = Clock;
  readonly MapPin = MapPin;
  readonly Bike = Bike;
  readonly QrCode = QrCode;

  readonly b = computed(() => this.tenant()?.branding ?? {});
  readonly phones = computed(() => (this.b().phones ?? []).map((p) => phone(p)).join(' · '));
  readonly whatsapp = computed(() => this.tenants.whatsappLink('Hola, quiero hacer un pedido a domicilio'));

  /** Dishes of the second category (the house specialties) or the first ones with a description. */
  readonly house = computed(() => {
    const m = this.menu();
    if (!m) return [];
    const withoutChoices = m.dishes.filter((d) => d.modifiers.length === 0 && d.description && d.price >= 10000);
    return withoutChoices.slice(0, 3);
  });

  /** The build-your-own category (dishes sharing a required choice), if the menu has one. */
  readonly build = computed(() => {
    const m = this.menu();
    const first = m?.dishes.find((d) => d.modifiers.some((g) => g.min > 0));
    if (!m || !first) return null;
    const group = first.modifiers.find((g) => g.min > 0)!;
    return {
      title: m.categories.find((c) => c.id === first.categoryId)?.name ?? first.name,
      dishes: m.dishes.filter((d) => d.categoryId === first.categoryId),
      options: group.options.map((o) => o.name.replace(/\s*\(.*\)$/, ''))
    };
  });

  ngOnInit() {
    this.api.menu().subscribe({ next: (m) => this.menu.set(m) });
  }
}
