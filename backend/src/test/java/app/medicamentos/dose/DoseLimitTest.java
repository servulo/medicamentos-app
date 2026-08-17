package app.medicamentos.dose;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DoseLimitTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void takenDoseCompletesFixedSchedule() {
        String email = "limit@example.com";
        authorize(email);
        String med = medication(email, "Limitado", 10, 2);
        String schedule = schedule(email, med, "FIXED_TAKEN_DOSES", 1);
        scheduler.run();
        String dose = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + schedule + "' }.id");
        as(email).post(API + "/doses/" + dose + "/take").then().statusCode(200);
        as(email).get(API + "/schedules/" + schedule).then().statusCode(200)
                .body("takenCount", equalTo(1)).body("status", equalTo("COMPLETED"));
    }
}
