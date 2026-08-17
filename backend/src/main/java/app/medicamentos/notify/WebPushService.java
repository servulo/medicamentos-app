package app.medicamentos.notify;

import app.medicamentos.device.PushDeviceEntity;
import app.medicamentos.dose.DoseOccurrenceEntity;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.security.Security;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class WebPushService {
    @ConfigProperty(name = "app.vapid.public-key", defaultValue = "") String publicKey;
    @ConfigProperty(name = "app.vapid.private-key", defaultValue = "") String privateKey;
    @ConfigProperty(name = "app.vapid.subject") String subject;
    private volatile PushService pushService;

    @Transactional
    public void send(DoseOccurrenceEntity dose) {
        List<PushDeviceEntity> devices = PushDeviceEntity.list("userId = ?1 and isMobile = true", dose.userId);
        for (PushDeviceEntity device : devices) {
            if (NotificationLogEntity.count("doseId = ?1 and deviceId = ?2 and scheduledAt = ?3",
                    dose.id, device.id, dose.scheduledAt) > 0) continue;
            NotificationLogEntity log = new NotificationLogEntity();
            log.id = UUID.randomUUID();
            log.doseId = dose.id;
            log.deviceId = device.id;
            log.scheduledAt = dose.scheduledAt;
            log.sentAt = OffsetDateTime.now();
            try {
                if (!configured()) throw new IllegalStateException("VAPID keys are not configured");
                Subscription subscription = new Subscription(device.endpoint,
                        new Subscription.Keys(device.p256dh, device.auth));
                String payload = "{\"title\":\"Hora do medicamento\",\"doseId\":\"" + dose.id
                        + "\",\"medicationId\":\"" + dose.medicationId + "\"}";
                int status = service().send(new Notification(subscription, payload)).getStatusLine().getStatusCode();
                log.success = status >= 200 && status < 300;
                if (!log.success) log.errorDetail = "Push provider returned HTTP " + status;
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                log.success = false;
                log.errorDetail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            }
            log.persist();
            if (!log.success) Log.warnf("Push failed for dose %s/device %s: %s", dose.id, device.id, log.errorDetail);
        }
    }

    private boolean configured() {
        return !publicKey.isBlank() && !privateKey.isBlank()
                && !publicKey.equals("not-configured") && !privateKey.equals("not-configured");
    }

    private PushService service() throws Exception {
        if (pushService == null) {
            synchronized (this) {
                if (pushService == null) {
                    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
                        Security.addProvider(new BouncyCastleProvider());
                    pushService = new PushService(publicKey, privateKey, subject);
                }
            }
        }
        return pushService;
    }
}
