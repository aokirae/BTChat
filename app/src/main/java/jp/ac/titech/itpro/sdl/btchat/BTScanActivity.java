package jp.ac.titech.itpro.sdl.btchat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class BTScanActivity extends AppCompatActivity {
    private final static String TAG = "BTScanActivity";

    private TextView scanStatusText;
    private ProgressBar scanProgress;
    private ListView devListView;

    private final static String KEY_DEVLIST = "devList";

    private ArrayList<BluetoothDevice> devList = null;
    private ArrayAdapter<BluetoothDevice> devListAdapter;

    private BluetoothAdapter btAdapter;
    private BroadcastReceiver btScanReceiver;
    private IntentFilter btScanFilter;

    private final static int REQCODE_ENABLE_BT = 1111;
    private final static int REQCODE_PERMISSIONS = 2222;

    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private enum State {
        Initializing,
        Stopped,
        Scanning
    }

    private State state = State.Initializing;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_btscan);

        scanStatusText = findViewById(R.id.scan_status_text);
        scanProgress = findViewById(R.id.scan_progress);
        devListView = findViewById(R.id.dev_list_view);

        if (savedInstanceState != null)
            devList = savedInstanceState.getParcelableArrayList(KEY_DEVLIST);
        if (devList == null)
            devList = new ArrayList<>();

        devListAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, devList) {
            @Override
            public @NonNull
            View getView(int pos, @Nullable View view, @NonNull ViewGroup parent) {
                if (view == null) {
                    view = LayoutInflater.from(getContext())
                            .inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView nameView = view.findViewById(android.R.id.text1);
                TextView addrView = view.findViewById(android.R.id.text2);
                BluetoothDevice device = getItem(pos);
                if (device != null) {
                    nameView.setText(getString(R.string.format_dev_name,
                                devName(device.getName()),
                                device.getBondState() == BluetoothDevice.BOND_BONDED ? "*" : " "));
                    addrView.setText(device.getAddress());
                }
                return view;
            }
        };
        devListView.setAdapter(devListAdapter);
        devListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                final BluetoothDevice dev = (BluetoothDevice) parent.getItemAtPosition(pos);
                new AlertDialog.Builder(BTScanActivity.this)
                        .setTitle(devName(dev.getName()))
                        .setMessage(R.string.alert_connection_confirmation)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (btAdapter.isDiscovering())
                                            btAdapter.cancelDiscovery();
                                        Intent data = new Intent();
                                        data.putExtra(BluetoothDevice.EXTRA_DEVICE, dev);
                                        BTScanActivity.this.setResult(Activity.RESULT_OK, data);
                                        BTScanActivity.this.finish();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .show();
            }
        });

        btScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    setState(State.Scanning);
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    setState(State.Stopped);
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice dev =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (devListAdapter.getPosition(dev) < 0)
                        devListAdapter.add(dev);
                    devListView.smoothScrollToPosition(devListAdapter.getCount());
                    break;
                }
            }
        };
        btScanFilter = new IntentFilter();
        btScanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btScanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btScanFilter.addAction(BluetoothDevice.ACTION_FOUND);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null)
            setupBT();
        else {
            Toast.makeText(this, getString(R.string.toast_bluetooth_not_supported),
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        registerReceiver(btScanReceiver, btScanFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        unregisterReceiver(btScanReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.btscan, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        MenuItem itemScanStart = menu.findItem(R.id.menu_scan_start);
        MenuItem itemScanStop = menu.findItem(R.id.menu_scan_stop);
        switch (state) {
        case Initializing:
        case Stopped:
            itemScanStart.setVisible(true);
            itemScanStop.setVisible(false);
            return true;
        case Scanning:
            itemScanStart.setVisible(false);
            itemScanStop.setVisible(true);
            return true;
        default:
            return super.onPrepareOptionsMenu(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOpeionsItemSelected");
        switch (item.getItemId()) {
        case R.id.menu_scan_start:
            startScan();
            return true;
        case R.id.menu_scan_stop:
            stopScan();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch (reqCode) {
        case REQCODE_ENABLE_BT:
            if (resCode == Activity.RESULT_OK)
                setupBT1();
            else {
                Toast.makeText(this, R.string.toast_bluetooth_disabled,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (reqCode) {
        case REQCODE_PERMISSIONS:
            for (int i = 0; i < permissions.length; i++) {
                if (grants[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, getString(
                            R.string.toast_scanning_requires_permission, permissions[i]),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startScan1();
            break;
        }
    }

    private void setState(State state) {
        this.state = state;
        switch (state) {
        case Initializing:
        case Stopped:
            scanStatusText.setText(R.string.scan_status_stopped);
            scanProgress.setIndeterminate(false);
            break;
        case Scanning:
            scanStatusText.setText(R.string.scan_status_scanning);
            scanProgress.setIndeterminate(true);
            break;
        }
        invalidateOptionsMenu();
    }

    private void setupBT() {
        Log.d(TAG, "setupBT");
        if (!btAdapter.isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                    REQCODE_ENABLE_BT);
        else
            setupBT1();
    }

    private void setupBT1() {
        Log.d(TAG, "setupBT1");
        for (BluetoothDevice device : btAdapter.getBondedDevices())
            devListAdapter.add(device);
        devListAdapter.notifyDataSetChanged();
        setState(State.Stopped);
    }

    private void startScan() {
        Log.d(TAG, "startScan");
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQCODE_PERMISSIONS);
                return;
            }
        }
        startScan1();
    }

    private void startScan1() {
        Log.d(TAG, "startScan1");
        devListAdapter.clear();
        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    private void stopScan() {
        Log.d(TAG, "stopScan");
        btAdapter.cancelDiscovery();
    }

    private String devName(String name) {
        return name == null ? "(no name)" : name;
    }
}
