# Security Policy

## Design Principles
- **No INTERNET permission** — the app cannot make network requests
- **No data exfiltration** — all card data stays on-device
- **Public data only** — reads the same data any POS terminal reads
- **No card cloning** — cannot write to cards or emulate cards

## Reporting a Vulnerability
If you discover a security vulnerability, please report it responsibly:
1. Do NOT open a public GitHub issue
2. Email the maintainer directly
3. Allow reasonable time for a fix before public disclosure

## Scope
This app reads publicly accessible EMV data per ISO/IEC 7816 and EMV specifications. 
No encryption is broken. No authentication is bypassed. No private keys are extracted.
