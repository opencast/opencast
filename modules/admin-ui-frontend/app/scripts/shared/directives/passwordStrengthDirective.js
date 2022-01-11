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

/**
 * @ngdoc directive
 * @description
 * Checks the strength of a password.
 *
 * @example
 * <pw-strength ng-model="model.password"></pw-strength>
 */

'use strict';

angular.module('adminNg.directives')
    .directive('pwStrength', [function () {
        return {
            restrict: 'E',
            scope: {
                password: '=ngModel'
            },

            link: function (scope, elem, attrs, ctrl) {
                scope.$watch('password', function (pw) {

                    scope.strength =
                        isValid(pw && /[A-Z]/.test(pw)) +
                        isValid(pw && /[a-z]/.test(pw)) +
                        isValid(pw && /\d/.test(pw)) +
                        isValid(pw && /(?=.*\W)/.test(pw)) +
                        isValid(pw && /^.{8,}$/.test(pw));

                    var bar = "background:white; width:0%;"
                    setColor(scope.strength);

                    function isValid(rule) {
                        return rule ? 1 : 0;
                    }

                    function setColor(validRules) {
                        if (validRules == 1) {
                            bar = "background:red; width:20%;"
                        }
                        else if (validRules == 2) {
                            bar = "background:orange; width:40%;"
                        }
                        else if (validRules == 3) {
                            bar = "background:yellow; width:60%;"
                        }
                        else if (validRules == 4) {
                            bar = "background:green; width:80%;"
                        }
                        else if (validRules == 5) {
                            bar = "background:darkgreen; width:100%;"
                        }
                        document.getElementById('bar').style = bar;
                    }
                });

            },
            template: '<div class="progress pw-strength">' +
                '<div id="bar" class="progress-bar"></div>' +
                '</div>'
        }
    }
    ]);