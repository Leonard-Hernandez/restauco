import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { WordmarkComponent } from './wordmark.component';

@Component({
  selector: 'app-site-header',
  imports: [RouterLink, RouterLinkActive, WordmarkComponent],
  template: `
    <div class="band strip" aria-hidden="true"></div>
    <header class="top">
      <a routerLink="/" class="home" aria-label="Inicio"><app-wordmark size="sm" /></a>
      <nav>
        <a routerLink="/menu" routerLinkActive="active" class="label">Carta</a>
      </nav>
    </header>
  `,
  styles: `
    :host { display: block; }
    .strip { height: 10px; --band-size: 36px; }
    .top { height: 60px; display: flex; align-items: center; justify-content: space-between; padding: 0 1rem; max-width: 72rem; margin: 0 auto; }
    .home { text-decoration: none; }
    nav a { color: var(--ink-muted); text-decoration: none; font-size: 1rem; padding: 0.6rem 0.4rem; }
    nav a.active, nav a:hover { color: var(--brand); }
  `
})
export class SiteHeaderComponent {}
