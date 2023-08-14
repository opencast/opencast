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

angular.module('adminNg.filters')
    .filter('translateOverrideFallback', ['$filter', function ($filter) {

      // This Filter enables text override, translate, or a fallback display when no translation is availble
      // Used for customized asset upload options that do not have a built in translation (display fallback)
      // Or a translation that requires site customization (display override).
      //
      // "item" parameter is required, expected to have "title" attribute at minimum
      // "subtitle" parameter is optional
      // Example of title from an asset upload item: 'EVENTS.EVENTS.NEW.SOURCE.UPLOAD.FOOBAR'
      // Example of a subtitle from an assets upload option: 'SHORT' or 'DETAIL'
      // Example of filter use in template: {{item | translateOverrideFallback: 'DETAIL'}}
      // Example of a passed in item object:
      //   {
      //    "title": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.AUDIO_ONLY",
      //    "displayOverride.SHORT": "Audio *ONLY*",
      //    "displayOverride.DETAIL": "Don't upload anything but an audio file here!"
      //   }

      return function (item, subtitle) {
        var result = null;
        var sub = subtitle == null ? '' : '.' + subtitle;
        var translatable = item['title'] + sub;

        if (item['displayOverride' + sub]) {
          result = item['displayOverride' + sub];

        } else if ($filter('translate')(translatable) != translatable) {
          result = $filter('translate')(translatable);

        } else if (item['displayFallback' + sub]) {
          result = item['displayFallback' + sub];

        } else {
          // no translate, override, or fallback, use what is given
          result = translatable;
        }
        return result;
      };
    }]);

