LDAP Authentication and Authorization (without CAS)
===================================================

> *This page describes how to use only an LDAP server to authenticate users in Opencast. If you just want to use LDAP*
*as an identity provider for another authentication mechanism, such as a CAS server, this guide does not apply to*
*you.*
>
> *You may find the instructions to configure an LDAP-backed CAS server [here](security.cas.md).*


Authentication
--------------

In order to authenticate your Opencast users using an LDAP server, you must follow the following steps:

### Step 1 ###

In a single-tenant deployment, your `security.xml` file is under `OPENCAST_HOME/security/mh_default_org.xml`. In an
RPM-based installation, it is located in `/etc/opencast/security/mh_default_org.xml`. You should make a backup copy of
the file and substitute it by the sample file named `security_sample_ldap.xml-example`. In other words:

    $> cd etc/security
    $> mv mh_default_org.xml mh_default_org.xml.old
    $> cp security_sample_ldap.xml-example mh_default_org.xml

The sample file should be exactly the same as the default security file, except for the parts only relevant to the
LDAP. If you have done custom modifications to your security file, make sure to incorporate them to the new file, too.

### Step 2 ###

Add the necessary configuration values to the LDAP section of the new security file. The comments should be
self-explanatory.

The first relevant section defines a context source. This contains the basic login information that enables Opencast to
request user information to the LDAP server in order to authenticate them.

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

Authorization
-------------

Now the system knows all the information necessary to authenticate users against the LDAP, but also need some
authorization information, to tell which services the user is allowed to use and which resources is allowed to see
and/or modify.

### Step 1 ###

The following snippet in the `security.xml` file provides the necessary information to map LDAP attributes to roles
during authentication and in single-machine environments.

    <constructor-arg>
      <!-- Get the authorities (roles) according to a certain attribute in the authenticated user -->
      <bean class="org.opencastproject.kernel.userdirectory.LdapAttributeAuthoritiesPopulator">
        <constructor-arg>
          <!-- List of attribute names in the user from which roles will be created -->
          <!-- The specified attributes must meet few requirements:
                 * They may be single-valued or multivalued
                 * They may contain single roles or comma-separated role lists
    
               The attributes read will be processed in the following way:
                 * Whitespace will converted to underscores ("_")
                 * Sequences of underscores ("_") will be collapsed into a single one.
          -->
          <list>
            <value>attributeName1</value>
            <value>attributeName2</value>
          </list>
        </constructor-arg>
        <!-- Whether or not to make all the extracted roles uppercase. 'true' by default. -->
        <!-- <property name="convertToUpperCase" value="true"/> -->
    
        <!-- Define a prefix to be appended to every role extracted from the LDAP. -->
        <!-- The convertToUpperCase property also affects the prefix -->
        <!-- <property name="rolePrefix" value=""/> -->
    
        <!-- Additional roles that will be added to those obtained from the attributes above -->
        <!-- The convertToUpperCase and rolePrefix properties also affect the roles indicated here-->
        <!-- <property name="additionalAuthorities">
          <set>
            <value>additional_authority_1</value>
            <value>additional_authority_2</value>
          </set>
        </property> -->
      </bean>
    </constructor-arg>

As you can see, the sample file is quite self-explanatory: a list of attribute names is provided, each of which will
contain the roles this user will be assigned to. These attributes may be multivalued (i.e. they may appear several
times in the user) and/or they may contain a comma-separated list of roles. The syntax of the roles found will be
checked so that they contain no whitespaces (they will be substituted by underscore characters  -"_"-) and all the
resulting underscore sequences will be collapsed to a single character. The resulting roles will be converted to
uppercase and finally assigned to the user.

Apart from the previous processing, a series of properties may be used to further customize the role syntax. In
particular, a prefix can be defined, which will be appended to every role obtained using the process described above.
The role capitalisation can also be disabled if desired.

Finally, it is possible to define an additional set of roles that will be appended to the list obtained by inspecting
the LDAP according to the configuration. In other words, every user will have, at least, the roles defined in this
list. The roles will be processed in the same way as the roles obtained from the LDAP server.

### Step 2 ###

> *This step is only necessary in Opencast deployments along multiple machines.*

Make a copy of the file `etc/org.opencastproject.userdirectory.ldap.cfg.template` in the same directory and name it as:

    org.opencastproject.userdirectory.ldap-<ID>.cfg

, where `<ID>` is a unique indentifier for each LDAP connection. Edit the parameters in the file with the same
information as in the `security.xml` file. The contents should be self-explanatory.

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
with their corresponding configurations. All the defined authentication providers must be added to the providers list
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
      <constructor-arg>
        <!-- Get the authorities (roles) according to a certain attribute in the authenticated user -->
        <bean class="org.opencastproject.kernel.userdirectory.LdapAttributeAuthoritiesPopulator">
          <constructor-arg>
            <!-- List of attribute names in the user from which roles will be created -->
            <!-- The specified attributes must meet few requirements:
                   * They may be single-valued or multivalued
                   * They may contain single roles or comma-separated role lists
     
              The attributes read will be processed in the following way:
                   * Whitespace will converted to underscores ("_")
                   * Sequences of underscores ("_") will be collapsed into a single one.
            -->
            <list>
              <value>attributeName1</value>
              <value>attributeName2</value>
            </list>
          </constructor-arg>
          <!-- Whether or not to make all the extracted roles uppercase. 'true' by default. -->
          <!-- <property name="convertToUpperCase" value="true"/> -->
    
          <!-- Define a prefix to be appended to every role extracted from the LDAP. -->
          <!-- The convertToUpperCase property also affects the prefix -->
          <!-- <property name="rolePrefix" value=""/> -->
    
          <!-- Additional roles that will be added to those obtained from the attributes above -->
          <!-- The convertToUpperCase and rolePrefix properties also affect the roles indicated here-->
          <!-- <property name="additionalAuthorities">
            <set>
              <value>additional_authority_1</value>
              <value>additional_authority_2</value>
            </set>
          </property>
          -->
        </bean>
      </constructor-arg>
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
      <constructor-arg>
        <bean class="org.opencastproject.kernel.userdirectory.LdapAttributeAuthoritiesPopulator">
          <constructor-arg>
            <list>
              <value>otherAttributeName1</value>
              <value>otherAttributeName2</value>
            </list>
          </constructor-arg>
       <property name="rolePrefix" value="my_prefix_"/>
       <property name="additionalAuthorities">
         <set>
           <value>default_role_1</value>
           <value>default_role_2</value>
         </set>
       </property>
        </bean>
      </constructor-arg>
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
