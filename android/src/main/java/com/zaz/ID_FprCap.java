package com.zaz;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;

public class ID_FprCap {
    static {
        System.loadLibrary("ID_FprCap");
    }

    public int LIVESCAN_SUCCESS = 1;
    public int LIVESCAN_E_PARAM = -1;
    public int LIVESCAN_E_MEMORY = -2;
    public int LIVESCAN_E_NO_DEVICE = -4;
    public int LIVESCAN_E_TRANS = -101;

    public native static int LIVESCAN_Handle(int handle);
    public native static int LIVESCAN_Init();
    public native static int LIVESCAN_Close();
    public native static int LIVESCAN_GetChannelCount();
    public native static int LIVESCAN_BeginCapture(int nChannel);
    public native static int LIVESCAN_GetFPRawData(int nChannel, byte[] pRawData);
    public native static int LIVESCAN_EndCapture(int nChannel);
    public native static int LIVESCAN_GetVersion();

    public native int FP_Begin();
    public native int FP_FeatureExtract(int cScannerType, int cFingerCode,
                                        byte[] pFingerImgBuf, byte[] pFeatureData);
    public native int FP_FeatureMatchPC(byte[] pFeatureData1, byte[] pFeatureData2,
                                        int[] pfSimilarity);
    public native int FP_End();

    private static final String TAG = "ZAZAPI";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int TARGET_VID = 0x2109;
    private static final int TARGET_PID = 0x7638;

    private UsbManager mDevManager = null;
    private PendingIntent permissionIntent = null;
    private UsbInterface intf = null;
    private UsbDeviceConnection connection = null;
    private UsbDevice device = null;
    public volatile int isusbfinshed = 0;

    public int opendevice(Context env) {
        device = null;
        isusbfinshed = 0;
        int iret = getrwusbdevices(env);
        if (iret == 2) {
            Log.e(TAG, "USB device VID=0x2109 PID=0x7638 not found");
            return -1;
        }
        if (!WaitForInterfaces()) {
            Log.e(TAG, "USB permission denied or timeout");
            return -1;
        }
        int fd = OpenDeviceInterfaces();
        if (fd == -1) {
            Log.e(TAG, "OpenDeviceInterfaces failed");
            return -2;
        }
        int status = LIVESCAN_Handle(fd);
        if (status == 0) {
            Log.i(TAG, "Device opened successfully");
            return 1;
        }
        Log.e(TAG, "LIVESCAN_Handle failed: " + status);
        return -3;
    }

    public int getrwusbdevices(Context env) {
        mDevManager = (UsbManager) env.getSystemService(Context.USB_SERVICE);

        // FLAG_MUTABLE required on Android 12+ (API 31+)
        int flags = Build.VERSION.SDK_INT >= 31
                ? PendingIntent.FLAG_MUTABLE
                : 0;
        permissionIntent = PendingIntent.getBroadcast(env, 0, new Intent(ACTION_USB_PERMISSION), flags);
        env.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));

        HashMap<String, UsbDevice> deviceList = mDevManager.getDeviceList();
        for (UsbDevice tdevice : deviceList.values()) {
            Log.i(TAG, "Found USB: VID=" + Integer.toHexString(tdevice.getVendorId())
                    + " PID=" + Integer.toHexString(tdevice.getProductId()));
            if (tdevice.getVendorId() == TARGET_VID && tdevice.getProductId() == TARGET_PID) {
                Log.i(TAG, "Target fingerprint device found, requesting permission");
                mDevManager.requestPermission(tdevice, permissionIntent);
                return 1;
            }
        }
        return 2;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            try { context.unregisterReceiver(this); } catch (Exception ignored) {}
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && dev != null) {
                    Log.i(TAG, "USB permission granted");
                    device = dev;
                    isusbfinshed = 1;
                } else {
                    Log.e(TAG, "USB permission denied");
                    isusbfinshed = 2;
                }
            }
        }
    };

    // Waits up to 15 seconds for USB permission response
    public boolean WaitForInterfaces() {
        int timeout = 1500; // 1500 x 10ms = 15 seconds
        while (timeout-- > 0) {
            if (isusbfinshed == 1 && device != null) return true;
            if (isusbfinshed == 2) return false;
            try { Thread.sleep(10); } catch (InterruptedException e) { return false; }
        }
        Log.e(TAG, "WaitForInterfaces timed out");
        return false;
    }

    public int OpenDeviceInterfaces() {
        if (device == null) return -1;
        connection = mDevManager.openDevice(device);
        if (connection == null) return -1;
        if (!connection.claimInterface(device.getInterface(0), true)) return -1;
        return connection.getFileDescriptor();
    }
}
