Serve Content Via HTTPS
=======================

For all production systems you want to enable HTTPS. To archieve that, you can either use an HTTP(S) proxy like Apache
httpd or Nginx (recommended) or enable HTTPS directly in Opencast.

- [Using Nginx to enable HTTPS](nginx.md)
- [Enable HTTPS directly in Opencast](opencast.only.md)


Note that introducing HTTPS will not automatically migrate old content. It may still use the previously configured HTTP
prorocol. For a semi-automatic migration, please take a look at the following guide:

- [Migrating old content to HTTPS](migration.md)


General Recommendations
-----------------------

It's hard to keep up with security (e.g. proper TLS configuration). That is why we recommend using a proxy like Nginx or
Apache as, due to their general popularity, it is usually much easier to find good configuration recommenations.

There are also a couple of great sites to test your final setup:

- [Observatory by Mozilla](https://observatory.mozilla.org/)
- [Qualys SSL Labs](https://ssllabs.com/ssltest/)


Additionally, if you have no easy way of obtaining proper TLS certificates for your organization, please consider using
[Let’s Encrypt](https://letsencrypt.org) instead of self-signed certificates.
