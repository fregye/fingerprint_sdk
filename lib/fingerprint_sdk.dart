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

  // Returns raw 256x360 grayscale bytes, or null on failure
  static Future<Uint8List?> getImage() async {
    try {
      final result = await _channel.invokeMethod('getImage');
      return Uint8List.fromList(List<int>.from(result));
    } catch (e) {
      return null;
    }
  }

  // Returns the enrolled ID (0-based index), or -1 on failure
  static Future<int> enrollFingerprint() async {
    try {
      final id = await _channel.invokeMethod('enrollFingerprint');
      return id as int;
    } catch (e) {
      return -1;
    }
  }

  // Returns {matched: bool, id: int, score: int}
  static Future<Map<String, dynamic>> verifyFingerprint() async {
    try {
      final result = await _channel.invokeMethod('verifyFingerprint');
      return Map<String, dynamic>.from(result);
    } catch (e) {
      return {'matched': false, 'id': -1, 'score': 0};
    }
  }

  static Future<bool> clearDatabase() async {
    return await _channel.invokeMethod('clearDatabase') ?? false;
  }

  static Future<int> getDatabaseCount() async {
    return await _channel.invokeMethod('getDatabaseCount') ?? 0;
  }

  static Future<String?> getVersion() async {
    return await _channel.invokeMethod('getVersion');
  }
}
