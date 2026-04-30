# Certificate Pinning — Proposed Plan (NOT YET IMPLEMENTED)

Status: **proposal only**. No production networking has been changed by this
document. The audit flagged the absence of certificate pinning as a P1 issue
(`network_security_config.xml` and `ApiClient.kt` rely solely on the system
trust store). Pinning is high-leverage but **dangerous to ship without
rotation infrastructure** — a misconfigured pin set bricks every installed
client until the next forced update. This document specifies the exact
prerequisites, change set, rollout, and rollback so the team can decide
whether to proceed.

## Why pinning was deliberately not landed in this commit batch

The user's hardening request explicitly said:

> Add certificate pinning only if we can manage pin rotation safely;
> otherwise propose the exact pinning plan before changing production
> networking.

Pin rotation requires:
1. A documented inventory of which leaf / intermediate / root the pins target.
2. At least one **backup pin** that is not yet served by the live edge so
   rotation can be performed by switching the active certificate to the
   backup-pinned key without a client release.
3. A monitoring path for pin failures (otherwise a botched rotation looks
   like a network outage).
4. A revocation path (how do we ship a fix if a pinned key is compromised
   or expires while clients are pinned to it).

None of those are visible in this repository today. Until they are, shipping
pins is a higher risk than the MITM threat they mitigate for our target user
base (consumer mobile, mostly mobile networks, no documented enterprise
proxy population).

## Pinning target — what to pin

**Recommendation: pin the SubjectPublicKeyInfo (SPKI) of two intermediate
certificates, not leaf certificates.**

- Leaf-cert pinning rotates every 60–90 days (Let's Encrypt) or on any
  cert reissue. That cadence is incompatible with a mobile release cadence
  measured in weeks. A leaf-pin rotation that misses a single client
  release bricks everyone on that release.
- Intermediate-cert pinning rotates on the order of years (root program
  driven). Two pins: the active intermediate + a backup intermediate.

Concrete proposal, assuming `*.pacedream.com` is fronted by a managed CDN
(Cloudflare / AWS CloudFront / Fastly):

| Pin slot | Source                                | Rotation cadence  |
|----------|---------------------------------------|-------------------|
| Primary  | Active intermediate of the live cert  | Years             |
| Backup   | Backup intermediate held in CDN config but not currently served | Pre-loaded so rotation is a server-side switch |

Pre-flight task: confirm with platform engineering which CA / intermediate
chain `pacedream.com` is currently served from. This determines the SHA-256
hashes we embed.

## Change set (when prerequisites are met)

### 1. `app/src/main/res/xml/network_security_config.xml`

Add a `<pin-set>` to the `pacedream.com` `<domain-config>`:

```xml
<domain-config>
    <domain includeSubdomains="true">pacedream.com</domain>
    <domain includeSubdomains="true">www.pacedream.com</domain>
    <trust-anchors>
        <certificates src="system" />
    </trust-anchors>
    <!-- Pin SPKI of intermediate CAs. Two pins: active + backup. -->
    <pin-set expiration="2027-04-01">
        <pin digest="SHA-256">PRIMARY_INTERMEDIATE_SPKI_HASH_HERE</pin>
        <pin digest="SHA-256">BACKUP_INTERMEDIATE_SPKI_HASH_HERE</pin>
    </pin-set>
</domain-config>
```

The `expiration` attribute is critical — when the date passes the
network-security-config silently falls back to the system trust store
(no enforcement). This is the platform's built-in fail-open against
forgotten rotations and is exactly what we want for a first deployment.
Pick a date 12 months out and set a calendar reminder.

### 2. `app/src/main/kotlin/com/pacedream/app/core/network/ApiClient.kt`

Optional belt-and-braces: add an OkHttp `CertificatePinner` matching the
manifest pins. This is redundant with the manifest config on API 24+, but
it gives us a centralised place to attach a failure callback for telemetry:

```kotlin
private val certificatePinner = CertificatePinner.Builder()
    .add("pacedream.com", "sha256/PRIMARY_INTERMEDIATE_SPKI_HASH_HERE")
    .add("pacedream.com", "sha256/BACKUP_INTERMEDIATE_SPKI_HASH_HERE")
    .add("*.pacedream.com", "sha256/PRIMARY_INTERMEDIATE_SPKI_HASH_HERE")
    .add("*.pacedream.com", "sha256/BACKUP_INTERMEDIATE_SPKI_HASH_HERE")
    .build()

private val client: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .certificatePinner(certificatePinner)
        .addInterceptor(createLoggingInterceptor())
        .addInterceptor(createHtmlHardeningInterceptor())
        .build()
}
```

### 3. Telemetry

Wire SSL pin failures to Crashlytics/Firebase as a non-fatal event so
spikes are visible:

```kotlin
.addInterceptor { chain ->
    try {
        chain.proceed(chain.request())
    } catch (e: javax.net.ssl.SSLPeerUnverifiedException) {
        FirebaseCrashlytics.getInstance().recordException(
            RuntimeException("CertificatePinner failure", e)
        )
        throw e
    }
}
```

A 5-minute spike of these on a particular app version means we have
shipped a stale pin set and need to ship a hotfix immediately.

## How to obtain the SPKI hashes

Run from a machine that can reach the live host:

```bash
echo | openssl s_client -servername pacedream.com -connect pacedream.com:443 \
  -showcerts 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | openssl enc -base64
```

Repeat for each certificate in the chain (the leaf, the intermediate, and
the root) by saving the chain from `-showcerts` and feeding each cert
separately. Pin the **intermediate**, not the leaf. Capture the backup
intermediate the same way against the backup CDN/edge.

## Rollout plan

1. **Staging-only first.** Ship the pin set behind a build-config flag
   (`pinning_enabled = true` only for `staging`, `false` for `prod`).
   Run the staging app for at least one full release cycle. Confirm
   zero `SSLPeerUnverifiedException` spikes in Crashlytics.
2. **Limited production rollout.** Enable pinning for a 5% staged
   rollout via Play Store staged rollout. Monitor Crashlytics 24 h for
   a `SSLPeerUnverifiedException` spike.
3. **Full rollout** if step 2 is clean.
4. **Rotation drill** before any cert rotation: switch the live edge
   to the backup-pinned intermediate, observe no client breakage,
   switch back.

## Rollback plan

1. **Server-side first.** If clients start failing, switch the edge
   to serve a chain that contains a currently-pinned intermediate.
   This is the only fast fix because deployed clients cannot accept
   a new pin.
2. **Client-side, slower.** Ship a hotfix removing the broken pin.
   Cannot rely on this within minutes because of Play review and
   user adoption lag.
3. **Last resort.** The `expiration` attribute in
   `network-security-config` makes the manifest pins fail-open after
   the configured date. Set this conservatively (12 months) so a
   forgotten rotation degrades to "no pinning" rather than "no
   network".

## Decision items for product / platform engineering

Before this proposal moves to implementation, please confirm:

- [ ] Which CDN / edge fronts `pacedream.com` and `*.pacedream.com`.
- [ ] Whether platform engineering can hold a backup intermediate
      configured but not currently served.
- [ ] Whether Crashlytics non-fatal capture is enabled in production
      (currently `firebase_analytics_collection_deactivated` is true
      by default per the manifest — the prod flavour overrides this,
      verify before relying on it for pin telemetry).
- [ ] Calendar owner for the pin expiration / rotation reminder.

Once these are answered, the change set above is ~30 lines across two
files and one days' staging soak before staged rollout.
