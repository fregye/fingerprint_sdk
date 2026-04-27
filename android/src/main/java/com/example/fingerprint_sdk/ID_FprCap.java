package com.example.fingerprint_sdk;

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

public class ID_FprCap{
	//by:zhiang
	//by:sunafterglow
	//by:20180402
 	static
	{
	 	System.loadLibrary("ID_FprCap");
	}
	public int LIVESCAN_SUCCESS = 1;
	public int LIVESCAN_E_NOERR = 1;    //没有错误
	public int LIVESCAN_E_PARAM = -1;    //参数错误
	public int LIVESCAN_E_MEMORY = -2;    //内存分配失败，没有分配到足够的内存
	public int LIVESCAN_E_FUNC_NOT_EXIST = -3;    //功能未实现
	public int LIVESCAN_E_NO_DEVICE = -4;    //设备不存在
	public int LIVESCAN_E_DEV_NOT_INIT = -5;    //设备未初始化
	public int LIVESCAN_E_ILLEGAL = -6;    //非法错误号
	public int LIVESCAN_E_UNDEFINED = -9;    //其它错误
	public int LIVESCAN_E_TRANS = -101;    //其它错误(和设备交互失败)
	public int LIVESCAN_E_BEGINCAP = -103;    //其它错误(未准备采集)
	public int LIVESCAN_E_ENDCAP = -104;    //其它错误(未结束采集)
	public int LIVESCAN_E_THEAD = -105;

	public native static int  LIVESCAN_Handle(int handle);
	public native static int  LIVESCAN_Init();
	public native static int  LIVESCAN_Close();
	public native static int  LIVESCAN_GetChannelCount();	
	public native static int  LIVESCAN_SetBright(int nChannel,int nBright);
	public native static int  LIVESCAN_SetContrast(int nChannel,int nContrast);
	public native static int  LIVESCAN_GetBright(int nChannel,int[] pnBright);
	public native static int  LIVESCAN_GetContrast(int nChannel,int[] pnContrast);
	public native static int  LIVESCAN_GetMaxImageSize(int nChannel,int[] pnWidth, int[] pnHeight);
	public native static int  LIVESCAN_GetCaptWindow(int nChannel,int[] pnOriginX,int[] pnOriginY,int[] pnWidth,int[] pnHeight);
	public native static int  LIVESCAN_SetCaptWindow(int nChannel,int nOriginX,int nOriginY,int nWidth,int nHeight);
	public native static int  LIVESCAN_Setup();
	public native static int  LIVESCAN_BeginCapture(int nChannel);
	public native static int  LIVESCAN_GetFPRawData(int nChannel,byte[] pRawData);
	public native static int  LIVESCAN_GetFPBmpData(int nChannel,byte[] pBmpData);
	public native static int  LIVESCAN_EndCapture(int nChannel);
	public native static int  LIVESCAN_IsSupportSetup();
	public native static int  LIVESCAN_GetVersion();
	public native static int  LIVESCAN_GetDesc(byte[] pszDesc);
	public native static int  LIVESCAN_GetErrorInfo(int nErrorNo,byte[] pszErrorInfo);
	public native static int  LIVESCAN_SetBufferEmpty(byte[] pImageData, long imageLength);




	public native int FP_GetVersion(byte[] code );
	public native int  FP_Begin();
	//3	1枚指纹图像特征提取
	public native int  FP_FeatureExtract(int  cScannerType, //输入参数 指纹采集器类型代码
										 int cFingerCode,	 // 指位代码。输入参数。
										 byte[] pFingerImgBuf,//输入参数 指纹图像数据指针 图像格式为256(width)*360(height) 8位灰度图像
										 byte[] pFeatureData); //输出参数 指纹图像特征数据指针

	//5	对2个指纹特征数据进行比对，得到匹配相似度值
	public native int  FP_FeatureMatch(byte[] pFeatureData1,	//输入参数 指纹特征数据1
									   byte[] pFeatureData2,	//输入参数 指纹特征数据2
									   int[] pfSimilarity);			//输出参数 匹配相似度值0.00-1.00
	public native int  FP_FeatureMatchPC(byte[] pFeatureData1,	//输入参数 指纹特征数据1
									   byte[] pFeatureData2,	//输入参数 指纹特征数据2
									   int[] pfSimilarity);			//输出参数 匹配相似度值0.00-1.00

	//6	对指纹图像和指纹特征进行比对，得到匹配相似度值
	public native int  FP_ImageMatch(byte[] pFingerImgBuf,	//输入参数 指纹图像数据指针 图像格式为256(width)*360(height)
									 byte[] pFeatureData,	//输入参数 指纹特征数据
									 int[] pfSimilarity);			//输出参数 指纹图像特征数据指针
	//7	对指纹图像数据进行压缩
	public native int  FP_Compress(  int cScannerType,	//输入参数 指纹采集器类型代码
									 int cEnrolResult,
									 int cFingerCode,		 // 指位代码。输入参数。
									 byte[] pFingerImgBuf,   //输入参数 指纹图像数据指针 图像格式为256(width)*360(height)
									 int nCompressRatio,				//输入参数 压缩倍数
									 byte[] pCompressedImgBuf,//输出参数 指纹图像压缩数据指针 空间为20K字节
									 byte[] strBuf);		//输出参数 返回-4时的错误信息
	//8	对指纹图像压缩数据进行复现
	public native int  FP_Decompress (byte[] pCompressedImgBuf,//输入参数 指纹图像压缩数据指针 已分配好20K字节空间
									  byte[] pFingerImgBuf,	//输入参数 指纹图像数据指针 大小256*360 字节
									  byte[] strBuf);		//输出参数 返回-4时的错误信息
	//9	获取指纹图像的质量值
	public native int  FP_GetQualityScore (byte[] pFingerImgBuf,//输入参数 指纹图像数据指针 图像格式为256(width)*360(height)
										   int[] pnScore);//输出参数 图像质量值 00H - 64H

	//10 生成"未注册"指纹特征数据
	public native int  FP_GenFeatureFromEmpty1(
			int   cScannerType,  //输入参数 指纹采集器类型代码
			int	cFingerCode,	 // 指位代码。输入参数。
			byte[] pFeatureData  //输出参数 已由调用者分配512字节空间
	);
	//10 生成"未注册"指纹特征数据
	public native int  FP_GenFeatureFromEmpty2(int cFingerCode, byte[] pFeatureData);//输出参数 已由调用者分配512字节空间
	//11 生成"未注册"指纹图像压缩数据
	public native int  FP_GenCompressDataFromEmpty(byte[] pCompressedImgBuf);//输出参数 已由调用者分配20K字节空间
	//12 结束操作
	public native int  FP_End();


	private String TAG = "ZAZAPI";
	private   int isonline = 0;
	private int isbus = 0;

	public int opendevice(Context env)
	{
		int rootqx;
		int status = -1;
		isusbfinshed = 3;
		rootqx = 0;
		if( 0 == rootqx)
		{
			Log.i(TAG,"use by not root ");
			device = null;
			isusbfinshed  = 0;
			int fd = 0;
			isusbfinshed = getrwusbdevices(env);
			Log.i(TAG,"waiting user put root ");
			if(WaitForInterfaces() == false)  {
				status = -1;
			}
			else {
				fd = OpenDeviceInterfaces();
				if (fd == -1) {
					status = -2;
				}
				else {
					status = LIVESCAN_Handle(fd);
					if (status == 0) {
						isonline = 1;
						status = 1;
					}
					else{
						status = -3;
					}

				}
			}
		}
		return status;
	}



	private UsbManager mDevManager = null;
	private PendingIntent permissionIntent = null;
	private UsbInterface intf = null;
	private UsbDeviceConnection connection = null;
	private UsbDevice device = null;
	public int isusbfinshed = 0;
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	public int getrwusbdevices(Context env) {

		mDevManager = ((UsbManager) env.getSystemService(Context.USB_SERVICE));
		permissionIntent = PendingIntent.getBroadcast(env, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		env.registerReceiver(mUsbReceiver, filter);
		//this.registerReceiver(mUsbReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
		HashMap<String, UsbDevice> deviceList = mDevManager.getDeviceList();
		if (true) Log.e(TAG, "news:" + "mDevManager");


		for (UsbDevice tdevice : deviceList.values()) {
			Log.i(TAG,	tdevice.getDeviceName() + " "+ Integer.toHexString(tdevice.getVendorId()) + " "
					+ Integer.toHexString(tdevice.getProductId()));
			if (tdevice.getVendorId() == 0x2109 && (tdevice.getProductId() == 0x7638))
			{
				Log.e(TAG, " 指纹设备准备好了 ");
				mDevManager.requestPermission(tdevice, permissionIntent);
				return 1;
			}
		}
		Log.e(TAG, "news:" + "mDevManager  end");
		return 2;
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			context.unregisterReceiver(mUsbReceiver);
			isusbfinshed = 0;
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (context) {
					device = (UsbDevice) intent	.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					Log.e("BroadcastReceiver","3333");
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
							if (true) Log.e(TAG, "Authorize permission " + device);
							isusbfinshed = 1;
						}
					}
					else {
						if (true) Log.e(TAG, "permission denied for device " + device);
						device=null;
						isusbfinshed = 2;

					}
				}
			}
		}
	};

	private void Sleep(int times)
	{
		try {
			Thread.sleep(times);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean WaitForInterfaces() {

		while (device==null || isusbfinshed == 0) {
			Sleep(10);
			if(isusbfinshed == 2)break;
			if(isusbfinshed == 3)break;
		}
		if(isusbfinshed == 2)
			return false;
		if(isusbfinshed == 3)
			return false;
		return true;
	}

	public int OpenDeviceInterfaces() {
		UsbDevice mDevice = device;
		Log.d(TAG, "setDevice " + mDevice);
		int fd = -1;
		if (mDevice == null) return -1;
		connection = mDevManager.openDevice(mDevice);
		if (!connection.claimInterface(mDevice.getInterface(0), true)) return -1;

		if (mDevice.getInterfaceCount() < 1) return -1;
		intf = mDevice.getInterface(0);

		if (intf.getEndpointCount() == 0) 	return -1;

		if ((connection != null)) {
			if (true) Log.e(TAG, "open connection success!");
			fd = connection.getFileDescriptor();
			return fd;
		}
		else {
			if (true) Log.e(TAG, "finger device open connection FAIL");
			return -1;
		}
	}
	public int getimageArea(byte[] pImage, int w, int h)
	{
		int temp = 0;
		int sorce = 0;
		int hkip = (h-10)/10;
		int wkip = (w-10)/10;
		int zlcun = 0;
		byte tem = 0;
		for (int i = 10; i < h;i+= hkip)
		{
			tem++;
			if(tem>10)break;
			for (int j = 10; j < w-wkip; j += wkip)
			{
				temp = backcount(pImage,i*w+j,w,h);
				if(temp>50)
					sorce += 1;

				if(j>120 & j<150)
				{
					if(temp>50)
						zlcun++;
				}
			}
		}
		if(zlcun>7)
			return sorce;
		else
			return 0;
	}

	public  int backcount(byte[]  pImage, int off, int w, int h)
	{
		int cnt = 0;
		for (int i = 0; i < 15; i++)
		{
			for (int j = 0; j < 15; j++)
			{
				if(off+w*i+j>(w*h))
					break;
				if ((pImage[off + w * i + j]&0xff) < 240)
					cnt++;
				// pImage [off + w * i + j]=0;

				if (cnt > 50)
					break;
			}
		}

		return cnt;
	}
}
