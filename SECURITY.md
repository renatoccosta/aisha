# Security Policy

Thank you for helping keep **AI$HA** secure.

AI$HA is open source and commonly deployed as a self-hosted service. Responsible vulnerability reporting helps protect every operator and user.

## Supported Versions

Security fixes are applied to the latest code on the default branch (`main`) and to the latest published release.

| Version line | Supported |
| --- | --- |
| `main` (development) | ✅ |
| Latest stable release | ✅ |
| Older releases | ❌ |

If you operate an older release, upgrade to the latest stable version before requesting support.

## Reporting a Vulnerability

Please **do not disclose vulnerabilities publicly** until remediation is available.

### Preferred private channels

- Open a **GitHub Private Vulnerability Report** in the Security tab of this repository.
- If that channel is unavailable, contact maintainers privately and request a secure channel for details exchange.

### What to include in a report

Provide as much detail as possible:

- Vulnerability type and affected component(s)
- Exact impacted versions / commit references
- Reproduction steps and prerequisites
- Proof of concept (minimal and safe)
- Security impact assessment (confidentiality/integrity/availability)
- Suggested mitigation or fix (optional)

## Coordinated Disclosure Process

After receiving a valid report, maintainers will:

1. Acknowledge receipt
2. Triage and confirm impact
3. Prepare and validate a fix
4. Coordinate disclosure timing with the reporter
5. Publish a patch and release notes

### Target response windows

- Acknowledgement: within **3 business days**
- Initial triage decision: within **7 business days**
- Status updates during investigation: at least every **14 days**

Complex issues may require more time. We will communicate delays and next steps.

## Scope

This policy covers this repository and official artifacts, including:

- Backend and frontend application code
- Authentication/session/security controls
- Build, container, and deployment configuration
- Dependencies when their usage introduces direct risk in AI$HA

Out-of-scope reports may include:

- Missing best-practice hardening that does not create an exploitable vulnerability by itself
- Vulnerabilities only present in unsupported, modified, or end-of-life environments

## Safe Harbor

We support good-faith security research. If you follow this policy and avoid privacy violations, service disruption, or data destruction, we will treat your report as authorized research.
