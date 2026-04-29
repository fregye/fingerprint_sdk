import 'dart:typed_data';
import 'package:flutter/services.dart';

/// Info returned each time a card is detected.
class NfcCardInfo {
  final String uid;           // Hex UID, e.g. "A1B2C3D4"
  final int cardType;         // Raw type int from SDK
  final String cardTypeName;  // Human-readable, e.g. "Mifare", "NTAG_Ultralight"
  final String ats;           // Answer-to-Select hex bytes

  NfcCardInfo({
    required this.uid,
    required this.cardType,
    required this.cardTypeName,
    required this.ats,
  });

  factory NfcCardInfo.fromMap(Map<String, dynamic> m) => NfcCardInfo(
        uid: m['uid'] as String,
        cardType: m['cardType'] as int,
        cardTypeName: m['cardTypeName'] as String,
        ats: m['ats'] as String,
      );

  @override
  String toString() =>
      'NfcCardInfo(uid: $uid, type: $cardTypeName, ats: $ats)';
}

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

  /// Emits [NfcCardInfo] when a card is detected, null when removed.
  static Stream<NfcCardInfo?> get nfcCardStream {
    return _nfcChannel.receiveBroadcastStream().map((event) {
      if (event == null) return null;
      return NfcCardInfo.fromMap(Map<String, dynamic>.from(event as Map));
    });
  }

  /// Opens the NFC reader. [port] defaults to first available port.
  static Future<bool> openNfc({String port = '', String baud = '115200'}) async {
    return await _channel.invokeMethod('openNfc', {'port': port, 'baud': baud}) ?? false;
  }

  static Future<void> closeNfc() async {
    await _channel.invokeMethod('closeNfc');
  }

  /// Returns available serial port names.
  static Future<List<String>> getNfcPorts() async {
    final List<dynamic> ports = await _channel.invokeMethod('getNfcPorts') ?? [];
    return ports.cast<String>();
  }

  /// Scans all ports, opens the one where the DK21 responds, returns port name.
  static Future<String> findAndOpenNfc() async {
    return await _channel.invokeMethod('findAndOpenNfc');
  }

  /// Sends a raw APDU/command to the card currently on the reader.
  /// Card must still be present (call from within nfcCardStream listener).
  /// Example — read NTAG page 4: transceiveNfc([0x30, 0x04])
  static Future<Uint8List> transceiveNfc(List<int> command,
      {int timeout = 1000}) async {
    final result = await _channel.invokeMethod('transceiveNfc', {
      'cmd': Uint8List.fromList(command),
      'timeout': timeout,
    });
    return Uint8List.fromList(List<int>.from(result));
  }

  // ── Helpers for common read patterns ─────────────────────────────────────────

  /// Reads NDEF data from NTAG21x / Ultralight cards.
  /// Returns raw bytes of the NDEF message, or null if not NDEF-formatted.
  static Future<Uint8List?> readNtag21xNdef() async {
    try {
      // Page 3 = Capability Container; byte 0 must be 0xE1 for NDEF
      final cc = await transceiveNfc([0x30, 0x03]);
      if (cc.isEmpty || cc[0] != 0xE1) return null;

      // cc[2] = data area size in 8-byte units
      final int dataBytes = cc[2] * 8;
      final int pages = (dataBytes / 4).ceil();

      final List<int> raw = [];
      for (int page = 4; page < 4 + pages; page++) {
        final data = await transceiveNfc([0x30, page]);
        raw.addAll(data.take(4)); // Each READ returns 4 pages; take only 1 page
      }
      return Uint8List.fromList(raw);
    } catch (_) {
      return null;
    }
  }

  /// Sends a SELECT by AID command to a CPU/ISO14443A card.
  /// [aid] is the hex string of the AID, e.g. "A0000000031010" for Visa.
  static Future<Uint8List> selectAid(String aid) async {
    final aidBytes = _hexToBytes(aid);
    final cmd = [0x00, 0xA4, 0x04, 0x00, aidBytes.length, ...aidBytes, 0x00];
    return transceiveNfc(cmd);
  }

  static List<int> _hexToBytes(String hex) {
    hex = hex.replaceAll(' ', '');
    final result = <int>[];
    for (int i = 0; i < hex.length; i += 2) {
      result.add(int.parse(hex.substring(i, i + 2), radix: 16));
    }
    return result;
  }
}
