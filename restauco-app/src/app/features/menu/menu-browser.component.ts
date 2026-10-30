import { Component, computed, ElementRef, input, output, signal, viewChildren } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Search, SlidersHorizontal, X, TriangleAlert } from 'lucide-angular';
import { Category, Dish, Menu } from '../../core/api/models';
import { DishPhotoComponent } from '../../shared/dish-photo.component';
import { price } from '../../shared/format';
import { EMPTY_FILTER, FilteredDish, filterDishes, isFiltering, MenuFilter } from '../../shared/menu-filter';

interface Section {
  category: Category;
  dishes: FilteredDish[];
}

/**
 * The menu as guests browse it: category tabs, search, and "sin..." ingredient filters.
 * Used read-only on /menu and for ordering on /mesa/:token.
 */
@Component({
  selector: 'app-menu-browser',
  imports: [FormsModule, LucideAngularModule, DishPhotoComponent],
  template: `
    <div class="bar" [style.top]="stickyTop()">
      <div class="search-row">
        <label class="search">
          <lucide-icon [img]="Search" [size]="18" aria-hidden="true" />
          <input type="search" placeholder="Buscar arepa, ingrediente…" [ngModel]="filter().query"
                 (ngModelChange)="patch({ query: $event })" aria-label="Buscar en la carta" />
        </label>
        <button type="button" class="filter-btn" [class.active]="filterCount() > 0" (click)="filtersOpen.set(true)"
                [attr.aria-label]="'Filtrar por ingredientes' + (filterCount() ? ' (' + filterCount() + ' activos)' : '')">
          <lucide-icon [img]="SlidersHorizontal" [size]="18" aria-hidden="true" />
          <span class="label">Filtrar</span>
          @if (filterCount()) { <span class="badge">{{ filterCount() }}</span> }
        </button>
      </div>
      @if (!filtering()) {
        <nav class="cats" aria-label="Categorías">
          @for (s of sections(); track s.category.id) {
            <button type="button" class="chip" [class.on]="activeCategory() === s.category.id" (click)="jump(s.category.id)">
              {{ s.category.name }}
            </button>
          }
        </nav>
      } @else {
        <div class="active-filters">
          <span>{{ resultCount() }} {{ resultCount() === 1 ? 'plato' : 'platos' }}</span>
          @for (id of filter().without; track id) {
            <button type="button" class="pill" (click)="toggleWithout(id)">sin {{ ingredientName(id) }} <lucide-icon [img]="X" [size]="14" /></button>
          }
          @for (id of filter().tags; track id) {
            <button type="button" class="pill" (click)="toggleTag(id)">{{ tagName(id) }} <lucide-icon [img]="X" [size]="14" /></button>
          }
          <button type="button" class="clear" (click)="clear()">Limpiar</button>
        </div>
      }
    </div>

    @for (s of sections(); track s.category.id) {
      <section class="section" #section [attr.data-category]="s.category.id" [attr.aria-labelledby]="'cat-' + s.category.id">
        <header>
          <h2 class="display" [id]="'cat-' + s.category.id">{{ s.category.name }}</h2>
          @if (s.category.description) { <p>{{ s.category.description }}</p> }
        </header>
        <ul class="dishes">
          @for (f of s.dishes; track f.dish.id) {
            <li>
              <button type="button" class="dish" [class.soldout]="!f.dish.orderable" (click)="open.emit(f.dish)">
                <app-dish-photo class="photo" [url]="f.dish.imageUrl" [seed]="f.dish.id" />
                <span class="body">
                  <span class="name">{{ f.dish.name }}</span>
                  @if (f.dish.description) { <span class="desc">{{ f.dish.description }}</span> }
                  <span class="meta">
                    @for (t of tagsOf(f.dish); track t) { <span class="tag">{{ t }}</span> }
                    @if (allergens(f.dish); as a) {
                      <span class="allergen"><lucide-icon [img]="TriangleAlert" [size]="13" aria-hidden="true" /> {{ a }}</span>
                    }
                  </span>
                  @if (f.askWithout.length) { <span class="ask">Pídela sin {{ f.askWithout.join(', ').toLowerCase() }}</span> }
                  <span class="price-row">
                    <span class="price tabular">{{ price(f.dish.price) }}</span>
                    @if (!f.dish.orderable) { <span class="out label">Agotado</span> }
                    @else if (orderable()) { <span class="add" aria-hidden="true">+</span> }
                  </span>
                </span>
              </button>
            </li>
          }
        </ul>
      </section>
    } @empty {
      <div class="empty">
        <p>Ningún plato coincide con tu búsqueda.</p>
        <button type="button" class="btn btn-ghost btn-sm" (click)="clear()">Ver toda la carta</button>
      </div>
    }

    @if (filtersOpen()) {
      <div class="scrim" (click)="filtersOpen.set(false)">
        <div class="sheet filters" role="dialog" aria-modal="true" aria-labelledby="filters-title" (click)="$event.stopPropagation()">
          <header class="sheet-head">
            <h2 id="filters-title" class="display">Filtrar la carta</h2>
            <button type="button" class="icon-btn" (click)="filtersOpen.set(false)" aria-label="Cerrar"><lucide-icon [img]="X" [size]="20" /></button>
          </header>
          <div class="sheet-body">
            <h3 class="label">No quiero…</h3>
            <p class="hint">Ocultamos los platos que lo traen, salvo los que se pueden pedir sin ese ingrediente.</p>
            <div class="chips">
              @for (i of menu().ingredients; track i.id) {
                <button type="button" class="chip off" [attr.aria-pressed]="filter().without.includes(i.id)" (click)="toggleWithout(i.id)">
                  {{ i.name }} @if (i.allergen) { <lucide-icon [img]="TriangleAlert" [size]="13" aria-label="alérgeno" /> }
                </button>
              }
            </div>
            @if (menu().tags.length) {
              <h3 class="label">Solo…</h3>
              <div class="chips">
                @for (t of menu().tags; track t.id) {
                  <button type="button" class="chip" [attr.aria-pressed]="filter().tags.includes(t.id)" (click)="toggleTag(t.id)">{{ t.name }}</button>
                }
              </div>
            }
          </div>
          <footer class="sheet-foot">
            <button type="button" class="btn btn-ghost" (click)="clear()" [disabled]="!filterCount()">Limpiar</button>
            <button type="button" class="btn btn-brand grow" (click)="filtersOpen.set(false)">
              Ver {{ resultCount() }} {{ resultCount() === 1 ? 'plato' : 'platos' }}
            </button>
          </footer>
        </div>
      </div>
    }
  `,
  styles: `
    :host { display: block; }
    .bar { position: sticky; z-index: 20; background: color-mix(in oklab, var(--ground) 92%, transparent); backdrop-filter: blur(10px); padding: 0.75rem 0 0.6rem; border-bottom: 1px solid var(--line); }
    .search-row { display: flex; gap: 0.5rem; padding: 0 1rem; }
    .search { flex: 1; display: flex; align-items: center; gap: 0.5rem; min-height: 46px; padding: 0 0.9rem; border-radius: 999px; background: var(--ground-raised); border: 1.5px solid var(--line); color: var(--ink-faint); }
    .search:focus-within { border-color: var(--brand); }
    .search input { flex: 1; min-width: 0; background: none; border: 0; outline: none; color: var(--ink); font: inherit; }
    .filter-btn { display: inline-flex; align-items: center; gap: 0.4rem; min-height: 46px; padding: 0 0.95rem; border-radius: 999px; border: 1.5px solid var(--line); background: var(--ground-raised); color: var(--ink); }
    .filter-btn.active { border-color: var(--brand); }
    .search-row > * { min-width: 0; }
    .filter-btn { flex: none; }
    @media (max-width: 420px) { .filter-btn .label { display: none; } }
    .cats { display: flex; gap: 0.45rem; overflow-x: auto; padding: 0.65rem 1rem 0.1rem; scrollbar-width: none; }
    .active-filters { display: flex; flex-wrap: wrap; align-items: center; gap: 0.4rem; padding: 0.65rem 1rem 0; font-size: 0.9rem; color: var(--ink-muted); }
    .pill { display: inline-flex; align-items: center; gap: 0.25rem; min-height: 32px; padding: 0 0.7rem; border-radius: 999px; border: 0; background: rgb(255 255 255 / 0.08); color: var(--ink); font: inherit; font-size: 0.88rem; }
    .clear { background: none; border: 0; color: var(--brand); font: inherit; font-weight: 700; min-height: 32px; }

    .section { padding: 1.6rem 1rem 0.4rem; scroll-margin-top: var(--browser-offset, 130px); }
    .section header { margin-bottom: 0.9rem; }
    .section h2 { margin: 0; font-size: 1.75rem; color: var(--brand); }
    .section header p { margin: 0.15rem 0 0; color: var(--ink-muted); }
    .dishes { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.75rem; }
    @media (min-width: 720px) { .dishes { grid-template-columns: repeat(2, 1fr); } }
    @media (min-width: 1100px) { .dishes { grid-template-columns: repeat(3, 1fr); } }

    .dish { width: 100%; height: 100%; display: grid; grid-template-columns: 1fr 112px; gap: 0.9rem; align-items: start; text-align: left; padding: 0.85rem; border-radius: 18px; border: 1px solid var(--line); background: var(--ground-raised); color: inherit; font: inherit; transition: border-color 160ms, transform 120ms; }
    .dish:hover { border-color: color-mix(in oklab, var(--brand) 45%, transparent); }
    .dish:active { transform: scale(0.99); }
    .photo { grid-column: 2; grid-row: 1; width: 112px; height: 112px; border-radius: 14px; }
    .body { grid-column: 1; grid-row: 1; display: flex; flex-direction: column; gap: 0.3rem; min-width: 0; }
    .name { font-family: var(--font-label); text-transform: uppercase; font-weight: 600; font-size: 1.12rem; letter-spacing: 0.02em; line-height: 1.2; }
    .desc { color: var(--ink-muted); font-size: 0.92rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
    .meta { display: flex; flex-wrap: wrap; gap: 0.35rem; }
    .meta:empty { display: none; }
    .tag { font-size: 0.75rem; font-weight: 700; padding: 0.1rem 0.5rem; border-radius: 999px; background: color-mix(in oklab, var(--ok) 20%, transparent); color: color-mix(in oklab, var(--ok) 70%, white); }
    .allergen { display: inline-flex; align-items: center; gap: 0.2rem; font-size: 0.78rem; color: var(--warn); }
    .ask { font-size: 0.85rem; color: var(--brand-bright); font-weight: 600; }
    .price-row { display: flex; align-items: center; gap: 0.6rem; margin-top: auto; padding-top: 0.2rem; }
    .price { font-family: var(--font-label); font-weight: 600; font-size: 1.15rem; color: var(--brand); }
    .add { margin-left: auto; width: 34px; height: 34px; border-radius: 999px; display: grid; place-items: center; background: var(--brand); color: var(--brand-ink); font-size: 1.4rem; font-weight: 700; line-height: 1; }
    .out { font-size: 0.85rem; color: var(--danger); }
    .soldout .photo, .soldout .name, .soldout .price { opacity: 0.45; }

    .sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 1.1rem 1.1rem 0.4rem; }
    .sheet-head h2 { margin: 0; font-size: 1.6rem; }
    .sheet-body { padding: 0 1.1rem 1rem; }
    .sheet-body h3 { margin: 1.1rem 0 0.3rem; font-size: 1rem; color: var(--brand); }
    .hint { margin: 0 0 0.7rem; color: var(--ink-faint); font-size: 0.9rem; }
    .chips { display: flex; flex-wrap: wrap; gap: 0.45rem; }
    .sheet-foot { position: sticky; bottom: 0; display: flex; gap: 0.6rem; padding: 0.9rem 1.1rem 1.1rem; background: var(--ground-raised); border-top: 1px solid var(--line); }
    .grow { flex: 1; }
  `
})
export class MenuBrowserComponent {
  readonly menu = input.required<Menu>();
  /** Shows the "+" affordance: tapping a dish adds it to an order */
  readonly orderable = input(false);
  /** Offset of the sticky bar (below a page header) */
  readonly stickyTop = input('0px');
  readonly open = output<Dish>();

  readonly filter = signal<MenuFilter>(EMPTY_FILTER);
  readonly filtersOpen = signal(false);
  readonly activeCategory = signal<number | null>(null);
  readonly price = price;
  readonly Search = Search;
  readonly SlidersHorizontal = SlidersHorizontal;
  readonly X = X;
  readonly TriangleAlert = TriangleAlert;

  private sectionEls = viewChildren<ElementRef<HTMLElement>>('section');

  readonly filtering = computed(() => isFiltering(this.filter()));
  readonly filterCount = computed(() => this.filter().without.length + this.filter().tags.length);

  readonly sections = computed<Section[]>(() => {
    const menu = this.menu();
    const matches = filterDishes(menu, this.filter());
    return menu.categories
      .map((category) => ({ category, dishes: matches.filter((m) => m.dish.categoryId === category.id) }))
      .filter((s) => s.dishes.length > 0);
  });
  readonly resultCount = computed(() => this.sections().reduce((n, s) => n + s.dishes.length, 0));

  private tagNames = computed(() => new Map(this.menu().tags.map((t) => [t.id, t.name])));

  patch(change: Partial<MenuFilter>) {
    this.filter.update((f) => ({ ...f, ...change }));
  }

  toggleWithout(id: number) {
    const without = this.filter().without;
    this.patch({ without: without.includes(id) ? without.filter((x) => x !== id) : [...without, id] });
  }

  toggleTag(id: number) {
    const tags = this.filter().tags;
    this.patch({ tags: tags.includes(id) ? tags.filter((x) => x !== id) : [...tags, id] });
  }

  clear() {
    this.filter.set(EMPTY_FILTER);
  }

  jump(categoryId: number) {
    this.activeCategory.set(categoryId);
    const el = this.sectionEls().find((s) => s.nativeElement.dataset['category'] === String(categoryId));
    el?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  ingredientName(id: number): string {
    return this.menu().ingredients.find((i) => i.id === id)?.name.toLowerCase() ?? '';
  }

  tagName(id: number): string {
    return this.tagNames().get(id) ?? '';
  }

  tagsOf(dish: Dish): string[] {
    return dish.tagIds.map((id) => this.tagNames().get(id)).filter((n): n is string => !!n);
  }

  allergens(dish: Dish): string {
    return dish.ingredients.filter((i) => i.allergen).map((i) => i.name).join(', ');
  }
}
