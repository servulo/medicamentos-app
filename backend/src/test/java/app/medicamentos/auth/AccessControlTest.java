package app.medicamentos.auth;

import app.medicamentos.support.ApiTestBase;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AccessControlTest extends ApiTestBase {
    @Test void requiresIdentityAndWhitelist() {
        given().get(API + "/me").then().statusCode(401);
        as("blocked@example.com").get(API + "/me").then().statusCode(403);
        authorize("allowed@example.com");
        as("allowed@example.com").get(API + "/me").then().statusCode(200)
                .body("email", equalTo("allowed@example.com"))
                .body("admin", equalTo(false));
    }
}
