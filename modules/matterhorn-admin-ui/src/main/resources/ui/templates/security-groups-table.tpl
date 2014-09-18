<table id="groupsTable" class="ui-widget" cellspacing="0" width="100%">
  <thead>
  <tr>
    <th width="30%" class="ui-widget-header">Name</th>
    <th width="30%" class="ui-widget-header">Description</th>
    <th width="30%" class="ui-widget-header sortable">Group Role</th>
    <th class="ui-widget-header">Delete</th>
    <th class="ui-widget-header">Edit</th>
  </tr>
  </thead>
  <tbody>
  	<% var self = this; %>
    <% _.each(ocUtils.ensureArray(this.groups.group), function(group) { %>
      <tr valign="top">
        <td class="ui-state-active"><%! group.name %></td>
        <td class="ui-state-active"><%! group.description %></td>
        <td class="ui-state-active"><%! group.role %></td>
        <td class="ui-state-active" style="text-align: center">
	      <a href="/groups/<%= group.id %>">Delete</a>
        </td>
        <td class="ui-state-active">
          <div class="toggle-groups ui-icon ui-icon-triangle-1-e"></div>
        </td>
      </tr>
      <tr colspan="5" style="display:none">
        <td class="ui-state-active security-group-header" colspan="1"><h2>Group definition editor</h2></td>
        <td class="ui-state-active security-group-content" colspan="4">
			<div class="form-box layout-centered ui-widget">
				<div class="form-box-content ui-widget-content ui-corner-all">
					<form>
				    	<p class="validateTips">Field 'Name' and 'Users' are required.</p>
						<ul class="oc-ui-form-list">
							<li>
								<label id="idLabel" for="id" class="groups-label"><span id="i18n_id_label">Id</span>:</label>
								<input type="text" disabled="disabled" value="<%! group.id %>" maxlength="255" class="oc-ui-form-field" name="id" id="id">
							</li>
							<li>
								<label id="nameLabel" for="name" class="groups-label"><span id="i18n_name_label">Name</span><span class="scheduler-required-text"> *</span>:</label>
								<input type="text" maxlength="255" value="<%! group.name %>" class="oc-ui-form-field" name="name" id="name">
							</li>
							<li>
								<label id="descriptionLabel" for="description" class="groups-label"><span id="i18n_description_label">Description</span>:</label>
								<input type="text" maxlength="255" value="<%! group.description %>" id="description" name="description" class="oc-ui-form-field">
							</li>
							<li>
								<label id="rolesLabel" for="roles" class="groups-label"><span id="i18n_roles_label">Roles</span>:</label>
								<input type="text" maxlength="255" id="roles" name="roles" class="oc-ui-form-field">
							</li>
							<li>
							    <select id="rolesSelect" name="top5" size="5" multiple>
							      <% _.each(ocUtils.ensureArray(group.roles.role), function(role) { %>
							      <option value=<%! role.name %>><%! role.name %></option>
							      <% }); %>
							    </select>
								<button id="removeRole" type="button">Remove Role</button>
							</li>
							<li>
								<label id="usersLabel" for="users" class="groups-label"><span id="i18n_users_label">Users</span><span class="scheduler-required-text"> *</span>:</label>
								<input type="text" maxlength="255" id="users" name="users" class="oc-ui-form-field">
							</li>
							<li>
							    <span><small><b>Note: Yellow users are external</b></small></span>
							    <select id="usersSelect" name="top5" size="5" multiple>
							      <% _.each(ocUtils.ensureArray(group.members.member), function(member) { %>
							      	<% if(_.contains(self.users, member)) { %>
							      		<option value=<%! member %>><%! member %></option>
	        	  					<% } else { %>
							      		<option style="background-color: Yellow;" value=<%! member %>><%! member %></option>
	        	  					<% } %>
							      <% }); %>
							    </select>
								<button id="addUser" type="button">Add User</button><button id="removeUser" type="button">Remove User</button>
							</li>
						</ul>
						<br />
						<button type="submit">Update Group</button>
					</form>
				</div>
			</div>
        </td>
      </tr>
    <% }); %>
    <% if (this.groups.group.length == 0) { %>
      <tr>
        <td colspan="5" align="center">No Groups found</td>
      </tr>
    <% } %>
  </tbody>
</table>
