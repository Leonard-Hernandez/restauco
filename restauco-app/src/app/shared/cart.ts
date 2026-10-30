import { Dish, ModifierGroup, OrderLine, TicketItem } from '../core/api/models';

/** A line being built, before it is sent. Prices here are only for display; the API reprices. */
export interface CartLine {
  key: string;
  dish: Dish;
  qty: number;
  note: string;
  /** Ingredient ids to leave out */
  removed: number[];
  /** modifier group id -> option ids */
  selections: Record<string, string[]>;
}

let seq = 0;

export function newLine(dish: Dish, init: Partial<Omit<CartLine, 'key' | 'dish'>> = {}): CartLine {
  return { key: `l${++seq}`, dish, qty: 1, note: '', removed: [], selections: {}, ...init };
}

export function unitPrice(line: CartLine): number {
  let total = line.dish.price;
  for (const group of line.dish.modifiers) {
    for (const id of line.selections[group.id] ?? []) {
      total += group.options.find((o) => o.id === id)?.price ?? 0;
    }
  }
  return total;
}

export function lineTotal(line: CartLine): number {
  return unitPrice(line) * line.qty;
}

export function cartTotal(lines: CartLine[]): number {
  return lines.reduce((sum, l) => sum + lineTotal(l), 0);
}

export function cartCount(lines: CartLine[]): number {
  return lines.reduce((sum, l) => sum + l.qty, 0);
}

/** The dish can go straight to the cart without asking anything. */
export function needsChoices(dish: Dish): boolean {
  return dish.modifiers.some((g) => g.min > 0);
}

/** Why the options are not complete yet, or null when they are. */
export function missingChoice(dish: Dish, selections: Record<string, string[]>): ModifierGroup | null {
  return dish.modifiers.find((g) => {
    const n = (selections[g.id] ?? []).length;
    return n < g.min || n > g.max;
  }) ?? null;
}

/** "Chicharrón, Reina pepiada · sin Aguacate" */
export function describe(line: CartLine): string {
  const parts: string[] = [];
  const options = line.dish.modifiers.flatMap((g) =>
    (line.selections[g.id] ?? []).map((id) => g.options.find((o) => o.id === id)?.name).filter(Boolean) as string[]);
  if (options.length) parts.push(options.join(', '));
  const removed = line.dish.ingredients.filter((i) => line.removed.includes(i.id)).map((i) => i.name);
  if (removed.length) parts.push(`sin ${removed.join(', ')}`);
  return parts.join(' · ');
}

export function toOrderLine(line: CartLine): OrderLine {
  return {
    dishId: line.dish.id,
    qty: line.qty,
    note: line.note.trim() || null,
    removedIngredientIds: line.removed,
    selections: line.selections
  };
}

/** Two identical lines (same dish and choices) merge into one. */
export function addToCart(lines: CartLine[], line: CartLine): CartLine[] {
  const same = lines.find((l) => l.dish.id === line.dish.id && l.note === line.note
    && sameSet(l.removed, line.removed) && sameSelections(l.selections, line.selections));
  if (same) {
    return lines.map((l) => (l === same ? { ...l, qty: Math.min(20, l.qty + line.qty) } : l));
  }
  return [...lines, line];
}

function sameSet<T>(a: T[], b: T[]): boolean {
  return a.length === b.length && a.every((x) => b.includes(x));
}

function sameSelections(a: Record<string, string[]>, b: Record<string, string[]>): boolean {
  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  return [...keys].every((k) => sameSet(a[k] ?? [], b[k] ?? []));
}

/**
 * A sent line back into a cart line ("otra igual"). Tickets keep names, not ids, so options and
 * removals are matched by name against today's menu; anything that no longer exists is dropped.
 */
export function lineFromTicketItem(item: TicketItem, dish: Dish): CartLine {
  const selections: Record<string, string[]> = {};
  for (const m of item.modifiers) {
    const group = dish.modifiers.find((g) => g.name === m.group);
    const option = group?.options.find((o) => o.name === m.name);
    if (group && option) selections[group.id] = [...(selections[group.id] ?? []), option.id];
  }
  const removed = dish.ingredients.filter((i) => i.removable && item.removed.includes(i.name)).map((i) => i.id);
  return newLine(dish, { qty: item.qty, note: item.note ?? '', removed, selections });
}
