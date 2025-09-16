# CAPABILITIES TOKENS — IMPLEMENTATION PLAN (v1, fixed signing key)

This document describes how to gate SDK features with **locally verifiable capabilities tokens** (JWT-style), issued from an API key. It avoids per-feature API calls, supports **retroactive activation**, and (for v1) uses a **fixed signing key** (no JWKS/rotation yet).

---

## 1) Goals & constraints

* **Local checks**: SDK decides “has feature X?” without calling the API.
* **Retroactive changes**: Flip features on/off server-side; clients pick up on next token refresh.
* **No hardcoding**: SDK reads feature flags from the token, not compiled logic.
* **v1 simplification**: Single fixed Ed25519 signing key (rotate manually when needed).

---

## 2) Key material (one-time setup)

Generate an **Ed25519** key pair (preferred: small, fast):

```bash
# Private key (PEM)
openssl genpkey -algorithm ed25519 -out ed25519_private.pem
# Public key (PEM)
openssl pkey -in ed25519_private.pem -pubout -out ed25519_public.pem
```

* Store `ed25519_private.pem` in the server’s secret manager.
* Bake `ed25519_public.pem` into the SDK(s) (or ship as a resource file).
* Record a `kid` (e.g., `sigkey_v1_fixed`) for the JWT header, even if fixed.

---

## 3) Data model

**Table: `api_keys`**

* `id` (uuid), `prefix` (short), `secret_hash` (Argon2id or HMAC-SHA256), `customer_id`
* `status` (`active|revoked`), `created_at`, `last_used_at`, `notes`

**Table: `entitlements`** (per `customer_id` or per `api_key_id`)

* `features` (string\[]), `plan`, `rate_limit_rpm`, `burst`, optional `bindings` (e.g., bundle ids)
* Optional: `min_cap_token_iat` (timestamp) for targeted invalidation

---

## 4) Token format (JWT claims)

Header:

```json
{ "alg": "EdDSA", "typ": "JWT", "kid": "sigkey_v1_fixed" }
```

Claims (example):

```json
{
  "iss": "https://api.example.com",
  "sub": "key_9Yf4...",            // api_key.id (or stable key_id)
  "aud": "your_sdk",
  "iat": 1736448000,
  "nbf": 1736448000,
  "exp": 1736620800,               // ~48h
  "plan": "pro",
  "features": ["metrics.raw","metrics.premium_focus","sdk.offline_cache"],
  "ratelimit": {"rpm": 600, "burst": 120},
  "binding": {"bundle_id": "com.acme.app", "sdk_min": "2.7.0"},
  "jti": "cap_9K3a..."             // random id for traceability
}
```

**TTL recommendation**: 24–72h. Start with **48h**.

---

## 5) Server endpoints

### 5.1 `POST /auth/capabilities`

* **Auth**: `Authorization: Bearer <API_KEY>`
* Steps:

    1. Parse/validate API key. Lookup by `prefix`, verify `secret_hash`, check `status`.
    2. Load entitlements (by customer or key).
    3. Create claims (above). Compute `exp = now + 48h`.
    4. Sign with Ed25519 private key.
    5. Return JSON:

       ```json
       { "token": "<JWT>", "expires_in": 172800 }
       ```

**Example (curl):**

```bash
curl -X POST https://api.example.com/auth/capabilities \
  -H "Authorization: Bearer qa_live_abc123..." \
  -H "Content-Type: application/json"
```

### 5.2 (Optional but recommended) `GET /auth/config`

Lightweight config to accelerate revocations/rollouts.

Response:

```json
{
  "force_refresh_after": "2025-09-10T12:00:00Z",
  "per_key": {
    "key_9Yf4...": {"min_cap_token_iat": "2025-09-11T09:00:00Z"}
  }
}
```

* SDK checks daily (or every 6–12h). If `token.iat < min_cap_token_iat` (for its key) or `< force_refresh_after`, it refreshes now.

---

## 6) SDK responsibilities

### 6.1 Bootstrap

* Load cached token from secure storage (Keychain/Keystore, encrypted file).
* If **missing**, **expired**, or **<6h** to expiry → call `/auth/capabilities`.
* Verify token:

    * Check `alg=EdDSA`, `kid=sigkey_v1_fixed`.
    * Verify signature with baked **public key**.
    * Check `nbf <= now < exp`, `aud == "your_sdk"`.
    * Optionally check `binding.bundle_id` matches app and `sdk_min` satisfied.

### 6.2 Feature checks

Provide a tiny API:

```ts
hasFeature("metrics.premium_focus"); // boolean
getRateLimit();                      // from token.ratelimit
```

**Rule**: Only allow features present in `claims.features`.

### 6.3 Refresh strategy

* Refresh on:

    * Init when exp near (e.g., < 6h).
    * Any 401/403 from protected API calls.
    * `auth/config` indicates a forced refresh.
* If offline with a still-valid token → proceed.
* If offline and token expired → degrade gracefully (disable gated features, keep core).

### 6.4 Telemetry (optional)

Batch anonymous counters: token age at use, feature usage, failures (helps detect abuse/misconfig).

---

## 7) Security notes

* **Never store API key in logs**. Log only `key_id` and `jti`.
* Store only **hash** of API key. Support multiple active keys per customer for rotation.
* Bind tokens to **package/bundle id** where possible.
* Keep tokens compact (<2 KB).
* Rate-limit `/auth/capabilities` (e.g., 10/min per key).

---

## 8) Rollout plan

1. **Server**

    * Add `api_keys`, `entitlements` tables/migrations.
    * Implement `/auth/capabilities` (+ optional `/auth/config`).
    * Generate Ed25519 key pair; load private key from secret manager.
    * Unit/integration tests for key verification, claim building, and signing.

2. **SDK**

    * Embed public key.
    * Add token storage, verification, refresh logic, and `hasFeature()` helper.
    * Add optional `auth/config` polling.

3. **Back-office**

    * Admin UI/CLI to edit entitlements (features/plan/limits).
    * Ability to set `min_cap_token_iat` for a key (for immediate invalidation).

4. **Docs**

    * List canonical **feature flag names** and semantics.
    * Document client upgrade path for future rotation/JWKS (v2).

---

## 9) Testing checklist

* [ ] Issue token for key with features A,B → SDK sees A,B.
* [ ] Remove B → SDK still shows B until refresh; after refresh, B is gone.
* [ ] Add C → SDK gains C after refresh.
* [ ] Expired token blocks gated features.
* [ ] Binding mismatch (wrong bundle id) fails verification.
* [ ] Force refresh via `/auth/config` works.
* [ ] Revoked key (status=revoked) cannot obtain token.
* [ ] Clock skew: accept ±120s.
* [ ] Malformed token/signature → rejected.

---

## 10) Example code snippets

### 10.1 Server (Python, `pyjwt[crypto]` + `cryptography`)

```python
import time, json
from datetime import timedelta, datetime, timezone
import jwt  # PyJWT 2.x

with open("ed25519_private.pem","rb") as f:
    PRIVATE_KEY = f.read()

def issue_capabilities_token(key_id, entitlements):
    now = int(time.time())
    exp = now + 48*3600
    claims = {
        "iss": "https://api.example.com",
        "sub": key_id,
        "aud": "your_sdk",
        "iat": now,
        "nbf": now,
        "exp": exp,
        "plan": entitlements.plan,
        "features": entitlements.features,
        "ratelimit": {"rpm": entitlements.rpm, "burst": entitlements.burst},
        "binding": entitlements.binding,
        "jti": f"cap_{key_id}_{now}"
    }
    token = jwt.encode(
        claims, PRIVATE_KEY, algorithm="EdDSA",
        headers={"kid": "sigkey_v1_fixed"}
    )
    return {"token": token, "expires_in": exp - now}
```

### 10.2 SDK (TypeScript)

```ts
import * as jose from "jose"; // npm i jose

const publicKeyPem = `-----BEGIN PUBLIC KEY-----
...your Ed25519 public key...
-----END PUBLIC KEY-----`;

let publicKey: CryptoKey;

export async function initVerifier() {
  publicKey = await jose.importSPKI(publicKeyPem, "Ed25519");
}

export async function verifyToken(jwt: string) {
  const { payload, protectedHeader } = await jose.compactVerify(jwt, publicKey);
  const header = JSON.parse(new TextDecoder().decode(protectedHeader));
  const claims = JSON.parse(new TextDecoder().decode(payload));
  if (header.alg !== "EdDSA" || header.kid !== "sigkey_v1_fixed") throw new Error("bad header");
  const now = Math.floor(Date.now()/1000);
  if (claims.nbf && claims.nbf > now) throw new Error("nbf");
  if (claims.exp && claims.exp <= now) throw new Error("exp");
  if (claims.aud !== "your_sdk") throw new Error("aud");
  return claims;
}

export function hasFeature(claims: any, name: string): boolean {
  return Array.isArray(claims.features) && claims.features.includes(name);
}
```

---

## 11) Operational guidance

* **TTL**: Start at 48h. If you need faster revocations without calls, reduce TTL; or use `/auth/config` with low polling cost.
* **Manual rotation (v1)**: If the key leaks, **ship a new SDK with a new public key** and update server to sign with the new private key (switch `kid` if you add a transitional period). This is the main downside of fixed keys—plan a v2 with JWKS.
* **Observability**: Log `jti`, `sub`, `exp`, `issued_for` (customer id) on issuance. Dashboard metrics: token issue rate, failure reasons, feature adoption.

---

## 12) Future (v2+) enhancements

* JWKS & automated key rotation (multiple `kid`s).
* Macaroons for derivable attenuated tokens (if you need sub-scoping).
* Per-feature offline license files for long offline periods.
* Signed usage receipts (anti-abuse).
* Multi-tenant issuer separation if you host for partners.

---

## 13) Quick reference (copy/paste)

* **Feature check in SDK**: `hasFeature(claims, "metrics.premium_focus")`
* **Refresh when**: missing token, `<6h` to expiry, 401/403, or `/auth/config` says so.
* **Server**: `POST /auth/capabilities` → `{ token, expires_in }`
* **Signing**: Ed25519, header `{ alg:"EdDSA", kid:"sigkey_v1_fixed" }`
* **Claims**: `iss, sub, aud, iat, nbf, exp, features[], plan, ratelimit, binding, jti`
* **Storage**: cache token securely; store only API key hashes server-side.

---

**Done.** This is a production-ready v1 plan with minimal moving parts and a clean upgrade path to rotation/JWKS later.
