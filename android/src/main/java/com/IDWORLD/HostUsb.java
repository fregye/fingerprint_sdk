package com.IDWORLD;

import java.util.HashMap;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

public class HostUsb {
    private static final String TAG = "OpenHostUsb";
    private static final String ACTION_USB_PERMISSION = "com.IDWORLD.USB_PERMISSION";
    private Context context = null;
    private UsbManager mDevManager = null;
    private UsbInterface intf = null;
    private UsbDeviceConnection connection = null;
    private UsbDevice device = null;

    private int m_nEPOutSize = 2048;
    private int m_nEPInSize = 2048;
    private byte[] m_abyTransferBuf = new byte[2048];

    UsbEndpoint endpoint_IN = null;
    UsbEndpoint endpoint_OUT = null;
    UsbEndpoint endpoint_INT = null;
    UsbEndpoint curEndpoint = null;

    public HostUsb() {}

    public boolean AuthorizeDevice(Context paramContext, int VID, int PID) {
        context = paramContext;
        mDevManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = mDevManager.getDeviceList();

        for (UsbDevice tdevice : deviceList.values()) {
            if (tdevice.getVendorId() == VID && tdevice.getProductId() == PID) {
                if (mDevManager.hasPermission(tdevice)) {
                    // Permission already granted — set device directly, no dialog needed
                    device = tdevice;
                    Log.i(TAG, "USB permission already granted for VID=" + Integer.toHexString(VID));
                    return true;
                } else {
                    // Request permission — FLAG_MUTABLE required on Android 12+
                    int flags = Build.VERSION.SDK_INT >= 31
                            ? PendingIntent.FLAG_MUTABLE
                            : 0;
                    PendingIntent permissionIntent = PendingIntent.getBroadcast(
                            context, 0, new Intent(ACTION_USB_PERMISSION), flags);
                    context.registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
                    mDevManager.requestPermission(tdevice, permissionIntent);
                    Log.i(TAG, "USB permission requested for VID=" + Integer.toHexString(VID));
                    return true;
                }
            }
        }
        Log.e(TAG, "USB device VID=" + Integer.toHexString(VID) + " PID=" + Integer.toHexString(PID) + " not found");
        return false;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                try { context.unregisterReceiver(this); } catch (Exception ignored) {}
                UsbDevice dev = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && dev != null) {
                    Log.i(TAG, "USB permission granted");
                    device = dev;
                } else {
                    Log.e(TAG, "USB permission denied");
                }
            }
        }
    };

    // Waits up to 15 seconds for permission dialog response
    public boolean WaitForInterfaces() {
        int timeout = 1500; // 1500 x 10ms = 15 seconds
        while (device == null && timeout-- > 0) {
            try { Thread.sleep(10); } catch (InterruptedException e) { return false; }
        }
        if (device == null) {
            Log.e(TAG, "WaitForInterfaces timed out");
            return false;
        }
        return true;
    }

    public int OpenDeviceInterfaces() {
        UsbDevice mDevice = device;
        if (mDevice == null) { Log.e(TAG, "OpenDeviceInterfaces: device is null"); return -1; }
        connection = mDevManager.openDevice(mDevice);
        if (connection == null) { Log.e(TAG, "OpenDeviceInterfaces: openDevice returned null"); return -1; }
        if (mDevice.getInterfaceCount() < 1) { Log.e(TAG, "OpenDeviceInterfaces: no interfaces"); return -1; }
        intf = mDevice.getInterface(0);
        if (!connection.claimInterface(intf, true)) { Log.e(TAG, "OpenDeviceInterfaces: claimInterface failed"); return -1; }
        Log.i(TAG, "OpenDeviceInterfaces: endpoints=" + intf.getEndpointCount());
        for (int i = 0; i < intf.getEndpointCount(); i++) {
            if (intf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
                    endpoint_IN = intf.getEndpoint(i);
                else if (intf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT)
                    endpoint_OUT = intf.getEndpoint(i);
            } else if (intf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                endpoint_INT = intf.getEndpoint(i);
            }
        }
        curEndpoint = intf.getEndpoint(0);
        int fd = connection.getFileDescriptor();
        Log.i(TAG, "OpenDeviceInterfaces: fd=" + fd);
        return fd;
    }

    public void CloseDeviceInterface() {
        if (connection != null) {
            connection.releaseInterface(intf);
            connection.close();
        }
    }

    public boolean USBBulkSend(byte[] pBuf, int nLen, int nTimeOut) {
        int i, n, r, w_nRet;
        n = nLen / m_nEPOutSize;
        r = nLen % m_nEPOutSize;
        for (i = 0; i < n; i++) {
            System.arraycopy(pBuf, i * m_nEPOutSize, m_abyTransferBuf, 0, m_nEPOutSize);
            w_nRet = connection.bulkTransfer(endpoint_OUT, m_abyTransferBuf, m_nEPOutSize, nTimeOut);
            if (w_nRet != m_nEPOutSize) return false;
        }
        if (r > 0) {
            System.arraycopy(pBuf, i * m_nEPOutSize, m_abyTransferBuf, 0, r);
            w_nRet = connection.bulkTransfer(endpoint_OUT, m_abyTransferBuf, r, nTimeOut);
            if (w_nRet != r) return false;
        }
        return true;
    }

    public boolean USBBulkReceive(byte[] pBuf, int nLen, int nTimeOut) {
        int i, n, r, w_nRet;
        n = nLen / m_nEPInSize;
        r = nLen % m_nEPInSize;
        for (i = 0; i < n; i++) {
            w_nRet = connection.bulkTransfer(endpoint_IN, m_abyTransferBuf, m_nEPInSize, nTimeOut);
            if (w_nRet != m_nEPInSize) return false;
            System.arraycopy(m_abyTransferBuf, 0, pBuf, i * m_nEPInSize, m_nEPInSize);
        }
        if (r > 0) {
            w_nRet = connection.bulkTransfer(endpoint_IN, m_abyTransferBuf, r, nTimeOut);
            if (w_nRet != r) return false;
            System.arraycopy(m_abyTransferBuf, 0, pBuf, i * m_nEPInSize, r);
        }
        return true;
    }
}
