package project.jujiiz.app.predictclient.models;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

/**
 * Created by JuJiiz on 6/3/2561.
 */

public class ModelNetwork {
    public static String getDeviceIP(Context context) {
        String ip = "";
        //ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        //String connectType = activeNetwork.getTypeName();
        //Log.d("MYLOG", "ipAddressTest: " + connectType);

        //VpnService vpnService = new VpnService();

        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wm.getConnectionInfo();

        int ipAddress = connectionInfo.getIpAddress();
        ip = Formatter.formatIpAddress(ipAddress);
        return ip;
    }
}
