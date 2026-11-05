import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ThemeToggle } from '../theme-toggle';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ThemeToggle],
  template: `
    <main class="mx-auto flex min-h-screen w-full max-w-[380px] flex-col items-center gap-6 px-4 pt-32">
      <app-theme-toggle class="absolute right-4 top-4" />
      <div class="flex items-center gap-2">
        <img src="logo.svg" alt="" class="size-6" />
        <span class="text-[17px] font-semibold tracking-tight">LeAIrning</span>
      </div>
      <div class="w-full rounded-2xl border border-line bg-card p-6">
        <nav class="mb-6 flex gap-1 rounded-[10px] border border-line bg-app p-1 text-center text-sm font-medium text-mute">
          @for (t of tabs; track t.path) {
            <a [routerLink]="t.path" routerLinkActive="bg-card text-ink shadow-sm" class="flex-1 rounded-lg py-1.5">{{ t.label }}</a>
          }
        </nav>
        <router-outlet />
      </div>
    </main>
  `,
})

export class AuthLayout {
  tabs = [
    { path: '/login', label: 'Iniciar sesión' },
    { path: '/register', label: 'Crear cuenta' },
  ];
}
