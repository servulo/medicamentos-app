package app.medicamentos.admin;

import app.medicamentos.auth.WhitelistEntryEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class WhitelistService {
    public List<WhitelistEntryEntity> list() {
        return WhitelistEntryEntity.list("order by email");
    }

    @Transactional
    public WhitelistEntryEntity add(String rawEmail, String actor) {
        String email = normalize(rawEmail);
        WhitelistEntryEntity existing = WhitelistEntryEntity.byEmail(email);
        if (existing != null) return existing;
        WhitelistEntryEntity entry = new WhitelistEntryEntity();
        entry.id = UUID.randomUUID();
        entry.email = email;
        entry.createdAt = OffsetDateTime.now();
        entry.createdByEmail = actor;
        entry.persist();
        return entry;
    }

    @Transactional
    public void remove(UUID id, String adminEmail) {
        WhitelistEntryEntity entry = WhitelistEntryEntity.findById(id);
        if (entry == null) throw error(404, "Whitelist entry not found");
        if (entry.email.equalsIgnoreCase(adminEmail)) throw error(403, "ADMIN_EMAIL cannot be removed");
        entry.delete();
    }

    private static String normalize(String value) {
        if (value == null || !value.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw error(400, "Invalid email");
        return value.trim().toLowerCase();
    }

    static WebApplicationException error(int status, String message) {
        return new WebApplicationException(Response.status(status).entity(Map.of("message", message)).build());
    }
}
