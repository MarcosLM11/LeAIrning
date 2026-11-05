import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-register-form',
  imports: [FormsModule],
  template: `
    <form class="space-y-3" (ngSubmit)="submit()" #f="ngForm">
      <h1 class="mb-4 text-2xl font-semibold tracking-tight">Crea tu cuenta</h1>
      <input class="input" name="name" [(ngModel)]="user.name" placeholder="Nombre" autocomplete="name" required />
      <input class="input" name="username" [(ngModel)]="user.username" placeholder="Usuario" autocomplete="username" required />
      <input class="input" name="email" [(ngModel)]="user.email" type="email" placeholder="Email" autocomplete="email" required />
      <input class="input" name="password" [(ngModel)]="user.password" type="password" placeholder="Contraseña" autocomplete="new-password" required minlength="8" maxlength="72" />
      <p class="text-[13px] text-mute">Mínimo 8 caracteres</p>
      @if (error()) { <p class="text-sm text-red-600">{{ error() }}</p> }
      <button class="btn" [disabled]="f.invalid || loading()">Crear cuenta</button>
    </form>
  `,
})

export class RegisterForm {
  private auth = inject(AuthService);
  private router = inject(Router);
  user = { name: '', username: '', email: '', password: '' };
  error = signal('');
  loading = signal(false);

  submit() {
    this.loading.set(true);
    this.auth.register(this.user).subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: () => { this.error.set('No se pudo crear la cuenta. Revisa los datos.'); this.loading.set(false); },
    });
  }
}
