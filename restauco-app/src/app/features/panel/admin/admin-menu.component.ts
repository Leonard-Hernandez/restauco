import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Pencil, Plus, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Category, Dish, Menu } from '../../../core/api/models';
import { DishPhotoComponent } from '../../../shared/dish-photo.component';
import { price } from '../../../shared/format';
import { DishEditorComponent } from './dish-editor.component';

interface CategoryDraft { id?: number; name: string; description: string; position: number; active: boolean }

/** The owner's menu editor: categories and dishes with photos, prices and options. */
@Component({
  selector: 'app-admin-menu',
  imports: [FormsModule, LucideAngularModule, DishPhotoComponent, DishEditorComponent],
  template: `
    <main class="wrap">
      <header class="page-head">
        <h1 class="display">Carta</h1>
        <div class="actions">
          <button type="button" class="btn btn-ghost btn-sm" (click)="editCategory(null)"><lucide-icon [img]="Plus" [size]="16" /> Categoría</button>
          <button type="button" class="btn btn-brand btn-sm" (click)="newDish(null)" [disabled]="!menu()?.categories?.length"><lucide-icon [img]="Plus" [size]="16" /> Plato</button>
        </div>
      </header>
      @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }

      @if (menu(); as m) {
        @for (c of m.categories; track c.id) {
          <section class="category">
            <header class="cat-head">
              <h2 class="label">{{ c.name }} @if (!c.active) { <span class="tag muted">oculta</span> }</h2>
              <button type="button" class="icon-btn" (click)="editCategory(c)" [attr.aria-label]="'Editar ' + c.name"><lucide-icon [img]="Pencil" [size]="16" /></button>
              <button type="button" class="link" (click)="newDish(c.id)"><lucide-icon [img]="Plus" [size]="14" /> Plato</button>
            </header>
            <ul class="dishes">
              @for (d of dishesOf(c.id); track d.id) {
                <li class="dish">
                  <button type="button" class="dish-main" (click)="editing.set({ dish: d, category: c.id })">
                    <app-dish-photo class="thumb" [url]="d.imageUrl" [seed]="d.id" />
                    <span class="dish-text">
                      <span class="dish-name">{{ d.name }}</span>
                      <span class="dish-meta tabular">
                        {{ price(d.price) }}
                        @if (d.modifiers.length) { · {{ d.modifiers.length }} {{ d.modifiers.length === 1 ? 'grupo' : 'grupos' }} de opciones }
                        @if (!d.active) { <span class="tag muted">oculto</span> }
                        @if (!d.imageUrl) { <span class="tag warn">sin foto</span> }
                      </span>
                    </span>
                  </button>
                  <button type="button" class="switch" role="switch" [attr.aria-checked]="d.available" (click)="toggleAvailable(d)"
                          [attr.aria-label]="d.name + (d.available ? ': disponible' : ': agotado')">{{ d.available ? 'Disponible' : 'Agotado' }}</button>
                </li>
              } @empty {
                <li class="empty-row">Sin platos.</li>
              }
            </ul>
          </section>
        } @empty {
          <div class="empty"><p>Empieza creando una categoría (ej: “Arepas”, “Bebidas”).</p></div>
        }
      } @else { <p class="empty">Cargando…</p> }
    </main>

    @if (editing(); as e) {
      <app-dish-editor [dish]="e.dish" [menu]="menu()!" [defaultCategory]="e.category"
                       (close)="editing.set(null)" (saved)="onSaved($event)" (deleted)="onDeleted($event)" />
    }

    @if (category(); as c) {
      <div class="scrim" (click)="category.set(null)">
        <form class="sheet small-sheet" role="dialog" aria-modal="true" aria-labelledby="cat-title" (click)="$event.stopPropagation()" (ngSubmit)="saveCategory(c)">
          <header class="sheet-head">
            <h2 id="cat-title" class="display">{{ c.id ? 'Editar categoría' : 'Nueva categoría' }}</h2>
            <button type="button" class="icon-btn" (click)="category.set(null)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
          </header>
          <div class="body">
            <label class="f"><span>Nombre</span><input class="field" name="name" [(ngModel)]="c.name" required maxlength="80" /></label>
            <label class="f"><span>Descripción</span><input class="field" name="description" [(ngModel)]="c.description" maxlength="255" /></label>
            <div class="row">
              <label class="f"><span>Orden</span><input class="field" type="number" name="position" [(ngModel)]="c.position" /></label>
              <label class="check"><input type="checkbox" name="active" [(ngModel)]="c.active" /> Visible en la carta</label>
            </div>
            @if (categoryError()) { <p class="error-text" role="alert">{{ categoryError() }}</p> }
          </div>
          <footer class="sheet-foot">
            @if (c.id) {
              @if (confirmDeleteCategory()) {
                <button type="button" class="btn btn-sm btn-ghost" (click)="confirmDeleteCategory.set(false)">No</button>
                <button type="button" class="btn btn-sm btn-danger" (click)="deleteCategory(c.id)">Sí, borrar</button>
              } @else {
                <button type="button" class="btn btn-sm btn-danger" (click)="confirmDeleteCategory.set(true)">Borrar</button>
              }
            }
            <button type="submit" class="btn btn-brand grow">Guardar</button>
          </footer>
        </form>
      </div>
    }
  `,
  styleUrl: './admin.css',
  styles: `
    .category { margin-top: 1.4rem; }
    .cat-head { display: flex; align-items: center; gap: 0.4rem; border-bottom: 2px solid var(--brand); padding-bottom: 0.35rem; }
    .cat-head h2 { margin: 0; font-size: 1.25rem; flex: 1; display: flex; gap: 0.5rem; align-items: center; }
    .dishes { list-style: none; margin: 0; padding: 0; }
    .dish { display: flex; align-items: center; gap: 0.6rem; border-bottom: 1px solid var(--line); }
    .dish-main { flex: 1; min-width: 0; display: flex; align-items: center; gap: 0.8rem; padding: 0.6rem 0; background: none; border: 0; color: inherit; font: inherit; text-align: left; }
    .thumb { width: 56px; height: 56px; border-radius: 12px; flex: none; }
    .dish-text { display: flex; flex-direction: column; min-width: 0; }
    .dish-name { font-family: var(--font-label); text-transform: uppercase; }
    .dish-meta { color: var(--ink-muted); font-size: 0.88rem; display: flex; flex-wrap: wrap; gap: 0.35rem; align-items: center; }
    .empty-row { padding: 0.8rem 0; color: var(--ink-faint); }
    .small-sheet { max-width: 28rem; }
    .row { display: flex; gap: 1rem; align-items: end; }
    .check { display: inline-flex; gap: 0.45rem; align-items: center; min-height: 48px; }
  `
})
export default class AdminMenuComponent implements OnInit {
  private api = inject(ApiService);

  readonly menu = signal<Menu | null>(null);
  readonly error = signal<string | null>(null);
  readonly editing = signal<{ dish: Dish | null; category: number | null } | null>(null);
  readonly category = signal<CategoryDraft | null>(null);
  readonly categoryError = signal<string | null>(null);
  readonly confirmDeleteCategory = signal(false);

  readonly price = price;
  readonly Plus = Plus;
  readonly Pencil = Pencil;
  readonly X = X;

  private byCategory = computed(() => {
    const map = new Map<number, Dish[]>();
    for (const d of this.menu()?.dishes ?? []) map.set(d.categoryId, [...(map.get(d.categoryId) ?? []), d]);
    map.forEach((list) => list.sort((a, b) => a.position - b.position || a.id - b.id));
    return map;
  });

  ngOnInit() {
    this.load();
  }

  load() {
    this.api.fullMenu().subscribe({
      next: (m) => this.menu.set({ ...m, categories: [...m.categories].sort((a, b) => a.position - b.position || a.id - b.id) }),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  dishesOf(categoryId: number): Dish[] {
    return this.byCategory().get(categoryId) ?? [];
  }

  newDish(categoryId: number | null) {
    this.editing.set({ dish: null, category: categoryId });
  }

  onSaved(dish: Dish) {
    this.editing.set(null);
    this.replaceDish(dish);
    this.load();
  }

  onDeleted(id: number) {
    this.editing.set(null);
    this.menu.update((m) => m && { ...m, dishes: m.dishes.filter((d) => d.id !== id) });
  }

  toggleAvailable(d: Dish) {
    this.api.setAvailability(d.id, !d.available).subscribe({
      next: (updated) => this.replaceDish(updated),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  editCategory(c: Category | null) {
    this.categoryError.set(null);
    this.confirmDeleteCategory.set(false);
    const next = (this.menu()?.categories.length ?? 0) + 1;
    this.category.set(c
      ? { id: c.id, name: c.name, description: c.description ?? '', position: c.position, active: c.active }
      : { name: '', description: '', position: next, active: true });
  }

  saveCategory(c: CategoryDraft) {
    if (!c.name.trim()) return;
    this.api.saveCategory({ id: c.id, name: c.name.trim(), description: c.description.trim() || null, position: Number(c.position) || 0, active: c.active })
      .subscribe({
        next: () => {
          this.category.set(null);
          this.load();
        },
        error: (e) => this.categoryError.set(errorMessage(e))
      });
  }

  deleteCategory(id: number) {
    this.api.deleteCategory(id).subscribe({
      next: () => {
        this.category.set(null);
        this.load();
      },
      error: (e) => this.categoryError.set(errorMessage(e))
    });
  }

  private replaceDish(dish: Dish) {
    this.menu.update((m) => m && {
      ...m,
      dishes: m.dishes.some((d) => d.id === dish.id) ? m.dishes.map((d) => (d.id === dish.id ? dish : d)) : [...m.dishes, dish]
    });
  }
}
