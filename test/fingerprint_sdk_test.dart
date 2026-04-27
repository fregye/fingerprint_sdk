import 'package:flutter_test/flutter_test.dart';
import 'package:fingerprint_sdk/fingerprint_sdk.dart';
import 'package:fingerprint_sdk/fingerprint_sdk_platform_interface.dart';
import 'package:fingerprint_sdk/fingerprint_sdk_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockFingerprintSdkPlatform
    with MockPlatformInterfaceMixin
    implements FingerprintSdkPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final FingerprintSdkPlatform initialPlatform = FingerprintSdkPlatform.instance;

  test('$MethodChannelFingerprintSdk is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelFingerprintSdk>());
  });

  test('getPlatformVersion', () async {
    FingerprintSdk fingerprintSdkPlugin = FingerprintSdk();
    MockFingerprintSdkPlatform fakePlatform = MockFingerprintSdkPlatform();
    FingerprintSdkPlatform.instance = fakePlatform;

    expect(await fingerprintSdkPlugin.getPlatformVersion(), '42');
  });
}
