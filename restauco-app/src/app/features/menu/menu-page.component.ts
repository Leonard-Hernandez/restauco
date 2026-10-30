import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService, errorMessage } from '../../core/api/api.service';
import { Dish, Menu } from '../../core/api/models';
import { SiteHeaderComponent } from '../../shared/site-header.component';
import { DishSheetComponent } from './dish-sheet.component';
import { MenuBrowserComponent } from './menu-browser.component';

/** The public menu (read-only: guests order from the table QR). */
@Component({
  selector: 'app-menu-page',
  imports: [SiteHeaderComponent, MenuBrowserComponent, DishSheetComponent],
  template: `
    <app-site-header />
    <main class="wrap">
      <h1 class="display title">La carta</h1>
      @if (menu(); as m) {
        <app-menu-browser [menu]="m" (open)="selected.set($event)" />
        @if (selected(); as d) {
          <app-dish-sheet [dish]="d" [tags]="m.tags" (close)="selected.set(null)"
                          hint="Para pedir, escanea el código QR de tu mesa o pide a domicilio desde la página de inicio." />
        }
      } @else if (error()) {
        <div class="empty"><p>{{ error() }}</p><button type="button" class="btn btn-ghost btn-sm" (click)="load()">Reintentar</button></div>
      } @else {
        <p class="empty">Cargando la carta…</p>
      }
    </main>
  `,
  styles: `
    .wrap { max-width: 72rem; margin: 0 auto; padding-bottom: 4rem; }
    .title { margin: 0.6rem 1rem 0; font-size: 2.6rem; }
  `
})
export default class MenuPageComponent implements OnInit {
  private api = inject(ApiService);

  readonly menu = signal<Menu | null>(null);
  readonly selected = signal<Dish | null>(null);
  readonly error = signal<string | null>(null);

  ngOnInit() {
    this.load();
  }

  load() {
    this.error.set(null);
    this.api.menu().subscribe({
      next: (m) => this.menu.set(m),
      error: (e) => this.error.set(errorMessage(e))
    });
  }
}
