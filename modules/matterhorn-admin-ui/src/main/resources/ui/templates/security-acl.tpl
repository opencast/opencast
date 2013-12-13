<div class="form-box">
  <div class="layout-centered ui-state-highlight ui-corner-all scheduler-info-container">
    <h3 style="position: relative; padding-left: 20px;">
      <span class="ui-icon ui-icon-info"></span> <b>Notice</b>
    </h3>
    <ul>
      <li id="noticeOffline" class="missing-fields-item">
		<span class="ui-icon ui-icon-carat-1-e"></span>Editing an ACL does not apply to currently active ACLs but only to future transitions.
      </li>
    </ul>
  </div>
</div>
<br />
<% _.each(this.data, function(acl) { %>
<h2><%= acl.name %></h2><img title="Delete ACL" id="<%= acl.id %>" class="deleteAcl" alt="delete" src="/admin/img/icons/delete.png">
<table id="aclTable" class="ui-widget" width="600px" cellspacing="0" border="1">
	<thead>
		<tr>
			<th width="370px" class="ui-widget-header">Role<span></span></th>
			<th width="100px" class="ui-widget-header">Read<span></span></th>
			<th width="100px" class="ui-widget-header">Write<span></span></th>
			<th width="30px" class="ui-widget-header"></th>
		</tr>
	</thead>
	<tbody>
		<% _.each(acl.roles, function(role) { %>
		<tr>
			<td><input type="text" class="role_search ui-autocomplete-input" value="<%= role.name %>" id="<%= role.name %>"></td>
			<td class="privilege_edit"><input type="checkbox" class="privilege_edit" name="priv_read" <% if (role.read) { %> checked="checked" <% } %>></td>
			<td class="privilege_edit"><input type="checkbox" class="privilege_edit" name="priv_write" <% if (role.write) { %> checked="checked" <% } %>></td>
			<td class="privilege_edit"><img title="Delete Role" alt="delete" src="/admin/img/icons/delete.png"></td>
		</tr>		
		<% }); %>
		<tr>
			<td><input type="text" class="role_search ui-autocomplete-input"></td>
			<td class="privilege_edit"><input type="checkbox" class="privilege_edit" name="priv_read" checked="checked" disabled="disabled"></td>
			<td class="privilege_edit"><input type="checkbox" class="privilege_edit" name="priv_write" disabled="disabled"></td>
			<td class="privilege_edit"><img title="Delete Role" alt="delete" src="/admin/img/icons/delete.png" style="display: none;"></td>
		</tr>
	</tbody>
</table>
<% }); %>