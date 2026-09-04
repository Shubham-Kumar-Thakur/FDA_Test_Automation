package FDA_Automation_Script.FDA_Automation_Script.utils;

import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class ApiUtility {

    private static final ConfigReader config = ConfigReader.getInstance();

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    /** POST with Envioclick Bearer token + cookie (existing behaviour). */
    private static Response post(String url, Map<String, Object> body) {
        return postWithAuth(url, config.getApiToken(), body);
    }

    /** POST with an explicitly supplied Bearer token + cookie. */
    private static Response postWithAuth(String url, String bearerToken, Map<String, Object> body) {
        String cookie = config.getApiCookie();
        LoggerUtility.info("API Request URL  : " + url);
        LoggerUtility.info("API Request Body : " + body);
        Response response = given()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .header("Cookie", cookie)
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();
        LoggerUtility.info("API Response Status : " + response.getStatusCode());
        LoggerUtility.info("API Response Body   : " + response.getBody().asString());
        return response;
    }

    // ----------------------------------------------------------------
    // Envioclick
    // ----------------------------------------------------------------

    public static Response postEnvioclickEnTransito(String tplShipmentId, String trackingCode,
                                                    String orderId, String carrierName) {
        LoggerUtility.info("========== Shipment Update ==========");
        LoggerUtility.info("Calling Shipment API");
        LoggerUtility.info("Calling Envioclick API — Status: En tránsito");
        return post(config.getEnvioclickUrl(),
                buildEnvioclickBody(tplShipmentId, trackingCode, orderId, carrierName, "En tránsito"));
    }

    public static Response postEnvioclickEntregado(String tplShipmentId, String trackingCode,
                                                   String orderId, String carrierName) {
        LoggerUtility.info("========== Shipment Delivered ==========");
        LoggerUtility.info("Calling Delivered API");
        LoggerUtility.info("Calling Envioclick API — Status: Entregado");
        return post(config.getEnvioclickUrl(),
                buildEnvioclickBody(tplShipmentId, trackingCode, orderId, carrierName, "Entregado"));
    }

    private static Map<String, Object> buildEnvioclickBody(String tplShipmentId, String trackingCode,
                                                           String orderId, String carrierName,
                                                           String status) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", "2022-01-17 13:21:01");
        event.put("status", status);
        event.put("statusDetail", "Envio Documentado");
        event.put("statusStep", "Pendiente de Recolección");
        event.put("incidence", false);
        event.put("incidenceType", null);
        event.put("description", "Envío documentado");
        event.put("receivedBy", null);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idCarrier", 6);
        body.put("carrier", carrierName.toUpperCase());
        body.put("idOrder", tplShipmentId);
        body.put("trackingCode", trackingCode);
        body.put("realPickupDate", "2022-01-17 16:34:13");
        body.put("arrivalDate", "2022-01-19 16:43:41");
        body.put("realDeliveryDate", "2022-01-18 12:23:48");
        body.put("myShipmentReference", orderId + "WEB");
        body.put("events", List.of(event));
        return body;
    }

    // ----------------------------------------------------------------
    // Skydropx — uses its own dedicated Bearer token
    // ----------------------------------------------------------------

    public static Response postSkydropxPickedUp(String tplShipmentId) {
        LoggerUtility.info("========== Shipment Update ==========");
        LoggerUtility.info("Calling Shipment API");
        LoggerUtility.info("Calling Skydropx API — Status: Picked_up");
        return postWithAuth(config.getSkydropxUrl(), config.getSkydropxApiToken(),
                buildSkydropxBody(tplShipmentId, "Picked_up"));
    }

    public static Response postSkydropxDelivered(String tplShipmentId) {
        LoggerUtility.info("========== Shipment Delivered ==========");
        LoggerUtility.info("Calling Delivered API");
        LoggerUtility.info("Calling Skydropx API — Status: Delivered");
        return postWithAuth(config.getSkydropxUrl(), config.getSkydropxApiToken(),
                buildSkydropxBody(tplShipmentId, "Delivered"));
    }

    private static Map<String, Object> buildSkydropxBody(String tplShipmentId, String status) {
        Map<String, Object> shipmentData = new LinkedHashMap<>();
        shipmentData.put("id", tplShipmentId);
        shipmentData.put("type", "shipments");

        Map<String, Object> links = new LinkedHashMap<>();
        links.put("related", "https://sb-pro.skydropx.com/api/v1/shipments/" + tplShipmentId);

        Map<String, Object> shipmentRelationship = new LinkedHashMap<>();
        shipmentRelationship.put("data", shipmentData);
        shipmentRelationship.put("links", links);

        Map<String, Object> relationships = new LinkedHashMap<>();
        relationships.put("shipment", shipmentRelationship);

        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("status", status);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", tplShipmentId);
        data.put("type", "packages");
        data.put("attributes", attributes);
        data.put("relationships", relationships);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        return body;
    }

    // ----------------------------------------------------------------
    // Cancel Full Order (TC_FBO_027)
    // ----------------------------------------------------------------

    /**
     * POST {cancel.order.url} with Cookie auth.
     * Body: { "orderId": "<commercialId>" }   e.g. "4000278812WEB"
     * Returns the raw Response; caller asserts status code.
     */
    public static Response cancelFullOrder(String commercialId) {
        String url    = config.getCancelOrderUrl();
        String cookie = config.getCancelOrderCookie();

        LoggerUtility.info("========== Cancel Full Order ==========");
        LoggerUtility.info("POST " + url);
        LoggerUtility.info("orderId : " + commercialId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", commercialId);

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

        LoggerUtility.info("Cancel Order Status : " + response.getStatusCode());
        LoggerUtility.info("Cancel Order Body   : " + response.getBody().asString());
        return response;
    }

    // ----------------------------------------------------------------
    // Cancel Shipment (TC_FBO_028)
    // ----------------------------------------------------------------

    /**
     * POST {cancel.shipment.url} with Cookie auth.
     * Body: { "shipmentId": "<shipmentId>", "productIds": [...], "orderId": "<commercialOrderId>" }
     * Example: shipmentId="4000278821WEB-A", productIds=["7080901020305"], orderId="4000278821WEB"
     * Returns the raw Response; caller asserts status code.
     */
    public static Response cancelShipment(String shipmentId, List<String> productIds,
                                          String commercialOrderId) {
        String url    = config.getCancelShipmentUrl();
        String cookie = config.getCancelShipmentCookie();

        LoggerUtility.info("========== Cancel Shipment ==========");
        LoggerUtility.info("POST " + url);
        LoggerUtility.info("shipmentId : " + shipmentId);
        LoggerUtility.info("productIds : " + productIds);
        LoggerUtility.info("orderId    : " + commercialOrderId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shipmentId", shipmentId);
        body.put("productIds", productIds);
        body.put("orderId", commercialOrderId);

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

        LoggerUtility.info("Cancel Shipment Status : " + response.getStatusCode());
        LoggerUtility.info("Cancel Shipment Body   : " + response.getBody().asString());
        return response;
    }

    // ----------------------------------------------------------------
    // Kibo Commerce API
    // ----------------------------------------------------------------

    /**
     * POST to Kibo OAuth endpoint with client_credentials grant.
     * Returns the access_token string.
     * Throws RuntimeException on failure so the test fails immediately.
     */
    public static String kiboAuthenticate() {
        String url = config.getKiboApiBaseUrl()
                + "/api/platform/applications/authtickets/oauth";
        LoggerUtility.info("========== Kibo Authentication ==========");
        LoggerUtility.info("Calling Kibo Auth API: " + url);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("client_id", config.getKiboApiClientId());
        body.put("client_secret", config.getKiboApiClientSecret());

        Response response = given()
                .header("Content-Type", "application/json")
                .body(body)
                .when()
                .post(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Kibo Auth Response Status : " + response.getStatusCode());
        LoggerUtility.info("Kibo Auth Response Body   : " + response.getBody().asString());

        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Kibo Authentication failed — HTTP " + response.getStatusCode()
                    + " | Body: " + response.getBody().asString());
        }

        String token = response.jsonPath().getString("access_token");
        if (token == null || token.isEmpty()) {
            throw new RuntimeException(
                    "Kibo Authentication succeeded (HTTP 200) but access_token is absent in response");
        }

        LoggerUtility.info("Authentication Successful");
        LoggerUtility.info("Access Token Generated");
        return token;
    }

    /**
     * GET /api/commerce/orders/ with pageSize=500, match by externalId.
     * Returns the Kibo internal order id.
     * Throws RuntimeException if no match is found.
     */
    public static String kiboFindOrderId(String accessToken, String externalId) {
        String url = config.getKiboApiBaseUrl() + "/api/commerce/orders/";
        LoggerUtility.info("========== Get Orders ==========");
        LoggerUtility.info("Calling Kibo Get All Orders API: " + url);
        LoggerUtility.info("Searching externalId: " + externalId);

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .queryParam("pageSize", 500)
                .queryParam("pageCount", 1)
                .queryParam("responseFields", "items(id,status,externalId)")
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get All Orders Response Status : " + response.getStatusCode());

        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Kibo Get All Orders failed — HTTP " + response.getStatusCode()
                    + " | Body: " + response.getBody().asString());
        }

        List<Map<String, Object>> items = response.jsonPath().getList("items");
        if (items == null || items.isEmpty()) {
            throw new RuntimeException(
                    "Kibo Get All Orders returned no items — cannot search for externalId: " + externalId);
        }

        LoggerUtility.info("Total orders returned: " + items.size());
        for (Map<String, Object> item : items) {
            Object ext = item.get("externalId");
            if (ext != null && externalId.equals(ext.toString())) {
                String kiboOrderId = String.valueOf(item.get("id"));
                LoggerUtility.info("Matching Order Found");
                LoggerUtility.info("Kibo Order ID : " + kiboOrderId);
                return kiboOrderId;
            }
        }

        throw new RuntimeException(
                "No Kibo order found with externalId: " + externalId
                + " — searched " + items.size() + " orders");
    }

    /**
     * GET /api/commerce/orders/{id}/shipments — returns the shipments list for an order.
     * Does NOT throw on non-200; returns the raw response so callers can inspect.
     */
    public static Response kiboGetOrderShipments(String accessToken, String kiboOrderId) {
        String url = config.getKiboApiBaseUrl() + "/api/commerce/orders/" + kiboOrderId + "/shipments";
        LoggerUtility.info("========== Get Order Shipments ==========");
        LoggerUtility.info("Calling Kibo Get Order Shipments API: " + url);

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Order Shipments Status : " + response.getStatusCode());
        LoggerUtility.info("Get Order Shipments Body   : " + response.getBody().asString());
        return response;
    }

    /**
     * GET /api/commerce/shipments?pageSize=200&filter=orderId=={kiboOrderId}
     * Returns all fulfillment shipments for a given Kibo order ID (same endpoint as
     * Get_Shipment_Details in the FBO FULLFILMENT API Postman collection).
     * Use this to retrieve deliveryPartner / 3pl_shipmentId from shipment items.
     */
    public static Response kiboGetShipmentsByOrderId(String accessToken, String kiboOrderId) {
        String url = config.getKiboApiBaseUrl() + "/api/commerce/shipments";
        LoggerUtility.info("========== Get Shipments by Order ID ==========");
        LoggerUtility.info("Calling Kibo Get Shipments API: " + url + " filter=orderId==" + kiboOrderId);

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .queryParam("pageSize", 200)
                .queryParam("filter", "orderId==" + kiboOrderId)
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Shipments by OrderId Status : " + response.getStatusCode());
        LoggerUtility.info("Get Shipments by OrderId Body   : " + response.getBody().asString());
        return response;
    }

    /**
     * PUT /api/commerce/shipments/{number}/tasks/Validate%20Items%20In%20Stock/skipped
     * Skips the inventory-validation workflow gate so the Kibo API Connector can proceed
     * to generate the carrier label. Required when staging SKUs have no warehouse stock.
     */
    public static Response kiboSkipValidateItemsTask(String accessToken, String shipmentNumber) {
        String url = config.getKiboApiBaseUrl()
                + "/api/commerce/shipments/" + shipmentNumber
                + "/tasks/Validate%20Items%20In%20Stock/skipped";
        LoggerUtility.info("========== Skip Validate Items In Stock Task ==========");
        LoggerUtility.info("PUT " + url);
        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .body("{}")
                .when()
                .put(url)
                .then()
                .extract()
                .response();
        LoggerUtility.info("Skip Task Status : " + response.getStatusCode());
        LoggerUtility.info("Skip Task Body   : " + response.getBody().asString());
        return response;
    }

    /**
     * GET /api/commerce/shipments/{shipmentNumber} — returns a single fulfillment shipment.
     * Does NOT throw on non-200; returns the raw response so callers can inspect.
     */
    public static Response kiboGetShipment(String accessToken, String shipmentNumber) {
        String url = config.getKiboApiBaseUrl() + "/api/commerce/shipments/" + shipmentNumber;
        LoggerUtility.info("========== Get Shipment ==========");
        LoggerUtility.info("Calling Kibo Get Shipment API: " + url);

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Shipment Response Status : " + response.getStatusCode());
        LoggerUtility.info("Get Shipment Response Body   : " + response.getBody().asString());
        return response;
    }

    /**
     * GET /api/commerce/orders/{id} — returns the full order Response.
     * Throws RuntimeException on non-200.
     */
    public static Response kiboGetOrder(String accessToken, String kiboOrderId) {
        String url = config.getKiboApiBaseUrl() + "/api/commerce/orders/" + kiboOrderId;
        LoggerUtility.info("========== Get Order ==========");
        LoggerUtility.info("Calling Kibo Get Order API: " + url);

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Order Response Status : " + response.getStatusCode());
        LoggerUtility.info("Get Order Response Body   : " + response.getBody().asString());

        if (response.getStatusCode() != 200) {
            throw new RuntimeException(
                    "Kibo Get Order failed — HTTP " + response.getStatusCode()
                    + " | Body: " + response.getBody().asString());
        }

        return response;
    }

    /**
     * GET /api/commerce/returns?filter=originalOrderId eq {kiboOrderId}
     * Returns all return records for a Kibo order.
     * Used by TC_FBO_026 to verify partial return status = "closed".
     * Does NOT throw on non-200; returns the raw response so callers can inspect.
     */
    public static Response kiboGetReturnsForOrder(String accessToken, String kiboOrderId) {
        String url = config.getKiboApiBaseUrl() + "/api/commerce/returns";
        LoggerUtility.info("========== Get Returns for Order ==========");
        LoggerUtility.info("GET " + url + " | filter=originalOrderId eq " + kiboOrderId);

        Response response = given()
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .queryParam("filter", "originalOrderId eq " + kiboOrderId)
                .when()
                .get(url)
                .then()
                .extract()
                .response();

        LoggerUtility.info("Get Returns Status : " + response.getStatusCode());
        LoggerUtility.info("Get Returns Body   : " + response.getBody().asString());
        return response;
    }

    /**
     * Extract a named field from a Kibo order response.
     * Search order:
     *   1. Order-level "data" map  (e.g. data.deliveryPartner, data.3pl_shipmentId)
     *   2. Each package's "data" map  (packages[*].data.fieldKey)
     *   3. extendedProperties key-value array
     * Keys like "3pl_shipmentId" (numeric prefix) are handled via Map, not JsonPath dot-notation.
     */
    public static String extractKiboCustomField(Response kiboOrderResponse, String fieldKey) {

        // 1. Order-level data map
        try {
            Map<String, Object> dataMap = kiboOrderResponse.jsonPath().getMap("data");
            if (dataMap != null && dataMap.containsKey(fieldKey)) {
                Object val = dataMap.get(fieldKey);
                if (val != null && !val.toString().isBlank()) {
                    return val.toString().trim();
                }
            }
        } catch (Exception e) {
            LoggerUtility.warn("Could not read order 'data' map for '" + fieldKey + "': " + e.getMessage());
        }

        // 2. Package-level data maps  (packages[*].data.fieldKey)
        try {
            List<Map<String, Object>> packages = kiboOrderResponse.jsonPath().getList("packages");
            if (packages != null) {
                for (Map<String, Object> pkg : packages) {
                    Object raw = pkg.get("data");
                    if (raw instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> pkgData = (Map<String, Object>) raw;
                        if (pkgData.containsKey(fieldKey)) {
                            Object val = pkgData.get(fieldKey);
                            if (val != null && !val.toString().isBlank()) {
                                return val.toString().trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtility.warn("Could not read packages data for '" + fieldKey + "': " + e.getMessage());
        }

        // 3. extendedProperties array  [{key, value}, ...]
        try {
            List<Map<String, Object>> props = kiboOrderResponse.jsonPath().getList("extendedProperties");
            if (props != null) {
                for (Map<String, Object> prop : props) {
                    if (fieldKey.equals(prop.get("key"))) {
                        Object val = prop.get("value");
                        if (val != null && !val.toString().isBlank()) {
                            return val.toString().trim();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerUtility.warn("Could not read 'extendedProperties' for '" + fieldKey + "': " + e.getMessage());
        }

        LoggerUtility.warn("Kibo field not found anywhere in order response: " + fieldKey);
        return "";
    }
}
