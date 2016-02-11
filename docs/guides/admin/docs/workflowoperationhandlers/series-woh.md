# SeriesWorkflowOperationHandler

## Description
The SeriesWorkflowOperation will apply a series to the mediapackage.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|series|"0d06537e-09d3-420c-8314-a21e45c5d032"|The optional series identifier. If empty the current series of the medipackage will be taken.|EMPTY|
|attach|"creativecommons/\*,dublincore/\*"|The flavors of the series catalogs to attach to the mediapackage.|EMPTY|
|apply-acl|"true"\|"false"|Whether the ACL should be applied or not.|"false"|


## Operation Example

    <operation
          id="series"
          fail-on-error="true"
          exception-handler-workflow="error"
          description="Applying series to mediapackage">
          <configurations>
            <configuration key="series">0d06537e-09d3-420c-8314-a21e45c5d032</configuration>
            <configuration key="attach">*</configuration>
            <configuration key="apply-acl">true</configuration>
          </configurations>
    </operation>

