package com.example.fingerprint_sdk;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

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
    public static final int FPINFO_STD_MAX_SIZE = 1024;	//512
    public static final int DEF_QUALITY_SCORE = 30;
    public static final int DEF_MATCH_SCORE = 45;
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    private static Activity m_content = null;


    static
	{
		try{
			System.loadLibrary("biofp_e_lapi");
		}
		catch(UnsatisfiedLinkError e) {
			if (D)	writeLog("LAPI","biofp_e_lapi load fail");
		}
	}


	public LAPI(Activity a) {
		m_content = a;
	}
    private  static void writeLog(String fileName,String content) {
        Log.e(TAG,content);
    }

	public int getrwusbdevices()
    {
		m_usbHost = new HostUsb ();
		if (!m_usbHost.AuthorizeDevice(m_content,VID,PID)) {
			m_usbHost = null;
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- Authorize fail");
			return 0;
		}
		if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- Authorize ok");
		if(m_usbHost.WaitForInterfaces() == false){
			m_usbHost = null;
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- WaitForInterfaces fail");
			return 0;
		}
		if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- WaitForInterfaces ok");
		m_hUSB = m_usbHost.OpenDeviceInterfaces();
		if (m_hUSB <= 0) {
			m_usbHost = null;
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- OpenDeviceInterfaces fail, m_hUSB : " +m_hUSB);
			return 0;
		}
		if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- OpenDeviceInterfaces ok, m_hUSB : "+m_hUSB);
		return m_hUSB;
	}

	private static int CallBack (int message, int notify, int param, Object data)
	{
		switch (message) {
		case MSG_OPEN_DEVICE:
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- start -------------------------");
			m_usbHost = new HostUsb ();
			if (!m_usbHost.AuthorizeDevice(m_content,VID,PID)) { 
				m_usbHost = null;
				if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- Authorize fail");
				return 0;
			}
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- Authorize ok");
			if(m_usbHost.WaitForInterfaces() == false){
				m_usbHost = null;
				if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- WaitForInterfaces fail");
				return 0;
			}
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- WaitForInterfaces ok");
		    m_hUSB = m_usbHost.OpenDeviceInterfaces();
			if (m_hUSB <= 0) {
				m_usbHost = null;
				if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- OpenDeviceInterfaces fail, m_hUSB : " +m_hUSB);
				return 0;
			}
			if (D)	writeLog("","CallBack MSG_OPEN_DEVICE ----------------------- OpenDeviceInterfaces ok, m_hUSB : "+m_hUSB);
			return m_hUSB;
		case MSG_CLOSE_DEVICE:
			if (m_usbHost != null) {
				m_usbHost.CloseDeviceInterface();
				m_hUSB = -1;
				m_usbHost = null;
			}
			return 1;
		case MSG_BULK_TRANS_IN:
			m_usbHost.USBBulkReceive((byte[])data,notify,param);
			return 1;
		case MSG_BULK_TRANS_OUT:
            m_usbHost.USBBulkSend((byte[])data,notify,param);
			return 1;
		}
		return 0;
	}

    public native String GetVersion();
    public native long OpenDevice();//If successful, return handle of device, else 0.
    public native int CloseDevice(long device);// If successful, return 1, else 0
    public native int GetImage(long device, byte[] image);// If successful, return 1, else 0
    public native int Calibration(long device);//If successful, return 1, else 0
    public native int IsPressFinger(long device,byte[] image);//return percent value measured finger-print on sensor(0~100).
    public native int CreateTemplate(long device,byte[] image, byte[] itemplate);//If this function successes, return none-zero, else 0.
    public native int GetImageQuality(long device,byte[] image);//return quality value(0~100) of fingerprint raw image.
    public native int CompareTemplates(long device,byte[] itemplateToMatch, byte[] itemplateToMatched);//return similar match score(0~100) of two finger-print templates.
	//------------------------------------------------------------------------------------------------//


}
