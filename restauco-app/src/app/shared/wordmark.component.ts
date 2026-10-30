import { Component, computed, inject, input } from '@angular/core';
import { TenantService } from '../core/api/tenant.service';

/**
 * The restaurant name as a logo: the first word big and yellow, the rest underneath
 * (Toche's / Arepas). Generic enough for any tenant until it has a real logo file.
 */
@Component({
  selector: 'app-wordmark',
  template: `
    <span class="mark" [class.sm]="size() === 'sm'" [class.lg]="size() === 'lg'">
      <span class="first display">{{ parts().first }}</span>
      @if (parts().rest) { <span class="rest display">{{ parts().rest }}</span> }
    </span>
  `,
  styles: `
    .mark { display: inline-flex; flex-direction: column; line-height: 0.88; }
    .first { color: var(--brand); font-size: 1.9rem; text-shadow: 0 2px 0 rgb(0 0 0 / 0.55); text-transform: uppercase; }
    .rest { color: var(--ink); font-size: 1.15rem; padding-left: 0.15em; }
    .sm .first { font-size: 1.35rem; }
    .sm .rest { font-size: 0.85rem; }
    .lg .first { font-size: clamp(3.4rem, 15vw, 6.5rem); }
    .lg .rest { font-size: clamp(1.8rem, 7vw, 3.1rem); }
  `
})
export class WordmarkComponent {
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  private tenant = inject(TenantService);

  readonly parts = computed(() => {
    const name = this.tenant.tenant()?.name ?? '';
    const space = name.indexOf(' ');
    return space < 0 ? { first: name, rest: '' } : { first: name.slice(0, space), rest: name.slice(space + 1) };
  });
}
