import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class AuthIntegrationTest {
    @BeforeAll
    static void setUp(){
        //integration tests will run outside our docker container hence localhost otherwise we would have used api-gateway
        RestAssured.baseURI = "http://localhost:4004"; //api=gateway url
    }

    @Test
    public void shouldReturnOKWithValidToken(){  //naming convention for function - should <Expected outcome> <with this data>
        //1. Arrange
        //2. Act
        //3. Assert
        String loginPayload = """
        {
            "email": "testuser@test.com",
            "password": "password123"
        }
        """;

        Response response = given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue())
                .extract()
                .response();
        System.out.println("Generated Token: "+ response.jsonPath().getString("token"));

    }

    @Test
    public void shouldReturnUnauthorizedOnInvalidLogin(){  //naming convention for function - should <Expected outcome> <with this data>
        //1. Arrange
        //2. Act
        //3. Assert
        String loginPayload = """
        {
            "email": "invalid_user@test.com",
            "password": "user123"
        }
        """;

        given()
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401);
        System.out.println("Receive unauthorized token");

    }
}
