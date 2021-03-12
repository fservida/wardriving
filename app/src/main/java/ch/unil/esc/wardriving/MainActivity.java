package ch.unil.esc.wardriving;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;

// TUTO - https://ssaurel.medium.com/develop-a-wifi-scanner-android-application-daa3b77feb73
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private Timer timer = new Timer();

    public FileWriter fw;
    static public LocationManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = findViewById(R.id.scanBtn);
        image = findViewById(R.id.imageView3);

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("Wardriving", "Scanning WiFi");
                scanWifi();
                buttonScan.setText("Scanning...");
            }
        });

        listView = findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        //scanWifi();
    }

    private void scanWifi() {
        //
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US);
        String filename = sdf.format(new Date()) + "_wardriving_log.csv";
        // https://stackoverflow.com/questions/15402976/how-to-create-a-csv-file-in-android
        //File folder = new File(Environment.getExternalStorageDirectory()
        //        + "/ch.unil.esc.wardriving");
        File folder = new File(String.valueOf(getExternalFilesDir(null)));
        filename = folder.toString() + "/" + filename;
        boolean var = false;
        if (!folder.exists())
            System.out.println("Creating Dir");
        var = folder.mkdir();
        System.out.println("" + var);
        System.out.println("" + filename);

        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new WardrivingLocationListener();
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
        this.locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

        Location location_gps = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        System.out.println(location_gps);

        try {
            this.fw = new FileWriter(filename);
            fw.append(CustomScanResult.csv_headers());
            arrayList.clear();
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            //wifiManager.startScan();
            Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
            this.timer.schedule(new TimerTask() {
                //https://stackoverflow.com/questions/4612579/call-particular-method-after-regular-interval-of-time
                @Override
                public void run() {
                    wifiManager.startScan();
                    /*@SuppressLint("MissingPermission") Location locationGPS = MainActivity.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    System.out.println(locationGPS);
                    if (locationGPS == null){
                        Toast.makeText(MainActivity.super.getBaseContext(), "Enable Location Services to scan", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Log.v("Wardriving", "Scanning");
                    }*/
                    Log.v("Wardriving", "Scanning");
                }
            }, 0, 2000);
        } catch (IOException e) {
            Toast.makeText(this, "Error Creating Log File", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

    }

    final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            results = wifiManager.getScanResults();
            //unregisterReceiver(this);

            try {
                Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                System.out.println(locationGPS);

                if (locationGPS != null){
                    for (ScanResult scanResult : results) {
                        CustomScanResult custom_result = new CustomScanResult(scanResult.BSSID, scanResult.SSID, scanResult.frequency, scanResult.level, scanResult.timestamp);
                        custom_result.add_location(locationGPS.getLatitude(), locationGPS.getLongitude(), locationGPS.getAccuracy());
                        String csv_line = custom_result.to_csv();
                        arrayList.add(csv_line);
                        adapter.notifyDataSetChanged();
                        fw.append(csv_line);
                    }
                }
                else {
                    Toast.makeText(MainActivity.super.getBaseContext(), "No GPS Fix", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    };

    private class WardrivingLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            String TAG = "Wardriving";
            float accuracy = loc.getAccuracy();
            double longitude = loc.getLongitude();
            double latitude = loc.getLatitude();
            String longitude_str = "Longitude: " + longitude;
            Log.v(TAG, longitude_str);
            String latitude_str = "Latitude: " + latitude;
            Log.v(TAG, latitude_str);
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}