<div id="recordings-header">
  <div id="stage" class="ui-widget">
    <div id="progressIndicator" class="layout-float-left ui-helper-hidden">
      <label for="editProgress" style="margin-left: 50px;">Editing in progress</label><br />
      <img src="/admin/img/misc/loading.gif" alt="editing in progress" title="Editing in progress" />
    </div>
  </div>
  <div id="uploadContainer">
    <button id="uploadButton" type="button">Upload Recording</button>
    <button id="scheduleButton" type="button">Schedule Recording</button>
  </div>
  <div id="controlsTop" class="ui-helper-clearfix">
    <div class="IE_buffer"></div>
    <div id="runningStatesContainer">
      <input type="radio" name="stateSelect" value="all" id="state-all" /><label for="state-all">All<span id="stats-all"></span></label>
      <input type="radio" name="stateSelect" value="upcoming" id="state-upcoming" /><label for="state-upcoming">Upcoming<span id="stats-upcoming"></span></label>
      <input type="radio" name="stateSelect" value="capturing" id="state-capturing" /><label for="state-capturing">Capturing<span id="stats-capturing"></span></label>
      <input type="radio" name="stateSelect" value="processing" id="state-processing" /><label for="state-processing">Processing<span id="stats-processing"></span></label>
      <input type="radio" name="stateSelect" value="finished" id="state-finished" /><label for="state-finished">Finished<span id="stats-finished"></span></label>
    </div>

    <div id="notRunningStatesContainer">
      <input type="radio" name="stateSelect" value="hold" id="state-hold" /><label for="state-hold">On Hold<span id="stats-hold"></span></label>
      <input type="radio" name="stateSelect" value="ignored" id="state-ignored" /><label for="state-ignored">Ignored<span id="stats-ignored"></span></label>
      <input type="radio" name="stateSelect" value="failed" id="state-failed" /><label for="state-failed">Failed<span id="stats-failed"></span></label>
    </div>

    <div id="searchBox" class="ui-state-hover"></div>
    <div class="clear"></div>
    <div id="search-result"><span id="filterRecordingCount"></span></div>

	<div id="dateFilterControls">
	  <select id="dateFilter">
	  	<option value="all">All Dates</option>
	  	<option value="today">Today</option>
	  	<option value="tomorrow">Tomorrow</option>
	  	<option value="yesterday">Yesterday</option>
	  	<option value="this_week">This Week</option>
	  	<option value="past_week">Past Week</option>
	  	<option value="next_week">Next Week</option>
	  	<option value="range">Custom Range</option>
	  </select>
	  <span>
	    <label for="fromdate" style="margin-left: 20px;">From: </label><input type="text" size="10" name="fromdate" id="fromdate" style="margin-right: 5px;" />
        <label for="todate" style="margin-left: 20px;">To: </label><input type="text" size="10" name="todate" id="todate" style="margin-right: 5px;" />
        <button id="setRange" type="button" style="margin:0 10px;">Set Range</button>
      </span>
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
              <label>&#160;</label>
              <span id="bulkActionApplyMessage"></span>
            </li>
            <li>
              <label>&#160;</label>
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

  <div id="controlsFoot" class="ui-helper-clearfix">
    <div id="refreshControlsContainer" class="ui-widget ui-state-hover ui-corner-all">
      <input type="checkbox" id="refreshEnabled" /><label for="refreshEnabled"></label>
      <span class="refresh-text">Update table every&#160;</span>
      <select id="refreshInterval">
        <option value="5">5</option>
        <option value="10">10</option>
        <option value="30">30</option>
      </select>
      <span class="refresh-text">&#160;seconds.</span>
    </div>

    <div id="perPageContainer" class="ui-widget ui-state-hover ui-corner-all">
      Show
      <select id="pageSize">
        <option>5</option>
        <option selected="selected">10</option>
        <option>20</option>
        <option>50</option>
        <option>100</option>
      </select>
      Recordings per page.
    </div>

    <!--<label for="page">Goto page: </label><input type="text" id="page" value="1" />-->
    <span id="pageWidget" class="layout-inline">
      <span id="prevButtons">
        <a class="prevPage" href="javascript:ocRecordings.Configuration.page=0;ocRecordings.reload();">&lt;&lt;first</a>
        <a class="prevPage" href="javascript:ocRecordings.previousPage();" id="previousPage">&lt;previous</a>
      </span>
      <span id="prevText" class="ui-helper-hidden">
        <span>&lt;&lt;first</span>
        <span>&lt;previous</span>
      </span>
      <span id="pageList"></span>
      <span id="nextButtons">
        <a class="nextPage" href="javascript:ocRecordings.nextPage();" id="nextPage">next&gt;</a>
        <a class="nextPage" href="javascript:ocRecordings.lastPage();" id="lastPage">last&gt;&gt;</a>
      </span>
      <span id="nextText" class="ui-helper-hidden">
        <span>next&gt;</span>
        <span>last&gt;&gt;</span>
      </span>
    </span>
  </div>
</div>
