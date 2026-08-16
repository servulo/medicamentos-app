package app.medicamentos.medication;

import app.medicamentos.auth.CurrentUser;
import app.medicamentos.dose.DoseOccurrenceEntity;
import app.medicamentos.notify.NotificationLogEntity;
import app.medicamentos.schedule.TreatmentScheduleEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class MedicationService {
    @Inject CurrentUser user;

    public List<MedicationEntity> list() {
        return MedicationEntity.list("userId = ?1 order by name", user.id());
    }

    public MedicationEntity get(UUID id) {
        MedicationEntity m = MedicationEntity.find("id = ?1 and userId = ?2", id, user.id()).firstResult();
        if (m == null) throw error(404, "Medication not found");
        return m;
    }

    @Transactional
    public MedicationEntity create(String name, String unit, Integer stock, Integer threshold) {
        MedicationEntity m = new MedicationEntity();
        m.id = UUID.randomUUID();
        m.userId = user.id();
        apply(m, name, unit, stock, threshold, true);
        m.createdAt = m.updatedAt = OffsetDateTime.now();
        m.persist();
        return m;
    }

    @Transactional
    public MedicationEntity update(UUID id, String name, String unit, Integer stock, Integer threshold) {
        MedicationEntity m = get(id);
        apply(m, name, unit, stock, threshold, false);
        m.updatedAt = OffsetDateTime.now();
        return m;
    }

    @Transactional
    public void delete(UUID id) {
        MedicationEntity m = get(id);
        List<UUID> doseIds = DoseOccurrenceEntity.<DoseOccurrenceEntity>list(
                "medicationId = ?1 and userId = ?2", id, user.id())
                .stream().map(d -> d.id).toList();
        for (UUID doseId : doseIds) {
            NotificationLogEntity.delete("doseId = ?1", doseId);
        }
        DoseOccurrenceEntity.delete("medicationId = ?1 and userId = ?2", id, user.id());
        TreatmentScheduleEntity.delete("medicationId = ?1 and userId = ?2", id, user.id());
        m.delete();
    }

    @Transactional
    public void decrementStock(MedicationEntity medication, int amount) {
        if (amount < 1) throw error(400, "stock decrement amount must be >= 1");
        medication.stockQuantity = Math.max(0, medication.stockQuantity - amount);
        medication.updatedAt = OffsetDateTime.now();
    }

    private static void apply(MedicationEntity m, String name, String unit, Integer stock, Integer threshold, boolean create) {
        if (create || name != null) {
            if (name == null || name.isBlank() || name.trim().length() > 120)
                throw error(400, "Name must have 1 to 120 characters");
            m.name = name.trim();
        }
        if (create || unit != null) m.unit = unit == null || unit.isBlank() ? "unidade" : unit.trim();
        if (create || stock != null) {
            m.stockQuantity = stock == null ? 0 : requireNonNegativeInt(stock, "stockQuantity");
        }
        if (create || threshold != null) {
            m.purchaseThresholdUnits = threshold == null ? 10 : requireNonNegativeInt(threshold, "purchaseThresholdUnits");
        }
    }

    static int requireNonNegativeInt(int value, String field) {
        if (value < 0) throw error(400, field + " cannot be negative");
        return value;
    }

    public static WebApplicationException error(int status, String message) {
        return new WebApplicationException(Response.status(status).entity(Map.of("message", message)).build());
    }
}
