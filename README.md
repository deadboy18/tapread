# TapRead

> **Tap. Read. Know.**

[![Download APK](https://img.shields.io/github/v/release/deadboy18/tapread?label=Download%20APK&style=for-the-badge)](https://github.com/deadboy18/tapread/releases/latest/download/tapread-v1.1.0-release.apk)
&nbsp;&nbsp;
[![Web Reader](https://img.shields.io/badge/TNG%20Serial-Web%20Reader-00a3e0?style=for-the-badge)](https://deadboy18.github.io/tng-serial/)

An Android NFC card reader for EMV contactless bank cards and **Touch 'n Go (TNG) stored-value cards**. Built for fintech professionals — POS sellers, terminal installers, payment gateway developers, and NFC/RFID engineers.

**Don't want to install an app?** Use the [TNG Serial Web Reader](https://deadboy18.github.io/tng-serial/) — tap your TNG card or MyKad in Chrome on Android to get the 10-digit serial number instantly. No app install needed. *(Balance and sector data require the full Android app.)*

Made with 💀 by [deadboy](https://github.com/deadboy18)

---

## What It Does

Hold any contactless card against your phone. TapRead automatically detects the card type and reads everything:

**EMV bank cards** — Visa, Mastercard, Amex, and 15+ other schemes. Reads PAN, expiry, Track 1/2, AIDs, ATR, CPLC, transaction history, CVM list, and full APDU log with TLV parsing.

**Touch 'n Go cards** — Malaysian TNG stored-value cards including MyKad+TNG combo cards and standalone TNG cards. Reads balance, serial number, card number, expiry, transaction history (toll, reload, transit, retail), chip type, and raw sector hex dump. Uses recovered master keys with XOR-based key derivation — works on any TNG card in Malaysia.

No internet. No ads. No analytics. Everything stays on your device.

---

## Touch 'n Go Reader

TapRead includes a full TNG MIFARE Classic reader with embedded master keys. When you tap a TNG card, the app derives per-card sector keys from the UID, authenticates to all accessible sectors, and decodes the data.

### What It Reads

| Field | Source | Example |
|-------|--------|---------|
| Balance | Sector 2, value block | RM 41.66 |
| Serial number | Derived from UID | 3268275621 |
| Card number | Sector 0 block 2 | 6014640011297642xx |
| Expiry | Sector 0 block 2 | 2032-12 |
| TNG fingerprint | Sector 0 block 1 | ✓ Verified (00 01 02...0F) |
| Chip type | MifareClassic API | MIFARE Classic 1K / 4K / Plus |
| Transaction count | Sector 3, value block | 25 transactions |
| Last reload | Sector 7 | RM 49.50, 2026-06-04 14:47:11 |
| Last toll | Sector 5 | Amount, timestamp, agency code |
| Last transit | Sector 6 | Entry/exit station, timestamps |
| Last retail | Sector 8 | Amount, timestamp, terminal |
| Raw hex dump | All sectors | Full sector-by-sector hex |

### Supported TNG Cards

| Card Type | Chip | SAK | Status |
|-----------|------|-----|--------|
| MyKad + TNG combo | Infineon MIFARE Classic 1K | 0x88 | ✓ Full read |
| Standalone TNG | MIFARE Classic 4K (Plus in Classic mode) | 0x38 | ✓ Full read |

### How It Works

The app uses 14 recovered master keys and an XOR-based key derivation function (KDF) to derive per-card sector keys from the card's 4-byte UID. The KDF is the same one used by all TNG infrastructure — same master keys work on every TNG card in Malaysia.

```
UID → KDF(master_key, uid) → per-card sector key → authenticate → read
```

Sector 5 factory template detection prevents false "Toll" transactions from appearing on brand-new cards that have never been used.

---

## Features

### Card Reading
- Reads Visa, Mastercard, Amex, JCB, UnionPay, Discover, RuPay, Maestro, CB, Dankort, Interac, and more
- **Touch 'n Go MIFARE Classic** — balance, serial, card number, expiry, transactions, raw dump
- Automatic card type detection — EMV (IsoDep) vs TNG (MifareClassic) routed automatically
- Automatic scheme detection from AID prefix, application label, and PAN range
- Multi-application support (reads all AIDs on the chip)
- Contactless-disabled detection (PPSE 6A82/6985 → "NFC is locked on your card")
- Tokenized card detection (Apple Pay, Google Pay, Samsung Pay, Garmin, Fitbit, Huawei)

### EMV Data Extracted
| Data | Source | Details |
|------|--------|---------|
| Card number (PAN) | Tag 5A | Full or masked, copyable |
| Expiry date | Tag 5F24 | MM/YY format |
| Cardholder name | Tag 5F20 | Often blank on modern cards |
| Track 1 data | Tag 56 | Raw hex |
| Track 2 data | Tag 57 | Raw hex + parsed service code |
| Service code | Derived from Track 2 | Decoded: "International, Normal, No restrictions" |
| All AIDs | Tag 4F | Hex + label + priority |
| ATR/ATS | Historical bytes | Chip platform identification |
| Card issuer (possible) | ATR description | From devnied library's ATR database |
| CPLC | Tag 9F7F | IC fabricator, type, OS, manufacturer |
| CVM list | Tag 8E | Decoded: PIN, signature, CDCVM, no CVM |
| Transaction history | Log records | Date, time, amount, currency, country, cryptogram |
| Transaction time | Tag 9F21 via 9F4F | Parsed from flat log records using log format |
| NFC status | PPSE response | Active / Disabled / Blocked |
| Wallet type | Application labels | Physical card or tokenized (Apple Pay, etc.) |

### APDU Log
- Full command/response hex with color coding (green commands, blue responses)
- BER-TLV tree parsing with 60+ EMV tag dictionary
- Status word descriptions (90 00 → "Command OK", 6A82 → "File not found")
- ASCII decoding where applicable
- Shareable as text with TLV tree included

### App Features
- **Navigation drawer** — Cards, Settings, About
- **Persistent storage** — Scanned cards survive app restarts (SharedPreferences + Gson)
- **Mask PAN** — Toggle to show/hide middle digits
- **Dark mode** — System default or manual toggle
- **Export JSON** — Share all stored card data via Android share sheet
- **Copy buttons** — Copy PAN, copy extended details to clipboard
- **BIN Lookup** — Opens bincheck.io with the card's BIN pre-filled
- **Haptic feedback** — Pulse on card detect, success pattern on read, ticks on UI interactions
- **Reading dialog** — "Reading in progress… Please do not remove or move card"
- **NFC intent filter** — App appears in "Choose an action" when tapping a card outside the app
- **NFC status detection** — Prompts to enable NFC if disabled, warns if hardware missing

### Easter Eggs 🥚
- Tap the title on the About screen 7 times
- Shake your phone on the About screen
- Both with custom haptic patterns

---

## Screenshots

| Home | Card Detail | Transactions | Log |
|------|-------------|--------------|-----|
| NFC tap illustration | Scheme-colored card face with brand logo | Expandable rows with time + cryptogram | Color-coded APDU + TLV tree |

---

## Supported Cards

### Payment Schemes
Visa, Mastercard, American Express, JCB, UnionPay, Discover, Maestro, CB (France), Dankort (Denmark), CoGeBan (Italy), Banrisul (Brazil), SPAN (Saudi Arabia), Interac (Canada), RuPay (India), Verve (Nigeria), TROY (Turkey), MIR (Russia)

### Touch 'n Go (Malaysia)
MyKad + TNG combo cards, standalone TNG cards (both 1K and 4K variants)

### Tokenized Wallets
Apple Pay, Google Pay, Samsung Pay, Garmin Pay, Fitbit Pay, Huawei Pay, Xiaomi Pay

### Contactless-Disabled Cards
Cards with NFC payment turned off in the bank app are detected and labeled with an orange warning banner.

---

## How It Works

### NFC Tag Routing
```
Tag discovered
├── Has IsoDep?  → EMV reader (bank cards)
└── Has MifareClassic / NfcA without IsoDep?  → TNG reader
```

### EMV Communication Flow
```
1. Phone enables ReaderMode (NFC-A + NFC-B)
2. Card enters RF field → IsoDep connection established
3. SELECT PPSE (2PAY.SYS.DDF01) → get list of payment apps
4. SELECT AID → select each payment application
5. GET PROCESSING OPTIONS → get AFL (file locator)
6. READ RECORD × N → read card data records
7. READ RECORD × N → read transaction log records
8. IsoDep closed
```

### TNG Communication Flow
```
1. Read UID (4 bytes, no authentication needed)
2. Derive serial number from UID (reverse bytes → decimal)
3. For each sector 0–15:
   a. Derive per-card key = master_key XOR pattern(UID)
   b. Authenticate with Key A (or Key B for sectors 11, 15)
   c. Read 3 data blocks (skip sector trailer)
4. Decode: balance (sector 2), card number (sector 0),
   transactions (sectors 5–8), counter (sector 3)
5. Detect factory template in sector 5 → skip if not a real transaction
```

### Architecture
```
MainActivity
├── NfcDispatcher          ReaderMode lifecycle
├── EmvReader              EMV card reading + data extraction
│   ├── IsoDepProvider     Bridges IsoDep ↔ devnied IProvider
│   ├── ApduLogger         Captures all APDU exchanges
│   └── TlvParser          BER-TLV parsing + EMV tag dictionary
├── TngCardReader          TNG MIFARE Classic reader
│   ├── Master keys        14 Key A + 2 Key B master keys
│   ├── KDF                XOR-based key derivation from UID
│   └── Sector decoders    Balance, identity, transactions, counter
├── CardsViewModel         Shared state + persistence
│   └── CardStorage        SharedPreferences + Gson
├── HomeFragment           Card list + NFC tap prompt
├── DetailFragment         ViewPager2 (tabs vary by card type)
│   ├── CardDetailFragment EMV card face + extended details
│   ├── TngDetailFragment  TNG balance card + info + transactions
│   ├── TransactionsFragment EMV transaction list
│   └── LogFragment        APDU log / raw hex dump
├── SettingsFragment       Dark mode, mask PAN, export, clear
└── AboutFragment          Credits + easter eggs
```

### Transaction Time Extraction (EMV)
The devnied library doesn't parse tag 9F21 (Transaction Time). TapRead extracts it directly:

1. **Find tag 9F4F** (Log Format) in APDU responses — defines the flat record structure
2. **Parse 9F4F** into tag-length pairs: `[9A(3), 9F21(3), 9F02(6), 5F2A(2), ...]`
3. **Calculate byte offset** of 9F21 within the flat record
4. **Read 3 BCD bytes** at that offset from each READ RECORD log response
5. `13 32 08` → "13:32:08"

---

## Build & Install

### Download
[![Download APK](https://img.shields.io/github/v/release/deadboy18/tapread?label=Download%20APK&style=for-the-badge)](https://github.com/deadboy18/tapread/releases/latest/download/tapread-v1.1.0-release.apk)

### Prerequisites
- Android Studio Hedgehog or later (Iguana recommended)
- Android device with NFC (min SDK 24 / Android 7.0)
- USB debugging enabled

### Quick Start
```bash
# Open in Android Studio
File → Open → select tapread/ folder

# Let Gradle sync (downloads ~50MB of dependencies first time)
# Connect your phone via USB
# Run → green ▶ button or Shift+F10

# Or build a signed release APK:
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/tapread-v1.1.0-release.apk
```

### Windows Users
Run this before first build to clean Windows Explorer's auto-generated files:
```powershell
.\clean-desktop-ini.ps1
```

### Signing
| | |
|--|--|
| Keystore | `keystore/tapread.jks` |
| Alias | `tapread` |
| Password | `deadboy` |

---

## Project Structure
```
tapread/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── assets/logback.xml
│   ├── kotlin/com/tapread/nfc/
│   │   ├── App.kt                    Application init
│   │   ├── MainActivity.kt           NFC dispatch + drawer + tag routing
│   │   ├── model/
│   │   │   ├── ApduEntry.kt          APDU command/response pair
│   │   │   ├── CardData.kt           All parsed EMV card fields
│   │   │   ├── ScanResult.kt         EMV or TNG result + timestamp
│   │   │   └── TngData.kt            TNG card fields + transactions
│   │   ├── nfc/
│   │   │   ├── ApduLogger.kt         Captures APDU exchanges
│   │   │   ├── EmvReader.kt          Core EMV reading logic
│   │   │   ├── IsoDepProvider.kt     devnied IProvider bridge
│   │   │   ├── NfcDispatcher.kt      ReaderMode lifecycle
│   │   │   └── TngCardReader.kt      TNG MIFARE Classic reader + KDF
│   │   ├── ui/
│   │   │   ├── CardsViewModel.kt     Shared state + persistence
│   │   │   ├── about/AboutFragment.kt
│   │   │   ├── detail/
│   │   │   │   ├── CardDetailFragment.kt  EMV card detail
│   │   │   │   ├── DetailFragment.kt
│   │   │   │   ├── DetailPagerAdapter.kt  Routes EMV vs TNG tabs
│   │   │   │   ├── LogFragment.kt         APDU log / raw hex
│   │   │   │   ├── TngDetailFragment.kt   TNG balance + info + txns
│   │   │   │   └── TransactionsFragment.kt EMV transactions
│   │   │   ├── home/
│   │   │   │   ├── CardListAdapter.kt
│   │   │   │   └── HomeFragment.kt
│   │   │   └── settings/SettingsFragment.kt
│   │   └── util/
│   │       ├── CardStorage.kt        SharedPreferences persistence
│   │       ├── HapticUtil.kt         Vibration patterns
│   │       ├── HexUtil.kt            Hex encoding/decoding
│   │       └── TlvParser.kt          BER-TLV + EMV tags + CVM + service code
│   └── res/
│       ├── drawable/                  Card gradients, icons, logos
│       ├── layout/                    All XML layouts
│       ├── menu/nav_drawer.xml        Drawer menu
│       ├── values/                    Colors, strings, themes (light)
│       ├── values-night/              Dark theme overrides
│       └── xml/nfc_tech_filter.xml    NFC intent filter
├── build.gradle.kts                   Root build file
├── app/build.gradle.kts               Module build + signing + APK naming
├── keystore/tapread.jks               Release signing key
└── clean-desktop-ini.ps1              Windows build fix script
```

---

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| devnied/emvnfccard | 3.1.0 | EMV card parsing (core library) |
| Material | 1.12.0 | Material 3 UI components |
| AndroidX Core | 1.13.1 | Kotlin extensions |
| Lifecycle ViewModel | 2.7.0 | Shared ViewModel |
| Coroutines | 1.8.1 | Async card reading |
| Gson | 2.11.0 | JSON persistence |
| SLF4J + Logback | 2.0.13 / 3.0.0 | Logging bridge for EMV library |

---

## Permissions

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.VIBRATE" />
<!-- NO android.permission.INTERNET — offline by design -->
```

The `INTERNET` permission is deliberately absent. The app cannot make network requests. BIN lookup opens the browser (a separate app with its own permissions).

---

## Security & Privacy

- **No internet** — architecturally impossible to leak data
- **No analytics** — zero telemetry, no crash reporting
- **No ads** — no advertising SDKs
- **On-device storage only** — SharedPreferences, app-private
- **Mask PAN by default** — middle digits hidden until toggled
- **User-initiated export only** — JSON share requires explicit tap
- **NFC only** — reads contactless data that any POS terminal can read
- **TNG keys are read-only** — the app authenticates with Key A (read access) and cannot write to cards

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App doesn't react to card taps | Check NFC is enabled in phone Settings |
| TNG card not detected | Samsung/NXP NFC controllers required for MIFARE Classic — some phones (Pixel) don't support it |
| TNG shows "MifareClassic unavailable" | Your phone's NFC chip doesn't support MIFARE Classic |
| TNG balance shows wrong value | Report it — timestamp/balance parsing is still being verified across card generations |
| New TNG card shows no toll transaction | Correct — sector 5 factory template is now detected and skipped |
| "Unsupported card type" | Card uses FeliCa or other non-EMV/non-MIFARE protocol |
| No transactions shown | Many modern EMV cards don't expose transaction logs for privacy |
| No transaction time | Card's log format (9F4F) doesn't include tag 9F21 |
| "NFC is locked on your card" | Contactless disabled in bank app — enable it there |
| Card number shows dots | PAN masking is enabled — toggle in Settings |
| `desktop.ini` build error | Run `.\clean-desktop-ini.ps1` (Windows only) |

---

## What This App Does NOT Do

- **Not a payment app** — cannot make transactions or charges
- **Not a card cloner** — cannot write data to cards or emulate cards
- **Not a balance editor** — reads TNG balance but cannot modify it
- **Not a security tool** — reads only public data (EMV) or data accessible with recovered read keys (TNG)
- **Not a hacker tool** — EMV data is freely exposed per spec; TNG read keys are documented in open-source projects

---

## Credits

- **EMV Library**: [devnied/EMV-NFC-Paycard-Enrollment](https://github.com/devnied/EMV-NFC-Paycard-Enrollment) v3.1.0
- **Reference**: [AndroidCrypto/Android-EMV-NFC-Paycard-Example](https://github.com/AndroidCrypto/Android-EMV-NFC-Paycard-Example)
- **TNG Research**: Key derivation based on [Metrodroid](https://github.com/metrodroid/metrodroid) open-source transit reader
- **EMV Specifications**: EMVCo Book 1-4, ISO/IEC 7816
- **Built by**: [deadboy](https://github.com/deadboy18)

---

## License

Apache 2.0 (matches the EMV library)
