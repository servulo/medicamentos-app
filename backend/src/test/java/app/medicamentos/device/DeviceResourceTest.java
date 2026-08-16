package app.medicamentos.device;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class DeviceResourceTest extends ApiTestBase {
    @Test void registersListsAndDeletesDeviceWithClientMobileFlag() {
        String email = "device@example.com";
        authorize(email);
        String endpoint = "https://push.example/" + System.nanoTime();
        String id = as(email).body(Map.of("endpoint", endpoint, "p256dh", "key", "auth", "secret", "isMobile", true))
                .post(API + "/devices").then().statusCode(201).body("isMobile", equalTo(true)).extract().path("id");
        as(email).get(API + "/devices").then().statusCode(200).body("id", hasItem(id));
        as(email).delete(API + "/devices/" + id).then().statusCode(204);
    }
}
