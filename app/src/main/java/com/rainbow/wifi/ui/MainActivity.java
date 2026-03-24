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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.rainbow.wifi.R;
import com.rainbow.wifi.utils.WifiConnector;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button btnScan, btnWps;
    private ArrayList<String> wifiNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    // Menangani izin untuk Android 13 (Tiramisu) ke atas dan versi di bawahnya
    private String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES} :
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Inisialisasi Komponen UI
        listView = findViewById(R.id.wifiList);
        btnScan = findViewById(R.id.btnScan);
        btnWps = findViewById(R.id.btnWps);
        
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 2. Setup List Adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiNames);
        listView.setAdapter(adapter);

        // 3. Listener Tombol Scan
        btnScan.setOnClickListener(v -> checkPermissionsAndScan());

        // 4. Listener Tombol WPS
        btnWps.setOnClickListener(v -> showWpsDialog());

        // 5. Listener Klik pada List WiFi (Untuk Koneksi WPA/WPA2)
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedWifi = wifiNames.get(position);
            // Mengambil SSID (Nama WiFi) sebelum tanda kurung
            String ssid = selectedWifi.split(" \\(")[0]; 
            showPasswordDialog(ssid);
        });
    }

    // --- LOGIKA PERMISSION & SCAN ---

    private void checkPermissionsAndScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 101);
        } else {
            startWifiScan();
        }
    }

    private void startWifiScan() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Nyalakan WiFi perangkat Anda!", Toast.LENGTH_SHORT).show();
            return;
        }

        wifiNames.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Memindai WiFi sekitar...", Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<ScanResult> results = wifiManager.getScanResults();
                unregisterReceiver(this);

                for (ScanResult result : results) {
                    // Format tampilan: NamaWiFi (MAC Address) [Sinyal dBm]
                    String details = result.SSID + " (" + result.BSSID + ") [" + result.level + "dBm]";
                    if (!result.SSID.isEmpty() && !wifiNames.contains(details)) {
                        wifiNames.add(details);
                    }
                }
                adapter.notifyDataSetChanged();
            }
        }
    };

    // --- DIALOG INPUT ---

    private void showPasswordDialog(String ssid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Hubungkan ke: " + ssid);

        final EditText input = new EditText(this);
        input.setHint("Masukkan Password WiFi");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Connect", (dialog, which) -> {
            String password = input.getText().toString();
            WifiConnector.connectToWifi(this, ssid, password);
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showWpsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rainbow WPS Utility");
        builder.setMessage("Masukkan data router target");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        final EditText bssidInput = new EditText(this);
        bssidInput.setHint("BSSID (Contoh: AA:BB:CC:DD:EE:FF)");
        layout.addView(bssidInput);

        final EditText pinInput = new EditText(this);
        pinInput.setHint("PIN WPS (8 Digit)");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(pinInput);

        builder.setView(layout);

        builder.setPositiveButton("Mulai Koneksi", (dialog, which) -> {
            String bssid = bssidInput.getText().toString();
            String pin = pinInput.getText().toString();
            if(!bssid.isEmpty() && pin.length() >= 4) {
                WifiConnector.connectToWps(this, bssid, pin);
            } else {
                Toast.makeText(this, "Data BSSID atau PIN tidak lengkap", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWifiScan();
        } else {
            Toast.makeText(this, "Izin diperlukan untuk scan WiFi", Toast.LENGTH_SHORT).show();
        }
    }
}
