package app.medicamentos.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class CurrentUserResolver {
    @Transactional
    public UserEntity resolve(String email, String sub, String displayName, boolean admin) {
        if (!admin && WhitelistEntryEntity.byEmail(email) == null) {
            throw new WebApplicationException(Response.status(403)
                    .entity(Map.of("message", "Email is not authorized", "code", "NOT_WHITELISTED")).build());
        }
        UserEntity user = UserEntity.byEmail(email);
        if (user == null) {
            user = new UserEntity();
            user.id = UUID.randomUUID();
            user.googleSub = sub == null || sub.isBlank() ? email : sub;
            user.email = email;
            user.displayName = displayName;
            user.createdAt = OffsetDateTime.now();
        }
        user.lastLoginAt = OffsetDateTime.now();
        user.persist();
        return user;
    }
}
