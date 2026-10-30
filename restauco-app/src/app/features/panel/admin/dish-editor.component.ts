import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, ImagePlus, Plus, Trash2, X } from 'lucide-angular';
import { ApiService, errorMessage } from '../../../core/api/api.service';
import { Dish, DishInput, Ingredient, Menu } from '../../../core/api/models';
import { DishPhotoComponent } from '../../../shared/dish-photo.component';

interface OptionDraft { id: string | null; name: string; price: number; ingredientIds: number[] }
interface GroupDraft { id: string | null; name: string; min: number; max: number; options: OptionDraft[] }

/** Create or edit a dish: data, photo, ingredients, tags and option groups. */
@Component({
  selector: 'app-dish-editor',
  imports: [FormsModule, LucideAngularModule, DishPhotoComponent],
  template: `
    <div class="scrim" (click)="close.emit()">
      <form class="sheet editor" role="dialog" aria-modal="true" aria-labelledby="editor-title" (click)="$event.stopPropagation()" (ngSubmit)="save()">
        <header class="sheet-head">
          <h2 id="editor-title" class="display">{{ dish() ? 'Editar plato' : 'Nuevo plato' }}</h2>
          <button type="button" class="icon-btn" (click)="close.emit()" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
        </header>

        <div class="body">
          <div class="photo-row">
            <app-dish-photo class="photo" [url]="previewUrl() ?? dish()?.imageUrl ?? null" [alt]="name" [seed]="dish()?.id ?? 0" />
            <div class="photo-actions">
              <label class="btn btn-ghost btn-sm">
                <lucide-icon [img]="ImagePlus" [size]="16" /> {{ dish()?.imageUrl || file() ? 'Cambiar foto' : 'Subir foto' }}
                <input type="file" accept="image/jpeg,image/png,image/webp" (change)="pick($event)" hidden />
              </label>
              @if (dish()?.imageUrl && !file()) {
                <button type="button" class="btn btn-sm btn-danger" (click)="removePhoto()">Quitar foto</button>
              }
              <small>JPG, PNG o WebP, máximo 5 MB.</small>
            </div>
          </div>

          <label class="f">
            <span>Nombre</span>
            <input class="field" name="name" [(ngModel)]="name" required maxlength="120" />
          </label>
          <div class="two">
            <label class="f">
              <span>Categoría</span>
              <select class="field" name="category" [(ngModel)]="categoryId" required>
                @for (c of menu().categories; track c.id) { <option [ngValue]="c.id">{{ c.name }}</option> }
              </select>
            </label>
            <label class="f">
              <span>Precio (COP)</span>
              <input class="field tabular" type="number" name="price" [(ngModel)]="price" min="0" step="500" required />
            </label>
          </div>
          <label class="f">
            <span>Descripción</span>
            <textarea class="field" name="description" [(ngModel)]="description" maxlength="500" rows="2"></textarea>
          </label>
          <div class="checks">
            <label><input type="checkbox" name="active" [(ngModel)]="active" /> Visible en la carta</label>
            <label><input type="checkbox" name="available" [(ngModel)]="available" /> Disponible hoy</label>
            <label class="pos">Orden <input class="field small" type="number" name="position" [(ngModel)]="position" /></label>
          </div>

          <fieldset>
            <legend class="label">Ingredientes</legend>
            <p class="hint">Sirven para el filtro de la carta. Marca “se puede quitar” si el cliente puede pedirlo sin ese ingrediente.</p>
            <ul class="ingredients">
              @for (i of catalog(); track i.id) {
                <li [class.on]="has(i.id)">
                  <label class="grow"><input type="checkbox" [checked]="has(i.id)" (change)="toggleIngredient(i.id)" /> {{ i.name }} @if (i.allergen) { <small class="allergen">alérgeno</small> }</label>
                  @if (has(i.id)) {
                    <label class="removable"><input type="checkbox" [checked]="removable(i.id)" (change)="toggleRemovable(i.id)" /> se puede quitar</label>
                  }
                </li>
              }
            </ul>
            <div class="inline-add">
              <input class="field" name="newIngredient" [(ngModel)]="newIngredient" placeholder="Nuevo ingrediente" maxlength="80" />
              <button type="button" class="btn btn-ghost btn-sm" (click)="addIngredient()" [disabled]="!newIngredient.trim()">Agregar</button>
            </div>
          </fieldset>

          @if (menu().tags.length) {
            <fieldset>
              <legend class="label">Etiquetas</legend>
              <div class="chips">
                @for (t of menu().tags; track t.id) {
                  <button type="button" class="chip" [attr.aria-pressed]="tagIds.includes(t.id)" (click)="toggleTag(t.id)">{{ t.name }}</button>
                }
              </div>
            </fieldset>
          }

          <fieldset>
            <legend class="label">Opciones para escoger</legend>
            <p class="hint">Ej: “Rellenos” (escoge 2), “Término de la carne” (escoge 1), “Extras” (opcional, con precio).</p>
            @for (g of groups; track $index; let gi = $index) {
              <div class="group">
                <div class="group-head">
                  <input class="field" [name]="'g' + gi + 'name'" [(ngModel)]="g.name" placeholder="Nombre del grupo" maxlength="60" required />
                  <button type="button" class="icon-btn" (click)="groups.splice(gi, 1)" aria-label="Quitar grupo"><lucide-icon [img]="Trash2" [size]="16" /></button>
                </div>
                <div class="minmax">
                  <label>Mínimo <input class="field small" type="number" min="0" [name]="'g' + gi + 'min'" [(ngModel)]="g.min" /></label>
                  <label>Máximo <input class="field small" type="number" min="1" [name]="'g' + gi + 'max'" [(ngModel)]="g.max" /></label>
                  <small>{{ g.min === 0 ? 'Opcional' : 'Obligatorio' }}</small>
                </div>
                @for (o of g.options; track $index; let oi = $index) {
                  <div class="option">
                    <input class="field" [name]="'g' + gi + 'o' + oi" [(ngModel)]="o.name" placeholder="Opción" maxlength="80" required />
                    <input class="field price" type="number" min="0" step="500" [name]="'g' + gi + 'p' + oi" [(ngModel)]="o.price" aria-label="Precio adicional" />
                    <button type="button" class="icon-btn" (click)="g.options.splice(oi, 1)" aria-label="Quitar opción"><lucide-icon [img]="X" [size]="16" /></button>
                  </div>
                  <details class="uses">
                    <summary>{{ o.ingredientIds.length ? 'Hecho con: ' + ingredientNames(o.ingredientIds) : 'Hecho con… (para que se agote si se acaba un ingrediente)' }}</summary>
                    <div class="chips">
                      @for (i of menu().ingredients; track i.id) {
                        <button type="button" class="chip" [attr.aria-pressed]="o.ingredientIds.includes(i.id)" (click)="toggleUses(o, i.id)">{{ i.name }}</button>
                      }
                    </div>
                  </details>
                }
                <button type="button" class="link" (click)="g.options.push({ id: null, name: '', price: 0, ingredientIds: [] })"><lucide-icon [img]="Plus" [size]="14" /> Opción</button>
              </div>
            }
            <button type="button" class="btn btn-ghost btn-sm" (click)="addGroup()"><lucide-icon [img]="Plus" [size]="16" /> Grupo de opciones</button>
          </fieldset>

          @if (error()) { <p class="error-text" role="alert">{{ error() }}</p> }
        </div>

        <footer class="sheet-foot">
          @if (dish()) {
            @if (confirmDelete()) {
              <button type="button" class="btn btn-sm btn-ghost" (click)="confirmDelete.set(false)">No</button>
              <button type="button" class="btn btn-sm btn-danger" (click)="remove()">Sí, borrar</button>
            } @else {
              <button type="button" class="btn btn-sm btn-danger" (click)="confirmDelete.set(true)">Borrar</button>
            }
          }
          <button type="submit" class="btn btn-brand grow" [disabled]="saving()">{{ saving() ? 'Guardando…' : 'Guardar' }}</button>
        </footer>
      </form>
    </div>
  `,
  styles: `
    .editor { max-width: 40rem; }
    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.4rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .body { padding: 0 1.1rem 1rem; display: grid; gap: 0.9rem; }
    .photo-row { display: flex; gap: 0.9rem; align-items: center; }
    .photo { width: 112px; height: 112px; border-radius: 16px; flex: none; }
    .photo-actions { display: flex; flex-direction: column; align-items: flex-start; gap: 0.4rem; }
    .photo-actions small { color: var(--ink-faint); }
    .f { display: grid; gap: 0.3rem; font-size: 0.9rem; color: var(--ink-muted); }
    .two { display: grid; grid-template-columns: 1fr 1fr; gap: 0.7rem; }
    .checks { display: flex; flex-wrap: wrap; gap: 0.6rem 1.2rem; align-items: center; }
    .checks label { display: inline-flex; align-items: center; gap: 0.4rem; }
    .small { width: 5rem; min-height: 40px; }
    fieldset { border: 0; padding: 0; margin: 0.4rem 0 0; }
    legend { color: var(--brand); padding: 0; margin-bottom: 0.2rem; }
    .hint { margin: 0 0 0.5rem; font-size: 0.85rem; color: var(--ink-faint); }
    .ingredients { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.1rem; }
    .ingredients li { display: flex; gap: 0.6rem; align-items: center; min-height: 40px; padding: 0 0.5rem; border-radius: 10px; }
    .ingredients li.on { background: rgb(255 255 255 / 0.04); }
    .grow { flex: 1; }
    .ingredients label { display: inline-flex; gap: 0.45rem; align-items: center; }
    .removable { font-size: 0.85rem; color: var(--ink-muted); }
    .allergen { color: var(--warn); }
    .inline-add { display: flex; gap: 0.5rem; margin-top: 0.5rem; }
    .chips { display: flex; flex-wrap: wrap; gap: 0.4rem; }
    .group { border: 1px solid var(--line); border-radius: 14px; padding: 0.7rem; margin-bottom: 0.6rem; display: grid; gap: 0.5rem; }
    .group-head, .option { display: flex; gap: 0.4rem; align-items: center; }
    .uses { margin: 0.1rem 0 0.5rem; font-size: 0.85rem; color: var(--ink-faint); }
    .uses summary { cursor: pointer; min-height: 32px; display: flex; align-items: center; }
    .uses .chips { display: flex; flex-wrap: wrap; gap: 0.35rem; padding: 0.4rem 0; }
    .uses .chip[aria-pressed="true"] { background: var(--brand); color: var(--brand-ink); border-color: var(--brand); }
    .minmax { display: flex; gap: 0.8rem; align-items: center; font-size: 0.88rem; color: var(--ink-muted); }
    .minmax label { display: inline-flex; gap: 0.4rem; align-items: center; }
    .price { width: 7rem; }
    .link { justify-self: start; display: inline-flex; align-items: center; gap: 0.25rem; background: none; border: 0; color: var(--brand); font: inherit; font-weight: 700; min-height: 36px; }
    .sheet-foot { position: sticky; bottom: 0; display: flex; gap: 0.5rem; align-items: center; padding: 0.85rem 1.1rem 1rem; background: var(--ground-raised); border-top: 1px solid var(--line); }
  `
})
export class DishEditorComponent implements OnInit {
  readonly dish = input<Dish | null>(null);
  readonly menu = input.required<Menu>();
  readonly defaultCategory = input<number | null>(null);
  readonly close = output<void>();
  readonly saved = output<Dish>();
  readonly deleted = output<number>();

  private api = inject(ApiService);

  name = '';
  categoryId: number | null = null;
  price = 0;
  description = '';
  active = true;
  available = true;
  position = 0;
  ingredients = new Map<number, boolean>();
  tagIds: number[] = [];
  groups: GroupDraft[] = [];
  newIngredient = '';

  readonly catalog = signal<Ingredient[]>([]);
  readonly file = signal<File | null>(null);
  readonly previewUrl = signal<string | null>(null);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly confirmDelete = signal(false);

  readonly X = X;
  readonly Plus = Plus;
  readonly Trash2 = Trash2;
  readonly ImagePlus = ImagePlus;

  ngOnInit() {
    this.catalog.set(this.menu().ingredients);
    const d = this.dish();
    if (d) {
      this.name = d.name;
      this.categoryId = d.categoryId;
      this.price = d.price;
      this.description = d.description ?? '';
      this.active = d.active;
      this.available = d.available;
      this.position = d.position;
      d.ingredients.forEach((i) => this.ingredients.set(i.id, i.removable));
      this.tagIds = [...d.tagIds];
      this.groups = d.modifiers.map((g) => ({ ...g, options: g.options.map((o) => ({ id: o.id, name: o.name, price: o.price, ingredientIds: [...(o.ingredientIds ?? [])] })) }));
    } else {
      this.categoryId = this.defaultCategory() ?? this.menu().categories[0]?.id ?? null;
    }
  }

  has(id: number) { return this.ingredients.has(id); }
  removable(id: number) { return this.ingredients.get(id) === true; }

  toggleIngredient(id: number) {
    if (this.ingredients.has(id)) this.ingredients.delete(id); else this.ingredients.set(id, false);
  }

  toggleRemovable(id: number) {
    this.ingredients.set(id, !this.ingredients.get(id));
  }

  toggleTag(id: number) {
    this.tagIds = this.tagIds.includes(id) ? this.tagIds.filter((t) => t !== id) : [...this.tagIds, id];
  }

  addGroup() {
    this.groups.push({ id: null, name: '', min: 1, max: 1, options: [{ id: null, name: '', price: 0, ingredientIds: [] }] });
  }

  addIngredient() {
    this.api.saveIngredient({ name: this.newIngredient.trim(), allergen: false }).subscribe({
      next: (i) => {
        this.catalog.update((c) => [...c, i].sort((a, b) => a.name.localeCompare(b.name)));
        this.ingredients.set(i.id, false);
        this.newIngredient = '';
      },
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  pick(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      this.error.set('La foto pesa más de 5 MB.');
      return;
    }
    this.file.set(file);
    this.previewUrl.set(URL.createObjectURL(file));
  }

  removePhoto() {
    const d = this.dish();
    if (!d) return;
    this.api.removeDishImage(d.id).subscribe({
      next: (updated) => this.saved.emit(updated),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  save() {
    if (!this.name.trim() || this.categoryId === null) {
      this.error.set('El plato necesita nombre y categoría.');
      return;
    }
    const body: DishInput = {
      categoryId: this.categoryId,
      name: this.name.trim(),
      description: this.description.trim() || null,
      price: Math.max(0, Math.round(Number(this.price) || 0)),
      available: this.available,
      active: this.active,
      position: Number(this.position) || 0,
      ingredients: [...this.ingredients].map(([ingredientId, removable]) => ({ ingredientId, removable })),
      tagIds: this.tagIds,
      modifiers: this.groups.map((g) => ({
        id: g.id, name: g.name.trim(), min: Number(g.min) || 0, max: Number(g.max) || 1,
        options: g.options.filter((o) => o.name.trim()).map((o) => ({ id: o.id, name: o.name.trim(), price: Number(o.price) || 0, ingredientIds: o.ingredientIds }))
      }))
    };
    this.saving.set(true);
    this.error.set(null);
    this.api.saveDish(body, this.dish()?.id).subscribe({
      next: (dish) => {
        const file = this.file();
        if (!file) {
          this.saving.set(false);
          this.saved.emit(dish);
          return;
        }
        this.api.uploadDishImage(dish.id, file).subscribe({
          next: (withPhoto) => {
            this.saving.set(false);
            this.saved.emit(withPhoto);
          },
          error: (e) => {
            this.saving.set(false);
            this.error.set(`Plato guardado, pero la foto no: ${errorMessage(e)}`);
          }
        });
      },
      error: (e) => {
        this.saving.set(false);
        this.error.set(errorMessage(e));
      }
    });
  }

  remove() {
    const d = this.dish();
    if (!d) return;
    this.api.deleteDish(d.id).subscribe({
      next: () => this.deleted.emit(d.id),
      error: (e) => this.error.set(errorMessage(e))
    });
  }

  ingredientNames(ids: number[]): string {
    return ids.map((id) => this.menu().ingredients.find((i) => i.id === id)?.name).filter(Boolean).join(', ');
  }

  toggleUses(o: OptionDraft, id: number) {
    o.ingredientIds = o.ingredientIds.includes(id) ? o.ingredientIds.filter((x) => x !== id) : [...o.ingredientIds, id];
  }
}
