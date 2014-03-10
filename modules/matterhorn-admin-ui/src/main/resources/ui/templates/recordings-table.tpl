<div>
  <table id="recordingsTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr id="bulkHeader">
        <th colspan="6">&#160;</th>
        <th class="recordings-table-head ui-helper-hidden" id="bulkActionButton"><a href="javascript:ocRecordings.displayBulkAction()">Bulk Action</a></th>
      </tr>
      <tr>
        <th class="ui-state-default ui-helper-hidden bulkSelect"><input type="checkbox" id="selectAllRecordings" onclick="ocRecordings.selectAll(this.checked);" /></th>
        <th id="sortTitle" width="30%" class="ui-widget-header sortable"><div>Title<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<th id="sortPresenter" width="5%" class="ui-widget-header sortable"><div>Presenter<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<th id="sortSeries" width="15%" class="ui-widget-header sortable"><div>Course/Series<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
        <th id="sortAgent" width="10%" class="ui-widget-header sortable"><div>Capture Agent</div></th>
    	<th id="sortDate" width="10%" class="ui-widget-header sortable"><div>Recording Date &amp; Time<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<% if (ocRecordings.Configuration.state == 'failed') { %>
			<th id="sortFailDate" width="10%" class="ui-widget-header"><div>Failure Date &amp; Time</div></th>
		<% } %>
		<% if (ocRecordings.Configuration.state == 'finished') { %>
			<th id="sortFailDate" width="10%" class="ui-widget-header"><div>Finished Date &amp; Time</div></th>
		<% } %>
		<% if (ocRecordings.Configuration.state == 'hold') { %>
			<th id="sortFailDate" width="10%" class="ui-widget-header"><div>Hold Date &amp; Time</div></th>
		<% } %>
		<% if (ocRecordings.Configuration.state == 'processing') { %>
			<th id="sortFailDate" width="10%" class="ui-widget-header"><div>Processing Date &amp; Time</div></th>
		<% } %>
    	<th width="10%" class="ui-widget-header">Status</th>
    	<th width="10%" class="ui-widget-header">Action</th>
      </tr>
    </thead>
    <tbody>
      <% for(var i = 0; i < data[j].recordings.length; i++) { %>
      <tr valign="middle">
        <td  class="ui-state-active ui-helper-hidden bulkSelect">
          <input type="checkbox" value="<%= data[j].recordings[i].id %>" class="selectRecording" />
        </td>
        <td class="ui-state-active">
          <%= data[j].recordings[i].title %>
        </td>
        <td class="ui-state-active">
          <%= data[j].recordings[i].creators %>
        </td>
        <td class="ui-state-active">
          <%= (data[j].recordings[i].seriesTitle) ? data[j].recordings[i].seriesTitle : ''  %>
        </td>
        <td class="ui-state-active">
          <%= data[j].recordings[i].captureAgent %>
        </td>
        <td class="ui-state-active">
          <%= data[j].recordings[i].start %>
        </td>
        <% if (ocRecordings.Configuration.state == 'failed' || ocRecordings.Configuration.state == 'hold' || ocRecordings.Configuration.state == 'finished' || ocRecordings.Configuration.state == 'processing') { %>
  		  <td class="ui-state-active">
            <%= data[j].recordings[i].end %>
          </td>
		<% } %>
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