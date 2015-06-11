/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

var Videodisplay = Videodisplay || {};

/**
 * @namespace the global namespace Videodisplay
 */
Videodisplay = (function ()
{

  var b_Videodisplay_root,
  initD = false;
  var volSliderArgs,
  volPlayerArgs,
  closedCap = false,
  covOne,
  covTwo,
  strOne,
  strTwo,
  mimOne,
  mimTwo,
  plStyle,
  slideLen,
  capURL,
  vidSizeContLeft,
  vidSizeContRight,
  widthMediaOne,
  heightMediaOne,
  widthMediaTwo,
  heightMediaTwo,
  contLeft,
  bufferTime;

  /**
     * @memberOf Videodisplay
     * @description Initialize the "root" object. This represents the actual "Videodisplay mxml" flex application
     */
  function VideodisplayReady()
  {
    b_Videodisplay_root = FABridge['b_Videodisplay'].root().getFlexAjaxBridge();
    b_Videodisplay_root.onBridgeReady();
        
    initD = true;
    init();
  }

  /**
     * @memberOf Videodisplay
     * @description Returns a Flag that displays if Videdisplay has been initialized
     * @return true if Videodisplay has been initialized, false else
     */
  function initialized()
  {
    return initD;
  }

  /**
     * @memberOf Videodisplay
     * @description Initializez everything
     */
  function init()
  {
    if (volSliderArgs)
    {
      setVolumeSlider(volSliderArgs);
    }
    if (volPlayerArgs)
    {
      setVolumePlayer(volPlayerArgs);
    }
    if (closedCaptions)
    {
      closedCaptions();
    }
    if (covOne)
    {
      setMediaURL(covOne, covTwo, strOne, strTwo, mimOne, mimTwo, plStyle, slideLen, bufferTime);
    }
    if (capURL)
    {
      setCaptionsURL(capURL);
    }
    if (vidSizeContLeft)
    {
      VideoSizeControl(vidSizeContLeft, vidSizeContRight);
    }
    if (heightMediaOne)
    {
      setMediaResolution(widthMediaOne, heightMediaOne, widthMediaTwo, heightMediaTwo, contLeft);
    }
  }

  /**
     * @memberOf Videodisplay
     * @description play
     * @return false if something went wrong
     */
  function play()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.play();
	  v = b_Videodisplay_root.play();
        return v;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description stop
     * @return false if something went wrong
     */
  function stop()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.stop();
        return v;
      } 
    }
    catch (err)
    {
     $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description pause
     * @return false if something went wrong
     */
  function pause()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.pause();
        return v;
      } 
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description skipBackward
     * @return false if something went wrong
     */
  function skipBackward()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.skipBackward();
        return v;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description rewind
     * @return false if something went wrong
     */
  function rewind()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.rewind();
        return v;
      } 
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description stopRewind
     * @return false if something went wrong
     */
  function stopRewind()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.stopRewind();
        return v;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description fastForward
     * @return false if something went wrong
     */
  function fastForward()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.fastForward();
        return v;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description stopFastForward
     * @return false if something went wrong
     */
  function stopFastForward()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.stopFastForward();
        return v;
      } 
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description skipForward
     * @return false if something went wrong
     */
  function skipForward()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.skipForward();
        return v;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description passCharCode
     * @param argInt
     * @return false if something went wrong
     */
  function passCharCode(argInt)
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.passCharCode(argInt);
        return v;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };
    
  /**
     * @memberOf Videodisplay
     * @description seek
     *              Note: pause()/resume() bug red5
     * @param argNumber
     * @return false if seek failed
     */
  function seek(argNumber)
  {
    try
    {
      if (initialized())
      {
        var progress = Opencast.engage.getLoadProgress();
        // streaming
        if (progress === -1)
        {
          // red5 pause/resume bug
          if (Opencast.Player.isPlaying())
          {
            var v = b_Videodisplay_root.seek(argNumber);
            return v;
          }
          else
          {
            // player in pause mode
            b_Videodisplay_root.play();
            var v = b_Videodisplay_root.seek(argNumber);
            return v;
          }
        }
        // progressive download
        else
        {
          var seekValue = Math.min(argNumber, progress);
          var v = b_Videodisplay_root.seek(seekValue);
          return v;
        }
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description mute
     * @return false if something went wrong
     */
  function mute()
  {
    try
    {
      if (initialized())
      {
        var v =  b_Videodisplay_root.mute();
        return v;
      } 
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description setVolumeSlider
     * @param argNumber
     * @return false if something went wrong
     */
  function setVolumeSlider(argNumber)
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.setVolumeSlider(argNumber);
        return v;
      }
      else
      {
        volSliderArgs = argNumber;
      } 
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description setVolumePlayer
     * @param argNumber
     * @return false if something went wrong
     */
  function setVolumePlayer(argNumber)
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.setVolumePlayer(argNumber);
        Opencast.Player.addEvent(Opencast.logging.SET_VOLUME + argNumber);
        return v;
      }
      else
      {
        volPlayerArgs = argNumber;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description closedCaptions
     * @param argNumber
     * @return false if something went wrong
     */
  function closedCaptions()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.closedCaptions();
        return v;
      }
      else
      {
        closedCap = true;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description setMediaURL
     * @param argCoverOne
     * @param argCoverTwo
     * @param argStringOne
     * @param argStringTwo
     * @param argMimetypeOne
     * @param argMimetypeTwo
     * @param argPlayerstyle
     * @param slideLength
     * @param bufferTime
     * @return false if something went wrong
     */
  function setMediaURL(argCoverOne, argCoverTwo, argStringOne, argStringTwo, argMimetypeOne, argMimetypeTwo, argPlayerstyle, slideLength, argBufferTime)
  {
    if(argMimetypeOne == "")
    {
      argMimetypeOne = "video/flv";
    }

    $.log("-----");
    $.log("Videodisplay data");
    $.log("argCoverOne: " + argCoverOne);
    $.log("argCoverTwo: " + argCoverTwo);
    $.log("argStringOne: " + argStringOne);
    $.log("argStringTwo: " + argStringTwo);
    $.log("argMimetypeOne: " + argMimetypeOne);
    $.log("argMimetypeTwo: " + argMimetypeTwo);
    $.log("argPlayerstyle: " + argPlayerstyle);
    $.log("slideLength: " + slideLength);
    $.log("BufferTime: " + argBufferTime);
    $.log("-----");

    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.setMediaURL(argCoverOne, argCoverTwo, argStringOne, argStringTwo, argMimetypeOne, argMimetypeTwo, argPlayerstyle, slideLength, argBufferTime);
        return v;
      }
      else
      {
        covOne = argCoverOne;
        covTwo = argCoverTwo;
        strOne = argStringOne;
        strTwo = argStringTwo;
        mimOne = argMimetypeOne;
        mimTwo = argMimetypeTwo;
        plStyle = argPlayerstyle;
        slideLen = slideLength;
        bufferTime = argBufferTime;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description setCaptionsURL
     * @param argString
     * @return false if something went wrong
     */
  function setCaptionsURL(argString)
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.setCaptionsURL(argString);
        return v;
      }
      else
      {
        capURL = argString;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description videoSizeControl
     * @param argSizeLeft
     * @param argSizeRight
     * @return false if something went wrong
     */
  function videoSizeControl(argSizeLeft, argSizeRight)
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.videoSizeControl(argSizeLeft, argSizeRight);
        return v;
      }
      else
      {
        vidSizeContLeft = argSizeLeft;
        vidSizeContRight = argSizeRight;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description getViewState
     * @return false if something went wrong
     */
  function getViewState()
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.getViewState();
        return v;
      } 
    } catch(err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };

  /**
     * @memberOf Videodisplay
     * @description getViewState
     * @param argWidthMediaOne
     * @param argHeightMediaOne
     * @param argWidthMediaTwo
     * @param argHeightMediaTwo
     * @param argMultiMediaContainerLeft
     * @return false if something went wrong
     */
  function setMediaResolution(argWidthMediaOne, argHeightMediaOne, argWidthMediaTwo, argHeightMediaTwo, argMultiMediaContainerLeft)
  {
    try
    {
      if (initialized())
      {
        var v = b_Videodisplay_root.setMediaResolution(argWidthMediaOne, argHeightMediaOne, argWidthMediaTwo, argHeightMediaTwo, argMultiMediaContainerLeft);
        return v;
      }
      else
      {
        widthMediaOne = argWidthMediaOne;
        heightMediaOne = argHeightMediaOne;
        widthMediaTwo = argWidthMediaTwo;
        heightMediaTwo = argHeightMediaTwo;
        contLeft = argMultiMediaContainerLeft;
      }
    }
    catch (err)
    {
      $.log("Error in Videodisplay '" + arguments.callee.toString().substr(0, arguments.callee.toString().indexOf('(')) + "': " + err);
    }
    return false;
  };
    
  return {
    VideodisplayReady: VideodisplayReady,
    play: play,
    stop: stop,
    pause: pause,
    skipBackward: skipBackward,
    rewind: rewind,
    stopRewind: stopRewind,
    fastForward: fastForward,
    stopFastForward: stopFastForward,
    skipForward: skipForward,
    passCharCode: passCharCode,
    seek: seek,
    mute: mute,
    setVolumeSlider: setVolumeSlider,
    setVolumePlayer: setVolumePlayer,
    closedCaptions: closedCaptions,
    setMediaURL: setMediaURL,
    setCaptionsURL: setCaptionsURL,
    videoSizeControl: videoSizeControl,
    getViewState: getViewState,
    setMediaResolution: setMediaResolution
  };
}());

// Listen for the instantiation of the Flex application over the bridge
FABridge.addInitializationCallback("b_Videodisplay", Videodisplay.VideodisplayReady);
