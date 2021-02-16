package com.demo.assignment;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

  private SwitchCompat wifiSwitch, bluetoothSwitch;
  private WifiManager wifiManager;
  private boolean wifiStatus;
  private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


  @RequiresApi(api = Build.VERSION_CODES.O)
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    wifiSwitch = findViewById(R.id.wifi_switch);
    bluetoothSwitch = findViewById(R.id.bluetooth_switch);
    wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    if (bluetoothAdapter.isEnabled()) {
      bluetoothSwitch.setChecked(true);
      bluetoothSwitch.setText("Bluetooth is ON");
    } else {
      bluetoothSwitch.setChecked(false);
      bluetoothSwitch.setText("Bluetooth is OFF");
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel("channelId", "Connectivity Notification", NotificationManager.IMPORTANCE_HIGH);
      NotificationManager manager = getSystemService(NotificationManager.class);
      manager.createNotificationChannel(channel);
    }

    wifiSwitch.setOnClickListener(v -> toggleWifi(!wifiStatus));
    bluetoothSwitch.setOnClickListener(v -> toggleBluetooth());
  }

  private void toggleWifi(boolean status) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
      wifiManager.setWifiEnabled(status);
    } else {
      Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
      startActivityForResult(panelIntent, 1);
    }
  }

  private void toggleBluetooth() {
    boolean isEnabled = bluetoothAdapter.isEnabled();
    if (isEnabled) {
      bluetoothAdapter.disable();
    } else {
      bluetoothAdapter.enable();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == RESULT_CANCELED) {
      wifiSwitch.setChecked(wifiState());
    }
  }

  public boolean wifiState() {
    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    return wifiManager.isWifiEnabled();
  }

  @Override
  protected void onStart() {
    super.onStart();
    IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    registerReceiver(wifiStateReceiver, intentFilter);

    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    registerReceiver(bluetoothReceiver, filter);
  }

  @Override
  protected void onStop() {
    super.onStop();
    unregisterReceiver(wifiStateReceiver);
    unregisterReceiver(bluetoothReceiver);
  }

  private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
    @SuppressLint("SetTextI18n")
    @Override
    public void onReceive(Context context, Intent intent) {
      int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
        WifiManager.WIFI_STATE_UNKNOWN);
      switch (wifiStateExtra) {
        case WifiManager.WIFI_STATE_ENABLED:
          wifiStatus = true;
          wifiSwitch.setChecked(true);
          wifiSwitch.setText("WiFi is ON");
          break;
        case WifiManager.WIFI_STATE_DISABLED:
          wifiStatus = false;
          wifiSwitch.setChecked(false);
          wifiSwitch.setText("WiFi is OFF");
          break;
      }
      showNotification(wifiState(), bluetoothAdapter.isEnabled());
    }
  };

  private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
    @SuppressLint("SetTextI18n")
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
          BluetoothAdapter.ERROR);
        switch (state) {
          case BluetoothAdapter.STATE_OFF:
            bluetoothSwitch.setChecked(false);
            bluetoothSwitch.setText("Bluetooth is OFF");
            break;
          case BluetoothAdapter.STATE_ON:
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText("Bluetooth is ON");
            break;
          case BluetoothAdapter.STATE_TURNING_ON:
            Toast.makeText(getApplicationContext(), "Turning Bluetooth on...", Toast.LENGTH_SHORT).show();
            break;
          case BluetoothAdapter.STATE_TURNING_OFF:
            Toast.makeText(getApplicationContext(), "Turning Bluetooth off...", Toast.LENGTH_SHORT).show();
            break;
        }
        showNotification(wifiState(), bluetoothAdapter.isEnabled());
      }
    }
  };

  private void showNotification(boolean isWifiEnabled, boolean isBluetoothEnabled) {
    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId");
    builder.setContentTitle("Connectivity status");
    if (isWifiEnabled && !isBluetoothEnabled) {
      builder.setContentText("Only wifi is enabled");
    }
    if (!isWifiEnabled && isBluetoothEnabled) {
      builder.setContentText("Only bluetooth is enabled");
    }
    if (!isWifiEnabled && !isBluetoothEnabled) {
      builder.setContentText("Nothing enabled");
    }
    if (isWifiEnabled && isBluetoothEnabled) {
      builder.setContentText("Both wifi and bluetooth are enabled");
    }
    builder.setSmallIcon(R.drawable.ic_launcher_foreground);
    builder.setOngoing(true);
    builder.setAutoCancel(false);

    NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
    managerCompat.notify(1, builder.build());
  }

}