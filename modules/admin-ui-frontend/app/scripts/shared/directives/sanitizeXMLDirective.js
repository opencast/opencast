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
 * @ngdoc directive
 * @name adminNg.modal.sanitizeXml
 * @description
 * Removes illegal XML characters from the input string.
 *
 * Illegal characters are all characters not in the following list:
 * x09: Character Tabulation
 * x0A: Line Feed
 * x0D: Carriage Return
 * x20-xD7FF
 * uE000-uFFFD
 * x10000-x10FFFF
 *
 * Usage: Set this directive in the input element that needs to be sanitized.
 *
 * @example
 * <input sanitize-xml="user.username"/>
 */
angular.module('adminNg.directives')
.directive('sanitizeXml', function () {
  return {
    require: 'ngModel',
    link: function (scope, elem, attrs, ctrl) {
      scope.unregisterWatch = scope.$watch(attrs.sanitizeXml, function (input) {
        // Regexp matches illegal characters.
        // Unfortunately javascript does not play nice with the higher planes of unicode (x10000-x10FFFF).
        // With ES6 we might be able to switch to a nicer expression like
        // /([^\x09\x0A\x0D\x20-\uD7FF\uE000-\uFFFC\u{10000}-\u{10FFFF}])/ug
        // eslint-disable-next-line no-control-regex
        var NOT_SAFE_IN_XML_1_0 = new RegExp('((?:[\0-\x08\x0B\f\x0E-\x1F\uFFFD\uFFFE\uFFFF]|'
          + '[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?:[^\uD800-\uDBFF]|^)[\uDC00-\uDFFF]))', 'g');

        if (typeof input === 'string') {
          ctrl.$setViewValue(input.replace(NOT_SAFE_IN_XML_1_0, ''));
          ctrl.$render();
        }
      });

      scope.$on('$destroy', function () {
        scope.unregisterWatch();
      });
    }
  };
});
