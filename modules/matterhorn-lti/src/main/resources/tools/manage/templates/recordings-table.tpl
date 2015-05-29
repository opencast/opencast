<div>
<table id="recordingsTable" class="ui-widget" cellspacing="0">
<thead>
<tr id="bulkHeader">
<th colspan="6">&nbsp;</th>
<th class="recordings-table-head ui-helper-hidden" id="bulkActionButton"><a href="javascript:ocRecordings.displayBulkAction()">Bulk Action</a></th>
</tr>
<tr>
<th id="sortTitle"  class="ui-widget-header sortable"><div>Title<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
<th id="sortPresenter" class="ui-widget-header sortable"><div>Presenter<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
<th id="sortDate"  class="ui-widget-header sortable"><div>Date & Time<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></div></th>
<th class="ui-widget-header">Venue</th>
<th  class="ui-widget-header">Status</th>
<th  class="ui-widget-header">Manage</th>
</tr>
</thead>
<tbody>
<% for(var i = 0; i < data[j].recordings.length; i++) {
    if( data[j].recordings[i].state !== 'Failed' ){
%>
    <tr>
        <td class="ui-state-active">
        <%= data[j].recordings[i].title %>
        </td>
        <td class="ui-state-active">
        <%= data[j].recordings[i].creators %>
        </td>
        <td class="ui-state-active">
        <%= data[j].recordings[i].start %>
        </td>
        <td class="ui-state-active">
        <%= data[j].recordings[i].captureAgent %>
        </td>
        <td class="status-column-cell ui-state-active">
        <div class="workflowActionButton ui-helper-clearfix">
        <a href="index.html#/inspect?id=<%= data[j].recordings[i].id %>" title="View Technical Details for this Recording">
        <span class="inspect-workflow-button" ></span>
        </a>
        </div>
        <div>
        <% if (data[j].recordings[i].error) { %>
            <div class="foldable">
                <div class="fold-header"><%= data[j].recordings[i].state %></div>
                <div class="fold-body">
                <%= data[j].recordings[i].error %>
                </div>
                </div>
                <% } else { %>
                    <%= data[j].recordings[i].state %>
                        <% if (data[j].recordings[i].operation) { %>
                            :&nbsp;<%= data[j].recordings[i].operation %>
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
                                        <% }
                                        }%>
                                        <% if (data[j].recordings.length == 0) { %>
                                            <tr>
                                                <td colspan="6">No Recordings found.</td>
                                            </tr>
                                        <% } %>
                                                </tbody>
                                                </table>
                                                <script type="text/javascript">
						var cainfo = "";
						$.ajax({
							    url: "/info/me.json",
							    dataType: "json",
							    async: false,
		      	     				    success: function(data) {
							    var props = data.org.properties;
				      	  		    if(props){
						                cainfo = props["lti.cainfo.json"] ? props["lti.cainfo.json"].trim() : "";
						             }
						      }
						});

                                                $.ajax(
                                                {
                                                url: cainfo,
                                                dataType: 'json',
                                                success: function (data)
                                                    {
                                                    $("#recordingsTable > tbody > tr > td:nth-child(4)").each(function(){
                                                         if ( data[$.trim( $(this).text() )] ){
                                                           $(this).text( data[$.trim( $(this).text() )] );
                                                         }
                                                     });
                                                    }
                                                });

                                                $(".greyed_out").css('opacity', '0.4');

                                                $("#recordingsTable > tbody > tr > td:nth-child(5)").children().each(function(){
                                                    var images = {
                                                    "Published" : "/ltitools/shared/img/icons/published.png",
                                                    "On Hold" : "/ltitools/shared/img/icons/on_hold.png",
                                                    "Capturing" : "/ltitools/shared/img/icons/capturing.png",
                                                    "Processing" : "/ltitools/shared/img/icons/process.png",
                                                    "Upcoming" : "/ltitools/shared/img/icons/upcoming.png"
                                                    };

                                                    if( images[$.trim( $(this).text() )] ){
                                                      $(this).html("<img src="+ images[$.trim( $(this).text() )] +"></img>" + $.trim($(this).text()));
                                                    }
                                                });
                                                </script>
                                                </div>
