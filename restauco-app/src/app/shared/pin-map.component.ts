import { AfterViewInit, Component, effect, ElementRef, input, OnDestroy, output, viewChild } from '@angular/core';
import type * as Leaflet from 'leaflet';

export interface LatLng {
  lat: number;
  lng: number;
}

/**
 * A map with one pin the customer drags (or taps) to their door, the restaurant and the
 * delivery zone. OpenStreetMap tiles; Leaflet is loaded only when the map is shown.
 */
@Component({
  selector: 'app-pin-map',
  template: `<div #host class="map" role="application" aria-label="Mapa: arrastra el marcador o toca tu puerta"></div>`,
  styles: `
    :host { display: block; }
    .map { height: 100%; min-height: 15rem; border-radius: 16px; overflow: hidden; background: var(--ground-sunken); }
    :host ::ng-deep .pin { width: 34px; height: 34px; border-radius: 50% 50% 50% 0; transform: rotate(-45deg); background: var(--brand); border: 3px solid #fff; box-shadow: 0 6px 14px rgb(0 0 0 / 0.45); }
    :host ::ng-deep .home { width: 18px; height: 18px; border-radius: 6px; background: var(--ink); border: 3px solid var(--brand); }
    :host ::ng-deep .leaflet-control-attribution { font-size: 10px; }
  `
})
export class PinMapComponent implements AfterViewInit, OnDestroy {
  readonly pin = input<LatLng | null>(null);
  readonly restaurant = input.required<LatLng>();
  readonly radiusKm = input(0);
  readonly moved = output<LatLng>();

  private host = viewChild.required<ElementRef<HTMLDivElement>>('host');
  private L: typeof Leaflet | null = null;
  private map: Leaflet.Map | null = null;
  private marker: Leaflet.Marker | null = null;

  constructor() {
    effect(() => {
      const pin = this.pin();
      if (this.map) this.place(pin);
    });
  }

  async ngAfterViewInit() {
    // Leaflet is CommonJS: the namespace is either the module or its default export
    const mod = await import('leaflet');
    const L = ((mod as unknown as { default?: typeof Leaflet }).default ?? mod) as typeof Leaflet;
    this.L = L;
    const home = this.restaurant();
    this.map = L.map(this.host().nativeElement, { zoomControl: true, attributionControl: true })
      .setView([home.lat, home.lng], 14);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap'
    }).addTo(this.map);
    if (this.radiusKm() > 0) {
      L.circle([home.lat, home.lng], { radius: this.radiusKm() * 1000, color: '#ffc21a', weight: 2, fillOpacity: 0.06 }).addTo(this.map);
    }
    L.marker([home.lat, home.lng], { icon: L.divIcon({ className: '', html: '<div class="home"></div>', iconSize: [18, 18] }), keyboard: false, interactive: false })
      .addTo(this.map);
    this.map.on('click', (e: Leaflet.LeafletMouseEvent) => this.emit(e.latlng));
    this.place(this.pin());
  }

  ngOnDestroy() {
    this.map?.remove();
  }

  private place(pin: LatLng | null) {
    const L = this.L;
    if (!L || !this.map) return;
    if (!pin) {
      this.marker?.remove();
      this.marker = null;
      return;
    }
    if (!this.marker) {
      this.marker = L.marker([pin.lat, pin.lng], {
        draggable: true,
        autoPan: true,
        icon: L.divIcon({ className: '', html: '<div class="pin"></div>', iconSize: [34, 34], iconAnchor: [17, 40] })
      }).addTo(this.map);
      this.marker.on('dragend', () => this.emit(this.marker!.getLatLng()));
    } else {
      this.marker.setLatLng([pin.lat, pin.lng]);
    }
    this.map.setView([pin.lat, pin.lng], Math.max(this.map.getZoom(), 17));
  }

  private emit(p: Leaflet.LatLng) {
    this.moved.emit({ lat: +p.lat.toFixed(6), lng: +p.lng.toFixed(6) });
  }
}
