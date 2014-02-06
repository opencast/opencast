<link rel="stylesheet" href="ui-files/lib/codemirror/lib/codemirror.css">
<link rel="stylesheet" href="ui-files/lib/codemirror/addon/hint/show-hint.css">

<script src="ui-files/lib/codemirror/lib/codemirror.js"></script>
<script src="ui-files/lib/codemirror/addon/hint/show-hint.js"></script>
<script src="ui-files/lib/codemirror/addon/hint/xml-hint.js"></script>
<script src="ui-files/lib/codemirror/mode/xml/xml.js"></script>

<style type="text/css">
      .CodeMirror { 
    	  border: 0px solid #eee; 
      	  height: 100%;
      }
</style>

<script src="ui-files/js/utils/requestHandler.js"></script>
<script src="ui-files/js/utils/messageRenderer.js"></script>
<script src="ui-files/js/workflows/codeMirrorSchemaAndConfiguration.js"></script>
<script src="ui-files/js/workflows/workflowViewHandler.js"></script>
<script src="ui-files/js/utils/restartSystemScript.js"></script>

<div class="layout-page-content">
    
    <!-- file tree >
	<div class="config-file-tree">
    	<div id="workflowsTree"></div>
    </div-->

    <form id="uploadForm" name="uploadForm" action="" method="POST" enctype="multipart/form-data" accept-charset="UTF-8">
      
      <div class="form-box-main layout-centered container ui-widget">
        
        <div class="form-box-head ui-widget-header ui-corner-top">
        	<span>Workflows</span>
        </div>      
    	<div class="form-box-content ui-widget-content ui-corner-bottom">
			<div>
           		Workflow Files:<select id="workflow_selection" onchange="request.sendPostRequestWithWorkflowFileName()"/>       
            </div>
            <div class="ui-widget-content-margin-top">
           		<form><textarea id="code" name="code"></textarea></form>
           	</div>
        </div>
      </div>
    </form>
</div>

<script type="text/javascript">
  $("#config-controls").hide();
  $("#workflow-controls").attr("style", "visibility:visible");
</script>

<div id="messageRestart"/>
<div id="message"/>
<div id="messageCert"/>
<div id="newWorkflowFile"/>