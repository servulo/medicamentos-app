package app.medicamentos.dose;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DoseHistoryTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void filtersResolvedHistoryByMedicationAndStatus() {
        String email = "history@example.com";
        authorize(email);
        String med = medication(email, "Historico", 10, 2);
        String schedule = schedule(email, med, "INDEFINITE", null);
        scheduler.run();
        String dose = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + schedule + "' }.id");
        as(email).post(API + "/doses/" + dose + "/skip").then().statusCode(200);
        as(email).get(API + "/doses?status=SKIPPED&medicationId=" + med).then().statusCode(200)
                .body("id", hasItem(dose)).body("status", everyItem(equalTo("SKIPPED")));
    }
}
