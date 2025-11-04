Enable HTTPS using Caddy
========================

> [Using Nginx as reverse proxy for Opencast](nginx.md) is the preferred way of running Opencast.
> Refer to the [Nginx guide for configuration instructions](nginx.md) for that type of setup.

This guide will help you to configure Caddy 2 to act as HTTP(S) proxy for Opencast.


Opencast Configuration
----------------------

Make sure to use `https` as protocol for `org.opencastproject.server.url` in `etc/custom.properties`.

```ini
org.opencastproject.server.url=https://example.opencast.org
```

No other configuration is required. Do not enable TLS in Opencast. Listen to local connections only. Both are the
default settings.


Minimal Set-up
--------------

The following configuration is an example for `/etc/caddy/Caddyfile`.

Explanations for the configuration directives are provided inline. Please make sure to replace `example.opencast.org`
with your node's domain name.

The main goals of this set-up are:

- Always redirect to HTTPS
- Proxy to Opencast and take care of TLS

The great benefit of Caddy is that it takes care of most of the stuff without extra configuration, for example:

- Automatically redirects HTTP to HTTPS
- Has built-in support for ACME and automatically obtains certificates from Let's Encrypt
- Only uses secure TLS-Versions and Ciphers by default
- Automatically sets/passes the required headers


```caddy
example.opencast.org {

    # Proxy requests to Opencast. This expects Opencast to be running locally on port 8080 which
    # should be the default set-up.
    reverse_proxy 127.0.0.1:8080 {

        # Make sure to redirect location headers to HTTPS. This is just a precaution and shouldn't
        # strictly be necessary but it did prevent some issues in the past and it does not cost
        # much performance.
        header_down Location http:// https://

        # Make sure to serve cookies only via secure connections.
        header_down Set-Cookie (.*) "$1; HttpOnly; Secure; Partitioned;"

        # Depending on your integration, you may also want to allow cookies to be used on other
        # sites. In that case, use this instead:
        #header_down Set-Cookie (.*) "$1; HttpOnly; Secure; SameSite=None; Partitioned;"
    }

    # Optional - Set a custom certificate and key. This disables ACME.
    #tls /path/to/example.opencast.org.crt /path/to/example.opencast.org.key
}
```

> It is also possible to use a custom ACME CA. For instructions, please take a look at the "acme_..." options
> [here](https://caddyserver.com/docs/caddyfile/options).
