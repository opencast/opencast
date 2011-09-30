<div class="layout-page-content">
  <div>
    <h2 style="text-align:center"><span id="i18n_page_title">Add Series</span></h2>
  </div>
  <div class="form-box layout-centered ui-widget">
    <div class="form-box-content ui-widget-content ui-corner-all">
      <ul class="oc-ui-form-list">
        <li>
          <label for="title" class="scheduler-label"><span style="color: red;">* </span><span id="i18n_title">Series Title</span>:</label>
          <input type="text" class="oc-ui-form-field dc-metadata-field" id="title" />
        </li>
        <li>
          <label for="creator" class="scheduler-label"><span id="i18n_creator">Organizer</span>:</label>
          <input type="text" class="oc-ui-form-field dc-metadata-field" id="creator" />
        </li>
        <li>
          <label for="contributor" class="scheduler-label"><span id="i18n_contributor">Contributor</span>:</label>
          <input type="text" class="oc-ui-form-field dc-metadata-field" id="contributor" />
        </li>
      </ul>
    </div>
  </div>
  <div class="form-box layout-centered ui-widget oc-ui-collapsible-widget">
    <div class="form-box-head ui-widget-header ui-corner-top oc-ui-cursor">
      <div class="ui-icon ui-icon-triangle-1-e fold-icon"></div>
      <div id="i18n_content_descriptors" class="fold-icon-text">Additional Content Descriptors</div>
    </div>
    <div class="ui-widget-content ui-corner-bottom">
      <div id="additionalContentTabs" style="border: none;">
        <ul>
          <li><a href="#commonTab" id="i18n_common_tab">Common Descriptors</a></li>
          <!--<li><a href="#additionalTab" id="i18n_additional_tab">Additional Metadata</a></li>-->
        </ul>
        <div id="commonTab">
          <ul class="oc-ui-form-list">
            <li>
              <label for="subject" class="scheduler-label"><span id="i18n_subject">Subject</span>:</label>
              <input type="text" class="oc-ui-form-field dc-metadata-field" id="subject" />
            </li>
            <li>
              <label for="language" class="scheduler-label"><span id="i18n_langauge">Language</span>:</label>
              <input type="text" class="oc-ui-form-field dc-metadata-field" id="language" />
            </li>
            <li>
              <label for="license" class="scheduler-label"><span id="i18n_license">License</span>:</label>
              <input type="text" class="oc-ui-form-field dc-metadata-field" id="license" />
            </li>
            <li>
              <label for="description" class="scheduler-label"><span id="i18n_description">Description</span>:</label>
              <textarea class="oc-ui-form-field dc-metadata-field" id="description"></textarea>
            </li>
          </ul>
        </div>
        <!--<div id="additionalTab">Some Stuff</div>-->
      </div>
    </div>
  </div>
  <!-- PRIVILEGES -->
  <div class="form-box layout-centered ui-widget oc-ui-collapsible-widget">
    <div class="form-box-head ui-widget-header ui-corner-top oc-ui-cursor">
      <div class="ui-icon ui-icon-triangle-1-e fold-icon"></div>
      <div id="i18n_content_descriptors" class="fold-icon-text">Privileges</div>
    </div>
    <div id="rolesTableContainer" class="ui-widget-content ui-corner-bottom">
      <p id="hint">
        Specify roles for which users can access Recordings in this Series,
        and indicate what level of access they should have.<br />
        Hint: Role names are formatted according to rules specific to your institution.
      </p>
      <table id="rolePrivilegeTable" border="1">
        <thead>
          <tr>
            <th>Role</th>
            <th>View</th>
            <th>Administer</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Public (no authorization required)</td>
            <td class="privilege_edit"><input type="checkbox" id="anonymous_view" /></td>
            <td class="privilege_edit"><input type="checkbox" disabled="disabled" /></td>
            <td></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
  <div class="form-box layout-centered ui-widget">
    <div class="form-box-content ui-widget-content ui-corner-all">
      <form action="">
        <ul class="oc-ui-form-list">
          <li>
            <label class="scheduler-label">&nbsp;</label>
            <input type="button" value="Save" id="submitButton" />
            <input type="button" value="Cancel" id="cancelButton" />
            <input type="hidden" id="id" />
          </li>
          <li>
            <label class="scheduler-label">&nbsp;</label>
            <span class="scheduler-required-text">*</span><span id="i18n_required">Required</span>
          </li>
        </ul>
      </form>
    </div>
  </div>
</div>