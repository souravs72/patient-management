import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class PatientIntegrationTest {

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:4004/";
    }

    @Test
    public void shouldReturnPatientsWithValidToken() {
        String loginPayload = """
            {
                "email": "testuser@test.com",
                "password": "password123"
            }
            """;

        String token = given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .get("token");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                // content array must not be empty
                .body("content.size()", org.hamcrest.Matchers.greaterThan(0))
                // verify first patient has required fields
                .body("content[0].id", notNullValue())
                .body("content[0].name", notNullValue())
                .body("content[0].email", notNullValue());
    }
}
