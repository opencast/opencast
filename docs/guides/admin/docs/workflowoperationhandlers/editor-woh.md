VideoEditorWorkflowOperationHandler
===================================

Description
-----------

The editor operation processes the edited files. This operation needs the videoeditor API and impl (or remote on
distributed systems) to be installed.

## Parameter Table

|configuration keys|example    |description                                                    |
|------------------|-----------|---------------------------------------------------------------|
|source-flavors    |`*/work`   |the subtype of all media files to use                          |
|smil-flavors      |`*/smil`   |the smil file(s) to be used                                    |
|skipped-flavors   |`*/work`   |the subtype of all media files to be used if the editor skipped|
|target-smil-flavor| `smil/cutting` | the target-smil-flavor, should be the same as 'smil.catalog.flavor' in org.opencastproject.adminui.cfg| 

## Operation Example

    <operation
      id="editor"
      if="${trimHold}"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Waiting for user to review / video edit recording">
      <configurations>
        <configuration key="source-flavors">*/work</configuration>
        <configuration key="skipped-flavors">*/work</configuration>
        <configuration key="smil-flavors">*/smil</configuration>
        <configuration key="target-smil-flavor">smil/cutting</configuration>
        <configuration key="target-flavor-subtype">trimmed</configuration>
      </configurations>
    </operation>
