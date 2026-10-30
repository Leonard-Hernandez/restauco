import { inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ApiService } from './api.service';
import { Tenant } from './models';

/** Theme tokens a restaurant may override from its branding (see src/themes). */
const THEME_TOKENS = ['ground', 'ground-raised', 'ground-sunken', 'ink', 'ink-muted', 'brand', 'brand-bright', 'brand-ink'];

/** The restaurant this site belongs to (name, contact, branding). */
@Injectable({ providedIn: 'root' })
export class TenantService {

  private api = inject(ApiService);
  private title = inject(Title);

  readonly tenant = signal<Tenant | null>(null);

  load() {
    this.api.tenant().subscribe({
      next: (tenant) => {
        this.tenant.set(tenant);
        this.title.setTitle(tenant.name);
        this.applyTheme(tenant.branding.theme);
      },
      error: () => this.tenant.set(null)
    });
  }

  /** wa.me link to the restaurant's WhatsApp, optionally with a prefilled message. */
  whatsappLink(text?: string): string | null {
    const phone = this.tenant()?.contactPhone?.replace(/\D/g, '');
    if (!phone) return null;
    return `https://wa.me/${phone}${text ? `?text=${encodeURIComponent(text)}` : ''}`;
  }

  // The theme file is the base; branding.theme lets a restaurant adjust colors without a build.
  private applyTheme(theme: Record<string, string> | undefined) {
    if (!theme) return;
    const root = document.documentElement.style;
    for (const [token, value] of Object.entries(theme)) {
      if (THEME_TOKENS.includes(token) && /^#[0-9a-f]{3,8}$/i.test(value)) {
        root.setProperty(`--${token}`, value);
      }
    }
  }
}
