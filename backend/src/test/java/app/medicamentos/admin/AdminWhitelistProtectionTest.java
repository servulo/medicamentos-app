package app.medicamentos.admin;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AdminWhitelistProtectionTest extends ApiTestBase {
    @Test void adminIsAlwaysAuthorizedAndCannotBeRemoved() {
        as(ADMIN).get(API + "/me").then().statusCode(200).body("admin", equalTo(true));
        String id = as(ADMIN).get(API + "/admin/whitelist").then().statusCode(200)
                .extract().jsonPath().getString("find { it.email == 'admin@example.com' }.id");
        as(ADMIN).delete(API + "/admin/whitelist/" + id).then().statusCode(403);
    }
}
