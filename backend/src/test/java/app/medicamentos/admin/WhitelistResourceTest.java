package app.medicamentos.admin;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class WhitelistResourceTest extends ApiTestBase {
    @Test void adminManagesWhitelistAndUserCannot() {
        String email = "whitelist-" + System.nanoTime() + "@example.com";
        String id = as(ADMIN).body(Map.of("email", email)).post(API + "/admin/whitelist")
                .then().statusCode(201).body("email", equalTo(email)).extract().path("id");
        as(ADMIN).get(API + "/admin/whitelist").then().statusCode(200).body("email", hasItem(email));
        as(email).get(API + "/admin/whitelist").then().statusCode(403);
        as(ADMIN).delete(API + "/admin/whitelist/" + id).then().statusCode(204);
    }
}
