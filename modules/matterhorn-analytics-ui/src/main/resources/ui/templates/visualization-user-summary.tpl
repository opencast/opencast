<div>
  <div id="downloadCSVContainer" align="right" class="buttonContainer">
  	<input type="button" class="mouseover-pointer control-button ui-button ui-widget ui-state-default ui-corner-all" id="downloadCSVButton" value="Download as CSV File" role="button" aria-disabled="false">
  </div>
  <table id="summaryTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr>
        <th id="sortName" width="25%" class="ui-widget-header sortable"><div>Name<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<th id="sortSessions" width="10%" class="ui-widget-header sortable"><div># Sessions<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<th id="sortUniqueVideos" width="14%" class="ui-widget-header sortable"><div># Unique Videos<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<th id="sortTimeWatched" width="12%" class="ui-widget-header sortable"><div>Time Watched<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    	<th id="sortLastWatched" width="19%" class="ui-widget-header sortable"><div>Last Watched<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
    </tr>
    </thead>
    <tbody>
      <% if (data[j] != undefined && data[j].summaries != undefined && data[j].summaries.summary != undefined &&  data[j].summaries.summary.length > 0) { %>
      	<% for(var i = 0; i < data[j].summaries.summary.length; i++) { %>
    	  <tr valign="middle">
        	<td class="ui-state-active">
          		<%= data[j].summaries.summary[i].userId %>
        	</td>
        	<td class="ui-state-active">
          		<%= data[j].summaries.summary[i].sessionCount %>
        	</td>
        	<td class="ui-state-active">
          		<%= (data[j].summaries.summary[i].uniqueMediapackages) ? data[j].summaries.summary[i].uniqueMediapackages : ''  %>
        	</td>
        	<td class="ui-state-active">
          		<%= secondsToShortTime(data[j].summaries.summary[i].length).h + ":" + secondsToShortTime(data[j].summaries.summary[i].length).m + ":" + secondsToShortTime(data[j].summaries.summary[i].length).s %>
        	</td>
        	<td class="ui-state-active">
          		<%= data[j].summaries.summary[i].last.substring(0, 19).replace("T", " ") %>
        	</td>
      		</tr>
		<% } %>
      <% } %>
      <% if (data[j].summaries == undefined || data[j].summaries.summary == undefined || data[j].summaries.summary.length == 0) { %>
      	<tr>
        	<td colspan="5" align="center">
          	No User Summaries found
        	</td>
      	</tr>
      <% } %>
    </tbody>
  </table>
</div>