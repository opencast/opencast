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
var Opencast = Opencast || {
};
/**
 * @namespace the global Opencast namespace Initialize
 */
Opencast.Initialize = (function ()
{
    var VOLUME = 'volume',
        VIDEOSIZE = 'videosize',
        divId = '',
        timeout = 200, // http://javascript-array.com/scripts/jquery_simple_drop_down_menu/
        closetimer = 0,
        ddmenuitem = 0,
        keysSet = false,
        KEY_0,
        KEY_1,
        KEY_2,
        KEY_3,
        KEY_4,
        KEY_5,
        KEY_6,
        KEY_7,
        KEY_8,
        KEY_9,
        KEY_C,
        KEY_D,
        KEY_F,
        KEY_I,
        KEY_M,
        KEY_P,
        KEY_R,
        KEY_S,
        KEY_T,
        KEY_U,
        KEY_c,
        KEY_d,
        KEY_f,
        KEY_i,
        KEY_m,
        KEY_p,
        KEY_r,
        KEY_s,
        KEY_t,
        KEY_u;
    
    /**
     @memberOf Opencast.Initialize
     @description set the id of the div.
     */    
    function setDivId(id)
    {
        divId = id;
    }
    
    /**
     @memberOf Opencast.Initialize
     @description get the id of the div.
     */    
    function getDivId()
    {
        return divId;
    }
    
    /**
     @memberOf Opencast.Initialize
     @description sets the keys to its ascii characters
     */
    function setKeys()
    {
        if(!keysSet)
        {
            var asciiAlphabet = $.getAsciiAlphabet();
            KEY_0 = asciiAlphabet['0'],
            KEY_1 = asciiAlphabet['1'],
            KEY_2 = asciiAlphabet['2'],
            KEY_3 = asciiAlphabet['3'],
            KEY_4 = asciiAlphabet['4'],
            KEY_5 = asciiAlphabet['5'],
            KEY_6 = asciiAlphabet['6'],
            KEY_7 = asciiAlphabet['7'],
            KEY_8 = asciiAlphabet['8'],
            KEY_9 = asciiAlphabet['9'],
            KEY_C = asciiAlphabet['C'],
            KEY_D = asciiAlphabet['D'],
            KEY_F = asciiAlphabet['F'],
            KEY_I = asciiAlphabet['I'],
            KEY_M = asciiAlphabet['M'],
            KEY_P = asciiAlphabet['P'],
            KEY_R = asciiAlphabet['R'],
            KEY_S = asciiAlphabet['S'],
            KEY_T = asciiAlphabet['T'],
            KEY_U = asciiAlphabet['U'],
            KEY_c = asciiAlphabet['c'],
            KEY_d = asciiAlphabet['d'],
            KEY_f = asciiAlphabet['f'],
            KEY_i = asciiAlphabet['i'],
            KEY_m = asciiAlphabet['m'],
            KEY_p = asciiAlphabet['p'],
            KEY_r = asciiAlphabet['r'],
            KEY_s = asciiAlphabet['s'],
            KEY_t = asciiAlphabet['t'],
            KEY_u = asciiAlphabet['u'];
            keysSet = true;
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description Keylistener.
     */    
    function keyboardListener()
    {
        setKeys();
        $(document).keyup(function (event)
        {
            if (event.altKey === true && event.ctrlKey === true)
            {
                if (event.which === KEY_M ||
                    event.which === KEY_m)
                {
                    Opencast.Player.doToggleMute();
                }
                if (event.which === KEY_0 ||
                    event.which === KEY_0 ||
                    event.which === KEY_2 ||
                    event.which === KEY_3 ||
                    event.which === KEY_4 ||
                    event.which === KEY_5 ||
                    event.which === KEY_6 ||
                    event.which === KEY_7 ||
                    event.which === KEY_8 ||
                    event.which === KEY_9 ||
                    event.which === KEY_C ||
                    event.which === KEY_D ||
                    event.which === KEY_F ||
                    event.which === KEY_I ||
                    event.which === KEY_P ||
                    event.which === KEY_R ||
                    event.which === KEY_S ||
                    event.which === KEY_T ||
                    event.which === KEY_U ||
                    event.which === KEY_c ||
                    event.which === KEY_d ||
                    event.which === KEY_f ||
                    event.which === KEY_i ||
                    event.which === KEY_p ||
                    event.which === KEY_r ||
                    event.which === KEY_s ||
                    event.which === KEY_t ||
                    event.which === KEY_u)
                {
                    Videodisplay.passCharCode(event.which);
                }
                event.preventDefault();
            }
        });
    }
    
    /**
     @memberOf Opencast.Initialize
     @description close the drop dowan menue.
     */    
    function dropdown_close()
    {
        if (ddmenuitem)
        {
            ddmenuitem.css('visibility', 'hidden');
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description new timer.
     */    
    function dropdown_timer()
    {
        closetimer = window.setTimeout(dropdown_close, timeout);
    }
    
    /**
     @memberOf Opencast.Initialize
     @description cancel the timer.
     */    
    function dropdown_canceltimer()
    {
        if (closetimer)
        {
            window.clearTimeout(closetimer);
            closetimer = null;
        }
    }
    
    /**
     @memberOf Opencast.Initialize
     @description open the drop down menue.
     */    
    function dropdown_open()
    {
        dropdown_canceltimer();
        dropdown_close();
        if (getDivId() === VOLUME)
        {
            ddmenuitem = $('#oc_volume-menue').css('visibility', 'visible');
        }
        else if (getDivId() === VIDEOSIZE)
        {
            ddmenuitem = $('#oc_video-size-menue').css('visibility', 'visible');
        }
        else
        {
            ddmenuitem = $(this).find('ul').eq(0).css('visibility', 'visible');
        }
        setDivId('');
    }
    
    /**
     @memberOf Opencast.Initialize
     @description open the drop down meneue video.
     */    
    function dropdownVideo_open()
    {
        setDivId(VIDEOSIZE);
        dropdown_open();
    }
    
    /**
     * DOM ready
     */ 
    $(document).ready(function ()
    {
        keyboardListener();
        $('#oc_video-size-dropdown > li').bind('mouseover', dropdown_open);
        //$('#oc_video-size-dropdown > li').bind('click', dropdown_open);
        $('#oc_video-size-dropdown > li').bind('mouseout', dropdown_timer);
        // Handler focus
        $('#oc_btn-dropdown').focus(function ()
        {
            setDivId(VIDEOSIZE);
            dropdown_open();
        });
        // Handler blur
        $('#oc_btn-dropdown').blur(function ()
        {
            dropdown_timer();
        });
        $('#oc_volume-dropdown > li').bind('mouseover', dropdown_open);
        //$('#oc_video-size-dropdown > li').bind('click', dropdown_open);
        $('#oc_volume-dropdown > li').bind('mouseout', dropdown_timer);
        // Handler focus
        $('#oc_btn-volume').focus(function ()
        {
            setDivId(VOLUME);
            dropdown_open();
        })
        $('#slider_volume_Thumb').focus(function ()
        {
            setDivId(VOLUME);
            dropdown_open();
        })
        // Handler blur
        $('#oc_btn-volume').blur(function ()
        {
            dropdown_timer();
        })
        $('#slider_volume_Thumb').blur(function ()
        {
            dropdown_timer();
        })
        // init the aria slider for the volume
        Opencast.ariaSlider.init();
        // aria roles
        $("#editorContainer").attr("className", "oc_editTime");
        $("#editField").attr("className", "oc_editTime");
        $("#oc_btn-cc").attr('role', 'button');
        $("#oc_btn-cc").attr('aria-pressed', 'false');
        $("#oc_btn-volume").attr('role', 'button');
        $("#oc_btn-volume").attr('aria-pressed', 'false');
        $("#oc_btn-play-pause").attr('role', 'button');
        $("#oc_btn-play-pause").attr('aria-pressed', 'false');
        $("#oc_btn-skip-backward").attr('role', 'button');
        $("#oc_btn-skip-backward").attr('aria-labelledby', 'Skip Backward');
        $("#oc_btn-rewind").attr('role', 'button');
        $("#oc_btn-rewind").attr('aria-labelledby', 'Rewind: Control + Alt + R');
        $("#oc_btn-fast-forward").attr('role', 'button');
        $("#oc_btn-fast-forward").attr('aria-labelledby', 'Fast Forward: Control + Alt + F');
        $("#oc_btn-skip-forward").attr('role', 'button');
        $("#oc_btn-skip-forward").attr('aria-labelledby', 'Skip Forward');
        $("#oc_current-time").attr('role', 'timer');
        $("#oc_edit-time").attr('role', 'timer');
        $("#oc_btn-slides").attr('role', 'button');
        $("#oc_btn-slides").attr('aria-pressed', 'false');
        // Handler for .click()
        $('#oc_btn-skip-backward').click(function ()
        {
            Opencast.Player.doSkipBackward();
        });
        $('#oc_btn-play-pause').click(function ()
        {
            Opencast.Player.doTogglePlayPause();
        });
        $('#oc_btn-skip-forward').click(function ()
        {
            Opencast.Player.doSkipForward();
        });
        $('#oc_btn-volume').click(function ()
        {
            Opencast.Player.doToggleMute();
        });
        $('#oc_btn-cc').click(function ()
        {
            Opencast.Player.doToogleClosedCaptions();
        });
        $('#oc_current-time').click(function ()
        {
            Opencast.Player.showEditTime();
        });
        // Handler for .mouseover()
        $('#oc_btn-skip-backward').mouseover(function ()
        {
            this.className = 'oc_btn-skip-backward-over';
        });
        $('#oc_btn-rewind').mouseover(function ()
        {
            this.className = 'oc_btn-rewind-over';
        });
        $('#oc_btn-play-pause').mouseover(function ()
        {
            Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseover(function ()
        {
            this.className = 'oc_btn-fast-forward-over';
        });
        $('#oc_btn-skip-forward').mouseover(function ()
        {
            this.className = 'oc_btn-skip-forward-over';
        });
        $('#oc_btn-cc').mouseover(function ()
        {
            if (Opencast.Player.getCaptionsBool() === false)
            {
                this.className = 'oc_btn-cc-over';
            }
        });
        // Handler for .mouseout()
        $('#oc_btn-skip-backward').mouseout(function ()
        {
            this.className = 'oc_btn-skip-backward';
        });
        $('#oc_btn-rewind').mouseout(function ()
        {
            this.className = 'oc_btn-rewind';
        });
        $('#oc_btn-play-pause').mouseout(function ()
        {
            Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-fast-forward').mouseout(function ()
        {
            this.className = 'oc_btn-fast-forward';
        });
        $('#oc_btn-skip-forward').mouseout(function ()
        {
            this.className = 'oc_btn-skip-forward';
        });
        $('#oc_btn-cc').mouseout(function ()
        {
            if (Opencast.Player.getCaptionsBool() === false)
            {
                this.className = 'oc_btn-cc-off';
            }
        });
        // Handler for .mousedown()
        $('#oc_btn-skip-backward').mousedown(function ()
        {
            this.className = 'oc_btn-skip-backward-clicked';
        });
        $('#oc_btn-rewind').mousedown(function ()
        {
            this.className = 'oc_btn-rewind-clicked';
            Opencast.Player.doRewind();
        });
        $('#oc_btn-play-pause').mousedown(function ()
        {
            Opencast.Player.PlayPauseMouseOut();
        });
        $('#oc_btn-fast-forward').mousedown(function ()
        {
            this.className = 'oc_btn-fast-forward-clicked';
            Opencast.Player.doFastForward();
        });
        $('#oc_btn-skip-forward').mousedown(function ()
        {
            this.className = 'oc_btn-skip-forward-clicked';
        });
        // Handler for .mouseup()
        $('#oc_btn-skip-backward').mouseup(function ()
        {
            this.className = 'oc_btn-skip-backward-over';
        });
        $('#oc_btn-rewind').mouseup(function ()
        {
            this.className = 'oc_btn-rewind-over';
            Opencast.Player.stopRewind();
        });
        $('#oc_btn-play-pause').mouseup(function ()
        {
            Opencast.Player.PlayPauseMouseOver();
        });
        $('#oc_btn-fast-forward').mouseup(function ()
        {
            this.className = 'oc_btn-fast-forward-over';
            Opencast.Player.stopFastForward();
        });
        $('#oc_btn-skip-forward').mouseup(function ()
        {
            this.className = 'oc_btn-skip-forward-over';
        });
        // Handler onBlur
        $('#oc_edit-time').blur(function ()
        {
            Opencast.Player.hideEditTime();
        });
        // Handler keypress
        $('#oc_current-time').keypress(function (event)
        {
            if (event.keyCode === 13)
            {
                Opencast.Player.showEditTime();
            }
        });
        $('#oc_edit-time').keypress(function (event)
        {
            if (event.keyCode === 13)
            {
                Opencast.Player.editTime();
            }
        });
        // Handler keydown
        $('#oc_btn-rewind').keydown(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-rewind-clicked';
                Opencast.Player.doRewind();
            }
            else if (event.keyCode === 9)
            {
                this.className = 'oc_btn-rewind-over';
                Opencast.Player.stopRewind();
            }
        });
        $('#oc_btn-fast-forward').keydown(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-fast-forward-clicked';
                Opencast.Player.doFastForward();
            }
            else if (event.keyCode === 9)
            {
                this.className = 'oc_btn-fast-forward-over';
                Opencast.Player.stopFastForward();
            }
        });
        $('#oc_current-time').keydown(function (event)
        {
            if (event.keyCode === 37)
            {
                Opencast.Player.doRewind();
            }
            else if (event.keyCode === 39)
            {
                Opencast.Player.doFastForward();
            }
        });
        // Handler keyup
        $('#oc_btn-rewind').keyup(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-rewind-over';
                Opencast.Player.stopRewind();
            }
        });
        $('#oc_btn-fast-forward').keyup(function (event)
        {
            if (event.keyCode === 13 || event.keyCode === 32)
            {
                this.className = 'oc_btn-fast-forward-over';
                Opencast.Player.stopFastForward();
            }
        });
        $('#oc_current-time').keyup(function (event)
        {
            if (event.keyCode === 37)
            {
                Opencast.Player.stopRewind();
            }
            else if (event.keyCode === 39)
            {
                Opencast.Player.stopFastForward();
            }
        });
        // to calculate the embed flash height
        var iFrameHeight = document.documentElement.clientHeight;
        var otherDivHeight = 138;
        var flashHeight = iFrameHeight - otherDivHeight;
        $("#oc_flash-player").css('height', flashHeight + 'px');
        // to calculate the margin left of the video controls
        var marginleft = 0;
        controlsWidth = 165, flashWidth = document.documentElement.clientWidth;
        marginleft = Math.round((flashWidth * 0.4) - controlsWidth) / 2;
        $('.oc_btn-play').css("margin-left", marginleft + 'px');
        // create watch.html link
        var embedUrl = window.location.href;
        var advancedUrl = embedUrl.replace(/embed.html/g, "watch.html");
        $("a[href='#']").attr('href', '' + advancedUrl + '');
    });
    
    return {
        dropdownVideo_open: dropdownVideo_open,
        dropdown_timer: dropdown_timer
    };
}());
