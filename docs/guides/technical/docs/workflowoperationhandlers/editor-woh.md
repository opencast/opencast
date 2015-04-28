# VideoEditorWorkflowOperationHandler

## Description
The editor operation provides the UI for editing trim hold state and processes the edited files. This operation needs the videoeditor API and impl (or remote on distributed systems) to be installed.

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-flavors	|"*/work"	|the subtype of all media files in the best available quality and in a codec that can be processed by the videoeditor modules. The *-should usually not be changed, as tracks can be excluded in the editor UI too, only the subtype is important. All needed videos should be available within this flavor|EMPTY|
|preview-flavors	|"*/preview"	|the subtype of the media files that should be used for the preview player. This is an HTML5 player so the coded can be H.264 or WebM based on the browser. The main flavor should be the same as in source-flavors.	|EMPTY|
|smil-flavors |"*/smil"| the smil file(s) that should be used as a proposal within the editor UI. If * is used presenter/smil will be favored, if this is not available the first in the list will be used.|EMPTY|
|skipped-flavors|"*/work"	|the subtype of all media files that should be used in the following processing, if the editor operation was skipped|EMPTY|

## Video Editor UI

![Videoeditor UI](workflowoperationhandlers/editor.png)

## Operation Example

    <operation
      id="editor"
      if="${trimHold}"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Waiting for user to review / video edit recording">
      <configurations>
        <configuration key="source-flavors">*/work</configuration>
        <configuration key="preview-flavors">*/preview</configuration>
        <configuration key="skipped-flavors">*/work</configuration>
        <configuration key="smil-flavors">*/smil</configuration>
        <configuration key="target-smil-flavor">episode/smil</configuration>
        <configuration key="target-flavor-subtype">trimmed</configuration>
      </configurations>
    </operation>
