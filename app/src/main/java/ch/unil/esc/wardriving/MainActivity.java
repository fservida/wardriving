package ch.unil.esc.wardriving;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

// TUTO - https://ssaurel.medium.com/develop-a-wifi-scanner-android-application-daa3b77feb73
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileWriter;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
// END TUTO Requirements


public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private ImageView image;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;

    private List<CustomScanResult> all_results;

    static public LocationManager locationManager;

    //private boolean scanning = false;

    private int ONGOING_NOTIFICATION_ID = 1909;
    private TextView text_gps_latitude;
    private TextView text_gps_longitude;
    private TextView text_gps_accuracy;
    private TextView text_gps_timestamp;
    private TextView text_gps_timestamp_utc;
    private Timer timer;
    //private Intent wifi_scan_intent;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = findViewById(R.id.scanBtn);
        image = findViewById(R.id.imageView3);


        text_gps_latitude = findViewById(R.id.textView4);
        text_gps_longitude = findViewById(R.id.textView9);
        text_gps_accuracy = findViewById(R.id.textView11);
        text_gps_timestamp = findViewById(R.id.textView13);
        text_gps_timestamp_utc = findViewById(R.id.textView15);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        init_gps();

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);

        if (MainApplication.scanning) {
            Log.v("Wardriving", "Scan Already Underway");
            buttonScan.setText("STOP SCAN");
        }

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!MainApplication.scanning) {
                    Log.v("Wardriving", "Scanning WiFi");
                    start_scan();
                    buttonScan.setText("STOP SCAN");
                } else {
                    stop_scan();
                    buttonScan.setText("SCAN WIFI");
                }
            }
        });
    }

    private void init_gps() {
        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // Need to setup a listener to force research of a GPS fix, else LastKnownLocation will always return a cached location.
        LocationListener locationListener = new GPSLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100, 1, locationListener);
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            //https://stackoverflow.com/questions/4612579/call-particular-method-after-regular-interval-of-time
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        update_location();
                    }
                });
            }
        }, 0, 2000);
    }

    private void stop_scan() {
        MainApplication.scanning = false;
        stopService(MainApplication.wifi_scan_intent);
    }

    private void start_scan() {
        MainApplication.scanning = true;
        MainApplication.wifi_scan_intent = new Intent(this, WiFiScan.class);
        startService(MainApplication.wifi_scan_intent);
    }

    public void update_location() {
        @SuppressLint("MissingPermission") Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        float accuracy = loc.getAccuracy();
        long timestamp = loc.getTime();
        double longitude = loc.getLongitude();
        double latitude = loc.getLatitude();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US);
        String fix_datetime = sdf.format(new Date(timestamp));

        text_gps_latitude.setText(String.valueOf(latitude));
        text_gps_longitude.setText(String.valueOf(longitude));
        text_gps_accuracy.setText(String.valueOf(accuracy));
        text_gps_timestamp.setText(String.valueOf(timestamp));
        text_gps_timestamp_utc.setText(fix_datetime);
    }

    private class GPSLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            Log.v("Wardriving", "Received Location Update on Main Activity");
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}