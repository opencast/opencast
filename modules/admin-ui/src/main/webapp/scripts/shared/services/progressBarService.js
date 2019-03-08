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

'use strict';

/**
 * @ngdoc service
 * @name adminNg.modal ProgressBar
 * @description
 * Provides a service to show an "upload" progress bar for users to gauge the status
 *  of big file uploads from the browser.
 */

angular.module('adminNg.services')
.factory('ProgressBar',[ '$translate', 'cfpLoadingBar', function ($translate, cfpLoadingBar) {

  // One upload/progress bar at a time, vars global for the life of the upload
  var progressTextElement, // current reference to DOM element for manually updating progress percentage text
      progressTranslatedLabel, // current translated progress label (exists for life of current upload)
      progressLabel = 'EVENTS.EVENTS.STATUS.UPLOADING',
      progressTextElementSelector = '#loading-bar-text';

  /**
   * Calculate the current upload event progress and set it
   * in the cfpLoadingBar and the progressTextElement.
   * @param event
   */
  this.onUploadFileProgress = function (event) {

    // Get current progress ratio from the event and give it to the loading bar
    var ratioLoaded = event.loaded / event.total;
    cfpLoadingBar.set(ratioLoaded);

    // Get localized translation for "Uploading" to give the percentage text, and loading bar, context
    if (! progressTranslatedLabel) {
      progressTranslatedLabel = $translate.instant(progressLabel);
    }
    // Find the DOM progress text element the first time, reuse it on the next progress update
    // NOTE: cfpLoadingBar does not broadcast for when DOM elements are attached
    if (! progressTextElement) {
      progressTextElement = angular.element(document.querySelector(progressTextElementSelector));
    }

    // If progress text element is initialized, set the percentage progress text
    if (progressTextElement) {
      var uploadProgressText = progressTranslatedLabel + ': ' + (ratioLoaded * 100).toFixed(2) + '%';
      $(progressTextElement).text(uploadProgressText);
    }
  };

  /**
   * Complete the progress bar and re-init class vars
   */
  this.complete = function () {
    cfpLoadingBar.complete();
    progressTextElement = progressTranslatedLabel = false;
  };

  /**
   * Start loading bar. Wait for the start broadcast
   * before looking for progress text DOM element.
   */
  this.start = function () {
    cfpLoadingBar.start();
  };

  return {
    complete: this.complete,
    onUploadFileProgress: this.onUploadFileProgress,
    start: this.start
  };
}]);
