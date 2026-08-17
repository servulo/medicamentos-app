package app.medicamentos.dose;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DoseResourceTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void snoozesSkipsAndRejectsResolvedDose() {
        String email = "dose@example.com";
        authorize(email);
        String med = medication(email, "Dose", 10, 2);
        String schedule = schedule(email, med, "INDEFINITE", null);
        scheduler.run();
        String id = as(email).get(API + "/doses?status=PENDING").then().statusCode(200)
                .extract().jsonPath().getString("find { it.scheduleId == '" + schedule + "' }.id");
        as(email).body(Map.of("minutes", 10)).post(API + "/doses/" + id + "/snooze")
                .then().statusCode(200).body("snoozeCount", equalTo(1));
        as(email).post(API + "/doses/" + id + "/skip").then().statusCode(200).body("status", equalTo("SKIPPED"));
        as(email).post(API + "/doses/" + id + "/take").then().statusCode(409);
    }
}
