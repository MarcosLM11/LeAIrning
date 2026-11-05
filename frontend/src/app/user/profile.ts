import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { UserService } from './user.service';

@Component({
  selector: 'app-profile',
  host: { class: 'mx-auto block max-w-[760px]' },
  imports: [DatePipe],
  template: `
    <header class="mb-6">
      <h1 class="text-2xl font-semibold tracking-tight">Perfil</h1>
      <p class="mt-1 text-sm text-mute">Gestiona tu cuenta y tus preferencias.</p>
    </header>

    @if (me(); as u) {
      <section class="mb-5 flex items-center gap-5 rounded-2xl border border-line bg-card p-6">
        <div class="relative shrink-0">
          <span class="grid size-[72px] place-items-center rounded-full bg-brand text-[28px] font-semibold text-white">{{ u.name.charAt(0).toUpperCase() }}</span>
          <!-- Sin función hasta que exista el endpoint de foto -->
          <button title="Cambiar foto de perfil" aria-label="Cambiar foto de perfil"
                  class="absolute -bottom-1 -right-1 grid size-[30px] place-items-center rounded-full border-2 border-card bg-ink text-card hover:bg-brand hover:text-white">
            <svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z" /><path d="m15 5 4 4" />
            </svg>
          </button>
        </div>
        <div>
          <p class="text-xl font-semibold tracking-tight">{{ u.name }}</p>
          <p class="mt-1 flex flex-wrap gap-x-3.5 text-[13px] text-mute">
            <span>&#64;{{ u.username }}</span><span>{{ u.email }}</span><span>Miembro desde {{ u.createdAt | date: 'MMM y' }}</span>
          </p>
        </div>
      </section>

      <section class="mb-5 rounded-2xl border border-line bg-card p-6">
        <h2 class="text-[15px] font-semibold">Datos personales</h2>
        <p class="mb-4 mt-0.5 text-[13px] text-mute">Así te ven en la aplicación.</p>
        <div class="grid gap-4 sm:grid-cols-2">
          <div><label class="mb-1.5 block text-[13px] font-medium">Nombre</label><input class="input" [value]="u.name" disabled /></div>
          <div><label class="mb-1.5 block text-[13px] font-medium">Usuario</label><input class="input" [value]="u.username" disabled /></div>
          <div class="sm:col-span-2">
            <label class="mb-1.5 block text-[13px] font-medium">Correo electrónico</label>
            <input class="input" [value]="u.email" disabled />
            <p class="mt-1 text-xs text-mute">Los cuestionarios por email se envían a esta dirección.</p>
          </div>
        </div>
      </section>
    }

    <section class="flex items-center justify-between gap-4 rounded-2xl border border-line bg-card p-6">
      <div>
        <h2 class="text-[15px] font-semibold">Apariencia</h2>
        <p class="mt-0.5 text-[13px] text-mute">Elige el tema de la interfaz.</p>
      </div>
      <div class="inline-flex gap-0.5 rounded-[10px] border border-line p-[3px]">
        @for (t of themes; track t.value) {
          <button (click)="setTheme(t.value)" class="h-8 rounded-lg px-3.5 text-sm font-medium"
                  [class]="theme() === t.value ? 'bg-app text-ink' : 'text-mute'">{{ t.label }}</button>
        }
      </div>
    </section>
  `,
})

export class Profile {
  me = inject(UserService).me;
  themes = [{ value: 'light', label: 'Claro' }, { value: 'dark', label: 'Oscuro' }];
  theme = signal(document.documentElement.dataset['theme'] ?? 'light');

  setTheme(t: string) {
    this.theme.set(t);
    document.documentElement.dataset['theme'] = t;
    localStorage.setItem('theme', t);
  }
}
