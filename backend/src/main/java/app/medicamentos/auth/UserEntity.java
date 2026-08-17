package app.medicamentos.auth;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "google_sub", nullable = false, unique = true) public String googleSub;
    @Column(nullable = false, unique = true) public String email;
    @Column(name = "display_name") public String displayName;
    @Column(name = "created_at", nullable = false) public OffsetDateTime createdAt;
    @Column(name = "last_login_at") public OffsetDateTime lastLoginAt;

    public static UserEntity byEmail(String email) {
        return find("lower(email) = ?1", email.toLowerCase()).firstResult();
    }
}
