Trusted Proxies
===============

Opencast is usually run behind a reverse proxy which terminates TLS and forwards requests to Opencast itself. Since
Opencast only ever sees a connection coming from that proxy, the proxy has to tell it about the original request using
these headers:

- `X-Forwarded-For` – the address of the client the proxy received the request from
- `X-Forwarded-Proto` / `X-Forwarded-SSL` – whether the client was talking HTTPS

Nothing stops a regular client from sending those headers as well. Opencast therefore only honors them if the request
was received from a host you declared to be a reverse proxy of yours. Requests from anywhere else are taken at face
value and the headers are ignored.

Configuration
-------------

The list of proxies lives in `etc/org.opencastproject.kernel.filter.proxy.TrustedProxies.cfg`:

```properties
trusted.proxies=127.0.0.0/8, ::1, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fc00::/7
```

Entries are IP addresses or CIDR ranges and may be IPv4 or IPv6. The value shown above is the default and covers the
loopback interface plus the private address ranges, which is where the reverse proxy sits in a typical installation.

Narrow this down to the address your proxy actually uses if you can. Any host matching this list gets to choose which
client address Opencast sees, so the shorter the list, the better. Adding public address space effectively turns the
protection off.

Setting the option to an empty value makes Opencast ignore forwarding headers on every request.

Which requests this affects
---------------------------

The client address Opencast ends up with is used for logging and user tracking, but also for
[signed URLs restricted to a single client](stream-security/stream-security-config.md) via
`url.signing.use.client.ip`. If forwarding headers were trusted unconditionally, a client could pick the address such a
URL is bound to, which is why that feature is only meaningful together with a correct list of proxies here.

Making sure your proxy overwrites the header
--------------------------------------------

A trusted proxy is trusted for one thing only: the address it appends itself. Opencast reads the
`X-Forwarded-For` list from the right and skips over addresses belonging to configured proxies, so entries a client
sent along are discarded on the way. This works regardless of whether your proxy replaces or extends the header.

If requests pass through several proxies in a row, list all of them here. Only the outermost proxy may replace the
header, while the ones behind it need to extend it and pass `X-Forwarded-Proto` and `X-Forwarded-SSL` on unchanged.

It is still cleaner to have the proxy overwrite whatever the client sent. See the
[Nginx](https/nginx.md) and [Apache httpd](https/apache-httpd.md) guides for configuration examples.
