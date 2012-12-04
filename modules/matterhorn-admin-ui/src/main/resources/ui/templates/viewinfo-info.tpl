<div>
  <% if ( data[j].workflow.info) { %>
  <div class="form-box-content ui-widget-content ui-corner-all">
    <table>
      <tr>
        <td class="td-key">Title:</td>
        <td class="td-value"><%= data[j].workflow.info.title %></td>
      </tr>
      <tr>
        <td class="td-key">Presenter:</td>
        <td class="td-value"><%= data[j].workflow.info.creators %></td>
      </tr>
      <% if ( data[j].workflow.info.seriestitle) { %>
      <tr>
        <td class="td-key">Series:</td>
        <td class="td-value"><%= data[j].workflow.info.seriestitle %></td>
      </tr>
      <% } %>
      <tr>
        <td class="td-key">Recording Date:</td>
        <td class="td-value"><%= ocUtils.fromUTCDateStringToFormattedTime(data[j].workflow.info.start) %></td>
      </tr>
    </table>
  </div>
  <% } %>
  <% if ( data[j].workflow.template != "scheduling") { %>
  <div class="form-box layout-centered ui-widget layout-centered">
    <div class="form-box-head ui-widget-header ui-corner-top">File Upload</div>
    <div class="form-box-content ui-widget-content ui-corner-bottom">
      <table id="uploadedFileContainer">
        <tr>
          <td class="td-key">File Uploaded:</td>
          <td class="td-value"></td>
        </tr>
      </table>
    </div>
  </div>
  <% } %>
  <% if ( data[j].workflow.info.episodeDC) { %>
  <div class="form-box layout-centered  ui-widget layout-centered">
    <div class="form-box-head ui-widget-header ui-corner-top unfoldable-header">
      <div class="ui-icon fold-icon ui-icon-triangle-1-e"></div>
      <div class="fold_icon_text">Additional Description</div>
      <div class="clear"></div>
    </div>
    <div id="episodeContainer" class="form-box-content ui-widget-content ui-corner-bottom unfoldable-content">
    </div>
  </div>
  <% } %>
  <% if ( data[j].workflow.info.seriesDC) { %>
  <div class="form-box layout-centered  ui-widget layout-centered">
    <div class="form-box-head ui-widget-header ui-corner-top unfoldable-header">
      <div class="ui-icon ui-icon-triangle-1-e fold-icon"></div>
      <div class="fold_icon_text">Series Description</div>
      <div class="clear"></div>
    </div>
    <div id="seriesContainer" class="form-box-content ui-widget-content ui-corner-bottom unfoldable-content">
    </div>
  </div>
  <% } %>
  <% if ( data[j].workflow.config['schedule.location']) { %>
  <div class="form-box layout-centered ui-widget layout-centered">
    <div class="form-box-head ui-widget-header ui-corner-top">Capture</div>
    <div class="form-box-content ui-widget-content ui-corner-bottom">
      <table>
        <tr>
          <td class="td-key">Start Time:</td>
          <td class="td-value"><%= ocUtils.makeLocaleDateString(ocUtils.fromUTCDateString(data[j].workflow.mediapackage.start)) %></td>
        </tr>
        <tr>
          <td class="td-key">Duration:</td>
          <td class="td-value"><%= data[j].workflow.mediapackage.duration %></td>
        </tr>
        <tr>
          <td class="td-key">Capture Agent:</td>
          <td class="td-value"><%= data[j].workflow.config['schedule.location'] %></td>
        </tr>
      </table>
    </div>
  </div>
  <% } %>
  <% if ( data[j].workflow.config.distribution) { %>
  <div class="form-box layout-centered ui-widget layout-centered">
    <div class="form-box-head ui-widget-header ui-corner-top">Distribution</div>
    <div class="form-box-content ui-widget-content ui-corner-bottom">
      <table>
        <tr>
          <td class="td-key">Distribution channel(s):</td>
          <td class="td-value"><%= data[j].workflow.config.distribution %></td>
        </tr>
        <tr>
          <td class="td-key">License:</td>
          <td id="licenseField" class="td-value"><%= (data[j].workflow.mediapackage.license) ? data[j].workflow.mediapackage.license : '' %></td>
        </tr>
      </table>
    </div>
  </div>
  <div class="form-box layout-centered ui-widget layout-centered">
    <div class="form-box-head ui-widget-header ui-corner-top">Workflow</div>
    <div class="form-box-content ui-widget-content ui-corner-bottom">
      <table>
        <tr>
          <td class="td-key">Processing Instructions:</td>
          <td class="td-value"><%= data[j].workflow.title %></td>
        </tr>
        <tr>
          <td class="td-key">Holds:</td>
          <td id="licenseField" class="td-value">
            <%= (data[j].workflow.config.trimHold) ? 'Review / Trim before encoding (with option to edit info)' : '' %>
          </td>
        </tr>
      </table>
    </div>
  </div>
  <% } %>
</div>