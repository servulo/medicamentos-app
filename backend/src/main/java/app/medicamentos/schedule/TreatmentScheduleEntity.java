package app.medicamentos.schedule;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "treatment_schedules")
public class TreatmentScheduleEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(name = "medication_id", nullable = false) public UUID medicationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) public ScheduleStatus status;
    @Column(name = "days_of_week", nullable = false) public String daysOfWeek;
    @Column(name = "times_of_day", nullable = false) public String timesOfDay;
    @Enumerated(EnumType.STRING) @Column(name = "duration_type", nullable = false) public DurationType durationType;
    @Column(name = "max_taken_doses") public Integer maxTakenDoses;
    @Column(name = "taken_count", nullable = false) public int takenCount;
    @Column(name = "quantity_per_dose", nullable = false) public int quantityPerDose;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) public OffsetDateTime updatedAt;
}
