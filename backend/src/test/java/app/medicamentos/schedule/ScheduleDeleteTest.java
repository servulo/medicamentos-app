package app.medicamentos.schedule;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ScheduleDeleteTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void deleteRemovesScheduleAndAllDoseHistory() {
        String email = "sched-delete@example.com";
        authorize(email);
        String med = medication(email, "Delete Sched", 20, 10);
        String id = schedule(email, med, "INDEFINITE", null, 1);

        scheduler.run();
        String takenId = pendingDose(email, id);
        as(email).post(API + "/doses/" + takenId + "/take").then().statusCode(200);

        int stockBefore = as(email).get(API + "/medications/" + med).then().statusCode(200).extract().path("stockQuantity");

        as(email).delete(API + "/schedules/" + id).then().statusCode(204);
        as(email).get(API + "/schedules/" + id).then().statusCode(404);
        as(email).get(API + "/doses?medicationId=" + med).then().statusCode(200).body("id", not(hasItem(takenId)));
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("stockQuantity", equalTo(stockBefore));
        as(email).delete(API + "/schedules/" + id).then().statusCode(404);
    }

    private String pendingDose(String email, String scheduleId) {
        return as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + scheduleId + "' }.id");
    }
}
