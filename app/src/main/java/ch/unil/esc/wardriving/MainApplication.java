package ch.unil.esc.wardriving;

import android.app.Application;
import android.content.Intent;

public class MainApplication extends Application {
    static public boolean scanning = false;
    static public Intent wifi_scan_intent;
}
