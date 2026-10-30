import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Printer, RefreshCw } from 'lucide-angular';
import QRCode from 'qrcode';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { DiningTable } from '../../../core/api/models';
import { TenantService } from '../../../core/api/tenant.service';

/** Tables and their QR codes, ready to print and stick on each table. */
@Component({
  selector: 'app-admin-tables',
  imports: [FormsModule, LucideAngularModule],
  template: `
    <main class="wrap">
      <header class="page-head no-print">
        <h1 class="display">Mesas y QR</h1>
        <button type="button" class="btn btn-brand btn-sm" (click)="print()" [disabled]="!tables().length"><lucide-icon [img]="Printer" [size]="16" /> Imprimir QR</button>
      </header>
      <p class="page-lead no-print">Cada mesa tiene su código. Quien lo escanea ve la carta y pide; el mesero confirma antes de pasarlo a la cocina.</p>
      @if (error()) { <p class="error-text no-print" role="alert">{{ error() }}</p> }

      <ul class="cards">
        @for (t of tables(); track t.id) {
          <li class="card table-card" [class.inactive]="!t.active">
            <div class="print-only print-head">
              <strong class="display">{{ tenant()?.name }}</strong>
              <span>Escanea para ver la carta y pedir</span>
            </div>
            @if (qr()[t.id]; as src) { <img class="qr" [src]="src" [alt]="'Código QR de ' + t.name" /> }
            <div class="table-name display">{{ t.name }}</div>
            <div class="controls no-print">
              <label>N.º <input class="field small" type="number" min="1" [ngModel]="t.number" (change)="update(t, { number: +$any($event.target).value })" /></label>
              <input class="field" placeholder="Nombre (opcional)" [ngModel]="t.label" (change)="update(t, { label: $any($event.target).value || null })" maxlength="40" />
              <div class="row">
                <button type="button" class="switch" role="switch" [attr.aria-checked]="t.active" (click)="update(t, { active: !t.active })">{{ t.active ? 'Activa' : 'Inactiva' }}</button>
                @if (confirmQr() === t.id) {
                  <button type="button" class="btn btn-sm btn-danger" (click)="regenerate(t)">Sí, nuevo QR</button>
                } @else {
                  <button type="button" class="btn btn-sm btn-ghost" (click)="confirmQr.set(t.id)" title="El QR impreso actual deja de funcionar">
                    <lucide-icon [img]="RefreshCw" [size]="14" /> Nuevo QR
                  </button>
                }
              </div>
              <a class="open" [href]="urlOf(t)" target="_blank" rel="noopener">Probar enlace</a>
            </div>
          </li>
        }
      </ul>

      <form class="add-row no-print" (ngSubmit)="add()">
        <label class="f"><span>Número</span><input class="field" type="number" name="number" min="1" [(ngModel)]="newNumber" /></label>
        <label class="f grow"><span>Nombre (opcional)</span><input class="field" name="label" [(ngModel)]="newLabel" placeholder="Ej: Terraza 1" maxlength="40" /></label>
        <button type="submit" class="btn btn-brand btn-sm" [disabled]="!newNumber">Agregar mesa</button>
      </form>
    </main>
  `,
  styleUrl: './admin.css',
  styles: `
    .cards { list-style: none; margin: 1rem 0 0; padding: 0; display: grid; gap: 0.8rem; grid-template-columns: repeat(auto-fill, minmax(14.5rem, 1fr)); }
    .table-card { padding: 1rem; display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
    .table-card.inactive { opacity: 0.55; }
    .qr { width: 168px; height: 168px; border-radius: 12px; background: #fff; padding: 8px; }
    .table-name { font-size: 1.6rem; color: var(--brand); }
    .controls { width: 100%; display: grid; gap: 0.5rem; }
    .controls label { display: flex; gap: 0.5rem; align-items: center; color: var(--ink-muted); }
    .small { width: 5.5rem; min-height: 40px; }
    .row { display: flex; gap: 0.4rem; flex-wrap: wrap; }
    .open { color: var(--ink-faint); font-size: 0.85rem; justify-self: start; }
    .print-only { display: none; }
    @media print {
      :host { background: #fff; color: #000; }
      .no-print { display: none !important; }
      .wrap { max-width: none; padding: 0; }
      .cards { grid-template-columns: repeat(2, 1fr); gap: 12mm; }
      .table-card { break-inside: avoid; border: 2px solid #000; border-radius: 6mm; background: #fff; padding: 8mm; opacity: 1; }
      .table-card.inactive { display: none; }
      .print-only { display: flex; }
      .print-head { flex-direction: column; align-items: center; color: #000; }
      .print-head strong { font-size: 22pt; }
      .qr { width: 60mm; height: 60mm; }
      .table-name { color: #000; font-size: 20pt; }
    }
  `
})
export default class AdminTablesComponent implements OnInit {
  private api = inject(ApiService);
  readonly tenant = inject(TenantService).tenant;

  readonly tables = signal<DiningTable[]>([]);
  readonly qr = signal<Record<number, string>>({});
  readonly error = signal<string | null>(null);
  readonly confirmQr = signal<number | null>(null);
  newNumber: number | null = null;
  newLabel = '';
  readonly Printer = Printer;
  readonly RefreshCw = RefreshCw;

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.tables().subscribe({
      next: (tables) => {
        this.tables.set(tables);
        this.newNumber = Math.max(0, ...tables.map((t) => t.number)) + 1;
        this.renderQr(tables);
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  urlOf(t: DiningTable): string {
    return `${window.location.origin}/mesa/${t.qrToken}`;
  }

  update(t: DiningTable, change: Partial<Pick<DiningTable, 'number' | 'label' | 'active'>>) {
    const next = { number: t.number, label: t.label, active: t.active, ...change };
    this.api.saveTable(next, t.id).subscribe({
      next: () => this.load(),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.load();
      }
    });
  }

  regenerate(t: DiningTable) {
    this.api.regenerateQr(t.id).subscribe({
      next: () => {
        this.confirmQr.set(null);
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  add() {
    if (!this.newNumber) return;
    this.api.saveTable({ number: this.newNumber, label: this.newLabel.trim() || null, active: true }).subscribe({
      next: () => {
        this.newLabel = '';
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  print() {
    window.print();
  }

  private async renderQr(tables: DiningTable[]) {
    const entries = await Promise.all(tables.map(async (t) =>
      [t.id, await QRCode.toDataURL(this.urlOf(t), { margin: 1, width: 360, errorCorrectionLevel: 'M' })] as const));
    this.qr.set(Object.fromEntries(entries));
  }
}
