import 'dart:typed_data';
import 'package:flutter/services.dart';

class FingerprintSdk {
  static const MethodChannel _channel = MethodChannel('fingerprint_sdk');
  static const EventChannel _nfcChannel = EventChannel('fingerprint_sdk/nfc_events');

  // ── Fingerprint ──────────────────────────────────────────────────────────────

  static Future<bool> openDevice() async {
    return await _channel.invokeMethod('openDevice') ?? false;
  }

  static Future<bool> closeDevice() async {
    return await _channel.invokeMethod('closeDevice') ?? false;
  }

  /// Returns raw 256×360 grayscale bytes, or null on failure.
  static Future<Uint8List?> getImage() async {
    try {
      final result = await _channel.invokeMethod('getImage');
      return Uint8List.fromList(List<int>.from(result));
    } catch (e) {
      return null;
    }
  }

  // ── NFC ──────────────────────────────────────────────────────────────────────

  /// Stream of card UIDs (hex string) as cards are presented.
  /// Emits null when a card is removed.
  static Stream<String?> get nfcCardStream {
    return _nfcChannel
        .receiveBroadcastStream()
        .map((event) => event as String?);
  }

  /// Opens the NFC reader serial port.
  /// [port] defaults to the first available port.
  /// [baud] defaults to '115200'.
  static Future<bool> openNfc({String port = '', String baud = '115200'}) async {
    return await _channel.invokeMethod('openNfc', {'port': port, 'baud': baud}) ?? false;
  }

  static Future<void> closeNfc() async {
    await _channel.invokeMethod('closeNfc');
  }

  /// Returns the list of available serial port names on this device.
  static Future<List<String>> getNfcPorts() async {
    final List<dynamic> ports = await _channel.invokeMethod('getNfcPorts') ?? [];
    return ports.cast<String>();
  }

  /// Scans all serial ports, opens the one where the DK21 responds, and
  /// returns the port name (e.g. "/dev/ttyS4"). Throws on failure.
  static Future<String> findAndOpenNfc() async {
    return await _channel.invokeMethod('findAndOpenNfc');
  }
}
