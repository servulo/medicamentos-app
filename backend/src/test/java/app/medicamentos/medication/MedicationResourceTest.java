package app.medicamentos.medication;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MedicationResourceTest extends ApiTestBase {
    @Test void createsListsGetsAndUpdatesMedication() {
        String email = "medication@example.com";
        authorize(email);
        String id = medication(email, "Dipirona", 20, 10);
        as(email).get(API + "/medications/" + id).then().statusCode(200)
                .body("name", equalTo("Dipirona"))
                .body("purchaseThresholdUnits", equalTo(10))
                .body("$", not(hasKey("quantityPerDose")));
        as(email).body(Map.of("stockQuantity", 5)).patch(API + "/medications/" + id)
                .then().statusCode(200).body("stockQuantity", equalTo(5));
        as(email).get(API + "/medications").then().statusCode(200).body("id", hasItem(id));
    }
}
