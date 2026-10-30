import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Trash2 } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Member, Role } from '../../../core/api/models';

const ROLES: { value: Role; label: string; hint: string }[] = [
  { value: 'WAITER', label: 'Mesero', hint: 'Mesas, pedidos y cuentas' },
  { value: 'COOK', label: 'Cocina', hint: 'Pantalla de cocina y agotados' },
  { value: 'OWNER', label: 'Dueño', hint: 'Todo, incluida la carta y el equipo' }
];

/** Who can use the panel. People sign in with Google; the invitation links on first login. */
@Component({
  selector: 'app-admin-team',
  imports: [FormsModule, LucideAngularModule],
  template: `
    <main class="wrap">
      <header class="page-head"><h1 class="display">Equipo</h1></header>
      <p class="page-lead">Invita con el correo de Google de cada persona. Entra en este mismo sitio, en “Equipo” al pie de la página.</p>
      @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }
      <ul class="list">
        @for (m of members(); track m.id) {
          <li>
            <div class="grow who">
              <span>{{ m.name ?? m.email }}</span>
              <small>{{ m.email }} · @if (m.linked) { <span class="tag ok">activo</span> } @else { <span class="tag warn">invitado</span> }</small>
            </div>
            <select class="field role" [ngModel]="m.role" (ngModelChange)="changeRole(m, $event)" [attr.aria-label]="'Rol de ' + m.email">
              @for (r of roles; track r.value) { <option [value]="r.value">{{ r.label }}</option> }
            </select>
            @if (confirm() === m.id) {
              <button type="button" class="btn btn-sm btn-ghost" (click)="confirm.set(null)">No</button>
              <button type="button" class="btn btn-sm btn-danger" (click)="remove(m)">Quitar</button>
            } @else {
              <button type="button" class="icon-btn" (click)="confirm.set(m.id)" [attr.aria-label]="'Quitar a ' + m.email"><lucide-icon [img]="Trash2" [size]="16" /></button>
            }
          </li>
        }
      </ul>
      <form class="add-row" (ngSubmit)="invite()">
        <label class="f grow"><span>Correo de Google</span><input class="field" type="email" name="email" [(ngModel)]="email" placeholder="nombre@gmail.com" required /></label>
        <label class="f"><span>Rol</span>
          <select class="field" name="role" [(ngModel)]="role">
            @for (r of roles; track r.value) { <option [value]="r.value">{{ r.label }} — {{ r.hint }}</option> }
          </select>
        </label>
        <button type="submit" class="btn btn-brand btn-sm" [disabled]="!email.includes('@')">Invitar</button>
      </form>
    </main>
  `,
  styleUrl: './admin.css',
  styles: `
    .who { display: flex; flex-direction: column; min-width: 12rem; }
    .who small { color: var(--ink-faint); display: flex; gap: 0.3rem; align-items: center; flex-wrap: wrap; }
    .role { width: auto; min-width: 8rem; }
  `
})
export default class AdminTeamComponent implements OnInit {
  private api = inject(ApiService);

  readonly members = signal<Member[]>([]);
  readonly error = signal<string | null>(null);
  readonly confirm = signal<number | null>(null);
  readonly roles = ROLES;
  readonly Trash2 = Trash2;
  email = '';
  role: Role = 'WAITER';

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.members().subscribe({
      next: (m) => this.members.set(m),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  invite() {
    this.error.set(null);
    this.api.invite(this.email.trim(), this.role).subscribe({
      next: () => {
        this.email = '';
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  changeRole(m: Member, role: Role) {
    this.error.set(null);
    this.api.changeRole(m.id, role).subscribe({
      next: () => this.load(),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.load();
      }
    });
  }

  remove(m: Member) {
    this.api.removeMember(m.id).subscribe({
      next: () => {
        this.confirm.set(null);
        this.load();
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }
}
