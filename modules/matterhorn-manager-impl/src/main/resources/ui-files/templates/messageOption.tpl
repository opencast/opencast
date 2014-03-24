<div id="popup_content" class="layout-page-content">
      <div class="form-box-message layout-centered container ui-widget">   
        <div class="form-box-head ui-widget-header ui-corner-top">
        	<div class="form-box-pos">
        		<span>Message</span>
        	</div>
        </div>      
       	<div class="form-box-message-content ui-widget-content ui-corner-bottom">
				<div class="form-box-message-container center">
	           		<span id="textMessage"></span>
	            </div>
	            <div class="form-box-message-container form-box-message-margin-left">
	            	<button id="okButton" onclick="handleXMLRequest();" type='button' class='ui-button ui-corner-all ui-widget-button-position'>OK</button>
	            	<button id="cancelButton" onclick="message.closeMessageView();" type='button' class='ui-button ui-corner-all ui-widget-button-position'>cancel</button>
	            </div>
         </div>
      </div>
</div>