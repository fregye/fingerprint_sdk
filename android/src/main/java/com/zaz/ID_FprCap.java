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

    private String TAG = "ZAZAPI";
    private int isonline = 0;
    private int isbus = 0;

    private UsbManager mDevManager = null;
    private PendingIntent permissionIntent = null;
    private UsbInterface intf = null;
    private UsbDeviceConnection connection = null;
    private UsbDevice device = null;
    public int isusbfinshed = 0;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public int opendevice(Context env) {
        device = null;
        isusbfinshed = 0;
        int fd = 0;
        isusbfinshed = getrwusbdevices(env);
        if (WaitForInterfaces() == false) return -1;
        fd = OpenDeviceInterfaces();
        if (fd == -1) return -2;
        int status = LIVESCAN_Handle(fd);
        if (status == 0) { isonline = 1; return 1; }
        return -3;
    }

    public int getrwusbdevices(Context env) {
        mDevManager = ((UsbManager) env.getSystemService(Context.USB_SERVICE));
        permissionIntent = PendingIntent.getBroadcast(env, 0, new Intent(ACTION_USB_PERMISSION), 0);
        env.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        HashMap<String, UsbDevice> deviceList = mDevManager.getDeviceList();
        for (UsbDevice tdevice : deviceList.values()) {
            if (tdevice.getVendorId() == 0x2109 && tdevice.getProductId() == 0x7638) {
                mDevManager.requestPermission(tdevice, permissionIntent);
                return 1;
            }
        }
        return 2;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(mUsbReceiver);
            isusbfinshed = 0;
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                synchronized (context) {
                    device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) isusbfinshed = 1;
                    } else {
                        device = null;
                        isusbfinshed = 2;
                    }
                }
            }
        }
    };

    public boolean WaitForInterfaces() {
        while (device == null || isusbfinshed == 0) {
            try { Thread.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
            if (isusbfinshed == 2 || isusbfinshed == 3) return false;
        }
        return true;
    }

    public int OpenDeviceInterfaces() {
        if (device == null) return -1;
        connection = mDevManager.openDevice(device);
        if (!connection.claimInterface(device.getInterface(0), true)) return -1;
        if (connection != null) return connection.getFileDescriptor();
        return -1;
    }
}
