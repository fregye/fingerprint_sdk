import 'dart:typed_data';
import 'package:flutter/services.dart';

class FingerprintSdk {
  static const MethodChannel _channel = MethodChannel('fingerprint_sdk');

  static Future<bool> openDevice() async {
    return await _channel.invokeMethod('openDevice') ?? false;
  }

  static Future<bool> closeDevice() async {
    return await _channel.invokeMethod('closeDevice') ?? false;
  }

  /// Returns raw 256×360 grayscale bytes, or null on failure
  static Future<Uint8List?> getImage() async {
    try {
      final result = await _channel.invokeMethod('getImage');
      return Uint8List.fromList(List<int>.from(result));
    } catch (e) {
      return null;
    }
  }
}
