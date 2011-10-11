/**
 *  Copyright 2009-2011 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace embedControlHide
 */
Opencast.embedControlHide = (function ()
{
    /**
     * Intervals used, bind waits for #oc_body to appear and binds key and mouse
     * actions, params checks width and heith of elements
     */ 
    var _intvalBind = '';
    var _intvalParams = '';
    /**
     * player control bar status, shown, hidden
     * 
     * @var  string
     */
    var _status = 'hidden';
    /**
     * player control bar height
     * 
     * @var  int
     */
    var _height = 0;
    /**
     * player advanced link width
     * 
     * @var  int
     */
    var _width = 0;
    /**
     * show/hide effect time in ms
     * 
     * @var int
     */
    var _time = 0;
    /**
     * Array of element ids to get height from if visible in control bar. If not
     * visible the elemts height is ignored
     *
     * var array
     */
    var _elements = ['#oc_controlbar-embed', '#oc_video-player-controls', '#analytics-and-annotations'];
    
    /**
     * initalize the effects
     * 
     * @param  int    the duration of the slide effect in ms
     */
    function initialize(time)
    { 
        $.log('Initializing');
        
        var controls = $.getURLParameter('controls');
        if((controls != null) && controls.toLowerCase() == "true")
        {
            $.log("Controls hiding disabled.");
            controls = true;
        } else
        {
            $.log("Controls hiding enabled.");
            controls = false;
        }
        _time = time | 500;
        
        //start width height listener and call it one time direct
        setParams();
        _intvalParams = setInterval('Opencast.embedControlHide.setParams()', 1000);
        
        //hide advanced Link
        var advHeight = $('#oc-link-advanced-player img').outerHeight();
        $('#oc_flash-player').css('marginTop', '-'+ (advHeight + 2)  +'px');
        doHideAdvLinkFast();
        
        if(!controls)
        {
            //start interval to bind key and mouse actions
            bindActions();
        } else
        {
            doShowFast();
        }
    }
    
    /**
     * Hide elements
     * 
     * @param  bool    true if elements should disappear instantly
     */
    function hide(fast) 
    {
        if(fast === true) {
            doHideFast();
        } else {
            doHideSlide();
        }
    }
    /**
     * Show elements
     * 
     * @param  bool    true if elements should appear instantly
     */
    function show(fast) 
    {
        if(fast === true) {
            doShowFast();
        } else {
            doShowSlide();
        }
    }
    /**
     * Hide elements fast
     */
    function doHideFast()
    {
        setStatus('hidden');
        if($('#oc_flash-player').is(':animated')) {
            $('#oc_flash-player').stop();
        }
        $('#oc_flash-player').css('marginBottom', '0px');
        doHideAdvLinkFast();
    }
    /**
     * Show elements fast
     */
    function doShowFast()
    {
        setStatus('shown');
        if($('#oc_flash-player').is(':animated')) {
            $('#oc_flash-player').stop();
        }
        $('#oc_flash-player').css('marginBottom', '-'+_height+'px');
        $('html').scrollTop(0);//prevent iframe scroll to focused element
        doShowAdvLinkFast();
    }
    /**
     * Hide link to advanced player fast
     */
    function doHideAdvLinkFast()
    {
        if($('#oc-link-advanced-player').is(':animated')) {
            $('#oc-link-advanced-player').stop();
        }
        $('#oc-link-advanced-player').css('marginLeft', '-'+_width+'px');
    }
    /**
     * Show link to advanced player fast
     */
    function doShowAdvLinkFast() 
    {
        if($('#oc-link-advanced-player').is(':animated')) {
            $('#oc-link-advanced-player').stop();
        }
        $('#oc-link-advanced-player').css('marginLeft', '0px');
    }
    /**
     * Hide elements with slide effect
     */
    function doHideSlide()
    {
        // no need to check status cause math always sets margin bottom to
        // completely hide the bar
        setStatus('hidden');
        var margin;
        
        //stop other slide effect
        if($('#oc_flash-player').is(':animated')) {
            $('#oc_flash-player').stop();
        }
        
        margin = '+='+ (-1 * parseInt($('#oc_flash-player').css('marginBottom'))).toString() +'px';
        
        $('#oc_flash-player').animate({
                marginBottom: margin
            }, {
                duration: _time,
                specialEasing: 'swing'
            });
       doHideAdvLinkSlide();
         
    }
    /**
     * Show elements with slide effect
     */
    function doShowSlide()
    {
        // check status if control bar is shown by key action do nothing
        if(getStatus() == 'shown') {
            return true;
        }
        setStatus('shown');
        var margin;
        
        //check if other slide effect is in action and set margin
        if($('#oc_flash-player').is(':animated')) {
            $('#oc_flash-player').stop();
            margin = '-='+ (_height + parseInt($('#oc_flash-player').css('marginBottom'))).toString() +'px';
        } else {
            margin = '-='+ _height.toString() +'px';
        }
        
        $('#oc_flash-player').animate({
                marginBottom:  margin
            }, {
                duration: _time,
                specialEasing: 'swing' 
            });
        doShowAdvLinkSlide();
    }
    /**
     * Hide link to advanced player with slide effect
     */
    function doHideAdvLinkSlide()
    {
        var margin;
        
        //check if other slide effect is in action and set margin
        if($('#oc-link-advanced-player').is(':animated')) {
            $('#oc-link-advanced-player').stop();
            margin = '-='+ (_width + parseInt($('#oc-link-advanced-player').css('marginLeft'))).toString() +'px';
        } else {
            margin = '-='+ _width.toString() +'px';
        }
        $('#oc-link-advanced-player').animate({
                marginLeft:  margin
            }, {
                duration: _time/2,
                specialEasing: 'swing' 
            });
    }
    /**
     * Show link to advanced palayer with slide effect
     */
    function doShowAdvLinkSlide()
    {
        var margin;
        
        //stop other slide effect
        if($('#oc-link-advanced-player').is(':animated')) {
            $('#oc-link-advanced-player').stop();
        }
        
        margin = '+='+ (-1 * parseInt($('#oc-link-advanced-player').css('marginLeft'))).toString() +'px';
        $('#oc-link-advanced-player').animate({
                marginLeft: margin
            }, {
                duration: _time/2,
                specialEasing: 'swing'
            });
    }
    /**
     * Set status
     */
    function setStatus(string)
    {
        status = string;
    }
    /**
     * Get status
     * 
     * @return string
     */
    function getStatus()
    {
        return status;
    }
    /**
     * set control bars height
     */
    function setHeight(height)
    {
        _height = height;
    }
    /**
     * set advanced player links width
     */
    function setWidth(width)
    {
        _width = width;
    }
    /**
     * Set width height params
     */
    function setParams()
    {
        //get advanced links width
        var width = $('#oc-link-advanced-player').outerWidth();
        Opencast.embedControlHide.setWidth(width);
        
        //get controll bars height
        var height = 0;
        
        for (var id in _elements) {
            if ($(_elements[id]).css('display') !== 'none') {
                height += $(_elements[id]).outerHeight();
            }
        }
        Opencast.embedControlHide.setHeight(height);
    }
    /**
     * Bind actions 
     */
    function bindActions()
    {
        //makes embedControlHide.initialize callable anytime
        if(_intvalBind === '') {
            _intvalBind = setInterval('Opencast.embedControlHide.doBindActions()', 100);
        }
        
    }
    function doBindActions()
    {
        if($('#oc_body')) {
            window.clearInterval(_intvalBind);
            // bind key actions
            $('#oc_body *').focus(function() {
                    show(true);
                }).blur(function() {
                    hide();
                });

            // bind controll bar toggle actions
            $('#oc_body').mouseenter(function() {
                    show();
                }).mouseleave(function() {
                    hide();
                });
        }
    }
    return {
        initialize: initialize,
        doBindActions: doBindActions,
        setParams: setParams,
        setWidth: setWidth,
        setHeight: setHeight
    }
}());
