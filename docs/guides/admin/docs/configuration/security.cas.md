Configure Central Authentication Service (CAS)
==============================================

Authentication
--------------

Many campuses use some kind of single sign on, such as JASIG's Central Authentication Service, or CAS. This guide
describes how to integrate Opencast into such a system.

### Step 1

First, you need to edit the file `etc/org.apache.karaf.features.cfg` and add the `opencast-security-cas` to the `featuresBoot` variable.

    featuresBoot = ..., opencast-security-cas

### Step 2

In a single-tenant deployment, your `security.xml` file is under `OPENCAST_HOME/etc/security/mh_default_org.xml`. In an
RPM/DEB based installation, it is located in `/etc/opencast/security/mh_default_org.xml`. You should make a backup copy of
the file and substitute it by the sample file named `security_sample_cas.xml-example`. In other words:

    $> cd etc/security
    $> mv mh_default_org.xml mh_default_org.xml.old
    $> cp security_sample_cas.xml-example mh_default_org.xml

The sample file should be exactly the same as the default security file, except for the parts only relevant to the
CAS. If you have done custom modifications to your security file, make sure to incorporate them to the new file, too.

### Step 3

Add the necessary configuration values to the CAS section of the new security file. The comments should be self-explanatory.

You must modify several settings in the sample to point to your CAS server:

    <bean id="casEntryPoint" class="org.springframework.security.cas.web.CasAuthenticationEntryPoint">
      <property name="loginUrl" value="https://auth-test.berkeley.edu/cas/login"/>
      <property name="serviceProperties" ref="serviceProperties"/>
    </bean>

    <bean id="casAuthenticationProvider" class="org.springframework.security.cas.authentication.CasAuthenticationProvider">
      <property name="userDetailsService" ref="userDetailsService"/>
      <property name="serviceProperties" ref="serviceProperties" />
      <property name="ticketValidator">
        <bean class="org.jasig.cas.client.validation.Cas20ServiceTicketValidator">
          <constructor-arg index="0" value="https://auth-test.berkeley.edu/cas" />
        </bean>
      </property>
      <property name="key" value="cas"/>
    </bean>

You will also need to set the public URL for your Opencast server:

    <bean id="serviceProperties" class="org.springframework.security.cas.ServiceProperties">
      <property name="service" value="http://localhost:8080/j_spring_cas_security_check"/>
      <property name="sendRenew" value="false"/>
    </bean>


Authorization
-------------

Now the system knows all the information necessary to authenticate users against CAS, but also need some authorization information, to tell which services the user is allowed to use and which resources is allowed to see and/or modify.

You will need to configure a UserProvider to look up users as identified by CAS.

  - [Sakai User Provider](security.user.sakai.md)
  - [LDAP User Provider](security.ldap.md) (Section `Authorization/Step 2`)


Original documentation from University of Saskatchewan
------------------------------------------------------

[University of Saskatchewan CAS and LDAP integration](https://opencast.jira.com/wiki/display/MH/University+of+Saskatchewan+CAS+and+LDAP+integration)
