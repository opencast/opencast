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
package org.opencast.engage.videodisplay.control
{
	import mx.rpc.AsyncToken;
	import mx.rpc.http.HTTPService;
	import org.opencast.engage.videodisplay.control.command.ClosedCaptionsCommand;
	import org.opencast.engage.videodisplay.control.command.DisplayCaptionCommand;
	import org.opencast.engage.videodisplay.control.command.InitMediaPlayerCommand;
	import org.opencast.engage.videodisplay.control.command.ResizeVideodisplayCommand;
	import org.opencast.engage.videodisplay.control.command.SetCurrentCaptionsCommand;
	import org.opencast.engage.videodisplay.control.command.SetVolumeCommand;
	import org.opencast.engage.videodisplay.control.command.VideoControlCommand;
	import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
	import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
	import org.opencast.engage.videodisplay.control.event.InitMediaPlayerEvent;
	import org.opencast.engage.videodisplay.control.event.LoadDFXPXMLEvent;
	import org.opencast.engage.videodisplay.control.event.ResizeVideodisplayEvent;
	import org.opencast.engage.videodisplay.control.event.SetCurrentCaptionsEvent;
	import org.opencast.engage.videodisplay.control.event.SetVolumeEvent;
	import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.videodisplay.control.responder.LoadDFXPXMLResponder;
	import org.swizframework.Swiz;
	import org.swizframework.controller.AbstractController;
	public class VideodisplayController extends AbstractController
	{
		/**
		 * Constructor
		 * Add the listeners.
		 */
		public function VideodisplayController()
		{
			Swiz.addEventListener(LoadDFXPXMLEvent.EVENT_NAME, loadDFXPXML);
			Swiz.addEventListener(SetVolumeEvent.EVENT_NAME, setVolume);
			Swiz.addEventListener(VideoControlEvent.EVENT_NAME, videoControl);
			Swiz.addEventListener(DisplayCaptionEvent.EVENT_NAME, displayCaption);
			Swiz.addEventListener(ResizeVideodisplayEvent.EVENT_NAME, resizeVideodisplay);
			Swiz.addEventListener(SetCurrentCaptionsEvent.EVENT_NAME, setCurrentCaptions);
			Swiz.addEventListener(ClosedCaptionsEvent.EVENT_NAME, closedCaptions);
			Swiz.addEventListener(InitMediaPlayerEvent.EVENT_NAME, initMediaPlayer);
		}

		/**
		 * closedCaptions
		 * Create new ClosedCaptionsCommand and assign him the event.
		 * @eventType ClosedCaptionsEvent event
		 * */
		public function closedCaptions(event:ClosedCaptionsEvent):void
		{
			var closedCaptionsCommand:ClosedCaptionsCommand=new ClosedCaptionsCommand();
			closedCaptionsCommand.execute(event);
		}

		/**
		 * displayCaption
		 * Create new DisplayCaptionCommand and assign him the event.
		 * @eventType DisplayCaptionEvent event
		 * */
		public function displayCaption(event:DisplayCaptionEvent):void
		{
			var displayCaptionCommand:DisplayCaptionCommand=new DisplayCaptionCommand();
			displayCaptionCommand.execute(event);
		}


		/**
		 * initMediaPlayer
		 * Create new InitMediaPlayerCommand and assign him the event.
		 * @eventType InitMediaPlayerEvent event
		 * */
		public function initMediaPlayer(event:InitMediaPlayerEvent):void
		{
			var initMediaPlayerCommand:InitMediaPlayerCommand=new InitMediaPlayerCommand();
			initMediaPlayerCommand.execute(event);
		}

		/**
		 * loadDFXP.XML
		 * Create new LoadDFXPXMLResponder and new HTTPService, send the service to the AsyncToken.
		 * @eventType LoadDFXPXMLEvent event
		 * */
		public function loadDFXPXML(event:LoadDFXPXMLEvent):void
		{
			var responder:LoadDFXPXMLResponder=new LoadDFXPXMLResponder();
			var service:HTTPService=new HTTPService();
			service.resultFormat="e4x";
			service.url=event.source;
			var token:AsyncToken=service.send();
			token.addResponder(responder);
		}

		/**
		 * resizeVideodisplay
		 * Create new ResizeVideodisplayCommand and assign him the event.
		 * @eventType ResizeVideodisplayEvent event
		 * */
		public function resizeVideodisplay(event:ResizeVideodisplayEvent):void
		{
			var resizeVideodisplayCommand:ResizeVideodisplayCommand=new ResizeVideodisplayCommand();
			resizeVideodisplayCommand.execute(event);
		}

		/**
		 * setCurrentCaptions
		 * Create new SetCurrentCaptionsCommand and assign him the event.
		 * @eventType SetCurrentCaptionsEvent event
		 * */
		public function setCurrentCaptions(event:SetCurrentCaptionsEvent):void
		{
			var setCurrentCaptionsCommand:SetCurrentCaptionsCommand=new SetCurrentCaptionsCommand();
			setCurrentCaptionsCommand.execute(event);
		}

		/**
		 * setVolume
		 * Create new SetVolumeCommand and assign him the event.
		 * @eventType SetVolumeEvent event
		 * */
		public function setVolume(event:SetVolumeEvent):void
		{
			var setVolumeCommand:SetVolumeCommand=new SetVolumeCommand();
			setVolumeCommand.execute(event);
		}

		/**
		 * videoControl
		 * Create new VideoControlCommand and assign him the event.
		 * @eventType VideoControlEvent event
		 * */
		public function videoControl(event:VideoControlEvent):void
		{
			var videoControlCommand:VideoControlCommand=new VideoControlCommand();
			videoControlCommand.execute(event);
		}
	}
}


