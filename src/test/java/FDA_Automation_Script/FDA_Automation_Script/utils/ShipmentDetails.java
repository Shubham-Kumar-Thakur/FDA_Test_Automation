package FDA_Automation_Script.FDA_Automation_Script.utils;

public class ShipmentDetails {

    public final String shipmentNumber;
    public final String deliveryPartner;
    public final String tplShipmentId;
    public final String carrierName;
    public final String trackingNumber;
    public String miraklSuffix;

    public ShipmentDetails(String shipmentNumber, String deliveryPartner,
                           String tplShipmentId, String carrierName,
                           String trackingNumber) {
        this.shipmentNumber  = shipmentNumber;
        this.deliveryPartner = deliveryPartner;
        this.tplShipmentId   = tplShipmentId;
        this.carrierName     = carrierName;
        this.trackingNumber  = trackingNumber;
    }

    @Override
    public String toString() {
        return "ShipmentDetails{"
               + "shipmentNumber='" + shipmentNumber + '\''
               + ", deliveryPartner='" + deliveryPartner + '\''
               + ", tplShipmentId='" + tplShipmentId + '\''
               + ", carrierName='" + carrierName + '\''
               + ", trackingNumber='" + trackingNumber + '\''
               + ", miraklSuffix='" + miraklSuffix + '\''
               + '}';
    }
}
