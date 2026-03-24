package com.rainbow.wifi.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;

public class WifiConnector {

    public static void connectToWifi(Context context, String ssid, String password) {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Cara Modern (Android 10+)
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

            connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    connectivityManager.bindProcessToNetwork(network);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                }
            });
            
            Toast.makeText(context, "Meminta izin koneksi ke " + ssid, Toast.LENGTH_SHORT).show();

        } else {
            // Cara Lama (Untuk HP jadul yang masih kamu dukung)
            Toast.makeText(context, "Gunakan metode lama untuk versi ini", Toast.LENGTH_SHORT).show();
        }
    }
}
