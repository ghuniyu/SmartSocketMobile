package id.ghostown.smartsocket;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

public class DeviceListActivity extends BaseActivity {

    private BluetoothAdapter mBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;

    @BindView(R.id.list_device)
    ListView listDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (mBluetooth == null) {
            Toast.makeText(this, "Can't Detect Bluetooth Device", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!mBluetooth.isEnabled()) {
                Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(btIntent, 1337);
            }
        }

        pairedDevices = mBluetooth.getBondedDevices();
        ArrayList devices = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                devices.add(String.format("%s\n%s", bt.getName(), bt.getAddress()));
            }
        }

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, devices);
        listDevice.setAdapter(adapter);
        listDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                Hawk.put(Constants.TAG_BT_ADD, address);
                Hawk.put(Constants.TAG_BT_INF, "Connected to " + info.substring(0, info.length() - 18));
                startActivity(new Intent(DeviceListActivity.this, MainActivity.class).putExtra(Constants.TAG_NEW_DEVICE, true));
            }
        });
    }

    @Override
    public int getContent() {
        return R.layout.activity_devicelist;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
