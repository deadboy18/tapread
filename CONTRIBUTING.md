# Contributing to TapRead

Thanks for your interest! Here's how to contribute.

## Reporting Issues
- Include your device model and Android version
- Paste the APDU log from the LOG tab (use the share button)
- Describe what you expected vs what happened

## Pull Requests
1. Fork the repo
2. Create a feature branch (`git checkout -b feature/bin-lookup`)
3. Make your changes
4. Test on a real device with a real card (emulators don't have NFC)
5. Submit a PR with a clear description

## Code Style
- Kotlin, following Android conventions
- ViewBinding everywhere (no findViewById)
- Reflection-defensive when accessing devnied library internals
- No INTERNET permission — this is a hard rule

## What We Need Help With
- Testing with cards from different countries/banks
- BIN database curation (especially non-Malaysian banks)
- MIFARE Classic / Touch 'n Go sector analysis
- Translations
- UI/UX improvements

## Security
If you find a security issue, email directly instead of opening a public issue.
