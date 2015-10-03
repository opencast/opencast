AppendWorkflowHandler
=====================

> *This operations has been deprecated with opencast 2.0*

Description
-----------

The AppendWorkflowOperation can be used to select additional workflows which should be appended to the current one. This
basically means that it can be used to build a workflow selection workflow, making it possible to select workflows after
the media was ingested.


Operation Example
-----------------

this example shows a complete workflow selection workflow:

    <?xml version="1.0" encoding="UTF-8"?>
    <definition xmlns="http://workflow.opencastproject.org">
      <id>default</id>
      <description>Puts mediapackages on hold</description>
      <operations>
        <operation
          id="append"
          fail-on-error="true"
          description="Hold for workflow selection">
        </operation>
      </operations>
    </definition>
