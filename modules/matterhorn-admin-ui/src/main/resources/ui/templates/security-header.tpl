<div id="stage" class="ui-widget">
  <div id="controlsTopSecurity" class="ui-helper-clearfix">
    <div class="state-filter-container">
      <input type="radio" name="stateSelect" value="groups" id="security-groups" /><label for="security-groups">Groups</label>
      <input type="radio" name="stateSelect" value="acl" id="security-acl" /><label for="security-acl">ACL</label>
    </div>
  </div>
  <div id="addGroup" class="ui-widget ui-helper-clearfix">
    <h3>Add Group<h3>
    <img class="ui-icon ui-icon-circle-plus"></img>
  </div>
  <div id="tableContainer" class="ui-widget ui-helper-clearfix"></div>
  <div id="groups-form" style="display:none;" title="Create new group"></div>
  <div id="addAcl" class="ui-widget ui-helper-clearfix">
    <h3>Add ACL<h3>
    <img class="ui-icon ui-icon-circle-plus"></img>
  </div>
  <div id="aclContainer" class="ui-widget ui-helper-clearfix"></div>
  <div id="acl-form" style="display:none;" title="Create new ACL"></div>
</div>