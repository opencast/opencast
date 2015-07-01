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
package org.opencast.engage.videodisplay.view
{
	import flash.display.Graphics;
	import flash.display.Sprite;
	import flash.events.Event;
	import flash.media.Sound;
	import flash.media.SoundChannel;
	import flash.media.SoundMixer;
	import flash.net.URLRequest;
	import flash.utils.ByteArray;
	public class SoundVisualizer extends Sprite
	{
		/**
		 * Constructor
		 * Add a event listener onEnterFrame.
		 */
		public function SoundVisualizer()
		{
			addEventListener(Event.ENTER_FRAME, onEnterFrame);
		}

		/**
		 * onEnterFrame
		 * Create the sound visualization.
		 * @eventType Event event
		 */
		private function onEnterFrame(event:Event):void
		{
			var bytes:ByteArray=new ByteArray();
			const PLOT_HEIGHT:int=80; //200
			const CHANNEL_LENGTH:int=256; //256
			SoundMixer.computeSpectrum(bytes, false, 0);

			var g:Graphics=this.graphics;

			g.clear();

			g.lineStyle(0, 0xFA6E23);
			g.beginFill(0xFA6E23);
			g.moveTo(0, PLOT_HEIGHT);

			var n:Number=0;

			for (var i:int=0; i < CHANNEL_LENGTH; i++)
			{
				n=(bytes.readFloat() * PLOT_HEIGHT);
				g.lineTo(i * 6, PLOT_HEIGHT - n);
			}

			g.lineTo(CHANNEL_LENGTH * 6, PLOT_HEIGHT);
			g.endFill();

			g.lineStyle(0, 0xFDB792);
			g.beginFill(0xFDB792, 0.5);
			g.moveTo(CHANNEL_LENGTH * 6, PLOT_HEIGHT);

			for (i=CHANNEL_LENGTH; i > 0; i--)
			{
				n=(bytes.readFloat() * PLOT_HEIGHT);
				g.lineTo(i * 6, PLOT_HEIGHT - n);
			}

			g.lineTo(0, PLOT_HEIGHT);
			g.endFill();
		}

		/**
		 * onPlaybackComplete
		 * Remove the listener onEnterFrame.
		 * @eventType Event event
		 */
		private function onPlaybackComplete(event:Event):void
		{
			removeEventListener(Event.ENTER_FRAME, onEnterFrame);
		}
	}
}

