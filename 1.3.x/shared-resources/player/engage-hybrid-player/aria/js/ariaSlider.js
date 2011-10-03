/*global $, ariaSlider, Opencast, window, Videodisplay*/
/*jslint browser: true, white: true, undef: true, nomen: true, eqeqeq: true, plusplus: true, bitwise: true, newcap: true, immed: true, onevar: false */


/**
 * @namespace the global Opencast namespace ariaSlider
 *  
 * http://www.paciellogroup.com/blog/?p=68
 * http://accessify.com/tools-and-wizards/accessibility-tools/aria/slider-generator/
 */
Opencast.ariaSlider = (function () 
{
    var gDragging    = '',
        gDragOffset  = 0,
        sliderVolume = 'slider_volume_Thumb',
        sliderSeek   = 'slider_seek_Thumb',
        EMBED        = 'embed',
        ADVANCED     = 'advanced',
        playerView   = '',
        intval       = '',
        	newTarget = '';
    
    /**
        @memberOf Opencast.ariaSlider
        @description Get the element by id.
        @param String id 
    */
    function getElementId(id) 
    {
        return document.getElementById(id);
    }

    /**
       @memberOf Opencast.ariaSlider
       @description Get the ratio between the slider length and the slider's value maximum.
       @param Target target 
    */
    function calibrate(target) 
    {
        var rail = target.parentNode;
        var sliderLength = rail.clientWidth - target.clientWidth;
        var max = parseInt(target.getAttribute('aria-valuemax'));
        return sliderLength / max;
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Get the left offset of the rail, needed for conversion of mouse coordinates.
        @param Target elem 
    */
    function getHOffset(elem) 
    {
        var node = elem;
        var offset = node.offsetLeft;
        while (node.offsetParent) 
        {
            node = node.offsetParent;
            if (node.nodeName.toLowerCase() !== 'html') 
            {
                offset += node.offsetLeft;
            }
        }
        return offset;
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Get the HScroll Offset.
    */
    function getHScrollOffset() 
    {
        var scrollOffset;
        if (window.pageLeft !== undefined) 
        {
            scrollOffset = window.pageLeft;
        }
        else if (document.documentElement && document.documentElement.scrollLeft !== undefined) 
        {
            scrollOffset = document.documentElement.scrollLeft;
        } 
        else if (document.body.scrollLeft !== undefined) 
        {
            scrollOffset = document.body.scrollLeft;
        }
        return scrollOffset;
    }
    
    /**
        @memberOf Opencast.ariaSlider
        @description Change the slider attributes.
        @param Target target, Number value 
    */
    function changeValue(target, value) 
    {
        var ratio = calibrate(target);
        var min = parseFloat(target.getAttribute('aria-valuemin'));
        var max = parseFloat(target.getAttribute('aria-valuemax'));
        var newValue = Math.min(Math.max(value, min), max);
        var newPos = Math.round(newValue * ratio);
        
       
    	// if target is the volume slider
        if (target.id === sliderVolume && $("#oc_volume-menue").css("visibility") === 'visible' )
        {
        	target.style.left = newPos + 'px';
            target.setAttribute('aria-valuenow', newValue);
        	target.setAttribute('aria-valuetext', 'Volume: ' + Math.round(newValue) + '%');
        	$("#slider_volume_Rail").attr("title", 'Volume ' + Math.round(newValue) + '%');
            Opencast.Player.setPlayerVolume(newValue / 100);
        }
        else if( target.id === sliderVolume && $("#oc_volume-menue").css("visibility") === undefined )
        {
        	target.style.left = newPos + 'px';
            target.setAttribute('aria-valuenow', newValue);
        	target.setAttribute('aria-valuetext', 'Volume: ' + Math.round(newValue) + '%');
        	$("#slider_volume_Rail").attr("title", 'Volume ' + Math.round(newValue) + '%');
            Opencast.Player.setPlayerVolume(newValue / 100);
        }
    }
    
    /**
        @memberOf Opencast.ariaSlider
        @description Change the slider attributes from the videodisplay.
        @param Target target, Number value 
    */
    function changeValueFromVideodisplay(target, value) {
    	
    	var ratio = calibrate(target);
        var min = parseFloat(target.getAttribute('aria-valuemin'));
        var max = parseFloat(target.getAttribute('aria-valuemax'));
        var newValue = Math.min(Math.max(value, min), max);
        var newPos = Math.round(newValue * ratio);
        target.style.left = newPos + 'px';
        target.setAttribute('aria-valuenow', Math.round(newValue));
        
        // if target is the volume slider
        if (target.id === sliderVolume)
        {
        	
        	target.setAttribute('aria-valuetext', 'Volume: ' + Math.round(newValue) + '%');
            $("#slider_volume_Rail").attr("title", 'Volume ' + Math.round(newValue) + '%');
         }
        
    }
    
    /**
        @memberOf Opencast.ariaSlider
        @description Change the position of the slider.
        @param Target target, Number value 
    */
    function increment() 
    {
    	var newValue = parseFloat(newTarget.getAttribute('aria-valuenow')) + (false ? 10 : 1); 
        changeValue(newTarget, newValue);
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Change the position of the slider.
        @param Target target, Number value 
    */
    function decrement() 
    {
    	var newValue = parseFloat(newTarget.getAttribute('aria-valuenow')) - (false ? 10 : 1); 
        changeValue(newTarget, newValue);
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Key listener.
        @param Event event 
    */
    function handleKeyDown(event) {
        var event = event || window.event;
        var keyCode = event.keyCode || event.charCode;
        var target = event.target || event.srcElement; 
        newTarget = event.target || event.srcElement; 
        
        switch (keyCode) {
        case 37: // left arrow
            if (intval === "")
            {
                decrement(target, false);
                intval = window.setInterval("Opencast.ariaSlider.decrement()", 10);
            }
            break;
        case 39: //right arrow
            if (intval === "")
            {
            	increment(target, false);
                intval = window.setInterval("Opencast.ariaSlider.increment()", 10);
            }
            break;
        case 33: // page up
            increment(target, true);
            break;
        case 34: // page down
            decrement(target, true);
            break;
        case 36: // home
            changeValue(target, 0);
            break;		
        case 35: // end
            changeValue(target, 100);
            break;		
        case 27: // escape
            target.blur();
            break;
        default:
            passThrough = true;
            break;
        }
    }
    
    
    /**
    @memberOf Opencast.ariaSlider
    @description Key listener.
    @param Event event 
*/
function handlerKeyUp(event) {
    var event = event || window.event;
    var keyCode = event.keyCode || event.charCode;
    var target = event.target || event.srcElement; 
    
    switch (keyCode) {
    case 37: // left arrow
    	if (intval !== "")
        {
            window.clearInterval(intval);
            intval = "";
        }
        break;
    case 39: //right arrow
    	if (intval !== "")
        {
            window.clearInterval(intval);
            intval = "";
        }
        break;
    default:
        passThrough = true;
        break;
    }
}

    /**
        @memberOf Opencast.ariaSlider
        @description Change the position.
        @param Event event 
    */
    function handleRailMouseDown(event) 
    {
        event = event || window.event;
        var target = event.target || event.srcElement;
        var thumb = getElementId(target.id.replace(/Rail/, 'Thumb'));
        var newPos = event.clientX - getHOffset(target) + getHScrollOffset() - (thumb.clientWidth / 2);
        changeValue(thumb, mapPositionToValue(thumb, newPos));
        if (!document.activeElement || !document.activeElement !== thumb) 
        {
            thumb.focus();
        }
        return false;
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Change the position.
        @param Event event 
    */
    function handleThumbMouseDown(event) 
    {
        event = event || window.event;
        var target = event.target || event.srcElement;
        gDragging = target.id;
        gDragOffset = event.clientX - getHOffset(target.parentNode) - target.offsetLeft + getHScrollOffset();
        document.onmousemove = handleDrag;
        document.onmouseup = stopDrag;
        if (!document.activeElement || document.activeElement !== target) 
        {
            target.focus();
        }
        cancelEvent(event);
        return false;
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Drag handler, when the user drag the slider.
        @param Event event 
    */
    function handleDrag(event) {
        event = event || window.event;
        if (gDragging === '') 
        {
            return;
        }
        else 
        {
            var target = getElementId(gDragging);
            var newPos = event.clientX - getHOffset(target.parentNode) + getHScrollOffset() - gDragOffset;
            changeValue(target, mapPositionToValue(target, newPos));
        }    
    }

    
    /**
        @memberOf Opencast.ariaSlider
        @description Drop handler, when the user drop the slider.
        @param Event event 
    */
    this.stopDrag = function (event) 
    {
        gDragging = '';
        gDragOffset = 0;
        document.onmousemove = null;
        document.onmouseup = null;
    };

    /**
        @memberOf Opencast.ariaSlider
        @description Get the postion of the Slider.
        @param Target target, Number pos 
    */
    function mapPositionToValue(target, pos) {
        return Math.round(pos / calibrate(target));
    }
   
    /**
        @memberOf Opencast.ariaSlider
        @description Update the value Indicator.
        @param Target target, Number value 
    */
    function updateValueIndicator(id, value) 
    {
        var elem = getElementId(id);
        elem.replaceChild(document.createTextNode(value), elem.firstChild);
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Set the attributes of the volume slider.
       @param Element slider 
    */
    function setVolumeHandlers(slider) 
    {
        slider.setAttribute('aria-labelledby', 'volumeLabel');
        slider.setAttribute('aria-valuemin', '0');
        slider.setAttribute('aria-valuemax', '100');
        slider.setAttribute('aria-valuenow', '0');
        slider.setAttribute('aria-valuetext', '0');
        slider.setAttribute('role', 'slider');
        slider.parentNode.onmousedown = handleRailMouseDown;
        slider.onmousedown = handleThumbMouseDown;
        slider.onkeydown = handleKeyDown;
        slider.onkeyup = handlerKeyUp;
        slider.parentNode.onfocus = 
        function (event) { //temp IE fix
            event = event || window.event;
            var target = event.target || event.srcElement;
            var thumb = getElementId(target.id.replace(/Rail/, 'Thumb'));
            if (thumb)
            {
                thumb.focus();
            }
        };
    }
    
  

    /**
        @memberOf Opencast.ariaSlider
        @description Stop the active event.
        @param Event event 
    */
    function cancelEvent(event) 
    {
        if (typeof event.stopPropagation === 'function') 
        {
            event.stopPropagation();
        }
        else if (typeof event.cancelBubble !== 'undefined') 
        {
            event.cancelBubble = true;	
        }
        if (event.preventDefault) 
        {
            event.preventDefault();
        }
        return false;
    }

    /**
        @memberOf Opencast.ariaSlider
        @description Init the sliders.
    */
    function init(playerView) 
    {
        setVolumeHandlers(getElementId(sliderVolume));
    }

    return {
        getElementId: getElementId,
        calibrate : calibrate,
        getHOffset : getHOffset,
        getHScrollOffset : getHScrollOffset,
        handleKeyDown : handleKeyDown,
        handleRailMouseDown: handleRailMouseDown,
        handleThumbMouseDown: handleThumbMouseDown,
        handleDrag: handleDrag,
        stopDrag : stopDrag,
        mapPositionToValue : mapPositionToValue,
        increment: increment,
        decrement : decrement,
        changeValue: changeValue,
        changeValueFromVideodisplay : changeValueFromVideodisplay,
        updateValueIndicator: updateValueIndicator,
        setVolumeHandlers: setVolumeHandlers,
        cancelEvent : cancelEvent,
        init : init
      
    };
}());

