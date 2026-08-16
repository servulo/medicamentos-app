package app.medicamentos.medication;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.*;

@Path("/alerts/purchase")
@Produces(MediaType.APPLICATION_JSON)
public class PurchaseAlertResource {
    @Inject MedicationService service;

    @GET
    public List<Alert> list() {
        return service.list().stream().filter(MedicationEntity::purchaseNeeded).map(Alert::of).toList();
    }

    public record Alert(UUID medicationId, String name, String unit, int stockQuantity, int purchaseThresholdUnits) {
        static Alert of(MedicationEntity m) {
            return new Alert(m.id, m.name, m.unit, m.stockQuantity, m.purchaseThresholdUnits);
        }
    }
}
