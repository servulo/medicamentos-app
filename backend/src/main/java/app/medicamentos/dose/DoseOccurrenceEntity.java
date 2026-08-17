package app.medicamentos.dose;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dose_occurrences")
public class DoseOccurrenceEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "schedule_id", nullable = false) public UUID scheduleId;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "medication_id", nullable = false) public UUID medicationId;
    @Column(name = "scheduled_at", nullable = false) public OffsetDateTime scheduledAt;
    @Column(name = "original_scheduled_at", nullable = false) public OffsetDateTime originalScheduledAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public DoseStatus status;
    @Column(name = "snooze_count", nullable = false) public int snoozeCount;
    @Column(name = "resolved_at") public OffsetDateTime resolvedAt;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
}
