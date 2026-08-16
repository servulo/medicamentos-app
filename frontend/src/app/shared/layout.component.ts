import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MeService } from '../core/api/api';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-layout', standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `<header><a class="brand" routerLink="/medicamentos">💊 Medicamentos</a>
    <nav>
      <a routerLink="/medicamentos" routerLinkActive="active">Medicamentos</a>
      <a routerLink="/agendas" routerLinkActive="active">Agendas</a>
      <a routerLink="/doses" routerLinkActive="active">Doses</a>
      <a routerLink="/alertas" routerLinkActive="active">Compras</a>
      <a routerLink="/historico" routerLinkActive="active">Histórico</a>
      @if (me.profile()?.admin) { <a routerLink="/admin/whitelist" routerLinkActive="active">Admin</a> }
    </nav>
    <button class="secondary compact" (click)="auth.logout()">Sair</button>
  </header><main class="container"><router-outlet /></main>`,
  styles: [`header{background:#fff;border-bottom:1px solid #dfe6e3;padding:.75rem max(1rem,calc((100% - 1100px)/2));display:flex;align-items:center;gap:1.25rem;position:sticky;top:0;z-index:2}.brand{font-weight:800;color:#126b50;text-decoration:none;white-space:nowrap}nav{display:flex;gap:.25rem;flex:1;overflow:auto}nav a{padding:.55rem .7rem;border-radius:.5rem;text-decoration:none;color:#43524d;white-space:nowrap}nav a.active{background:#e4f3ed;color:#126b50}@media(max-width:700px){header{align-items:flex-start;flex-wrap:wrap}nav{order:3;width:100%}}`]
})
export class LayoutComponent implements OnInit {
  readonly me = inject(MeService);
  readonly auth = inject(AuthService);
  ngOnInit() { if (!this.me.profile()) this.me.load().subscribe({ error: () => undefined }); }
}
