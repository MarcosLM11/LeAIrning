import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./shell/shell').then((m) => m.Shell),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('./home/home').then((m) => m.Home) },
      { path: 'documents', loadComponent: () => import('./documents/documents').then((m) => m.Documents) },
      { path: 'chat', loadComponent: () => import('./chat/chat').then((m) => m.Chat) },
      { path: 'quizzes', loadComponent: () => import('./quizzes/quizzes').then((m) => m.Quizzes) },
      { path: 'profile', loadComponent: () => import('./user/profile').then((m) => m.Profile) },
    ],
  },
  {
    path: '',
    loadComponent: () => import('./auth/auth-layout').then((m) => m.AuthLayout),
    children: [
      { path: 'login', loadComponent: () => import('./auth/login-form').then((m) => m.LoginForm) },
      { path: 'register', loadComponent: () => import('./auth/register-form').then((m) => m.RegisterForm) },
    ],
  },
  { path: '**', redirectTo: '' },
];
