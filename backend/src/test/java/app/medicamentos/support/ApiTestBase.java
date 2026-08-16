package app.medicamentos.support;

import io.restassured.specification.RequestSpecification;
import static io.restassured.RestAssured.given;
import java.time.*;
import java.util.*;

public abstract class ApiTestBase {
    protected static final String API = "";
    protected static final String ADMIN = "admin@example.com";

    protected RequestSpecification as(String email) {
        return given().header("X-Test-User-Email", email).contentType("application/json");
    }

    protected void authorize(String email) {
        as(ADMIN).body(Map.of("email", email)).post(API + "/admin/whitelist").then().statusCode(org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.is(200), org.hamcrest.Matchers.is(201)));
    }

    protected String medication(String email, String name, int stock, int threshold) {
        return as(email).body(Map.of("name", name, "stockQuantity", stock, "purchaseThresholdUnits", threshold))
                .post(API + "/medications").then().statusCode(201).extract().path("id");
    }

    protected String schedule(String email, String medicationId, String duration, Integer max) {
        return schedule(email, medicationId, duration, max, 1);
    }

    protected String schedule(String email, String medicationId, String duration, Integer max, int quantityPerDose) {
        ZoneId zone = ZoneId.of("America/Sao_Paulo");
        ZonedDateTime now = ZonedDateTime.now(zone);
        Map<String, Object> body = new HashMap<>();
        body.put("medicationId", medicationId);
        body.put("daysOfWeek", List.of(now.getDayOfWeek().getValue()));
        body.put("timesOfDay", List.of(now.toLocalTime().withSecond(0).withNano(0).toString()));
        body.put("durationType", duration);
        body.put("quantityPerDose", quantityPerDose);
        if (max != null) body.put("maxTakenDoses", max);
        return as(email).body(body).post(API + "/schedules").then().statusCode(201).extract().path("id");
    }
}
