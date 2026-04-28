package com.IDWORLD;

import android.content.Context;
import android.util.Log;

public class LAPI {
    static final String TAG = "LAPI";
    private static final boolean D = false;
    public static final int VID = 0x0483;
    public static final int PID = 0x5710;
    private static HostUsb m_usbHost = null;
    private static int m_hUSB = 0;
    public static final int MSG_OPEN_DEVICE = 0x10;
    public static final int MSG_CLOSE_DEVICE = 0x11;
    public static final int MSG_BULK_TRANS_IN = 0x12;
    public static final int MSG_BULK_TRANS_OUT = 0x13;
    public static final int WIDTH  = 256;
    public static final int HEIGHT  = 360;
    public static final int IMAGE_SIZE = WIDTH*HEIGHT;
    public static final int FPINFO_STD_MAX_SIZE = 1024;
    public static final int DEF_QUALITY_SCORE = 30;
    public static final int DEF_MATCH_SCORE = 45;
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    private static Context m_content = null;

    static {
        try {
            System.loadLibrary("biofp_e_lapi");
        } catch(UnsatisfiedLinkError e) {
            if (D) writeLog("LAPI","biofp_e_lapi load fail");
        }
    }

    public LAPI(Context a) {
        m_content = a;
    }

    private static void writeLog(String fileName, String content) {
        Log.e(TAG, content);
    }

    public int getrwusbdevices() {
        m_usbHost = new HostUsb();
        if (!m_usbHost.AuthorizeDevice(m_content, VID, PID)) {
            m_usbHost = null;
            return 0;
        }
        if (m_usbHost.WaitForInterfaces() == false) {
            m_usbHost = null;
            return 0;
        }
        m_hUSB = m_usbHost.OpenDeviceInterfaces();
        if (m_hUSB <= 0) {
            m_usbHost = null;
            return 0;
        }
        return m_hUSB;
    }

    private static int CallBack(int message, int notify, int param, Object data) {
        switch (message) {
            case MSG_OPEN_DEVICE:
                Log.i(TAG, "CallBack MSG_OPEN_DEVICE");
                m_usbHost = new HostUsb();
                if (!m_usbHost.AuthorizeDevice(m_content, VID, PID)) {
                    Log.e(TAG, "CallBack: AuthorizeDevice failed");
                    m_usbHost = null;
                    return 0;
                }
                if (m_usbHost.WaitForInterfaces() == false) {
                    Log.e(TAG, "CallBack: WaitForInterfaces failed");
                    m_usbHost = null;
                    return 0;
                }
                m_hUSB = m_usbHost.OpenDeviceInterfaces();
                Log.i(TAG, "CallBack: OpenDeviceInterfaces returned " + m_hUSB);
                if (m_hUSB <= 0) {
                    m_usbHost = null;
                    return 0;
                }
                return m_hUSB;
            case MSG_CLOSE_DEVICE:
                if (m_usbHost != null) {
                    m_usbHost.CloseDeviceInterface();
                    m_hUSB = -1;
                    m_usbHost = null;
                }
                return 1;
            case MSG_BULK_TRANS_IN:
                m_usbHost.USBBulkReceive((byte[])data, notify, param);
                return 1;
            case MSG_BULK_TRANS_OUT:
                m_usbHost.USBBulkSend((byte[])data, notify, param);
                return 1;
        }
        return 0;
    }

    public native String GetVersion();
    public native long OpenDevice();
    public native int CloseDevice(long device);
    public native int GetImage(long device, byte[] image);
    public native int Calibration(long device);
    public native int IsPressFinger(long device, byte[] image);
    public native int CreateTemplate(long device, byte[] image, byte[] itemplate);
    public native int GetImageQuality(long device, byte[] image);
    public native int CompareTemplates(long device, byte[] itemplateToMatch, byte[] itemplateToMatched);
}
