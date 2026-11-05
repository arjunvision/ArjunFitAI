# Privacy design

This is how ArjunFit handles personal and health data today (v0.9.0-alpha2), and the rules future
features must follow. It is a design document for contributors, **not** the user-facing privacy policy;
a published privacy policy is required before any Play Store release.

## Today

| Data | Where it lives | Leaves the phone? |
|---|---|---|
| Samsung Health data (steps, heart rate, sleep, SpO2, weight, exercise, food, water) | Read on demand; not copied into the app's databases | Only inside the AI Coach summary, after consent |
| Food, water and weight you log | `arjunfit-log.db` on the phone | No |
| Workouts | `arjunfit-workouts.db` on the phone | No |
| AI Coach chat | `arjunfit-coach.db` on the phone | Recent messages go to Claude when you send one |
| Name, goals, settings | DataStore on the phone | No |
| Claude API key | Encrypted with an Android Keystore key | Sent only to Anthropic's API, as authentication |

- **Read-only health access.** ArjunFit never requests WRITE permission for Samsung Health (or, later,
  Health Connect).
- **No cloud backup.** `android:allowBackup="false"`.
- **AI Coach consent.** Nothing is sent before you agree. The coach receives a short summary (no name,
  no raw records, no exact times) plus the recent chat. "What the coach sees" shows the exact text.
  "Clear chat" deletes the history; "Remove key" deletes the key.
- **No analytics, ads or tracking SDKs.**

## Rules for new features

1. Private by default. Nothing is shared or uploaded without a clear, specific opt-in.
2. Ask only for the permissions a visible feature needs, at the moment it needs them.
3. GPS routes: private by default, with privacy zones around places the user marks; exact routes are
   never public by default.
4. Social features (planned): explicit consent before anything appears in a feed or leaderboard;
   Google Play forbids "socially sharing sensitive data without explicit user consent".
5. Users can delete their data. A "Delete all my data" control is planned before release.
6. No health data to advertising platforms or data brokers, ever (Health Connect policy).
7. Children: India's DPDP Rules require verifiable parental consent for users under 18 before any
   account or upload feature launches.

## References

- Health Connect policy: https://support.google.com/googleplay/android-developer/answer/9888170
- Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- DPDP Rules 2025 (PIB): https://static.pib.gov.in/WriteReadData/specificdocs/documents/2025/nov/doc20251117695301.pdf
