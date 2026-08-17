package app.medicamentos.admin;

import app.medicamentos.auth.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;

@Path("/admin/whitelist")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WhitelistResource {
    @Inject CurrentUser user;
    @Inject AdminEmailConfig admin;
    @Inject WhitelistService service;

    @GET
    public List<Entry> list() {
        requireAdmin();
        return service.list().stream().map(Entry::of).toList();
    }

    @POST
    public Response add(Create request, @Context UriInfo uri) {
        requireAdmin();
        WhitelistEntryEntity entry = service.add(request == null ? null : request.email(), user.email());
        return Response.created(URI.create(uri.getAbsolutePath() + "/" + entry.id)).entity(Entry.of(entry)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        requireAdmin();
        service.remove(id, admin.email());
        return Response.noContent().build();
    }

    private void requireAdmin() {
        if (!user.admin()) throw WhitelistService.error(403, "Administrator access required");
    }

    public record Create(String email) {}
    public record Entry(UUID id, String email, OffsetDateTime createdAt) {
        static Entry of(WhitelistEntryEntity e) { return new Entry(e.id, e.email, e.createdAt); }
    }
}
