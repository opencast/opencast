External API Configuration
==========================

The External API is an integral part of Opencast and therefore does not need to be enabled. To be able to access the
External API, you need to configure a user that is authorized to do so.

Perform the following steps to get the External API running:

1. Enable basic authentication (see section Authentication)
2. Create a new user or choose an existing user (administrative user interface)
3. Authorize the user to access the External API (see section Authorization)
4. Test whether access works (see section Testing)

Authentication
--------------

The External API currenlty only supports basic authentication. To enable basic authentication, uncomment the following
blocks in `/etc/security/mh_default.org`:

    <!-- Basic authentication
    <sec:custom-filter after="BASIC_AUTH_FILTER" ref="basicAuthenticationFilter" />
    -->

    <!-- Basic authentication
    <bean id="basicEntryPoint" class="org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint">
      <property name="realmName" value="Opencast"/>
    </bean>
    -->

    <!-- Basic authentication
    <bean id="basicAuthenticationFilter" class="org.springframework.security.web.authentication.www.BasicAuthenticationFilter">
      <property name="authenticationManager" ref="authenticationManager"/>
      <property name="authenticationEntryPoint" ref="basicEntryPoint"/>
    </bean>
    -->

**Note:** Since basic authentication involves sending unencrypted passwords over the network, it is strongly
recommended to use HTTPS.

Authorization
-------------

The External API supports fine-grained access control on request level allowing it to be tailored to your
specific needs. A number of roles are used to authorize access to individual endpoints. Those roles can be configured
directly in the Opencast administrative user interface.

**Note:** Users owning the role ROLE_ADMIN have full access to the External API.

**Base API**

|ROLE     |METHOD | URL                                                                       |
|---------|-------|---------------------------------------------------------------------------|
|ROLE_API |GET    |/api<br>/api/info/\*<br>/api/info/me/\*<br>/api/version<br>/api/version/\* |

**Events API**

|ROLE                              |METHOD      | URL                                                          |
|----------------------------------|------------|--------------------------------------------------------------|
|ROLE_API_EVENTS_CREATE            |POST        |/api/events                                                   |
|ROLE_API_EVENTS_VIEW              |GET         |/api/events<br>/api/events/\*<br>                             |
|ROLE_API_EVENTS_EDIT              |PUT<br>POST |/api/events/\*<br><br>/api/events/\*                          |
|ROLE_API_EVENTS_DELETE            |DELETE      |/api/events/\*                                                |
|ROLE_API_EVENTS_ACL_VIEW          |GET         |/api/events/\*/acl                                            |
|ROLE_API_EVENTS_ACL_EDIT          |PUT<br>POST |/api/events/\*/acl<br>/api/events/\*/acl/\*                   |
|ROLE_API_EVENTS_ACL_DELETE        |DELETE      |/api/events/\*/acl/\*/\*                                      |
|ROLE_API_EVENTS_MEDIA_VIEW        |GET         |/api/events/\*/media<br>/api/events/\*/media/\*               |
|ROLE_API_EVENTS_METADATA_VIEW     |GET         |/api/events/\*/metadata<br>/api/events/\*/metadata/\*         |
|ROLE_API_EVENTS_METADATA_EDIT     |PUT         |/api/events/\*/metadata<br>/api/events/\*/metadata/\*         |
|ROLE_API_EVENTS_METADATA_DELETE   |DELETE      |/api/events/\*/metadata<br>/api/events/\*/metadata/\*         |
|ROLE_API_EVENTS_PUBLICATIONS_VIEW |GET         |/api/events/\*/publications<br>/api/events/\*/publications/\* |
|ROLE_API_EVENTS_SCHEDULING_EDIT   |PUT         |/api/events/\*/scheduling                                     |
|ROLE_API_EVENTS_SCHEDULING_VIEW   |GET         |/api/events/\*/scheduling                                     |

**Series API**

|ROLE                            |METHOD | URL                                                  |
|--------------------------------|-------|------------------------------------------------------|
|ROLE_API_SERIES_CREATE          |POST   |/api/series                                           |
|ROLE_API_SERIES_VIEW            |GET    |/api/series<br>/api/series/\*                         |
|ROLE_API_SERIES_EDIT            |PUT    |/api/series/\*                                        |
|ROLE_API_SERIES_ACL_VIEW        |GET    |/api/series/\*/acl                                    |
|ROLE_API_SERIES_ACL_EDIT        |PUT    |/api/series/\*/metadata<br>/api/series/\*/metadata/\* |
|ROLE_API_SERIES_METADATA_VIEW   |GET    |/api/series/\*/metadata<br>/api/series/\*/metadata/\* |
|ROLE_API_SERIES_METADATA_EDIT   |PUT    |/api/series/\*/metadata<br>/api/series/\*/metadata/\* |
|ROLE_API_SERIES_METADATA_DELETE |DELETE |/api/series/\*/metadata<br>/api/series/\*/metadata/\* |
|ROLE_API_SERIES_PROPERTIES_VIEW |GET    |/api/series/\*/properties                             |
|ROLE_API_SERIES_PROPERTIES_EDIT |PUT    |/api/series/\*/properties                             |
|ROLE_API_SERIES_DELETE          |DELETE |/api/series/\*                                        |


**Groups API**

|ROLE                   |METHOD      | URL                                        |
|-----------------------|------------|--------------------------------------------|
|ROLE_API_GROUPS_CREATE |POST        |/api/groups                                 |
|ROLE_API_GROUPS_VIEW   |GET         |/api/groups<br>/api/groups/\*               |
|ROLE_API_GROUPS_EDIT   |PUT<br>POST |/api/groups/\*<br>/api/groups/\*/members/\* |
|ROLE_API_GROUPS_DELETE |DELETE      |/api/groups/\*                              |

**Security API**

|ROLE                   |METHOD | URL               |
|-----------------------|-------|-------------------|
|ROLE_API_SECURITY_EDIT |POST   |/api/security/sign |

**Agents API**

|ROLE                         |METHOD | URL                           |
|-----------------------------|-------|-------------------------------|
|ROLE_API_CAPTURE_AGENTS_VIEW |GET    |/api/agents</br>/api/agents/\* |

**Administrative API**

|ROLE       |METHOD | URL               |
|-----------|-------|-------------------|
|ROLE_ADMIN |POST   |/api/recreateIndex |

**Workflow API**

|ROLE                                |METHOD | URL                                                    |
|------------------------------------|-------|--------------------------------------------------------|
|ROLE_API_WORKFLOW_INSTANCE_CREATE   |POST   |/api/workflow                                           |
|ROLE_API_WORKFLOW_INSTANCE_VIEW     |GET    |/api/workflow<br>/api/workflow/\*                       |
|ROLE_API_WORKFLOW_INSTANCE_EDIT     |PUT    |/api/workflow/\*                                        |
|ROLE_API_WORKFLOW_INSTANCE_DELETE   |DELETE |/api/workflow/\*                                        |
|ROLE_API_WORKFLOW_DEFINITION_VIEW   |GET    |/api/workflow-definition<br>/api/workflow-definition/\* |

**User- and Role-switching**

The External API supports user- and role-switching, i.e. it is possible to perform requests on behalf of another
user or role. The be able to perform this kind of requests, the user doing the actual requests needs to own ROLE_SUDO.

For more details on this API, please take a look at the developer documentation under External API.

Testing
-------

    curl -u <api-user>:<api-user-passowrd> <admin-node>/api/info/me

should return a JSON containing information about the user *api-user*.

Accessing Distribution Artefacts
--------------------------------

A major use case of the External API is to provide External Applications secure access to distribution artefacts.

For this purpose, Opencast comes with a special workflow operation: WOH publish-configure
(see [ConfigurablePublishWorkflowOperationHandler](../workflowoperationhandlers/publish-configure-woh.md))
creates publication elements that do not just contain a single URL to the publication channel,
but also contain URLs for each of the attachments and tracks that have been published.

**Note:** Secure access to distribution artefacts requires stream security to be enabled,
see [Stream Security Configuration](stream-security.md).
