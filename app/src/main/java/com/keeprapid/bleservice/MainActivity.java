package com.keeprapid.bleservice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    public static final String TAG = "MainActivity";

    private BluetoothDevice bdCurrent;

    //从旧代码获得的UUID
    private static UUID UUID_SERVER = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARREAD = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_CHARWRITE = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothGattCharacteristic characteristicRead;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textview1 = (TextView) findViewById(R.id.textview1);

        //1. Get the BluetoothAdapter
        // Use this check to determine whether BLE is supported on the device.  Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        mBluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        //2.Enable Bluetooth
        // Ensures Bluetooth is available on the device and it is enabled.  If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        initView();
        initGATTServer();
    }

    private TextView textview1;
    private void initView() {
        textview1.setText("");
        showText("starting ...");
        ImageView ivClear = (ImageView) findViewById(R.id.ivClear);
        ivClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textview1.setText("");
            }
        });

        Button bSend = (Button) findViewById(R.id.bSend);
        final EditText etSend = (EditText) findViewById(R.id.etSend);
        bSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] send = OutputStringUtil.toCmdArray(etSend.getText().toString());
                characteristicRead.setValue(send);
                bluetoothGattServer.notifyCharacteristicChanged(bdCurrent, characteristicRead, false);
                showTextView(R.id.tvWrite, OutputStringUtil.toHexString(send));
            }
        });
    }

    private void initGATTServer() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
                .build();

        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUID_SERVER))
                .setIncludeTxPowerLevel(true)
                .build();

        AdvertiseCallback callback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d(TAG, "BLE advertisement added successfully");
                showText("1. initGATTServer success");
                initServices(MainActivity.this);
            }

            @Override
            public void onStartFailure(int errorCode) {
                showText("Failed to add BLE advertisement, reason: " + errorCode);
                showText("1. initGATTServer failure");
            }
        };

        BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, callback);
    }

    private void initServices(Context context) {
        bluetoothGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
        BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //add a read characteristic.
        characteristicRead = new BluetoothGattCharacteristic(UUID_CHARREAD, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //add a descriptor
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicRead.addDescriptor(descriptor);
        service.addCharacteristic(characteristicRead);

        //add a write characteristic.
        BluetoothGattCharacteristic characteristicWrite = new BluetoothGattCharacteristic(UUID_CHARWRITE,
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_READ |
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristicWrite);

        bluetoothGattServer.addService(service);
        showText("2. initServices ok");
    }

    /**
     * 服务事件的回调
     */
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            showText(String.format("onServiceAdded：\n\tstatus = %s", status));
        }

        /**
         * 1.连接状态发生变化时
         * @param device
         * @param status
         * @param newState
         */
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            showText(String.format("1.onConnectionStateChange：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("1.onConnectionStateChange：\n\tstatus = %s, newState =%s ", status, newState));

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            showText(String.format("onCharacteristicReadRequest：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("onCharacteristicReadRequest：\n\trequestId = %s, offset = %s", requestId, offset));

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
//            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
        }

        /**
         * 2.描述被写入时，在这里执行 bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS...  收，触发 onCharacteristicWriteRequest
         * @param device
         * @param requestId
         * @param descriptor
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param value
         */
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            showText(String.format("2.onDescriptorWriteRequest：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("2.onDescriptorWriteRequest：\n\trequestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, OutputStringUtil.toHexString(value)));

            // now tell the connected device that this was all successfull
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        /**
         * 3. onCharacteristicWriteRequest,接收具体的字节
         * @param device
         * @param requestId
         * @param characteristic
         * @param preparedWrite
         * @param responseNeeded
         * @param offset
         * @param requestBytes
         */
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
            showText(String.format("3.onCharacteristicWriteRequest：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("3.onCharacteristicWriteRequest：\n\trequestId = %s, preparedWrite=%s, responseNeeded=%s, offset=%s, value=%s", requestId, preparedWrite, responseNeeded, offset, OutputStringUtil.toHexString(requestBytes)));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);

            //4.处理响应内容
            onResponseToClient(requestBytes, device, requestId, characteristic);
        }

        /**
         * 4.处理响应内容
         *
         * @param requestBytes
         * @param device
         * @param requestId
         * @param characteristic
         */
        private void onResponseToClient(byte[] requestBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic) {
            String msg = OutputStringUtil.toHexString(requestBytes);
            showTextView(R.id.tvRead, msg);
            showText(String.format("4.onResponseToClient：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("4.onResponseToClient：\n\trequestId = %s", requestId));

            String[] array = OutputStringUtil.toHexArray(requestBytes);
            byte[] bytes = new byte[0];
            switch (array[0].toUpperCase()){
                case "89":
                    bytes = getDevicetime();
                    break;
                case "C2":
                    bytes = setDevicetime();
                    break;
            }
            characteristicRead.setValue(bytes);
            bluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false);

            msg = OutputStringUtil.toHexString(bytes);
            showTextView(R.id.tvWrite, msg);
        }

        /**
         * 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
         * @param device
         * @param requestId
         * @param offset
         * @param descriptor
         */
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            showText(String.format("onDescriptorReadRequest：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("onDescriptorReadRequest：\n\trequestId = %s", requestId));
//            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            showText(String.format("5.onNotificationSent：\n\tdevice name = %s, address = %s", device.getName(), device.getAddress()));
            showText(String.format("5.onNotificationSent：\n\tstatus = %s", status));
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            showText(String.format("onMtuChanged：\n\tmtu = %s", mtu));
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            super.onExecuteWrite(device, requestId, execute);
            showText(String.format("onExecuteWrite：\n\trequestId = %s", requestId));
        }
    };

    private void showText(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, msg);
                textview1.append(msg + "\r\n");
            }
        });
    }

    private void showTextView(int id, String text){
        final TextView tv = (TextView) findViewById(id);
        final String msg = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv.setText(msg);
            }
        });
    }

    protected byte[] getDevicetime() {

        Calendar c= Calendar.getInstance();

        c.setFirstDayOfWeek(Calendar.MONDAY);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (0 == dayOfWeek) {
            dayOfWeek = 7;
        }

        int year = c.get(Calendar.YEAR) % 100;
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        int checksum = year ^ month ^ day ^ hour ^ minute ^ second ^ dayOfWeek;

        byte params[] = new byte[10];
        params[0] = (byte) 0x29;
        params[1] = (byte) 0x07;
        params[2] = (byte) year;
        params[3] = (byte) month;
        params[4] = (byte) day;
        params[5] = (byte) hour;
        params[6] = (byte) minute;
        params[7] = (byte) second;
        params[8] = (byte) dayOfWeek;
        params[9] = (byte) checksum;

        String setDateTime = String.format(Locale.getDefault(), "%1$d-%2$d-%3$d %4$d:%5$d:%6$d week:%7$d checksum:%8$d", year, month, day, hour, minute, second, dayOfWeek, checksum);

        showText("setDeviceTime : " + setDateTime);

        return params;
    }

    protected byte[] setDevicetime() {
        byte params[] = new byte[10];
        params[0] = (byte) 0x22;
        params[1] = (byte) 0x04;

        return params;
    }

}