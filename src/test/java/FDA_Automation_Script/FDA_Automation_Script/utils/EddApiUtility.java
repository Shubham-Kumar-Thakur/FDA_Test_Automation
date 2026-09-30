package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

// Independent REST utility for TC_EDD_001, mirroring ReturnApiUtility's pattern (small, single-purpose,
// own raw config.get(...) calls) rather than extending the shared ApiUtility — keeps this independent
// EDD flow decoupled from ApiUtility's tc.ou009.*-scoped pushOffersToEmpathy(). CONFIRMED live
// (2026-09-24): both endpoints return HTTP 200. getShopBusinessDays() body is
// {"message":"Calendars synced successfully","scope":"all"} — this is a SYNC TRIGGER, not a data
// query; it does NOT return working days/non-working days/holidays/cut-off time/business hours at
// all, contradicting the manual test case's expectation of reading that data back from this call.
// pushOffersToEmpathy()'s body shape varies with catalog state — empty catalog returns
// {"code":200,"status":"Success","offers":[]}; a populated one returns a full "offers" array. Per
// explicit instruction (2026-09-30), holiday data for EDD calculation is sourced from the seller's own
// Mirakl Business Calendar page (MiraklShopSettingsPage, Phase 1), not from this response — callers
// should assert only HTTP 200 against these two calls.
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

    /**
     * POST tc.edd001.buybox.url with the literal "{product-sku}" placeholder substituted for productSku,
     * Cookie-only auth, empty body — matches the given curl exactly (curl defaults to POST when --data
     * is present with no explicit -X). Expected to return the PDP's own estimatedDeliveryDate directly
     * for this product, as an independent cross-check against the FDA storefront's own PDP text.
     */
    public static Response getBuybox(String productSku) {
        String url = config.get("tc.edd001.buybox.url").replace("{product-sku}", productSku);
        String cookie = config.get("tc.edd001.buybox.cookie");

        LoggerUtility.info("========== Get Buybox (EDD) ==========");
        LoggerUtility.info("POST " + url);

        Response response = given()
                .header("Cookie", cookie)
                .body("")
                .when()
                .post(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Buybox Status : " + response.getStatusCode());
        LoggerUtility.info("Get Buybox Body   : " + response.getBody().asString());
        return response;
    }
}
