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
 * Provides a service to show an "upload" progress status for users to see the progress
 *  of big file uploads from the browser.
 */

angular.module('adminNg.services')
.factory('ProgressBar', function () {

  // One upload notification message at a time, variables help track the life of the current session upload
  var progressTextId = 'new-event-upload-progress-text',
      progressNotificationParentQuery = '[data-message=\'NOTIFICATIONS.EVENTS_UPLOAD_STARTED\']',
      progressNotificationTemplate = '<p id="' + progressTextId + '"></p>',
      // The following must be reset on upload complete
      progressNotificationElement,
      progressLastRatio = 0,  // Track progress of first upload update and ignore others until current is uploaded
      progressCurrentTotal = 0; // Track current upload and ignore others until current is uploaded

  /**
   * Calculate the current upload event progress
   * @param event
   */
  this.onUploadFileProgress = function (event) {

    // init new upload tracking total
    if (progressCurrentTotal == 0) {
      progressCurrentTotal = event.total;
    } else if (event.total != progressCurrentTotal) {
      // do not track additional uploads until the current one finishes
      return;
    }

    // Get current progress ratio from the event
    var ratioLoaded = event.loaded / event.total;
    var ratioLoadedPercentText =  (ratioLoaded * 100).toFixed(2) + '%';

    if (progressLastRatio < ratioLoaded) {
      progressLastRatio = ratioLoaded;
    } else {
      // do not track newer uploads, of the same file size, until the current one finishes
      return;
    }

    if (! progressNotificationElement) {
      // Locate the upload notification element
      // Ignore this update when the upload notification element is not found
      var parentEl = angular.element.find(progressNotificationParentQuery);
      if (parentEl.length > 0) {
        // create a progress text element and update it
        parentEl[0].append(angular.element(progressNotificationTemplate)[0]);
        progressNotificationElement = document.getElementById(progressTextId);
        progressNotificationElement.innerText = ratioLoadedPercentText;
      }
    } else {
      // upate the existing progress text element
      progressNotificationElement.innerText = ratioLoadedPercentText;
    }
  };

  /**
   * Reset upload vars
   */
  this.reset = function () {
    progressLastRatio = progressCurrentTotal = 0;
    progressNotificationElement = undefined;
  };

  /**
   *  Helper for unit tests
   */
  this.getProgress = function () {
    return progressLastRatio;
  };

  return {
    reset: this.reset,
    onUploadFileProgress: this.onUploadFileProgress,
    getProgress: this.getProgress
  };
});
