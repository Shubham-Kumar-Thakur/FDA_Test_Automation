package FDA_Automation_Script.FDA_Automation_Script.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

	private static final ConfigReader INSTANCE = new ConfigReader();
	private final Properties properties = new Properties();

	private ConfigReader() {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
			if (in == null)
				throw new RuntimeException("config.properties not found in classpath");
			properties.load(in);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load config.properties", e);
		}
	}

	public static ConfigReader getInstance() {
		return INSTANCE;
	}

	public String get(String key) {
		String value = properties.getProperty(key);
		if (value == null)
			throw new RuntimeException("Property not found: " + key);
		return value.trim();
	}

	public String getFdaUrl() {
		return get("fda.url");
	}

	public String getFdaUsername() {
		return get("fda.username");
	}

	public String getFdaPassword() {
		return get("fda.password");
	}

	public String getFdaSku() {
		return get("fda.sku");
	}

	public String getFdaCardNumber() {
		return get("fda.card.number");
	}

	public String getFdaCardExpiry() {
		return get("fda.card.expiry");
	}

	public String getFdaCardCvv() {
		return get("fda.card.cvv");
	}

	public String getKiboUrl() {
		return get("kibo.url");
	}

	public String getKiboUsername() {
		return get("kibo.username");
	}

	public String getKiboPassword() {
		return get("kibo.password");
	}

	public String getMiraklUrl() {
		return get("mirakl.url");
	}

	public String getMiraklUsername() {
		return get("mirakl.username");
	}

	public String getMiraklPassword() {
		return get("mirakl.password");
	}

	public String getEnvioclickUrl() {
		return get("envioclick.url");
	}

	public String getSkydropxUrl() {
		return get("skydropx.url");
	}

	public String getApiToken() {
		return get("api.auth.token");
	}

	public String getSkydropxApiToken() {
		return get("skydropx.api.token");
	}

	public String getApiCookie() {
		return get("api.cookie");
	}

	public String getKiboApiBaseUrl() {
		return get("kibo.api.base.url");
	}

	public String getKiboApiClientId() {
		return get("kibo.api.client.id");
	}

	public String getKiboApiClientSecret() {
		return get("kibo.api.client.secret");
	}

	public String getCancelOrderUrl() {
		return get("cancel.order.url");
	}

	public String getCancelOrderCookie() {
		return get("cancel.order.cookie");
	}

	public String getCancelShipmentUrl() {
		return get("cancel.shipment.url");
	}

	public String getCancelShipmentCookie() {
		return get("cancel.shipment.cookie");
	}
}
