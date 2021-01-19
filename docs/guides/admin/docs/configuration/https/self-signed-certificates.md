Self-Signed Certificates
========================

Since commit [4225bf](https://github.com/opencast/opencast/commit/4225bf90af74557deaf8fb6b80b0705c9621acfc)
([security advisory GHSA-44cw-p2hm-gpf6](https://github.com/opencast/opencast/security/advisories/GHSA-44cw-p2hm-gpf6))
Opencast services check the validity of the certificates presented by
third parties and when connecting to each other remotely.

The validity check by Opencast's HTTPS client is basically performed the same
way as any other HTTPS client would, for instance:

* validate the host name
* look up if the certificate is signed by a trusted Certificate Authority (CA)

In case of self-signed certificates, a check whether the certificate is signed
by a trusted CA, would fail.

You are advised to obtain a valid certificate, issued by a trusted CA like
[Let's Encrypt](https://letsencrypt.org/).

However, valid certificates are not always an option for testing or developer
instances of Opencast, especially with Enterprise Firewalls or CAA records in
place.

Generating Self-Signed Certificates
-----------------------------------

A self-signed certificate for multiple host names can be created with openSSL
as follows:

`testing-cert.req.conf`:

```ini
[req]
distinguished_name = req_distinguished_name
x509_extensions = v3_req
prompt = no
[req_distinguished_name]
C = CH
ST = Wallis
L = Matterhorn
O = Apereo
OU = Opencast
CN = opencast
[v3_req]
keyUsage = nonRepudiation, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
[alt_names]
DNS.1 = opencast
DNS.2 = presentation.local
DNS.3 = admin.local
DNS.4 = admin
DNS.5 = presentation
DNS.6 = worker
DNS.7 = worker.local
```

```bash
openssl req \
  -x509 \
  -nodes \
  -days 365 \
  -newkey rsa:2048 \
  -keyout key.pem \
  -out testing.pem \
  -config testing-cert.req.conf \
  -extensions 'v3_req'
```

In order to view the just generated cert:

```bash
openssl x509 -in testing.pem -text -noout
```

Trusting Self-Signed Certificates
---------------------------------

In order to use self-signed certificates for testing or developer instances of
Opencast, import your self-signed certificate(s) into the Java Trust Store
(bundle of trusted CA certs) and restart Opencast.

1. Store your certificate in a format compatible with `keytool` somewhere:

        cat >/tmp/testing.crt <<EOL
        -----BEGIN CERTIFICATE-----
        MIIGJzCCBA+gAw...
        ...O6g==
        -----END CERTIFICATE-----
        EOL

2. Import the certificate with alias `testing_root` into the
`javax.net.ssl.trustStrore` whose password defaults to `changeit` without asking
questions:

        keytool \
           -import \
           -noprompt \
           -trustcacerts \
           -storepass changeit \
           -alias testing_root \
           -file /tmp/testing.crt \
           -keystore $JAVA_HOME/jre/lib/security/cacerts

3. Delete the temporary file:

        rm /tmp/testing.crt

4. Restart Opencast.
