import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/auth/guards';
import { LoginPage, BlockedPage } from './features/auth/auth-pages';
import { LayoutComponent } from './shared/layout.component';
import { MedicationsPage, MedicationFormPage } from './features/medications/medications.page';
import { SchedulesPage, ScheduleFormPage } from './features/schedules/schedules.page';
import { DosesPage } from './features/doses/doses.page';
import { AlertsPage } from './features/alerts/alerts.page';
import { AdherencePage } from './features/adherence/adherence.page';
import { WhitelistPage } from './features/admin/whitelist/whitelist.page';

export const routes: Routes = [
  { path: 'login', component: LoginPage },
  { path: 'bloqueado', component: BlockedPage },
  {
    path: '', component: LayoutComponent, canActivate: [authGuard], children: [
      { path: 'medicamentos', component: MedicationsPage },
      { path: 'medicamentos/novo', component: MedicationFormPage },
      { path: 'medicamentos/:id/editar', component: MedicationFormPage },
      { path: 'agendas', component: SchedulesPage },
      { path: 'agendas/nova', component: ScheduleFormPage },
      { path: 'agendas/:id/editar', component: ScheduleFormPage },
      { path: 'doses', component: DosesPage },
      { path: 'alertas', component: AlertsPage },
      { path: 'historico', component: AdherencePage },
      { path: 'admin/whitelist', component: WhitelistPage, canActivate: [adminGuard] },
      { path: '', pathMatch: 'full', redirectTo: 'medicamentos' }
    ]
  },
  { path: '**', redirectTo: '' }
];
