Configuration for JWT-based Authentication and Authorization
============================================================

This page describes how to configure Opencast to enable authentication and authorization based on
[JSON Web Tokens (JWTs)](https://datatracker.ietf.org/doc/html/rfc7519). With this feature, a login-mechanism based
on the [OpenID Connect (OIDC)](https://openid.net/connect/) protocol can be configured, since OIDC uses JWTs.

Prerequisites
-------------

This guide assumes that a JWT provider is already setup and Opencast receives the JWT within an HTTP request header,
or in a request parameter (i.e. a query parameter for `GET`-requests, and a form parameter for `POST`-requests).
In order to integrate Opencast with an OIDC provider you could use the
[oauth2-proxy](https://github.com/oauth2-proxy/oauth2-proxy).

Spring Security Configuration
-----------------------------

In order to active JWT-based authentication and authorization, you will need to uncomment and adapt the following
sections found in `etc/security/mh_default_org.xml`. Some of the options are configured with the
[Spring Expression Language (SpEL)](https://docs.spring.io/spring-framework/docs/3.0.x/reference/expressions.html).

* Enable the `preauthAuthProvider`.
```xml
<!-- Uncomment this if using Shibboleth or JWT authentication -->
<sec:authentication-provider ref="preauthAuthProvider" />
```
* Enable the `userReferenceProvider`.
```xml
<!-- Uncomment to enable external users e.g. used together with shibboleth or JWT -->
<osgi:reference id="userReferenceProvider" cardinality="1..1"
                interface="org.opencastproject.userdirectory.api.UserReferenceProvider" />
```
* Add the configured `jwtHeaderFilter` and/or `jwtRequestParameterFilter` to the filters list
  of the `preAuthenticationFilters` bean.
```xml
<!-- Uncomment the line below to support JWT. -->
<ref bean="jwtHeaderFilter" />
<!-- Additionally/alternatively uncomment this to support passing a JWT in a URL parameter. -->
<ref bean="jwtRequestParameterFilter" />
```
* Configure the `jwtHeaderFilter` and/or `jwtRequestParameterFilter` beans.
```xml
<!-- General JWT header extraction filter -->
<bean id="jwtHeaderFilter" class="org.opencastproject.security.jwt.JWTRequestHeaderAuthenticationFilter">
  <!-- Name of the HTTP request header that contains the JWT (default: SM_USER) -->
  <property name="principalRequestHeader" value="Authorization"/>
  <!-- Prefix string occurring before the JWT value within the configured header (default: null) -->
  <property name="principalPrefix" value="Bearer "/>
  <property name="authenticationManager" ref="authenticationManager" />
  <property name="loginHandler" ref="jwtLoginHandler" />
  <!-- Throws an exception if a request is missing the configured header (default: true) -->
  <property name="exceptionIfHeaderMissing" value="false" />
</bean>

<!-- General JWT request parameter extraction filter -->
<bean id="jwtRequestParameterFilter" class="org.opencastproject.security.jwt.JWTRequestParameterAuthenticationFilter">
  <!-- Name of the request parameter that contains the JWT (default: jwt) -->
  <property name="parameterName" value="jwt" />
  <property name="authenticationManager" ref="authenticationManager" />
  <property name="loginHandler" ref="jwtLoginHandler" />
  <!-- Throws an exception if a request is missing the configured parameter (default: true) -->
  <property name="exceptionIfParameterMissing" value="false" />
</bean>
```
* Configure the `jwtLoginHandler`. For the JWT validation, either configure the `secret` property for JWTs signed with
  a symmetric algorithm or configure the `jwksUrl` for JWTs signed with an asymmetric algorithm. The `jwksUrl` should
  provide the [JSON Web Key Set (JWKS)](https://datatracker.ietf.org/doc/html/rfc7517). If both `secret` and `jwksUrl`
  are specified an exception is thrown and the login mechanism will not be activated. So make sure to configure only one
  of them.
```xml
<!-- JWT login handler -->
<bean id="jwtLoginHandler" class="org.opencastproject.security.jwt.DynamicLoginHandler">
  <property name="userDetailsService" ref="userDetailsService" />
  <property name="userDirectoryService" ref="userDirectoryService" />
  <property name="securityService" ref="securityService" />
  <property name="userReferenceProvider" ref="userReferenceProvider" />
  <!-- JWKS URL to use for JWT validation (asymmetric algorithms) (default: null) -->
  <property name="jwksUrl" value="https://auth.example.org/.well-known/jwks.json" />
  <!-- How many minutes to cache a fetched JWKS before re-fetching (default: 1440) -->
  <property name="jwksCacheExpiresIn" value="1440" />
  <!-- Secret to use for JWT validation (symmetric algorithms) (default: null) -->
  <property name="secret" value="***" /> <-- Change this
  <property name="expectedAlgorithms" ref="jwtExpectedAlgorithms" />
  <property name="claimConstraints" ref="jwtClaimConstraints" />
  <!-- Mapping used to extract the username from the JWT (default: null) -->
  <property name="usernameMapping" value="['username'].asString()" />
  <!-- Mapping used to extract the name from the JWT (default: null) -->
  <property name="nameMapping" value="['name'].asString()" />
  <!-- Mapping used to extract the email from the JWT (default: null) -->
  <property name="emailMapping" value="['email'].asString()" />
  <property name="roleMappings" ref="jwtRoleMappings" />
  <!-- JWT cache holds already validated JWTs to not always re-validate in subsequent requests -->
  <!-- Maximum number of validated JWTs to cache (default: 500) -->
  <property name="jwtCacheSize" value="500" />
  <!-- How many minutes to cache a JWT before re-validating (default: 60) -->
  <property name="jwtCacheExpiresIn" value="60" />
</bean>
```
* Configure the `jwtExpectedAlgorithms` list. This list holds the allowed algorithms with which a valid JWT may be
  signed (`alg` claim).
```xml
<!-- The signing algorithms expected for the JWT signature -->
<util:list id="jwtExpectedAlgorithms" value-type="java.lang.String">
  <value>RS256</value>
</util:list>
```
* Configure the `jwtClaimConstraints` list. This list contains claim constraints that are expected to be fulfilled by
  a valid JWT. If you are using JWTs for OpenID Connect, see the
  [specification](https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation) for claims that must be
  validated.
```xml
<!-- The claim constraints that are expected to be fulfilled by the JWT -->
<util:list id="jwtClaimConstraints" value-type="java.lang.String">
  <value>containsKey('iss')</value>
  <value>containsKey('aud')</value>
  <value>containsKey('username')</value>
  <value>containsKey('name')</value>
  <value>containsKey('email')</value>
  <value>containsKey('domain')</value>
  <value>containsKey('affiliation')</value>
  <value>['iss'].asString() eq 'https://auth.example.org'</value>
  <value>['aud'].asString() eq 'client-id'</value>
  <value>['username'].asString() matches '.*@example\.org'</value>
  <value>['domain'].asString() eq 'example.org'</value>
  <value>['affiliation'].asList(T(String)).contains('faculty@example.org')</value>
</util:list>
```
* Configure the `jwtRoleMappings` list. This list contains expressions used to construct Opencast roles from JWT
  claims.
```xml
<!-- The mapping from JWT claims to Opencast roles -->
<util:list id="jwtRoleMappings" value-type="java.lang.String">
  <value>'ROLE_JWT_USER'</value>
  <value>'ROLE_JWT_USER_' + ['username'].asString()</value>
  <value>('ROLE_JWT_OWNER_' + ['username'].asString()).replaceAll("[^a-zA-Z0-9]","_").toUpperCase()</value>
  <value>['domain'] != null ? 'ROLE_JWT_ORG_' + ['domain'].asString() + '_MEMBER' : null</value>
  <value>['username'].asString() eq ('j_doe01@example.org') ? 'ROLE_ADMIN' : null</value>
  <value>['affiliation'].asList(T(String)).contains('faculty@example.org') ? 'ROLE_GROUP_JWT_TRAINER' : null</value>
</util:list>
```
* Enable single log out (optional). Make sure to comment out the standard `logoutSuccessHandler` (otherwise the
  standard logout mechanism will still be active and the configured logout URL will not be used). Some authentication
  providers allow to specify a redirection URL to which the user is redirected after a successful logout (e.g. using an
  URL parameter like `rd`). Make sure to change the URL in the example below.
```xml
<!-- Enables log out -->
<!-- <sec:logout success-handler-ref="logoutSuccessHandler" /> -->

<!-- JWT single log out -->
<sec:logout logout-success-url="https://auth.example.org/sign_out?rd=http://www.opencast.org" />
```
