package app.medicamentos.dose;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DoseStockFromScheduleTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void takeUsesScheduleQuantityAndPatchUpdatesStoredValue() {
        String email = "dose-stock@example.com";
        authorize(email);
        String med = medication(email, "Multi Agenda", 20, 10);
        String a = schedule(email, med, "INDEFINITE", null, 1);
        String b = schedule(email, med, "INDEFINITE", null, 2);
        scheduler.run();

        String doseA = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + a + "' }.id");
        String doseB = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + b + "' }.id");

        as(email).post(API + "/doses/" + doseA + "/take").then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("stockQuantity", equalTo(19));
        as(email).post(API + "/doses/" + doseB + "/take").then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("stockQuantity", equalTo(17));

        as(email).body(Map.of("quantityPerDose", 3)).patch(API + "/schedules/" + b)
                .then().statusCode(200).body("quantityPerDose", equalTo(3));

        String c = schedule(email, med, "INDEFINITE", null, 3);
        scheduler.run();
        String doseC = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + c + "' }.id");
        as(email).post(API + "/doses/" + doseC + "/take").then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("stockQuantity", equalTo(14));
    }
}
