<div id="stage" class="ui-widget">
  <div id="controlsTopMetrics" class="ui-helper-clearfix">
    <div class="state-filter-container">
      <input type="radio" name="stateSelect" value="matterhorn" id="metrics-matterhorn" /><label for="metrics-matterhorn">Matterhorn</label>
      <input type="radio" name="stateSelect" value="jvm" id="metrics-jvm" /><label for="metrics-jvm">Java</label>
	  <input type="radio" name="stateSelect" value="os" id="metrics-os" /><label for="metrics-os">Operating System</label>
    </div>
  </div>
  <div class="portlet ui-widget-content ui-helper-clearfix ui-corner-all" id="portlet-template">
	<div class="portlet-header ui-widget-header ui-corner-all"><span class='ui-icon ui-icon-minusthick'></span><span class="title">&nbsp;</span></div>
	<div class="portlet-content"></div>
  </div>
  <div id="matterhorn-tableContainer" class="ui-widget ui-helper-clearfix jmx-tableContainer"></div>
  <div id="jvm-tableContainer" class="ui-widget ui-helper-clearfix jmx-tableContainer"></div>
  <div id="os-tableContainer" class="ui-widget ui-helper-clearfix jmx-tableContainer"></div>
</div>