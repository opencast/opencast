<div id="episodes-header">
  <div id="searchBox" class="ui-state-hover"></div>
  <div class="clear"></div>
  <div id="search-result"><span id="filterRecordingCount"></span></div>

  <div id="tableContainer" class="ui-widget ui-helper-clearfix"></div>

  <!-- div id="controlsFoot" class="ui-widget-header ui-corner-bottom ui-helper-clearfix" -->
  <div id="controlsFoot" class="ui-helper-clearfix">
    <div id="refreshControlsContainer" class="ui-widget ui-state-hover ui-corner-all">
      <input type="checkbox" id="refreshEnabled"/><label for="refreshEnabled"></label>
      <span class="refresh-text">Update table every&nbsp;</span>
      <select id="refreshInterval">
        <option value="5">5</option>
        <option value="7">7</option>
        <option value="10">10</option>
      </select>
      <span class="refresh-text">&nbsp;seconds.</span>
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
      episodes per page.
    </div>

    <!--<label for="page">Goto page: </label><input type="text" id="page" value="1" />-->
                        <span id="pageWidget" class="layout-inline">
                            <span id="prevButtons">
            <a class="prevPage" href="javascript:ocArchive.conf.page=0;ocArchive.reload();">
              &lt;&lt;first</a>
            <a class="prevPage" href="javascript:ocArchive.previousPage();" id="previousPage">&lt;previous</a>
                            </span>
                            <span id="prevText" class="ui-helper-hidden">
                              <span>&lt;&lt;first</span>
                              <span>&lt;previous</span>
                            </span>
                            <span id="pageList"></span>
                            <span id="nextButtons">
            <a class="nextPage" href="javascript:ocArchive.nextPage();" id="nextPage">next&gt;</a>
            <a class="nextPage" href="javascript:ocArchive.lastPage();" id="lastPage">last&gt;&gt;</a>
                            </span>
                            <span id="nextText" class="ui-helper-hidden">
                              <span>next&gt;</span>
                              <span>last&gt;&gt;</span>
                            </span>
                        </span>
  </div>
</div>