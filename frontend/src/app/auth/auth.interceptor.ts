import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

// Añade el JWT a las peticiones; /auth/* queda fuera (un token caducado ahí daría 401).
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).token();
  return next(token && !req.url.includes('/auth/') ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req);
};
