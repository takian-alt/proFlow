# Security Policy

## Supported Versions

Only the most recent release of proFlow receives security fixes.

| Version | Supported |
|---|---|
| 4.0.x (latest) | ✅ Yes |
| 3.0.x | ❌ No |
| 1.0.x | ❌ No |
| < 1.0 | ❌ No |

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub Issues.**

If you believe you have found a security vulnerability in proFlow, please disclose it responsibly by opening a [GitHub Security Advisory](https://github.com/takian-alt/proFlow/security/advisories/new). This creates a private channel between you and the maintainers so the issue can be assessed and patched before it is made public.

When reporting, please include as much of the following information as possible to help us understand and reproduce the issue:

- **Type of vulnerability** (e.g., data exposure, injection, insecure storage, broken authentication)
- **Location** — relevant file paths, class names, or code snippets
- **Proof of concept** — steps to reproduce, or a minimal reproducible example
- **Impact** — what an attacker could achieve by exploiting this vulnerability
- **Suggested fix** (optional, but welcome)

---

## Response Process

1. We will acknowledge your report within **72 hours**.
2. We will confirm whether the reported issue is a valid vulnerability within **7 days**.
3. We will work on a fix and keep you informed of progress.
4. Once the fix is released, we will credit you in the release notes (unless you prefer to remain anonymous).
5. We ask that you do not disclose the vulnerability publicly until we have released a fix.

---

## Security Considerations for Users

proFlow stores all task and preference data **locally on your device** using Room (SQLite) and Proto DataStore. There is no network communication or remote server. The application requests the following Android permissions:

| Permission | Required? | Purpose |
|---|---|---|
| `POST_NOTIFICATIONS` | ⚠️ Optional | Task reminders, daily planning, autonomy nudge, and streak alerts |
| `SCHEDULE_EXACT_ALARM` | ⚠️ Optional | Reliable delivery of time-sensitive notifications |
| `RECEIVE_BOOT_COMPLETED` | ✅ Yes | Restart scheduled workers after device reboot via `BootReceiver` |
| `VIBRATE` | ⚠️ Optional | Haptic feedback for notifications |
| `FOREGROUND_SERVICE` | ✅ Yes | Run HyperFocus monitor and unlock-timer services in the foreground |
| `FOREGROUND_SERVICE_SPECIAL_USE` | ✅ Yes | Declare `specialUse` foreground service subtype for unlock-countdown and session-monitor services |
| `QUERY_ALL_PACKAGES` | ✅ Yes (launcher) | Enumerate installed apps for the app drawer, dock, and folder grid |
| `BIND_APPWIDGET` | ⚠️ Optional | Embed Android home-screen widgets in launcher page slots |
| `PACKAGE_USAGE_STATS` | ⚠️ Optional | Power the Distraction Engine — user must grant via *Settings → Special App Access → Usage Access* |
| `USE_BIOMETRIC` | ⚠️ Optional | Biometric app-lock: authenticate before forwarding a locked-app launch intent |
| `USE_FINGERPRINT` | ⚠️ Optional | Legacy fingerprint fallback on pre-Android 9 devices |
| `READ_CONTACTS` | ⚠️ Optional | Surface contact shortcuts in the launcher (feature disabled if denied) |
| `EXPAND_STATUS_BAR` | ✅ Yes (launcher) | Allow swipe-down gesture on the home screen to expand the notification shade |
| `BIND_ACCESSIBILITY_SERVICE` | ✅ Yes (HyperFocus) | `AppBlockingService` — intercepts window-state events to redirect blocked apps during a HyperFocus session |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | ⚠️ Optional | `NotificationBadgeService` — counts unread notifications per package for badge overlays on app icons |

**HyperFocus unlock codes** are encrypted with AES-256 before being stored in the Room database (`unlock_codes` table). Keys are generated per session and are never written to external storage or transmitted off-device.

The home screen widget (`FocusWidgetProvider`) displays only the title of your current top-priority task on the home screen. No sensitive data beyond the task title is rendered outside the app.

---

## Scope

The following are in scope for security reports:

- Unauthorized access to or exfiltration of locally stored user data
- SQL injection or other data-layer vulnerabilities via Room
- Issues with exported Android components (Activities, Services, BroadcastReceivers, ContentProviders) that allow exploitation from other apps
- Insecure data storage (e.g., sensitive data written to external storage or shared preferences without encryption)
- Weaknesses in the AES-256 HyperFocus unlock-code generation, storage, or verification logic (`AESUtil`, `UnlockCodeRepository`)
- Privilege escalation or unintended capability grants via the `AppBlockingService` (AccessibilityService)

The following are **out of scope**:

- Vulnerabilities in third-party libraries (please report those directly to the library maintainers)
- Issues that require physical access to an unlocked device
- Denial-of-service attacks that require direct device access
- Social engineering attacks

---

Thank you for helping keep proFlow and its users safe.
