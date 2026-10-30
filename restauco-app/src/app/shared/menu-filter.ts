import { Dish, Menu } from '../core/api/models';
import { fold } from './format';

export interface MenuFilter {
  query: string;
  /** Ingredients the guest does not want */
  without: number[];
  /** Every selected tag must be on the dish */
  tags: number[];
}

export const EMPTY_FILTER: MenuFilter = { query: '', without: [], tags: [] };

export interface FilteredDish {
  dish: Dish;
  /** Unwanted ingredients the kitchen can leave out ("pídela sin queso") */
  askWithout: string[];
}

export function isFiltering(f: MenuFilter): boolean {
  return f.query.trim() !== '' || f.without.length > 0 || f.tags.length > 0;
}

/**
 * Dishes matching the filter. A dish with an unwanted ingredient still matches when that
 * ingredient can be removed: the guest just orders it without.
 */
export function filterDishes(menu: Menu, f: MenuFilter): FilteredDish[] {
  const q = fold(f.query.trim());
  const result: FilteredDish[] = [];
  for (const dish of menu.dishes) {
    if (f.tags.some((t) => !dish.tagIds.includes(t))) continue;
    const unwanted = dish.ingredients.filter((i) => f.without.includes(i.id));
    if (unwanted.some((i) => !i.removable)) continue;
    if (q && !searchText(dish).includes(q)) continue;
    result.push({ dish, askWithout: unwanted.map((i) => i.name) });
  }
  return result;
}

function searchText(dish: Dish): string {
  return fold([
    dish.name,
    dish.description ?? '',
    ...dish.ingredients.map((i) => i.name),
    ...dish.modifiers.flatMap((g) => g.options.map((o) => o.name))
  ].join(' '));
}
