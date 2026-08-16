package app.medicamentos.device;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "push_devices")
public class PushDeviceEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "user_id", nullable = false) public UUID userId;
    @Column(nullable = false, unique = true, length = 2048) public String endpoint;
    @Column(nullable = false) public String p256dh;
    @Column(nullable = false) public String auth;
    @Column(name = "user_agent") public String userAgent;
    @Column(name = "is_mobile", nullable = false) public boolean isMobile;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
    @Column(name = "last_seen_at", nullable = false) public OffsetDateTime lastSeenAt;
}
