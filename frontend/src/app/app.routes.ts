import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./auth/auth-layout').then((m) => m.AuthLayout),
    children: [
      { path: 'login', loadComponent: () => import('./auth/login-form').then((m) => m.LoginForm) },
      { path: 'register', loadComponent: () => import('./auth/register-form').then((m) => m.RegisterForm) },
      { path: '**', redirectTo: 'login' },
    ],
  },
];
