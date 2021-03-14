package ch.unil.esc.wardriving;

public class CustomScanResult {
    String delimiter = ";";

    String bssid;
    String ssid;
    int frequency;
    int level;
    long timestamp;
    long gpstimestamp;
    double latitude;
    double longitude;
    float accuracy;

    CustomScanResult(String bssid, String ssid, int frequency, int level, long timestamp) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.frequency = frequency;
        this.level = level;
        this.timestamp = timestamp;
    }

    static public String csv_headers() {
        String delimiter = ";";
        return "BSSID" + delimiter + "\"SSID\"" + delimiter + "frequency" + delimiter + "level" + delimiter + "scan_timestamp" + delimiter + "gps_timestamp" + delimiter + "latitude" + delimiter + "longitude" + delimiter + "accuracy" + "\n";
    }

    public String to_csv() {
        return this.bssid + this.delimiter + '"' + this.ssid + '"' + this.delimiter + this.frequency + this.delimiter + this.level + this.delimiter + this.timestamp + this.delimiter + this.gpstimestamp + this.delimiter + this.latitude + this.delimiter + this.longitude + this.delimiter + this.accuracy + "\n";
    }

    public void add_location(double latitude, double longitude, float accuracy, long gpstimestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.gpstimestamp = gpstimestamp;
    }
}
