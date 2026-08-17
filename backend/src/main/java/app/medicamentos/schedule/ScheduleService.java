package app.medicamentos.schedule;

import app.medicamentos.auth.CurrentUser;
import app.medicamentos.medication.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ScheduleService {
    @Inject CurrentUser user;
    @Inject MedicationService medications;

    public List<TreatmentScheduleEntity> list(UUID medicationId, ScheduleStatus status) {
        return TreatmentScheduleEntity.<TreatmentScheduleEntity>list("userId = ?1 order by createdAt desc", user.id())
                .stream().filter(s -> medicationId == null || s.medicationId.equals(medicationId))
                .filter(s -> status == null || s.status == status).toList();
    }

    public TreatmentScheduleEntity get(UUID id) {
        TreatmentScheduleEntity s = TreatmentScheduleEntity.find("id = ?1 and userId = ?2", id, user.id()).firstResult();
        if (s == null) throw MedicationService.error(404, "Schedule not found");
        return s;
    }

    @Transactional
    public TreatmentScheduleEntity create(UUID medicationId, List<Integer> days, List<String> times,
                                           DurationType duration, Integer max, Integer quantityPerDose) {
        medications.get(medicationId);
        TreatmentScheduleEntity s = new TreatmentScheduleEntity();
        s.id = UUID.randomUUID();
        s.userId = user.id();
        s.medicationId = medicationId;
        s.status = ScheduleStatus.ACTIVE;
        s.takenCount = 0;
        s.quantityPerDose = normalizeQuantityPerDose(quantityPerDose);
        apply(s, days, times, duration, max);
        s.createdAt = s.updatedAt = OffsetDateTime.now();
        s.persist();
        return s;
    }

    @Transactional
    public TreatmentScheduleEntity update(UUID id, ScheduleStatus status, List<Integer> days, List<String> times,
                                           DurationType duration, Integer max, Boolean reset, Integer quantityPerDose) {
        TreatmentScheduleEntity s = get(id);
        if (days != null || times != null || duration != null || max != null)
            apply(s, days == null ? days(s) : days, times == null ? times(s) : times,
                    duration == null ? s.durationType : duration,
                    max == null && duration == null ? s.maxTakenDoses : max);
        if (quantityPerDose != null) {
            s.quantityPerDose = normalizeQuantityPerDose(quantityPerDose);
        }
        if (status != null) {
            if (status == ScheduleStatus.COMPLETED && s.durationType != DurationType.FIXED_TAKEN_DOSES)
                throw MedicationService.error(400, "Only fixed schedules complete by dose limit");
            if (status == ScheduleStatus.ACTIVE && s.status != ScheduleStatus.ACTIVE
                    && s.durationType == DurationType.FIXED_TAKEN_DOSES && (reset == null || reset))
                s.takenCount = 0;
            s.status = status;
        }
        s.updatedAt = OffsetDateTime.now();
        return s;
    }

    static int normalizeQuantityPerDose(Integer quantityPerDose) {
        int value = quantityPerDose == null ? 1 : quantityPerDose;
        if (value < 1) throw MedicationService.error(400, "quantityPerDose must be an integer >= 1");
        return value;
    }

    private static void apply(TreatmentScheduleEntity s, List<Integer> days, List<String> times,
                              DurationType duration, Integer max) {
        if (days == null || days.isEmpty() || days.stream().anyMatch(d -> d == null || d < 1 || d > 7))
            throw MedicationService.error(400, "daysOfWeek must contain ISO days 1 through 7");
        if (times == null || times.isEmpty()) throw MedicationService.error(400, "timesOfDay is required");
        List<String> normalized = new ArrayList<>();
        try { for (String time : times) normalized.add(LocalTime.parse(time).withSecond(0).withNano(0).toString()); }
        catch (RuntimeException e) { throw MedicationService.error(400, "timesOfDay must use HH:mm"); }
        if (duration == null) throw MedicationService.error(400, "durationType is required");
        if (duration == DurationType.FIXED_TAKEN_DOSES && (max == null || max <= 0))
            throw MedicationService.error(400, "maxTakenDoses must be positive for fixed schedules");
        s.daysOfWeek = days.stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(","));
        s.timesOfDay = normalized.stream().distinct().sorted().collect(Collectors.joining(","));
        s.durationType = duration;
        s.maxTakenDoses = duration == DurationType.INDEFINITE ? null : max;
    }

    public static List<Integer> days(TreatmentScheduleEntity s) {
        return Arrays.stream(s.daysOfWeek.split(",")).map(Integer::valueOf).toList();
    }
    public static List<String> times(TreatmentScheduleEntity s) {
        return Arrays.asList(s.timesOfDay.split(","));
    }
}
