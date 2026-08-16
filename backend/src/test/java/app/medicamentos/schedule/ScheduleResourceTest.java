package app.medicamentos.schedule;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ScheduleResourceTest extends ApiTestBase {
    @Test void createsPausesAndReactivatesFixedSchedule() {
        String email = "schedule@example.com";
        authorize(email);
        String medicationId = medication(email, "Agenda", 10, 2);
        String id = schedule(email, medicationId, "FIXED_TAKEN_DOSES", 2);
        as(email).body(Map.of("status", "PAUSED")).patch(API + "/schedules/" + id)
                .then().statusCode(200).body("status", equalTo("PAUSED"));
        as(email).body(Map.of("status", "ACTIVE")).patch(API + "/schedules/" + id)
                .then().statusCode(200).body("status", equalTo("ACTIVE")).body("takenCount", equalTo(0));
    }
}
