JWT Test Issuer
===============

A minimal JWT provider for testing Opencast's [JWT support](../../guides/admin/docs/configuration/security.jwt.md).
It signs arbitrary payloads with its own key and serves the public key as JWKS.

**Testing only!** Signs arbitrary user provided payloads.

Run (Node 16+):

```sh
node jwt-test-issuer.js
```

The default port is 3180. The key is generated on first start and stored as `jwt-test-issuer-key.pem` next to the
script.
Configure Opencast to use it in `etc/security/mh_default_org.xml`:

```xml
<property name="jwksUrl" value="http://localhost:3180/.well-known/jwks.json" />
```

JWTs can be created like this:

```sh
curl -d '{"exp": 2000000000, "sub": "jane", "name": "Jane Doe", "oc": {"e:EVENT-ID": ["read"]}}' http://localhost:3180/jwt
```

The response is the JWT. The server allows all cross-origin requests.
