VideoEditorWorkflowOperationHandler
===================================

Description
-----------

The editor operation processes the edited files. This operation needs the videoeditor API and impl (or remote on
distributed systems) to be installed.

## Parameter Table

|configuration keys|example    |description                                                    |
|------------------|-----------|---------------------------------------------------------------|
|source-flavors    |`*/work`   |The flavor(s) of all media files to process                    |
|smil-flavors      |`*/smil`   |The flavor(s) of the SMIL file(s) to be used                   |
|skipped-flavors   |`*/work`   |The flavor(s) of all media files to be "processed" (cloned) if the editor operation is skipped|
|target-flavor-subtype|`trimmed`|The flavor subtype to be applied to all resulting videos, e.g. for a value of `baz`, a track with flavor `foo/bar` will generate another track with flavor `foo/baz`|
|target-smil-flavor| `smil/cutting` |the flavor of the SMIL file containing the final video segments.<br/>Should be the same as the `smil.catalog.flavor` property in `etc/org.opencastproject.adminui.cfg`|
|*preview_flavors*|*`*/preview`*|*(Legacy) Flavors used to preview the video in the editor.*<br/>***Currently has no effect. Preview flavors are now configured in the file `etc/org.opencastproject.adminui.cfg`***|
|*interactive*|*`false`*|*(Legacy) If `true` make the operation interactive, i.e. pause and wait for user input.*<br/>***Do not use. Interactive operations are deprecated in the current API.***|

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
