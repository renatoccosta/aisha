# Terms of Use for AI$HA

Last updated: March 7, 2026.

## 1. About AI$HA and these terms

AI$HA is an open source personal finance management application with AI features, distributed for self-hosted operation.

These Terms of Use describe the conditions for using the software and the boundaries of responsibility between:

- The open source project maintainers.
- The party that installs and operates a specific instance (the "Operator").
- End users of that instance.

By using AI$HA, you acknowledge these terms.

## 2. Open source licensing

AI$HA is distributed under the license defined in the repository `LICENSE` file.

Your rights to use, modify, and redistribute the software depend on that license. If there is any conflict between this document and the license, the license prevails.

## 3. Self-hosted model and responsibility

AI$HA is not offered by the project as a hosted SaaS.

In self-hosted deployments, the Operator is responsible for:

- Infrastructure, hosting, and security controls.
- Access management, backups, and disaster recovery.
- Compliance with applicable laws and regulations.
- Defining acceptable use and governance rules for users of that instance.

The project maintainers do not have automatic access to data stored in third-party self-hosted instances.

## 4. Intended use and financial nature of the software

AI$HA is designed to support personal finance management workflows such as tracking accounts, categories, entries, and reports.

The software and its AI-assisted features are provided for informational and operational support. They do not constitute legal, tax, accounting, investment, or fiduciary advice.

Users and Operators remain solely responsible for:

- Financial decisions made based on application outputs.
- Validating data quality and calculation expectations.
- Meeting jurisdiction-specific accounting, tax, and regulatory obligations.

## 5. AI-assisted features

AI$HA may provide local AI suggestions (for example, category suggestions) based on data available in the instance.

AI outputs can be incomplete or incorrect and must be reviewed by users before being relied on for financial records or decisions.

## 6. Security and authentication baseline

The application includes security controls in the current baseline, such as:

- Authenticated access for protected routes.
- CSRF protection for state-changing requests.
- Session controls (fixation protection, idle timeout, absolute timeout, concurrent session limits).
- Password hashing for local accounts.

Because AI$HA is self-hosted, the Operator must complement these controls with secure infrastructure practices.

## 7. Data and auditability

AI$HA is intended to support correctness and traceability of financial records.

Operators and users must avoid unauthorized or fraudulent use, including tampering with historical financial data, account ownership, or audit-relevant records.

## 8. Availability and support

The software is provided on an "as is" basis, under the terms of the project license.

The open source community and maintainers do not guarantee uninterrupted operation, error-free behavior, or suitability for every financial context.

## 9. Limitation of liability

To the maximum extent allowed by applicable law, project maintainers and contributors are not liable for direct or indirect losses arising from use, misconfiguration, or inability to use AI$HA.

This includes, without limitation, data loss, financial losses, compliance issues, and service interruption.

## 10. Changes to these terms

These terms may be updated to reflect legal, technical, or functional changes.

The "Last updated" date at the top indicates the currently published version.
