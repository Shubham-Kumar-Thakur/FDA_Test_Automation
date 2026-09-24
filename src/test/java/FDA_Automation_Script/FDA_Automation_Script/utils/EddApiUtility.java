package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

// Independent REST utility for TC_EDD_001, mirroring ReturnApiUtility's pattern (small, single-purpose,
// own raw config.get(...) calls) rather than extending the shared ApiUtility — keeps this independent
// EDD flow decoupled from ApiUtility's tc.ou009.*-scoped pushOffersToEmpathy(). Response JSON field
// names are unverified until a live run (no sample response was available at write time) — callers
// should log the full body and treat structured assertions as best-effort until confirmed.
public class EddApiUtility {

    private static final ConfigReader config = ConfigReader.getInstance();

    private EddApiUtility() {}

    /** POST tc.edd001.shop.business.days.url with Cookie-only auth, empty JSON body — expected to return the seller's business calendar config. */
    public static Response getShopBusinessDays() {
        String url = config.get("tc.edd001.shop.business.days.url");
        String cookie = config.get("tc.edd001.shop.business.days.cookie");

        LoggerUtility.info("========== Get Shop Business Days ==========");
        LoggerUtility.info("POST " + url);

        Response response = given()
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .body("{}")
                .when()
                .post(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Shop Business Days Status : " + response.getStatusCode());
        LoggerUtility.info("Get Shop Business Days Body   : " + response.getBody().asString());
        return response;
    }

    /** GET tc.edd001.empathy.url — no auth headers (matches the given curl exactly). */
    public static Response pushOffersToEmpathy() {
        String url = config.get("tc.edd001.empathy.url");

        LoggerUtility.info("========== Push Offers to Empathy (EDD) ==========");
        LoggerUtility.info("GET " + url);

        Response response = given()
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Push Offers to Empathy Status : " + response.getStatusCode());
        LoggerUtility.info("Push Offers to Empathy Body   : " + response.getBody().asString());
        return response;
    }
}
