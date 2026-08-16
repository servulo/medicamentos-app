package app.medicamentos.dose;

import app.medicamentos.medication.MedicationEntity;
import app.medicamentos.medication.MedicationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.*;

@Path("/doses")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DoseResource {
    @Inject DoseService service;

    @GET
    public List<View> list(@QueryParam("medicationId") UUID medicationId, @QueryParam("status") DoseStatus status,
                           @QueryParam("from") OffsetDateTime from, @QueryParam("to") OffsetDateTime to) {
        return service.list(medicationId, status, from, to).stream().map(View::of).toList();
    }
    @POST @Path("/{id}/take")
    public View take(@PathParam("id") UUID id) { return View.of(service.take(id)); }
    @POST @Path("/{id}/skip")
    public View skip(@PathParam("id") UUID id) { return View.of(service.skip(id)); }
    @POST @Path("/{id}/snooze")
    public View snooze(@PathParam("id") UUID id, Snooze request) {
        if (request == null) throw MedicationService.error(400, "Request body is required");
        return View.of(service.snooze(id, request.minutes));
    }

    public static class Snooze { public int minutes; }
    public record View(UUID id, UUID scheduleId, UUID medicationId, String medicationName,
                       OffsetDateTime scheduledAt, OffsetDateTime originalScheduledAt,
                       DoseStatus status, int snoozeCount, OffsetDateTime resolvedAt) {
        public static View of(DoseOccurrenceEntity d) {
            MedicationEntity m = MedicationEntity.findById(d.medicationId);
            String name = m == null ? null : m.name;
            return new View(d.id, d.scheduleId, d.medicationId, name, d.scheduledAt, d.originalScheduledAt,
                    d.status, d.snoozeCount, d.resolvedAt);
        }
    }
}
