package ch.unil.esc.wardriving;

import android.Manifest;
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
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;


public class WiFiScan extends Service {
    private WifiManager wifiManager;
    private ListView listView;
    private Button buttonScan;
    private ImageView image;
    private int size = 0;
    private List<ScanResult> results;
    private ArrayList<String> arrayList = new ArrayList<>();

    private List<CustomScanResult> all_results;
    public Timer timer;

    public FileWriter fw;
    public LocationManager locationManager;

    private int ONGOING_NOTIFICATION_ID = 1909;
    private NotificationChannel notificationChannel;
    private String NOTIFICATION_CHANNEL_ID = "ch.unil.esc.wardriving";
    private Notification notification;

    private long network_measures_count;
    private String filename;
    private String run_name;
    private String run_date;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        start_scan();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start_scan() {
        init_gps();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ", Locale.US);
        run_date = sdf.format(new Date());
        run_name = run_date + "_wardriving_log.csv";
        // https://stackoverflow.com/questions/15402976/how-to-create-a-csv-file-in-android

        File folder = new File(String.valueOf(getExternalFilesDir(null)));
        filename = folder.toString() + "/" + run_name;
        boolean var = false;
        if (!folder.exists())
            System.out.println("Creating Dir");
        var = folder.mkdir();
        System.out.println("" + var);
        System.out.println("" + filename);

        // Keep app in foreground (cf. https://developer.android.com/guide/components/foreground-services)
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        this.notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                "Wardriving", NotificationManager.IMPORTANCE_LOW);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(notificationChannel);

        notification =
                new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("Scanning... Run: " + run_date)
                        .setContentText("")
                        .setSmallIcon(R.drawable.icon)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        // Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification);

        network_measures_count = 0;

        try {
            this.fw = new FileWriter(filename);
            fw.append(CustomScanResult.csv_headers());
            arrayList.clear();
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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
                        //arrayList.add(csv_line);
                        fw.append(csv_line);
                        network_measures_count += 1;
                    }
                    Intent notificationIntent = new Intent(WiFiScan.super.getBaseContext(), MainActivity.class);
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    PendingIntent pendingIntent =
                            PendingIntent.getActivity(WiFiScan.super.getBaseContext(), 0, notificationIntent, 0);
                    notification =
                            new Notification.Builder(WiFiScan.super.getBaseContext(), NOTIFICATION_CHANNEL_ID)
                                    .setContentTitle("Scanning... Run: " + run_date)
                                    .setContentText(network_measures_count + " Networks Logged.")
                                    .setSmallIcon(R.drawable.icon)
                                    .setContentIntent(pendingIntent)
                                    .setTicker(getText(R.string.ticker_text))
                                    .build();
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(ONGOING_NOTIFICATION_ID, notification);

                } else {
                    //Toast.makeText(WiFiScan.super.getBaseContext(), "No GPS Fix", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ;
    };

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

        Location location_gps = this.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        System.out.println(location_gps);
    }

    private class GPSLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            Log.v("Wardriving", "Received Location Update on Service");
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }
}
