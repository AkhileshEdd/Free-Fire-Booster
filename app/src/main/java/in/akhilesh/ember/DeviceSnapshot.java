package in.akhilesh.ember;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;

final class DeviceSnapshot {
    int battery = -1;
    Float temperature;
    long availableMemory, totalMemory, freeStorage;
    boolean connected, charging, saver;
    String network = "Offline";
    static DeviceSnapshot read(Context context) {
        DeviceSnapshot d = new DeviceSnapshot();
        Intent b = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            int level = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) d.battery = Math.round(100f * level / scale);
            if (b.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) d.temperature = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
            d.charging = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        }
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        context.getSystemService(ActivityManager.class).getMemoryInfo(info);
        d.availableMemory = info.availMem; d.totalMemory = info.totalMem;
        d.freeStorage = new StatFs(Environment.getDataDirectory().getPath()).getAvailableBytes();
        d.saver = context.getSystemService(PowerManager.class).isPowerSaveMode();
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (caps != null) {
            d.connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            d.network = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ? "Wi-Fi" : caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ? "Mobile data" : "Network";
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) d.network += " · VPN";
            if (!d.connected) d.network += " · no internet";
        }
        return d;
    }
}
