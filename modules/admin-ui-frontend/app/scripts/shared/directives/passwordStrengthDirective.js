/*
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
    .directive('pwStrength', ['$translate', function ($translate) {
      return {
        restrict: 'E',
        scope: {
          password: '=ngModel'
        },

        link: function (scope, elem, attrs, ctrl) {

          scope.$watch('password', function (pw) {

            scope.rule =
                requirements(pw && /[A-Z]/.test(pw)) +
                requirements(pw && /[a-z]/.test(pw)) +
                requirements(pw && /\d/.test(pw)) +
                requirements(pw && /\W/.test(pw)) +
                requirements(pw && /^.{8,}$/.test(pw));

            // bad passwords from https://en.wikipedia.org/wiki/List_of_the_most_common_passwords
            // plus Opencast's default password
            const bad_passwords = ['0', '111111', '1111111', '123', '123123', '123321',
              '1234', '12345', '123456', '1234567', '12345678', '123456789', '1234567890',
              '12345679', '123qwe', '18atcskd2w', '1q2w3e', '1q2w3e4r', '1q2w3e4r5t',
              '3rjs1la7qe', '555555', '654321', '666666', '7777777', '888888',
              '987654321', 'aa12345678', 'abc123', 'admin', 'dragon', 'Dragon', 'google',
              'iloveyou', 'Iloveyou', 'lovely', 'Monkey', 'mynoob', 'password',
              'password1', 'password12', 'password123', 'princess', 'qwerty', 'qwerty123', 'qwertyuiop',
              'Qwertyuiop', 'welcome', 'zxcvbnm', 'opencast' ];

            var bar_color = 'background:white;';
            var bar_text = '';
            var strength = calcStrength(scope.rule, pw);

            setProgBar(strength);

            function requirements(rule) {
              return rule ? 1 : 0;
            }

            function calcStrength(rule, pw){

              if (bad_passwords.indexOf(pw) > -1) {
                strength = 0;
                return strength;
              }
              // for every unused rule/requirement = -5 points
              var usedRules = (rule - 5) * 5;

              var uniqueChars = new Set(pw).size * 2;
              var pw_length = pw.length * 4;
              var lowerCase = (pw.length - pw.replace(/[a-z]/g, '').length) * 2;
              var upperCase = (pw.length - pw.replace(/[A-Z]/g, '').length) * 2;
              var number = (pw.length - pw.replace(/[0-9]/g, '').length) * 4;
              var symbol = (pw.length - pw.replace(/\W/g, '').length) * 6;

              var strength = Math.max(1, usedRules + uniqueChars + pw_length + lowerCase + upperCase + number + symbol);
              return strength;
            }

            function setProgBar(strength) {

              if (strength >= 90) {
                bar_color = 'background:#388ed6;';
                bar_text = 'USERS.USERS.DETAILS.STRENGTH.VERYSTRONG';
              }
              else if (strength >= 70) {
                bar_color = 'background:green;';
                bar_text = 'USERS.USERS.DETAILS.STRENGTH.STRONG';
              }
              else if (strength >= 50) {
                bar_color = 'background:gold;';
                bar_text = 'USERS.USERS.DETAILS.STRENGTH.GOOD';
              }
              else if (strength >= 30) {
                bar_color = 'background:darkorange;';
                bar_text = 'USERS.USERS.DETAILS.STRENGTH.WEAK';
              }
              else if (strength >= 2) {
                bar_color = 'background:red;';
                bar_text = 'USERS.USERS.DETAILS.STRENGTH.VERYWEAK';
              }
              else if (strength <= 1){
                strength = 0;
                bar_color = 'background:white;';
                bar_text = 'USERS.USERS.DETAILS.STRENGTH.BAD';
              }

              document.getElementById('bar').style = bar_color + 'width:' + strength + '%;';
              $translate(bar_text).then(function(translation) {
                document.getElementById('pw').innerText = translation;
              });
            }

          });

        },
        template:
                '<div class="progress pw-strength">' +
                    '<div id="bar" class="progress-bar"></div>' +
                '</div>' +
                '<label id="pw" style="text-align: left;">{{bar_text}}</label>'
      };
    }
    ]);
