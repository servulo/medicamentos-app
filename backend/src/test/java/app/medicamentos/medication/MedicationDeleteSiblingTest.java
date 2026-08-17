package app.medicamentos.medication;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MedicationDeleteSiblingTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void deletingOneMedicationLeavesSiblingCatalogSchedulesDosesAndAlerts() {
        String email = "delete-sibling@example.com";
        authorize(email);
        String first = medication(email, "Apagar", 5, 10);
        String sibling = medication(email, "Manter", 4, 10);
        String firstSchedule = schedule(email, first, "INDEFINITE", null, 1);
        String siblingSchedule = schedule(email, sibling, "INDEFINITE", null, 1);
        scheduler.run();
        String siblingDose = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + siblingSchedule + "' }.id");

        as(email).get(API + "/alerts/purchase").then().statusCode(200)
                .body("medicationId", hasItems(first, sibling));

        as(email).delete(API + "/medications/" + first).then().statusCode(204);

        as(email).get(API + "/medications/" + sibling).then().statusCode(200).body("name", equalTo("Manter"));
        as(email).get(API + "/schedules/" + siblingSchedule).then().statusCode(200).body("status", equalTo("ACTIVE"));
        as(email).get(API + "/schedules/" + firstSchedule).then().statusCode(404);
        as(email).get(API + "/doses?medicationId=" + sibling).then().statusCode(200)
                .body("id", hasItem(siblingDose));
        as(email).get(API + "/doses?medicationId=" + first).then().statusCode(200).body("", hasSize(0));
        as(email).get(API + "/alerts/purchase").then().statusCode(200)
                .body("medicationId", not(hasItem(first)))
                .body("medicationId", hasItem(sibling));
    }
}
