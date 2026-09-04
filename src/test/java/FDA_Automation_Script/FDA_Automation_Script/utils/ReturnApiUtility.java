package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class ReturnApiUtility {

    private static final ConfigReader config = ConfigReader.getInstance();

    /**
     * GET Mirakl /api/orders?order_ids={commercialId}
     * Returns the first order_line_id found in the response.
     * Throws RuntimeException on failure.
     */
    public static String getMiraklOrderLineId(String commercialId) {
        String miraklBase = config.getMiraklUrl();
        if (miraklBase.endsWith("/")) {
            miraklBase = miraklBase.substring(0, miraklBase.length() - 1);
        }
        String url   = miraklBase + "/api/orders";
        String token = config.get("mirakl.api.token");

        LoggerUtility.info("========== Get Mirakl Order Line ID ==========");
        // Mirakl sub-order IDs are suffixed with "-A", "-B", etc.
        // The commercialId (e.g. "4000276298WEB") is not a direct Mirakl order_id;
        // append "-A" to target the first sub-order.
        String miraklOrderId = commercialId + "-A";
        LoggerUtility.info("GET " + url + "?order_ids=" + miraklOrderId);

        Response response = given()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .queryParam("order_ids", miraklOrderId)
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Order Line ID Status : " + response.getStatusCode());
        LoggerUtility.info("Get Order Line ID Body   : " + response.getBody().asString());

        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Get Mirakl Order Line ID failed — HTTP " + response.getStatusCode()
                    + " | Body: " + response.getBody().asString());
        }

        // Response: { "orders": [{ "order_lines": [{ "order_line_id": "...", ... }] }] }
        String orderLineId = response.jsonPath().getString("orders[0].order_lines[0].order_line_id");
        if (orderLineId == null || orderLineId.isEmpty()) {
            throw new RuntimeException(
                    "order_line_id not found in Mirakl API response for: " + commercialId
                    + " | Body: " + response.getBody().asString());
        }

        LoggerUtility.info("Order Line ID: " + orderLineId);
        return orderLineId;
    }

    /**
     * POST {return.service.url} to create a return with the specified quantity.
     * Uses Cookie auth (no Bearer token).
     * Returns the raw Response; caller asserts status code.
     */
    public static Response postReturn(String orderCommercialId, String orderLineId, int quantity) {
        String url    = config.get("return.service.url");
        String cookie = config.get("return.service.cookie");

        LoggerUtility.info("========== Create Return ==========");
        LoggerUtility.info("POST " + url);
        LoggerUtility.info("order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("order_line_id       : " + orderLineId);
        LoggerUtility.info("quantity            : " + quantity);

        Map<String, Object> returnLine = new LinkedHashMap<>();
        returnLine.put("order_line_id", orderLineId);
        returnLine.put("quantity", quantity);

        Map<String, Object> returnItem = new LinkedHashMap<>();
        returnItem.put("accepted", true);
        returnItem.put("method_code", "RETURN_METHOD_DROP_OFF_POINT");
        returnItem.put("order_commercial_id", orderCommercialId);
        returnItem.put("reason_code", "RETURN_CHANGED_MIND");
        returnItem.put("return_lines", List.of(returnLine));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("returns", List.of(returnItem));

        LoggerUtility.info("API Request Body : " + body);

        Response response = given()
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Return Service Status : " + response.getStatusCode());
        LoggerUtility.info("Return Service Body   : " + response.getBody().asString());
        return response;
    }

    /**
     * POST {return.service.url} to create a return.
     * Uses Cookie auth (no Bearer token).
     * Returns the raw Response; caller asserts status code.
     */
    public static Response postReturn(String orderCommercialId, String orderLineId) {
        String url    = config.get("return.service.url");
        String cookie = config.get("return.service.cookie");

        LoggerUtility.info("========== Create Return ==========");
        LoggerUtility.info("POST " + url);
        LoggerUtility.info("order_commercial_id : " + orderCommercialId);
        LoggerUtility.info("order_line_id       : " + orderLineId);

        Map<String, Object> returnLine = new LinkedHashMap<>();
        returnLine.put("order_line_id", orderLineId);
        returnLine.put("quantity", 1);

        Map<String, Object> returnItem = new LinkedHashMap<>();
        returnItem.put("accepted", true);
        returnItem.put("method_code", "RETURN_METHOD_DROP_OFF_POINT");
        returnItem.put("order_commercial_id", orderCommercialId);
        returnItem.put("reason_code", "RETURN_CHANGED_MIND");
        returnItem.put("return_lines", List.of(returnLine));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("returns", List.of(returnItem));

        LoggerUtility.info("API Request Body : " + body);

        Response response = given()
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Return Service Status : " + response.getStatusCode());
        LoggerUtility.info("Return Service Body   : " + response.getBody().asString());
        return response;
    }
}
