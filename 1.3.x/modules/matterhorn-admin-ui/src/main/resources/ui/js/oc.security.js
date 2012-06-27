ocSecurity = new (function() {

  var ROLES_LIST_URL = '/roles/list.json';

  /** Get list of roles. If callback is provided call will be asynchronous, otherwise
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
  
  /** Update the privileges of an array of Roles. Each Roles privileges are set
   *  according to the value of checkboxes with id=${role name}CanRead / ${role name}CanRead.
   *
   */
  this.updatePrivileges = function(privileges) {
    $(privileges).each(function(index, role) {
      role.canRead = getCheckboxValue(role.name + 'CanRead');
      role.canWrite = getCheckboxValue(role.name + 'CanWrite');
    });
  }

  function getCheckboxValue(name) {
    $cb = $('#' + name);
    if ($cb.length != 0) {
      return $cb.is(':checked');
    } else {
      return false;
    }
  }

  /** Constructor for a Role that consists of a name and the read and write
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

});
