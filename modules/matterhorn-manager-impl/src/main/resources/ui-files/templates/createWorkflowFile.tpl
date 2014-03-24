<div id="popup_content" class="layout-page-content">
      <div class="form-box-message layout-centered container ui-widget">   
        <div class="form-box-head ui-widget-header ui-corner-top">
        	<div class="form-box-pos">
        		<span>Create new Workflow File</span>
        	</div>
        </div>      
       	<div class="form-box-message-content ui-widget-content ui-corner-bottom">
				<div class="form-box-message-container center">
	           		<span>New File:</span>
			    		<input id="workflowInputFile" type="text" value="">
	            </div>
	            <div class="form-box-message-container center">
	            	 <span id="fileState" style="font-weight:bold; color:#A72123"></span>
	            </div>
	            <div class="form-box-message-container form-box-message-margin-left">
	            	<button id="saveButton" onclick="saveNewWorkflowFile();" type='button' class='ui-button ui-corner-all ui-widget-button-position'>save</button>
	            	<button id="workflowCancelButton" onclick="message.closeWorkflowFileView();" type='button' class='ui-button ui-corner-all ui-widget-button-position'>cancel</button>
	            </div>
         </div>
      </div>
</div>