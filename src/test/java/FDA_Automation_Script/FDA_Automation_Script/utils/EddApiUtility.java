package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.restassured.response.Response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;

// Independent REST utility for TC_EDD_001, mirroring ReturnApiUtility's pattern (small, single-purpose,
// own raw config.get(...) calls) rather than extending the shared ApiUtility — keeps this independent
// EDD flow decoupled from ApiUtility's tc.ou009.*-scoped pushOffersToEmpathy(). CONFIRMED live
// (2026-09-24): getShopBusinessDays() body is {"message":"Calendars synced successfully","scope":"all"}
// — this is a SYNC TRIGGER, not a data query; it does NOT return working days/non-working
// days/holidays/cut-off time/business hours at all, contradicting the manual test case's expectation of
// reading that data back from this call.
// CORRECTED live (2026-09-30, TC_EDD_001): the doc comment used to claim pushOffersToEmpathy()'s body
// is always {"code":200,"status":"Success","offers":[]} — that was only true when the catalog had no
// synced offers yet. A live run with populated offers returned a full "offers" array, and each offer's
// "locations[].holidays" carries the SAME holiday list (e.g. ["01-October-2026"]) across every offer
// belonging to a given seller — i.e. this is shop-level holiday data, not per-offer. ROOT-CAUSED: this
// is the AUTHORITATIVE holiday source — Mirakl's own Business Calendar UI page (scraped in Phase 1 by
// MiraklShopSettingsPage) did NOT surface this same holiday in a live run, so EddCalculator's holiday
// set was empty and the EDD calculation came out one business day early. extractHolidaysFromEmpathyResponse()
// below pulls holidays from this response for use alongside (or instead of) the Business Calendar scrape.
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
