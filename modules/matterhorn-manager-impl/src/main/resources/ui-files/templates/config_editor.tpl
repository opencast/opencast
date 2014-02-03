<link rel="stylesheet" href="ui-files/lib/codemirror/lib/codemirror.css">
<link rel="stylesheet" href="ui-files/lib/codemirror/addon/hint/show-hint.css">

<script type="text/javascript" src="ui-files/js/config-editor/configeditor.js"></script>

<script src="ui-files/lib/codemirror/lib/codemirror.js"></script>
<script src="ui-files/lib/codemirror/addon/hint/show-hint.js"></script>
<script src="ui-files/lib/codemirror/addon/hint/xml-hint.js"></script>
<script src="ui-files/lib/codemirror/mode/xml/xml.js"></script>

<script src="ui-files/js/utils/requestHandler.js"></script>
<script src="ui-files/js/utils/messageRenderer.js"></script>
<script src="ui-files/js/utils/restartSystemScript.js"></script>


<div class="layout-page-content">
    
    <!-- file tree -->
	<div class="config-file-tree">
    	<div id="configsTree"></div>
    </div>
	
	<div id="dialog-reload">
		<p id="dialog-message"></p>
	</div>

	<!-- config container -->
	<div class="config-box layout-centered ui-widget config-container" id="config-container">
	  	<!-- Config Name -->
      	<div class="config-box-head ui-widget-header ui-corner-top" id="config-title" style="display: none;">
	    	<span id="i18n_config_title"></span>
	  	</div>
	  	<div class="config-box-content ui-widget-content ui-corner-bottom" id="config-body" style="display: none;" />
	</div>
	
	<script type="text/javascript">
	    $("#config-controls").show();
	 	$("#config-controls").attr("style", "visibility:visible");
	</script>
	
	<script type="text/javascript">
	    loadConfig("/config.properties");
	 	$('a:contains("config.properties")').focus();
	</script>

</div>

<div id="messageRestart"/>
<div id="message"/>	