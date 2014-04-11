<div class="episode-schedule" id="<%= this.id %>">
    <span>
        <span class="currentText">Active</span>
        <input type="text" class="from" />
    </span>
    <span>
        <select class="acl" id="acl-<%= this.id %>">
            <%= this.acl %>
        </select>
        <span class="aclLabel"></span>
    </span>
    <span>
        <select class="workflows" id="workflowDef-<%= this.id %>">
            <%= this.workflows %>
        </select>
        <span class="workflowsLabel"></span>
    </span>
    <span class="actions">
        <div class="ui-icon ui-icon-pencil params"></div>
        <div class="ui-icon ui-icon-trash delete"></div>
        <button class="save"><div class="ui-icon ui-icon-check" title="Save this schedule"></div> Save</button>
        <button class="cancel"><div class="ui-icon ui-icon-close" title="Cancel"></div> Cancel</button>
    </span>
    <span class="fromSeries">From series</span>
    <span class="error"></span>
</div>
