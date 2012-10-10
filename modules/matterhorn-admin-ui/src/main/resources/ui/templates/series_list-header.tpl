<div class="layout-page-content">
  <button type="button" id="addSeriesButton" onclick="document.location = '/admin/index.html#/series'">
    <span id="i18n_button_schedule">&nbsp; Add Series</span>
  </button>
  <div id="seriesTableContainer" />
  <!-- TABLE PAGINATION OPTIONS -->
  <div class="ui-helper-clearfix">
    <div id="perPageContainer" class="ui-widget ui-state-hover ui-corner-all">
      <span>
        Show
        <select id="pageSize" class="paging-nav-pagesize-selector">
          <option value="5" >5</option>
          <option value="10" selected="selected">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
        </select>
        Series per page
      </span>
    </div>
    <span id="pageWidget" class="layout-inline">
      <span id="prevButtons" class="ui-helper-hidden">
        <a class="prevPage" href="javascript:ocSeriesList.Configuration.startPage=0;ocSeriesList.askForSeries();">&laquo; first</a>
        <a class="prevPage" href="javascript:ocSeriesList.previousPage();" id="previousPage">&lt;previous</a>
      </span>
      <span id="prevText">
        <span>&laquo; first</span>
        <span>&lt;previous</span>
      </span>
      <span>
        <span id="curPage"></span>
        <span> of </span>
        <span id="numPage"></span>
      </span>
      <span id="pageList"></span>
      <span id="nextButtons">
        <a class="nextPage" href="javascript:ocSeriesList.nextPage();" id="nextPage">next&gt;</a>
        <a class="nextPage" href="javascript:ocSeriesList.Configuration.startPage = ocSeriesList.Configuration.lastPage-1;ocSeriesList.askForSeries();" id="lastPage">last &raquo;</a>
      </span>
      <span id="nextText" class="ui-helper-hidden">
        <span>next&gt;</span>
        <span>last &raquo;</span>
      </span>
    </span>
  </div>
  <!-- END Table pagination Options -->
</div>