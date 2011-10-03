/**
 * jQuery UI - spinner like component
 *
 * Allows for setting number values by either using the de-/increase button
 * or clicking the label button and entering the value in the appearing
 * text input.
 *
 * bwulff@uos.de
 */
(function($) {
  $.widget("ui.spinner", {
    options : {
      value : 0,
      minValue: null,
      maxValue: null,
      allowEdit : true,
      stepWidth : 1,
      strictStepping : false,
      decIcon : 'ui-icon-circle-triangle-s',
      incIcon : 'ui-icon-circle-triangle-n',
      decTitle : 'decrease',
      incTitle : 'increase',
      middleTitle : 'click to enter a value',
      labelText : null
    },

    // creation function
    _create : function() {
      var self = this;
      // decrease button
      this.decButton = $('<div class="spinner-button">prev</div>')
      .attr('title', this.options.decTitle)
      .button({
        text: false,
        icons: {
          primary:this.options.decIcon
        }
      })
      .click(function() {
        $(this).parent().spinner('decrease');
        return false;
      });
      // increase button
      this.incButton = $('<div  class="spinner-button">next</div>')
      .attr('title', this.options.incTitle)
      .button({
        text: false,
        icons: {
          primary:this.options.incIcon
        }
      })
      .click(function() {
        $(this).parent().spinner('increase');
        return false;
      });
      // middle button
      var text = this.options.value;
      if (this.options.labelText != null) {
        text = this.options.labelText(this);
      }
      this.middleButton = $('<div class="spinner-button"></div>')
      .attr('title', this.options.middleTitle)
      .button({
        label : text
      })
      .click( function() {
        if (self.options.allowEdit === true) {
          var label = $(this).find('span');
          if (label.length !== 0) {
            var width = $(this).width();
            label.replaceWith(
              $('<input type="text">')
              .css('width', width)
              .data('before', label)          // save the old label so we can restore it
              .focusout( function() {
                $(this).replaceWith($(this).data('before'));
              })
              .keydown( function(event) {
                if (event.keyCode) {
                  if (event.keyCode == $.ui.keyCode.ESCAPE) {
                    $(this).replaceWith($(this).data('before'));
                  } else if (event.keyCode === $.ui.keyCode.ENTER) {
                    self.value($(this).val());
                  }
                }
              })
            );
            $(this).find('input').focus();
          }
        }
      });
        
      // create the component
      this.element
      .append(this.decButton)
      .append(this.middleButton)
      .append(this.incButton)
      .buttonset();
    },

    _setOption : function(key, value) {
      if (key === 'value') {
        // illegal (non-number inputs) yields setting the old value
        if (this._invalidNumber(value)) {
          value = this.options.value;
        }
        value = value - 0;    // force value to be a number
        // apply strict stepping if enabled
        if (this.options.strictStepping == true) {
          var modulo = value % this.options.stepWidth;
          value = (value - modulo);
        }
        // apply limits if defined
        if (this.options.minValue != null) {
          value = Math.max(this.options.minValue, value);
          if (value == this.options.minValue) {
            this._trigger("minValueReached");
          }
        }
        if (this.options.maxValue != null) {
          value = Math.min(this.options.maxValue, value);
          if (value == this.options.maxValue) {
            this._trigger("maxValueReached");
          }
        }
        // save value and update view
        var old = this.options.value;
        this.options.value = value;
        if (old != value) {
          this._refreshValue();
        }
      }
      $.Widget.prototype._setOption.apply( this, arguments );
    },

    // set spinner value
    value : function(newVal) {
      var edit = $(this.element).find('input');
      if (edit.length != 0) {
        edit.replaceWith(edit.data('before'));
      }
      this._setOption('value', newVal);
      return this;
    },

    // get spinner value
    _value : function() {
      return this.options.value;
    },

    _refreshValue : function() {
      var value = this._value();
      var text = value;

      // call labelText() function if defined
      if (this.options.labelText != null) {
        text = this.options.labelText(value);
      }
      $(this.middleButton).button('option', 'label', text);
      this._trigger('change', null, this);
    },

    increase : function() {
      var value = this._value();
      value = value + this.options.stepWidth;
      this._setOption('value', value);
      return this.value();
    },

    decrease : function() {
      var value = this._value();
      value = value - this.options.stepWidth;
      this._setOption('value', value);
      return this.value();
    },

    _invalidNumber : function(value) {
      return (value === undefined || value === '' || isNaN(value));
    }
  });
})(jQuery);