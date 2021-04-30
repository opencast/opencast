Enable HTTPS using Apache httpd
===============================

> [Using Nginx as reverse proxy for Opencast](nginx.md) is the preferred way of running Opencast.
> Refer to the [Nginx guide for configuration instructions](nginx.md) for that type of setup.

This guide will help you to configure httpd to act as HTTP(S) proxy for Opencast.


Opencast Configuration
----------------------

Make sure to use `https` as protocol for `org.opencastproject.server.url` in `etc/custom.properties`.

```ini
org.opencastproject.server.url=https://example.opencast.org
```

No other configuration is required.
Do not enable TLS in Opencast.
Listen to local connections only.
Both are the default settings.


Minimal Set-up
--------------

> Note that this guide does not give any security advice but is meant to provide a minimal working example which works
> well with Opencast.

The following configuration is an example for `/etc/httpd/conf.d/opencast.conf`.
Note that depending on your distributions packaging, often `conf.d` or `sites-enabled` directories are used.
Adjust the file path accordingly.

Explanations for the configuration directives are provided inline. Please make sure to replace `example.opencast.org`
with your node's domain name.

The main goals of this set-up are:

- Always redirect to HTTPS
- Proxy to Opencast and take care of TLS
- Avoid caching


```
<VirtualHost *:80>
  ServerName example.opencast.org
  RewriteEngine on
  RewriteRule ^/(.*)$ https://example.opencast.org/$1 [NC]
</VirtualHost>

<VirtualHost *:443>
  ServerName example.opencast.org

  # Enable TLS
  SSLEngine on
  SSLProxyEngine on

  SSLCertificateFile      /etc/ssl/certs/oc-cert.crt
  SSLCertificateKeyFile   /etc/ssl/private/oc-key.key
  SSLCertificateChainFile /etc/ssl/certs/oc-chain.crt

  # Make sure Opencast knows about HTTPS being used
  RequestHeader set X-Forwarded-SSL "on"
  RequestHeader set X-Forwarded-Proto "https"

  # Make sure to serve cookies only via secure connections.
  Header edit Set-Cookie ^(.*)$ $1;HttpOnly;Secure

  # Depending on your integration, you may also want to allow cookies
  # to be used on other sites. In that case, use this instead:
  #Header edit Set-Cookie ^(.*)$ $1; HttpOnly; Secure; SameSite=None

  # Proxy requests to Opencast
  ProxyPreserveHost On
  ProxyPass / http://127.0.0.1:8080/
  ProxyPassReverse / http://example.opencast.org
</VirtualHost>
```
