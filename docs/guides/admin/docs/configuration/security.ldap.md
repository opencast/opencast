LDAP Authentication and Authorization (without CAS)
===================================================

---

> *This page describes how to use only an LDAP server to authenticate users in Opencast. If you just want to use LDAP*
*as an identity provider for another authentication mechanism, such as a CAS server, this guide does not apply to*
*you.*
>
> *You may find the instructions to configure an LDAP-backed CAS server [here](security.cas.md).*

---

> *The variable `$OPENCAST_ETC` used below stands for the location of Opencast's configuration folder within your*
*system. Its location varies depending on whether Opencast was installed from source or a packaged version (e.g. RPM)*
*was used:*
>
> * ***Source installation:** The `etc` folder within the directory containing the Opencast code.*
> * ***Packaged installation:*** A subdirectory of your system's usual configuration directory, most likely*
> *`/etc/opencast`.*

---


Set up an LDAP provider
-----------------------

### Step 1 ###

Opencast's `security.xml` files are located in the folder `$OPENCAST_ETC/security`. In a single-tenant deployment, the
file is named `$OPENCAST_ETC/security/mh_default_org.xml` for the default organization. In a multi-tenant installation,
or when a non-default organization is used, the file(s) are `$OPENCAST_ETC/security/<organization_id>.xml`.

You should make a backup copy of the file and substitute it with the sample file named `security_sample_ldap.xml-example`.
In other words:

    $> cd $OPENCAST_ETC/security
    $> mv mh_default_org.xml mh_default_org.xml.old
    $> cp security_sample_ldap.xml-example mh_default_org.xml

The sample file should be exactly the same as the replaced security file, except for the parts only relevant to the
LDAP which will be discussed below. If you have done custom modifications to your security file, please make sure to
incorporate them to the new file, too.


### Step 2 ###

Add the necessary configuration values to the LDAP section of the new security file.

The first relevant section defines a context source. This contains the basic login information that enables Opencast to
request information about users from the LDAP server in order to authenticate them.

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

The next part tells the system how to search for users in the LDAP server:

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

As the previous snippet shows, there are two alternative ways to find users in your LDAP:

* **Using the property userDnPatterns:** This property accepts a list of search patterns to match against the user's
DN. The patterns will be tried in order until a match is found. The placeholder `{0}` can be used to represent the
username in such patterns.
* **Using a userSearch filter:** With the previous approach, it is not possible to find users whose login name is not
    part of their DN. In such cases, you can use the userSearch property, that allows you to search the users based on a
    filter. The filter requires three parameters:
    * The first parameter specifies the "root node" where the searches will start from.
    * The second one specifies the filter, where, again, the placeholder `{0}` will be substituted by the username
      during the searches.
    * The third parameter should be the contextSource defined above.

Both methods are not mutually exclusive --i.e. both can be activated at the same time, even though only the first one
is uncommented in the sample file because it is the most usual.

The last constructor argument is defined as follows:

    <!-- Defines how the user attributes are converted to authorities (roles) -->
    <constructor-arg ref="authoritiesPopulator" />

, which refers to the following definition:

    <osgi:reference id="authoritiesPopulator" cardinality="1..1"
                    interface="org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator"
                    filter="(instanceId=theId)"/>

You may edit `theId` to any value, ideally one that is descriptive of the LDAP connection being configured. The same
value used here must be set as the `org.opencastproject.userdirectory.ldap.id` in the LDAP configuration file described
below.

### <a name="cfg"></a> Step 3 ###

Make a copy of the file `$OPENCAST_ETC/org.opencastproject.userdirectory.ldap.cfg.template` in the same directory and
rename it as:

    org.opencastproject.userdirectory.ldap-<ID>.cfg

, where `<ID>` is a unique identifier for each LDAP connection. This identifier is only use to distinguish between the
files and is not used internally. It might have any value, but for the sake of clarity, it is recommended to use the
same value as in the `org.opencastproject.userdirectory.ldap.id` parameter in the file.


### Step 4 ###

Edit the parameters in the file with your particular configuration. Unfortunately, some of those are duplicated in the
`security.xml` file, but this situation cannot currently be avoided.

The parameters that are exclusive to this `.cfg` file control the user authorization, i.e. how the roles obtained from
the LDAP are handled and assigned to the users. Please refer to the documentation in the file itself to know the meaning
of these parameters and how to use them.

**IMPORTANT**: The `org.opencastproject.userdirectory.ldap.id` parameter in the file must be configured to the same
value as the ID of the OSGI reference in the `security.xml` file above (at the end of the step #2).


Combination with Existing authorization Mechanisms
--------------------------------------------------

In the default configuration included in the `security_sample_ldap.xml-example` file, the LDAP is tried after the
normal authorization mechanisms (i.e. the database). This means that if a user is present in both the database and the
LDAP, the database will take precedence. The order is determined by the order in which the authentication providers
appear on the security file. The relevant snippet is this:

    <sec:authentication-manager alias="authenticationManager">
      <sec:authentication-provider user-service-ref="userDetailsService">  # \
        <sec:password-encoder hash="md5">                                  # |
          <sec:salt-source user-property="username" />                     # -> These lines must be moved as a block
        </sec:password-encoder>                                            # |
      </sec:authentication-provider>                                       # /
      <sec:authentication-provider ref="ldapAuthProvider" />               # The LDAP provider appears in the second position, therefore it is the second provider to consider
    </sec:authentication-manager>

By switching the position of the authentication providers, you will give them more or less priority.


Adding more LDAP servers
------------------------

More LDAP servers can be added to the configuration by including the LDAP-related sections as many times as necessary
with their corresponding configurations. The new authentication providers must also be added to the providers list
at the bottom of the file. Please see the example below:

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
      <constructor-arg ref="authoritiesPopulator" />
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
      <constructor-arg ref="authoritiesPopulator2" />
    </bean>

    <!-- [ ... SKIPPED LINES ... ] -->

    <osgi:reference id="authoritiesPopulator" cardinality="1..1"
                    interface="org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator"
                    filter="(instanceId=theId)"/>
    <osgi:reference id="authoritiesPopulator2" cardinality="1..1"
                    interface="org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator"
                    filter="(instanceId=theId2)"/>

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


Then, a separate `.cfg` must be generated for each of the configured providers, as explained [here](#cfg). Please make
sure to configure the `org.opencastproject.userdirectory.ldap.id` parameter correctly. In this case, the values should
be `theId` and `theId2`, respectively.
