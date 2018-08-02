package com.suhen.android.libble.peripheral;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.suhen.android.libble.BLE;
import com.suhen.android.libble.utils.ClsUtils;
import com.suhen.android.libble.utils.StringUtil;
import com.suhen.android.libble.utils.TypeConversion;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by liuqing
 * 2018/7/26.
 * Email: suhen0420@163.com
 * <p>
 * This is BLE Peripheral simple encapsulation, if you need more character, just override.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public abstract class BlePeripheral extends BluetoothGattServerCallback implements IPeripheral {
    protected static final String TAG = BlePeripheral.class.getSimpleName();

    protected Context mContext;

    protected BluetoothManager mBluetoothManager;
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeAdvertiser mLeAdvertiser;
    protected BluetoothGattServer mBluetoothGattServer;

    private boolean isConnected;
    private BluetoothStatusReceiver mBluetoothStatusReceiver;

    private Toast mToast;

    protected BlePeripheral(Context context) {
        mContext = context.getApplicationContext();
    }

    @SuppressLint("ShowToast")
    @Override
    public void onCreate() {
        mBluetoothStatusReceiver = new BluetoothStatusReceiver();
        IntentFilter bleIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(mBluetoothStatusReceiver, bleIntentFilter);

        mToast = Toast.makeText(mContext, BLE.NOT_SUPPORT_PERIPHERAL, Toast.LENGTH_LONG);
    }

    @Override
    public boolean isSupportPeripheral() {
        if (mContext.getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            mBluetoothManager = (BluetoothManager) mContext.getSystemService(
                    Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }

            mBluetoothAdapter = mBluetoothManager.getAdapter();

            return mBluetoothAdapter != null;
        } else {
            return false;
        }
    }

    @Override
    public void setup() {
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(
                Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.d(TAG, "setup: " + BLE.NOT_SUPPORT_PERIPHERAL);
            mToast.show();
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.d(TAG, "setup: BluetoothAdapter is null, " + BLE.NOT_SUPPORT_PERIPHERAL);
            mToast.show();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) { // if bt not enable

            if (!mBluetoothAdapter.enable()) { // if bt open fail
                openBTByUser();
            }

        } else {
            start();
        }
    }

    @Override
    public void preparePair() {
        mBluetoothAdapter.startDiscovery();
        ClsUtils.setDiscoverableTimeout(100);
    }

    @Override
    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mBluetoothStatusReceiver);
        stop();
    }

    protected abstract void onPeripheralStartSuccess(AdvertiseSettings settingsInEffect);

    protected abstract void onPeripheralStartFailure(int errorCode);

    protected abstract void onConnected();

    protected abstract void onDisconnected();

    protected abstract void onReceiveBytes(byte[] bytes);

    @Override
    public abstract void sendBleBytes(byte[] bytes);

    /**
     * Note that some devices do not support long names.
     * Recommended within 12 bytes.
     */
    protected String generatePeripheralName() {
        String mac = StringUtil.getString(mContext, BLE.BT_MAC, "");

        mac = mac.substring(mac.length() - 7, mac.length());
        mac = mac.replaceAll(":", "");

        return "Suhen_" + mac;
    }

    /**
     * @return Your BLE peripheral settings.
     */
    protected AdvertiseSettings generateAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();
    }

    /**
     * Recommended within 8 bytes.
     */
    protected AdvertiseData generateAdvertiseData() {
        String mac = StringUtil.getString(mContext, BLE.BT_MAC, "00:00:00:00:00:00");
        mac = mac.replaceAll(":", "");
        byte[] mac_bytes = TypeConversion.hexString2Bytes(mac);
        Log.d(TAG, "generateAdvertiseData: " + Arrays.toString(mac_bytes));

        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addManufacturerData(BLE.MANUFACTURER_ID, mac_bytes)
                //.addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_UUID)))
                .build();
    }

    /**
     * All params must be standard UUID, you can use {@link java.util.UUID} generate this param,
     * or manually generated UUID.
     * <p>
     * If you use illegal UUID, peripheral will be open failure.
     */
    protected abstract void addGattService();

    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */
    /* BluetoothGattServerCallback START */     /* BluetoothGattServerCallback START */

    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
        super.onConnectionStateChange(device, status, newState);
        switch (newState) {

            case BluetoothProfile.STATE_CONNECTED:
                isConnected = true;
                Log.d(TAG, "onConnectionStateChange: peripheral STATE_CONNECTED");

                onConnected(); // central incomming
                break;

            case BluetoothProfile.STATE_DISCONNECTED:
                isConnected = false;
                Log.d(TAG, "onConnectionStateChange: peripheral STATE_DISCONNECTED");

                onDisconnected(); // central outgoing
                break;
        }
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
        super.onServiceAdded(status, service);
        Log.v(TAG, "onServiceAdded: status: " + status + ", service: " + service.getUuid());
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
            Log.v(TAG, "onServiceAdded: service has characteristic: " + characteristic.getUuid());
        }
    }

    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
    }

    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattCharacteristic characteristic,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                                           responseNeeded, offset, value);
        if (characteristic.getUuid()
                          .toString()
                          .equals(BLE.CHAR_WRITE_UUID)) {
            // receive data:
            Log.i(TAG, "onCharacteristicWriteRequest: " + Arrays.toString(value));
            onReceiveBytes(value);
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                                              value);
        }
    }

    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                        BluetoothGattDescriptor descriptor) {
        super.onDescriptorReadRequest(device, requestId, offset, descriptor);
    }

    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                         BluetoothGattDescriptor descriptor,
                                         boolean preparedWrite,
                                         boolean responseNeeded, int offset, byte[] value) {
        super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite,
                                       responseNeeded,
                                       offset, value);
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
        super.onExecuteWrite(device, requestId, execute);
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
        super.onNotificationSent(device, status);
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
        super.onMtuChanged(device, mtu);
    }

    @Override
    public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyUpdate(device, txPhy, rxPhy, status);
    }

    @Override
    public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
        super.onPhyRead(device, txPhy, rxPhy, status);
    }

    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */
    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */
    /* BluetoothGattServerCallback END */       /* BluetoothGattServerCallback END */

    private class BluetoothStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        isConnected = false;
                        Log.e(TAG, "bluetooth OFF");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.e(TAG, "bluetooth TURNING_OFF");
                        if (isSupportPeripheral()) {
                            stop();
                        }
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.e(TAG, "bluetooth ON");

                        if (isSupportPeripheral()) {
                            start();
                        }

                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.e(TAG, "bluetooth TURNING_ON");
                        break;
                }
            }
        }
    }

    // Avoid multiple initialization
    private AtomicBoolean peripheralFlag = new AtomicBoolean(false);

    /**
     * on bluetooth is open, or it is already open.
     */
    private synchronized void start() {
        if (peripheralFlag.get()) {
            Log.w(TAG, "start: peripheral is already initialized.");
            return;
        }

        try {
            // step 1
            String btAddress = ClsUtils.getBtAddressViaReflection();
            StringUtil.putString(mContext, BLE.BT_MAC, btAddress);

            // step 2
            preparePair();

            // step 3
            String bluetoothName = generatePeripheralName();
            Log.d(TAG, "start: " + bluetoothName);
            mBluetoothAdapter.setName(bluetoothName);

            // step 4
            if (!openGattServer()) {
                Log.d(TAG, "start: openGattServer error, " + BLE.NOT_SUPPORT_PERIPHERAL);
                mToast.show();
                return;
            }
            addGattService();

            // step 5
            startAdvertising();

        } catch (Exception e) {
            Log.w(TAG, "start: ", e);
        } finally {
            if (mBluetoothAdapter.isEnabled()) {
                peripheralFlag.set(true);
            }
        }
    }

    private synchronized void stop() {
        try {
            stopAdvertising();
            mBluetoothGattServer.clearServices();
            mBluetoothGattServer.close();
        } catch (Exception e) {
            Log.w(TAG, "stop: ", e);
        } finally {
            peripheralFlag.set(false);
        }
    }

    private void openBTByUser() {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(mContext, BLE.BT_NO_PERMISSION, Toast.LENGTH_LONG)
                 .show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mContext.startActivity(enableBtIntent);
        }
    }

    /* GATT */
    private boolean openGattServer() {
        mLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mLeAdvertiser == null) {
            return false;
        }

        mBluetoothGattServer = mBluetoothManager.openGattServer(mContext, this);
        mBluetoothGattServer.clearServices();

        return true;
    }

    protected void startAdvertising() {
        if (mLeAdvertiser != null) {
            mLeAdvertiser.startAdvertising(generateAdvertiseSettings(),
                                           generateAdvertiseData(), mAdvertiseCallback);
        }
    }

    protected void stopAdvertising() {
        if (mLeAdvertiser != null) {
            mLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "onStartSuccess: " + settingsInEffect.toString());
            onPeripheralStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.w(TAG, "onStartFailure: " + errorCode);
            onPeripheralStartFailure(errorCode);
        }
    };
}
