<div class="series-schedule current" id="<%! this.id %>">
    <span>
        Active ACL
    </span>
    <span>
        <input type="checkbox" class="override" /> Override
    </span>
    <span>
        <select class="acl" id="acl-<%! this.id %>">
            <%! this.acl %>
        </select>
        <span class="aclLabel"></span>
    </span>
    <span>
    </span>
    <span class="actions">
        <button class="save"><div class="ui-icon ui-icon-check" title="Save this schedule"></div> Apply</button>
        <button class="cancel"><div class="ui-icon ui-icon-close" title="Cancel"></div> Cancel</button>
        <img class="loading-spinner" src="/admin/img/misc/loading_small.gif" />
    </span>
    <span class="error"></span>
</div>
