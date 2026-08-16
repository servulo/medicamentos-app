import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MeService } from '../../core/api/api';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <main class="auth-card">
      <h1>Medicamentos</h1>
      <p>Acesse seu controle de medicamentos.</p>
      <form [formGroup]="form" (ngSubmit)="enter()">
        <label>
          E-mail
          <input
            type="email"
            formControlName="email"
            placeholder="voce@exemplo.com"
            autocomplete="username"
          />
        </label>
        @if (form.controls.email.touched && form.controls.email.invalid) {
          <p class="error">Informe um e-mail válido.</p>
        }
        @if (error) {
          <p class="error">{{ error }}</p>
        }
        <button type="submit" [disabled]="busy">
          {{ busy ? 'Entrando…' : 'Entrar' }}
        </button>
      </form>
      <small>
        Ambiente de teste: use o e-mail definido em <code>ADMIN_EMAIL</code> no arquivo
        <code>deploy/.env</code>.
      </small>
    </main>
  `
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly me = inject(MeService);
  private readonly router = inject(Router);

  readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email]
    })
  });
  busy = false;
  error = '';

  enter(): void {
    this.error = '';
    const control = this.form.controls.email;
    const value = control.value.trim().toLowerCase();
    control.setValue(value);
    control.markAsTouched();
    if (control.invalid) {
      this.error = 'Informe um e-mail válido.';
      return;
    }

    this.busy = true;
    this.auth.login(value);

    this.me.load().subscribe({
      next: () => {
        this.busy = false;
        void this.router.navigateByUrl('/medicamentos');
      },
      error: (err: Error) => {
        this.busy = false;
        localStorage.removeItem('medicamentos.test.email');
        const msg = err?.message || 'Falha ao autenticar.';
        if (/not authorized|whitelist|não autoriz/i.test(msg)) {
          void this.router.navigateByUrl('/bloqueado');
          return;
        }
        this.error = msg;
      }
    });
  }
}

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="auth-card">
      <h1>Acesso bloqueado</h1>
      <p>Seu e-mail não tem permissão para acessar esta área.</p>
      <a routerLink="/login">Voltar ao login</a>
    </main>
  `
})
export class BlockedPage {}
