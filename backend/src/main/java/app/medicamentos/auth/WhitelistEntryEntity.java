package app.medicamentos.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "whitelist_entries")
public class WhitelistEntryEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(nullable = false, unique = true) public String email;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
    @Column(name = "created_by_email", nullable = false) public String createdByEmail;

    public static WhitelistEntryEntity byEmail(String email) {
        return find("lower(email) = ?1", email.toLowerCase()).firstResult();
    }
}
