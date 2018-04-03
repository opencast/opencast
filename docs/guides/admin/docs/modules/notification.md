# Mattermost Notification
#### Example for mattermost-notify operation:

~~~~
  <operation
      id="mattermost-notify"
      fail-on-error="false"
      exception-handler-workflow="error"
      description="Notify mattermost about error">
      <configurations>
        <configuration key="url">insert-url-of-mattermost-webhook</configuration>
        <configuration key="message">Error at Workflow %i (%t) State: %s</configuration>
        <configuration key="method">post</configuration>
        <configuration key="max-retry">3</configuration>
        <configuration key="timeout">5</configuration>
      </configurations>
    </operation>
~~~~

#### List of parameters
All parameters (%<letter>) will be substituted with corresponding meta-data of current workflow instance.

Parameter | Meta-Data  
--- | :---:
%t | Title of workflow
%i | ID of workflow
%s | State of workflow
%o | ID of current workflow operation
%I | ID of Mediapackage
%T | Title of Mediapackage
%C | Creators of Mediapackage
%c | Contributors of Mediapackage
%D | Date of Mediapackage
%d | Duration of Mediapackage
%L | License of Mediapackage
%l | Language of Mediapackage
%S | Series-Title of Mediapackage
