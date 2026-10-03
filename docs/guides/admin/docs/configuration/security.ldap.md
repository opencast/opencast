LDAP Authentication and Authorization
=====================================

> This page describes how to use LDAP as an authentication and user provider Opencast.
> There are separate instructions on how to [configure an LDAP-backed CAS server](security.cas.md).


The Two Configuration Files
---------------------------

Setting up LDAP involves two configuration files, and several of their settings look alike. They have different jobs:

- `etc/security/mh_default_org.xml` takes care of **authentication**. It defines how Opencast finds a user in LDAP and
  checks the password when the user logs in. This decides who can log in.
- `etc/org.opencastproject.userdirectory.ldap-<ID>.cfg` takes care of **authorization**. It defines how the roles, name
  and email address of a user are read from LDAP. It does not decide who can log in.

The `<ID>` links both files: The security configuration refers to `ldapAuthoritiesPopulator_<ID>`, which is provided by
the LDAP service configuration with the matching `org.opencastproject.userdirectory.ldap.id`.

Both files need to describe the same LDAP directory. These are the settings which appear in both of them:

| Purpose                        | Security configuration                     | LDAP service configuration                   |
|--------------------------------|--------------------------------------------|----------------------------------------------|
| Address of the LDAP server     | `contextSource`, constructor argument      | `org.opencastproject.userdirectory.ldap.url` |
| Account used to search in LDAP | `userDn` and `password` of `contextSource` | `userDn` and `password` (optional)           |
| Where and how to find the user | `userDnPatterns` and `userSearch`          | `searchbase` and `searchfilter`              |

The search settings are used for different things:

- The `userDnPatterns` and `userSearch` of the security configuration find the user who wants to log in. They decide who
  is allowed to log in. To restrict the login to certain users, restrict them there, for example with a filter which
  also requires a group membership, like `(&(uid={0})(memberOf=cn=opencast,ou=groups,dc=example,dc=org))`.
- The `searchbase` and `searchfilter` of the LDAP service configuration are used whenever Opencast needs the details of
  a user, for example to determine their roles, name and email address. The results are cached. If the filter finds no
  entry for a user, Opencast gets no LDAP data about this user from this provider. This does not prevent the user from
  logging in.

Usually, both should search the same part of the directory with the same filter.


Security Configuration
----------------------


Edit the `etc/org.opencastproject.kernel.security.SpringSecurityConfigurationArtifactInstaller.cfg`, adding all the expected ldap service configurations for each organization:

```
# ldap.instances.<organization_id>=<comma_separated_ldap_configuration_list>
ldap.instances.mh_default_org=ldapinstance1,ldapinstance2
```
In the example above, _ldapinstance1_ and _ldapinstance2_ correspond to LDAP service instances configured in `etc/org.opencastproject.userdirectory.ldap-<ID>.cfg` (see _LDAP Service Configuration_ below).

Edit the security configuration file at `etc/security/mh_default_org.xml`. In a multi-tenant set-up, you will have one
configuration file for each tenant at `etc/security/<organization_id>.xml`.

You will find several commented out LDAP sections in this file.
Uncomment them and fill in the necessary configuration values.

The first relevant section defines a context source. This contains the basic login information that enables Opencast to
request information about users from the LDAP server in order to authenticate them.

```xml
<bean id="contextSource"
  class="org.springframework.security.ldap.DefaultSpringSecurityContextSource">
  <!-- URL of the LDAP server -->
  <constructor-arg value="ldap://myldapserver:myport" />
  <!-- "Distinguished name" for the unprivileged user -->
  <!-- This user is merely to perform searches in the LDAP to find the users to login -->
  <property name="userDn" value="uid=user-id,ou=GroupName,dc=my-institution,dc=country" />
  <!-- Password of the user above -->
  <property name="password" value="mypassword" />
</bean>
```

The next part tells the system how to search for users in LDAP:

```xml
<constructor-arg>
  <bean class="org.springframework.security.ldap.authentication.BindAuthenticator">
    <constructor-arg ref="contextSource" />
    <property name="userDnPatterns">
      <list>
        <!-- Dn patterns to search for valid users. Multiple "<value>" tags are allowed -->
        <value>uid={0},ou=Group,dc=my-institution,dc=country</value>
      </list>
    </property>
    <!-- If your user IDs are not part of the user Dn's, you can use a search filter to find them -->
    <!-- This property can be used together with the "userDnPatterns" above -->
    <!--
    <property name="userSearch">
      <bean name="filterUserSearch" class="org.springframework.security.ldap.search.FilterBasedLdapUserSearch">
        < ! - - Base Dn from where the users will be searched for - - >
        <constructor-arg index="0" value="ou=GroupName,dc=my-institution,dc=country" />
        < ! - - Filter to located valid users. Use {0} as a placeholder for the login name - - >
        <constructor-arg index="1" value="(uid={0})" />
        <constructor-arg ref="contextSource" />
      </bean>
    </property>
    -->
  </bean>
</constructor-arg>
```

As the previous snippet shows, there are two alternative ways to find users in your LDAP:

- Using the property userDnPatterns:
  This property accepts a list of search patterns to match against the user's DN. The patterns will be tried in order
  until a match is found. The placeholder `{0}` can be used to represent the username in such patterns.

- Using a userSearch filter:
  With the previous approach, it is not possible to find users whose login name is not part of their DN. In such cases,
  you can use the userSearch property, that allows you to search the users based on a filter. The filter requires three
  parameters:
    - The first parameter specifies the "root node" where the searches will start from.
    - The second one specifies the filter, where, again, the placeholder `{0}` will be substituted by the username
      during the searches.
    - The third parameter should be the contextSource defined above.

Both methods are not mutually exclusive – i.e. both can be activated at the same time, even though only the first one
is uncommented in the sample file because it is the most usual.


Next, make sure to name the LDAP authorities populator(s) passed to the
LdapAuthenticationProvider(s) as a constructor parameter following this
convention: `ldapAuthoritiesPopulator_<ID>`, where ID needs to match the one used later in the LDAP service configuration.


Finally, enable the authentication provider by uncommenting:

```xml
<sec:authentication-provider ref="ldapAuthProvider" />
```

LDAP Service Configuration
--------------------------

Make a copy of the file `etc/org.opencastproject.userdirectory.ldap.cfg.template` in the same directory and
rename it as:

    org.opencastproject.userdirectory.ldap-<ID>.cfg

…where `<ID>` is a unique identifier for each LDAP connection.


Now adjust the service configuration to your needs.
The parameters in this file control the user authorization, i.e. how the roles obtained from LDAP are handled and
assigned to the users. Note that the `searchbase` and `searchfilter` do not restrict who can log in, see
[The Two Configuration Files](#the-two-configuration-files).


Combination with Existing authorization Mechanisms
--------------------------------------------------

In the default configuration in `etc/security/mh_default_org.xml`, the LDAP is tried after the
normal authorization mechanisms (i.e. the database). This means that if a user is present in both the database and the
LDAP, the database will take precedence. The order is determined by the order in which the authentication providers
appear on the security file. The relevant snippet is this:

```xml
<sec:authentication-manager alias="authenticationManager">
  <sec:authentication-provider user-service-ref="userDetailsService">…</sec:authentication-provider>
  <sec:authentication-provider ref="ldapAuthProvider" />
</sec:authentication-manager>
```


Adding more LDAP servers
------------------------

More LDAP servers can be added to the configuration by including the LDAP-related sections as many times as necessary
with their corresponding configurations. The new authentication providers must also be added to the providers list
at the bottom of the file. Please see the example below:

```xml
<bean id="contextSource"
  class="org.springframework.security.ldap.DefaultSpringSecurityContextSource">
  <!-- URL of the LDAP server -->
  <constructor-arg value="ldap://myldapserver:myport" />
  <!-- "Distinguished name" for the unprivileged user -->
  <!-- This user is merely to perform searches in the LDAP to find the users to login -->
  <property name="userDn" value="uid=user-id,ou=GroupName,dc=my-institution,dc=country" />
  <!-- Password of the user above -->
  <property name="password" value="mypassword" />
</bean>

<bean id="ldapAuthProvider"
  class="org.springframework.security.ldap.authentication.LdapAuthenticationProvider">
  <constructor-arg>
    <bean
      class="org.springframework.security.ldap.authentication.BindAuthenticator">
      <constructor-arg ref="contextSource" />
      <property name="userDnPatterns">
        <list>
          <!-- Dn patterns to search for valid users. Multiple "<value>" tags are allowed -->
          <value>uid={0},ou=Group,dc=my-institution,dc=country</value>
        </list>
     </property>
     <!-- If your user IDs are not part of the user Dn's, you can use a search filter to find them -->
     <!-- This property can be used together with the "userDnPatterns" above -->
     <!--
     <property name="userSearch">
       <bean name="filterUserSearch" class="org.springframework.security.ldap.search.FilterBasedLdapUserSearch">
         < ! - - Base Dn from where the users will be searched for - - >
         <constructor-arg index="0" value="ou=GroupName,dc=my-institution,dc=country" />
         < ! - - Filter to located valid users. Use {0} as a placeholder for the login name - - >
         <constructor-arg index="1" value="(uid={0})" />
         <constructor-arg ref="contextSource" />
       </bean>
      </property>
     -->
    </bean>
  </constructor-arg>
  <!-- Defines how the user attributes are converted to authorities (roles) -->
  <!-- Note that <ID> must match one of the LDAP service configurations -->
  <!-- defined in etc/org.opencastproject.userdirectory.ldap-<ID>.cfg -->
  <!-- constructor-arg ref="ldapAuthoritiesPopulator_<ID>" /-->
  <constructor-arg ref="ldapAuthoritiesPopulator_theId" />
</bean>

<!-- PLEASE NOTE: The ID below must be changed for each context source instance -->
<bean id="contextSource2"
  class="org.springframework.security.ldap.DefaultSpringSecurityContextSource">
  <constructor-arg value="ldap://myldapserver:myport" />
  <property name="userDn" value="uid=user-id,ou=GroupName,dc=my-institution,dc=country" />
  <property name="password" value="mypassword" />
</bean>

<!-- PLEASE NOTE: The ID below must be changed for each LDAP authentication provider instance -->
<bean id="ldapAuthProvider2"
  class="org.springframework.security.ldap.authentication.LdapAuthenticationProvider">
  <constructor-arg>
    <bean
      class="org.springframework.security.ldap.authentication.BindAuthenticator">
      <!-- PLEASE NOTE: the ref below must match the corresponding context source ID -->
      <constructor-arg ref="contextSource2" />
       <property name="userDnPatterns">
        <list>
          <value>uid={0},ou=OtherGroup,dc=my-other-institution,dc=other-country</value>
        </list>
       </property>
    <property name="userSearch">
      <bean name="filterUserSearch" class="org.springframework.security.ldap.search.FilterBasedLdapUserSearch">
        <constructor-arg index="0" value="ou=OtherGroup,dc=my-other-institution,dc=other-country" />
        <constructor-arg index="1" value="(uid={0})" />
        <!-- PLEASE NOTE: the ref below must match the corresponding context source ID -->
        <constructor-arg ref="contextSource2" />
         </bean>
       </property>
     </bean>
  </constructor-arg>
  <!-- Defines how the user attributes are converted to authorities (roles) -->
  <!-- PLEASE NOTE: the ref below must match the corresponding authoritiesPopulator -->
  <!-- Note that <ID> must match one of the LDAP service configurations -->
  <!-- defined in etc/org.opencastproject.userdirectory.ldap-<ID>.cfg -->
  <!-- constructor-arg ref="ldapAuthoritiesPopulator_<ID>" /-->
  <constructor-arg ref="ldapAuthoritiesPopulator_theId2" />
</bean>

<!-- [ ... SKIPPED LINES ... ] -->

<sec:authentication-manager alias="authenticationManager">
  <sec:authentication-provider user-service-ref="userDetailsService">
    <sec:password-encoder hash="md5">
      <sec:salt-source user-property="username" />
    </sec:password-encoder>
  </sec:authentication-provider>
  <!-- PLEASE NOTE: In this example, the 2nd LDAP provider defined in the file has more priority that the first one -->
  <sec:authentication-provider ref="ldapAuthProvider2" />
  <sec:authentication-provider ref="ldapAuthProvider" />
</sec:authentication-manager>
```

Then, a separate `.cfg` must be generated for each of the configured providers, as explained in [LDAP Service Configuration](#ldap-service-configuration). Please make
sure to configure the `org.opencastproject.userdirectory.ldap.id` parameter correctly. In this case, the values should
be `theId` and `theId2`, respectively.
