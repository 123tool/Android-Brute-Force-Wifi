package com.rainbow.wifi.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.rainbow.wifi.R;
import com.rainbow.wifi.utils.WifiConnector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button btnScan, btnWps;
    private TextView txtLog;
    private ArrayList<String> wifiNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // Menangani izin dinamis (Android 13+ butuh NEARBY_WIFI_DEVICES)
    private String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES} :
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi UI
        listView = findViewById(R.id.wifiList);
        btnScan = findViewById(R.id.btnScan);
        btnWps = findViewById(R.id.btnWps);
        txtLog = findViewById(R.id.txtLog);
        
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 2. Setup List WiFi
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiNames);
        listView.setAdapter(adapter);

        // 3. Klik Tombol Scan
        btnScan.setOnClickListener(v -> {
            addToLog("> Memulai pemindaian WiFi...");
            checkPermissionsAndScan();
        });

        // 4. Klik Tombol WPS
        btnWps.setOnClickListener(v -> showWpsDialog());

        // 5. Klik pada Item WiFi untuk Connect
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedWifi = wifiNames.get(position);
            String ssid = selectedWifi.split(" \\(")[0]; 
            showPasswordDialog(ssid);
        });

        addToLog("> System initialized. Ready for SPY-E operation.");
    }

    // --- LOGIKA SCAN & PERMISSION ---

    private void checkPermissionsAndScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            addToLog("> Meminta izin lokasi & nearby devices...");
            ActivityCompat.requestPermissions(this, permissions, 101);
        } else {
            startWifiScan();
        }
    }

    private void startWifiScan() {
        if (!wifiManager.isWifiEnabled()) {
            addToLog("> Error: WiFi dalam keadaan OFF.");
            Toast.makeText(this, "Nyalakan WiFi dulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        wifiNames.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        addToLog("> Scanning in progress...");
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<ScanResult> results = wifiManager.getScanResults();
                unregisterReceiver(this);

                for (ScanResult result : results) {
                    String details = result.SSID + " (" + result.BSSID + ") [" + result.level + "dBm]";
                    if (!result.SSID.isEmpty() && !wifiNames.contains(details)) {
                        wifiNames.add(details);
                    }
                }
                adapter.notifyDataSetChanged();
                addToLog("> Ditemukan " + results.size() + " Access Points.");
            }
        }
    };

    // --- DIALOG INPUT ---

    private void showPasswordDialog(String ssid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Target: " + ssid);

        final EditText input = new EditText(this);
        input.setHint("Masukkan Password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Connect", (dialog, which) -> {
            String password = input.getText().toString();
            addToLog("> Mencoba otentikasi ke " + ssid);
            WifiConnector.connectToWifi(this, ssid, password);
        });

        builder.setNegativeButton("Cancel", (dialog, id) -> addToLog("> Operasi dibatalkan."));
        builder.show();
    }

    private void showWpsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("WPS Utility Mode");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText bssidInput = new EditText(this);
        bssidInput.setHint("BSSID (MAC Address)");
        layout.addView(bssidInput);

        final EditText pinInput = new EditText(this);
        pinInput.setHint("WPS PIN (8 Digits)");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(pinInput);

        builder.setView(layout);

        builder.setPositiveButton("Attack/Connect", (dialog, which) -> {
            String bssid = bssidInput.getText().toString();
            String pin = pinInput.getText().toString();
            addToLog("> Mencoba koneksi WPS ke BSSID: " + bssid);
            WifiConnector.connectToWps(this, bssid, pin);
        });

        builder.setNegativeButton("Back", null);
        builder.show();
    }

    // --- HELPER LOG ---

    public void addToLog(String message) {
        runOnUiThread(() -> {
            String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            txtLog.append("\n[" + time + "] " + message);
            
            // Auto-scroll ScrollView ke paling bawah
            final View parent = (View) txtLog.getParent();
            parent.post(() -> ((ScrollView) parent).fullScroll(View.FOCUS_DOWN));
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            addToLog("> Izin diberikan. Memulai scan...");
            startWifiScan();
        } else {
            addToLog("> Error: Izin ditolak oleh user.");
        }
    }
}
