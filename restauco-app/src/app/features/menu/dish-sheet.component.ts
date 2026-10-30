import { Component, computed, input, OnDestroy, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Minus, Plus, TriangleAlert, X } from 'lucide-angular';
import { Dish, ModifierGroup, Tag } from '../../core/api/models';
import { CartLine, missingChoice, newLine, unitPrice } from '../../shared/cart';
import { DishPhotoComponent } from '../../shared/dish-photo.component';
import { extra, price } from '../../shared/format';

/**
 * A dish up close: photo, ingredients, allergens. In order mode the guest (or waiter) picks
 * the options, leaves out removable ingredients, adds a note and the quantity.
 */
@Component({
  selector: 'app-dish-sheet',
  imports: [FormsModule, LucideAngularModule, DishPhotoComponent],
  template: `
    <div class="scrim" (click)="close.emit()">
      <article class="sheet" role="dialog" aria-modal="true" [attr.aria-label]="dish().name" (click)="$event.stopPropagation()">
        <div class="hero">
          <app-dish-photo class="photo" [url]="dish().imageUrl" [alt]="dish().name" [seed]="dish().id" />
          <button type="button" class="icon-btn close" (click)="close.emit()" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
        </div>

        <div class="content">
          <h2 class="name label">{{ dish().name }}</h2>
          <p class="price tabular">{{ price(dish().price) }}</p>
          @if (dish().description) { <p class="desc">{{ dish().description }}</p> }
          @if (tagNames().length) {
            <p class="tags">@for (t of tagNames(); track t) { <span class="tag">{{ t }}</span> }</p>
          }
          @if (dish().ingredients.length) {
            <p class="ingredients">
              <span class="muted">Lleva:</span>
              @for (i of dish().ingredients; track i.id; let last = $last) {
                <span [class.allergen]="i.allergen" [class.ran-out]="!i.available">{{ i.name }}@if (i.allergen) {<lucide-icon [img]="TriangleAlert" [size]="12" aria-label="alérgeno" />}</span>@if (!last) {<span>, </span>}
              }
            </p>
          }

          @if (!dish().orderable) {
            <p class="soldout label">Agotado por hoy</p>
            @if (soldOutText(); as why) { <p class="why">{{ why }}</p> }
          } @else if (orderable()) {
            @if (leftOut().length) {
              <p class="left-out">Hoy va sin {{ leftOutNames() }}: se nos acabó.</p>
            }
            @for (g of dish().modifiers; track g.id) {
              <fieldset class="group">
                <legend>
                  <span class="label">{{ g.name }}</span>
                  <span class="rule" [class.missing]="showMissing() && missing()?.id === g.id">{{ ruleOf(g) }}</span>
                </legend>
                @for (o of g.options; track o.id) {
                  <button type="button" class="option" [attr.role]="g.max === 1 ? 'radio' : 'checkbox'"
                          [attr.aria-checked]="isChosen(g, o.id)" [disabled]="!o.available || (!isChosen(g, o.id) && isFull(g) && g.max > 1)"
                          (click)="toggle(g, o.id)">
                    <span class="tick" [class.round]="g.max === 1" aria-hidden="true"></span>
                    <span class="option-name">{{ o.name }}</span>
                    <span class="option-price tabular">{{ o.available ? extra(o.price) : 'Agotado' }}</span>
                  </button>
                }
              </fieldset>
            }

            @if (removable().length) {
              <fieldset class="group">
                <legend><span class="label">¿Le quitamos algo?</span></legend>
                <div class="chips">
                  @for (i of removable(); track i.id) {
                    <button type="button" class="chip off" [attr.aria-pressed]="removed().includes(i.id)" (click)="toggleRemoved(i.id)">
                      Sin {{ i.name.toLowerCase() }}
                    </button>
                  }
                </div>
              </fieldset>
            }

            <label class="group note">
              <span class="label">Nota para la cocina</span>
              <textarea class="field" maxlength="140" rows="2" placeholder="Ej: bien tostada, salsa aparte" [(ngModel)]="note"></textarea>
            </label>
          } @else if (hint()) {
            <p class="hint">{{ hint() }}</p>
          }
        </div>

        @if (orderable() && dish().orderable) {
          <footer class="foot">
            <div class="stepper" role="group" aria-label="Cantidad">
              <button type="button" class="icon-btn" (click)="qty.set(qty() - 1)" [disabled]="qty() <= 1" aria-label="Uno menos"><lucide-icon [img]="Minus" [size]="18" /></button>
              <span class="qty tabular" aria-live="polite">{{ qty() }}</span>
              <button type="button" class="icon-btn" (click)="qty.set(qty() + 1)" [disabled]="qty() >= 20" aria-label="Uno más"><lucide-icon [img]="Plus" [size]="18" /></button>
            </div>
            <button type="button" class="btn btn-brand add" (click)="submit()">
              @if (showMissing() && missing(); as m) { Escoge {{ m.name.toLowerCase() }} }
              @else { {{ actionLabel() }} · {{ price(total()) }} }
            </button>
          </footer>
        }
      </article>
    </div>
  `,
  styles: `
    .hero { position: relative; }
    .photo { width: 100%; aspect-ratio: 16 / 10; }
    .close { position: absolute; top: 0.75rem; right: 0.75rem; background: rgb(0 0 0 / 0.55); backdrop-filter: blur(6px); }
    .content { padding: 1.1rem 1.15rem 1.2rem; }
    .name { margin: 0; font-size: 1.6rem; line-height: 1.15; }
    .price { margin: 0.2rem 0 0; font-family: var(--font-label); font-weight: 600; font-size: 1.3rem; color: var(--brand); }
    .desc { margin: 0.6rem 0 0; color: var(--ink-muted); }
    .tags { display: flex; gap: 0.35rem; margin: 0.6rem 0 0; }
    .tag { font-size: 0.78rem; font-weight: 700; padding: 0.15rem 0.55rem; border-radius: 999px; background: color-mix(in oklab, var(--ok) 20%, transparent); color: color-mix(in oklab, var(--ok) 70%, white); }
    .ingredients { margin: 0.7rem 0 0; font-size: 0.93rem; }
    .muted { color: var(--ink-faint); margin-right: 0.3rem; }
    .allergen { color: var(--warn); display: inline-flex; align-items: center; gap: 0.15rem; }
    .soldout { margin: 1.2rem 0 0; color: var(--danger); font-size: 1.1rem; }
    .why { margin: 0.3rem 0 0; color: var(--ink-muted); font-size: 0.93rem; }
    .ran-out { text-decoration: line-through; color: var(--ink-faint); }
    .left-out { margin: 1rem 0 0; padding: 0.7rem 0.9rem; border-radius: 12px; background: color-mix(in oklab, var(--warn) 16%, transparent); color: var(--ink); font-size: 0.93rem; }
    .hint { margin: 1.2rem 0 0; padding: 0.8rem 0.9rem; border-radius: 12px; background: var(--ground-sunken); color: var(--ink-muted); font-size: 0.93rem; }

    .group { border: 0; margin: 1.3rem 0 0; padding: 0; display: block; }
    .group legend { display: flex; align-items: baseline; justify-content: space-between; width: 100%; padding: 0; margin-bottom: 0.45rem; }
    .group .label { font-size: 1.02rem; color: var(--ink); }
    .rule { font-size: 0.85rem; color: var(--ink-faint); }
    .rule.missing { color: var(--brand); font-weight: 700; }
    .option { width: 100%; display: flex; align-items: center; gap: 0.8rem; min-height: 50px; padding: 0 0.2rem; background: none; border: 0; border-bottom: 1px solid var(--line); color: var(--ink); font: inherit; text-align: left; }
    .option:disabled { opacity: 0.4; }
    .tick { flex: none; width: 22px; height: 22px; border-radius: 6px; border: 2px solid var(--ink-faint); }
    .tick.round { border-radius: 999px; }
    .option[aria-checked="true"] .tick { border-color: var(--brand); background: var(--brand); box-shadow: inset 0 0 0 3px var(--ground-raised); }
    .option-name { flex: 1; }
    .option-price { color: var(--ink-muted); font-size: 0.92rem; }
    .chips { display: flex; flex-wrap: wrap; gap: 0.45rem; }
    .note { display: grid; gap: 0.45rem; }

    .foot { position: sticky; bottom: 0; display: flex; gap: 0.7rem; align-items: center; padding: 0.85rem 1.15rem 1rem; background: var(--ground-raised); border-top: 1px solid var(--line); }
    .stepper { display: flex; align-items: center; gap: 0.35rem; }
    .stepper .icon-btn:disabled { opacity: 0.35; }
    .qty { min-width: 1.6rem; text-align: center; font-family: var(--font-label); font-size: 1.25rem; font-weight: 600; }
    .add { flex: 1; }
  `
})
export class DishSheetComponent implements OnInit, OnDestroy {
  readonly dish = input.required<Dish>();
  readonly tags = input<Tag[]>([]);
  readonly orderable = input(false);
  /** Shown instead of the order form when not orderable */
  readonly hint = input<string | null>(null);
  readonly actionLabel = input('Agregar');
  readonly close = output<void>();
  readonly add = output<CartLine>();

  readonly qty = signal(1);
  readonly selections = signal<Record<string, string[]>>({});
  readonly removed = signal<number[]>([]);
  readonly showMissing = signal(false);
  note = '';

  readonly price = price;
  readonly extra = extra;
  readonly X = X;
  readonly Plus = Plus;
  readonly Minus = Minus;
  readonly TriangleAlert = TriangleAlert;

  readonly removable = computed(() => this.dish().ingredients.filter((i) => i.removable && i.available));
  /** Ran out but the dish can go without: the API leaves them out on its own */
  readonly leftOut = computed(() => this.dish().ingredients.filter((i) => i.removable && !i.available));
  readonly leftOutNames = computed(() => this.leftOut().map((i) => i.name.toLowerCase()).join(', '));
  /** "Se acabó el pollo desmechado" / "Se acabaron las opciones de relleno" */
  readonly soldOutText = computed(() => {
    const reason = this.dish().soldOutReason;
    if (!reason || !this.dish().available) return null;
    return this.dish().modifiers.some((g) => g.name === reason)
      ? `Se acabaron las opciones de ${reason.toLowerCase()}.`
      : `Se nos acabó: ${reason.toLowerCase()}.`;
  });
  readonly tagNames = computed(() => this.dish().tagIds
    .map((id) => this.tags().find((t) => t.id === id)?.name).filter((n): n is string => !!n));
  readonly missing = computed(() => missingChoice(this.dish(), this.selections()));
  readonly total = computed(() => unitPrice(this.draft()) * this.qty());

  ngOnInit() {
    document.body.style.overflow = 'hidden';
  }

  ngOnDestroy() {
    document.body.style.overflow = '';
  }

  ruleOf(g: ModifierGroup): string {
    if (g.min === g.max) return g.min === 1 ? 'Escoge 1' : `Escoge ${g.min}`;
    if (g.min === 0) return g.max === 1 ? 'Opcional' : `Opcional · hasta ${g.max}`;
    return `Escoge de ${g.min} a ${g.max}`;
  }

  isChosen(g: ModifierGroup, optionId: string): boolean {
    return (this.selections()[g.id] ?? []).includes(optionId);
  }

  isFull(g: ModifierGroup): boolean {
    return (this.selections()[g.id] ?? []).length >= g.max;
  }

  toggle(g: ModifierGroup, optionId: string) {
    this.selections.update((all) => {
      const current = all[g.id] ?? [];
      let next: string[];
      if (current.includes(optionId)) next = current.filter((id) => id !== optionId);
      else if (g.max === 1) next = [optionId];
      else if (current.length < g.max) next = [...current, optionId];
      else next = current;
      return { ...all, [g.id]: next };
    });
  }

  toggleRemoved(id: number) {
    this.removed.update((r) => (r.includes(id) ? r.filter((x) => x !== id) : [...r, id]));
  }

  submit() {
    if (this.missing()) {
      this.showMissing.set(true);
      return;
    }
    this.add.emit(this.draft());
  }

  private draft(): CartLine {
    return newLine(this.dish(), {
      qty: this.qty(),
      note: this.note,
      removed: this.removed(),
      selections: this.selections()
    });
  }
}
