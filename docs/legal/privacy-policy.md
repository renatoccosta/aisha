# Privacy Policy for AI$HA

Last updated: March 7, 2026.

## 1. Scope of this policy

AI$HA is a personal finance management application with AI features, distributed for self-hosted use.

In this model, the party that installs and operates the instance (the "Operator") is responsible for defining the purpose of processing, the applicable legal basis, and the data retention period. The project team does not provide the software as a SaaS offering.

## 2. Data processed by the application

Depending on how an instance is used, the application may process the following data:

- Financial account data: title, description, initial balance, and initial balance date.
- Category data: title, description, and category hierarchy.
- Financial entry data: account, movement and settlement dates, description, category, notes, amount, and external identifier (when provided).
- Local authentication data: username, password hash (BCrypt), and account enabled status.
- Federated identity data (when OAuth2/OIDC is enabled): provider, provider subject, email, and link creation timestamp.
- Technical session/request data: session identifier, source IP address, and `X-Correlation-Id` header (or an automatically generated ID).

The application does not store passwords in plain text.

## 3. Purposes of processing

Data is processed to:

- Enable authentication and access control.
- Register, categorize, query, import, and maintain financial entries.
- Generate financial dashboards and reports.
- Provide local AI category suggestions to support entry classification.
- Support operational security, traceability, and incident diagnosis through technical logs.

## 4. How authentication works

AI$HA supports local authentication with server-side sessions and can support federated OAuth2/OIDC login depending on Operator configuration.

Security controls currently implemented include:

- Form login (`/login`) and logout (`/logout`).
- Global CSRF protection.
- Session fixation protection (session migration after login).
- Idle timeout of 45 minutes.
- Absolute session timeout of 12 hours.
- Maximum of 1 concurrent session per user (new login replaces the previous one).
- Session cookie with `HttpOnly` and `SameSite=Lax`.
- In `prod` profile, cookie `Secure=true`.

## 5. Logging and traceability

The application records technical events required for operations and security, including:

- Authentication success and failure.
- Logout.
- Absolute session timeout.
- Unhandled errors returned to users.

Logs include technical metadata such as username (when applicable), source IP, request method/path, exception type and stack trace, plus correlation ID.

The application is designed not to log passwords.

## 6. AI usage and data minimization

Category suggestions are generated locally on the application instance, based on entries and categories available in that instance database.

There is no automatic transmission of financial data to external AI services in the current default flow.

## 7. Data sharing

In standard self-hosted mode, the application does not automatically share personal data with third parties.

When the Operator enables federated login (for example, OIDC), authentication data is exchanged with the selected provider to enable sign-in.

## 8. Retention and deletion

Data retention depends on the Operator's governance and configuration decisions.

The software provides data maintenance and deletion operations in the application domain, respecting referential integrity and transactional consistency rules.

## 9. Security

AI$HA adopts technical safeguards in the codebase to reduce unauthorized access risk and support auditing, including:

- BCrypt password hashing.
- Session controls described in this policy.
- Backend input validation.
- Versioned schema evolution through Flyway migrations.
- Correlation IDs for incident traceability.

No safeguard is absolute. The Operator must also adopt infrastructure, network, backup, and credential management best practices.

## 10. Data subject rights and contact

Because AI$HA is self-hosted, requests from data subjects (access, correction, deletion, portability, and other rights under applicable law) must be addressed to the specific instance Operator.

The open source project team has no automatic access to data stored in self-hosted instances.

## 11. Changes to this policy

This policy may be updated to reflect legal, technical, or functional changes. The "Last updated" date at the top indicates the current version.
