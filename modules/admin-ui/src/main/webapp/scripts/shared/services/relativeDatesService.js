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

angular.module('adminNg.services')
.factory('RelativeDatesService', function () {

    var RelativeDatesService = function () {

        // keep the english parsing since the configuration file is in english, but change first day of week and
        // first week of year according to the user's locale if possible, fall back to iso definition otherwise
        this.updateDefaultLocale = function () {

            var defaultLocale = Sugar.Date.getLocale() //default is en-us

            // fall back to ISO definition:
            // first day of week = Monday (1)
            // first week of year: First week with the majority (4) of its days in January
            delete defaultLocale.firstDayOfWeek;
            delete defaultLocale.firstDayOfWeekYear;
            defaultLocale.code = defaultLocale.code + "-mod"

            var userLanguage = navigator.language;

            if (typeof userLanguage !== "undefined") {

                if (userLanguage.indexOf("-") !== -1) {
                    userLanguage = userLanguage.split("-")[0];
                }

                var userLocale = Sugar.Date.getLocale(userLanguage);

                if (typeof userLocale !== "undefined") {

                    if ("firstDayOfWeek" in userLocale) {
                        defaultLocale.firstDayOfWeek = userLocale.firstDayOfWeek;
                    }

                    if ("firstDayOfWeekYear" in userLocale) {
                        defaultLocale.firstDayOfWeekYear = userLocale.firstDayOfWeekYear;
                    }
                }
            }
        }

        this.relativeToAbsoluteDate = function(relativeDate) {

            var absoluteDate = Sugar.Date.create(relativeDate);
            return absoluteDate;
        };

        this.relativeDateSpanToFilterValue = function(relativeDateFrom, relativeDateTo) {

            var fromAbsoluteDate = this.relativeToAbsoluteDate(relativeDateFrom);
            var toAbsoluteDate = this.relativeToAbsoluteDate(relativeDateTo);

            return fromAbsoluteDate.toISOString() + "/" + toAbsoluteDate.toISOString();
        };

        this.updateDefaultLocale();
    };

    return new RelativeDatesService();

});
