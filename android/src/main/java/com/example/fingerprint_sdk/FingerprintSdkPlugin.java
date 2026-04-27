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
import android.app.Activity;
import java.util.ArrayList;
import java.util.HashMap;

public class FingerprintSdkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Context context;
    private Activity activity;

    // SDK objects
    private Interface lapiInterface;
    private ID_FprCap fprCap;
    private int devtype = 0; // 1 = LAPI, 2 = ID_FprCap
    private long deviceHandle = 0;

    // In-memory template store
    private ArrayList<byte[]> templates = new ArrayList<>();
    private static final int MAX_TEMPLATES = 40;
    private static final int MATCH_THRESHOLD = 60;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "fingerprint_sdk");
        channel.setMethodCallHandler(this);
        context = binding.getApplicationContext();
        lapiInterface = new Interface(context);
        fprCap = new ID_FprCap(context);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "openDevice":
                openDevice(result);
                break;
            case "closeDevice":
                closeDevice(result);
                break;
            case "getImage":
                getImage(result);
                break;
            case "enrollFingerprint":
                enrollFingerprint(result);
                break;
            case "verifyFingerprint":
                verifyFingerprint(result);
                break;
            case "clearDatabase":
                templates.clear();
                result.success(true);
                break;
            case "getDatabaseCount":
                result.success(templates.size());
                break;
            case "getVersion":
                result.success(lapiInterface.F_GetVersion());
                break;
            default:
                result.notImplemented();
        }
    }

    private void openDevice(Result result) {
        new Thread(() -> {
            // Try LAPI first (zaz0c0 device)
            long handle = lapiInterface.F_OpenDevice();
            if (handle > 0) {
                deviceHandle = handle;
                devtype = 1;
                channel.invokeMethod("onDeviceStatus", "opened:lapi");
                result.success(true);
                return;
            }
            // Fall back to ID_FprCap
            int ret = fprCap.LIVESCAN_Init();
            if (ret == 1) {
                devtype = 2;
                channel.invokeMethod("onDeviceStatus", "opened:fpr");
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
        new Thread(() -> {
            byte[] image = new byte[256 * 360];
            int ret;
            if (devtype == 1) {
                ret = lapiInterface.F_GetImage(deviceHandle, image);
            } else if (devtype == 2) {
                fprCap.LIVESCAN_BeginCapture(0);
                ret = fprCap.LIVESCAN_GetFPRawData(0, image);
                fprCap.LIVESCAN_EndCapture(0);
            } else {
                result.error("NO_DEVICE", "Device not open", null);
                return;
            }
            if (ret == 1) {
                result.success(image); // returns raw byte array to Flutter
            } else {
                result.error("CAPTURE_FAILED", "Image capture returned: " + ret, null);
            }
        }).start();
    }

    private void enrollFingerprint(Result result) {
        if (templates.size() >= MAX_TEMPLATES) {
            result.error("DB_FULL", "Max 40 fingerprints stored", null);
            return;
        }
        new Thread(() -> {
            byte[] image = new byte[256 * 360];
            byte[] tmpl = new byte[512];
            int ret;
            if (devtype == 1) {
                ret = lapiInterface.F_GetImage(deviceHandle, image);
                if (ret != 1) { result.error("CAPTURE_FAILED", "ret=" + ret, null); return; }
                ret = lapiInterface.F_CreateTemplate(deviceHandle, image, tmpl);
            } else if (devtype == 2) {
                fprCap.LIVESCAN_BeginCapture(0);
                ret = fprCap.LIVESCAN_GetFPRawData(0, image);
                fprCap.LIVESCAN_EndCapture(0);
                if (ret != 1) { result.error("CAPTURE_FAILED", "ret=" + ret, null); return; }
                fprCap.FP_Begin();
                ret = fprCap.FP_FeatureExtract(43, 99, image, tmpl);
                fprCap.FP_End();
            } else {
                result.error("NO_DEVICE", "Device not open", null);
                return;
            }
            if (ret == 1) {
                templates.add(tmpl);
                result.success(templates.size() - 1); // returns enrolled ID
            } else {
                result.error("ENROLL_FAILED", "Template creation failed: " + ret, null);
            }
        }).start();
    }

    private void verifyFingerprint(Result result) {
        new Thread(() -> {
            byte[] image = new byte[256 * 360];
            byte[] tmpl = new byte[512];
            int ret;
            if (devtype == 1) {
                ret = lapiInterface.F_GetImage(deviceHandle, image);
                if (ret != 1) { result.error("CAPTURE_FAILED", "ret=" + ret, null); return; }
                ret = lapiInterface.F_CreateTemplate(deviceHandle, image, tmpl);
            } else if (devtype == 2) {
                fprCap.LIVESCAN_BeginCapture(0);
                ret = fprCap.LIVESCAN_GetFPRawData(0, image);
                fprCap.LIVESCAN_EndCapture(0);
                if (ret != 1) { result.error("CAPTURE_FAILED", "ret=" + ret, null); return; }
                fprCap.FP_Begin();
                ret = fprCap.FP_FeatureExtract(43, 99, image, tmpl);
                fprCap.FP_End();
            } else {
                result.error("NO_DEVICE", "Device not open", null);
                return;
            }
            if (ret != 1) { result.error("TEMPLATE_FAILED", "ret=" + ret, null); return; }

            // Search all stored templates
            for (int i = 0; i < templates.size(); i++) {
                int score;
                if (devtype == 1) {
                    score = lapiInterface.F_CompareTemplates(deviceHandle, tmpl, templates.get(i));
                } else {
                    int[] scoreArr = new int[1];
                    fprCap.FP_Begin();
                    fprCap.FP_FeatureMatchPC(tmpl, templates.get(i), scoreArr);
                    fprCap.FP_End();
                    score = scoreArr[0];
                }
                if (score >= MATCH_THRESHOLD) {
                    HashMap<String, Object> res = new HashMap<>();
                    res.put("matched", true);
                    res.put("id", i);
                    res.put("score", score);
                    result.success(res);
                    return;
                }
            }
            HashMap<String, Object> res = new HashMap<>();
            res.put("matched", false);
            res.put("id", -1);
            res.put("score", 0);
            result.success(res);
        }).start();
    }

    @Override public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
    @Override public void onAttachedToActivity(@NonNull ActivityPluginBinding b) { activity = b.getActivity(); }
    @Override public void onDetachedFromActivityForConfigChanges() {}
    @Override public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding b) { activity = b.getActivity(); }
    @Override public void onDetachedFromActivity() { activity = null; }
}