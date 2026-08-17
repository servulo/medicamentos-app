package app.medicamentos.medication;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.*;

@Path("/medications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MedicationResource {
    @Inject MedicationService service;

    @GET
    public List<View> list() { return service.list().stream().map(View::of).toList(); }

    @GET @Path("/{id}")
    public View get(@PathParam("id") UUID id) { return View.of(service.get(id)); }

    @POST
    public Response create(Change c, @Context UriInfo uri) {
        if (c == null) throw MedicationService.error(400, "Request body is required");
        MedicationEntity m = service.create(c.name, c.unit, c.stockQuantity, c.purchaseThresholdUnits);
        return Response.created(URI.create(uri.getAbsolutePath() + "/" + m.id)).entity(View.of(m)).build();
    }

    @PATCH @Path("/{id}")
    public View update(@PathParam("id") UUID id, Change c) {
        if (c == null) throw MedicationService.error(400, "Request body is required");
        return View.of(service.update(id, c.name, c.unit, c.stockQuantity, c.purchaseThresholdUnits));
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }

    public static class Change {
        public String name;
        public String unit;
        public Integer stockQuantity;
        public Integer purchaseThresholdUnits;
    }

    public record View(UUID id, String name, String unit, int stockQuantity,
                       int purchaseThresholdUnits, boolean purchaseNeeded) {
        public static View of(MedicationEntity m) {
            return new View(m.id, m.name, m.unit, m.stockQuantity,
                    m.purchaseThresholdUnits, m.purchaseNeeded());
        }
    }
}
