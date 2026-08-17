package app.medicamentos.medication;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class MedicationDeleteTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void hardDeletesMedicationSchedulesAndAllDoseHistory() {
        String email = "delete-cascade@example.com";
        authorize(email);
        String med = medication(email, "Para Excluir", 20, 10);

        String takenSchedule = schedule(email, med, "INDEFINITE", null, 1);
        scheduler.run();
        String takenId = pendingDose(email, takenSchedule);
        as(email).post(API + "/doses/" + takenId + "/take").then().statusCode(200);

        String skippedSchedule = schedule(email, med, "INDEFINITE", null, 1);
        scheduler.run();
        String skippedId = pendingDose(email, skippedSchedule);
        as(email).post(API + "/doses/" + skippedId + "/skip").then().statusCode(200);

        String pendingSchedule = schedule(email, med, "INDEFINITE", null, 1);
        scheduler.run();
        String pendingId = pendingDose(email, pendingSchedule);

        as(email).delete(API + "/medications/" + med).then().statusCode(204);
        as(email).get(API + "/medications").then().statusCode(200).body("id", not(hasItem(med)));
        as(email).get(API + "/medications/" + med).then().statusCode(404);
        as(email).get(API + "/schedules/" + takenSchedule).then().statusCode(404);
        as(email).get(API + "/schedules/" + skippedSchedule).then().statusCode(404);
        as(email).get(API + "/schedules/" + pendingSchedule).then().statusCode(404);
        as(email).get(API + "/doses?medicationId=" + med).then().statusCode(200).body("", hasSize(0));
        as(email).get(API + "/doses?status=PENDING").then().statusCode(200)
                .body("id", not(hasItem(pendingId)))
                .body("medicationId", not(hasItem(med)));
        as(email).get(API + "/doses?status=TAKEN").then().statusCode(200).body("id", not(hasItem(takenId)));
        as(email).get(API + "/doses?status=SKIPPED").then().statusCode(200).body("id", not(hasItem(skippedId)));
        as(email).delete(API + "/medications/" + med).then().statusCode(404);
    }

    @Test void hardDeletesEveryScheduleStatus() {
        String email = "delete-multi-sched@example.com";
        authorize(email);
        String med = medication(email, "Varias Agendas", 8, 10);
        String active = schedule(email, med, "INDEFINITE", null, 1);
        String paused = schedule(email, med, "INDEFINITE", null, 1);
        as(email).body(Map.of("status", "PAUSED")).patch(API + "/schedules/" + paused).then().statusCode(200);
        String cancelled = schedule(email, med, "INDEFINITE", null, 1);
        as(email).body(Map.of("status", "CANCELLED")).patch(API + "/schedules/" + cancelled).then().statusCode(200);

        as(email).delete(API + "/medications/" + med).then().statusCode(204);
        as(email).get(API + "/schedules/" + active).then().statusCode(404);
        as(email).get(API + "/schedules/" + paused).then().statusCode(404);
        as(email).get(API + "/schedules/" + cancelled).then().statusCode(404);
        as(email).get(API + "/medications/" + med).then().statusCode(404);
    }

    @Test void hardDeletesCatalogOnlyMedication() {
        String email = "delete-catalog-only@example.com";
        authorize(email);
        String med = medication(email, "So Catalogo", 3, 10);
        as(email).delete(API + "/medications/" + med).then().statusCode(204);
        as(email).get(API + "/medications/" + med).then().statusCode(404);
        as(email).get(API + "/medications").then().statusCode(200).body("id", not(hasItem(med)));
    }

    private String pendingDose(String email, String scheduleId) {
        return as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + scheduleId + "' }.id");
    }
}
