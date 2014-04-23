ocSecurity = new (function() {
	
  var self = this;

  var ROLES_LIST_URL = '/roles/roles.json';
  var GROUPS_LIST_URL = '/groups/groups.json';
  var USERS_LIST_URL = '/users/users.json';
  var ACLS_LIST_URL = '/acl-manager/acl/acls.json';
  
  // this var holds the actual refresh function and gets initialized lazily
  var _theRefreshFunc = function () {return $.Deferred()};
  
  var refreshing = false; // indicates if JSONP requesting recording data is in progress
  
  var A = ocUtils.ensureArray;
  
  var nameSort = function(a, b) {
	  return a.innerHTML.toLowerCase() > b.innerHTML.toLowerCase() ? 1 : -1;
  };
  
  /** 
   * Executed when directly when script is loaded: parses url parameters and
   * returns the configuration object.
   */
  this.Configuration = new (function() {

    // default configuartion
    this.state = 'groups';
    this.refresh = 10000;

    // parse url parameters
    try {
      var p = document.location.href.split('?', 2)[1] || false;
      if (p !== false) {
        p = p.split('&');
        for (i in p) {
          var param = p[i].split('=');
          if (this[param[0]] !== undefined) {
            this[param[0]] = unescape(param[1]);
          }
        }
      }
    } catch (e) {
      alert('Unable to parse url parameters:\n' + e.toString());
    }

    return this;
  })();
  
  /** 
   *  @param params -- parameter object for the groups rest endpoint
   *  @return jqXHR containing raw json
   */
  this.getGroups = function(params) {
    return $.getJSON(GROUPS_LIST_URL, params);
  }
  
  /** 
   *  @param params -- parameter object for the roles rest endpoint
   *  @return jqXHR containing raw json
   */
  this.getRoles = function(params) {
	return $.getJSON(ROLES_LIST_URL, params);
  }
  
  /** 
   *  @param params -- parameter object for the users rest endpoint
   *  @return jqXHR containing raw json
   */
  this.getUsers = function(params) {
	  return $.getJSON(USERS_LIST_URL, params);
  }
  
  /** 
   *  @param params -- parameter object for the acl providers rest endpoint
   *  @return jqXHR containing raw json
   */
  this.getAcls = function(params) {
	  return $.getJSON(ACLS_LIST_URL, params);
  }

  /** 
   *  Get list of roles. If callback is provided call will be asynchronous, otherwise
   *  this function will block.
   *
   *  @param callback function to call when list of roles was successfully retrieved
   */
  this.loadRoles = function(callback) {
    out = false;
    $.ajax({
      url : ROLES_LIST_URL,
      type : 'GET',
      dataType : 'json',
      async : false,
      error : function() {
        if (ocUtils !== undefined) {
          ocUtils.log("Could not retrieve roles list from " + ROLES_LIST_URL);
        }
      },
      success : function(data) {
        if (callback) {
          callback(data);
        } else {
          out = data;
        }
      }
    });
    return out;
  };
  
  /** 
   *  Update the privileges of an array of Roles. Each Roles privileges are set
   *  according to the value of checkboxes with id=${role name}CanRead / ${role name}CanRead.
   */
  this.updatePrivileges = function(privileges) {
    $(privileges).each(function(index, role) {
      role.canRead = self.getCheckboxValue(role.name + 'CanRead');
      role.canWrite = self.getCheckboxValue(role.name + 'CanWrite');
    });
  }

  this.getCheckboxValue = function(name) {
    $cb = $('#' + name);
    if ($cb.length != 0) {
      return $cb.is(':checked');
    } else {
      return false;
    }
  }
  
  this.renderGroups = function(groupsData, usersData) {
	  refreshing = false;
	  $('#addGroup').show();
	  var userList = $.map(A(usersData.users.user), function(user) {
		  return user.username;
	  });
	  self.renderTable(groupsData.groups, userList);
  }
  
  this.parseAclRoles = function(acl) {
	  var roles = new Array();
	  if(acl.ace == undefined) return roles;
	  $.each(A(acl.ace), function(i, ace) {
		  var even = _.find(roles, function(role){
			  return role.name == ace.role;
		  });
		  if(even == null) {
			  var newRole = {};
			  newRole.name = ace.role;
			  newRole[ace.action] = ace.allow;
			  roles.push(newRole);
		  } else {
			  even[ace.action] = ace.allow;
		  }
	  });
	  return roles;
  }
  
  this.renderAcl = function(aclData, roleData) {
	  refreshing = false;
      $('#addAcl').show();
      
      var data = new Array();

      $.each(A(aclData), function(i, aclEntry) {
    	 data.push({
    		 id: aclEntry.id,
    		 name: aclEntry.name,
    		 roles: self.parseAclRoles(aclEntry.acl)
    	 });
      });
      
      $('#aclContainer').jqotesubtpl('templates/security-acl.tpl', {data: data});
      $('#aclContainer').show();
      
      var privilegeRow = '<tr>';
      privilegeRow += '<td><input type="text" class="role_search ui-autocomplete-input"></td>';
      privilegeRow += '<td class="privilege_edit"><input type="checkbox" class="privilege_edit" name="priv_read" checked="checked" disabled="disabled"></td>';
      privilegeRow += '<td class="privilege_edit"><input type="checkbox" class="privilege_edit" name="priv_write" disabled="disabled"></td>';
      privilegeRow += '<td class="privilege_edit"><span class="ui-icon ui-icon-trash" title="Delete Role" alt="delete"></span></td>';
      privilegeRow += '</tr>';
      
	  var roleList = $.map(A(roleData.roles.role), function(role) {
          return role.name;
      });
	  
	  var createACLDocument = function(aclId) {
		  var out = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><acl xmlns="http://org.opencastproject.security">';
		  var table = $('div#aclContainer span#' + aclId).next();
		  table.find('input.role_search').each(function () {
		    var $field = $(this);
		    var value = $.trim($field.attr('id'));
        var canWrite = ($field.parents('tr').find('input[name|="priv_write"]:checked').length > 0 ? "true" : "false");
        var canRead = ($field.parents('tr').find('input[name|="priv_read"]:checked').length > 0 ? "true" : "false"); 
		    //check whether there is a value and entered value is a valid role
		    if (value != "" && _.contains(roleList, value)) {
		        out += '<ace>';
		        out += '<role>' + value + '</role>';
		        out += '<action>read</action>';
		        out += '<allow>' + canRead + '</allow>';
		        out += '</ace>';
		        out += '<ace>';
		        out += '<role>' + value + '</role>';
		        out += '<action>write</action>';
		        out += '<allow>' + canWrite + '</allow>';
		        out += '</ace>';
		    }
		  });
		  out += '</acl>';
		  return out;
	  }
	  
	  var removeRole = function() {
		var aclId = $(this).parents('table').prev().attr('id');
		var name = $(this).parents('table').prev().prev().text();
	    $(this).parent().parent().remove();
	    self.updateAcl(aclId, name, createACLDocument(aclId), function() {});
	  }
	  
	  var updateCheckbox = function() {
		  var aclId = $(this).parents('table').prev().attr('id');
		  var name = $(this).parents('table').prev().prev().text();
		  self.updateAcl(aclId, name, createACLDocument(aclId), function() {});
	  }
	  
	  var sourceFunction = function(request, response) {
	    var matcher = new RegExp($.ui.autocomplete.escapeRegex(request.term), "i");
	    var matched = $.grep(roleList, function(value) {
	      return matcher.test(value);
	    });
	    if($(matched).size() != 0) {
	      response(matched);
	    } else {
	      if(this.element.parent().find('p').size() == 0) {
	        this.element.parent().prepend('<p class="no_valid_rule" style="color: red;">Not a valid role</p>');
	      }
	      response(['No Match']);
	    }
	  };
	  
	  var closeFunction = function(event, ui) {
		$(this).removeClass("ui-corner-top").addClass("ui-corner-all");
	    if($.inArray(roleList, $(this).val()) == -1 && $(this).parent().find('p').size() == 0 && event.originalEvent.type != "menuselected") {
	      $(this).parent().prepend('<p class="no_valid_rule" style="color: red;">Not a valid role</p>');
	    }
	  }
	  
	  var append = function(event, ui) {
	    if(ui.item.value == 'No Match') {
	      return false;
	    }
	    if($(this).attr('id') == ('' || undefined)) {
	      var row = $(privilegeRow);
	      row.find('span').click(removeRole);
	      row.find('input.privilege_edit[type|="checkbox"]').click(updateCheckbox);
	      row.find('input').autocomplete({
	          source: sourceFunction,
	          minLength: 2,
	          select: append,
	          close: closeFunction,
	          open: function() {
	              $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
	          }
	      });
	      $(this).parents("#aclTable").append(row);
	      $(this).parents('tr').find('input:disabled').removeAttr('disabled');
	      $(this).parents('tr').find('span').show();
	    }
	    $(this).attr('id', ui.item.value);
	    $(this).parent().find('p').remove();
		var aclId = $(this).parents('table').prev().attr('id');
		var name = $(this).parents('table').prev().prev().text();
		self.updateAcl(aclId, name, createACLDocument(aclId), function() {});
	  };
	  
      $("table#aclTable input").autocomplete({
          source: sourceFunction,
          minLength: 2,
          select: append,
          close: closeFunction,
          open: function() {
              $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
          }
      });
      
      $('table#aclTable span').click(removeRole);
      $('table#aclTable input.privilege_edit[type|="checkbox"]').click(updateCheckbox);
      $('#aclContainer span.deleteAcl').click(function() {
    	  self.removeAcl($(this).attr('id'));
      });
  }
  
  this.initAclDialog = function() {
	  $('#acl-form').jqotesubtpl('templates/security-acl-dialog.tpl', {});
	  
	  var name = $("#acl-form #name");
	  var allFields = $([]).add(name);
	  var tips = $("#acl-form p.validateTips");
	  
      $("#acl-form").dialog({
          autoOpen: false,
          height: 250,
          width: 500,
          modal: false,
          buttons: {
              "Create an ACL": function() {
                  var bValid = true;
                  allFields.removeClass("ui-state-error");
                  
                  bValid = bValid && name.val().length > 0;

                  if(bValid) {
                	  self.createAcl(name.val());
                	  $(this).dialog("close");
                  } else {
                	  tips.addClass("ui-state-highlight").show();
                  }
              },
              Cancel: function() {
            	  tips.hide();
                  $(this).dialog("close");
              }
          },
          close: function() {
        	  tips.hide();
              allFields.val("").removeClass("ui-state-error");
          }
      });
      
	  $('div#addAcl img.ui-icon-circle-plus').click(function(event) {
		  $("#acl-form").dialog("open");
	  });
  }
  
  this.initGroupDialog = function() {
	  $('#groups-form').jqotesubtpl('templates/security-groups-dialog.tpl', {});
	  
      var name = $("#groups-form #name");
      var description = $("#groups-form #description");
      var allFields = $([]).add(name);
      var tips = $("#groups-form p.validateTips");
      var roles = $('#groups-form #rolesSelect');
      var users = $('#groups-form #usersSelect');
      var addUser = $("#groups-form button#addUser");
      var usersInput = $("#groups-form input#users");
      
      self.getRoles({}).success(function (data) {
    	  var data = $.map(A(data.roles.role), function(role) {
              return role.name;
          });
    	  
	      $("#groups-form input#roles").autocomplete({
	          source: data,
	          minLength: 2,
	          select: function(event, ui) {
	        	  roles.find("option[value='" + ui.item.label + "']").remove();
	        	  roles.append('<option value=' + ui.item.label + '>' + ui.item.label + '</option>');
	        	  roles.find('option').sort(nameSort).appendTo(roles);
	        	  $(this).val('');
	        	  return false;
	          },
	          open: function() {
	              $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
	          },
	          close: function() {
	              $(this).removeClass("ui-corner-top").addClass("ui-corner-all");
	          }
	      });
	      
	      $("#groups-form button#removeRole").button().click(function(event) {
	    	  roles.find(':selected').each(function(i, option) {
	    		  roles.find("option[value='" + option.label + "']").remove();
	    	  });
	      });
      });
      
      self.getUsers({limit:999999}).success(function (data) {
		  var data = $.map(A(data.users.user), function(user) {
			  return user.username;
		  });
		  
          addUser.button({ disabled: true }).click(function(event) {
        	  var userValue = $.trim(usersInput.val());
        	  if(userValue == '') return;
        	  
        	  users.find("option[value='" + userValue + "']").remove();
        	  if(_.contains(data, userValue)) {
        		  users.append('<option value=' + userValue + '>' + userValue + '</option>');
        	  } else {
        		  users.append('<option style="background-color: Yellow;" value=' + userValue + '>' + userValue + '</option>');
        	  }
        	  users.find('option').sort(nameSort).appendTo(users);
			  usersInput.val('');
			  $(this).button("option", "disabled", true);
          });
		  
          usersInput.autocomplete({
			  source: data,
			  minLength: 2,
			  select: function(event, ui) {
				  users.find("option[value='" + ui.item.label + "']").remove();
				  users.append('<option value=' + ui.item.label + '>' + ui.item.label + '</option>');
				  users.find('option').sort(nameSort).appendTo(users);
				  $(this).val('');
				  $(this).keyup();
				  return false;
			  },
			  open: function() {
				  $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
			  },
			  close: function() {
				  $(this).removeClass("ui-corner-top").addClass("ui-corner-all");
			  }
		  }).keyup(function() {
			  if($.trim(usersInput.val()) == '') {
				  addUser.button("option", "disabled", true);
			  } else {
				  addUser.button("option", "disabled", false);
			  }
		  });
		  
		  $("#groups-form button#removeUser").button().click(function(event) {
			  users.find(':selected').each(function(i, option) {
				  users.find("option[value='" + option.label + "']").remove();
			  });
		  });	  
      });
      
      $("#groups-form").dialog({
          autoOpen: false,
          height: 600,
          width: 500,
          modal: false,
          buttons: {
              "Create a group": function() {
                  var bValid = true;
                  allFields.removeClass("ui-state-error");
                  
                  bValid = bValid && name.val().length > 0;
                  bValid = bValid && users.find('option').length > 0;

                  if(bValid) {
                	  self.createGroup(name.val(), description.val(), self.getOptions(roles), self.getOptions(users));
                	  $(this).dialog("close");
                  } else {
                	  tips.addClass("ui-state-highlight").show();
                  }
              },
              Cancel: function() {
            	  tips.hide();
                  $(this).dialog("close");
              }
          },
          close: function() {
        	  tips.hide();
              $("#groups-form input").val('').removeClass("ui-state-error");
              $("#groups-form select option").remove();
          }
      });
      
	  $('div#addGroup img.ui-icon-circle-plus').click(function(event) {
		  $("#groups-form").dialog("open");
	  });
  }
  
  this.getOptions = function(select) {
	  var values = [];
	  select.find('option').each(function(i, option){
		values.push($(option).val());
	  });
	  return values.join();
  }
  
  this.removeAcl = function(aclId) {
      $.ajax({
    	  type: 'DELETE',
    	  url: "/acl-manager/acl/" + aclId,
    	  success: function() {
    		  self.refresh();
    	  }
      });
  }
  
  this.createAcl = function(name) {
	  var emptyAcl = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><acl xmlns="http://org.opencastproject.security"></acl>';
	  $.ajax({
		  type: 'POST',
		  url: "/acl-manager/acl/",
		  data: {
			  name: name,
			  acl: emptyAcl
		  },
		  success: function() {
			  self.refresh();
		  }
	  });
  }
  
  this.updateAcl = function(aclId, name, aclXml, callback) {
      $.ajax({
    	  type: 'PUT',
    	  url: "/acl-manager/acl/" + aclId,
    	  data: {
    		  name: name,
    		  acl: aclXml
    	  },
    	  success: function() {
    		  if (callback) {
    			  callback();
    		  } else {
    			  self.refresh();
    		  }
    	  }
      });
  }
  
  this.createGroup = function(name, description, roles, users) {
      $.ajax({
    	  type: 'POST',
    	  url: "/groups",
    	  data: {
    		  name: name,
    		  description: description,
    		  roles: roles,
    		  users: users
    	  },
    	  success: function() {
    		  self.refresh();
    	  }
      });
  }
  
  this.updateGroup = function(id, name, description, roles, users) {
	  $.ajax({
		  type: 'PUT',
		  url: "/groups/" + id,
		  data: {
			  name: name,
			  description: description,
			  roles: roles,
			  users: users
		  },
		  success: function() {
			  self.refresh();
		  }
	  });
  }
  
  this.renderTable = function(groups, userList) {
	  var table = $("#tableContainer");
	  table.unbind();
	  table.jqotesubtpl("templates/security-groups-table.tpl", {groups: groups, users: userList});
	  table.show();
	  
      table.find('div.toggle-groups').parent().click(function(){
    	  var toogleDiv = $(this).find('div.toggle-groups');
          if(!toogleDiv.hasClass('ui-icon-triangle-1-e')) {
        	  $(this).parent().next().hide();
        	  toogleDiv.addClass('ui-icon-triangle-1-e');
        	  toogleDiv.removeClass('ui-icon-triangle-1-s');
          }
          else {
        	  $(this).parent().next().show();
        	  toogleDiv.addClass('ui-icon-triangle-1-s');
        	  toogleDiv.removeClass('ui-icon-triangle-1-e');
          }
      });
      
      table.find('a').click(function(event) {
    	event.preventDefault();
	    $.ajax({
	      type: 'DELETE',
		  url: this.href,
		  success: function() {
			console.log("successfully deleted");
			self.refresh();
		  }
	  	}); 
      });
      
  	  self.getRoles({}).success(function (roleData) {
    	  var roleList = $.map(A(roleData.roles.role), function(role) {
              return role.name;
          });
		  
	      table.find('form').each(function(i, form) {
	    	  var id = $(form).find("#id");
	          var name = $(form).find("#name");
	          var description = $(form).find("#description");
	          var roles = $(form).find('#rolesSelect');
	          var usersInput = $(form).find('input#users');
	          var users = $(form).find('#usersSelect');
	          var tips = $(form).find("p.validateTips");
	          var removeRoles = $(form).find("button#removeRole");
	          var addUser = $(form).find("button#addUser");
	          var removeUsers = $(form).find("button#removeUser");
	          
	          roles.find('option').sort(nameSort).appendTo(roles);
	          users.find('option').sort(nameSort).appendTo(users);
	          
	          $(form).find('input#roles').autocomplete({
		          source: roleList,
		          minLength: 2,
		          select: function(event, ui) {
		        	  roles.find("option[value='" + ui.item.label + "']").remove();
		        	  roles.append('<option value=' + ui.item.label + '>' + ui.item.label + '</option>');
		        	  roles.find('option').sort(nameSort).appendTo(roles);
		        	  $(this).val('');
		        	  return false;
		          },
		          open: function() {
		              $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
		          },
		          close: function() {
		              $(this).removeClass("ui-corner-top").addClass("ui-corner-all");
		          }
		      });
		      
	          removeRoles.button().click(function(event) {
		    	  roles.find(':selected').each(function(i, option) {
		    		  roles.find("option[value='" + option.label + "']").remove();
		    	  });
		      });
	          
	          addUser.button({ disabled: true }).click(function(event) {
	        	  var userValue = $.trim(usersInput.val());
	        	  if(userValue == '') return;
	        	  
	        	  users.find("option[value='" + userValue + "']").remove();
	        	  if(_.contains(userList, userValue)) {
	        		  users.append('<option value=' + userValue + '>' + userValue + '</option>');
	        	  } else {
	        		  users.append('<option style="background-color: Yellow;" value=' + userValue + '>' + userValue + '</option>');
	        	  }
	        	  users.find('option').sort(nameSort).appendTo(users);
				  usersInput.val('');
				  $(this).button("option", "disabled", true);
	          });
	          
	          usersInput.autocomplete({
				  source: userList,
				  minLength: 2,
				  select: function(event, ui) {
					  users.find("option[value='" + ui.item.label + "']").remove();
					  users.append('<option value=' + ui.item.label + '>' + ui.item.label + '</option>');
					  users.find('option').sort(nameSort).appendTo(users);
					  $(this).val('');
					  $(this).keyup();
					  return false;
				  },
				  open: function() {
					  $(this).removeClass("ui-corner-all").addClass("ui-corner-top");
				  },
				  close: function() {
					  $(this).removeClass("ui-corner-top").addClass("ui-corner-all");
				  }
			  }).keyup(function() {
				  if($.trim(usersInput.val()) == '') {
					  addUser.button("option", "disabled", true);
				  } else {
					  addUser.button("option", "disabled", false);
				  }
			  });
	          
	          removeUsers.button().click(function(event) {
				  users.find(':selected').each(function(i, option) {
					  users.find("option[value='" + option.label + "']").remove();
				  });
			  });
	          
	          $(form).find('button[type|="submit"]').button().click(function(event) {
	         	 event.preventDefault();
	             var valid = true;
	             valid = valid && name.val().length > 0;
	             valid = valid && users.find('option').length > 0;

	             if(valid) {
	            	 self.updateGroup(id.val(), name.val(), description.val(), self.getOptions(roles), self.getOptions(users));
	             } else {
	            	 tips.addClass("ui-state-highlight").show();
	             }
	           });
	      });
      });
  }
  
  /** 
   *  Make the page reload with the currently set configuration
   *  @param newState -- object with new state parameters, see the main state object
   */
  this.reload = function(newState) {
    self.refresh();
  }
  
  this.refresh = function() {
    return _theRefreshFunc();
  }
  
  /** 
   *  Initiate new ajax call to groups list endpoint
   *  @return deferred object
   */
  this._refresh = function() {
    if (!refreshing) {
      refreshing = true;
      
      $('#addGroup').hide();
      $('#tableContainer').hide();
      $('#addAcl').hide();
      $('#aclContainer').hide();
      // issue the ajax request
      return $.Deferred(function (d) {
    	if(ocSecurity.Configuration.state == 'groups') {
    		$.when(self.getGroups({limit:999999}), self.getUsers({limit:999999})).done(function(groups, users) {
                console.log("success groups");
                // ensure rendering has finished before any further functions apply
                self.renderGroups(groups[0], users[0]);
    		});
    	} else if(ocSecurity.Configuration.state == 'acl') {
    		$.when(self.getAcls({}), self.getRoles({})).done(function (acls, roles) {
    			console.log("success acl");
    			self.renderAcl(acls[0], roles[0]);
    		});
    	}
      });
    } else {
      // return an empty deferred
      return $.Deferred();
    }
  }

  /** 
   *  Constructor for a Role that consists of a name and the read and write
   *  privileges
   *
   *  @param name of the role
   *  @param canRead indicates if this role can read (optional)
   *  @param canWrite indicates if this roles can write (optional)
   */
  this.Role = function(name, canRead, canWrite) {
    this.name = name;
    this.canRead = canRead !== undefined ? canRead : false;
    this.canWrite = canWrite !== undefined ? canWrite : false;
    return this;
  }
  
  /** 
   * $(document).ready()
   */
  this.init = function() {
	  $('#addHeader').jqotesubtpl('templates/security-header.tpl', {});

	  // ocSecurity state selectors
	  $('#security-' +  ocSecurity.Configuration.state).attr('checked', true);
	  $('.state-filter-container').buttonset();
	  $('.state-filter-container input').click(function() {
		  ocSecurity.Configuration.state = $(this).val();
		  self.refresh();
	  });
	  
	  self.initGroupDialog();
	  self.initAclDialog();
	  
      _theRefreshFunc = function () {
        return self._refresh();
      };
      self.refresh();
  };
  
  return this;
})();
