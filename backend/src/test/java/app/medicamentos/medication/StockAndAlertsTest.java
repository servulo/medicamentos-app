package app.medicamentos.medication;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class StockAndAlertsTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;

    @Test void takeDecrementsStockAndPurchaseAlertIsInAppByUnits() {
        String email = "stock@example.com";
        authorize(email);
        String med = medication(email, "Estoque", 2, 1);
        String schedule = schedule(email, med, "INDEFINITE", null, 1);
        scheduler.run();
        String dose = as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + schedule + "' }.id");
        as(email).post(API + "/doses/" + dose + "/take").then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200)
                .body("stockQuantity", equalTo(1))
                .body("purchaseThresholdUnits", equalTo(1))
                .body("purchaseNeeded", equalTo(true));
        as(email).get(API + "/alerts/purchase").then().statusCode(200)
                .body("medicationId", hasItem(med))
                .body("find { it.medicationId == '" + med + "' }.purchaseThresholdUnits", equalTo(1))
                .body("find { it.medicationId == '" + med + "' }", not(hasKey("remainingDoses")))
                .body("find { it.medicationId == '" + med + "' }", not(hasKey("quantityPerDose")));
    }

    @Test void noAlertWhenStockAboveThresholdEvenIfDoseQtyLarge() {
        String email = "stock-above@example.com";
        authorize(email);
        String med = medication(email, "Acima", 15, 10);
        schedule(email, med, "INDEFINITE", null, 5);
        as(email).get(API + "/alerts/purchase").then().statusCode(200).body("medicationId", not(hasItem(med)));
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("purchaseNeeded", equalTo(false));
    }
}
