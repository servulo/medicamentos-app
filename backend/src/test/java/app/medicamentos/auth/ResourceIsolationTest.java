package app.medicamentos.auth;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;

@QuarkusTest
class ResourceIsolationTest extends ApiTestBase {
    @Test void hidesOtherUsersResources() {
        String a = "isolation-a@example.com", b = "isolation-b@example.com";
        authorize(a); authorize(b);
        String medicationId = medication(a, "Privado", 10, 2);
        String scheduleId = schedule(a, medicationId, "INDEFINITE", null);
        as(b).get(API + "/medications/" + medicationId).then().statusCode(404);
        as(b).body(Map.of("name", "Ataque")).patch(API + "/medications/" + medicationId).then().statusCode(404);
        as(b).get(API + "/schedules/" + scheduleId).then().statusCode(404);
    }
}
