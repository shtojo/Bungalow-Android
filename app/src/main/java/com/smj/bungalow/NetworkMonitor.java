package com.smj.bungalow;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

import java.util.Objects;

class NetworkMonitor {
    public enum activeNetworkType {NONE, WIFI, CELLULAR}
    private activeNetworkType activeNetwork = activeNetworkType.NONE;
    String routerMacAddress = "";  // valid if on wifi
    String wifiName = "";  // valid if on wifi
    private Activity activity;

    private ConnectivityManager networkConnectivityManager;

    NetworkMonitor(MainActivity activity) {
        this.activity = activity;
        networkConnectivityManager = (ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        Objects.requireNonNull(networkConnectivityManager).registerDefaultNetworkCallback(networkCallback);
    }

    private final ConnectivityManager.NetworkCallback networkCallback
            = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            activeNetwork = activeNetworkType.NONE;
            routerMacAddress = "";
            wifiName = "";

            if (networkConnectivityManager.isActiveNetworkMetered()) {
                activeNetwork = activeNetworkType.CELLULAR;
            } else {
                NetworkInfo networkInfo = networkConnectivityManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    if (networkInfo.isConnected()) {
                        final WifiManager wifiManager =
                                (WifiManager)activity.getSystemService(Context.WIFI_SERVICE);
                        final WifiInfo connectionInfo =
                                Objects.requireNonNull(wifiManager).getConnectionInfo();
                        routerMacAddress = connectionInfo.getBSSID();
                        wifiName = connectionInfo.getSSID();
                        if ((routerMacAddress == null) || (wifiName == null)) return;
                        activeNetwork = activeNetworkType.WIFI;
                    }
                }
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            activeNetwork = activeNetworkType.NONE;
        }
    };

    void stop() {
        // remember to stop the callback in on destroy
        activeNetwork = activeNetworkType.NONE;
        networkConnectivityManager.unregisterNetworkCallback(networkCallback);
        routerMacAddress = "";
        wifiName = "";
    }

    boolean isWifi() {
        return activeNetwork== activeNetworkType.WIFI;
    }
    boolean isCellular() {
        return activeNetwork== activeNetworkType.CELLULAR;
    }
    boolean isNone() {
        return activeNetwork== activeNetworkType.NONE;
    }
    String getMacAddress() { return routerMacAddress; }
    String getWifiName() { return wifiName; }
}
