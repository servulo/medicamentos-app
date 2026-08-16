import { Injectable, inject } from '@angular/core';
import { ApiService } from '../../core/api/api';

@Injectable({ providedIn: 'root' })
export class PushRegistrationService {
  private readonly api = inject(ApiService);
  private readonly isMobile = /Android|iPhone|iPad|iPod|Mobile/i.test(navigator.userAgent);

  async register(): Promise<void> {
    if (!('Notification' in window)) return;
    const permission = await Notification.requestPermission();
    if (permission !== 'granted') return;

    let endpoint = `https://local.invalid/push/${crypto.randomUUID()}`;
    let p256dh = 'mock-p256dh';
    let auth = 'mock-auth';
    if ('serviceWorker' in navigator && 'PushManager' in window) {
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.getSubscription();
      if (subscription) {
        const json = subscription.toJSON();
        endpoint = subscription.endpoint;
        p256dh = json.keys?.['p256dh'] || p256dh;
        auth = json.keys?.['auth'] || auth;
      }
    }
    this.api.registerDevice({ endpoint, p256dh, auth, isMobile: this.isMobile, userAgent: navigator.userAgent }).subscribe();
  }
}
