package project.jujiiz.app.predictclient.models;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by JuJiiz on 6/3/2561.
 */

public class ModelNetwork {

    public static String getWifiIP(Context context) {
        String ip = "";

        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo connectionInfo = wm.getConnectionInfo();

        int ipAddress = connectionInfo.getIpAddress();
        ip = Formatter.formatIpAddress(ipAddress);
        return ip;
    }

    public static String getMobileIP() {
        String ipaddress = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ipaddress = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.d("MYLOG", "Exception in Get IP Address: " + ex.toString());
        }
        return ipaddress;
    }
}
