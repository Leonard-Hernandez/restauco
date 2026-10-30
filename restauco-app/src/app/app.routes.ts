import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/landing/landing.component') },
  { path: 'menu', loadComponent: () => import('./features/menu/menu-page.component') },
  { path: 'domicilio', loadComponent: () => import('./features/delivery/delivery-order.component') },
  { path: 'domicilio/:token', loadComponent: () => import('./features/delivery/delivery-track.component') },
  { path: 'mesa/:token', loadComponent: () => import('./features/table/table-order.component') },
  { path: 'auth/callback', loadComponent: () => import('./features/auth/auth-callback.component') },
  { path: 'panel', canActivate: [authGuard], loadChildren: () => import('./features/panel/panel.routes') },
  { path: '**', redirectTo: '' }
];
