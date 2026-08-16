package app.medicamentos.medication;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MedicationIntegerFieldsTest extends ApiTestBase {
    @Test void rejectsNegativeStockAndThresholdAllowsZeroAndDefaultsTen() {
        String email = "int-fields@example.com";
        authorize(email);
        as(email).body(Map.of("name", "Neg", "stockQuantity", -1, "purchaseThresholdUnits", 10))
                .post(API + "/medications").then().statusCode(400);
        as(email).body(Map.of("name", "NegT", "stockQuantity", 1, "purchaseThresholdUnits", -1))
                .post(API + "/medications").then().statusCode(400);

        String id = as(email).body(Map.of("name", "ZeroOk", "stockQuantity", 0))
                .post(API + "/medications").then().statusCode(201)
                .body("stockQuantity", equalTo(0))
                .body("purchaseThresholdUnits", equalTo(10))
                .extract().path("id");

        as(email).body(Map.of("stockQuantity", 0, "purchaseThresholdUnits", 0)).patch(API + "/medications/" + id)
                .then().statusCode(200).body("purchaseThresholdUnits", equalTo(0)).body("purchaseNeeded", equalTo(true));
    }
}
