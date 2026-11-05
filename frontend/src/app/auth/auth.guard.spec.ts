import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

function run(token: string | null, refresh = () => of({})) {
  TestBed.configureTestingModule({
    providers: [{ provide: AuthService, useValue: { token: () => token, refresh } }],
  });
  return TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
}

describe('authGuard', () => {
  it('deja pasar si hay token', () => {
    expect(run('t')).toBe(true);
  });

  it('renueva el token y deja pasar', async () => {
    const r = run(null) as Observable<boolean>;
    await expect(new Promise((res) => r.subscribe(res))).resolves.toBe(true);
  });

  it('redirige a /login si no se puede renovar', async () => {
    const r = run(null, () => throwError(() => new Error('401'))) as Observable<UrlTree>;
    const tree = (await new Promise((res) => r.subscribe(res))) as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(tree)).toBe('/login');
  });
});
