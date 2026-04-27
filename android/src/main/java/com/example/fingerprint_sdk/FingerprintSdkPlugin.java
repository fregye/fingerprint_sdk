package com.example.fingerprint_sdk;

import androidx.annotation.NonNull;
import android.content.Context;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FingerprintSdkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Interface lapiInterface;
    private ID_FprCap fprCap;
    private int devtype = 0;   // 1 = LAPI, 2 = ID_FprCap
    private long deviceHandle = 0;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "fingerprint_sdk");
        channel.setMethodCallHandler(this);
        Context context = binding.getApplicationContext();
        lapiInterface = new Interface(context);
        fprCap = new ID_FprCap();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "openDevice":  openDevice(result);  break;
            case "closeDevice": closeDevice(result); break;
            case "getImage":    getImage(result);    break;
            default: result.notImplemented();
        }
    }

    private void openDevice(Result result) {
        new Thread(() -> {
            long handle = lapiInterface.F_OpenDevice();
            if (handle > 0) {
                deviceHandle = handle;
                devtype = 1;
                result.success(true);
                return;
            }
            int ret = fprCap.LIVESCAN_Init();
            if (ret == 1) {
                devtype = 2;
                result.success(true);
                return;
            }
            devtype = 0;
            result.success(false);
        }).start();
    }

    private void closeDevice(Result result) {
        new Thread(() -> {
            if (devtype == 1) lapiInterface.F_CloseDevice(deviceHandle);
            else if (devtype == 2) fprCap.LIVESCAN_Close();
            devtype = 0;
            deviceHandle = 0;
            result.success(true);
        }).start();
    }

    private void getImage(Result result) {
        if (devtype == 0) {
            result.error("NO_DEVICE", "Device not open", null);
            return;
        }
        new Thread(() -> {
            byte[] image = new byte[256 * 360];
            int ret;
            if (devtype == 1) {
                ret = lapiInterface.F_GetImage(deviceHandle, image);
            } else {
                fprCap.LIVESCAN_BeginCapture(0);
                ret = fprCap.LIVESCAN_GetFPRawData(0, image);
                fprCap.LIVESCAN_EndCapture(0);
            }
            if (ret == 1) {
                result.success(image);
            } else {
                result.error("CAPTURE_FAILED", "ret=" + ret, null);
            }
        }).start();
    }

    @Override public void onDetachedFromEngine(@NonNull FlutterPluginBinding b) { channel.setMethodCallHandler(null); }
    @Override public void onAttachedToActivity(@NonNull ActivityPluginBinding b) {}
    @Override public void onDetachedFromActivityForConfigChanges() {}
    @Override public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding b) {}
    @Override public void onDetachedFromActivity() {}
}
