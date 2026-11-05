import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ThemeToggle } from '../theme-toggle';
import { UserService } from '../user/user.service';

// Iconos lucide (trazos SVG).
const NAV = [
  { path: '/', label: 'Inicio', icon: ['m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z', 'M9 22V12h6v10'] },
  { path: '/documents', label: 'Documentos', icon: ['M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7z', 'M14 2v4a2 2 0 0 0 2 2h4', 'M16 13H8', 'M16 17H8'] },
  { path: '/chat', label: 'Chat', icon: ['M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z'] },
  { path: '/quizzes', label: 'Cuestionarios', icon: ['m3 17 2 2 4-4', 'm3 7 2 2 4-4', 'M13 6h8', 'M13 12h8', 'M13 18h8'] },
];

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ThemeToggle],
  template: `
    <div class="flex min-h-screen">
      <aside class="sticky top-0 flex h-screen shrink-0 flex-col justify-between border-r border-line bg-card transition-[width]"
             [class]="collapsed() ? 'w-16' : 'w-60'">
        <div>
          <div class="flex items-center gap-2 py-6" [class]="collapsed() ? 'flex-col px-0' : 'justify-between px-5'">
            <div class="flex items-center gap-2">
              <img src="logo.svg" alt="" class="size-6" />
              @if (!collapsed()) { <span class="text-[15px] font-semibold tracking-tight">LeAIrning</span> }
            </div>
            <button (click)="toggle()" [attr.aria-label]="collapsed() ? 'Expandir barra lateral' : 'Compactar barra lateral'"
                    class="grid size-8 place-items-center rounded-[10px] text-mute hover:bg-app hover:text-ink">
              <svg class="size-[18px]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="18" height="18" x="3" y="3" rx="2" /><path d="M9 3v18" />
              </svg>
            </button>
          </div>
          <nav class="space-y-1 px-3">
            @for (n of nav; track n.path) {
              <a [routerLink]="n.path" routerLinkActive="bg-app text-ink" [routerLinkActiveOptions]="{ exact: true }" [title]="n.label"
                 class="flex h-9 items-center gap-3 rounded-[10px] px-3 text-sm font-medium text-mute hover:text-ink">
                <svg class="size-[18px] shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  @for (d of n.icon; track d) { <path [attr.d]="d" /> }
                </svg>
                @if (!collapsed()) { {{ n.label }} }
              </a>
            }
          </nav>
        </div>
        <div class="flex items-center gap-3 pb-5" [class]="collapsed() ? 'flex-col' : 'justify-between px-5'">
          <a routerLink="/profile" routerLinkActive="bg-app" title="Perfil" class="-m-1 flex items-center gap-2.5 rounded-[10px] p-1 hover:bg-app">
            <span class="grid size-8 place-items-center rounded-full bg-brand text-xs font-semibold text-white">{{ me()?.name?.charAt(0)?.toUpperCase() }}</span>
            @if (!collapsed()) { <span class="text-[13px] font-medium">{{ me()?.name }}</span> }
          </a>
          <app-theme-toggle />
        </div>
      </aside>
      <main class="flex-1 px-8 py-10">
        <router-outlet />
      </main>
    </div>
  `,
})

export class Shell {
  private users = inject(UserService);
  me = this.users.me;
  nav = NAV;
  collapsed = signal(localStorage.getItem('sidebar') === 'collapsed');

  constructor() {
    this.users.loadMe().subscribe();
  }

  toggle() {
    this.collapsed.update((c) => !c);
    localStorage.setItem('sidebar', this.collapsed() ? 'collapsed' : 'open');
  }
}
