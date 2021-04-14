Configure Central Authentication Service (CAS)
==============================================

Authentication
--------------

Many campuses use some kind of single sign on, such as JASIG's Central Authentication Service, or CAS. This guide
describes how to integrate Opencast into such a system.

### Step 1: Enable Opencast CAS feature 

First, you need to edit the file `etc/org.apache.karaf.features.cfg` and add the `opencast-security-cas` to the
`featuresBoot` variable.

    featuresBoot = ..., opencast-security-cas

### Step 2: Security Configuration

Edit the security configuration file at `etc/security/mh_default_org.xml`. In a multi-tenant set-up, you will have one
configuration file for each tenant at `etc/security/<organization_id>.xml`.

You need to comment or uncomment some sections in this file.
All necessary changes are marked with a `CAS Auth:` tag. You can use the find function of your editor to
find the parts of the file you need to modify.

Add the necessary configuration values to the CAS section of the new security file. The comments should be
self-explanatory.

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

Now the system knows all the information necessary to authenticate users against CAS, but also need some authorization
information, to tell which services the user is allowed to use and which resources is allowed to see and/or modify.

You will need to configure a [UserProvider](security.md) to look up users as identified by CAS.

* LDAP User Provider, described in [LDAP Security and Authorization](security.ldap.md)
* [Sakai User Provider](security.user.sakai.md)
* [Moodle User Provider](security.user.moodle.md)
* [Brightspace D2L User Provider](security.user.brightspace.md)
* [Canvas LMS User Provider](security.user.canvas.md)

Further Information
-------------------
<!-- _This leads to Atlassian/Confluence which needs to be joined first, assuming the user already has an account. Is this necessary to include here? -->
Original documentation from University of Saskatchewan:
[University of Saskatchewan CAS and LDAP
integration](https://opencast.jira.com/wiki/display/MH/University+of+Saskatchewan+CAS+and+LDAP+integration)
