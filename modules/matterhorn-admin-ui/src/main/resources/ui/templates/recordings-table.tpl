<div>
  <table id="recordingsTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr id="bulkHeader">
        <th colspan="6">&#160;</th>
        <th class="recordings-table-head ui-helper-hidden" id="bulkActionButton"><a href="javascript:ocRecordings.displayBulkAction()">Bulk Action</a></th>
      </tr>
      <tr>
        <th class="ui-state-default ui-helper-hidden bulkSelect"><input type="checkbox" id="selectAllRecordings" onclick="ocRecordings.selectAll(this.checked);" /></th>
        <th id="sortTitle" width="25%" class="ui-widget-header sortable"><div>Title<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    <th id="sortPresenter" width="15%" class="ui-widget-header sortable"><div>Presenter<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    <th id="sortSeries" width="15%" class="ui-widget-header sortable"><div>Course/Series<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    <% if (ocRecordings.Configuration.state == 'upcoming' || ocRecordings.Configuration.state == 'capturing') { %>
    <th class="ui-widget-header" style="white-space:nowrap;padding-left:5px;padding-right:5px;">Capture Agent</th>
    <% } %>
    <th id="sortDate" width="18%" class="ui-widget-header sortable"><div>Recording Date&amp;Time<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    <th width="17%" class="ui-widget-header">Status</th>
    <th width="10%" class="ui-widget-header">Action</th>
    </tr>
    </thead>
    <tbody>
      <% for(var i = 0; i < data[j].recordings.length; i++) { %>
      <tr valign="middle">
        <td  class="ui-state-active ui-helper-hidden bulkSelect">
          <input type="checkbox" value="<%= data[j].recordings[i].id %>" class="selectRecording" />
        </td>
        <td class="ui-state-active" title="Processing Instruction: <%= data[j].recordings[i].workflowTitle %>&#013;Processing Start Time: <%= data[j].recordings[i].workflowStart %>">
          <%= data[j].recordings[i].title %>
        </td>
        <td class="ui-state-active">
          <%= data[j].recordings[i].creators %>
        </td>
        <td class="ui-state-active">
          <%= (data[j].recordings[i].seriesTitle) ? data[j].recordings[i].seriesTitle : ''  %>
        </td>
        <% if (ocRecordings.Configuration.state == 'upcoming' || ocRecordings.Configuration.state == 'capturing') { %>
        <td class="ui-state-active" style="white-space:nowrap;">
          <%= data[j].recordings[i].captureAgent %>
        </td>
        <% } %>
        <td class="ui-state-active">
          <%= data[j].recordings[i].start %>
        </td>
        <td class="status-column-cell ui-state-active">

          <div class="workflowActionButton ui-helper-clearfix" style="float:left;">
            <a href="index.html#/inspect?id=<%= data[j].recordings[i].id %>" title="View Technical Details for this Recording">
              <span class="ui-icon ui-icon-gear inspect-workflow-button" style="float:left;"></span>
            </a>
          </div>
          <div style="padding-top:3px;">
            <% if (data[j].recordings[i].error) { %>
            <div class="foldable">
              <div class="fold-header" style="color:red;font-weight:bold;"><%= data[j].recordings[i].state %></div>
              <div class="fold-body">
                <%= data[j].recordings[i].error %>
              </div>
            </div>
            <% } else { %>
            <%= data[j].recordings[i].state %>
            <% if (data[j].recordings[i].operation) { %>
            :&#160;<%= data[j].recordings[i].operation %>
            <% } %>
            <% } %>
          </div>
        </td>
        <td class="ui-state-active">
          <%= ocRecordings.makeActions(data[j].recordings[i], data[j].recordings[i].actions) %>
          <% if (data[j].recordings[i].holdAction) { %>
          <br /><a href="javascript:ocRecordings.displayHoldUI(<%= data[j].recordings[i].id %>);" title="<%= data[j].recordings[i].holdAction.title %>"><%= data[j].recordings[i].holdAction.title %></a>
          <% } %>
        </td>
      </tr>
      <% } %>
      <% if (data[j].recordings.length == 0) { %>
      <tr>
        <td colspan="6" align="center">
          No Recordings found
        </td>
      </tr>
      <% } %>
    </tbody>
  </table>
</div>