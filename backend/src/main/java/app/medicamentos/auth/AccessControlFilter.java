package app.medicamentos.auth;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.Map;

@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class AccessControlFilter implements ContainerRequestFilter {
    @Inject SecurityIdentity identity;
    @Inject CurrentUser currentUser;
    @Inject AdminEmailConfig admin;
    @ConfigProperty(name = "app.auth.test-mode", defaultValue = "false") boolean testMode;

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return;
        }
        String path = request.getUriInfo().getPath();
        if (path.startsWith("q/")) return;

        String email = null;
        String sub = null;
        String displayName = null;
        if (testMode) {
            email = request.getHeaderString("X-Test-User-Email");
            sub = request.getHeaderString("X-Test-User-Sub");
        } else if (identity != null && !identity.isAnonymous() && identity.getPrincipal() instanceof JsonWebToken jwt) {
            email = jwt.getClaim("email");
            sub = jwt.getSubject();
            displayName = jwt.getClaim("name");
        }
        if (email == null || email.isBlank()) {
            abort(request, 401, "Authentication required", "UNAUTHORIZED");
            return;
        }
        email = email.trim().toLowerCase();
        boolean isAdmin = email.equals(admin.email());
        currentUser.initialize(email, sub, displayName, isAdmin);
    }

    private static void abort(ContainerRequestContext request, int status, String message, String code) {
        request.abortWith(Response.status(status).entity(Map.of("message", message, "code", code)).build());
    }
}
