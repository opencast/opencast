# Configure Central Authentication Service (CAS)

## CAS
 
Many campuses use some kind of single sign on, such as JASIG's Central Authentication Service, or CAS. This guide describes how to integrate Matterhorn into such a system.
 
### Step 1
At first uncomment Lines for OpenId and CAS in system.properties as described in the same file:

    file:${felix.home}/lib/ext/cas-client-core-3.1.12.jar \
    file:${felix.home}/lib/ext/com.springsource.org.apache.xml.security-1.4.2.jar \
    file:${felix.home}/lib/ext/com.springsource.org.opensaml-1.1.0.jar \
    file:${felix.home}/lib/ext/spring-security-cas-3.1.0.RELEASE.jar \
    file:${felix.home}/lib/ext/com.springsource.org.jasig.cas.client-3.1.12.jar \
    file:${felix.home}/lib/ext/com.springsource.org.openid4java-0.9.5.jar \
    file:${felix.home}/lib/ext/spring-security-openid-3.1.0.RELEASE.jar
 
### Step 2
To configure matterhorn to use CAS, simply replace the default mh_default_org.xml with the contents of security_sample_cas.xml, available in the Matterhorn source. You must modify several settings in the sample to point to your CAS server:

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

You will also need to set the public URL for your Matterhorn server:

    <bean id="serviceProperties" class="org.springframework.security.cas.ServiceProperties">
      <property name="service" value="http://localhost:8080/j_spring_cas_security_check"/>
      <property name="sendRenew" value="false"/>
    </bean>
 
### Step 3
Assuming you are using matterhorn version 1.4 and are using LDAP for user provisioning, you will need to build and deploy relevant modules with:

    mvn clean install -Pdirectory-ldap,directory-cas,directory-openid -DdeployTo={your runtime server location here}

If not using LDAP, of course, you don't need the directory-ldap module but CAS alone will require deploying both the directory-cas and directory-openid modules.
 
### Step 4
Finally, you will need to configure a UserProvider to look up users as identified by CAS, for example see:

[University of Saskatchewan CAS and LDAP integration](https://opencast.jira.com/wiki/display/MH/University+of+Saskatchewan+CAS+and+LDAP+integration)
