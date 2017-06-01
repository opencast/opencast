### What it does

The LDAP User Provider enriches Opencast users with a set of roles derived from
user information in an LDAP Directory.

For information about authenticating users with LDAP, see 
[LDAP Authentication and Authorization](security.ldap.md)

### Step 1

To enable the LDAP User Provider, copy and rename the bundled configuration template from 
`OPENCAST/etc/org.opencastproject.userdirectory.ldap.cfg.template` to 
`OPENCAST/etc/org.opencastproject.userdirectory.ldap.cfg`

Edit the configuration file to set the LDAP URL, searchbase and filter, and other parameters
as required:

```
org.opencastproject.userdirectory.ldap.url=
org.opencastproject.userdirectory.ldap.searchbase=
org.opencastproject.userdirectory.ldap.searchfilter=(uid={0})
```

### Step 2

Verify that the LDAP User Provider starts up by looking for a log entry which includes:

```
Creating LdapUserProvider instance
```

Then login to Opencast using a username which also exists in your LDAP Directory.
Verify the roles granted to the user by opening the url OPENCAST-URL/info/me.json
in a new browser tab.

If necessary, you can increase the logging detail from the LDAP user provider by
adding an entry to `OPENCAST/etc/org.ops4j.pax.logging.cfg`:

```
log4j.logger.org.opencastproject.userdirectory.ldap=DEBUG
```

