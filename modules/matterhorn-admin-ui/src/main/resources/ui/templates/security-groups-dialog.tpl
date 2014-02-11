<p class="validateTips">Field 'Name' and 'Users' are required.</p>
<form>
	<fieldset>
		<ul class="oc-ui-form-list">
			<li>
				<label id="nameLabel" for="name" class="groups-label"><span id="i18n_name_label">Name</span><span class="scheduler-required-text"> *</span>:</label>
				<input type="text" maxlength="255" class="oc-ui-form-field" name="name" id="name">
			</li>
			<li>
				<label id="descriptionLabel" for="description" class="groups-label"><span id="i18n_description_label">Description</span>:</label>
				<input type="text" maxlength="255" id="description" name="description" class="oc-ui-form-field">
			</li>
			<li>
				<label id="rolesLabel" for="roles" class="groups-label"><span id="i18n_roles_label">Roles</span>:</label>
				<input type="text" maxlength="255" id="roles" name="roles" class="oc-ui-form-field">
			</li>
			<li>
			    <select id="rolesSelect" name="top5" size="5" multiple></select>
				<button id="removeRole" type="button">Remove Role</button>
			</li>
			<li>
				<label id="usersLabel" for="users" class="groups-label"><span id="i18n_users_label">Users</span><span class="scheduler-required-text"> *</span>:</label>
				<input type="text" maxlength="255" id="users" name="users" class="oc-ui-form-field">
			</li>
			<li>
			    <span><small><b>Note: Yellow users are external</b></small></span>
			    <select id="usersSelect" name="top5" size="5" multiple></select>
				<button id="addUser" type="button">Add User</button><button id="removeUser" type="button">Remove User</button>
			</li>
		</ul>
	</fieldset>
</form>