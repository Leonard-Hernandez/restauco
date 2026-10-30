import { Component, input } from '@angular/core';
import { TicketItem } from '../../core/api/models';

/** The lines of a ticket, with what the kitchen must not miss (removals, notes) highlighted. */
@Component({
  selector: 'app-ticket-items',
  template: `
    <ul [class.lg]="size() === 'lg'">
      @for (i of items(); track $index) {
        <li>
          <span class="qty tabular">{{ i.qty }}×</span>
          <span class="body">
            <span class="name">{{ i.name }}</span>
            @if (i.modifiers.length) { <span class="mods">{{ modifiers(i) }}</span> }
            @if (i.removed.length) { <span class="removed">Sin {{ i.removed.join(', ').toLowerCase() }}</span> }
            @if (i.note) { <span class="note">“{{ i.note }}”</span> }
          </span>
        </li>
      }
    </ul>
  `,
  styles: `
    ul { list-style: none; margin: 0; padding: 0; display: grid; gap: 0.45rem; }
    li { display: flex; gap: 0.55rem; align-items: baseline; }
    .qty { flex: none; min-width: 1.9rem; font-family: var(--font-label); font-weight: 600; color: var(--brand); }
    .body { display: flex; flex-direction: column; gap: 0.1rem; min-width: 0; }
    .name { font-family: var(--font-label); text-transform: uppercase; font-weight: 500; letter-spacing: 0.02em; }
    .mods { color: var(--ink-muted); font-size: 0.9rem; }
    .removed { align-self: flex-start; font-weight: 800; font-size: 0.82rem; text-transform: uppercase; letter-spacing: 0.04em; color: #fff; background: var(--danger); padding: 0.05rem 0.45rem; border-radius: 6px; }
    .note { color: var(--brand-bright); font-weight: 700; font-size: 0.92rem; }
    .lg { gap: 0.7rem; }
    .lg .qty, .lg .name { font-size: 1.3rem; }
    .lg .mods { font-size: 1.05rem; }
    .lg .removed { font-size: 0.95rem; }
    .lg .note { font-size: 1.05rem; }
  `
})
export class TicketItemsComponent {
  readonly items = input.required<TicketItem[]>();
  readonly size = input<'md' | 'lg'>('md');

  modifiers(item: TicketItem): string {
    return item.modifiers.map((m) => m.name).join(', ');
  }
}
