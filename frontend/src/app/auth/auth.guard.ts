import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

// Sin token en memoria (p. ej. tras recargar) intenta renovarlo con la cookie; si falla, a /login.
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const login = inject(Router).parseUrl('/login');
  return auth.token() ? true : auth.refresh().pipe(map(() => true), catchError(() => of(login)));
};
