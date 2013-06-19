(function($) {
  $.widget("ui.searchbox", {

    options : {
      border : false,
      searchText : '',
      idleTextColor : 'silver',
      textColor : 'black',
      textBackground : 'white',
      searchIcon : 'ui-icon-search',
      clearIcon : 'ui-icon-close',
      options : null,
      selectedOption : null
    },

    _version : 'searchbox v0.1',

    _css :
    '.ui-searchbox {padding:5px;} \n' +
    '.ui-searchbox .searchbox-search-icon {float:left;cursor:pointer;margin-right:3px;} \n' +
    '.ui-searchbox .searchbox-text-container {float:left;} \n' +
    '.ui-searchbox select {float:right;}' +
    '.ui-searchbox .searchbox-text-container .searchbox-clear-icon {float:right;} \n' +
    '.ui-searchbox .searchbox-text-container .searchbox-text-input {float:left;border:none;}' +
    '.ui-searchbox select optgroup option {padding-left:0px;}' +
    '.ui-searchbox select .nosep {border:none;}' +
    '.ui-searchbox select .withsep {border-top:1px solid black;}',    // TODO make separator style configurable

    _markup :
    '<span class="searchbox-text-container ui-corner-all ui-helper-clearfix" style="float:left;">' +
    '  <input type="text" class="searchbox-text-input ui-corner-all" style="float:left;border:none;">' +
    '  <span class="searchbox-clear-icon ui-icon" style="float:right;"></span>' +
    '</span>' +
    '<span class="searchbox-search-icon ui-icon" style="float:left;cursor:pointer;margin-right:3px;"></span>',

    clear : function() {
      if (this.options.clear !== undefined) {
        this.options.clear();
      }
    },

    search : function(t) {
      if (this.options.search !== undefined && $.isFunction(this.options.search)) {
        if (this.options.options !== undefined) {
          this.options.search(this.options._input.val(), this.element.find('select').val());
        } else {
          this.options.search(this.options._input.val());
        }
      }
    },

    /** creation function *
     */
    _create : function() {
      var self = this;

      // prepare container
      var width = this.element.width();
      if (width < 140) {
        width = 140;
      }
      this.element.addClass('ui-searchbox ui-widget ui-state-hover ui-corner-all ui-helper-clearfix');
      if (this.options.border) {
        this.element.css('border', this.options.border);
      }

      // prepare markup
      $(this._markup).appendTo(this.element);
      this.element.find('.searchbox-text-container').css('background', this.options.textBackground);

      this.element.find('.searchbox-clear-icon')
      .addClass(this.options.clearIcon)
      .click(function(event) {
        self.element.find('input').val('');
        self.clear();
      });

      // options dropdown
      if (this.options.options != null) {
        var dropdown = $('<select></select>').css('float', 'right');
        var selected = this.options.selectedOption;

        // care for options that are grouped
        if ($.isArray(this.options.options)) {
          $.each(this.options.options, function(index, options) {
            var optgroup = $('<optgroup></optgroup>');
            if (index == 0) {
              optgroup.css('border', 'none');
            } else {
              optgroup.css('border-top', '1px solid black');
            }
            self._appendOptions(optgroup, options, selected);
            dropdown.append(optgroup);
          });
        } else {
          self._appendOptions(dropdown, this.options.options);
        }
        this.element.css('padding', '5px');
        this.element.append(dropdown);
      }

      this.element.find('.searchbox-search-icon').addClass(this.options.searchIcon)
      .click(function(event) {
        var t = self.element.find('input').val();
        if (t == '') {
          self.element.find('input').focus();
        } else {
          self.search(t);
        }
      });

      // correct layout
      var textboxwidth = this.element.innerWidth() - this.element.find('.searchbox-search-icon').outerWidth(true) - 5;
      if (dropdown !== undefined) {
        textboxwidth -= dropdown.outerWidth(true) + 9;
        this.element.find('.searchbox-text-container').height(dropdown.outerHeight());
      }
      this.element.find('.searchbox-text-container').css('width', textboxwidth);
      var inputheight = this.element.find('.searchbox-text-container input').outerHeight();
      var inputContainerHeight = this.element.find('.searchbox-text-container').innerHeight();
      this.element.find('.searchbox-text-container input').css('width', textboxwidth-16);
      this.element.find('.searchbox-text-container input').css('margin-top', (inputContainerHeight - inputheight) / 2);
      this.element.find('.searchbox-search-icon').css('margin-top', (this.element.innerHeight() - 10 - 16) / 2);
      this.element.find('.searchbox-clear-icon').css('margin-top', (inputContainerHeight - 16) / 2);

      // text input
      this.options._input = this.element.find('.searchbox-text-input')
      .css({
        'color' : this.options.textColor,
        'background' : this.options.textBackground
      })
      .val(this.options.searchText)
      .keydown(function(event) {
        if (event.keyCode) {
          var input = self.element.find('input');
          if (event.keyCode == $.ui.keyCode.ENTER) {
            self.search(input.val());
          }
        }
      });
    },

    _appendOptions: function(elm, options, selected_val) {
      $.each(options, function(key, val) {
        var option = $('<option></option>').css('padding-left', '0px').text(val).val(key);
        if (selected_val !== undefined && selected_val == key) {
          option.attr('selected', 'selected');
        }
        elm.append(option);
      });
    }

  });
})(jQuery);
