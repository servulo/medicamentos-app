import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';

const EMAIL_KEY = 'medicamentos.test.email';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly router = inject(Router);

  get email(): string | null {
    return environment.useTestAuth ? localStorage.getItem(EMAIL_KEY) : null;
  }

  get isAuthenticated(): boolean {
    return environment.useTestAuth ? !!this.email : false; // TODO: connect to OIDC session.
  }

  login(email: string): void {
    if (environment.useTestAuth) {
      localStorage.setItem(EMAIL_KEY, email.trim().toLowerCase());
      return;
    }
    // TODO: initiate the configured OIDC authorization-code flow.
  }

  logout(): void {
    localStorage.removeItem(EMAIL_KEY);
    void this.router.navigateByUrl('/login');
  }
}
