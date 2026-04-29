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
        uid: (m['uid'] as String?) ?? '',
        cardType: (m['cardType'] as int?) ?? 0,
        cardTypeName: (m['cardTypeName'] as String?) ?? 'Unknown',
        ats: (m['ats'] as String?) ?? '',
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

  // ── DESFire public commands (no authentication required) ─────────────────────

  /// Returns the list of Application IDs on a DESFire card as hex strings,
  /// e.g. ["000001", "AABBCC"]. Call while card is still on the reader.
  static Future<List<String>> readDesFireApplicationIds() async {
    final List<int> buf = await _desFireMultiFrame([0x90, 0x6A, 0x00, 0x00, 0x00]);
    final List<String> aids = [];
    for (int i = 0; i + 2 < buf.length; i += 3) {
      aids.add(buf
          .sublist(i, i + 3)
          .map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase())
          .join());
    }
    return aids;
  }

  /// Returns DESFire card version / manufacture info.
  /// Keys: hwMajorVersion, hwMinorVersion, swMajorVersion, swMinorVersion,
  ///       uid (7-byte hex), productionWeek, productionYear, rawHex.
  static Future<Map<String, dynamic>> readDesFireVersion() async {
    final List<int> d = await _desFireMultiFrame([0x90, 0x60, 0x00, 0x00, 0x00]);
    final raw = d.map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase()).join(' ');
    if (d.length < 28) return {'rawHex': raw};
    return {
      'hwVendorId':      d[0].toRadixString(16).padLeft(2, '0').toUpperCase(),
      'hwMajorVersion':  d[3],
      'hwMinorVersion':  d[4],
      'swMajorVersion':  d[10],
      'swMinorVersion':  d[11],
      'uid': d.sublist(14, 21)
              .map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase())
              .join(),
      'productionWeek':  d[26],
      'productionYear':  2000 + d[27],
      'rawHex': raw,
    };
  }

  /// Scans all DESFire applications and reads every publicly accessible file.
  ///
  /// Returns a Map keyed by AID hex string (e.g. "000001"). Each value is a
  /// list of file entries with keys:
  ///   fileId     – hex string (e.g. "01")
  ///   accessible – bool: false if protected by a key
  ///   hex        – raw data as uppercase hex (only when accessible)
  ///   text       – printable ASCII extracted from data (only when present)
  ///
  /// The Ghana Card Number (GHA-XXXXXXXXX-X) will appear in [text] if it is
  /// stored in a publicly readable file. Expect this call to take 5–15 s
  /// while the card is held on the reader.
  static Future<Map<String, List<Map<String, dynamic>>>> scanDesFirePublicData() async {
    final result = <String, List<Map<String, dynamic>>>{};

    // Try the PICC master application (AID 000000) plus all user apps.
    final List<String> aids = ['000000', ...await readDesFireApplicationIds()];

    for (final aid in aids) {
      try {
        final aidBytes = _hexToBytes(aid);

        // Select application
        final selResp = await transceiveNfc(
            [0x90, 0x5A, 0x00, 0x00, 0x03, ...aidBytes, 0x00]);
        if (!_desFireOk(selResp)) continue;

        // GetFileIDs
        final fileIdsResp = await transceiveNfc([0x90, 0x6F, 0x00, 0x00, 0x00]);
        if (!_desFireOk(fileIdsResp)) continue;
        final fileIds = fileIdsResp.sublist(0, fileIdsResp.length - 2);

        final files = <Map<String, dynamic>>[];

        for (final fileId in fileIds) {
          final fileIdHex =
              fileId.toRadixString(16).padLeft(2, '0').toUpperCase();
          try {
            // GetFileSettings
            final sResp = await transceiveNfc(
                [0x90, 0xF5, 0x00, 0x00, 0x01, fileId, 0x00]);
            if (!_desFireOk(sResp)) continue;
            final s = sResp.sublist(0, sResp.length - 2);
            if (s.length < 4) continue;

            final fileType = s[0];
            // AccessRights: s[2]=RW|Change nibbles, s[3]=Read|Write nibbles
            // 0xE = free/public
            final readKey = (s[3] >> 4) & 0x0F;

            if (readKey != 0x0E) {
              files.add({'fileId': fileIdHex, 'fileType': fileType,
                         'accessible': false, 'readKey': readKey});
              continue;
            }

            // Read the file (offset=0, length=0 means read all in DESFire)
            List<int> cmd;
            if (fileType == 0x02) {
              // Value file → GetValue
              cmd = [0x90, 0x6C, 0x00, 0x00, 0x01, fileId, 0x00];
            } else if (fileType == 0x03 || fileType == 0x04) {
              // Record file → ReadRecords
              cmd = [0x90, 0xBB, 0x00, 0x00, 0x07,
                     fileId, 0, 0, 0, 0, 0, 0, 0x00];
            } else {
              // Standard / Backup → ReadData
              cmd = [0x90, 0xBD, 0x00, 0x00, 0x07,
                     fileId, 0, 0, 0, 0, 0, 0, 0x00];
            }

            final dResp = await transceiveNfc(cmd, timeout: 3000);
            if (!_desFireOk(dResp)) {
              files.add({'fileId': fileIdHex, 'fileType': fileType,
                         'accessible': false, 'error': 'read denied'});
              continue;
            }

            final data = dResp.sublist(0, dResp.length - 2);
            final hex = data
                .map((b) => b.toRadixString(16).padLeft(2, '0').toUpperCase())
                .join();
            // Extract printable ASCII — Ghana Card No. is ASCII text
            final text = String.fromCharCodes(
                data.where((b) => b >= 0x20 && b <= 0x7E));

            files.add({
              'fileId': fileIdHex,
              'fileType': fileType,
              'accessible': true,
              'hex': hex,
              if (text.trim().isNotEmpty) 'text': text.trim(),
            });
          } catch (_) {
            files.add(
                {'fileId': fileIdHex, 'accessible': false, 'error': 'exception'});
          }
        }

        if (files.isNotEmpty) result[aid] = files;
      } catch (_) {
        continue;
      }
    }
    return result;
  }

  // ── DeSFire internals ─────────────────────────────────────────────────────────

  // Sends a DeSFire command and collects all frames (status 91 AF = more data).
  static Future<List<int>> _desFireMultiFrame(List<int> firstCmd) async {
    const moreData = [0x90, 0xAF, 0x00, 0x00, 0x00];
    final List<int> buf = [];
    var resp = await transceiveNfc(firstCmd);
    while (resp.length >= 2) {
      final sw1 = resp[resp.length - 2];
      final sw2 = resp[resp.length - 1];
      buf.addAll(resp.sublist(0, resp.length - 2));
      if (sw1 == 0x91 && sw2 == 0xAF) {
        resp = await transceiveNfc(moreData);
      } else {
        break;
      }
    }
    return buf;
  }

  static bool _desFireOk(Uint8List resp) =>
      resp.length >= 2 &&
      resp[resp.length - 2] == 0x91 &&
      resp[resp.length - 1] == 0x00;

  static List<int> _hexToBytes(String hex) {
    hex = hex.replaceAll(' ', '');
    final result = <int>[];
    for (int i = 0; i < hex.length; i += 2) {
      result.add(int.parse(hex.substring(i, i + 2), radix: 16));
    }
    return result;
  }
}
