import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { MeService } from '../api/api';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.isAuthenticated ? true : inject(Router).createUrlTree(['/login']);
};

export const adminGuard: CanActivateFn = () => {
  const me = inject(MeService);
  const router = inject(Router);
  if (me.profile()) return me.profile()!.admin ? true : router.createUrlTree(['/bloqueado']);
  return me.load().pipe(
    map(profile => profile.admin ? true : router.createUrlTree(['/bloqueado'])),
    catchError(() => of(router.createUrlTree(['/bloqueado'])))
  );
};
