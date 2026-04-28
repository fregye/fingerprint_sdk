package com.example.fingerprint_sdk;

import androidx.annotation.NonNull;
import android.content.Context;
import com.IDWORLD.Interface;
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
    private long deviceHandle = 0;
    private boolean deviceOpen = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "fingerprint_sdk");
        channel.setMethodCallHandler(this);
        Context context = binding.getApplicationContext();
        lapiInterface = new Interface(context);
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
            try {
                long handle = lapiInterface.F_OpenDevice();
                if (handle != 0) {
                    deviceHandle = handle;
                    deviceOpen = true;
                    result.success(true);
                } else {
                    deviceOpen = false;
                    result.success(false);
                }
            } catch (Exception e) {
                deviceOpen = false;
                result.error("OPEN_FAILED", e.getMessage(), null);
            }
        }).start();
    }

    private void closeDevice(Result result) {
        new Thread(() -> {
            lapiInterface.F_CloseDevice(deviceHandle);
            deviceOpen = false;
            deviceHandle = 0;
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
            int ret = lapiInterface.F_GetImage(deviceHandle, image);
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
