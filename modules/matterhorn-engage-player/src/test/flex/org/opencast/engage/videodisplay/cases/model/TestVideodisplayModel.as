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

package org.opencast.engage.videodisplay.cases.model
{
	import mx.collections.ArrayCollection;

	import org.flexunit.Assert;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.vo.LanguageVO;

	public class TestVideodisplayModel
	{
		private var videoDisplayModel:VideodisplayModel;

		[Before]
		public function setUp():void
		{
			this.videoDisplayModel=new VideodisplayModel();

		}

		[After]
		public function tearDown():void
		{
			this.videoDisplayModel=null;
		}

		[Test]
		public function testModelAttributes_currentDuration():void
		{
			var currentDuration:Number;
			this.videoDisplayModel.currentDuration=currentDuration;
			Assert.assertEquals("VideodisplayModel needs a currentDuration object ", this.videoDisplayModel.currentDuration != 0, true);
		}

		[Test]
		public function testModelAttributes_currentPlayhead():void
		{
			var currentPlayhead:Number;
			this.videoDisplayModel.currentPlayhead=currentPlayhead;
			Assert.assertTrue("VideodisplayModel needs currentPlayhead attribute ", this.videoDisplayModel.currentPlayhead=currentPlayhead, true);
		}

		[Test]
		public function testModelAttributes_currentPlayerState():void
		{
			var currentPlayerState:String;
			this.videoDisplayModel.currentPlayerState=currentPlayerState;
			Assert.assertTrue("VideodisplayModel needs currentPlayerState attribute ", this.videoDisplayModel.currentPlayerState=currentPlayerState, true);
		}

		[Test]
		public function testModelAttributes_currentCaptionSet():void
		{
			var currentCaptionSet:Array;
			this.videoDisplayModel.currentCaptionSet=currentCaptionSet;
			Assert.assertTrue("VideodisplayModel needs currentCaptionSet ", this.videoDisplayModel.currentCaptionSet=currentCaptionSet, true);
		}

		[Test]
		public function testModelAttributes_oldSubtitle():void
		{
			var oldSubtitle:String='';
			this.videoDisplayModel.oldSubtitle=oldSubtitle;
			Assert.assertEquals("VideodisplayModel needs a initial string ", this.videoDisplayModel.oldSubtitle == '', true);
		}

		[Test]
		public function testModelAttributes_fontSizeCaptions():void
		{
			var fontSizeCaptions:int=12;
			Assert.assertEquals("VideodisplayModel needs fontSize 16 for Captions ", this.videoDisplayModel.fontSizeCaptions == fontSizeCaptions, true);
		}

		[Test]
		public function testModelAttributes_captionsHeight():void
		{
			var captionsHeight:int=50;
			Assert.assertEquals("VideodisplayModel needs captionsHeight 50 for Captions ", this.videoDisplayModel.captionsHeight == captionsHeight, true);
		}

		[Test]
		public function testModelAttributes_captionSets():void
		{
			var captionSets:ArrayCollection;
			this.videoDisplayModel.captionSets=captionSets;
			Assert.assertTrue("VideodisplayModel needs captionSets Array ", this.videoDisplayModel.captionSets=captionSets, true);
		}

		[Test]
		public function testModelAttributes_languageComboBox():void
		{
			var languageComboBox:Array;
			this.videoDisplayModel.languageComboBox=languageComboBox;
			Assert.assertTrue("VideodisplayModel needs a languageComboBox Array ", this.videoDisplayModel.languageComboBox=languageComboBox, true);
		}

		[Test]
		public function testModelAttributes_languages():void
		{
			var languages:ArrayCollection;
			Assert.assertTrue("VideodisplayModel needs languageVO ", this.videoDisplayModel.languages=languages, true);
		}

	}
}