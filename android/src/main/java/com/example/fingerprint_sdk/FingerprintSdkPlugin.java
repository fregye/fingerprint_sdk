package com.example.fingerprint_sdk;

import androidx.annotation.NonNull;
import android.content.Context;
import com.zaz.ID_FprCap;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FingerprintSdkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Context context;
    private ID_FprCap fprCap;
    private boolean deviceOpen = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "fingerprint_sdk");
        channel.setMethodCallHandler(this);
        context = binding.getApplicationContext();
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
            // opendevice() handles Android USB permission + passes fd to native via LIVESCAN_Handle()
            int ret = fprCap.opendevice(context);
            if (ret == 1) {
                deviceOpen = true;
                result.success(true);
            } else {
                deviceOpen = false;
                result.success(false);
            }
        }).start();
    }

    private void closeDevice(Result result) {
        new Thread(() -> {
            ID_FprCap.LIVESCAN_Close();
            deviceOpen = false;
            result.success(true);
        }).start();
    }

    private void getImage(Result result) {
        if (!deviceOpen) {
            result.error("NO_DEVICE", "Device not open", null);
            return;
        }
        new Thread(() -> {
            byte[] image = new byte[256 * 360];
            ID_FprCap.LIVESCAN_BeginCapture(0);
            int ret = ID_FprCap.LIVESCAN_GetFPRawData(0, image);
            ID_FprCap.LIVESCAN_EndCapture(0);
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
