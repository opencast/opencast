Enable HTTPS using Nginx
========================

This guide will help you to configure Nginx to act as HTTP(S) proxy for Opencast.


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

> Note that this guide does not give any security advise but is meant to provide a minimal working example which works
> well with Opencast.

The following configuration is an example for `/etc/nginx/nginx.conf`. Note that depending on your distributions
packaging, often `conf.d` or `sites-enabled` directories are used. But since this is an Opencast only set-up (we do not
use the web server for anything else), we are just using the main configuration file.

Explanations for the configuration directives are provided inline. Please make sure to replace `example.opencast.org`
with your nodes domain name.

The main goals of this set-up are:

- Always redirect to HTTPS
- Proxy to Opencast and take care of TLS
- Avoid caching


```
# Check your distributions default nginx.conf to make sure the first
# configuration keys (up until the http section) make sense within your
# distribution's set-up.

# Defines user and group credentials used by worker processes. If group is
# omitted, a group whose name equals that of user is used.
user    nginx;

# Configures logging to `/var/log/…`. Log level `error` is used by default.
error_log    /var/log/nginx/error.log;

# Defines a file that will store the process ID of the main process. This needs
# to match the Systemd unit file.
pid /run/nginx.pid;

events {
    # Sets the maximum number of simultaneous connections that can be opened by
    # a worker process.
    worker_connections 1024;
}

###
# What follows is the specific http(s) set-up for Opencast.
##

http {

    # HTTP set-up
    server {
        listen 80;
        listen [::]:80;
        server_name example.opencast.org;

        # Enforce HTTPS by redirecting requests
        location / {
            return 301 https://example.opencast.org$request_uri;
        }
    }

    # HTTPS set-up
    server {
        listen      443 ssl http2;
        listen [::]:443 ssl http2;
        server_name example.opencast.org;

        # Path to the TLS certificate and private key. In almost all cases, you
        # need to provide intermediate certificates as well to ensure browsers
        # get the whole certificate chain.
        ssl_certificate_key /path/to/example.opencast.org.key;
        ssl_certificate     /path/to/example.opencast.org.crt;

        # Accept large ingests. There should be no limit since Opencast may get
        # really large ingests.
        client_max_body_size 0;

        # Proxy configuration for Opencast
        location / {

            # Make sure to pass the real addresses as well as the fact that
            # outwards we are using HTTPS to Opencast.
            proxy_set_header        Host $host;
            proxy_set_header        X-Real-IP $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        X-Forwarded-Proto $scheme;

            # Pass requests to this location. This expects Opencast to be
            # running locally on port 8080 which should be the default set-up.
            proxy_pass              http://127.0.0.1:8080;

            # Make sure to redirect location headers to HTTPS. This is just a
            # precaution and shouldn't strictly be necessary but it did prevent
            # some issues in the past and it does not cost much performance.
            proxy_redirect          http://$host https://$host;

            # Do not buffer responses
            proxy_buffering         off;

            # Do not buffer requests
            proxy_request_buffering off;
        }
    }
}
```
