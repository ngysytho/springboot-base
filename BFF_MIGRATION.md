# BFF authentication migration

## Architecture and endpoints

Browser → Spring Security OAuth2 Login → Keycloak `mpl-one` → backend code
exchange → server session. No application response, cookie or redirect contains
an OAuth access token, refresh token or ID token. Only Spring's opaque session
identifier is sent as an HttpOnly cookie. Safe profile fields are subject,
username, displayName and ROLE_-prefixed application roles, preserving `/me`.

- GET `/oauth2/authorization/mpl-bff`: login navigation.
- GET `/login/oauth2/code/mpl-bff`: framework callback, state/nonce validation,
  token exchange, session ID rotation; redirects to `/api/v1/auth/me`.
- GET `/api/v1/auth/me` and `/api/v1/me`: safe profile; no session gives JSON 401.
- GET `/api/v1/auth/csrf`: public, session-bound masked CSRF token, headerName and
  parameterName. Response is not cacheable. Resolving the token materializes
  Spring Security's deferred token.
- POST `/logout`: CSRF required; destroys session and its authorized client,
  clears cookies, then redirects to Keycloak logout. It uses client_id and an
  explicitly configured post_logout_redirect_uri, without id_token_hint, so no
  JWT reaches the browser. Keycloak may ask for logout confirmation. Local
  logout has already completed even if the user cancels that confirmation.

CSRF uses HttpSessionCsrfTokenRepository and XorCsrfTokenRequestAttributeHandler.
Send the masked value returned by the endpoint unchanged, including for POST,
PUT, PATCH and DELETE. Fetch a fresh token after login/logout and do not blindly
retry failed mutations. Login redirects and callback URLs contain authorization
codes/state as required by OIDC, never OAuth tokens.

## Keycloak Admin Console

1. Select existing realm `mpl-one`. Create/update `mpl-bff`, protocol OpenID
   Connect, client authentication ON, standard flow ON, implicit flow OFF,
   direct access grants OFF, service accounts OFF.
2. Use exactly `http://localhost:8081/login/oauth2/code/mpl-bff` as the local
   valid redirect URI and `http://localhost:8081/` as the valid post-logout URI.
   Configure exact HTTPS equivalents for production. No wildcard redirects.
3. Copy the confidential client secret into backend-only
   `KEYCLOAK_BFF_CLIENT_SECRET`. Do not put it into frontend build variables.
   The local realm import references this environment variable; Compose supplies
   it to Keycloak and the backend. Existing realms are NOT updated by startup
   import: apply these settings manually for existing installations.
4. Assign profile, email and roles default client scopes. Retain existing realm
   roles PLATFORM_ADMIN, ORG_ADMIN, TEACHER, STUDENT, PARENT, BUSINESS_MANAGER.
   Ensure realm roles and `mpl-api` client roles are included in access-token
   realm_access/resource_access claims. Add an audience mapper with included
   client audience `mpl-api`, access token ON, ID token OFF.
5. Keep `mpl-web` and `mpl-api`; they remain in the import. The old Swagger/browser
   Bearer flow has been removed. Inspect external consumers before retiring
   these clients. `mpl-api` remains the validated access-token audience.

## Environment and deployment

Local: activate `local`, configure DB_PASSWORD, DB_USERNAME and
KEYCLOAK_BFF_CLIENT_SECRET. Local cookies permit HTTP; other profiles default to
Secure cookies. Sessions expire after 30 idle minutes. Configuration never
contains a literal BFF secret. Existing demo credentials are local-only.

KEYCLOAK_ISSUER must be the browser-visible realm URL, locally
http://localhost:8180/realms/mpl-one. KEYCLOAK_BACKCHANNEL is the backend-reachable
realm URL for token/userinfo calls; Compose uses http://keycloak:8080/realms/mpl-one.
KEYCLOAK_JWKS is the backend-reachable certificates URL. Issuer validation always
uses KEYCLOAK_ISSUER, even with internal backchannel addresses. Explicit provider
URLs avoid startup discovery through the container's localhost.

Production: activate `production`, set HTTPS issuer/backchannel/JWKS endpoints,
POST_LOGOUT_URI to an exactly registered public HTTPS URL, REDIS_HOST,
REDIS_PORT (default 6379), REDIS_USERNAME and REDIS_PASSWORD. Redis TLS is enabled.
RedisSessionConfig shares both security context and authorized-client state via
Spring Session with namespace mpl:one:sessions and a 30-minute idle TTL. Local
uses servlet sessions without Redis. Production cookies explicitly use Secure,
HttpOnly, SameSite=Lax and path /. Use consistent application versions across
instances because session objects use Java serialization.

Deploy frontend and BFF behind the same public HTTPS origin. Proxy `/api/`,
`/oauth2/`, `/login/oauth2/`, `/logout` and session cookies without caching.
Preserve the public host/scheme. When TLS terminates at a trusted proxy, configure
SERVER_FORWARD_HEADERS_STRATEGY=framework and ensure that proxy strips client
Forwarded/X-Forwarded-* headers and the backend is not directly reachable.
Do not enable forwarded header trust on an exposed server.

Separate local frontend origins require explicit CORS_ALLOWED_ORIGINS such as
http://localhost:3000 and credentials: include. No wildcard origins. Use top-level
navigation to the BFF for login and form submission to the BFF for logout.
SameSite=Lax assumes same-site origins; use a same-origin dev proxy when possible.

## Frontend integration

No matching MPL frontend was found in this workspace, so unrelated frontend
projects were left unchanged. Remove keycloak-js/token storage and Bearer headers
when connecting your frontend. Example same-origin integration:

```js
export const login = () => window.location.assign('/oauth2/authorization/mpl-bff');
export async function bffFetch(url, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers);
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const response = await fetch('/api/v1/auth/csrf', {
      credentials: 'same-origin', cache: 'no-store'
    });
    if (!response.ok) throw new Error('Cannot obtain CSRF token');
    const csrf = await response.json();
    headers.set(csrf.headerName, csrf.token);
  }
  return fetch(url, {...options, method, headers, credentials: 'same-origin'});
}
export async function logout() {
  const response = await fetch('/api/v1/auth/csrf', {
    credentials: 'same-origin', cache: 'no-store'
  });
  if (!response.ok) throw new Error('Cannot obtain CSRF token');
  const csrf = await response.json();
  const form = document.createElement('form');
  form.method = 'POST'; form.action = '/logout';
  const input = document.createElement('input');
  input.type = 'hidden'; input.name = csrf.parameterName; input.value = csrf.token;
  form.append(input); document.body.append(form); form.submit();
}
// Profile: (await (await bffFetch('/api/v1/auth/me')).json()).data
```

Logout uses navigation so the browser can follow the cross-origin Keycloak
redirect; fetch-based logout would encounter cross-origin redirect restrictions.
The CSRF token above is not an authentication/OAuth token.

## Files and architecture

```text
src/main/java/vn/mpl/one/
  config/
    SecurityConfig.java          OIDC chain, JSON errors, CSRF, logout, JWT validator
    OAuth2ClientConfig.java      Authorized clients stored in the HTTP session
    RedisSessionConfig.java      Production shared sessions and secure cookie
    CorsConfig.java              Explicit credentialed origins and CSRF header
    OpenApiConfig.java           Session authentication documentation
  core/security/
    AuthenticatedUser.java       Safe identity data with displayName
    CurrentUserProvider.java     Application identity abstraction
    OidcCurrentUserProvider.java Session OIDC principal adapter
    OidcAuthoritiesMapper.java   Validated server-side access-token roles
    KeycloakJwtRoleConverter.java Existing realm/mpl-api role extraction
  modules/identity/api/controller/
    AuthController.java         Safe profile and CSRF endpoints
    MeController.java           Existing compatible /api/v1/me
src/main/resources/
  application.yml               OIDC, session, environment configuration
  application-local.yml         HTTP cookie development override
  application-production.yml    Redis TLS configuration
  keycloak/mpl-one-realm.json    Confidential mpl-bff client
```

Additional changed files: pom.xml (OAuth2 client and Redis dependencies),
docker-compose.yml, .env.example, README.md, BFF_MIGRATION.md, and
scripts/smoke_local.py (retired legacy Bearer entry point).

The existing TenantAuthorizationService, TenantMembershipLookup and organization
repository adapters remain unchanged. Lesson method authorization still requires
application roles, active database membership, and organization-scoped resources.
No migration or business module was replaced. Device pairing/registration are
extension interfaces only in this repository; there are no implemented public
TV routes or independent device/mobile Bearer chain to preserve. Future device
routes must have their own credential/authorization policy, not teacher cookies.
Unknown routes remain protected; no speculative public TV whitelist was added.

## Verification and remaining considerations

Per request, no build, application startup or tests were run, and no test changes
were retained. This is an unverified code migration. Existing JWT HTTP tests and
the former CurrentUserProvider concrete-class fixtures need migration before
running the test suite. The historical verification reports do not validate BFF.

Protect Redis with private network access, restricted ACLs, TLS and encrypted
storage/backups. Sessions contain credentials: never log or dump session objects,
token responses or OAuth principals. Keep security/HTTP wire logging disabled.
Session role snapshots are established at login; role changes/global revocation
need reauthentication or explicit session revocation. Back-channel logout and
proactive token refresh are not implemented. No outbound delegated API currently
requires refresh; add an authorized-client manager before introducing one.
The session idle TTL is not an absolute login lifetime. Production should decide
an absolute reauthentication policy and support immediate session revocation.

The implementation uses Spring Security's OAuth2 Login facilities:
https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html
