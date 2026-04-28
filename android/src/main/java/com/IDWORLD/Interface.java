package com.IDWORLD;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

public class Interface {
    ArrayList<byte[]> fpchars;
    LAPI c6;

    public Interface(Context a) {
        c6 = new LAPI(a);
        fpchars = new ArrayList<byte[]>();
    }

    private int isbus = 0;
    private int isonline = 0;

    public String F_GetVersion() {
        if (isonline != 1) return null;
        if (isbus != 0) return null;
        isbus = 1;
        String ret = c6.GetVersion();
        isbus = 0;
        return ret;
    }

    public long F_OpenDevice() {
        if (isbus != 0) return -101;
        isbus = 1;
        long vals = 0xffffffff & 0xffffffffL;
        long ret = c6.OpenDevice();
        android.util.Log.i("IDWORLD_Interface", "F_OpenDevice: raw ret=" + ret + " (0x" + Long.toHexString(ret) + ")");
        if (ret == vals) {
            isbus = 0;
            android.util.Log.e("IDWORLD_Interface", "F_OpenDevice: OpenDevice returned 0xFFFFFFFF (-1), treating as 0");
            return 0;
        }
        if (ret != 0) isonline = 1;
        isbus = 0;
        return ret;
    }

    public int F_CloseDevice(long device) {
        if (isonline != 1) return -100;
        if (isbus != 0) return -101;
        isbus = 1;
        int ret = c6.CloseDevice(device);
        isbus = 0;
        isonline = 0;
        return ret;
    }

    public int F_GetImage(long device, byte[] image) {
        if (isonline != 1) return -100;
        if (isbus != 0) return -101;
        isbus = 1;
        int ret = c6.GetImage(device, image);
        isbus = 0;
        return ret;
    }

    public int F_CreateTemplate(long device, byte[] image, byte[] itemplate) {
        if (isonline != 1) return -100;
        if (isbus != 0) return -101;
        isbus = 1;
        int ret = c6.CreateTemplate(device, image, itemplate);
        isbus = 0;
        return ret;
    }

    public int F_GetImageQuality(long device, byte[] image) {
        if (isonline != 1) return -100;
        if (isbus != 0) return -101;
        isbus = 1;
        int ret = c6.GetImageQuality(device, image);
        isbus = 0;
        return ret;
    }

    public int F_CompareTemplates(long device, byte[] itemplateToMatch, byte[] itemplateToMatched) {
        if (isonline != 1) return -100;
        if (isbus != 0) return -101;
        isbus = 1;
        int ret = c6.CompareTemplates(device, itemplateToMatch, itemplateToMatched);
        isbus = 0;
        return ret;
    }

    public int insert(byte[] fpchar) {
        fpchars.add(fpchar);
        return 1;
    }

    public int deletetable() {
        fpchars.clear();
        return 1;
    }

    public ArrayList<byte[]> searchtable() {
        return fpchars;
    }

    public int getSize() {
        return fpchars.size();
    }
}
