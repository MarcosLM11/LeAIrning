import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-login-form',
  imports: [FormsModule],
  template: `
    <form class="space-y-3" (ngSubmit)="submit()" #f="ngForm">
      <h1 class="mb-4 text-2xl font-semibold tracking-tight">Bienvenido de nuevo</h1>
      <input class="input" name="identifier" [(ngModel)]="identifier" placeholder="Email o usuario" autocomplete="username" required />
      <input class="input" name="password" [(ngModel)]="password" type="password" placeholder="Contraseña" autocomplete="current-password" required />
      @if (error()) { <p class="text-sm text-red-600">{{ error() }}</p> }
      <button class="btn" [disabled]="f.invalid || loading()">Iniciar sesión</button>
    </form>
  `,
})

export class LoginForm {
  private auth = inject(AuthService);
  identifier = '';
  password = '';
  error = signal('');
  loading = signal(false);

  submit() {
    this.loading.set(true);
    this.auth.login(this.identifier, this.password).subscribe({
      // TODO: navegar a la home cuando exista
      error: () => { this.error.set('Credenciales incorrectas'); this.loading.set(false); },
      complete: () => this.loading.set(false),
    });
  }
}
