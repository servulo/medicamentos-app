package app.medicamentos.schedule;

import app.medicamentos.notify.DoseScheduler;
import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ScheduleEditTest extends ApiTestBase {
    @Inject DoseScheduler scheduler;
    @Inject ScheduleTestHelper schedules;

    @Test void structuralPatchUpdatesFields() {
        String email = "edit-struct@example.com";
        authorize(email);
        String med = medication(email, "Edit Med", 20, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "INDEFINITE", null, 1);

        as(email).body(structuralBody(days(), List.of(otherTime()), "FIXED_TAKEN_DOSES", 10, 2))
                .patch(API + "/schedules/" + id).then().statusCode(200)
                .body("daysOfWeek", containsInAnyOrder(days().toArray()))
                .body("timesOfDay", containsInAnyOrder(otherTime()))
                .body("durationType", equalTo("FIXED_TAKEN_DOSES"))
                .body("maxTakenDoses", equalTo(10))
                .body("quantityPerDose", equalTo(2));
    }

    @Test void pausedScheduleReactivatesOnStructuralPatch() {
        String email = "edit-paused@example.com";
        authorize(email);
        String med = medication(email, "Paused", 10, 10);
        String id = schedule(email, med, "INDEFINITE", null);
        as(email).body(Map.of("status", "PAUSED")).patch(API + "/schedules/" + id).then().statusCode(200);

        as(email).body(structuralBody(days(), List.of(nowTime()), "INDEFINITE", null, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200).body("status", equalTo("ACTIVE"));
    }

    @Test void cancelledScheduleReactivatesOnStructuralPatch() {
        String email = "edit-cancel@example.com";
        authorize(email);
        String med = medication(email, "Cancelled", 10, 10);
        String id = schedule(email, med, "INDEFINITE", null);
        as(email).body(Map.of("status", "CANCELLED")).patch(API + "/schedules/" + id).then().statusCode(200);

        as(email).body(structuralBody(days(), List.of(nowTime()), "INDEFINITE", null, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200).body("status", equalTo("ACTIVE"));
    }

    @Test void rejectsMaxBelowTakenCount() {
        String email = "edit-max@example.com";
        authorize(email);
        String med = medication(email, "Max", 10, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 3, 1);
        setTakenCount(id, 2);

        as(email).body(structuralBody(days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 1, 1))
                .patch(API + "/schedules/" + id).then().statusCode(400);
    }

    @Test void changingTimesPurgesPendingIncludingSnoozed() {
        String email = "edit-times@example.com";
        authorize(email);
        String med = medication(email, "Times", 10, 10);
        String time = nowTime();
        String id = scheduleWith(email, med, days(), List.of(time), "INDEFINITE", null, 1);
        scheduler.run();
        String pendingId = pendingDose(email, id);
        as(email).body(Map.of("minutes", 10)).post(API + "/doses/" + pendingId + "/snooze").then().statusCode(200);

        as(email).body(structuralBody(days(), List.of(otherTime()), "INDEFINITE", null, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200);
        as(email).get(API + "/doses?status=PENDING&medicationId=" + med).then().statusCode(200).body("id", not(hasItem(pendingId)));
    }

    @Test void changingTimesKeepsTakenHistory() {
        String email = "edit-times-taken@example.com";
        authorize(email);
        String med = medication(email, "TimesTaken", 10, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "INDEFINITE", null, 1);
        scheduler.run();
        String takenId = pendingDose(email, id);
        as(email).post(API + "/doses/" + takenId + "/take").then().statusCode(200);

        as(email).body(structuralBody(days(), List.of(otherTime()), "INDEFINITE", null, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200);
        as(email).get(API + "/doses?status=TAKEN&medicationId=" + med).then().statusCode(200).body("id", hasItem(takenId));
    }

    @Test void changingDaysPurgesPending() {
        String email = "edit-days@example.com";
        authorize(email);
        String med = medication(email, "Days", 10, 10);
        int today = today();
        int otherDay = today == 7 ? 1 : today + 1;
        String id = scheduleWith(email, med, List.of(today), List.of(nowTime()), "INDEFINITE", null, 1);
        scheduler.run();
        pendingDose(email, id);

        as(email).body(structuralBody(List.of(otherDay), List.of(nowTime()), "INDEFINITE", null, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200);
        as(email).get(API + "/doses?status=PENDING&medicationId=" + med).then().statusCode(200).body("", hasSize(0));
    }

    @Test void durationOnlyChangeDoesNotPurgePending() {
        String email = "edit-duration@example.com";
        authorize(email);
        String med = medication(email, "Duration", 10, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "INDEFINITE", null, 1);
        scheduler.run();
        String pendingId = pendingDose(email, id);

        as(email).body(structuralBody(days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 5, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200);
        as(email).get(API + "/doses?status=PENDING").then().statusCode(200).body("id", hasItem(pendingId));
    }

    @Test void completedBecomesActiveWhenMaxIncreased() {
        String email = "edit-completed@example.com";
        authorize(email);
        String med = medication(email, "Completed", 10, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 1, 1);
        takeDoses(email, id, 1);
        as(email).get(API + "/schedules/" + id).then().statusCode(200).body("status", equalTo("COMPLETED"));

        as(email).body(structuralBody(days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 5, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200).body("status", equalTo("ACTIVE"));
    }

    @Test void activeBecomesCompletedWhenMaxReached() {
        String email = "edit-to-completed@example.com";
        authorize(email);
        String med = medication(email, "ToCompleted", 10, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 5, 1);
        setTakenCount(id, 2);

        as(email).body(structuralBody(days(), List.of(nowTime()), "FIXED_TAKEN_DOSES", 2, 1))
                .patch(API + "/schedules/" + id).then().statusCode(200).body("status", equalTo("COMPLETED"));
    }

    @Test void ignoresMedicationIdChange() {
        String email = "edit-medid@example.com";
        authorize(email);
        String med = medication(email, "Keep Med", 10, 10);
        String other = medication(email, "Other Med", 10, 10);
        String id = schedule(email, med, "INDEFINITE", null);

        Map<String, Object> body = structuralBody(days(), List.of(nowTime()), "INDEFINITE", null, 1);
        body.put("medicationId", other);
        as(email).body(body).patch(API + "/schedules/" + id).then().statusCode(200).body("medicationId", equalTo(med));
    }

    @Test void otherUserStructuralPatchReturns404() {
        String owner = "edit-owner@example.com";
        String other = "edit-other@example.com";
        authorize(owner);
        authorize(other);
        String med = medication(owner, "Owner Med", 10, 10);
        String id = schedule(owner, med, "INDEFINITE", null);

        as(other).body(structuralBody(days(), List.of(nowTime()), "INDEFINITE", null, 1))
                .patch(API + "/schedules/" + id).then().statusCode(404);
        as(owner).get(API + "/schedules/" + id).then().statusCode(200);
    }

    @Test void quantityChangeDoesNotRetroactivelyAdjustStock() {
        String email = "edit-stock@example.com";
        authorize(email);
        String med = medication(email, "Stock", 10, 10);
        String id = scheduleWith(email, med, days(), List.of(nowTime()), "INDEFINITE", null, 1);
        scheduler.run();
        String doseId = pendingDose(email, id);
        as(email).post(API + "/doses/" + doseId + "/take").then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("stockQuantity", equalTo(9));

        as(email).body(structuralBody(days(), List.of(nowTime()), "INDEFINITE", null, 2))
                .patch(API + "/schedules/" + id).then().statusCode(200);
        as(email).get(API + "/medications/" + med).then().statusCode(200).body("stockQuantity", equalTo(9));
    }

    private static int today() {
        return ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).getDayOfWeek().getValue();
    }

    private static List<Integer> days() {
        return List.of(today());
    }

    private static String nowTime() {
        return ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).toLocalTime().withSecond(0).withNano(0).toString();
    }

    private static String otherTime() {
        int hour = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).getHour();
        int other = (hour + 1) % 24;
        return String.format("%02d:00", other);
    }

    private Map<String, Object> structuralBody(List<Integer> days, List<String> times, String duration,
                                               Integer max, int qty) {
        Map<String, Object> body = new HashMap<>();
        body.put("daysOfWeek", days);
        body.put("timesOfDay", times);
        body.put("durationType", duration);
        body.put("quantityPerDose", qty);
        if (max != null) body.put("maxTakenDoses", max);
        return body;
    }

    private String scheduleWith(String email, String med, List<Integer> days, List<String> times,
                                String duration, Integer max, int qty) {
        Map<String, Object> body = structuralBody(days, times, duration, max, qty);
        body.put("medicationId", med);
        return as(email).body(body).post(API + "/schedules").then().statusCode(201).extract().path("id");
    }

    private void setTakenCount(String id, int count) {
        schedules.setTakenCount(UUID.fromString(id), count);
    }

    private void takeDoses(String email, String scheduleId, int count) {
        for (int i = 0; i < count; i++) {
            scheduler.run();
            String doseId = pendingDose(email, scheduleId);
            as(email).post(API + "/doses/" + doseId + "/take").then().statusCode(200);
        }
    }

    private String pendingDose(String email, String scheduleId) {
        return as(email).get(API + "/doses?status=PENDING").then().statusCode(200).extract().jsonPath()
                .getString("find { it.scheduleId == '" + scheduleId + "' }.id");
    }
}
