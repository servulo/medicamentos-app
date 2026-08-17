package app.medicamentos.schedule;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ScheduleQuantityPerDoseTest extends ApiTestBase {
    @Test void createUpdateAndRejectInvalidQuantityPerDose() {
        String email = "qty-sched@example.com";
        authorize(email);
        String med = medication(email, "Agenda Qty", 30, 10);
        String id = schedule(email, med, "INDEFINITE", null, 2);
        as(email).get(API + "/schedules/" + id).then().statusCode(200).body("quantityPerDose", equalTo(2));

        as(email).body(Map.of("quantityPerDose", 3)).patch(API + "/schedules/" + id)
                .then().statusCode(200).body("quantityPerDose", equalTo(3));

        as(email).body(Map.of(
                "medicationId", med,
                "daysOfWeek", java.util.List.of(1),
                "timesOfDay", java.util.List.of("09:00"),
                "durationType", "INDEFINITE",
                "quantityPerDose", 0
        )).post(API + "/schedules").then().statusCode(400);

        as(email).body(Map.of("quantityPerDose", 0)).patch(API + "/schedules/" + id).then().statusCode(400);
    }
}
