import { Component, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TenantService } from './core/api/tenant.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent implements OnInit {
  private tenant = inject(TenantService);

  ngOnInit() {
    // The silent-renewal iframe only needs the callback page.
    if (window.parent === window) {
      this.tenant.load();
    }
  }
}
