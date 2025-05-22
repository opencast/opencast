Configuration for JWT-based Authentication and Authorization
============================================================

This page describes how to configure Opencast to enable authentication and/or authorization based on
[JSON Web Tokens (JWTs)](https://datatracker.ietf.org/doc/html/rfc7519). JWTs can be used in various ways:

- With static file authorization (particularly useful with external tools).
- External apps can send users to Opencast (e.g. for Studio or Editor) with a session that has certain roles.
- A login-mechanism based on the [OpenID Connect (OIDC)](https://openid.net/connect/) protocol can be configured.

*Note*: Some of these use cases might not be perfectly supported by Opencast yet, but are possible in principle.

Prerequisites
-------------

Opencast does not generate JWTs, but reads them in incoming requests. This guide assumes that you already have a JWT provider, i.e. another services that generates JWTs for consumption in Opencast. This could be [Tobira](https://elan-ev.github.io/tobira/setup/auth/jwt), an LMS, an OIDC provider, or something else. These generally have their own setup guides. The JWT has to be passed to Opencast via HTTP header or via request parameter (i.e. a query parameter for `GET`-requests, and a form parameter for `POST`-requests).

In order to integrate Opencast with an OIDC provider you could use the
[oauth2-proxy](https://github.com/oauth2-proxy/oauth2-proxy).

Standard OC Schema for JWTs
---------------------------

After the signature verification, a JWT is just a bunch of "claims" (JSON fields). The communicating parties can decide fairly freely on how to interpret these claims. Protocols like OIDC define exactly which claims are to be used and how. For other cases where external Opencast apps (like Tobira or an LMS) need to communicate with Opencast, this section defines a standard schema to follow. You can configure Opencast differently, but this aims at standardizing JWT usage across Opencast-related applications and offer an out-of-the-box solution.

This schema is designed to be flexible and support all current and conceivable future use cases, while keeping the JWT size for each use case small.

### Quick overview

```json5
{
    // Standard claims
    "exp": 1499863217, // required
    "nbf": 1548632170, // optional

    // User info
    "sub": "jose", // username
    "name": "José Carreño Quiñones",
    "email": "jose@example.com",

    // Authorization, i.e. privileges the request/user should have
    "roles": ["ROLE_STUDIO", "ROLE_API_EVENTS_VIEW"],
    "oc": {
        "e:d622b861-4264-4947-8db1-c754c5956433": ["read, "annotate"],
        "s:4ed02421-144c-42a1-b98a-22e84f3ac691": ["write"]
    }
}
```

### Claims

A claim being "required" means that the JWT provider (e.g. Tobira, LMS) has to include it. Opencast will reject all JWTs which lack required claims.

- `exp` (expiration time): As [defined in the JWT RFC](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4), a number timestamp in seconds since the unix epoch. JWTs with `exp < now()` are rejected by Opencast. This claim is *required*.

- `nbf` (not before): As [defined in the JWT RFC](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.5), a number timestamp in seconds since the unix epoch. JWTs with `nbf > now()` are rejected by Opencast. This claim is optional.

- *User Info*: information about the user that's possibly used by parts of Opencast. Some JWT use cases don't require user information at all (e.g. static file auth), while others do (e.g. Studio). Therefore, these claims are optional. (TODO: specify when this is stored as user reference)
    - `sub`: Opencast username, string. (TODO: explain how this is used to lookup existing users, if at all)
    - `name`: display name, string.
    - `email`: string.

- *Authorization*: at least one of these claims has to be set, as otherwise no privileges are granted (which is the same as not sending a JWT at all).
    - `roles`: array of strings, specifying roles to grant to the request/user directly.
    - `oc`: JSON map that grants access to single items in Opencast, as identified by the map key.
        - The map key consists of a one-letter prefix (the item type), a colon, and finally the actual ID of the Opencast item. The following item types are supported. Note: giving access to a series or playlist only gives access to that item directly, and *not* to its videos (e.g. give write access for a series to be able to edit its metadata or add videos to it).
            - `e`: event
            - `s`: series
            - `p`: playlist
        - The map value is just an array of actions, corresponding to actions in the OC ACLs, e.g. `read` and `write`.


### Examples

#### Static file authorization

An external application wants a user to load a protected static file from Opencast, e.g. a video file. The request needs read permission for the that event. The external app can achieve this with the following JWT (which can simply be attached to the URL as query parameter):

```json
{
    "exp": 1499863217,
    "oc": { "e:d622b861-4264-4947-8db1-c754c5956433": ["read"] }
}
```

#### Opencast Studio

An external application wants to let a user use Opencast Studio to record and upload a video. Since this does not happen in a single request, a browser session needs to be created. This can be achieved with the [`/redirect` API](https://develop.opencast.org/docs.html?path=/redirect). The JWT sent to that API needs to grant permissions to use all required APIs (which can be done by granting `ROLE_STUDIO`) and also specify user info, since Studio uses some use information.

```json
{
    "exp": 1499863217,
    "sub": "peter",
    "name": "Peter Lustig",
    "email": "peter@example.com",
    "roles": ["ROLE_STUDIO"]
}
```



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
  <property name="usernameMapping" value="['sub'].asString()" />
  <!-- Mapping used to extract the name from the JWT (default: null) -->
  <property name="nameMapping" value="['name'].asString()" />
  <!-- Mapping used to extract the email from the JWT (default: null) -->
  <property name="emailMapping" value="['email'].asString()" />
  <!-- Opencast standard role mapping as defined above in this chapter of the docs. -->
  <!-- I.e. reads `roles` and `oc` claims to specify roles. -->
  <property name="ocStandardRoleMappings" value="true" />
  <!-- Custom role mappings, optional -->
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
  <value>containsKey('sub')</value>
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
* Optionally configure the `jwtRoleMappings` list. This list contains expressions used to construct Opencast roles from JWT
  claims. This can be used instead of or in addition to the OC standard role mapping. Each entry in this list contains a SPeL expression that needs to return `null`, or a string, or a `List<String>`, or `String[]`.
```xml
<!-- The mapping from JWT claims to Opencast roles -->
<util:list id="jwtRoleMappings" value-type="java.lang.String">
  <value>'ROLE_JWT_USER'</value>
  <value>'ROLE_JWT_USER_' + ['sub'].asString()</value>
  <value>('ROLE_JWT_OWNER_' + ['sub'].asString()).replaceAll("[^a-zA-Z0-9]","_").toUpperCase()</value>
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
