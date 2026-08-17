package app.medicamentos.dose;

import app.medicamentos.auth.CurrentUser;
import app.medicamentos.medication.*;
import app.medicamentos.schedule.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class DoseService {
    @Inject CurrentUser user;
    @Inject MedicationService medications;

    public List<DoseOccurrenceEntity> list(UUID medicationId, DoseStatus status, OffsetDateTime from, OffsetDateTime to) {
        return DoseOccurrenceEntity.<DoseOccurrenceEntity>list("userId = ?1 order by scheduledAt desc", user.id()).stream()
                .filter(d -> medicationId == null || d.medicationId.equals(medicationId))
                .filter(d -> status == null || d.status == status)
                .filter(d -> from == null || !d.scheduledAt.isBefore(from))
                .filter(d -> to == null || !d.scheduledAt.isAfter(to)).toList();
    }

    @Transactional
    public DoseOccurrenceEntity take(UUID id) {
        DoseOccurrenceEntity dose = pendingLocked(id);
        dose.status = DoseStatus.TAKEN;
        dose.resolvedAt = OffsetDateTime.now();
        TreatmentScheduleEntity schedule = TreatmentScheduleEntity.findById(dose.scheduleId);
        schedule.takenCount++;
        schedule.updatedAt = OffsetDateTime.now();
        if (schedule.durationType == DurationType.FIXED_TAKEN_DOSES
                && schedule.takenCount >= schedule.maxTakenDoses)
            schedule.status = ScheduleStatus.COMPLETED;
        medications.decrementStock(MedicationEntity.findById(dose.medicationId), schedule.quantityPerDose);
        return dose;
    }

    @Transactional
    public DoseOccurrenceEntity skip(UUID id) {
        DoseOccurrenceEntity dose = pendingLocked(id);
        dose.status = DoseStatus.SKIPPED;
        dose.resolvedAt = OffsetDateTime.now();
        return dose;
    }

    @Transactional
    public DoseOccurrenceEntity snooze(UUID id, int minutes) {
        if (minutes != 10 && minutes != 30 && minutes != 60)
            throw MedicationService.error(400, "minutes must be 10, 30 or 60");
        DoseOccurrenceEntity dose = pendingLocked(id);
        dose.scheduledAt = dose.scheduledAt.plusMinutes(minutes);
        dose.snoozeCount++;
        return dose;
    }

    private DoseOccurrenceEntity pendingLocked(UUID id) {
        DoseOccurrenceEntity dose = DoseOccurrenceEntity.find("id = ?1 and userId = ?2", id, user.id())
                .withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();
        if (dose == null) throw MedicationService.error(404, "Dose not found");
        if (dose.status != DoseStatus.PENDING) throw MedicationService.error(409, "Dose is already resolved");
        return dose;
    }
}
