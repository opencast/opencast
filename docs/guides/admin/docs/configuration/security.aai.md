Authentication and Authorization Infrastructure (AAI) Configuration
===================================================================

This page describes how to configure Opencast to take advantage of the Authentication and Authorization
Infrastructure (AAI).

Prerequesites
-------------

This guides assumes that you know how to setup and configure a Shibboleth Service Provider, i.e. you are assumed
to already have performed the following steps:

- Registeration of your Shibboleth Service Provider at your Shibboleth Federation Service Registry
- Setup and configuration of Shibboleth on the servers you want to use it
- Configuration of your web server

In case you require help on this, contact the institution responsilbe for managing the Shibboleth Federation you
are part of.

An informative list of Shibboleth Federations can be found on:

[https://refeds.org/federations](https://refeds.org/federations)

Step 1: Configuration of the AAI Login handler
-----------------------------------------------------

Opencast ships with a configurable AAI Login handler that needs to be adjusted to your environment.
The configuration can be found in `etc/org.opencastproject.security.aai.ConfigurableLoginHandler.cfg`.

First off all, enable the AAI login handler:

    enabled=true

For bootstrapping purposes, you might want to configure the AAI bootstrap user:

    bootstrap.user.id=<AAI ID>

That user will be assigned ROLE_ADMIN at login time. This enables you to access the administrative UI and
configure user authorization without the need to fiddle with the database directly.
Once user authorization has been setup, disable the AAI bootstrap user.

Since the HTTP request header names required by the AAI login handler are specific to Shibboleth Federations,
you will need to first adjust the following properties.

Set the following header names to the correct values:

    header.given_name = "<Name of Shibboleth attribute>"
    header.surname = "<Name of Shibboleth attribute>"
    header.email = "<Name of Shibboleth attribute>"
    header.home_organization = "<Name of Shibboleth attribute>"

Optionally, you can configure the name of some basic roles the AAI login handler will assign to authenticated users.

The prefix of the user role will determine what unique role a given Shibboleth user has. The role is of the
form *role.user.prefix + Unique ID provided by Shibboleth*.

    role.user.prefix = "ROLE_AAI_USER_"

To indicate the AAI home organization a user belongs to, the organization membership role is assigned to the user.
The role is of the form *role.organization.prefix + Home Organization provided by Shibboleth +
role.organization.suffix*

    role.organization.prefix = "ROLE_AAI_ORG_"
    role.organization.suffix = "_MEMBER"

To indicate the fact that a user has authenticated himself using Shibboleth, the login handler assigns the
role as specified by the property *role.federation*.

    role.federation = "ROLE_AAI_USER"

Step 2: Spring Security Configuration
-------------------------------------

In order to take advantage of Shibboleth authentication, you will need to uncomment the following lines found
in `etc/security/mh_default_org.xml`:

The Shibboleth header authentification filter needs to be enabled to get access to the Shibboleth information
within the HTTP request headers.

    <!-- Shibboleth header authentication filter -->
    <sec:custom-filter ref="shibbolethHeaderFilter" position="PRE_AUTH_FILTER"/>

To ensure that a logout is not just logging out the user from the Opencast application but also from Shibboleth,
you will need to configure the logout-success-url:

    <!-- Enables log out -->
    <sec:logout logout-success-url="/Shibboleth.sso/Logout?return=www.opencast.org" />

**IMPORTANT:** In the section *Shibboleth Support*, be sure to adapt the value of *principalRequestHeader* to the
respective name of the Shibboleth attribute you use in your Shibboleth Federation:

    <!-- ###################### -->
    <!-- # Shibboleth Support # -->
    <!-- ###################### -->

    <!-- General Shibboleth header extration filter -->
    <bean id="shibbolethHeaderFilter"
          class="org.opencastproject.security.shibboleth.ShibbolethRequestHeaderAuthenticationFilter">
      <property name="principalRequestHeader" value="<Shibboleth attribute name>"/>
      <property name="authenticationManager" ref="authenticationManager" />
      <property name="userDetailsService" ref="userDetailsService" />
      <property name="userDirectoryService" ref="userDirectoryService" />
      <property name="shibbolethLoginHandler" ref="configurableLoginHandler" />
      <property name="exceptionIfHeaderMissing" value="false" />
    </bean>

    <!-- AAI specific header extractor and user generator -->
    <bean id="configurableLoginHandler" class="org.opencastproject.security.aai.ConfigurableLoginHandler">
      <property name="securityService" ref="securityService" />
      <property name="userReferenceProvider" ref="userReferenceProvider" />
    </bean>

    <bean id="preauthAuthProvider"
          class="org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider">
      <property name="preAuthenticatedUserDetailsService">
        <bean id="userDetailsServiceWrapper"
              class="org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper">
          <property name="userDetailsService" ref="userDetailsService"/>
        </bean>
      </property>
    </bean>

Finally be sure to enable the user reference provider to enable support for externally provided users:

    <osgi:reference id="userReferenceProvider" cardinality="1..1"
                  interface="org.opencastproject.userdirectory.api.UserReferenceProvider" />

Since the Opencast login page is not used when Shibboleth authentication is in place, there is no point in redirecting
unauthenticated requests to the Opencast login form. You can redirect them directly to the administrative user
interface which is supposed to be protected by Shibboleth.

    <!-- Redirects unauthenticated requests to the login form -->
    <bean id="userEntryPoint" class="org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint">
      <property name="loginFormUrl" value="/admin-ng/index.html" />
    </bean>

Last but not least, you need to add the *preauthAuthProvider* authentication provider to the *authentication-manager*:

    <sec:authentication-manager alias="authenticationManager">
      <sec:authentication-provider ref="preauthAuthProvider">
      <sec:authentication-provider user-service-ref="userDetailsService">
        <sec:password-encoder hash="md5"><sec:salt-source user-property="username" /></sec:password-encoder>
      </sec:authentication-provider>
    </sec:authentication-manager>

Step 3: Protecting HTML pages by Shibboleth
-------------------------------------------

It is important to understand that Shibboleth is only used to protect content that is accessed by human users.
Access to APIs is protected by other means of authentication as, for example, digest authentication.

To protect HTML pages, you will need to adapt the configuration of your web server:

    <LocationMatch \.(htm|html)$>
        AuthType shibboleth
        ShibRequireSession On
        ShibUseHeaders On
        require valid-user
    </LocationMatch>

