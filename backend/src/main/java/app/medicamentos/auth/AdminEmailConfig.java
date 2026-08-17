package app.medicamentos.auth;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class AdminEmailConfig {
    @ConfigProperty(name = "app.admin.email")
    String configuredEmail;

    public String email() {
        return configuredEmail.trim().toLowerCase();
    }

    @Transactional
    void seed(@Observes StartupEvent event) {
        if (WhitelistEntryEntity.byEmail(email()) == null) {
            WhitelistEntryEntity entry = new WhitelistEntryEntity();
            entry.id = UUID.randomUUID();
            entry.email = email();
            entry.createdAt = OffsetDateTime.now();
            entry.createdByEmail = email();
            entry.persist();
        }
    }
}
