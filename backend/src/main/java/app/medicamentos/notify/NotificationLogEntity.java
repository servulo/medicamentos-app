package app.medicamentos.notify;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_log")
public class NotificationLogEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "dose_id", nullable = false) public UUID doseId;
    @Column(name = "device_id", nullable = false) public UUID deviceId;
    @Column(name = "scheduled_at", nullable = false) public OffsetDateTime scheduledAt;
    @Column(name = "sent_at", nullable = false) public OffsetDateTime sentAt;
    @Column(nullable = false) public boolean success;
    @Column(name = "error_detail") public String errorDetail;
}
