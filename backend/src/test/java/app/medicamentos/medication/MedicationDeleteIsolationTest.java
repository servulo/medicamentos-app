package app.medicamentos.medication;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MedicationDeleteIsolationTest extends ApiTestBase {
    @Test void cannotDeleteAnotherUsersMedicationOrScheduleOnDeleted() {
        String owner = "owner-del@example.com";
        String other = "other-del@example.com";
        authorize(owner);
        authorize(other);
        String med = medication(owner, "Privado", 5, 10);
        as(other).delete(API + "/medications/" + med).then().statusCode(404);
        as(owner).get(API + "/medications/" + med).then().statusCode(200).body("name", equalTo("Privado"));

        as(owner).delete(API + "/medications/" + med).then().statusCode(204);
        as(owner).body(Map.of(
                "medicationId", med,
                "daysOfWeek", java.util.List.of(1),
                "timesOfDay", java.util.List.of("08:00"),
                "durationType", "INDEFINITE",
                "quantityPerDose", 1
        )).post(API + "/schedules").then().statusCode(404);
    }
}
