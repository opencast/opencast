Serve Content Via HTTPS
=======================

To make your installation available from the outside worls, you want to allow non-local traffic and want to secure
connections from and to Opencast.  To archieve that, you can either use an HTTP(S) proxy like Apache httpd or Nginx
(recommended) or enable HTTPS directly in Opencast.

- [Using Nginx to enable HTTPS](nginx.md) (recommended)
- [Using Apache httpd to enable HTTPS](apache-httpd.md)
- [Enable HTTPS directly in Opencast](opencast.only.md)


Note that introducing HTTPS will not automatically migrate old content.
It may still use the previously configured HTTP protocol.
For a semi-automatic migration, please take a look at the following guide:

- [Migrating old content to HTTPS](migration.md)


General Recommendations
-----------------------

It's hard to keep up with security (e.g. proper TLS configuration). That is why we recommend using a proxy like Nginx or
Apache as, due to their general popularity, it is usually much easier to find good configuration recommendations.

There are also a couple of great sites to test your final setup:

- [Observatory by Mozilla](https://observatory.mozilla.org/)
- [Qualys SSL Labs](https://ssllabs.com/ssltest/)


If you have no easy way of obtaining proper TLS certificates for your organization, please consider using
[Let’s Encrypt](https://letsencrypt.org).

For testing and developer servers, [properly configured self-signed certificates](self-signed-certificates.md) can be an option.
