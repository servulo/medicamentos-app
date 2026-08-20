package app.medicamentos.schedule;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ScheduleDeleteIsolationTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void otherUserCannotDeleteSchedule() {
        String owner = "sched-del-owner@example.com";
        String other = "sched-del-other@example.com";
        authorize(owner);
        authorize(other);
        String med = medication(owner, "Owner Only", 10, 10);
        String id = schedule(owner, med, "INDEFINITE", null);

        as(other).delete(API + "/schedules/" + id).then().statusCode(404);
        as(owner).get(API + "/schedules/" + id).then().statusCode(200);
    }

    @Test void deletingOneScheduleLeavesSiblingIntact() {
        String email = "sched-del-sibling@example.com";
        authorize(email);
        String med = medication(email, "Sibling Med", 10, 10);
        String first = schedule(email, med, "INDEFINITE", null, 1);
        String second = schedule(email, med, "INDEFINITE", null, 1);

        scheduler.run();
        String doseFirst = pendingDose(email, first);
        as(email).post(API + "/doses/" + doseFirst + "/take").then().statusCode(200);
        scheduler.run();
        String doseSecond = pendingDose(email, second);

        as(email).delete(API + "/schedules/" + first).then().statusCode(204);
        as(email).get(API + "/schedules/" + first).then().statusCode(404);
        as(email).get(API + "/schedules/" + second).then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200);
        as(email).get(API + "/doses?medicationId=" + med + "&status=PENDING").then().statusCode(200)
                .body("id", hasItem(doseSecond));
        as(email).get(API + "/doses?medicationId=" + med + "&status=TAKEN").then().statusCode(200).body("", hasSize(0));
    }

    private String pendingDose(String email, String scheduleId) {
        return as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + scheduleId + "' }.id");
    }
}
