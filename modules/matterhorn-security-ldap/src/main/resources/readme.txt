This bundle adds the imports needed to run Matterhorn with an LDAP security configuration. It does
not provide the LDAP code itself.

The default system.properties does not include the LDAP bundles by default, but the runtime
dependencies should have been put into the lib/ext directory already. Uncommenting the relevant
section in /etc/system.properties will enable you to configure security.xml for use with LDAP.

Also, note that since the spring framework does not provide an OSGi bundle of the packages under 
org.springframework.ldap, it has been produced and uploaded to the Opencast Maven repository.