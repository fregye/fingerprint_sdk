package com.example.fingerprint_sdk;

import androidx.annotation.NonNull;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.IDWORLD.Interface;
import com.dk.log.DKLog;
import com.dk.log.DKLogCallback;
import com.dk.uartnfc.Card.Card;
import com.dk.uartnfc.DeviceManager.DeviceManager;
import com.dk.uartnfc.DeviceManager.DeviceManagerCallback;
import com.dk.uartnfc.DeviceManager.UartNfcDevice;
import com.dk.uartnfc.Exception.DeviceNoResponseException;
import com.dk.uartnfc.Tool.StringTool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class FingerprintSdkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private Interface lapiInterface;
    private long deviceHandle = 0;
    private boolean deviceOpen = false;

    private UartNfcDevice uartNfcDevice;
    private EventChannel.EventSink nfcEventSink;
    private volatile Card currentCard = null;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        channel = new MethodChannel(binding.getBinaryMessenger(), "fingerprint_sdk");
        channel.setMethodCallHandler(this);

        Context context = binding.getApplicationContext();
        lapiInterface = new Interface(context);

        // Prevent SDK crash: SerialHelper.ReadThread catches IOExceptions whose
        // getMessage() is null; the default DKLogCallback passes null to
        // android.util.Log.println_native which throws NPE and kills the process.
        DKLog.setLogCallback(new DKLogCallback() {
            @Override public void onReceiveLogI(String tag, String msg) {
                if (msg != null) android.util.Log.i(tag, msg);
            }
            @Override public void onReceiveLogD(String tag, String msg) {
                if (msg != null) android.util.Log.d(tag, msg);
            }
            @Override public void onReceiveLogE(String tag, String msg) {
                if (msg != null) android.util.Log.e(tag, msg);
            }
        });

        uartNfcDevice = new UartNfcDevice();
        uartNfcDevice.setCallBack(nfcCallback);

        new EventChannel(binding.getBinaryMessenger(), "fingerprint_sdk/nfc_events")
            .setStreamHandler(new EventChannel.StreamHandler() {
                @Override
                public void onListen(Object arguments, EventChannel.EventSink events) {
                    nfcEventSink = events;
                }
                @Override
                public void onCancel(Object arguments) {
                    nfcEventSink = null;
                }
            });
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "openDevice":     openDevice(result);        break;
            case "closeDevice":    closeDevice(result);       break;
            case "getImage":       getImage(result);          break;
            case "openNfc":        openNfc(call, result);     break;
            case "closeNfc":       closeNfc(result);          break;
            case "getNfcPorts":    getNfcPorts(result);       break;
            case "findAndOpenNfc": findAndOpenNfc(result);    break;
            case "transceiveNfc":  transceiveNfc(call, result); break;
            default: result.notImplemented();
        }
    }

    // ── Fingerprint ────────────────────────────────────────────────────────────

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

    // ── NFC ────────────────────────────────────────────────────────────────────

    private void openNfc(MethodCall call, Result result) {
        new Thread(() -> {
            try {
                String port = call.argument("port");
                String baud = call.argument("baud");
                if (baud == null || baud.isEmpty()) baud = "115200";
                if (port == null || port.isEmpty()) {
                    List<String> ports = uartNfcDevice.serialManager.getAvailablePorts();
                    if (ports.isEmpty()) {
                        result.error("NO_PORT", "No serial ports found", null);
                        return;
                    }
                    port = ports.get(0);
                }
                boolean ok = uartNfcDevice.serialManager.open(port, baud);
                result.success(ok);
            } catch (Exception e) {
                result.error("OPEN_NFC_FAILED", e.getMessage(), null);
            }
        }).start();
    }

    private void closeNfc(Result result) {
        new Thread(() -> {
            uartNfcDevice.serialManager.close();
            result.success(true);
        }).start();
    }

    private void getNfcPorts(Result result) {
        List<String> ports = uartNfcDevice.serialManager.getAvailablePorts();
        result.success(ports);
    }

    private void findAndOpenNfc(Result result) {
        new Thread(() -> {
            try {
                List<String> ports = uartNfcDevice.serialManager.getAvailablePorts();
                android.util.Log.i("NFC", "Scanning ports: " + ports);
                for (String port : ports) {
                    if (uartNfcDevice.serialManager.isOpen()) {
                        uartNfcDevice.serialManager.close();
                    }
                    boolean opened = uartNfcDevice.serialManager.open(port, "115200");
                    if (!opened) {
                        android.util.Log.w("NFC", "Could not open " + port);
                        continue;
                    }
                    try {
                        Thread.sleep(600);
                        String version = uartNfcDevice.getFirmwareVersion();
                        android.util.Log.i("NFC", "DK21 found on " + port + ", firmware=" + version);
                        result.success(port);
                        return;
                    } catch (DeviceNoResponseException e) {
                        android.util.Log.w("NFC", port + " no response");
                        uartNfcDevice.serialManager.close();
                    } catch (InterruptedException e) {
                        result.error("INTERRUPTED", null, null);
                        return;
                    }
                }
                result.error("NO_NFC", "DK21 not found on any port", null);
            } catch (Exception e) {
                result.error("SCAN_FAILED", e.getMessage(), null);
            }
        }).start();
    }

    // Sends a raw APDU/command to the currently active card via reflection,
    // since transceive() is not on the Card base class but on each subtype.
    private void transceiveNfc(MethodCall call, Result result) {
        byte[] command = call.argument("cmd");
        Integer timeout = call.argument("timeout");
        if (timeout == null) timeout = 1000;
        Card card = currentCard;
        if (card == null) {
            result.error("NO_CARD", "No card present on reader", null);
            return;
        }
        final int t = timeout;
        new Thread(() -> {
            try {
                java.lang.reflect.Method m = card.getClass()
                        .getMethod("transceive", byte[].class, int.class);
                byte[] response = (byte[]) m.invoke(card, command, t);
                result.success(response);
            } catch (NoSuchMethodException e) {
                result.error("NOT_SUPPORTED",
                        "Card type " + card.getClass().getSimpleName() + " does not support transceive", null);
            } catch (java.lang.reflect.InvocationTargetException e) {
                result.error("TRANSCEIVE_FAILED",
                        e.getCause() != null ? e.getCause().getMessage() : "no response", null);
            } catch (Exception e) {
                result.error("TRANSCEIVE_FAILED", e.getMessage(), null);
            }
        }).start();
    }

    private final DeviceManagerCallback nfcCallback = new DeviceManagerCallback() {
        @Override
        public void onReceiveRfnSearchCard(boolean blnIsSus, int cardType,
                                           byte[] bytCardSn, byte[] bytCarATS) {
            currentCard = uartNfcDevice.getCard();
            Map<String, Object> event = new HashMap<>();
            event.put("uid", StringTool.byteHexToSting(bytCardSn));
            event.put("cardType", cardType);
            event.put("cardTypeName", cardTypeName(cardType));
            event.put("ats", StringTool.byteHexToSting(bytCarATS));
            mainHandler.post(() -> {
                if (nfcEventSink != null) nfcEventSink.success(event);
            });
        }

        @Override
        public void onReceiveCardLeave() {
            currentCard = null;
            mainHandler.post(() -> {
                if (nfcEventSink != null) nfcEventSink.success(null);
            });
        }
    };

    private static String cardTypeName(int type) {
        switch (type) {
            case DeviceManager.CARD_TYPE_ISO4443_A:  return "CPU_ISO14443A";
            case DeviceManager.CARD_TYPE_FELICA:     return "FeliCa";
            case DeviceManager.CARD_TYPE_ULTRALIGHT: return "NTAG_Ultralight";
            case DeviceManager.CARD_TYPE_MIFARE:     return "Mifare";
            case DeviceManager.CARD_TYPE_ISO15693:   return "ISO15693";
            case DeviceManager.CARD_TYPE_DESFire:    return "DESFire";
            case DeviceManager.CARD_TYPE_ISO4443_B:  return "ISO14443B";
            case 7:                                   return "ID_125kHz";
            default:                                  return "Unknown_" + type;
        }
    }

    @Override public void onDetachedFromEngine(@NonNull FlutterPluginBinding b) {
        channel.setMethodCallHandler(null);
        uartNfcDevice.destroy();
    }
    @Override public void onAttachedToActivity(@NonNull ActivityPluginBinding b) {}
    @Override public void onDetachedFromActivityForConfigChanges() {}
    @Override public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding b) {}
    @Override public void onDetachedFromActivity() {}
}
