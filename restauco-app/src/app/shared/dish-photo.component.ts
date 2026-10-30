import { Component, computed, input, signal } from '@angular/core';
import { imageSrc } from '../core/api/api.service';

/** Dish photo, or a drawn arepa while the restaurant has not uploaded one. */
@Component({
  selector: 'app-dish-photo',
  template: `
    @if (src() && !failed()) {
      <img [src]="src()" [alt]="alt()" loading="lazy" decoding="async" (error)="failed.set(true)" />
    } @else {
      <div class="placeholder" [attr.role]="alt() ? 'img' : null" [attr.aria-label]="alt() || null" [attr.aria-hidden]="alt() ? null : 'true'">
        <svg viewBox="0 0 120 90" aria-hidden="true">
          <ellipse cx="60" cy="58" rx="46" ry="16" fill="#e9b65a" />
          <path d="M16 52 q10 -10 22 -2 q10 -10 22 -2 q10 -10 22 -2 q12 -8 22 2 l0 6 l-88 0z" [attr.fill]="filling()" />
          <ellipse cx="60" cy="40" rx="46" ry="18" fill="#f3c873" />
          <ellipse cx="44" cy="35" rx="6" ry="2.2" fill="#d79a3c" opacity=".6" />
          <ellipse cx="70" cy="42" rx="5" ry="2" fill="#d79a3c" opacity=".6" />
          <ellipse cx="78" cy="32" rx="4" ry="1.6" fill="#d79a3c" opacity=".5" />
        </svg>
      </div>
    }
  `,
  styles: `
    :host { display: block; overflow: hidden; background: var(--ground-sunken); }
    img { width: 100%; height: 100%; object-fit: cover; display: block; }
    .placeholder {
      width: 100%; height: 100%; display: grid; place-items: center;
      background: radial-gradient(circle at 50% 60%, rgb(255 194 26 / 0.16), transparent 65%), var(--ground-sunken);
    }
    svg { width: 62%; max-width: 160px; }
  `
})
export class DishPhotoComponent {
  readonly url = input<string | null>(null);
  /** Empty when the photo sits inside a control already named after the dish */
  readonly alt = input('');
  /** Varies the drawn filling so placeholders do not all look the same */
  readonly seed = input(0);

  readonly failed = signal(false);
  readonly src = computed(() => imageSrc(this.url()));
  readonly filling = computed(() => ['#7a4a2a', '#c9a15a', '#5e8c3a', '#8f2f2a', '#3b2a1e'][this.seed() % 5]);
}
