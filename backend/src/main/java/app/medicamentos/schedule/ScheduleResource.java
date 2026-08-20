package app.medicamentos.schedule;

import app.medicamentos.medication.MedicationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.net.URI;
import java.util.*;

@Path("/schedules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScheduleResource {
    @Inject ScheduleService service;

    @GET
    public List<View> list(@QueryParam("medicationId") UUID medicationId, @QueryParam("status") ScheduleStatus status) {
        return service.list(medicationId, status).stream().map(View::of).toList();
    }
    @GET @Path("/{id}")
    public View get(@PathParam("id") UUID id) { return View.of(service.get(id)); }

    @POST
    public Response create(Change c, @Context UriInfo uri) {
        if (c == null || c.medicationId == null) throw MedicationService.error(400, "medicationId is required");
        TreatmentScheduleEntity s = service.create(c.medicationId, c.daysOfWeek, c.timesOfDay,
                c.durationType, c.maxTakenDoses, c.quantityPerDose);
        return Response.created(URI.create(uri.getAbsolutePath() + "/" + s.id)).entity(View.of(s)).build();
    }

    @PATCH @Path("/{id}")
    public View update(@PathParam("id") UUID id, Change c) {
        if (c == null) throw MedicationService.error(400, "Request body is required");
        return View.of(service.update(id, c.status, c.daysOfWeek, c.timesOfDay,
                c.durationType, c.maxTakenDoses, c.resetTakenCount, c.quantityPerDose));
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        service.delete(id);
        return Response.noContent().build();
    }

    public static class Change {
        public UUID medicationId;
        public ScheduleStatus status;
        public List<Integer> daysOfWeek;
        public List<String> timesOfDay;
        public DurationType durationType;
        public Integer maxTakenDoses;
        public Boolean resetTakenCount;
        public Integer quantityPerDose;
    }
    public record View(UUID id, UUID medicationId, ScheduleStatus status, List<Integer> daysOfWeek,
                       List<String> timesOfDay, DurationType durationType, Integer maxTakenDoses,
                       int takenCount, int quantityPerDose) {
        static View of(TreatmentScheduleEntity s) {
            return new View(s.id, s.medicationId, s.status, ScheduleService.days(s),
                    ScheduleService.times(s), s.durationType, s.maxTakenDoses, s.takenCount, s.quantityPerDose);
        }
    }
}
