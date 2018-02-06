HTTPS
=====

This document will assist you in enabling HTTPS for a new or an existing
Opencast installation, either via HTTPS termination proxy or directly in
Opencast.

Changing the domain name of an Opencast installation is a similar procedure.


Motivation: HTTPS for Opencast, why?
------------------------------------

Your site visitor's privacy. While ISPs and public HotSpot operators and those
who sniff unencrypted WiFi traffic might be able to see your visitors
connecting to your site, they can't see, which video is watched when HTTPS is
enabled.


Enable HTTPS directly in Opencast – without proxy
-------------------------------------------------

In `opencast/etc/`, use the `org.ops4j.pax.web.cfg` file for
configuration:

```
# ...

# Whether Opencast itself should handle HTTPS traffic.
# Even if you set this to 'false',you can still use an HTTP proxy to handle SSL.
org.osgi.service.http.secure.enabled=true

# The secure server port to use if running Opencast with HTTPS (as opposed to
# a proxy handling HTTPS).
# Note that we use the docker proxy for the port-mapping from 8843 from within
# the container to 443 at the host
# Don't run Opencast with root privileges, which is a security issue
org.osgi.service.http.port.secure=8443

# Path to the keystore file.
# Use the Java `keytool` to generate this file.
# Example:
#   keytool -genkey -keyalg RSA -validity 365 -alias serverkey \
#     -keypass password -storepass password -keystore keystore.jks
org.ops4j.pax.web.ssl.keystore=<path_to_keystore>

# Password used for keystore integrity check.
org.ops4j.pax.web.ssl.password=<the_keystore_password>

# Password used for keystore.
org.ops4j.pax.web.ssl.keypassword=<the_key_password>
```


### Port-Forwarding required

Note that Opencast most likely can't bind to port 443. That's why you still
need to reverse-proxy or port-forward if you want to avoid URLs with port
specified like `https://host:8443/` which is technically perfectly okay but may
confuse users or may be perceived as "ugly".

Here is a non-comprehensive lists of tools and methods which can be used for
port forwarding:


#### Port-Forwarding with iptables

A rule like

```sh
iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 443 -j REDIRECT --to-port 8443
# Allow also localhost traffic to use :443
# iptables -A OUTPUT -t nat -o lo -p tcp --dport 443 -j REDIRECT --to-port 8443
```

should do the job after replacing `eth0` with the network interface your
Opencast consumers will connect on. Note that you usually want to persist the
rule.


#### Port-Forwarding with docker(-proxy)

When starting a container from an Opencast image, either insert a
command line argument to docker run: `-p 443:8443` or add a `ports:`
in `docker-compose.yaml`.


#### Port-Forwarding with sniproxy

[Sniproxy](https://github.com/dlundquist/sniproxy) can be used as well,
especially if you have multiple servers running on the same machine that handle
HTTPS individually (no termination proxy).

```
user, pidfile, error_log ...

listen 443 {
    proto tls
    table https_hosts
    fallback 127.0.0.1:8443

    access_log {
        filename /var/log/sniproxy/https_access.log
        priority notice
    }
}

table https_hosts {
    .*\.opencast\.example\.org 127.0.0.1:8443
}
```


### Creating the keystore

What you need, is the TLS private key and the certificate including the
whole chain between the root certificate, all intermediates and the
certificate itself.


#### Obtaining the certificate chain

If you only have the key and the certificate, I recommend
[certificatechain.io](https://certificatechain.io/) or
[cert-chain-resolver](https://github.com/zakjan/cert-chain-resolver).
The latter can be used as follows:

```bash
# Obtain the chain for cert.pem and save it at opencast.chain.pem.tmp
# The -s command switch includes the root certificate; this is not
# mandatory and might add some overhead
cert-chain-resolver -s -o "opencast.chain.pem.tmp" "cert.pem"

# Verify the certificate using the chain
openssl verify -crl_download -crl_check -untrusted "opencast.chain.pem.tmp" "cert.pem"
```

#### Create the p12 keystore

If the private key (assumed to be `key.pem`) is encrypted
(password protected), issue the following command. Note that there
are safer ways supplying the key's password to OpenSSL.

```bash
openssl pkcs12 \
        -export \
        -inkey "key.pem" \
        -passin "pass:<the_keys_password>" \
        -in "opencast.chain.pem.tmp" \
        -name "serverkey" \
        -out "opencast.p12" \
        -passout "pass:<the_keystore_password>"
```

In case the private key is not protected by password:

```bash
openssl pkcs12 \
        -export \
        -inkey "key.pem" \
        -in "opencast.chain.pem.tmp" \
        -name "serverkey" \
        -out "opencast.p12" \
        -passout "pass:<the_keystore_password>"
```


#### Import the p12 keystore into a Java keystore:

```bash
keytool \
        -importkeystore \
        -srckeystore "opencast.p12" \
        -srcstoretype "pkcs12" \
        -srcstorepass "<the_keystore_password>" \
        -destkeystore "keystore.jks" \
        -storepass "<the_keystore_password>"
# print out details about the JKS built
keytool \
        -keystore "keystore.jks" \
        -list \
        -destalias serverkey \
        -storepass "<the_keystore_password>"
```

[There exists a shell script automating that task
](https://gist.github.com/pawohl/dd92ff4909e3e2704e36dec747ea238e).


### Default to HTTPS

When finished, restarted and verified that HTTPS works as expected,
you can change Opencast's default URL to the HTTPS one.

Set `org.opencastproject.server.url` to the  HTTPS-URL in
`etc/custom.properties`.

```ini
org.opencastproject.server.url=https://opencast.example.com
```


Enable HTTPS using a termination-proxy
--------------------------------------

All you have to do is to set `org.opencastproject.server.url` to the
HTTPS-URL in `etc/custom.properties`.

```ini
org.opencastproject.server.url=https://opencast.example.com
```


Upgrading to HTTPS if already-processed media packages exist
------------------------------------------------------------

1. Backup your database, and the Solr and Elasticsearch indices.
2. Tell your other Opencast systems to use HTTPS for each other,
   or at least for the system delivering the videos to the visitors
   and creating the search indices.
3. Put all your nodes into maintenance mode, or, at least do
   not process any videos.
4. Update the media packages:
   `find . -type f -name "*.xml" -exec \
    sed -i 's/http\:\/\/presentation\.opencast\.example\.com\:80/https:\/\/presentation.opencast.example.com/g' {} +`
5. Update 2 database tables:

        UPDATE opencast.mh_archive_episode
        SET mediapackage_xml =
           REPLACE( mediapackage_xml,
                    'http://presentation.opencast.example.com:80',
                    'https://presentation.opencast.example.com')
           WHERE INSTR( mediapackage_xml,
                        'http://presentation.opencast.example.com:80') > 0;
        UPDATE opencast.mh_search
        SET mediapackage_xml =
           REPLACE( mediapackage_xml,
                    'http://presentation.opencast.example.com:80',
                    'https://presentation.opencast.example.com')
           WHERE INSTR( mediapackage_xml,
                        'http://presentation.opencast.example.com:80') > 0;

6. Rebuild the Elasticsearch indices.
   Visit your REST API and push the button:
   https://admin.opencast.example.com/docs.html?path=/admin-ng/index
7. Move the search service's old Solr index away. There might be a directory
   named `solr-indexes/search` but its configuration really depends on
   `org.opencastproject.solr.dir`, or if set in `custom.properties`,
   `org.opencastproject.search.solr.dir`
8. Rebuild the Solr indices by re-starting your Opencast node running the
   search service (usually presentation).
