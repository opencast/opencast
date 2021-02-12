Send Email Workflow Operation
=============================

ID: `send-email`

Description
-----------

The send-email operation invokes the SMTP Service to send an email with the parameters provided.
It is useful to send email notifications when some operation(s) have been completed or some error(s) have occurred in a
workflow.

The email body, if not specified by body or body-template-file, will consist of a single line of the form:
`<Recording Title> (<Mediapackage ID>)`.

Freemarker templates can be used in the following fields to allow replacement with values obtained from the workflow or
media package: to, cc, bcc, subject, and body. If body-template-file is specified, the operation will use a Freemarker template
file located in `<config_dir>/etc/email` to generate the email body.

User names can be provided in `to`, `cc`, or `bcc` in lieu of email addresses so that the user directory is searched
and that user's email address is used (see Example 5).


Parameter Table
---------------

|configuration keys|description|default value|example|
|------------------|-------|-----------|-------------|
|body|Email body content.<br>Takes precedence over body-template-file.|`<Recording Title> (<Mediapackage ID>)`|Lecture 1 (4bf316fc-ea78-4903-b00e-9976b0912e4d)|
|body-template-file|Name of file that will be used as a template for the content of the email body.|EMPTY|templateName|
|subject|Email subject.|EMPTY|Operation has been completed|
|to|The field `to` of the email<br>i.e. the comma separated list of email accounts the email will be sent to.|EMPTY|email-account@email-domain.org,second-account@second-domain.org|
|cc|The field `cc` of the email<br>i.e. the comma separated list of email accounts that will receive a carbon copy of the email.|EMPTY|email-account@email-domain.org,second-account@second-domain.org|
|bcc|The field `bcc` of the email<br>i.e. the comma separated list of email accounts that will receive a blind carbon copy of the email.|EMPTY|email-account@email-domain.org,second-account@second-domain.org|
|use-html|Flag to indicate that the email content should be displayed as 'text/html'|false|true/false|

**Some other email parameters can be customized in the SMTP Service configuration**


Variable Substitution
---------------------

The template will have access to the media package, workflow instance (including its configuration properties and last
failed operation), catalogs, and any incidents. Fields should be tested for null/empty values before being used.

### Media Package Information

Use `${mediaPackage.FIELD}`

#### Examples

|Field                  |How To Get It|
|-----------------------|-------------|
|media package id       |${mediaPackage.identifier}|
|recording title        |${mediaPackage.title}|
|recording date and time|${mediaPackage.date?datetime?iso_utc} - See Freemarker manual for date manipulation <br>(extract date only, time only, format, etc)|
|series title           |${mediaPackage.seriesTitle}|
|series id              |${mediaPackage.series}|

### Workflow Information

Use `${workflow.FIELD}`

|Field                    |How To Get It             |
|-------------------------|--------------------------|
|workflow id              |${workflow.id}            |
|workflow template id     |${workflow.template}      |
|workflow state           |${workflow.state}         |
|workflow title           |${workflow.title}         |
|workflow description     |${workflow.description}   |
|id of parent workflow    |${workflow.parentId}      |
|name of workflow creator |${workflow.creatorName}   |
|workflow organization    |${workflow.organizationId}|

Note that some of these values may not exist depending on the workflow.


### Workflow Configuration Properties

Use `${workflowConfig['PROPERTY']}`

### Last Failed Operation

Operation that caused the workflow to fail. Should be tested before accessing any of its fields:

    <#if failedOperation?has_content>Workflow failed in operation: ${failedOperation.template}</#if>


### Incidents

In your email template:

```
<#if incident?has_content>
  <#list incident as inc>
    <#list inc.details as dets>${dets.b}</#list>
  </#list>
</#if>
```

### Catalog fields

Use `${catalogs['SUBTYPE']['FIELD']}`

#### Examples

|Field          |How To Get It|
|---------------|-------------|
|episode creator|${catalogs['episode']['creator']}|
|episode title  |${catalogs['episode']['title']}|
|series creator |${catalogs['series']['creator']}|
|series title   |${catalogs['series']['title']}|


Examples
--------

### Example 1

Media package title in subject field, default email body.

```xml
<operation
  id="send-email"
  fail-on-error="false"
  exception-handler-workflow="email-error"
  description="Sending email to user after media package is published">
  <configurations>
    <configuration key="to">email-account@email-domain.org</configuration>
    <!-- This is going to be replaced with the media package title -->
    <configuration key="subject">${mediaPackage.title} has been published</configuration>
    <!-- Neither body or body-template-file specified so default body <Recording Title> (<Mediapackage ID>)<br>is sent -->
  </configurations>
</operation>
```

### Example 2

To and subject are inline templates; the email body uses a template file named sample stored in`…/etc/email`:

```xml
<operation
  id="send-email"
  fail-on-error="false"
  exception-handler-workflow="email-error"
  description="Sending email to user before holding for edit">
  <configurations>
    <!-- This is going to be replaced with the episode catalog publisher field, which in this example it is assumed
    it contains a notification email address -->
    <configuration key="to">${catalogs['episode']['publisher']}</configuration>
    <!-- This is going to be replaced with the episode catalog title field -->
    <configuration key="subject">${catalogs['episode']['title']} is ready for EDIT</configuration>
    <!-- Email body is going to be built using the sample template found in <config_dir>/etc/email -->
    <configuration key="body-template-file">sample</configuration>
  </configurations>
</operation>
```

#### Template: sample

The contents of the `…/etc/email/sample` email template:

```
Event Details
<#if catalogs['series']?has_content>
Series Title: ${catalogs['series']['title']}
Instructor: ${catalogs['series']['contributor']}
</#if>
Media Package Id: ${mediaPackage.identifier}
Title: ${mediaPackage.title}
Workflow Id: ${workflow.id}
Event Date: ${mediaPackage.date?datetime?iso_local}
```

### Example 3

Email address entered via admin UI as a workflow configuration parameter:

```xml
<operation
  id="send-email"
  fail-on-error="false"
  exception-handler-workflow="email-error"
  description="Sends email">
  <configurations>
    <configuration key="to">${workflowConfig['emailAddress']}</configuration>
    <configuration key="subject">Media package has been published</configuration>
    <configuration key="body-template-file">sample</configuration>
  </configurations>
</operation>
```

Workflow Configuration Panel:

```xml
<configuration_panel>
<![CDATA[
   <!-- Add after the other configuration fields (Holds, Archive, etc) -->
   <fieldset>
      <legend>Notification</legend>
      <ul class="oc-ui-form-list">
        <li class="ui-helper-clearfix">
          <label class="scheduler-label">
            <span class="color-red">* </span><span id="i18n_email_label">Email Address</span>:
          </label>
          <span id="emailconfig">
            <input id="emailAddress" name="emailAddress" type="text" class="configField"
                   value="my-email-account@my-email-domain.org"/>
          </span>
        </li>
      </ul>
    </fieldset>

    <script type="text/javascript">

      // Add email variable
      var emailAddress = $('input#emailAddress');

      // Register email configuration property
      ocWorkflowPanel.registerComponents = function(components){
        /* components with keys that begin with 'org.opencastproject.workflow.config' will be passed
         * into the workflow. The component's nodeKey must match the components array key.
         *
         * Example:'org.opencastproject.workflow.config.myProperty' will be availible at ${my.property}
         */
        // After the other components (Hold, Archive, etc), add:
        components['org.opencastproject.workflow.config.emailAddress'] = new ocAdmin.Component(
          ['emailAddress'],
          {key: 'org.opencastproject.workflow.config.emailAddress'},
          {getValue: function(){ return this.fields.emailAddress.value;}
          });

          //etc...
      }
      ocWorkflowPanel.setComponentValues = function(values, components){
        // After the other components (Hold, Archive, etc), add:
        components['org.opencastproject.workflow.config.emailAddress'].setValue(
          values['org.opencastproject.workflow.config.emailAddress']);
      }
    </script>
]]>
</configuration_panel>
```

### Example 4

In error handling workflow (email-error):

```xml
<operation
  id="send-email"
  fail-on-error="true"
  exception-handler-workflow="error"
  description="Sends email">
    <configurations>
    <!-- Note that you can use variable substitution in to, subject, body
         e.g. ${(catalogs['episode']['FIELD']!'root@localhost'}  -->
    <configuration key="to">root@localhost</configuration>
    <configuration key="subject">Failure processing a mediapackage</configuration>
    <configuration key="body-template-file">errorDetails</configuration>
  </configurations>
</operation>
```

#### Template: errorDetails

The contents of the <config_dir>/etc/email/errorDetails email template:

```
Error Details

<#if catalogs['series']?has_content>
Course: ${catalogs['series']['subject']!'series subject missing'}-${catalogs['series']['title']!'series title missing'}
Instructor: ${catalogs['series']['contributor']!'instructor missing'}
</#if>
Title: ${catalogs['episode']['title']!'title missing'}
Event Date: ${mediaPackage.date?datetime?iso_local}

<#if failedOperation?has_content>
  Workflow failed in operation: ${failedOperation.position}-${failedOperation.template}
  Started: ${failedOperation.dateStarted?datetime?iso_local}
  Ended: ${failedOperation.dateCompleted?datetime?iso_local}
  Execution Host: ${failedOperation.executionHost}
</#if>

Logged incident of the error looks like this:

<#if incident?has_content>
  <#list incident as inc>
    <#list inc.details as dets>${dets.b} </#list>
  </#list>
</#if>
```

### Example 5

The user name is stored in the episode dublin core `contributor` field. There's a user `jharvard` with email
`jharvard@harvard.edu` defined in the system. The message will be sent to `jharvard@harvard.edu`:

```xml
   <operation
      id="send-email"
      fail-on-error="false"
      description="Notify user associated to this recording that it is ready to be trimmed">
      <configurations>
        <configuration key="to">${(catalogs['episode']['contributor'])}</configuration>
        <configuration key="subject">Recording is ready for EDIT</configuration>
        <configuration key="body-template-file">eventDetails</configuration>
      </configurations>
    </operation>
```

#### Episode Dublin Core

```xml
<?xml version="1.0" encoding="UTF-8"?>
<dublincore xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/"
    xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <dcterms:contributor>jharvard</dcterms:contributor>
    <dcterms:created>2018-05-01T16:14:00Z</dcterms:created>
    <dcterms:extent xsi:type="dcterms:ISO8601">PT17M1.933S</dcterms:extent>
    <dcterms:isPartOf>20180229999</dcterms:isPartOf>
    <dcterms:spatial>classroom-20</dcterms:spatial>
    <dcterms:temporal>start=2018-05-01T16:14:00Z; end=2018-05-01T16:31:00Z;
        scheme=W3C-DTF;</dcterms:temporal>
    <dcterms:title>Test Lecture</dcterms:title>
</dublincore>
```


### Example 6

Send an email to the user which started the workflow.
Requires the user account to have a valid email address.

```xml
<operation
    id="send-email"
    description="Sending email to user after media package is published">
  <configurations>
    <!-- Lookup email address of the creator -->
    <configuration key="to">${workflow.creatorName}</configuration>
    <configuration key="subject">${mediaPackage.title} has been published</configuration>
  </configurations>
</operation>
```
