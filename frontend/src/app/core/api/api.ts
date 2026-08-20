import { HttpClient, HttpContextToken, HttpErrorResponse, HttpInterceptorFn, HttpParams } from '@angular/common/http';
import { InjectionToken, Injectable, inject, signal } from '@angular/core';
import { catchError, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => environment.apiBaseUrl
});
export const SKIP_AUTH = new HttpContextToken(() => false);

export interface UserProfile { email: string; displayName?: string; admin: boolean; timezone: string; }
export interface Medication { id: string; name: string; unit: string; stockQuantity: number; purchaseThresholdUnits: number; purchaseNeeded: boolean; }
export type MedicationInput = Omit<Medication, 'id' | 'purchaseNeeded'>;
export interface Schedule { id: string; medicationId: string; status: 'ACTIVE'|'PAUSED'|'COMPLETED'|'CANCELLED'; daysOfWeek: number[]; timesOfDay: string[]; durationType: 'INDEFINITE'|'FIXED_TAKEN_DOSES'; maxTakenDoses?: number; takenCount: number; quantityPerDose: number; }
export interface Dose { id: string; scheduleId: string; medicationId: string; medicationName?: string; scheduledAt: string; originalScheduledAt?: string; status: 'PENDING'|'TAKEN'|'SKIPPED'; snoozeCount: number; resolvedAt?: string; }
export interface PurchaseAlert { medicationId: string; name: string; unit: string; stockQuantity: number; purchaseThresholdUnits: number; }
export interface WhitelistEntry { id: string; email: string; createdAt: string; }

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  if (environment.useTestAuth && auth.email && !request.context.get(SKIP_AUTH)) {
    request = request.clone({ setHeaders: { 'X-Test-User-Email': auth.email } });
  }
  // Production OIDC will clone the request with Authorization: Bearer <token>.
  return next(request);
};

export const errorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(catchError((error: HttpErrorResponse) => {
    const message = error.error?.message || (error.status === 0 ? 'API indisponível.' : 'Não foi possível concluir a operação.');
    console.error(message, error);
    return throwError(() => new Error(message));
  }));

@Injectable({ providedIn: 'root' })
export class MeService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  readonly profile = signal<UserProfile | null>(null);
  load(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.base}/me`).pipe(tap(profile => this.profile.set(profile)));
  }
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(API_BASE_URL);
  medications() { return this.http.get<Medication[]>(`${this.base}/medications`); }
  medication(id: string) { return this.http.get<Medication>(`${this.base}/medications/${id}`); }
  createMedication(value: MedicationInput) { return this.http.post<Medication>(`${this.base}/medications`, value); }
  updateMedication(id: string, value: Partial<MedicationInput>) { return this.http.patch<Medication>(`${this.base}/medications/${id}`, value); }
  deleteMedication(id: string) { return this.http.delete<void>(`${this.base}/medications/${id}`); }
  schedules() { return this.http.get<Schedule[]>(`${this.base}/schedules`); }
  schedule(id: string) { return this.http.get<Schedule>(`${this.base}/schedules/${id}`); }
  createSchedule(value: Partial<Schedule>) { return this.http.post<Schedule>(`${this.base}/schedules`, value); }
  updateSchedule(id: string, value: Partial<Schedule> & { resetTakenCount?: boolean }) { return this.http.patch<Schedule>(`${this.base}/schedules/${id}`, value); }
  deleteSchedule(id: string) { return this.http.delete<void>(`${this.base}/schedules/${id}`); }
  doses(status?: Dose['status'], medicationId?: string) {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (medicationId) params = params.set('medicationId', medicationId);
    return this.http.get<Dose[]>(`${this.base}/doses`, { params });
  }
  doseAction(id: string, action: 'take'|'skip'|'snooze', minutes?: 10|30|60) {
    return this.http.post<Dose>(`${this.base}/doses/${id}/${action}`, action === 'snooze' ? { minutes } : {});
  }
  alerts() { return this.http.get<PurchaseAlert[]>(`${this.base}/alerts/purchase`); }
  whitelist() { return this.http.get<WhitelistEntry[]>(`${this.base}/admin/whitelist`); }
  addWhitelist(email: string) { return this.http.post<WhitelistEntry>(`${this.base}/admin/whitelist`, { email }); }
  removeWhitelist(id: string) { return this.http.delete<void>(`${this.base}/admin/whitelist/${id}`); }
  registerDevice(value: { endpoint: string; p256dh: string; auth: string; isMobile: boolean; userAgent: string }) {
    return this.http.post(`${this.base}/devices`, value);
  }
}
