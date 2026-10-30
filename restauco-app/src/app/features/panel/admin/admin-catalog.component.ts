import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { LucideAngularModule, Trash2 } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Ingredient, Tag } from '../../../core/api/models';

/** Ingredients (for the "sin..." filter and allergy warnings) and dish tags. */
@Component({
  selector: 'app-admin-catalog',
  imports: [FormsModule, LucideAngularModule],
  template: `
    <main class="wrap">
      <header class="page-head"><h1 class="display">Ingredientes</h1></header>
      <p class="page-lead">Los clientes filtran la carta por ingredientes. Marca los alérgenos (lácteos, gluten, maní…) para avisarles, y "Se acabó" cuando un ingrediente se agote en el día.</p>
      @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }

      <ul class="list">
        @for (i of ingredients(); track i.id) {
          <li>
            <input class="field grow" [ngModel]="i.name" (change)="renameIngredient(i, $any($event.target).value)" [attr.aria-label]="'Nombre de ' + i.name" maxlength="80" />
            <button type="button" class="switch" role="switch" [attr.aria-checked]="!i.allergen" (click)="toggleAllergen(i)">
              {{ i.allergen ? 'Alérgeno' : 'Normal' }}
            </button>
            <button type="button" class="switch" role="switch" [attr.aria-checked]="i.available" (click)="toggleAvailable(i)"
                    [attr.aria-label]="i.name + (i.available ? ': hay' : ': se acabó')">
              {{ i.available ? 'Hay' : 'Se acabó' }}
            </button>
            @if (confirm() === 'i' + i.id) {
              <button type="button" class="btn btn-sm btn-ghost" (click)="confirm.set(null)">No</button>
              <button type="button" class="btn btn-sm btn-danger" (click)="deleteIngredient(i)">Borrar</button>
            } @else {
              <button type="button" class="icon-btn" (click)="confirm.set('i' + i.id)" [attr.aria-label]="'Borrar ' + i.name"><lucide-icon [img]="Trash2" [size]="16" /></button>
            }
          </li>
        }
      </ul>
      <form class="add-row" (ngSubmit)="addIngredient()">
        <input class="field" name="ingredient" [(ngModel)]="newIngredient" placeholder="Nuevo ingrediente" maxlength="80" />
        <label class="check"><input type="checkbox" name="allergen" [(ngModel)]="newAllergen" /> Alérgeno</label>
        <button type="submit" class="btn btn-brand btn-sm" [disabled]="!newIngredient.trim()">Agregar</button>
      </form>

      <header class="page-head second"><h2 class="display">Etiquetas</h2></header>
      <p class="page-lead">Ej: Vegetariana, Picante, Nuevo. Aparecen en la carta y sirven como filtro.</p>
      <ul class="list">
        @for (t of tags(); track t.id) {
          <li>
            <input class="field grow" [ngModel]="t.name" (change)="renameTag(t, $any($event.target).value)" [attr.aria-label]="'Nombre de ' + t.name" maxlength="40" />
            @if (confirm() === 't' + t.id) {
              <button type="button" class="btn btn-sm btn-ghost" (click)="confirm.set(null)">No</button>
              <button type="button" class="btn btn-sm btn-danger" (click)="deleteTag(t)">Borrar</button>
            } @else {
              <button type="button" class="icon-btn" (click)="confirm.set('t' + t.id)" [attr.aria-label]="'Borrar ' + t.name"><lucide-icon [img]="Trash2" [size]="16" /></button>
            }
          </li>
        }
      </ul>
      <form class="add-row" (ngSubmit)="addTag()">
        <input class="field" name="tag" [(ngModel)]="newTag" placeholder="Nueva etiqueta" maxlength="40" />
        <button type="submit" class="btn btn-brand btn-sm" [disabled]="!newTag.trim()">Agregar</button>
      </form>
    </main>
  `,
  styleUrl: './admin.css',
  styles: `
    .second { margin-top: 2.4rem; }
    .second h2 { margin: 0; font-size: 1.8rem; color: var(--brand); }
    .check { display: inline-flex; gap: 0.4rem; align-items: center; min-height: 48px; }
    .switch[aria-checked="false"] { background: color-mix(in oklab, var(--warn) 25%, transparent); color: var(--warn); }
  `
})
export default class AdminCatalogComponent implements OnInit {
  private api = inject(ApiService);

  readonly ingredients = signal<Ingredient[]>([]);
  readonly tags = signal<Tag[]>([]);
  readonly error = signal<string | null>(null);
  readonly confirm = signal<string | null>(null);
  newIngredient = '';
  newAllergen = false;
  newTag = '';
  readonly Trash2 = Trash2;

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.fullMenu().subscribe({
      next: (m) => {
        this.ingredients.set(m.ingredients);
        this.tags.set(m.tags);
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  addIngredient() {
    this.run(this.api.saveIngredient({ name: this.newIngredient.trim(), allergen: this.newAllergen }), () => {
      this.newIngredient = '';
      this.newAllergen = false;
    });
  }

  renameIngredient(i: Ingredient, name: string) {
    if (name.trim() && name.trim() !== i.name) this.run(this.api.saveIngredient({ name: name.trim(), allergen: i.allergen }, i.id));
  }

  toggleAllergen(i: Ingredient) {
    this.run(this.api.saveIngredient({ name: i.name, allergen: !i.allergen }, i.id));
  }

  toggleAvailable(i: Ingredient) {
    this.run(this.api.setIngredientAvailability(i.id, !i.available));
  }

  deleteIngredient(i: Ingredient) {
    this.run(this.api.deleteIngredient(i.id));
  }

  addTag() {
    this.run(this.api.saveTag(this.newTag.trim()), () => (this.newTag = ''));
  }

  renameTag(t: Tag, name: string) {
    if (name.trim() && name.trim() !== t.name) this.run(this.api.saveTag(name.trim(), t.id));
  }

  deleteTag(t: Tag) {
    this.run(this.api.deleteTag(t.id));
  }

  private run(call: Observable<unknown>, done?: () => void) {
    this.error.set(null);
    call.subscribe({
      next: () => {
        this.confirm.set(null);
        done?.();
        this.load();
      },
      error: (e: unknown) => {
        this.error.set(errorMessage(e));
        this.load();
      }
    });
  }
}
