import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { unlockChime } from '../../shared/chime';
import { WordmarkComponent } from '../../shared/wordmark.component';
import { FloorStore } from './floor/floor-store.service';
import { OrderComposerComponent } from './floor/order-composer.component';
import { PanelStream } from './panel-stream.service';
import { StaffContext } from './staff.service';

type Access = 'checking' | 'granted' | 'denied' | 'error';

const ROLE_LABEL = { OWNER: 'Dueño', WAITER: 'Mesero', COOK: 'Cocina' } as const;

@Component({
  selector: 'app-panel-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, WordmarkComponent, OrderComposerComponent],
  providers: [PanelStream, StaffContext, FloorStore],
  template: `
    <header class="top">
      <a routerLink="/" class="home" aria-label="Ver el sitio"><app-wordmark size="sm" /></a>
      @if (ctx.staff(); as s) {
        <nav class="tabs" aria-label="Panel">
          @if (ctx.canFloor()) {
            <a routerLink="mesas" routerLinkActive="active">Mesas</a>
            <a routerLink="domicilios" routerLinkActive="active">Domicilios @if (floor.pendingDeliveries(); as n) { <span class="count">{{ n }}</span> }</a>
          }
          @if (ctx.canKitchen()) { <a routerLink="cocina" routerLinkActive="active">Cocina</a> }
          @if (ctx.isOwner()) {
            <a routerLink="estadisticas" routerLinkActive="active">Estadísticas</a>
            <a routerLink="carta" routerLinkActive="active">Carta</a>
            <a routerLink="ingredientes" routerLinkActive="active">Ingredientes</a>
            <a routerLink="qr" routerLinkActive="active">Mesas y QR</a>
            <a routerLink="pagos" routerLinkActive="active">Pagos</a>
            <a routerLink="domicilio-ajustes" routerLinkActive="active">Reparto</a>
            <a routerLink="equipo" routerLinkActive="active">Equipo</a>
          }
        </nav>
        <span class="live" [attr.data-status]="stream.status()" [attr.title]="liveTitle()">
          <span class="dot" aria-hidden="true"></span><span class="live-text">{{ liveLabel() }}</span>
        </span>
        <span class="who"><span>{{ s.name ?? s.email }}</span><small>{{ roleLabel[s.role] }}</small></span>
      }
      <button type="button" class="exit" (click)="auth.logout()">Salir</button>
    </header>

    @if (floor.alerts().length) {
      <div class="alerts" aria-live="assertive">
        @for (a of floor.alerts(); track a.id) {
          <a class="alert" [attr.data-kind]="a.kind" [routerLink]="a.link" (click)="floor.dismiss(a.id)">{{ a.text }}</a>
        }
      </div>
    }

    @switch (access()) {
      @case ('granted') { <router-outlet /> }
      @case ('denied') {
        <main class="gate">
          <h1 class="display">Esta cuenta no es del equipo</h1>
          <p>Entraste como {{ auth.displayName() }}. El panel es solo para el equipo del restaurante; pide al dueño que invite tu correo.</p>
          <div class="row">
            <button type="button" class="btn btn-brand" (click)="auth.logout()">Entrar con otra cuenta</button>
            <a routerLink="/" class="btn btn-ghost">Ir al sitio</a>
          </div>
        </main>
      }
      @case ('error') {
        <main class="gate">
          <h1 class="display">No pudimos abrir el panel</h1>
          <p>El servidor no respondió. Revisa tu conexión.</p>
          <button type="button" class="btn btn-brand" (click)="check()">Reintentar</button>
        </main>
      }
      @default { <main class="gate"><p class="checking label">Abriendo el panel…</p></main> }
    }

    @if (access() === 'granted' && floor.composing() !== null && floor.menu()) {
      <app-order-composer />
    }
  `,
  styles: `
    :host { display: block; min-height: 100dvh; background: var(--ground); }
    .top { position: sticky; top: 0; z-index: 30; display: flex; flex-wrap: wrap; align-items: center; gap: 0.2rem 0.9rem; min-height: 60px; padding: 0 0.9rem; background: var(--ground-sunken); border-bottom: 3px solid var(--brand); }
    .home { text-decoration: none; display: inline-flex; flex: none; }
    .tabs { display: flex; gap: 0.15rem; overflow-x: auto; scrollbar-width: none; flex: 1; min-width: 0; }
    .tabs a { display: inline-flex; align-items: center; min-height: 44px; padding: 0 0.8rem; border-radius: 999px; font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.05em; font-weight: 500; color: var(--ink-muted); text-decoration: none; white-space: nowrap; }
    .tabs a:hover { color: var(--ink); }
    .tabs a.active { background: var(--brand); color: var(--brand-ink); }
    @media (max-width: 720px) {
      .top { padding-top: 0.4rem; }
      .tabs { order: 3; flex-basis: 100%; padding-bottom: 0.45rem; }
      .home { margin-right: auto; }
    }
    .live { display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.8rem; color: var(--ink-faint); flex: none; }
    .dot { width: 9px; height: 9px; border-radius: 999px; background: var(--ink-faint); }
    .live[data-status="live"] .dot { background: var(--ok); box-shadow: 0 0 0 4px color-mix(in oklab, var(--ok) 25%, transparent); }
    .live[data-status="offline"] .dot { background: var(--danger); }
    .live-text { display: none; }
    .who { display: none; flex-direction: column; line-height: 1.15; text-align: right; font-size: 0.85rem; flex: none; }
    .who small { color: var(--ink-faint); }
    @media (min-width: 900px) { .who { display: flex; } .live-text { display: inline; } }
    .exit { flex: none; background: none; border: 0; color: var(--ink-muted); font-family: var(--font-label); text-transform: uppercase; letter-spacing: 0.06em; min-height: 44px; padding: 0 0.3rem; }
    .exit:hover { color: var(--brand); }
    .gate { max-width: 34rem; margin: 0 auto; padding: 4rem 1.25rem; }
    .gate h1 { font-size: 2rem; margin: 0 0 0.8rem; color: var(--brand); }
    .gate p { color: var(--ink-muted); }
    .row { display: flex; flex-wrap: wrap; gap: 0.7rem; margin-top: 1.4rem; }
    .checking { color: var(--brand); }
    .alerts { position: fixed; z-index: 70; top: 72px; right: 0.9rem; left: 0.9rem; display: grid; justify-items: end; gap: 0.5rem; pointer-events: none; }
    .alert { pointer-events: auto; max-width: 26rem; padding: 0.95rem 1.15rem; border-radius: 16px; font-weight: 800; font-size: 1.05rem; text-decoration: none; box-shadow: 0 12px 32px rgb(0 0 0 / 0.55); animation: sheet-up 260ms var(--ease-out-expo); }
    .alert[data-kind="ready"] { background: var(--ok); color: #04150c; }
    .alert[data-kind="qr"], .alert[data-kind="delivery"] { background: var(--warn); color: #1d1002; }
    .count { margin-left: 0.35rem; min-width: 1.3rem; padding: 0 0.35rem; border-radius: 999px; background: var(--warn); color: #1d1002; font-size: 0.8rem; text-align: center; }
    .alert[data-kind="sent"] { background: var(--ground-raised); color: var(--ink); border: 2px solid var(--brand); font-weight: 700; }
  `
})
export default class PanelLayoutComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly ctx = inject(StaffContext);
  readonly stream = inject(PanelStream);
  readonly floor = inject(FloorStore);
  private api = inject(ApiService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly access = signal<Access>('checking');
  readonly roleLabel = ROLE_LABEL;

  ngOnInit() {
    unlockChime();
    this.check();
  }

  liveLabel(): string {
    return { live: 'En vivo', connecting: 'Conectando…', offline: 'Sin conexión' }[this.stream.status()];
  }

  liveTitle(): string {
    return this.stream.status() === 'live'
      ? 'Los pedidos llegan en tiempo real'
      : 'Reconectando: los cambios aparecerán al volver la conexión';
  }

  check() {
    this.access.set('checking');
    this.api.staff().subscribe({
      next: (staff) => {
        this.ctx.staff.set(staff);
        this.access.set('granted');
        this.stream.start();
        if (!this.route.firstChild) {
          this.router.navigate([this.ctx.home(staff.role)], { relativeTo: this.route, replaceUrl: true });
        }
      },
      error: (e) => this.access.set(e instanceof HttpErrorResponse && e.status === 403 ? 'denied' : 'error')
    });
  }
}
