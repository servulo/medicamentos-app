import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { MeService } from '../core/api/api';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-layout', standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `<header [class.menu-open]="menuOpen">
    <a class="brand" routerLink="/medicamentos" (click)="closeMenu()">💊 Medicamentos</a>
    <button type="button" class="menu-toggle" aria-label="Menu" [attr.aria-expanded]="menuOpen" (click)="toggleMenu()">
      <span class="menu-toggle-bars" aria-hidden="true"></span>
    </button>
    <nav>
      <a routerLink="/medicamentos" routerLinkActive="active" (click)="closeMenu()">Medicamentos</a>
      <a routerLink="/agendas" routerLinkActive="active" (click)="closeMenu()">Agendas</a>
      <a routerLink="/doses" routerLinkActive="active" (click)="closeMenu()">Doses</a>
      <a routerLink="/alertas" routerLinkActive="active" (click)="closeMenu()">Compras</a>
      <a routerLink="/historico" routerLinkActive="active" (click)="closeMenu()">Histórico</a>
      @if (me.profile()?.admin) { <a routerLink="/admin/whitelist" routerLinkActive="active" (click)="closeMenu()">Admin</a> }
    </nav>
    <button type="button" class="secondary compact" (click)="auth.logout()">Sair</button>
  </header>
  @if (menuOpen) { <div class="nav-overlay" (click)="closeMenu()" aria-hidden="true"></div> }
  <main class="container"><router-outlet /></main>`,
  styles: [`
    header {
      background: #fff;
      border-bottom: 1px solid #dfe6e3;
      padding: .75rem max(1rem, calc((100% - 1100px) / 2));
      display: flex;
      align-items: center;
      gap: 1.25rem;
      position: sticky;
      top: 0;
      z-index: 4;
    }
    .brand { font-weight: 800; color: #126b50; text-decoration: none; white-space: nowrap; }
    .menu-toggle {
      display: none;
      align-items: center;
      justify-content: center;
      width: 2.5rem;
      height: 2.5rem;
      padding: 0;
      margin-left: auto;
      background: #e4f0ec;
      color: #185d49;
      flex-shrink: 0;
    }
    .menu-toggle-bars,
    .menu-toggle-bars::before,
    .menu-toggle-bars::after {
      display: block;
      width: 1.15rem;
      height: 2px;
      background: currentColor;
      border-radius: 1px;
    }
    .menu-toggle-bars { position: relative; }
    .menu-toggle-bars::before,
    .menu-toggle-bars::after { content: ''; position: absolute; left: 0; }
    .menu-toggle-bars::before { top: -6px; }
    .menu-toggle-bars::after { top: 6px; }
    nav { display: flex; gap: .25rem; flex: 1; overflow: auto; }
    nav a { padding: .55rem .7rem; border-radius: .5rem; text-decoration: none; color: #43524d; white-space: nowrap; }
    nav a.active { background: #e4f3ed; color: #126b50; }
    .nav-overlay {
      display: none;
      position: fixed;
      inset: 0;
      background: rgba(20, 40, 34, .35);
      z-index: 3;
    }
    @media (max-width: 767px) {
      .menu-toggle { display: inline-flex; }
      .nav-overlay { display: block; }
      nav {
        display: none;
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        flex-direction: column;
        flex: none;
        overflow: auto;
        background: #fff;
        border-bottom: 1px solid #dfe6e3;
        padding: .5rem max(1rem, calc((100% - 1100px) / 2));
        gap: .15rem;
        z-index: 5;
      }
      header.menu-open nav { display: flex; }
    }
    @media (min-width: 768px) {
      .menu-toggle { display: none !important; }
      .nav-overlay { display: none !important; }
      nav { display: flex; flex-direction: row; position: static; }
    }
  `]
})
export class LayoutComponent implements OnInit, OnDestroy {
  readonly me = inject(MeService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  menuOpen = false;
  private phoneQuery?: MediaQueryList;
  private onPhoneQueryChange?: (event: MediaQueryListEvent) => void;
  private navigationSub?: Subscription;

  ngOnInit() {
    if (!this.me.profile()) this.me.load().subscribe({ error: () => undefined });
    this.phoneQuery = window.matchMedia('(max-width: 767px)');
    this.onPhoneQueryChange = () => this.closeMenu();
    this.phoneQuery.addEventListener('change', this.onPhoneQueryChange);
    this.navigationSub = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe(() => this.closeMenu());
  }

  ngOnDestroy() {
    if (this.phoneQuery && this.onPhoneQueryChange) {
      this.phoneQuery.removeEventListener('change', this.onPhoneQueryChange);
    }
    this.navigationSub?.unsubscribe();
  }

  toggleMenu() { this.menuOpen = !this.menuOpen; }
  closeMenu() { this.menuOpen = false; }
}
