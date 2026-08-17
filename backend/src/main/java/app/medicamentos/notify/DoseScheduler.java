package app.medicamentos.notify;

import app.medicamentos.dose.*;
import app.medicamentos.schedule.*;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@ApplicationScoped
public class DoseScheduler {
    @ConfigProperty(name = "app.timezone") String timezone;
    @Inject WebPushService push;

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void run() {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        OffsetDateTime instantNow = now.toOffsetDateTime();

        List<DoseOccurrenceEntity> expired = DoseOccurrenceEntity.list(
                "status = ?1 and scheduledAt < ?2", DoseStatus.PENDING, instantNow.minusHours(2));
        for (DoseOccurrenceEntity dose : expired) {
            dose.status = DoseStatus.SKIPPED;
            dose.resolvedAt = instantNow;
        }

        LocalDate date = now.toLocalDate();
        int day = date.getDayOfWeek().getValue();
        for (TreatmentScheduleEntity schedule : TreatmentScheduleEntity.<TreatmentScheduleEntity>list("status", ScheduleStatus.ACTIVE)) {
            if (!ScheduleService.days(schedule).contains(day)) continue;
            for (String rawTime : ScheduleService.times(schedule)) {
                LocalTime time = LocalTime.parse(rawTime);
                OffsetDateTime target = ZonedDateTime.of(date, time, zone).toOffsetDateTime();
                if (!target.truncatedTo(ChronoUnit.MINUTES).equals(instantNow.truncatedTo(ChronoUnit.MINUTES))) continue;
                if (DoseOccurrenceEntity.count("scheduleId = ?1 and originalScheduledAt = ?2", schedule.id, target) == 0) {
                    DoseOccurrenceEntity dose = new DoseOccurrenceEntity();
                    dose.id = UUID.randomUUID();
                    dose.scheduleId = schedule.id;
                    dose.userId = schedule.userId;
                    dose.medicationId = schedule.medicationId;
                    dose.scheduledAt = target;
                    dose.originalScheduledAt = target;
                    dose.status = DoseStatus.PENDING;
                    dose.snoozeCount = 0;
                    dose.createdAt = instantNow;
                    dose.persist();
                }
            }
        }

        List<DoseOccurrenceEntity> due = DoseOccurrenceEntity.list(
                "status = ?1 and scheduledAt >= ?2 and scheduledAt < ?3",
                DoseStatus.PENDING, instantNow.truncatedTo(ChronoUnit.MINUTES),
                instantNow.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1));
        due.forEach(push::send);
    }
}
