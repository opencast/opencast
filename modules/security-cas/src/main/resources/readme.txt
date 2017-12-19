This bundle adds the imports needed to run Matterhorn with a CAS security configuration. It does not
provide the CAS code itself.

The default system.properties does not include the CAS bundles by default, but the runtime
dependencies should have been put into the lib/ext directory already. Uncommenting the relevant
section in /etc/system.properties will enable you to configure security.xml for use with CAS.