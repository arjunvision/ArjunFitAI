# Security policy

## Reporting a vulnerability

Please **do not** open a public issue for a security problem. Email
**[SECURITY CONTACT EMAIL — to be added by the maintainer]** with:

- what you found and where (file, screen or build),
- steps to reproduce,
- what an attacker could do with it.

You'll get an acknowledgement within 7 days. Please give us a reasonable time to fix it before telling
anyone else.

## Supported versions

ArjunFit is in development (0.x). Only the latest commit on `main` receives fixes.

## How the project protects secrets

- **Never committed:** API keys, `.env` files, `local.properties`, keystores (`*.jks`, `*.keystore`,
  `*.p12`), the Samsung Health SDK AAR, real health records or screenshots of them. `.gitignore`
  blocks the file types.
- **Secret scanning:** GitHub Actions runs [Gitleaks](https://github.com/gitleaks/gitleaks) over the
  whole history on every push and pull request.
- **Claude API key:** never built into the app. Each user enters their own key; it is encrypted on the
  phone with an Android Keystore key (AES-GCM). Before distributing the app to people without their own
  key, the key must move to a server-side proxy. Anthropic recommends keeping API keys in secure
  server-side storage: https://platform.claude.com/docs/en/manage-claude/authentication
- **Release signing (planned):** the signing key will live only in GitHub Secrets, never in the
  repository or logs.
- **Dependencies:** Dependabot opens update pull requests; each one runs CI and is reviewed by a person.

## If a key leaks

1. Revoke it immediately (for Claude: console.anthropic.com › API keys).
2. Create a new key and update where it is used.
3. Removing it from Git history is not enough on its own; a leaked key must always be revoked.
