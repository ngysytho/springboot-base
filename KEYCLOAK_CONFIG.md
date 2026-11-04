# Keycloak configuration as code

## Scope and version pin

`src/main/resources/keycloak/mpl-one-realm.json` remains the single source of
truth for realm settings, roles and managed clients. The `keycloak-config`
Compose service applies it via the Keycloak Admin API, then exits. It is not a
Spring Boot dependency or a Flyway migration and does not run on API startup.
Keycloak's `--import-realm` and its import mount have been removed so there is
only one configuration writer. Existing PostgreSQL volumes are retained.

The official image is pinned to `adorsys/keycloak-config-cli:6.5.1-26.5.5` and its
multi-architecture index digest. Docker Hub metadata confirms both linux/amd64
and linux/arm64, supporting Intel and Apple Silicon Docker Desktop. This pin is
built for Keycloak 26.5.5; the repository retains Keycloak **26.8.1**. Upstream's
release CI matrix does not establish compatibility with 26.8.1. This is a
candidate integration, not a verified 26.8.1 pairing; no matching verified
release was found during implementation. Validate on staging before production,
particularly before adding newer features such as organizations. The release's
organization limitations are another reason not to expand the managed scope.

Sources consulted (metadata/documentation only, no container execution):

- [Pinned release CI matrix](https://github.com/adorsys/keycloak-config-cli/blob/v6.5.1/.github/workflows/ci.yaml)
- [Official image metadata](https://hub.docker.com/v2/repositories/adorsys/keycloak-config-cli/tags/6.5.1-26.5.5)
- [Release configuration defaults](https://github.com/adorsys/keycloak-config-cli/blob/v6.5.1/src/main/resources/application.properties)
- [Resource management](https://github.com/adorsys/keycloak-config-cli/blob/v6.5.1/docs/MANAGED.md)

## Developer workflow (macOS / Docker Desktop)

From `springboot-base`, copy `.env.example` to `.env` if it does not already
exist. Set backend/client and administrator credentials; for an existing realm,
use its existing BFF secret unless intentionally rotating it. Do not overwrite
an existing `.env`. Keep secrets out of Git. Use a long random hex or base64url
BFF secret: substitution occurs before JSON parsing, so raw quotes/backslashes
in secrets require JSON escaping. Do not use shell tracing or print rendered
Compose configuration to shared logs.

```sh
# Start persistent infrastructure; no reset, no realm import.
docker compose up -d app-db keycloak-db keycloak

# Waits up to 120 seconds for Keycloak, applies JSON and exits.
docker compose run --rm keycloak-config

# After editing the same realm JSON, apply again using the same command.
docker compose run --rm keycloak-config

# Optional: start the existing backend after a successful sync.
docker compose --profile api up -d --build api
```

These are commands for the developer to execute; they were not run during this
change. Stop on a nonzero sync exit code and inspect its error before starting
the backend. `run --rm` propagates the CLI exit status and removes only its
one-shot container. It does not remove database volumes. Do not use `down -v`
or realm deletion as a synchronization step. Run one synchronization at a time;
updates are not a database transaction and a failure can leave partial changes.

The service is behind the `keycloak-config` profile so ordinary `compose up`
does not trigger synchronization. Explicit `compose run` targets it without
needing to enable the profile. No port is published by the sync service.
Keycloak remains bound to `127.0.0.1:8180` for local browser/Admin Console access.
Its Admin API is reached internally at `http://keycloak:8080`. Production needs
a restricted administrative network and HTTPS; do not publish admin routes
through the public frontend reverse proxy.

Environment:

| Variable | Meaning |
|---|---|
| KEYCLOAK_CONFIG_URL | Internal Admin API base URL; default http://keycloak:8080 |
| KEYCLOAK_CONFIG_ADMIN | Optional sync operator; defaults to KEYCLOAK_ADMIN |
| KEYCLOAK_CONFIG_ADMIN_PASSWORD | Optional operator password; defaults to KEYCLOAK_ADMIN_PASSWORD |
| KEYCLOAK_CONFIG_FILES | Exact in-container JSON path; default /config/mpl-one-realm.json |
| KEYCLOAK_BFF_CLIENT_SECRET | Existing backend confidential client secret, required |
| KEYCLOAK_LOCAL_DEMO_PASSWORD | Disposable password, required only for explicit local seed |

Operator credentials authenticate in the master realm using admin-cli. Changing
bootstrap admin environment variables does not reset an existing administrator
password; supply credentials that actually exist. Prefer a dedicated sync
operator with permissions for the resources it manages in a deployed setup.
Only the base JSON is mounted for ordinary synchronization. To manage other
files, add explicit read-only mounts; avoid a recursive glob that could select
local users. The externally visible OIDC issuer, BFF callback, OAuth2 session
security and application configuration are unchanged.

## Add a role or update a client

Add an entry inside `roles.realm` in the existing JSON:

```json
{"name": "CONTENT_EDITOR", "description": "Manage editorial content"}
```

Apply with `docker compose run --rm keycloak-config`. Roles are matched by name;
clients by clientId, so repeated application updates the same resources rather
than inserting new ones. Edit the existing client entry to update its settings.
Preserve `mpl-bff` confidentiality, standard flow, exact callback/logout URIs,
disabled password/implicit grants and its audience mapper. Production URLs must
be exact HTTPS URLs. Never put a literal client secret in the JSON.

Creating a Keycloak role does not automatically grant application permissions:
the BFF intentionally allowlists its application roles, and method security and
PostgreSQL memberships still determine business access. New application roles
require a separate deliberate application authorization change.

## Non-destructive policy and limits

All supported `IMPORT_MANAGED_*` resource categories are explicitly `no-delete`.
The ordinary JSON has **no users section**, so ordinary sync neither updates
user passwords/profiles nor imports user role assignments. User role/group merge
settings are additive for the explicit local seed. Normal account registration,
password changes and user role management belong in an authorized Admin API
workflow, not this Git-managed realm configuration.

Removing an entry from JSON does not delete that resource from Keycloak.
Renaming a role/client creates a new identity and leaves the old one; it is not
a rename migration. Existing user role assignments remain on the old role.
Intentional deletion or migration requires a separate reviewed Admin API action.

`no-delete` prevents resource pruning; it does not mean all edits are additive.
Fields explicitly managed on a client/realm still overwrite those fields,
including redirect lists, scope lists and nested protocol mapper definitions.
Do not place real users, role mappings, composite changes or broad realm exports
into this file without reviewing their effect. Unrelated top-level unmanaged
resources are not intentionally pruned; additional nested client configuration
must be reviewed before adopting a manually configured client into Git.

Remote-state persistence is disabled to avoid storing a configuration snapshot
containing the BFF secret in realm attributes. The import cache is disabled so
an explicit run reconciles settings even when the file is unchanged, including
manual drift or secret rotation. No deletion tracking is needed for this policy.
HTTP wire and realm-configuration logging are OFF to avoid secret disclosure.
An environment-secret change can rotate the managed client secret on the next
run; update the backend consistently. Git rollback changes desired settings,
not database history, and does not remove resources introduced in later commits.

## Explicit local demo seed only

The only extra JSON is `keycloak/local/mpl-one-users.json`, a **users-only
fragment**, not a second roles/clients/realm-settings source. It preserves the
existing teacher/student IDs used by the local Flyway membership fixtures.
Passwords come from KEYCLOAK_LOCAL_DEMO_PASSWORD, never tracked credentials.

After applying the base realm, set that variable in `.env`, then explicitly run:

```sh
docker compose -f docker-compose.yml -f docker-compose.keycloak-local.yml \
  run --rm --no-deps keycloak-config
```

The override selects only the users fragment and adds its read-only mount.
Never combine this override with production credentials or a production URL.
It deliberately updates the named demo accounts on each execution, including
passwords, names and enabled state; existing extra role/group assignments merge.
If teacher/student usernames already belong to real users, do not execute it.
Existing accounts with different IDs are not recreated to match the fixture;
resolve their local membership mapping separately. Production users and real
passwords must never be added here. Ordinary synchronization does not seed users.

## Manual verification checklist (not executed)

1. Open http://localhost:8180/admin and select `mpl-one`.
2. Under Realm roles, inspect the six application roles plus DEVICE and any
   role you added. Under Clients, confirm mpl-web, mpl-api and mpl-bff remain.
3. Inspect mpl-bff settings, callback/logout URLs, credentials and audience
   mapper; confirm existing BFF login remains session-based.
4. Record existing users, role mappings and a manually created unmanaged role
   before applying; compare afterward. Check demo users only if you opted in.
5. Run the ordinary sync twice; check exit code zero and confirm no duplicate
   roles/clients or removed users/unmanaged resources in the Admin Console.
6. Review these checks on an isolated/staging realm before a production run.

No application, build, JSON validation command, Compose validation command,
container, synchronization or test suite was executed at the user's request.
Image metadata and source documentation were inspected only. Runtime readiness,
26.8.1 compatibility, repeatability and preservation remain unverified.

## Files changed

```text
springboot-base/
├── docker-compose.yml                      # One-shot service; remove startup import
├── docker-compose.keycloak-local.yml       # Explicit local user seed override
├── .env.example                            # Sync and local-seed variables
├── README.md                               # Updated developer startup workflow
├── BFF_MIGRATION.md                         # Updated realm application instructions
├── KEYCLOAK_CONFIG.md                      # This guide and limitations
└── src/main/resources/keycloak/
    ├── mpl-one-realm.json                   # Existing source; roles/clients, no users
    └── local/mpl-one-users.json             # Optional local users only
```

No Java security/business code, pom.xml, application YAML or Flyway migration
was modified for this task.
