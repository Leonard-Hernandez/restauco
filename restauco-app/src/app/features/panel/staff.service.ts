import { computed, Injectable, signal } from '@angular/core';
import { Role, Staff } from '../../core/api/models';

/** Who is using the panel, and what they may open. */
@Injectable()
export class StaffContext {
  readonly staff = signal<Staff | null>(null);
  readonly role = computed(() => this.staff()?.role ?? null);
  readonly isOwner = computed(() => this.role() === 'OWNER');
  readonly canFloor = computed(() => this.role() === 'OWNER' || this.role() === 'WAITER');
  readonly canKitchen = computed(() => this.role() === 'OWNER' || this.role() === 'COOK');

  /** Landing screen of each role */
  home(role: Role): string {
    return role === 'COOK' ? 'cocina' : 'mesas';
  }
}
