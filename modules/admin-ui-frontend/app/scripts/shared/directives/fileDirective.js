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

angular.module('adminNg.directives')
.directive('adminNgFileUpload', ['$parse', function ($parse) {
  return {
    link: function (scope, element, attrs) {
      var model = $parse(attrs.file),
          modelSetter = model.assign;

      element.bind('change', function () {
        scope.$apply(function () {
          // allow multiple element files
          modelSetter(scope, element[0].files);
        });
      });

      scope.$on('$destroy', function () {
        element.unbind('change');
      });
    }
  };
}]);

