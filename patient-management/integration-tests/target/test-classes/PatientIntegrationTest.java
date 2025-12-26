import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

//focus on calling api-gateway routes because you can't access patient-service directly
public class PatientIntegrationTest {

    @BeforeAll
    static void setUp(){
        RestAssured.baseURI = "http://localhost:4004";
    }

    //Reject any invalid token requests
    @Test
    void shouldRejectInvalidToken() {
        given()
                .header("Authorization", "Bearer INVALID_TOKEN")
                .when()
                .get("/api/patients")
                .then()
                .statusCode(401);  // Gateway should block here
    }

    //helper login function to generate valid token
    private String loginAndGetToken() {
        return given()
                .contentType("application/json")
                .body("""
            {
                "email": "testuser@test.com",
                "password": "password123"
            }
            """)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    //get patient lists
    @Test
    public void shouldReturnPatientsWithValidToken(){

        String token = loginAndGetToken();
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/patients")
                .then()
                .statusCode(200)
                .body("patients", notNullValue());
    }

    @Test
    void shouldReturnCreatedPatientOnValidToken() {
        String token = loginAndGetToken();

        Response response =
                given()
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .body("""
                        {
                            "name": "Jack Doe",
                            "email": "jackdoe7.test@ex.com",
                            "address": "Delhi",
                            "dateOfBirth": "1990-05-10",
                            "registeredDate": "2024-01-01"
                        }
                        """)
                        .when()
                        .post("/api/patients")
                        .then()
                        .statusCode(200)
                        .body("id", notNullValue())
                        .body("name", equalTo("Jack Doe"))
                        .extract()
                        .response();
        System.out.println(response.getBody().asString());
    }

    @Test
    void shouldReturnErrorOnCreatingInvalidPatient() {
        String token = loginAndGetToken();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                {
                    "name": "",
                    "email": "",
                    "address": "Delhi",
                    "dateOfBirth": "2000-05-01"
                }
                """)
                .when()
                .post("/api/patients")
                .then()
                .statusCode(400); // Because @Valid + validation group
    }

    //UPDATE A PATIENT
    @Test
    void shouldReturnUpdatedPatientOnValidToken() {
        String token = loginAndGetToken();

        // First create a patient
        String patientId =
                given()
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .body("""
                        {
                            "name": "Updated Name",
                            "email": "update.test@ex.com",
                            "address": "Delhi Updated",
                            "dateOfBirth": "1995-03-10",
                            "registeredDate": "2024-01-01"
                        }
                        
                        """)
                        .when()
                        .post("/api/patients")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getString("id");

        // Now update the patient
        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                {
                    "name": "Old Name",
                    "email": "update.test@ex.com",
                    "address": "Delhi",
                    "dateOfBirth": "1995-03-10",
                    "registeredDate": "2024-01-01"
                }
                """)
                .when()
                .put("/api/patients/" + patientId)
                .then()
                .statusCode(200)
                .body("name", equalTo("Updated Name"));
    }

    @Test
    void shouldDeletePatientOnValidToken() {
        String token = loginAndGetToken();

        // Create first
        String patientId =
                given()
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .body("""
                        {
                            "name": "Delete Me",
                            "email": "delete.me@ex.com",
                            "address": "Agra",
                            "dateOfBirth": "1998-10-15",
                            "registeredDate": "2024-01-01"
                        }
                        """)
                        .when()
                        .post("/api/patients")
                        .then()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getString("id");

        // Delete
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/patients/" + patientId)
                .then()
                .statusCode(204);
    }



}
