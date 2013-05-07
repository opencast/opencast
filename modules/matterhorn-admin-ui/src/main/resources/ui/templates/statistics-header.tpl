<div id="stage" class="ui-widget">
  <!-- div id="controlsTopStatistic" class="ui-widget-header ui-corner-top ui-helper-clearfix" -->
  <div id="controlsTopStatistic" class="ui-helper-clearfix">
    <div class="state-filter-container">
      <input type="radio" name="stateSelect" value="servers" id="stats-servers" /><label for="stats-servers">Servers</label>
      <input type="radio" name="stateSelect" value="services" id="stats-services" /><label for="stats-services">Services</label>
    </div>
  </div>
  <div id="tableContainer" class="ui-widget ui-helper-clearfix"></div>

  <div id="controlsFoot" class="ui-helper-clearfix">

    <div id="refreshControlsContainer" class="ui-widget ui-state-hover ui-corner-all">
      <input type="checkbox" id="refreshEnabled" /><label for="refreshEnabled"></label>
      <span class="refresh-text">Update table every&#160;</span>
      <select id="refreshInterval">
        <option value="5">5</option>
        <option value="7">7</option>
        <option value="10">10</option>
      </select>
      <span class="refresh-text">&#160;seconds.</span>
    </div>

  </div>

</div>