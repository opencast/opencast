function TableView(params) {
  Object.defineProperty(this, 'table', {
    value: $('#grid')
  });
  Object.defineProperty(this, 'eventMgr', {
    get: function() {
      return params.eventManager
    }
  });

  var _tableLimit = 10;
  Object.defineProperty(this, 'tableLimit', {
    get: function() {
      return _tableLimit;
    },
    set: function(val) {
      if (!isNaN(val)) {
        _tableLimit = val;
        $('#limitDropup').text(val);
      }
    }
  });
  this.tableCurrent = 0;
  this.tableTotal = 0;
  this.isPersonal = !!params.isPersonal;

  params.eventManager.on('complete,filtered', function(list) {
    this.tableLimit = +(this.storage('session', 'table.limit') || 10);
    this.tableTotal = list.length;
    this.setPagination();
    this.populateTable(list.sort(this.sortFn.bind(this)));
    if (window.self !== window.top) {
      window.top.postMessage(JSON.stringify({
                subject: "lti.frameResize",
                height: document.body.clientHeight
        }), "*");
    }
  }.bind(this));

  this.setTemplate();
  this.attachListeners();

  var _sortKey = 'start_date';
  Object.defineProperty(this, 'sortKey', {
    get: function() {
      return _sortKey;
    },
    set: function(key) {
      if (typeof key == 'string') {
        _sortKey = key;
      }
    }
  });
}

TableView.prototype = {
  constructor: TableView,
  setPagination: function(current) {
    this.tableCurrent = current || this.tableCurrent;
    var currentPage = (this.tableCurrent / this.tableLimit) >> 0;
    $('#grid-footer').html(tmpl('tmpl-footer', {shown: this.tableLimit, total: this.tableTotal, start: currentPage}));
  },
  setLimit: function(e) {
    this.tableLimit = +($(e.target).data('limit'));
    this.tableCurrent = (this.tableCurrent/this.tableLimit >> 0) * this.tableLimit;

    this.storage('session', 'table.limit', this.tableLimit);

    this.setPagination();
    this.requestPopulate();
  },
  setPage: function(e) {
    e.preventDefault();
    var _this = e.target;

    while (_this && _this.tagName.toLowerCase() !== 'a') {
      _this = _this.parentNode;
    }

	if (!$(_this).attr('href')) {
	  return;
	}

    if ($(_this).attr('aria-label') === 'Next') {
      if (!$(_this).parent().siblings('.active').next().find('a').attr('aria-label')) {
        $(_this).parent().siblings('.active').next().find('a').click();
      }
      return;
    }
    if ($(_this).attr('aria-label') === 'Previous') {
      if (!$(_this).parent().siblings('.active').prev().find('a').attr('aria-label')) {
        $(_this).parent().siblings('.active').prev().find('a').click();
      }
      return;
    }

    $(_this).parent()
      .addClass('active')
      .siblings('.active')
        .removeClass('active');

    this.tableCurrent = (+(($(_this).attr('href').split('_'))[1]) - 1) * this.tableLimit;

    this.setPagination();
    this.requestPopulate();
  },
  populateTable: function(list, cb) {
    var displayList = list.filter(function(details, i) {
                        return i >= this.tableCurrent && i < this.tableCurrent + this.tableLimit;
                      }.bind(this));

    if (displayList.length === 0) {
      $('#grid-body').empty();
      var emptyRow = $('<tr/>');
      var emptyCell = $('<td/>', {text: 'No Results'});

      emptyCell.attr('colspan', $('#grid thead th').length);
      emptyCell.css({
         textAlign: 'center',
         fontStyle: 'italic',
            height: '5rem',
        lineHeight: '5rem',
             color: '#777'
      });

      emptyRow.append(emptyCell);
      $('#grid-body').append(emptyRow);
      return;
    }
    $('#grid-body').html(this.isPersonal ? tmpl('tmpl-personalbody', displayList) : tmpl('tmpl-body', displayList));

    $('#currentTotals')
      .attr('data-start', this.tableCurrent + 1)
      .attr('data-end', Math.min(this.tableCurrent + this.tableLimit, list.length));

    this.eventMgr.once('selection', this.showTableSelections.bind(this));
  },
  showTableSelections: function(selectedEvents) {
    var selectedIds = selectedEvents.map(function(event) { return event.id });

    $('#grid-body tr[data-status="Upcoming"] input').each(function() {
      var id = this.id.replace(/chk-/g, '');
      this.checked = selectedIds.indexOf(id) > -1;
    });

    var maxChecked = $('#grid-body tr[data-status="Upcoming"] input').length;
    var numChecked = $('#grid-body tr[data-status="Upcoming"] input:checked').length;
    $('#chk-head')[0].className = 'checkbox ' + (maxChecked === numChecked ? 'all' :
                                  (numChecked === 0 ? 'none' : 'partial'));
  },
  requestPopulate: function() {
    this.eventMgr.once('filtered', function(list) {
      this.populateTable(list.sort(this.sortFn.bind(this)));
      if (window.self !== window.top) {
        window.top.postMessage(JSON.stringify({
                  subject: "lti.frameResize",
                  height: document.body.clientHeight
          }), "*");
      }
    }.bind(this));
  },
  toggleSelection: function(e) {
    var eventId = e.target.id.replace(/chk-/g, '');
    this.eventMgr[e.target.checked ? 'addSelection' : 'removeSelection'](eventId);
  },
  changeSort: function(e) {
    var _this = e.target;
    var col = $(_this).data('column');

    if ($(_this).hasClass('currentSort')) {
      _this.setAttribute('data-sortdir', _this.getAttribute('data-sortdir') === 'asc' ? 'desc' : 'asc');
    }
    else {
      $(_this).addClass('currentSort')
        .siblings('.currentSort').removeClass('currentSort');
    }

    this.setSortFn(col, _this.getAttribute('data-sortdir'));
    this.requestPopulate();
  },
  setSortFn: function(col, dir) {
    this.sortKey = col;

    var newSortFn = null;

    switch (this.sortKey) {
      case 'title':
      case 'agent_id':
      case 'status':
      case 'series':
        newSortFn = dir === 'asc' ? 'sortAlpha' : 'revSortAlpha';
        break;

      case 'presenters':
        newSortFn = dir === 'asc' ? 'sortMixedAlpha' : 'revSortMixedAlpha';
        break;

      case 'start_date':
        newSortFn = dir === 'asc' ? 'sortDate' : 'revSortDate';
        break;
    }

    if (newSortFn) {
      this.__proto__.sortFn = this.__proto__[newSortFn];
    }
  },
  sortAlpha: function(a, b) {
    if (this.sortKey === 'series') {
      if (a.series.title.toLowerCase() < b.series.title.toLowerCase()) {
        return -1;
      }
      if (a.series.title.toLowerCase() > b.series.title.toLowerCase()) {
        return 1;
      }
      return 0;    
    }

    if (a[this.sortKey].toLowerCase() < b[this.sortKey].toLowerCase()) {
      return -1;
    }
    if (a[this.sortKey].toLowerCase() > b[this.sortKey].toLowerCase()) {
      return 1;
    }
    return 0;    
  },
  revSortAlpha: function(a, b) {
    return -1 * this.sortAlpha(a, b);
  },
  sortMixedAlpha: function(a, b) {
    if (a[this.sortKey].join(',').toLowerCase() < b[this.sortKey].join(',').toLowerCase()) {
      return -1;
    }
    if (a[this.sortKey].join(',').toLowerCase() > b[this.sortKey].join(',').toLowerCase()) {
      return 1;
    }
    return 0;    
  },
  revSortMixedAlpha: function(a, b) {
    return -1 * this.sortMixedAlpha(a, b);
  },
  sortDate: function(a, b) {
    return ((new Date(a[this.sortKey])).getTime() - (new Date(b[this.sortKey])).getTime());
  },
  revSortDate: function(a, b) {
    return -1 * this.sortDate(a, b);
  },
  sortFn: function(a, b) {
    return this.sortDate(a, b);
  },
  setTemplate: function() {
    $('#grid-header .nav li:not([data-ref="all"])').show();
    $('#grid-body td input[type="checkbox"]')
      .customCheckbox()
      .on('selected.all', $('#grid').selectRows)
      .on('selected.none', $('#grid').selectRows);
  },
  attachListeners: function() {
    $('#grid-footer').on('click', '[aria-labelledby=limitDropup] li[data-limit]', this.setLimit.bind(this));
    $('#grid-footer').on('click', 'nav a', this.setPage.bind(this));
    $('#grid').on('click', 'tr th[data-column]', this.changeSort.bind(this));
    $('#grid-body').on('change', 'tr[data-status=Upcoming] input[type=checkbox]', this.toggleSelection.bind(this));
    this.eventMgr.on('selection', this.showTableSelections);
  },
  storage: function(type, key, val) {
    var storageType = type === 'session' ? 'sessionStorage' : 'localStorage';
    if (window[storageType]) {
      if (typeof val == 'undefined') {
        try {
          return JSON.parse(window[storageType].getItem(key));
        } catch (e) {
          return window[storageType].getItem(key);
        }
      }
      else {
        val = typeof val == 'string' ? val : JSON.stringify(val);
        window[storageType].setItem(key, val);
      }
    }
  },
}
