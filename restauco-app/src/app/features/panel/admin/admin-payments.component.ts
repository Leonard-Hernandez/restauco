import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { PaymentMethod } from '../../../core/api/models';

/** How the restaurant gets paid; the waiter picks one when closing a bill. */
@Component({
  selector: 'app-admin-payments',
  imports: [FormsModule],
  template: `
    <main class="wrap">
      <header class="page-head"><h1 class="display">Métodos de pago</h1></header>
      <p class="page-lead">El mesero escoge uno al cerrar cada cuenta. Desactiva los que ya no recibas (las cuentas pasadas conservan el nombre).</p>
      @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }
      <ul class="list">
        @for (m of methods(); track m.id) {
          <li>
            <input class="field small" type="number" [ngModel]="m.position" (change)="save(m, { position: +$any($event.target).value })" aria-label="Orden" />
            <input class="field grow" [ngModel]="m.name" (change)="save(m, { name: $any($event.target).value })" maxlength="40" aria-label="Nombre" />
            <button type="button" class="switch" role="switch" [attr.aria-checked]="m.active" (click)="save(m, { active: !m.active })">{{ m.active ? 'Activo' : 'Inactivo' }}</button>
          </li>
        }
      </ul>
      <form class="add-row" (ngSubmit)="add()">
        <input class="field" name="name" [(ngModel)]="newName" placeholder="Ej: Daviplata, Tarjeta, Bre-B" maxlength="40" />
        <button type="submit" class="btn btn-brand btn-sm" [disabled]="!newName.trim()">Agregar</button>
      </form>
    </main>
  `,
  styleUrl: './admin.css',
  styles: `.small { width: 4.5rem; flex: none; }`
})
export default class AdminPaymentsComponent implements OnInit {
  private api = inject(ApiService);

  readonly methods = signal<PaymentMethod[]>([]);
  readonly error = signal<string | null>(null);
  newName = '';

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.paymentMethods().subscribe({
      next: (m) => this.methods.set(m),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  save(m: PaymentMethod, change: Partial<PaymentMethod>) {
    const next = { name: m.name, position: m.position, active: m.active, ...change };
    if (!next.name.trim()) return;
    this.error.set(null);
    this.api.savePaymentMethod({ ...next, name: next.name.trim() }, m.id).subscribe({
      next: () => this.load(),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.load();
      }
    });
  }

  add() {
    const position = Math.max(0, ...this.methods().map((m) => m.position)) + 1;
    this.api.savePaymentMethod({ name: this.newName.trim(), position, active: true }).subscribe({
      next: () => {
        this.newName = '';
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }
}
