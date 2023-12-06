<div id="recordings-header">
    <div class="lti_links">
    <ul>
    <li><a>Recordings</a></li>
    <li><a>Manage</a></li>
    <li><a>Upload</a></li>
    <li><a>Schedule</a></li>
    </ul>
    </div>
    <div class="layout-centered">
      <h2><span id="i18n_page_title">Manage recordings</span></h2>
    </div>
  <div id="stage" class="ui-widget">
    <div id="progressIndicator" class="layout-float-left ui-helper-hidden">
      <label for="editProgress" style="margin-left: 50px;">Editing in progress</label><br />
    </div>
  </div>
    <div class="layout-page-header recordings-bulk-action ui-corner-all layout-centered" id="bulkActionPanel">
      <div class="ui-widget layout-centered">
        <select id="bulkActionSelect">
          <option value="select">- Select Action -</option>
          <option value="edit">Edit Metadata</option>
          <option value="delete">Delete Recordings</option>
        </select>
        <button type="button" class="recordings-cancel-bulk-action" id="cancelBulkAction">
          <span id="i18n_button_cancel_bulk_action">Cancel Bulk Action</span>
        </button>
      </div>
      <div id="bulkEditPanel">
        <!-- Common Information -->
        <div class="form-box layout-centered ui-widget">
          <div class="form-box-content ui-widget-content ui-corner-all">
            <form action="">
              <ul class="oc-ui-form-list">
                <li>
                  <label for="title" id="titleLabel" class="scheduler-label"><span id="i18n_title_label">Title</span>:</label>
                  <input type="text" id="title" name="title" class="oc-ui-form-field" maxlength="255" />
                </li>
                <li>
                  <label for="creator" id="creatorLabel" class="scheduler-label"><span id="i18n_presenter_label">Presenter</span>:</label>
                  <input type="text" class="oc-ui-form-field" name="creator" id="creator" maxlength="255" />
                </li>
                <li id="seriesContainer">
                  <label for="seriesSelect" id="seriesLabel" class="scheduler-label"><span id="i18n_series_label">Course/Series</span>:</label>
                  <input type="text" class="oc-ui-form-field ui-autocomplete-input" name="seriesSelect" id="seriesSelect" maxlength="255" />
                  <input type="hidden" id="series" />
                </li>
              </ul>
            </form>
          </div>
        </div>
        <!-- Optional Information -->
        <div class="form-box layout-centered ui-widget oc-ui-collapsible-widget">
          <div class="form-box-head ui-widget-header ui-corner-top oc-ui-cursor">
            <div id="additional_icon" class="ui-icon ui-icon-triangle-1-e"></div>
            <div id="i18n_additional">Additional Description</div>
            <div class="clear"></div>
          </div>

          <div class="form-box-content ui-widget-content ui-corner-bottom">
            <form action="">
              <ul class="oc-ui-form-list">
                <li>
                  <label for="contributor" id="contributorLabel" class="scheduler-label"><span id="i18n_dept_label">Contributor</span>:</label>
                  <input type="text" class="oc-ui-form-field" name="contributor" id="contributor" maxlength="255" />
                </li>
                <li>
                  <label for="subject" id="subjectLabel" class="scheduler-label"><span id="i18n_sub_label">Subject</span>:</label>
                  <input type="text" class="oc-ui-form-field" name="subject" id="subject" maxlength="255" />
                </li>
                <li>
                  <label for="language" id="languageLabel" class="scheduler-label"><span id="i18n_lang_label">Language</span>:</label>
                  <input type="text" class="oc-ui-form-field" name="language" id="language" maxlength="255" />
                </li>
                <li>
                  <label for="description" id="descriptionLabel" class="scheduler-label"><span id="i18n_desc_label">Description</span>:</label>
                  <textarea name="description" id="description" class="oc-ui-form-field" rows="5" cols="10"></textarea>
                </li>
              </ul>
            </form>
          </div>
        </div>
      </div>

      <div id="bulkActionApply" class="form-box layout-centered ui-widget">
        <div class="form-box-content ui-widget-content ui-corner-all">
          <ul class="oc-ui-form-list">
            <li>
              <label>&nbsp;</label>
              <span id="bulkActionApplyMessage"></span>
            </li>
            <li>
              <label>&nbsp;</label>
              <button type="button" id="applyBulkAction">
                <span id="i18n_button_apply_bulk_action">Apply Changes</span>
              </button>
              <button type="button" id="cancelBulkAction" class="recordings-cancel-bulk-action">
                <span id="i18n_button_cancel_bulk_action">Cancel Bulk Action</span>
              </button>
            </li>
          </ul>
        </div>
      </div>

      <div id="deleteModal" title="Deleting Recordings">
        <div id="deleteProgress"> </div>
        <div id="deleteError" class="ui-helper-hidden">
          <div id="deleteErrorMessage"></div>
          <button id="closeDelete" onclick="ocRecordings.closeDeleteDialog()">Close</button>
        </div>
      </div>
    </div>
  </div>

  <div id="tableContainer" class="ui-widget ui-helper-clearfix"></div>

