package app.medicamentos.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {
    @Inject CurrentUser user;
    @ConfigProperty(name = "app.timezone") String timezone;

    @GET
    public Profile me() {
        return new Profile(user.email(), user.displayName(), user.admin(), timezone);
    }

    public record Profile(String email, String displayName, boolean admin, String timezone) {}
}
