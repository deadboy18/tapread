# TapRead — Future Upgrades Roadmap

> Last updated: 25 May 2026
> Current version: v1.0.0

---

## v1.1 — Quick Wins (1-2 hours each)

### Card Count Badge
Show a small badge on the toolbar or drawer header indicating how many cards are stored. Helps users know at a glance how many scans they have.

### Swipe to Delete
Add ItemTouchHelper to the home RecyclerView. Swipe left on a card to delete it, with an undo snackbar. Currently you can only clear all cards from Settings.

### Long-Press to Copy PAN
Long-press a card in the home list to copy the PAN to clipboard without opening the detail view. Quick workflow for people scanning multiple cards.

### "Scan Another" Button
Add a floating action button or top button in the detail view that says "Scan Another Card" — saves the user from pressing back to get to the home screen.

### Vibration on Export
Add `HapticUtil.success()` when JSON export completes so the user gets tactile confirmation.

### Search / Filter
Add a search bar to the home screen to filter stored cards by scheme, last 4 digits, or date. Useful when 10+ cards are stored.

---

## v1.2 — Offline BIN Lookup (half day)

### Bundled BIN Database
Download `bins_global.csv` from [venelinkochev/bin-list-data](https://github.com/nicknisi/bin-list-data) (~9K entries, CC BY 4.0). Bundle it in `assets/`. On first launch, parse into an in-memory HashMap for O(1) lookups.

### What BIN Lookup Shows
- Issuing bank name (e.g. "Maybank", "CIMB", "HSBC")
- Card type: DEBIT / CREDIT / PREPAID / CHARGE
- Card category: PLATINUM / GOLD / CLASSIC / SIGNATURE
- Issuer country + country flag emoji
- Bank website + phone number
- Currency (e.g. MYR, USD, EUR)

### Where to Show It
Replace the "BIN Lookup" button (currently opens browser) with an instant in-app card showing the bank details. Falls back to the browser button if the BIN isn't in the database.

### Malaysian BIN Supplement
Hand-curate `bins_malaysia.json` (~200 entries) from bincheck.io covering all Malaysian banks. Merge with the global CSV for better local coverage.

---

## v1.3 — Bank-Themed Card Faces (half day)

### Asset Source
Use SVGs from [SnorSnor9998/Payment-Icon](https://github.com/SnorSnor9998/Payment-Icon) — covers all Malaysian banks + wallet brands.

### How It Works
1. BIN lookup returns bank code (e.g. "maybank", "cimb")
2. `ThemeRegistry` maps bank code → `CardTheme(primaryColor, secondaryColor, textColor, logoSvg)`
3. Card face gradient cross-fades from brand fallback to bank theme over 200ms

### Banks Covered
| Bank | Primary | Secondary |
|------|---------|-----------|
| Maybank | #FFCC00 | #000000 |
| CIMB | #A41E22 | #7A1418 |
| Public Bank | #CC0000 | #8B0000 |
| RHB | #003D7C | #1565C0 |
| Hong Leong | #E1251B | #FF6F00 |
| AmBank | #E60000 | #A30000 |
| HSBC | #DB0011 | #000000 |
| OCBC | #E60012 | #000000 |
| UOB | #005EB8 | #003366 |
| Standard Chartered | #0473EA | #38D200 |
| Affin Bank | #FFC72C | #1B3A6B |
| Bank Islam | #00833E | #005A2B |
| BSN | #E2231A | #A30E08 |
| GXBank | #000000 | #333333 |
| Boost Bank | #EE2737 | #FF9933 |

Plus brand fallbacks: Visa navy, Mastercard red/orange, Amex blue, JCB blue/green, UnionPay red/blue.

### Dependency
Add `com.caverock:androidsvg-aar:1.4` for runtime SVG rendering.

---

## v1.4 — PDF Export (half day)

### Professional Card Analysis Report
Generate a PDF document containing:
- Card face visual (rendered as bitmap)
- All extended card details
- Transaction history table
- APDU log with TLV tree
- Timestamp and device info

### Use Case
Terminal installers document card compatibility. Payment gateway developers attach card analysis to support tickets. QA teams include card data in test reports.

### Implementation
Use Android's `PdfDocument` API (no external dependency). Render each section as a View, draw to PDF canvas.

---

## v2.0 — Touch 'n Go / MIFARE Classic

### What's Readable Without Keys
| Data | Source |
|------|--------|
| UID (hex + decimal) | `tag.getId()` |
| SAK | `nfcA.getSak()` |
| ATQA | `nfcA.getAtqa()` |
| Memory size | `mifareClassic.getSize()` |
| Sector/block count | `getSectorCount()` / `getBlockCount()` |
| Tag type | `getType()` |
| Manufacturer block | Sector 0 Block 0 (usually readable with default key) |

### Standard Key Probe
Try these keys against all 16 sectors (harmless, same as MIFARE Classic Tool):
```
FFFFFFFFFFFF  (factory default)
A0A1A2A3A4A5  (MAD key)
D3F7D3F7D3F7  (well-known)
000000000000  (zero key)
```

### User-Provided Keys
Allow users to paste their own MIFARE keys (obtained from Proxmark, MCT, etc.):
- Drawer menu: "TnG Keys"
- File import or manual paste
- Format: one 12-char hex key per line
- If keys authenticate, read and decode balance

### TnG Card Face
Special blue theme with TnG logo. Show:
- UID
- "🔒 Balance: requires proprietary keys" (if no keys provided)
- Or actual balance if keys are provided

### Legal
We don't ship TnG's proprietary keys. Supplying your own keys is legal (same as MCT). The app just provides the framework.

---

## v2.1 — Compare Tool

### Side-by-Side Card Comparison
Select two cards from the stored list → see differences highlighted:
- Different AIDs
- Different ATS/ATR (different chip platform)
- Different CVM rules
- Different service codes
- Transaction currency differences

### Use Case
POS programmers debugging why Card A works at a terminal but Card B doesn't. The compare view highlights exactly which EMV parameters differ.

---

## v2.2 — Advanced EMV Decoding

### Application Usage Control (tag 9F07)
Decode the 2-byte AUC into human-readable flags:
- Domestic / International
- Cash / Cashback / Goods / Services
- At ATM / Not at ATM

### Issuer Action Codes (tags 9F0D/9F0E/9F0F)
Decode Default/Denial/Online action codes — shows which conditions the issuer wants the terminal to go online, decline, or approve offline.

### Terminal Risk Management
Show the card's risk management data alongside decoded CVM list. Full picture of how the card would behave at a real terminal.

---

## v2.3 — Polish & Distribution

### Splash Screen
Android 12+ Splash API with the TapRead icon.

### Animated Card Theme Transition
When BIN lookup completes, the card face gradient cross-fades from brand fallback to bank theme over 200ms.

### App Icon Polish
Generate proper mipmap PNGs at every density from the vector source, or commission a professional icon.

### Signed Release
Generate a proper release keystore (not the dev one with password "deadboy"). Build a signed, optimized APK. Write release notes.

### GitHub Release
Tag v1.0.0 on GitHub. Attach the APK as a release asset. Write a proper release description with screenshots.

### F-Droid / IzzyOnDroid
Submit to F-Droid or IzzyOnDroid as an open-source NFC tool. No Google Play (we have no INTERNET permission and no ads, which makes F-Droid a better fit).

---

## Ideas Parking Lot (no timeline)

- **Card database cloud sync** — optional, requires INTERNET permission, user opt-in. Sync cards across devices via Firebase or self-hosted.
- **NFC card emulation** — Host Card Emulation (HCE) to replay a card's PPSE response for terminal testing. Legal grey area.
- **Raw APDU console** — type custom APDU commands and send them to the card. For advanced EMV debugging.
- **ISO 7816 T=0/T=1 support** — contact card reading via USB smart card reader (OTG).
- **QR code sharing** — generate QR code of card details for quick device-to-device transfer.
- **Card art recognition** — camera-based card design recognition to auto-theme even without BIN lookup.
- **Wear OS companion** — read cards with an NFC-enabled smartwatch.
- **Widget** — home screen widget showing last scanned card summary.

---

*Made with 💀 by [deadboy](https://github.com/deadboy18)*
