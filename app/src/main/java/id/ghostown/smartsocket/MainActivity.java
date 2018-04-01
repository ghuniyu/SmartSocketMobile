package id.ghostown.smartsocket;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.hawk.Hawk;

import java.io.IOException;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.name)
    TextView tvName1;

    @BindView(R.id.name2)
    TextView tvName2;

    @BindView(R.id.device)
    TextView deviceInfo;

    @BindView(R.id.btn_1)
    ImageView btn1;

    @BindView(R.id.btn_2)
    ImageView btn2;

    @BindView(R.id.status_1)
    TextView status1;

    @BindView(R.id.status_2)
    TextView status2;

    ProgressDialog progress;

    BluetoothSocket btSocket = null;
    BluetoothAdapter myBluetooth = null;
    Boolean isBtConnected = false;

    boolean s1_isOn = false;
    boolean s2_isOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String address = Hawk.get(Constants.TAG_BT_ADD, null);
        if (address == null) {
            startActivity(new Intent(this, DeviceListActivity.class));
        }
        checkName();

        if (getIntent().getBooleanExtra(Constants.TAG_NEW_DEVICE, false))
            new ConnectBT().execute();
    }

    private void checkStatus() {
        String status = Hawk.get(Constants.TAG_BT_INF, "Disconnected");
        deviceInfo.setText("Device Status : " + status);
        if (isBtConnected) {
            deviceInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        } else {
            deviceInfo.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent));
        }
    }

    @Override
    public int getContent() {
        return R.layout.activity_main;
    }

    @OnClick(R.id.name)
    void edit1() {
        editName(tvName1, Constants.TAG_SOC_1_NAME);
    }

    @OnClick(R.id.name2)
    void edit2() {
        editName(tvName2, Constants.TAG_SOC_2_NAME);
    }

    void editName(final TextView v, final String tag) {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("SmartSocket");

        alertDialog.setMessage("Enter New Socket Name");
        final EditText input = new EditText(MainActivity.this);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setPositiveButton("SAVE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString();
                        if (name.length() == 0) {
                            input.setError("Name Can't be Blank");
                        } else {
                            v.setText(name);
                            Hawk.put(tag, name);
                            Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    private void checkName() {
        tvName1.setText(Hawk.get(Constants.TAG_SOC_1_NAME, "Tap to Change"));
        tvName2.setText(Hawk.get(Constants.TAG_SOC_2_NAME, "Tap to Change"));
    }

    @OnClick(R.id.device)
    void chooseDevice() {
        startActivity(new Intent(this, DeviceListActivity.class));
    }

    @OnLongClick(R.id.device)
    boolean reset() {
        Toast.makeText(this, "App Reset", Toast.LENGTH_SHORT).show();
        Hawk.deleteAll();

        if (btSocket != null) {
            try {
                switchSocket("#1LOW#");
                switchSocket("#2LOW#");
                btSocket.close();
            } catch (IOException e) {
                Toast.makeText(this, "Failed to Reset", Toast.LENGTH_SHORT).show();
            }
        }

        finish();
        System.exit(2);
        return false;
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {

        private boolean ConnectSuccess;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait...");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(Hawk.get(Constants.TAG_BT_ADD, ""));
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(Constants.TAG_UUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                deviceInfo.setText("Device Status : Connection Failed");
                finish();
            } else {
                isBtConnected = true;
                checkStatus();
            }
            progress.dismiss();
        }
    }

    void switchSocket(String cmd) {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write(cmd.toString().getBytes());
            } catch (IOException e) {
                Toast.makeText(this, "Failed to Send Command", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @OnClick(R.id.btn_1)
    void socket1() {
        if (s1_isOn) {
            switchSocket("#1LOW#");
        } else
            switchSocket("#1HIGH#");
        s1_isOn = !s1_isOn;
        notifyIcon(btn1, status1, s1_isOn);
    }

    @OnClick(R.id.btn_2)
    void socket2() {
        if (s2_isOn) {
            switchSocket("#2LOW#");
        } else
            switchSocket("#2HIGH#");
        s2_isOn = !s2_isOn;
        notifyIcon(btn2, status2, s2_isOn);
    }

    void notifyIcon(ImageView view, TextView title, boolean isON) {
        if (isON) {
            view.setImageResource(R.drawable.ic_socket);
            title.setText("ACTIVE");
            title.setTextColor(ContextCompat.getColor(this, R.color.colorGreen));
        } else {
            view.setImageResource(R.drawable.ic_socket_off);
            title.setText("INACTIVE");
            title.setTextColor(ContextCompat.getColor(this, R.color.colorTextGrey));
        }
    }
}
