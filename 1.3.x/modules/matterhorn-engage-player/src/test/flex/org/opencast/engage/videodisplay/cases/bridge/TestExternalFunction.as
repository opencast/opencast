/**
 *  Copyright 2009 The Regents of the University of California
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

package org.opencast.engage.videodisplay.cases.bridge
{
    import bridge.ExternalFunction;
    
    import org.flexunit.Assert;

    public class TestExternalFunction
    {
       
        private var externalFunction:ExternalFunction;

        [Before]
        public function setUp():void
        {
            this.externalFunction = new ExternalFunction();
        }

        [After]
        public function tearDown():void
        {
            this.externalFunction = null;
        }

        [Test]
        public function testExternalFunction():void
        {
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETDURATION == 'Opencast.Player.setDuration', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETTOTALTIME == 'Opencast.Player.setTotalTime', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETPROGRESS == 'Opencast.Player.setProgress', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETCURRENTTIME == 'Opencast.Player.setCurrentTime', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETPLAYHEAD == 'Opencast.Player.setPlayhead', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETPLAYHEADFULLSCREEN == 'Opencast.Player.setPlayheadFullscreen', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETVOLUME == 'Opencast.Player.setVolume', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETVOLUMESLIDER == 'Opencast.Player.setVolumeSlider', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETPLAYERVOLUME == 'Opencast.Player.setPlayerVolume', true );    
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETPLAYPAUSESTATE == 'Opencast.Player.setPlayPauseState', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETCAPTIONS == 'Opencast.Player.setCaptions', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.MUTE == 'Opencast.Player.doToggleMute', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.CURRENTTIME == 'Opencast.Player.currentTime', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.TOGGLESHORTCUTS == 'Opencast.Player.doToggleShortcuts', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.OPENCASTVOLUME == 'Opencast.Player.setOpencastVolume', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.PLAYPAUSE == 'Opencast.Player.doTogglePlayPause', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.DOSETVOLUME == 'Opencast.Player.doSetVolume', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.ONPLAYERREADY == 'Opencast.Watch.onPlayerReady', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.LOWSOUND == 'Opencast.Player.lowSound', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.NONESOUND == 'Opencast.Player.noneSound', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.HIGHSOUND == 'Opencast.Player.highSound', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.MUTESOUND == 'Opencast.Player.muteSound', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETCCICONON == 'Opencast.Player.setCCIconOn', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SETCCICONOFF == 'Opencast.Player.setCCIconOff', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SKIPBACKWARD == 'Opencast.Player.doSkipBackward', true );
            Assert.assertEquals( "externalFunction ", ExternalFunction.SKIPFORWARD == 'Opencast.Player.doSkipForward', true );         
        }
        
      }
}