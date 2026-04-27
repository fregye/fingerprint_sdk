package com.example.fingerprint_sdk;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

public class Interface {
    ArrayList<byte[]> fpchars ;
    LAPI c6 ;
    public Interface(Activity a) {
        c6 = new LAPI(a);
        fpchars = new ArrayList<byte[]>();
    }
    private int isbus = 0;
    private int isonline = 0;
    public  String F_GetVersion()
    {   if(isonline!=1)return null;
        if(isbus!=0)return null;
        isbus = 1;
        String ret = c6.GetVersion();
        isbus = 0;
        return ret;
    }


    public  long F_OpenDevice()
    {
        if(isbus!=0)return -101;
        isbus = 1;
        long vals = 0xffffffff & 0xffffffffL;
        long ret =  c6.OpenDevice();
        if(ret == vals) {
            isbus = 0;
            return 0;
        }
        if(ret != 0)
            isonline = 1;
        isbus = 0;
        return ret;
    }

    public int F_CloseDevice(long device)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.CloseDevice(device);
        isbus = 0;
        isonline=0;
        return ret;
    }
    public int F_GetImage(long device, byte[] image)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.GetImage(device,image);
        isbus = 0;
        return ret;
    }
    public int F_Calibration(long device)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.Calibration(device);
        isbus = 0;
        return ret;
    }
    public int F_IsPressFinger(long device,byte[] image)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.IsPressFinger(device,image);
        isbus = 0;
        return ret;
    }
    public int F_CreateTemplate(long device,byte[] image, byte[] itemplate)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.CreateTemplate(device,image,itemplate);
        isbus = 0;
        return ret;
    }
    public int F_GetImageQuality(long device,byte[] image)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.GetImageQuality(device,image);
        isbus = 0;
        return ret;
    }
    public int F_CompareTemplates(long device,byte[] itemplateToMatch, byte[] itemplateToMatched)
    {
        if(isonline!=1)return -100;
        if(isbus!=0)return -101;
        isbus = 1;
        int ret = c6.CompareTemplates(device,itemplateToMatch,itemplateToMatched);
        isbus = 0;
        return ret;
    }

    public static final String byte2hex(byte b[], int nLen) {
        if (b == null) {
            throw new IllegalArgumentException("Argument b ( byte array ) is null! ");
        }
        String hs = "";
        String stmp = "";
        int len, k;
        k = 0;
        len = nLen;
        if(len > 100)	len = 100;
        for (int n = 0; n < len; n++) {
            stmp = Integer.toHexString(b[n] & 0xff);
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
            k++;
            if(k == 4){
                k = 0;
                hs = hs + " ";
            }
        }
        return hs.toUpperCase();
    }

    public int insert(byte[] fpchar)
    {
        fpchars.add(fpchar);
        return 1;
    }
    public int deletetable()
    {
        fpchars.clear();
        return 1;
    }


    public ArrayList<byte[]> searchtable()
    {

        return fpchars;

    }

    public int getSize(){
        return fpchars.size();
    }
}
