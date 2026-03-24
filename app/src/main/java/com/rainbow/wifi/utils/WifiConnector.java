package com.rainbow.wifi.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class WifiConnector {

    /**
     * Fungsi untuk menghubungkan ke WiFi menggunakan SSID dan Password (WPA/WPA2)
     * Kompatibel dengan Android 10 (API 29) hingga Android 14+
     */
    public static void connectToWifi(Context context, String ssid, String password) {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Konfigurasi Spesifik untuk Android Modern
            WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Memungkinkan koneksi tanpa internet
                    .setNetworkSpecifier(specifier)
                    .build();

            connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    // Mengikat proses aplikasi ke jaringan ini agar bisa berkomunikasi
                    connectivityManager.bindProcessToNetwork(network);
                    
                    showToast(context, "Berhasil Terhubung ke " + ssid);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    showToast(context, "Koneksi ke " + ssid + " Dibatalkan atau Gagal");
                }
            });

            showToast(context, "Menunggu konfirmasi sistem untuk: " + ssid);

        } else {
            // Untuk Android 9 ke bawah, sistem keamanannya berbeda
            showToast(context, "Versi Android lama memerlukan metode Legacy (WifiConfiguration)");
        }
    }

    /**
     * Fungsi untuk mencoba koneksi spesifik (Pendekatan WPS Modern)
     * Catatan: Android 10+ tidak mendukung Brute Force PIN secara native lewat API Publik
     */
    public static void connectToWps(Context context, String bssid, String pin) {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                        .setBssid(MacAddress.fromString(bssid))
                        .build();

                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .setNetworkSpecifier(specifier)
                        .build();

                connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        connectivityManager.bindProcessToNetwork(network);
                        showToast(context, "Berhasil Menemukan & Terhubung ke BSSID: " + bssid);
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        showToast(context, "Target WPS tidak ditemukan atau tidak merespon");
                    }
                });

                showToast(context, "Menganalisis Target BSSID...");
                
            } catch (IllegalArgumentException e) {
                showToast(context, "Format BSSID Salah!");
            }
        } else {
            showToast(context, "Fitur ini memerlukan Android 10 atau akses Root");
        }
    }

    // Helper untuk menampilkan Toast dari dalam Callback (Thread berbeda)
    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> 
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }
}
