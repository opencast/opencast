<div id="content">
    <div class="lti_links">
    <ul>
    <li><a>Recordings</a></li>
    <li><a>Manage</a></li>
    <li><a>Upload</a></li>
    <li><a>Schedule</a></li>
    </ul>
    </div>
  <div class="layout-page-content">
    <div class="form-box scheduler-selection-container">
      <h2><span id="i18n_page_title">Schedule Recording</span></h2>
      <ul id="recordingType">
        <li>
          <input type="radio" id="singleRecording" name="recordingType" checked="checked" />
          <label for="singleRecording" class="lbl_radio"> Single Recording</label>
        </li>
        <li>
          <input type="radio" id="multipleRecordings" name="recordingType" />
          <label for="multipleRecordings" class="lbl_radio"> Group of Recordings</label>
        </li>
      </ul>
    </div>

  <div class="form-box layout-centered layout-page-header ui-helper-hidden" id="missingFieldsContainer">
    <div class="ui-state-error ui-corner-all scheduler-info-container">
      <h3 style="position: relative; padding-left: 20px;">
        <span class="ui-icon ui-icon-alert"></span> <b>Missing or invalid input</b>
      </h3>
      <ul>
        <li id="missingTitle" class="ui-state-error-text single-error multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>Please enter a <a href="javascript:$('#titleField')[0].focus();">title</a> for the recording.
        </li>
        <li id="missingSeries" class="ui-state-error-text multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>Please enter a <a href="javascript:$('#isPartOf')[0].focus();">Series name</a> for this group of recordings.
        </li>
        <li id="missingDistribution" class="ui-state-error-text single-error multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>Please choose at least one <a href="javascript:document.getElementById('distITunesU').focus();">distribution channel</a>.
        </li>
        <li id="missingStartdate" class="ui-state-error-text single-error multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>Please choose a starting <a href="javascript:document.getElementById('startDate').focus()">date</a> and <a href="javascript:document.getElementById('startTimeHour').focus()">time</a> in the future for your recording.
        </li>
        <li id="missingDuration" class="ui-state-error-text single-error multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>You must have a <a href="javascript:document.getElementById('durationHour').focus();">duration</a> greater than 0 hours and 0 minutes.
        </li>
        <li id="missingAgent" class="ui-state-error-text single-error multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>Please choose a <a href="javascript:document.getElementById('agent').focus();">capture agent</a> to record the event.
        </li>
        <li id="missingInputs" class="ui-state-error-text single-error multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>You must choose at least one <a href="javascript:document.getElementById('inputList').focus();">input</a>.
        </li>
        <li id="errorRecurStartEnd" class="ui-state-error-text multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>Please choose an <a href="javascript:$('#recurEnd')[0].focus()">end date</a> that occurs after the start date.
        </li>
        <li id="errorRecurrence" class="ui-state-error-text multiple-error">
          <span class="ui-icon ui-icon-carat-1-e"></span>You must select at least one day of the week.
        </li>
        <li id="errorConflict" class="ui-state-error-text">
          <span class="ui-icon ui-icon-carat-1-e" title=""></span>The following events conflict with your scheduled time on your selected capture agent:
          <ul id="conflictingEvents" style="margin-top: 0px;">
          </ul>
        </li>
      </ul>
    </div>
  </div>

    <!-- Common Scheduling Information -->
    <div class="form-box layout-centered ui-widget">
      <div class="form-box-content ui-widget-content ui-corner-all">
        <form action="">
            <div id="common-data"></div>
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
      <div id="additional-description" class="form-box-content ui-widget-content ui-corner-bottom unfoldable-content"></div>
    </div>

    <!-- WARNINGS -->
    <div class="form-box layout-centered layout-page-header ui-helper-hidden" id="noticeContainer">
      <div class="layout-centered ui-state-highlight ui-corner-all scheduler-info-container">
        <h3 style="position: relative; padding-left: 20px;">
          <span class="ui-icon ui-icon-info"></span> <b>Notice</b>
        </h3>
        <ul>
          <li id="noticeOffline" class="missing-fields-item ui-helper-hidden">
			<span class="ui-icon ui-icon-carat-1-e"></span>The <a href="javascript:document.getElementById('agent').focus();">capture agent</a> you previously selected is currently offline. You may want to select a different capture agent.
          </li>
          <li id="noticeTzDiff" class="missing-fields-item ui-helper-hidden">
            <span class="ui-icon ui-icon-carat-1-e" title=""></span>The timezone for this <a href="javascript:document.getElementById('agent').focus();">capture agent</a> is <span id="tzdiff"></span>. Capture will occur according to the time local to the agent, though when you look at your list of Recordings it will be shown in your local time.
          </li>
          <li id="noticeStartDateMoved" class="missing-fields-item ui-helper-hidden">
            <span class="ui-icon ui-icon-carat-1-e" title=""></span>The start date of the Series has been changed to be in the future.
          </li>
        </ul>
      </div>
    </div>

    <!-- Scheduling Specifics (Single Recording) -->
    <div id="singleRecordingPanel" class="form-box layout-centered ui-widget">
      <div class="form-box-head ui-widget-header ui-corner-top"><span id="i18n_capture">Capture</span></div>
      <div class="form-box-content ui-widget-content ui-corner-bottom">
        <form action="">
          <!--
          <div id="schedule-tpye-select">
            <input type="radio" value="automatic" name="schedule-type" id="automatic-sched">
            <label for="automatic-sched"><span id="i18n_sched_automatic">Automatic</span></label>
            <input type="radio" value="manual" name="schedule-type" id="manual-sched">
            <label for="manual-sched"><span id="i18n_sched_manual">Manual</span></label>
          </div>
          -->
          <ul class="oc-ui-form-list">
            <li>
              <label class="scheduler-label" id="startDateLabel"><span class="scheduler-required-text">* </span><span id="i18n_startdate_label"></span>:</label>
              <input type="text" size="10" name="startDate" id="startDate" />
            </li>
          </ul>
          <ul class="oc-ui-form-list">
            <li>
              <label class="scheduler-label" id="startTimeLabel"><span class="scheduler-required-text">* </span><span id="i18n_starttime_label"></span>:</label>
              <select id="startTimeHour">
                <option value="0">00</option>
                <option value="1">01</option>
                <option value="2">02</option>
                <option value="3">03</option>
                <option value="4">04</option>
                <option value="5">05</option>
                <option value="6">06</option>
                <option value="7">07</option>
                <option value="8">08</option>
                <option value="9">09</option>
                <option value="10">10</option>
                <option value="11">11</option>
                <option value="12">12</option>
                <option value="13">13</option>
                <option value="14">14</option>
                <option value="15">15</option>
                <option value="16">16</option>
                <option value="17">17</option>
                <option value="18">18</option>
                <option value="19">19</option>
                <option value="20">20</option>
                <option value="21">21</option>
                <option value="22">22</option>
                <option value="23">23</option>
              </select>
              <select id="startTimeMin">
                <option value="0">00</option>
                <option value="1">01</option>
                <option value="2">02</option>
                <option value="3">03</option>
                <option value="4">04</option>
                <option value="5">05</option>
                <option value="6">06</option>
                <option value="7">07</option>
                <option value="8">08</option>
                <option value="9">09</option>
                <option value="10">10</option>
                <option value="11">11</option>
                <option value="12">12</option>
                <option value="13">13</option>
                <option value="14">14</option>
                <option value="15">15</option>
                <option value="16">16</option>
                <option value="17">17</option>
                <option value="18">18</option>
                <option value="19">19</option>
                <option value="20">20</option>
                <option value="21">21</option>
                <option value="22">22</option>
                <option value="23">23</option>
                <option value="24">24</option>
                <option value="25">25</option>
                <option value="26">26</option>
                <option value="27">27</option>
                <option value="28">28</option>
                <option value="29">29</option>
                <option value="30">30</option>
                <option value="31">31</option>
                <option value="32">32</option>
                <option value="33">33</option>
                <option value="34">34</option>
                <option value="35">35</option>
                <option value="36">36</option>
                <option value="37">37</option>
                <option value="38">38</option>
                <option value="39">39</option>
                <option value="40">40</option>
                <option value="41">41</option>
                <option value="42">42</option>
                <option value="43">43</option>
                <option value="44">44</option>
                <option value="45">45</option>
                <option value="46">46</option>
                <option value="47">47</option>
                <option value="48">48</option>
                <option value="49">49</option>
                <option value="50">50</option>
                <option value="51">51</option>
                <option value="52">52</option>
                <option value="53">53</option>
                <option value="54">54</option>
                <option value="55">55</option>
                <option value="56">56</option>
                <option value="57">57</option>
                <option value="58">58</option>
                <option value="59">59</option>
              </select>
            </li>
            <li>
              <label class="scheduler-label" id="durationLabel"><span class="scheduler-required-text">* </span><span id="i18n_dur_label"></span>:</label>
              <select id="durationHour">
                <option value="0">0</option>
                <option value="1">1</option>
                <option value="2">2</option>
                <option value="3">3</option>
                <option value="4">4</option>
                <option value="5">5</option>
                <option value="6">6</option>
                <option value="7">7</option>
                <option value="8">8</option>
                <option value="9">9</option>
                <option value="10">10</option>
                <option value="11">11</option>
                <option value="12">12</option>
              </select>
              <span id="i18n_dur_hours"></span>
              <select id="durationMin">
                <option value="0">0</option>
                <option value="1">1</option>
                <option value="5">5</option>
                <option value="10">10</option>
                <option value="15">15</option>
                <option value="20">20</option>
                <option value="25">25</option>
                <option value="30">30</option>
                <option value="35">35</option>
                <option value="40">40</option>
                <option value="45">45</option>
                <option value="50">50</option>
                <option value="55">55</option>
              </select>
              <span id="i18n_dur_minutes"></span>
            </li>
            <li>
              <label class="scheduler-label"  id="agentLabel"><span class="scheduler-required-text">* </span><span id="i18n_agent_label"></span>:</label>
              <select id="agent" name="agent">
                <option value="">Choose one:</option>
              </select>
            </li>
            <li>
              <label class="scheduler-label" for="inputList" id="inputLabel"><span class="scheduler-required-text">* </span><span id="i18n_input_label"></span>:</label>
              <div class="scheduler-radio-list">
                <div id="inputList"></div>
              </div>
            </li>
          </ul>
        </form>
      </div>
    </div>

    <!-- Scheduling Specifics (Multiple Recordings) -->
    <div id="recurringRecordingPanel" class="form-box layout-centered ui-widget">
      <div class="form-box-head ui-widget-header"><b id="i18n_capture_recur">Capture</b></div>
      <div class="form-box-content ui-widget-content">
        <form action="">
          <fieldset class="form-box-content">
            <legend><span id="i18n_recording_date">Recording Date(s)</span></legend>
            <!--
            <div id="schedule-tpye-select">
              <input type="radio" value="automatic" name="schedule-type" id="automatic-sched">
              <label for="automatic-sched"><span id="i18n_sched_automatic">Automatic</span></label>
              <input type="radio" value="manual" name="schedule-type" id="manual-sched">
              <label for="manual-sched"><span id="i18n_sched_manual">Manual</span></label>
            </div>
            -->
            <fieldset>
              <ul class="oc-ui-form-list">
                <li>
                  <label for="scheduleRepeat" class="scheduler-label form-box-label"><span id="i18n_sched_repeats">Repeats</span>:</label>
                  <select id="scheduleRepeat" disabled="true">
                    <option value="norepeat" id="i18n_sched_no_repeat">Don't Repeat</option>
                    <option value="weekly" selected="selected" id="i18n_sched_weekly">Weekly</option>
                  </select>
                </li>
                <li id="daySelect">
                  <label class="scheduler-label" for="repeatDays"><span class="scheduler-required-text">* </span><span id="i18n_sched_days">Days</span>:</label>
                  <table id="agentsTable2" class="layout-inline scheduler-day-table">
                    <thead>
                      <tr>
                        <th id="i18n_day_short_sun">S</th>
                        <th id="i18n_day_short_mon">M</th>
                        <th id="i18n_day_short_tue">T</th>
                        <th id="i18n_day_short_wed">W</th>
                        <th id="i18n_day_short_thu">Th</th>
                        <th id="i18n_day_short_fri">F</th>
                        <th id="i18n_day_short_sat">Sa</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td><input type="checkbox" id="repeatSun" value="SU" /></td>
                        <td><input type="checkbox" id="repeatMon" value="MO" /></td>
                        <td><input type="checkbox" id="repeatTue" value="TU" /></td>
                        <td><input type="checkbox" id="repeatWed" value="WE" /></td>
                        <td><input type="checkbox" id="repeatThu" value="TH" /></td>
                        <td><input type="checkbox" id="repeatFri" value="FR" /></td>
                        <td><input type="checkbox" id="repeatSat" value="SA" /></td>
                      </tr>
                    </tbody>
                  </table>
                </li>
                <li>
                  <label class="scheduler-label" id="recurStartLabel"><span class="scheduler-required-text">* </span><span id="i18n_recurstart_label">Start Date</span>:</label>
                  <input type="text" size="10" name="recurStart" id="recurStart" style="margin-right:5px;" />
                </li>
                <li>
                  <label class="scheduler-label" for="recurEnd" id="recurEndLabel"><span class="scheduler-required-text">* </span><span id="i18n_recurend_label">End Date</span>:</label>
                  <input type="text" size="10" name="recurEnd" id="recurEnd" style="margin-right:5px;" />
                </li>
              </ul>
            </fieldset>
            <ul class="oc-ui-form-list">
              <li>
                <label class="scheduler-label" id="recurStartTimeLabel"><span class="scheduler-required-text">* </span><span id="i18n_recur_start_label">Start Time</span>:</label>
                <select id="recurStartTimeHour">
                  <option value="0">00</option>
                  <option value="1">01</option>
                  <option value="2">02</option>
                  <option value="3">03</option>
                  <option value="4">04</option>
                  <option value="5">05</option>
                  <option value="6">06</option>
                  <option value="7">07</option>
                  <option value="8">08</option>
                  <option value="9">09</option>
                  <option value="10">10</option>
                  <option value="11">11</option>
                  <option value="12">12</option>
                  <option value="13">13</option>
                  <option value="14">14</option>
                  <option value="15">15</option>
                  <option value="16">16</option>
                  <option value="17">17</option>
                  <option value="18">18</option>
                  <option value="19">19</option>
                  <option value="20">20</option>
                  <option value="21">21</option>
                  <option value="22">22</option>
                  <option value="23">23</option>
                </select>
                <select id="recurStartTimeMin">
                  <option value="0">00</option>
                  <option value="1">01</option>
                  <option value="2">02</option>
                  <option value="3">03</option>
                  <option value="4">04</option>
                  <option value="5">05</option>
                  <option value="6">06</option>
                  <option value="7">07</option>
                  <option value="8">08</option>
                  <option value="9">09</option>
                  <option value="10">10</option>
                  <option value="11">11</option>
                  <option value="12">12</option>
                  <option value="13">13</option>
                  <option value="14">14</option>
                  <option value="15">15</option>
                  <option value="16">16</option>
                  <option value="17">17</option>
                  <option value="18">18</option>
                  <option value="19">19</option>
                  <option value="20">20</option>
                  <option value="21">21</option>
                  <option value="22">22</option>
                  <option value="23">23</option>
                  <option value="24">24</option>
                  <option value="25">25</option>
                  <option value="26">26</option>
                  <option value="27">27</option>
                  <option value="28">28</option>
                  <option value="29">29</option>
                  <option value="30">30</option>
                  <option value="31">31</option>
                  <option value="32">32</option>
                  <option value="33">33</option>
                  <option value="34">34</option>
                  <option value="35">35</option>
                  <option value="36">36</option>
                  <option value="37">37</option>
                  <option value="38">38</option>
                  <option value="39">39</option>
                  <option value="40">40</option>
                  <option value="41">41</option>
                  <option value="42">42</option>
                  <option value="43">43</option>
                  <option value="44">44</option>
                  <option value="45">45</option>
                  <option value="46">46</option>
                  <option value="47">47</option>
                  <option value="48">48</option>
                  <option value="49">49</option>
                  <option value="50">50</option>
                  <option value="51">51</option>
                  <option value="52">52</option>
                  <option value="53">53</option>
                  <option value="54">54</option>
                  <option value="55">55</option>
                  <option value="56">56</option>
                  <option value="57">57</option>
                  <option value="58">58</option>
                  <option value="59">59</option>
                </select>
              </li>
              <li>
                <label class="scheduler-label form-box-label" id="recurDurationLabel"><span class="scheduler-required-text">* </span><span id="i18n_recur_dur_label">Duration</span>:</label>
                <select id="recurDurationHour">
                  <option value="0">0</option>
                  <option value="1">1</option>
                  <option value="2">2</option>
                  <option value="3">3</option>
                  <option value="4">4</option>
                  <option value="5">5</option>
                  <option value="6">6</option>
                  <option value="7">7</option>
                  <option value="8">8</option>
                  <option value="9">9</option>
                  <option value="10">10</option>
                  <option value="11">11</option>
                  <option value="12">12</option>
                </select>
                <span id="i18n_recur_dur_hours"></span>
                <select id="recurDurationMin">
                  <option value="0">0</option>
                  <option value="1">1</option>
                  <option value="5">5</option>
                  <option value="10">10</option>
                  <option value="15">15</option>
                  <option value="20">20</option>
                  <option value="25">25</option>
                  <option value="30">30</option>
                  <option value="35">35</option>
                  <option value="40">40</option>
                  <option value="45">45</option>
                  <option value="50">50</option>
                  <option value="55">55</option>
                </select>
                <span id="i18n_recur_dur_minutes"></span>
              </li>
              <li>
                <label class="scheduler-label"  id="recurAgentLabel"><span class="scheduler-required-text">* </span><span id="i18n_agent_label">Agent</span>:</label>
                <select id="recurAgent" name="recurAgent">
                  <option value="">Choose one:</option>
                </select>
              </li>
              <li>
                <label class="scheduler-label" id="inputLabel"><span class="scheduler-required-text">* </span><span id="i18n_input_label">Inputs</span>:</label>
                <div class="scheduler-radio-list">
                  <div id="recurInputList"></div>
                </div>
              </li>
            </ul>
          </fieldset>
        </form>
      </div>
    </div>

    <!-- Processing Instructions -->
    <div id="processingScheduler"></div>

    <div class="form-box layout-centered ui-widget">
      <div class="form-box-content ui-widget-content ui-corner-all">
        <form action="">
          <ul class="oc-ui-form-list">
            <li>
              <label class="scheduler-label">&nbsp;</label>
              <input type="button" value="Schedule" id="submitButton" class="mouseover-pointer control-button" />
              <a id="cancelButton" title="Cancel" class="secondaryButton">Cancel</a>
              <input type="hidden" id="eventId" />
              <input type="hidden" id="recurrenceId" />
              <input type="hidden" id="recurrencePosition" />
              <input type="hidden" id="agentTimeZone" />

              <input type="hidden" id="abstract" />
              <input type="hidden" id="accessRights" />
              <input type="hidden" id="available" />
              <input type="hidden" id="coverage" />
              <input type="hidden" id="created" />
              <input type="hidden" id="date" />
              <input type="hidden" id="extent" />
              <input type="hidden" id="format" />
              <input type="hidden" id="isReferencedBy" />
              <input type="hidden" id="isReplacedBy" />
              <input type="hidden" id="publisher" />
              <input type="hidden" id="relation" />
              <input type="hidden" id="replaces" />
              <input type="hidden" id="rights" />
              <input type="hidden" id="rightsHolder" />
              <input type="hidden" id="source" />
              <input type="hidden" id="type" />
     	    </li>
            <li>
              <label class="scheduler-label">&nbsp;</label>
              <span class="scheduler-required-text">*</span><span id="i18n_required">Required</span>
            </li>
          </ul>
        </form>
      </div>
    </div>

    <!-- Modal popover -->
    <div id="submitModal" title="Scheduling Recordings" class="ui-helper-hidden">
      Adding a large number of recordings may take a few minutes. You may <a href="/admin/">continue scheduling in background</a>, but all recordings may not be listed immediately.
    </div>
  </div>
</div>