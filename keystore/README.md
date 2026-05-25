# TapRead Keystore

| Field | Value |
|-------|-------|
| File | `tapread.jks` |
| Alias | `tapread` |
| Store password | `deadboy` |
| Key password | `deadboy` |
| Algorithm | RSA 2048 |
| Validity | ~27 years |
| SHA1 | `C1:A4:43:1A:7C:5A:0E:D8:BC:08:3E:D5:34:A9:BE:B0:D0:CD:71:FE` |

## Regenerating

If you need to recreate this keystore (e.g. lost or corrupted):

```bash
keytool -genkeypair \
  -alias tapread \
  -keyalg RSA -keysize 2048 \
  -validity 9999 \
  -storepass deadboy -keypass deadboy \
  -dname "CN=TapRead, OU=Dev, O=TapRead, L=KL, ST=KL, C=MY" \
  -keystore tapread.jks
```

**Warning:** Regenerating produces a different key. You'll need to uninstall
any existing TapRead build from your device before installing with the new key.
