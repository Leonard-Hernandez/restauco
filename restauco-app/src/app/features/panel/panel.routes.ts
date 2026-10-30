import { Routes } from '@angular/router';
import PanelLayoutComponent from './panel-layout.component';

export default [
  {
    path: '',
    component: PanelLayoutComponent,
    children: [
      { path: 'mesas', loadComponent: () => import('./floor/floor.component') },
      { path: 'mesas/:id', loadComponent: () => import('./floor/table.component') },
      { path: 'domicilios', loadComponent: () => import('./floor/deliveries.component') },
      { path: 'domicilio-ajustes', loadComponent: () => import('./admin/admin-delivery.component') },
      { path: 'cocina', loadComponent: () => import('./kitchen/kitchen.component') },
      { path: 'estadisticas', loadComponent: () => import('./admin/admin-stats.component') },
      { path: 'carta', loadComponent: () => import('./admin/admin-menu.component') },
      { path: 'ingredientes', loadComponent: () => import('./admin/admin-catalog.component') },
      { path: 'qr', loadComponent: () => import('./admin/admin-tables.component') },
      { path: 'pagos', loadComponent: () => import('./admin/admin-payments.component') },
      { path: 'equipo', loadComponent: () => import('./admin/admin-team.component') }
    ]
  }
] satisfies Routes;
