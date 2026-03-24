package com.rainbow.wifi.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.rainbow.wifi.R;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ListView listView;
    private Button btnScan;
    private ArrayList<String> wifiNames = new ArrayList<>();
    private ArrayAdapter adapter;

    // Daftar Izin yang dibutuhkan untuk Android 13+
    private String[] permissions = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES} :
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.wifiList);
        btnScan = findViewById(R.id.btnScan);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiNames);
        listView.setAdapter(adapter);
listView.setOnItemClickListener((parent, view, position, id) -> {
    String selectedWifi = wifiNames.get(position);
    // Ambil SSID saja dari string (menghapus BSSID dan dBm)
    String ssid = selectedWifi.split(" \\(")[0]; 
    
    showPasswordDialog(ssid);
    private void showPasswordDialog(String ssid) {
    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
    builder.setTitle("Connect to " + ssid);

    final android.widget.EditText input = new android.widget.EditText(this);
    input.setHint("Masukkan Password");
    input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    builder.setView(input);

    builder.setPositiveButton("Connect", (dialog, which) -> {
        String password = input.getText().toString();
        com.rainbow.wifi.utils.WifiConnector.connectToWifi(this, ssid, password);
    });

    builder.setNegativeButton("Batal", (dialog, which) -> dialog.cancel());
    builder.show();
}

});

        btnScan.setOnClickListener(v -> checkPermissionsAndScan());
    }

    private void checkPermissionsAndScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 101);
        } else {
            startWifiScan();
        }
    }

    private void startWifiScan() {
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "Nyalakan WiFi dulu!", Toast.LENGTH_SHORT).show();
            return;
        }

        wifiNames.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
        Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            unregisterReceiver(this);

            for (ScanResult result : results) {
                String details = result.SSID + " (" + result.BSSID + ") [" + result.level + "dBm]";
                wifiNames.add(details);
            }
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWifiScan();
        }
    }
}
