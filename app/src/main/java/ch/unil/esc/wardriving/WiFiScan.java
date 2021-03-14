package ch.unil.esc.wardriving;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
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

import androidx.annotation.Nullable;


public class WiFiScan extends Service {
    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private ImageView image;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;

    private List<CustomScanResult> all_results;
    public Timer timer;

    public FileWriter fw;
    static public LocationManager locationManager;

    private int ONGOING_NOTIFICATION_ID = 1909;
    private NotificationChannel notificationChannel;
    private String NOTIFICATION_CHANNEL_ID = "ch.unil.esc.wardriving";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start_scan();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start_scan() {
        // Keep app in foreground (cf. https://developer.android.com/guide/components/foreground-services)
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        this.notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                "Wardriving", NotificationManager.IMPORTANCE_LOW);

        Notification notification =
                new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.icon)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        // Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification);

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

        try {
            this.fw = new FileWriter(filename);
            fw.append(CustomScanResult.csv_headers());
            arrayList.clear();
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            //wifiManager.startScan();
            Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
            this.timer = new Timer();
            this.timer.schedule(new TimerTask() {
                //https://stackoverflow.com/questions/4612579/call-particular-method-after-regular-interval-of-time
                @Override
                public void run() {
                    wifiManager.startScan();
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

            try {
                Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                System.out.println(locationGPS);
                if (locationGPS != null) {
                    for (ScanResult scanResult : results) {
                        long real_timestamp = System.currentTimeMillis() - SystemClock.elapsedRealtime() + (scanResult.timestamp / 1000);
                        CustomScanResult custom_result = new CustomScanResult(scanResult.BSSID, scanResult.SSID, scanResult.frequency, scanResult.level, real_timestamp);
                        custom_result.add_location(locationGPS.getLatitude(), locationGPS.getLongitude(), locationGPS.getAccuracy(), locationGPS.getTime());
                        String csv_line = custom_result.to_csv();
                        arrayList.add(csv_line);
                        adapter.notifyDataSetChanged();
                        fw.append(csv_line);
                    }
                } else {
                    //Toast.makeText(MainActivity.super.getBaseContext(), "No GPS Fix", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ;
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
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
