package app.medicamentos.device;

import app.medicamentos.auth.CurrentUser;
import app.medicamentos.medication.MedicationService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.*;
import java.time.OffsetDateTime;
import java.util.*;

@Path("/devices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DeviceResource {
    @Inject CurrentUser user;

    @GET
    public List<View> list() {
        return PushDeviceEntity.<PushDeviceEntity>list("userId = ?1 order by createdAt", user.id())
                .stream().map(View::of).toList();
    }

    @POST
    @Transactional
    public Response register(Register r, @Context UriInfo uri) {
        validate(r);
        PushDeviceEntity d = PushDeviceEntity.find("endpoint", r.endpoint).firstResult();
        boolean created = d == null;
        if (d != null && !d.userId.equals(user.id())) throw MedicationService.error(409, "Endpoint is registered to another user");
        if (d == null) {
            d = new PushDeviceEntity();
            d.id = UUID.randomUUID();
            d.userId = user.id();
            d.endpoint = r.endpoint;
            d.createdAt = OffsetDateTime.now();
        }
        d.p256dh = r.p256dh;
        d.auth = r.auth;
        d.userAgent = r.userAgent;
        d.isMobile = r.isMobile;
        d.lastSeenAt = OffsetDateTime.now();
        d.persist();
        Response.ResponseBuilder response = created
                ? Response.created(URI.create(uri.getAbsolutePath() + "/" + d.id))
                : Response.ok();
        return response.entity(View.of(d)).build();
    }

    @DELETE @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") UUID id) {
        PushDeviceEntity d = PushDeviceEntity.find("id = ?1 and userId = ?2", id, user.id()).firstResult();
        if (d == null) throw MedicationService.error(404, "Device not found");
        d.delete();
        return Response.noContent().build();
    }

    private static void validate(Register r) {
        if (r == null || r.endpoint == null || r.p256dh == null || r.auth == null)
            throw MedicationService.error(400, "endpoint, p256dh and auth are required");
        try {
            URI parsed = URI.create(r.endpoint);
            if (parsed.getScheme() == null || (!parsed.getScheme().equals("https") && !parsed.getScheme().equals("http")))
                throw new IllegalArgumentException();
        } catch (RuntimeException e) { throw MedicationService.error(400, "Invalid endpoint URI"); }
    }

    public static class Register {
        public String endpoint;
        public String p256dh;
        public String auth;
        public boolean isMobile;
        public String userAgent;
    }
    public record View(UUID id, String endpoint, boolean isMobile, OffsetDateTime lastSeenAt) {
        static View of(PushDeviceEntity d) { return new View(d.id, d.endpoint, d.isMobile, d.lastSeenAt); }
    }
}
