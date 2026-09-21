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

	public String getFboUsername() {
		return get("fbo.username");
	}

	public String getFboPassword() {
		return get("fbo.password");
	}

	public String getFbsUsername() {
		return get("fbs.username");
	}

	public String getFbsPassword() {
		return get("fbs.password");
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

	public String getFbs001InvoiceFilePath() {
		return get("fbs001.invoice.file.path");
	}

	public String getFbs001Sku() {
		return get("fbs001.sku");
	}

	public String getFbs001CardNumber() {
		return get("fbs001.card.number");
	}

	public String getFbs001CardExpiry() {
		return get("fbs001.card.expiry");
	}

	public String getFbs001CardCvv() {
		return get("fbs001.card.cvv");
	}

	public String getFbs002Sku1() {
		return get("fbs002.sku1");
	}

	public String getFbs002Sku2() {
		return get("fbs002.sku2");
	}

	public String getFbs002CardNumber() {
		return get("fbs002.card.number");
	}

	public String getFbs002CardExpiry() {
		return get("fbs002.card.expiry");
	}

	public String getFbs002CardCvv() {
		return get("fbs002.card.cvv");
	}

	public String getFbs002InvoiceFilePath() {
		return get("fbs002.invoice.file.path");
	}

	public String getFbs003Sku() {
		return get("fbs003.sku");
	}

	public String getFbs003CardNumber() {
		return get("fbs003.card.number");
	}

	public String getFbs003CardExpiry() {
		return get("fbs003.card.expiry");
	}

	public String getFbs003CardCvv() {
		return get("fbs003.card.cvv");
	}

	public String getFbs003InvoiceFilePath() {
		return get("fbs003.invoice.file.path");
	}

	public String getFbs004Sku1() {
		return get("fbs004.sku1");
	}

	public String getFbs004Sku2() {
		return get("fbs004.sku2");
	}

	public String getFbs004CardNumber() {
		return get("fbs004.card.number");
	}

	public String getFbs004CardExpiry() {
		return get("fbs004.card.expiry");
	}

	public String getFbs004CardCvv() {
		return get("fbs004.card.cvv");
	}

	public String getFbs004InvoiceFilePath() {
		return get("fbs004.invoice.file.path");
	}

	public String getFbs005Sku1() {
		return get("fbs005.sku1");
	}

	public String getFbs005Sku2() {
		return get("fbs005.sku2");
	}

	public String getFbs005CardNumber() {
		return get("fbs005.card.number");
	}

	public String getFbs005CardExpiry() {
		return get("fbs005.card.expiry");
	}

	public String getFbs005CardCvv() {
		return get("fbs005.card.cvv");
	}

	public String getFbs005InvoiceFilePath() {
		return get("fbs005.invoice.file.path");
	}

	public String getFbs006Sku1() {
		return get("fbs006.sku1");
	}

	public String getFbs006Sku2() {
		return get("fbs006.sku2");
	}

	public String getFbs006CardNumber() {
		return get("fbs006.card.number");
	}

	public String getFbs006CardExpiry() {
		return get("fbs006.card.expiry");
	}

	public String getFbs006CardCvv() {
		return get("fbs006.card.cvv");
	}

	public String getFbs006InvoiceFilePath() {
		return get("fbs006.invoice.file.path");
	}

	public String getFbs007Sku() {
		return get("fbs007.sku");
	}

	public String getFbs007InvoiceFilePath() {
		return get("fbs007.invoice.file.path");
	}

	public String getFbs008Sku1() {
		return get("fbs008.sku1");
	}

	public String getFbs008Sku2() {
		return get("fbs008.sku2");
	}

	public String getFbs008InvoiceFilePath() {
		return get("fbs008.invoice.file.path");
	}

	public String getFbs009Sku() {
		return get("fbs009.sku");
	}

	public String getFbs009InvoiceFilePath() {
		return get("fbs009.invoice.file.path");
	}

	public String getFbs010Sku1() {
		return get("fbs010.sku1");
	}

	public String getFbs010Sku2() {
		return get("fbs010.sku2");
	}

	public String getFbs010InvoiceFilePath() {
		return get("fbs010.invoice.file.path");
	}

	public String getFbs011Sku1() {
		return get("fbs011.sku1");
	}

	public String getFbs011Sku2() {
		return get("fbs011.sku2");
	}

	public String getFbs011InvoiceFilePath() {
		return get("fbs011.invoice.file.path");
	}

	public String getFbs012Sku1() {
		return get("fbs012.sku1");
	}

	public String getFbs012Sku2() {
		return get("fbs012.sku2");
	}

	public String getFbs012InvoiceFilePath() {
		return get("fbs012.invoice.file.path");
	}

	public String getFbs013Sku() {
		return get("fbs013.sku");
	}

	public String getFbs013CardNumber() {
		return get("fbs013.card.number");
	}

	public String getFbs013CardExpiry() {
		return get("fbs013.card.expiry");
	}

	public String getFbs013CardCvv() {
		return get("fbs013.card.cvv");
	}

	public String getFbs013InvoiceFilePath() {
		return get("fbs013.invoice.file.path");
	}

	public String getFbs014Sku1() {
		return get("fbs014.sku1");
	}

	public String getFbs014Sku2() {
		return get("fbs014.sku2");
	}

	public String getFbs014CardNumber() {
		return get("fbs014.card.number");
	}

	public String getFbs014CardExpiry() {
		return get("fbs014.card.expiry");
	}

	public String getFbs014CardCvv() {
		return get("fbs014.card.cvv");
	}

	public String getFbs014InvoiceFilePath() {
		return get("fbs014.invoice.file.path");
	}

	public String getFbs015Sku() {
		return get("fbs015.sku");
	}

	public String getFbs015CardNumber() {
		return get("fbs015.card.number");
	}

	public String getFbs015CardExpiry() {
		return get("fbs015.card.expiry");
	}

	public String getFbs015CardCvv() {
		return get("fbs015.card.cvv");
	}

	public String getFbs015InvoiceFilePath() {
		return get("fbs015.invoice.file.path");
	}

	public String getFbs016Sku1() {
		return get("fbs016.sku1");
	}

	public String getFbs016Sku2() {
		return get("fbs016.sku2");
	}

	public String getFbs016CardNumber() {
		return get("fbs016.card.number");
	}

	public String getFbs016CardExpiry() {
		return get("fbs016.card.expiry");
	}

	public String getFbs016CardCvv() {
		return get("fbs016.card.cvv");
	}

	public String getFbs016InvoiceFilePath() {
		return get("fbs016.invoice.file.path");
	}

	public String getFbs017Sku1() {
		return get("fbs017.sku1");
	}

	public String getFbs017Sku2() {
		return get("fbs017.sku2");
	}

	public String getFbs017CardNumber() {
		return get("fbs017.card.number");
	}

	public String getFbs017CardExpiry() {
		return get("fbs017.card.expiry");
	}

	public String getFbs017CardCvv() {
		return get("fbs017.card.cvv");
	}

	public String getFbs017InvoiceFilePath() {
		return get("fbs017.invoice.file.path");
	}
}
