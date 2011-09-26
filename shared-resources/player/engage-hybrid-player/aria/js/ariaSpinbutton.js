/**
 * @namespace the global Opencast namespace ariaSpinbutton
 *
 * Handles the spinbutton element and its sctions
 */
Opencast.ariaSpinbutton = (function ()
{
  /**
   * spinbutton wrapper
   */
  var parentEl;
  /**
   * for volum bar design element with empty bars
   * width is alwas 100%
   */
  var backEl;
  /**
   * element with selected bars, width depends on current step
   */
  var foreEl;
  /**
   * steps to split range
   */
  var countSteps = 0;
  /**
   * length of range
   */
  var range = 0;
  /**
   * maximum of range to select from
   */
  var rangeMaximum = 0;
  /**
   * minimum of range to select from
   */
  var rangeMinimum = 0;
  /**
   * current step
   */
  var currentPosition = 0;
  /**
   * each steps width on html in px
   */
  var visualStepWidth = 0.0;
  /**
   * each steps width for range
   */
  var rangeStepWidth = 0.0;
  
  /**
   * saves last volume before mute
   */
  var beforeMute = 0;
  /**
   * saves last volume before mute
   */
  var mute = false;
  /**
   * true saves/reads volume with cookie in browser
   */
  var withVolumeCookie = false;


  /**
   * Initalises the spinbutton for the given elements
   *
   * @param count     int     count of steps (positions)
   * @param minimum   mixed   minimum of range
   * @param maximum   mixed   maximum of range
   */

  function initialize(parentElementId, backgroundElementId, foregroundElementId, count, minimum, maximum, withVolumeCookie)
  {

    //set obj properties
    this.parentEl = $('#'+parentElementId);
    this.backEl = $('#'+backgroundElementId);
    this.foreEl = $('#'+foregroundElementId);
    this.countSteps = count;
    this.rangeMaximum = maximum;
    this.rangeMinimum = minimum;
    this.withVolumeCookie = !isNaN(withVolumeCookie); 
    this.doHover = true;

    //calculate width of each step in px
    var vwidth = (this.backEl.innerWidth() / count);
    this.visualStepWidth = vwidth;

    //calculate width of each step in range steps
    var rwidth = (this.range = maximum - minimum) / count;
    this.rangeStepWidth = rwidth;

    //jump to default position, read from cookie if set
    var defaultPosition = this.getCookie() ? this.getCookie() : count;
    this.jumpTo(defaultPosition);
    this.initalizeARIA();

    //initalise mouse possition tracking
    this.parentEl.mouseenter(function(e) {Opencast.ariaSpinbutton.handleMouseenterEvent(e);} );
    this.parentEl.mouseleave(function(e) {Opencast.ariaSpinbutton.handleMouseleaveEvent(e);} );
    this.parentEl.mousemove(function(e) {Opencast.ariaSpinbutton.handleMousemoveEvent(e);});
    this.parentEl.click(function(e) {Opencast.ariaSpinbutton.handleClickEvent(e);} );

    //initalise klick events
    this.parentEl.keydown(function(e) {Opencast.ariaSpinbutton.handleKeydownEvent(e);} );
    
    //hack: click event is handeld in flash Videodisplay.passCharCode(event.which);
    $(document).keydown(function (event)
    {
      if (event.altKey === true && event.ctrlKey === true)
      {
        switch(event.keyCode) {
        case 85:Opencast.ariaSpinbutton.increase();break;
        case 68:Opencast.ariaSpinbutton.decrease();break;
        }
      }
    });
  }
  
  /**
   * Jumps to Position if range value is given
   */
  function jumpToRange(number)
  {
    var step = Math.round(number / this.rangeStepWidth);
    this.jumpTo(step);
  }

  /**
   * only moves orange bars on mouseover
   */
  function jumpToVisualOnly(position)
  {
    var newWidth = Math.round(position * this.visualStepWidth);
    this.foreEl.css('width', newWidth+'px');
  }

  /**
   * jumps completly to position 
   * 
   * @param position int    one of the steps 
   */
  function jumpTo(position)
  {
    //mute is always false if volume jumps to any position
    this.mute = false;
    //check borders
    position = position > this.countSteps ? this.countSteps : position;
    position = position < 0 ? 0 : position;

    this.jumpToVisualOnly(position);
    this.currentPosition = position;
    this.updateARIA();
    this.setCookie();
    
    var newVal = (position * this.rangeStepWidth) / 100;
    //only set value if flex bridge is initalised
    if(typeof Videodisplay.setVolumePlayer == 'function') {
      Videodisplay.setVolumePlayer(newVal);
    }
  }

  /**
   * returns the position 
   */
  function getPosition(pageX)
  {
    var parentWidth = this.parentEl.width();
    var relOffset = pageX - this.parentEl.offset().left;
    var position = parseInt(relOffset / this.visualStepWidth) +1;
    return position;
  }

  function handleMouseenterEvent(event)
  {
    //hover always when accessing spinbutton
    this.doHover = true;
  }
  function handleMouseleaveEvent(event)
  {
    this.jumpToVisualOnly(this.currentPosition);
  }

  function handleMousemoveEvent(event)
  {
    if(this.doHover) {
      var position = this.getPosition(event.pageX);
      this.jumpToVisualOnly(position);
    }
  }

  function handleClickEvent(event)
  {
    //stop hover after clicked on step
    this.doHover = false;

    var position = this.getPosition(event.pageX);
    this.jumpTo(position);
  }

  function handleKeydownEvent(event)
  {
    switch(event.keyCode) {
      case 37:this.decrease();break;
      case 39:this.increase();break;
    }
  }

  function updateARIA()
  {
    var value = Math.round(this.currentPosition * this.rangeStepWidth);
    var currentAriaVal = this.parentEl.attr('aria-valuenow');
    if(currentAriaVal != value) {
      this.parentEl.attr('aria-valuenow', value);
    }
  }

  function initalizeARIA()
  {
    var value = Math.round(this.currentPosition * this.rangeStepWidth);
    this.parentEl.attr('role', 'slider');
    this.parentEl.attr('aria-valuemax', this.rangeMaximum);
    this.parentEl.attr('aria-valuemin', this.rangeMinimum);
  }

  function increase()
  {
    this.jumpTo(this.currentPosition + 1);
  }

  function decrease()
  {
    this.jumpTo(this.currentPosition - 1);
  }
  
  function toggleMute()
  {
    if(!this.mute) {
      this.beforeMute = this.currentPosition;
      this.jumpToVisualOnly(0);
      this.currentPosition = 0;
      this.mute = true;
    } else {
      if(this.beforeMute === 0) {
        this.jumpTo(this.countSteps);
      } else {
        this.jumpTo(this.beforeMute);
      }
    }
  }
  
  function setCookie()
  {
      if(this.withVolumeCookie) {
        $.cookie('public_player_volume', this.currentPosition, {expires: 365, path: '/'});
      }
      
  }
  function getCookie()
  {
      var cookieVal = $.cookie('public_player_volume');
      if(isNaN(parseInt(cookieVal)) || !this.withVolumeCookie) {
         $.cookie('public_player_volume', null);
         cookieVal = null;
      }
      return cookieVal;
  }      

  return {
    initialize : initialize,
    jumpTo : jumpTo,
    jumpToVisualOnly : jumpToVisualOnly,
    getPosition : getPosition,
    handleMouseenterEvent : handleMouseenterEvent,
    handleMouseleaveEvent : handleMouseleaveEvent,
    handleMousemoveEvent : handleMousemoveEvent,
    handleClickEvent : handleClickEvent,
    handleKeydownEvent : handleKeydownEvent,
    initalizeARIA : initalizeARIA,
    updateARIA : updateARIA,
    decrease : decrease,
    increase : increase,
    toggleMute : toggleMute,
    setCookie : setCookie,
    getCookie : getCookie
  };
 }());