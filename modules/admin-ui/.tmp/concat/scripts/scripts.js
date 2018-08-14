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

// File purpose: Declare app level module which depends on filters, and services

angular.module('LocalStorageModule').value('prefix', 'adminNg');
angular.module('adminNg', [
    'angularFileUpload',
    'pascalprecht.translate',
    'localytics.directives',
    'ui.sortable',
    'LocalStorageModule',
    'ngRoute',
    'cfp.hotkeys',
    'ngResource',
    'ngAnimate',
    'ngMessages',
    'angular-md5',
    'adminNg.controllers',
    'adminNg.services',
    'adminNg.filters',
    'adminNg.resources',
    'adminNg.services.language',
    'adminNg.services.table',
    'adminNg.services.modal',
    'adminNg.directives',
    'mgo-angular-wizard',
    'opencast.directives'
]).config(['$routeProvider', function ($routeProvider) {
    var firstCharToUpper = function (string) {
        return string.charAt(0).toUpperCase() + string.slice(1);
    };
    $routeProvider.when('/:category/:resource/:itemId/:subresource/:tab', {
        templateUrl: function (params) {
            return 'modules/' + params.resource + '/subresources/partials/' + params.subresource + '.html';
        },
        controller: ["$scope", "$routeParams", "$controller", function ($scope, $routeParams, $controller) {
            var capitalizedName = firstCharToUpper($routeParams.subresource);
            $controller(capitalizedName + 'Ctrl', {$scope: $scope});
        }]
    });
    $routeProvider.when('/:category/:resource', {
        templateUrl: function (params) {
            return 'modules/' + params.category + '/partials/index.html';
        },
        controller: ["$scope", "$routeParams", "$controller", function ($scope, $routeParams, $controller) {
            var capitalizedName = $routeParams.resource.charAt(0).toUpperCase() + $routeParams.resource.slice(1);
            $controller(capitalizedName + 'Ctrl', {$scope: $scope});
        }],
        reloadOnSearch: false
    });
    $routeProvider.otherwise({redirectTo: '/events/events'});
}])
.config(['LanguageProvider', '$translateProvider', function (LanguageProvider, $translateProvider) {
    LanguageProvider.setTranslateProvider($translateProvider);
}])
.config(['$translateProvider', function ($translateProvider) {
    var options = {
        'prefix': 'org/opencastproject/adminui/languages/lang-',
        'suffix': '.json'
    };
    $translateProvider.useLoader('customLanguageLoader', options);
    $translateProvider.preferredLanguage('en_US'); // This triggers the configuration process of our custom loader
    $translateProvider.useMissingTranslationHandler('customMissingTranslationHandler');
    $translateProvider.useSanitizeValueStrategy('escape');
}])
.config(['$logProvider', function ($logProvider) {
    $logProvider.debugEnabled(false);
}])
.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

    // Try to anticipate a session logout and reload if it applies. When reloading,
    // Spring Security will redirect to the login page.
    $httpProvider.interceptors.push(['$q', function ($q) {
        return {
            response: function (response) {
                try {
                    if (response.data.indexOf('login-container') >= 0) {
                        if (window.location.pathname.indexOf('login.html') === -1) {
                            window.location.reload();
                        }
                    }
                } catch (e) {}
                return response || $q.when(response);
            }
        };
    }]);
}])
.config(["hotkeysProvider", function(hotkeysProvider) {
    hotkeysProvider.includeCheatSheet = false;
}])
.config(['chosenProvider', function (chosenProvider) {
    chosenProvider.setOption({
        'search_contains': true,
    });
}])
.run(['$rootScope', function ($rootScope) {
    // Define wrappers around non-mockable native functions.
    $rootScope.location = {};
    $rootScope.location.reload = window.location.reload;

    $rootScope.toURL = function ( path ) {
        location.href = path;
    };
}]);

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

// Service modules registry
angular.module('adminNg.services', []);
angular.module('adminNg.services.language', []);
angular.module('adminNg.services.table', []);
angular.module('adminNg.services.stats', []);
angular.module('adminNg.services.modal', []);

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
 * @name adminNg.modal
 * @description
 * Provides a service for displaying details of table records.
 */
angular.module('adminNg.services')
    .factory('ResourceHelper',
    function () {

        /**
         * Returns input at it is. Can be used if no result transformation is necessary.
         * @param input
         * @returns {*}
         */
        var defaultMapper = function (input) {
            return input;
        };

        return {
            parseResponse: function (responseBody, mapper) {

                var effectiveMapper = mapper || defaultMapper;

                var result = [], data = {};

                try {
                    data = JSON.parse(responseBody);

                    // Distinguish between single and multiple values
                    if ($.isArray(data.results)) {
                        angular.forEach(data.results, function (event) {
                            result.push(effectiveMapper(event));
                        });
                    } else {
                        result.push(effectiveMapper(data.results));
                    }
                } catch (e) {
                    //console.warn(e);
                }

                // guard against empty dataset
                if (angular.isUndefined(data.results)) {
                    return;
                }


                return {
                    rows: result,
                    total: data.total,
                    offset: data.offset,
                    count: data.count,
                    limit: data.limit
                };
            }
        };
    });


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
.factory('AuthService', ['IdentityResource', function (IdentityResource) {
    var AuthService = function () {
        var me = this,
            isAdmin = false,
            isUserLoaded = false,
            callbacks = [],
            identity,
            userRole,
            isAuthorizedAs = function (role) {
                if (angular.isUndefined(me.user.roles)) {
                    return false;
                }
                return isAdmin ||
                    (angular.isArray(me.user.roles) && me.user.roles.indexOf(role) > -1) ||
                    me.user.roles === role;
            };

        this.user = {};

        this.loadUser = function () {
            identity = IdentityResource.get();
            identity.$promise.then(function (user) {
                // Users holding the global admin role shall always be authorized to do anything
                var globalAdminRole = "ROLE_ADMIN";
                me.user = user;
                isAdmin = angular.isDefined(globalAdminRole) && user.roles.indexOf(globalAdminRole) > -1;
                if (angular.isDefined(user.userRole)) {
                    userRole = user.userRole;
                }
                isUserLoaded = true;
                angular.forEach(callbacks, function (item) {
                    isAuthorizedAs(item.role) ? item.success() : item.error();
                });
            });
        };

        this.getUser = function () {
            return identity;
        };

        this.getUserRole = function () {
            return userRole;
        };

        this.userIsAuthorizedAs = function (role, success, error) {
            if (angular.isUndefined(success)) {
                return isAuthorizedAs(role);
            }

            if (isUserLoaded) {
                isAuthorizedAs(role) ? success() : error();
            } else {
                callbacks.push({
                    role    : role,
                    success : success,
                    error   : error
                });
            }
        };

        this.loadUser();
    };

    return new AuthService();
}]);

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
.factory('CryptService', ['md5', function (md5) {
    var CryptService = function () {
        this.createHashFromPasswortAndSalt = function (password, username) {
            return md5.createHash(password + '{' + username + '}');
        };
    };

    return new CryptService();
}]);

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

// Provides general utility functions
angular.module('adminNg.services')
.factory('JsHelper', [
    function () {
        return {
            getWeekDays: function () {
                return [
                    { key: 'MO', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.MO' },
                    { key: 'TU', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.TU' },
                    { key: 'WE', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.WE' },
                    { key: 'TH', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.TH' },
                    { key: 'FR', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.FR' },
                    { key: 'SA', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.SA' },
                    { key: 'SU', translation: 'EVENTS.EVENTS.NEW.WEEKDAYS.SU' }
                ];
            },

            map: function (array, key) {
                var i, result = [];
                for (i = 0; i < array.length; i++) {
                    result.push(array[i][key]);
                }
                return result;
            },

            /*
             * Creates an array containing the numberOfElements, starting from zero.
             */
            initArray: function (numberOfElements) {
                var i, result = [];
                for (i = 0; i < numberOfElements; i++) {
                    if (i < 10) {
                        result.push({
                            index: i,
                            value: '0' + i
                        });
                    }
                    else {
                        result.push({
                            index: i,
                            value: '' + i
                        });
                    }
                }
                return result;
            },

            /**
             * Finds nested properties in object literals in a graceful manner.
             * Usage: JsHelper.getNested(r, 'mediapackage', 'creators', 'creator');
             * @param r: The object which should contain the nested property.
             * all other params are the path to the wanted object.
             */
            getNested: function (targetObject, pathToProperty) {
                if (angular.isUndefined(targetObject)) {
                    throw 'illegal method call, I need at least two arguments';
                }
                var pathAsArray = pathToProperty.split('.'), i = 0, obj = targetObject;

                for (; i < pathAsArray.length; i++) {
                    if (!obj.hasOwnProperty(pathAsArray[i])) {
                        return null;
                    }
                    obj = obj[pathAsArray[i]];
                }
                return obj;
            },

            isEmpty: function (object) {
                var result = true;
                angular.forEach(object, function () {
                    result = false;
                    return;
                });
                return result;
            },

            /**
             * Checks if a potentially nested property exists in the targetObject.
             * Example: isPropertyDefined({inner: {property: 'prop'}}, 'inner.property') should return true.
             */
            isPropertyDefined: function (targetObject, pathToProperty) {
                var result = this.getNested(targetObject, pathToProperty);
                return (result !== null && angular.isDefined(result));
            },

            calculateStopTime: function (dateTimeString, duration) {
                if (!angular.isString(duration)) {
                    return '';
                }
                var d = new Date(dateTimeString),
                    unix = d.getTime(),
                    newUnix = unix + parseInt(duration, 10),
                    newDate = new Date(newUnix);
                return newDate.toISOString();
            },

            secondsToTime: function (duration) {
                var hours, minutes, seconds;
                hours   = Math.floor(duration / 3600);
                minutes = Math.floor((duration - (hours * 3600)) / 60);
                seconds = duration % 60;

                if (hours < 10)   { hours = '0' + hours; }
                if (minutes < 10) { minutes = '0' + minutes; }
                if (seconds < 10) { seconds = '0' + seconds; }

                return hours + ':' + minutes + ':' + seconds;
            },

            humanizeTime: function (hour, minute) {
                return moment().hour(hour).minute(minute).format('LT');
            },

            /**
             * Transform the UTC time string ('HH:mm') to a date object
             * @param  {String} utcTimeString the UTC time string
             * @return {Date}               the date object based on the string
             */
            parseUTCTimeString: function (utcTimeString) {
                    var dateUTC = new Date(0),
                        timeParts;

                    timeParts = utcTimeString.split(':');
                    if (timeParts.length === 2) {
                        dateUTC.setUTCHours(parseInt(timeParts[0]));
                        dateUTC.setUTCMinutes(parseInt(timeParts[1]));
                    }

                    return dateUTC;
            },

            /**
             * Converts obj to a Zulu Time String representation.
             *
             *  @param obj:
             *     { date: '2014-07-08',
             *       hour: '11',
             *       minute: '33'
             *     }
             *
             *  @param duration:
             *      Optionally, a duration { hour: '02', minute: '10' } can be added
             *      to the obj's time.
             */
            toZuluTimeString: function (obj, duration) {
                var momentDate,
                    dateParts = this.getDateParts(obj);
                    
                if (obj.hour) {
                    dateParts.hour = obj.hour;
                    dateParts.minute = obj.minute;
                }

                momentDate = moment(dateParts);

                if (duration) {
                    momentDate.add(parseInt(duration.hour, 10), 'h').add(parseInt(duration.minute, 10), 'm');
                }

                return momentDate.toISOString().replace('.000', '');
            },

            /**
             * Opposite as method toZuluTimeString
             * @param  {String} date as iso string
             * @return {object}      Date object with date, hour, minute:
             *                            { 
             *                              date: '2014-07-08',
             *                              hour: '11',
             *                              minute: '33'
             *                            }
             */
            zuluTimeToDateObject: function (date) {
                    var hour = date.getHours(),
                        minute = date.getMinutes();

                    return {
                        date: $.datepicker.formatDate('yy-mm-dd', date),
                        minute: minute.length === 1 ? '0' + minute : minute,
                        hour: hour.length === 1 ? '0' + hour : hour
                    };
            },

            /**
             * Splits a string or object with a date member into its year, month and day parts.
             * Months lose 1 to keep it consistent with Javascript date object.
             *
             *  @param obj:
             *     { date: '2015-01-02' } or "2015-01-02"
             */
            getDateParts: function(obj) {
                var dateStr = obj,
                    dateParts;

                if (angular.isDefined(obj.date) ) {
                    dateStr = obj.date;
                }

                dateParts = dateStr.split('-');

                return {
                    year  : parseInt(dateParts[0], 10),
                    month : parseInt(dateParts[1], 10) - 1,
                    day   : parseInt(dateParts[2], 10)
                };
            },

            /**
             * Checks a string to see if it is empty or undefined.
             */
            stringIsEmpty: function(str) {
                return (!str || (angular.isString(str) && 0 === str.length));
            },

            /**
             * Assembles an iCalendar RRULE (repetition instruction) for the
             * given user input.
             */
            assembleRrule: function (data) {
                var date,
                    weekdays = '',
                    weekdaysOffset = 0,
                    indexWeekdays = {},
                    weekdaysList = this.getWeekDays(),
                    dateParts = this.getDateParts(data.start);

                // Create a date object to translate it to UTC
                date = new Date(dateParts.year, dateParts.month, dateParts.day, data.start.hour, data.start.minute);

                if (data.weekdays) {
                    angular.forEach(weekdaysList, function(day, index) {
                        indexWeekdays[day.key] = index;
                    });

                    // Check if the weekdays need to be shifted because of timezone change
                    if (date.getUTCDate() > dateParts.day) {
                        weekdaysOffset = +1;
                    } else if (date.getUTCDate() < dateParts.day) {
                        weekdaysOffset = -1;
                    }

                    angular.forEach(data.weekdays, function (active, weekday) {
                        if (active) {
                            if (weekdays.length !== 0) {
                                weekdays += ',';
                            }
                            var idx = indexWeekdays[weekday.length > 2 ? weekday.substr(-2) : weekday] + weekdaysOffset;

                            // Check Sunday to Monday, Monday to Sunday
                            idx = (idx > 6 ? 0 : (idx < 0 ? 6 : idx));

                            weekdays += weekdaysList[idx].key.substr(-2);
                        }
                    });
                }

                return 'FREQ=WEEKLY;BYDAY=' + weekdays +
                        ';BYHOUR=' + date.getUTCHours() +
                        ';BYMINUTE=' + date.getUTCMinutes();
            },

            replaceBooleanStrings: function (metadata) {
                angular.forEach(metadata, function (md) {
                    angular.forEach(md.fields, function (field) {
                        if (field.type === 'boolean') {
                            if (field.value === 'true') {
                                field.value = true;
                            }
                            else if (field.value === 'false') {
                                field.value = false;
                            }
                            else {
                                throw 'unknown boolean value';
                            }
                        }
                    });
                });
            }
        };
    }
]);

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
 * @ngdoc overview
 * @name adminNg.Language
 *
 * @description
 * Provides functionality for the internationalization mechanism.
 */
angular.module('adminNg.services.language')
.provider('Language', function () {
    var me = this, Language;
    Language = function ($rootScope, $log, $translate, translateProvider, $http, $filter, $q, localStorageService) {
        var me = this;
        this.translateProvider = translateProvider;

        /**
        * @ngdoc function
        * @name Language.getLanguage
        * @description
        * Returns the currently set language.
        * @returns {string} The current language.
        */
        this.getLanguage = function () {
            return me.currentLanguage;
        };

        /**
        * @ngdoc function
        * @name Language.getLanguageCode
        * @description
        * Returns the currently set language code, chopping off the country if necessary.
        * @returns {string} The current language. ('en' if code is 'en_US')
        */
        this.getLanguageCode = function () {
            if (angular.isUndefined(me.currentLanguage)) {
                me.setLanguage();
                if (angular.isUndefined(me.currentLanguage)) {
                    $log.warn('No language set right now therefore can not get.');
                    //TODO Ensure that always a default language is set
                    return undefined;
                }
                return undefined;
            }

            return me.currentLanguage.code;
        };

        /**
        * @ngdoc function
        * @name Language.setLanguage
        * @description
        * Sets the language.
        * @param {language} The name of the language code, e.g. de_DE or de.
        */
        this.setLanguage = function (language) {
            me.currentLanguage = me.getAvailableLanguage(language);
        };

        /**
        * @ngdoc function
        * @name Language.getFallbackLanguage
        * @description
        * Returns the currently set language.
        * @returns {string} The current language.
        */
        this.getFallbackLanguage = function () {
            return me.fallbackLanguage;
        };

        /**
        * @ngdoc function
        * @name Language.setFallbackLanguage
        * @description
        * Sets the language.
        * @param {language} The name of the language code, e.g. de_DE or de.
        */
        this.setFallbackLanguage = function (language) {
            me.fallbackLanguage = language;
        };

        /**
        * @ngdoc function
        * @name Language.getAvailableLanguages
        * @description
        * Access to the currently configured translations.
        * @params {withCurrent} Define if the current language should be returned with (false by default).
        * @returns {string} All available language objects according to the current state of the server.
        */
        this.getAvailableLanguages = function (withCurrent) {
            if (withCurrent) {
                return me.availableLanguages;
            }


            var availables = [];

            angular.forEach(me.availableLanguages, function (lang) {
                if (lang.code !== me.getLanguage().code) {
                    availables.push(lang);
                }
            });

            return availables;
        };

        /**
        * @ngdoc function
        * @name Language.getAvailableLanguage
        * @description
        * Finds a previously registered language
        * @returns {string} The previously registered language object with code = language or undefined.
        */
        this.getAvailableLanguage = function (languageCode) {
            for (var l in me.availableLanguages) {
                if (me.availableLanguages[l].code === languageCode) {
                    return me.availableLanguages[l];
                }
            }
            return undefined;
        };

        /**
        * @ngdoc function
        * @name Language.getAvailableLanguageCodes
        * @description
        * Access to the currently available language codes.
        * @returns {Array.<string>} Array of available language codes according to the current state of the server.
        */
        this.getAvailableLanguageCodes = function () {
            var result = [];
            angular.forEach(me.availableLanguages, function (value) {
                result.push(value.code);
            }, this);
            return result;
        };

        /**
        * @ngdoc function
        * @name Language.setAvailableLanguages
        * @description
        * Setter for the available languages.
        * @param {Object.<string, string>} data Sets the available language data
        */
        this.setAvailableLanguages = function (data) {
            me.availableLanguages = data;
        };

        /**
        * @ngdoc function
        * @name Language.humanReadableDate
        * @description
        * Determines a string representation of the given date.
        * @param {dateString} Date String parsable by Date()
        * @returns {} An I18N key or an empty String
         */
        this.humanReadableDate = function (dateString) {
            var today     = new Date(),
                yesterday = new Date(),
                tomorrow  = new Date(),
                date      = new Date(dateString);

            yesterday.setDate(yesterday.getDate() - 1);
            tomorrow.setDate(tomorrow.getDate() + 1);

            // Zero out time component
            date.setHours(0, 0, 0, 0);
            today.setHours(0, 0, 0, 0);
            yesterday.setHours(0, 0, 0, 0);
            tomorrow.setHours(0, 0, 0, 0);

            switch (date.valueOf()) {
                case today.valueOf():
                    return 'TODAY';
                case tomorrow.valueOf():
                    return 'TOMORROW';
                case yesterday.valueOf():
                    return 'YESTERDAY';
                default:
                    return '';
            }
        };

        /**
        * @ngdoc function
        * @name Language.format
        * @description
        * Formats a time string in the defined style, depending on
        * the current language.
        * @param {style} Either short, medium or full
        * @param {dateTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized time for the given dateTimeString
        */
        this.format = function (style, string, format, humanReadableKey) {
            if (!angular.isDefined(string)) {
                return '';
            }

            if (humanReadableKey) {
                var humanReadableDate = this.humanReadableDate(string);
                if (humanReadableDate) {
                    return $filter('translate')(humanReadableKey + '.' + humanReadableDate, {
                        time: this.formatTime(style, string)
                    });
                }
            }

            return $filter('date')(string, me.currentLanguage.dateFormats[format][style]);
        };

        /**
        * @ngdoc function
        * @name Language.formatTime
        * @description
        * Formats a time string in the defined style, depending on
        * the current language.
        * @param {style} Either short, medium or full
        * @param {dateTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized time for the given dateTimeString
        */
        this.formatTime = function (style, dateTimeString) {
            return this.format(style, dateTimeString, 'time');
        };

        /**
        * @ngdoc function
        * @name Language.formatDate
        * @description
        * Formats a date string in the defined style, depending on the
        * current language. Special dates, like today, yesterday or
        * tomorrow will be rendered as a human readable string.
        * @param {style} Either short, medium or full
        * @param {dateTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized date for the given dateTimeString
        */
        this.formatDate = function (style, dateTimeString) {
            return this.format(style, dateTimeString, 'date', 'DATES');
        };

        /**
        * @ngdoc function
        * @name Language.formatDateRaw
        * @description
        * Formats a date string in the defined style, depending on the
        * current language.
        * @param {style} Either short, medium or full
        * @param {dateTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized date for the given dateTimeString
        */
        this.formatDateRaw = function (style, dateTimeString) {
            return this.format(style, dateTimeString, 'date');
        };

        /**
        * @ngdoc function
        * @name Language.formatDateTime
        * @description
        * Formats a date-time string in the defined style, depending on the
        * current language. Special dates, like today, yesterday or
        * tomorrow will be rendered as a human readable string.
        * @param {style} Either short, medium or full
        * @param {dateTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized date for the given dateTimeString
        */
        this.formatDateTime = function (style, dateTimeString) {
            return this.format(style, dateTimeString, 'dateTime', 'DATETIMES');
        };

        /**
        * @ngdoc function
        * @name Language.formatDateTimeRaw
        * @description
        * Formats a date-time string in the defined style, depending on the
        * current language.
        * @param {style} Either short, medium or full
        * @param {dateTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized date for the given dateTimeString
        */
        this.formatDateTimeRaw = function (style, dateTimeString) {
            return this.format(style, dateTimeString, 'dateTime');
        };

        /**
        * @ngdoc function
        * @name Language.toLocalTime
        * @description
        * Calculates the local time and formats it in the short format.
        * @param {zuluTimeString} The ISO formatted String to get the formatted time from.
        * @returns {string} The localized date for the given zuluTimeString
        */
        this.toLocalTime = function (zuluTimeString) {
        //    return $filter('date')(zuluTimeString, 'dd MMM yyyy, HH:mm');
            return $filter('date')(zuluTimeString, 'mediumDate');
        };

        /**
        * @ngdoc function
        * @name Language.formatDateRange
        * @description
        * Formats two dateTimeString separated by '/' in the defined style,
        * depending on the current language.
        *
        * @param {style} Either short, medium or full
        * @param {dateTimeStrings} The two ISO formatted Strings to get the formatted time from.
        * @returns {string} The localized date for the given dateTimeString
        */
        this.formatDateRange = function (style, dateTimeStrings) {
            if (!angular.isDefined(dateTimeStrings)) {
                return '';
            }
            var result = [];
            angular.forEach(dateTimeStrings.split('/'), function (date) {
                result.push($filter('date')(date, me.currentLanguage.dateFormats.date[style]));
            });
            return result.join(' - ');
        };

        /**
        * @ngdoc function
        * @name Language.loadLanguageFromServer
        * @description
        * Grabs the translation file from the server and registers it at angular-translate.
        *
        * @param {language} The language key
        * @param {deferred} The deferred object to which the result should be passed on
        * @param {translateProvider} The provider object for the translate service must be
        * passed in at least one time.
        */
        this.loadLanguageFromServer = function (language, deferred) {
            var translationPromise = $http({
                    method: 'GET',
                    url: 'public/org/opencastproject/adminui/languages/lang-' + language + '.json',
                    language: language
                });
            translationPromise.success(function (data, status, headers, config) {
                    me.translateProvider.translations(config.language, data);
                    data.code = config.language;
                    return deferred.resolve(data);
                })
                .error(function (data) {
                    $log.error('fatal, could not load translation');
                    return deferred.reject(data);
                });
            return deferred;
        };

        /**
        * @ngdoc function
        * @name Language.updateHttpLanguageHeader
        * @description
        * Update the default http header 'Accept-Language' with the current language and fallback language set.
        */
        this.updateHttpLanguageHeader = function () {
            $http.defaults.headers.common['Accept-Language'] = me.currentLanguage.code + ';q=1, ' + me.getFallbackLanguage().code + ';q=0.5';
        };

        this.changeLanguage = function (languageCode) {
            var deferred = $q.defer();
            me.loadLanguageFromServer(languageCode, deferred);
            deferred.promise.then(function () {
                $translate.use(languageCode);
                me.currentLanguage = me.getAvailableLanguage(languageCode);
                localStorageService.add('currentLanguage', languageCode);
                $rootScope.$emit('language-changed', languageCode);
                me.updateHttpLanguageHeader();
            });
        };

        this.setLanguages = function (data) {
            me.currentLanguage = data.bestLanguage;
            me.fallbackLanguage = data.fallbackLanguage;
            me.availableLanguages = data.availableLanguages;
            me.dateFormats = data.dateFormats;
        };

        /**
        * @ngdoc function
        * @name Language.configure
        *
        * @description
        * Configures this language service with the provided language service response
        *
        * @param {data} The response of the language service
        */
        this.configure = function (data, language) {
            me.fallbackLanguage = data.fallbackLanguage;
            me.availableLanguages = data.availableLanguages;
            me.dateFormats = data.dateFormats;
            me.currentLanguage = this.getAvailableLanguage(language) || data.bestLanguage;
            me.updateHttpLanguageHeader();
        };

        /**
        * @ngdoc function
        * @name Language.configureFromServer
        *
        * @description
        * Configures this language service with the currently available languages as well as
        * the best and fallback languages.
        *
        * @param {deferred} The deferred object to which the result should be passed on
        * @param {language} The language key
        */
        this.configureFromServer = function (deferred, language) {
            var me = this;
            $http({method: 'GET', url: '/i18n/languages.json'})
                .success(function (data) {
                    var fallbackDeferred = $q.defer();
                    me.configure(data, language);
                    // load the fallback language
                    if (me.currentLanguage.code !== me.fallbackLanguage.code) {
                        me.loadLanguageFromServer(data.fallbackLanguage.code, fallbackDeferred);
                        fallbackDeferred.promise.then(function (data) {
                            me.translateProvider.translations(me.fallbackLanguage.code, data);

                            // the fallback language has arrived now, lets register it
                            me.translateProvider.fallbackLanguage(me.fallbackLanguage.code);
                            // now we load the current translations
                            me.loadLanguageFromServer(me.currentLanguage.code, deferred);
                        });
                    }
                    else {
                        // There is no fallback language, lets load the main language
                        me.loadLanguageFromServer(me.currentLanguage.code, deferred);
                    }
                })
            .error(function (data) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
                $log.error('fatal, could not load the best language from server');
                return deferred.reject(data);
            });

            deferred.promise.then(function () {
                $rootScope.$emit('language-changed', me.currentLanguage.code);
            });
        };
    };

    this.setTranslateProvider = function (translateProvider) {
        me.translateProvider = translateProvider;
    };

    this.$get = ['$rootScope', '$log', '$translate',
        '$http', '$filter', '$q', 'localStorageService',
        function ($rootScope, $log, $translate, $http, $filter, $q, localStorageService) {
            return new Language($rootScope, $log, $translate, me.translateProvider, $http, $filter, $q, localStorageService);
        }
    ];
});

angular.module('adminNg.services.language')
.factory('customLanguageLoader', ['$http', '$q', 'Language', 'localStorageService', function ($http, $q, Language, localStorageService) {
    // return the function that will configure the translation service
    return function () {
        var deferred = $q.defer(), lastSavedLanguage = localStorageService.get('currentLanguage');
        Language.configureFromServer(deferred, lastSavedLanguage);
        return deferred.promise;
    };
}]);

angular.module('adminNg.services.language')
.factory('customMissingTranslationHandler', ['$log', function ($log) {
    return function (translationId) {
        if (angular.isDefined(translationId) && translationId !== '') {
            $log.debug('Missing translation: ' + translationId);
        }
    };
}]);

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
* A service to store arbitrary information without use of a backend.
*
* Information can either be stored in the localStorage or as a search
* parameter. Search parameters take precedence over localStorage
* values.
*
*/
angular.module('adminNg.services')
.factory('Storage', ['localStorageService', '$rootScope', '$location', function (localStorageService, $rootScope, $location) {
    var Storage = function () {
        var me = this;

        // Create a scope in order to broadcast changes.
        this.scope = $rootScope.$new();

        this.getFromStorage = function () {
            this.storage = angular.fromJson($location.search().storage) ||
                angular.fromJson(localStorageService.get('storage')) ||
                {};
        };

        this.get = function (type, namespace) {
            if (!me.storage[type]) {
                return {};
            }
            return me.storage[type][namespace] || {};
        };

        this.save = function () {
            var params = $location.search();
            params.storage = angular.toJson(me.storage);
            $location.search(params);
            localStorageService.add('storage', angular.toJson(me.storage));
        };

        this.put = function (type, namespace, key, value) {
            if (angular.isUndefined(me.storage[type])) {
                me.storage[type] = {};
            }

            if (angular.isUndefined(me.storage[type][namespace])) {
                me.storage[type][namespace] = {};
            }
            me.storage[type][namespace][key] = value;
            me.save();
            me.scope.$broadcast('change', type, namespace, key, value);
        };

        this.remove = function (type, namespace, key) {
            if (me.storage[type] && me.storage[type][namespace] && key) {
                delete me.storage[type][namespace][key];
            } else if (me.storage[type] && namespace) {
                delete me.storage[type][namespace];
            } else {
                delete me.storage[type];
            }

            me.save();
            me.scope.$broadcast('change', type, namespace, key);
        };

        this.replace = function (entries, type) {
            delete me.storage[type];
            // put for each entry
            angular.forEach(entries, function (entry) {
                if (angular.isUndefined(me.storage[type])) {
                    me.storage[type] = {};
                }
                if (angular.isUndefined(me.storage[type][entry.namespace])) {
                    me.storage[type][entry.namespace] = {};
                }
                me.storage[type][entry.namespace][entry.key] = entry.value;
            });

            // save and broadcast change
            me.save();
            me.scope.$broadcast('change', type);
        };

        this.getFromStorage();
    };
    return new Storage();
}]);

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
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying a modal filled with contents from a
 * partial.
 */
angular.module('adminNg.services.modal')
.factory('Modal', ['$http', '$compile', '$rootScope', '$location', '$timeout', 'Table', function ($http, $compile, $rootScope, $location, $timeout, Table) {
    var Modal = function () {
        var me = this;

        /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'shared/partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @returns {HttpPromise} a promise object for the template HTTP request
         */
        this.show = function (modalId) {
            var $scope, http, params;

            // When opening a modal from another modal, remove the latter from the DOM
            // first.
            if (this.$scope) {
                this.$scope.$destroy();
            }
            if (me.modal) {
                me.modal.remove();
            }
            if (me.overlay) {
                me.overlay.remove();
            }
            $scope = $rootScope.$new();
            this.$scope = $scope;

            /**
             * @ngdoc function
             * @name Modal.close
             * @methodOf adminNg.modal.Modal
             * @description
             * Close the currently open modal.
             *
             * Fades out the overlay and the tab content and updates the URL by
             * removing all search parameters.
             */
            $scope.close = function (fetch) {
                $scope.open = false;
                $location.search({});
                if (!angular.isUndefined(me.modal)) {
                    me.modal.remove();
                    me.overlay.remove();
                    delete me.modal;
                    delete me.overlay;
                }
                if(angular.isUndefined(fetch) || true === fetch) {
                    Table.fetch();
                }
                $scope.$destroy();
            };

            /**
             * @ngdoc function
             * @name Modal.keyUp
             * @methodOf adminNg.modal.Modal
             * @description
             * Closes the modal when pressing ESC.
             *
             * @param {event} event Event that triggered this function.
             */
            $scope.keyUp = function (event) {
                switch (event.keyCode) {
                case 27:
                    $scope.close();
                    break;
                }
            };

            // Guard against concurrent calls
            if (me.opening) { return; }
            me.opening = true;
            $scope.open = false;

            // Fetch the modal markup from the partial named after its ID
            http = $http.get('shared/partials/modals/' + modalId + '.html', {});

            http.then(function (html) {
                // Compile modal and overlay and attach them to the DOM.
                me.overlay = angular.element('<div ng-show="open" class="modal-animation modal-overlay"></div>');
                angular.element(document.body).append(me.overlay);
                $compile(me.overlay)($scope);

                me.modal = angular.element(html.data);
                angular.element(document.body).prepend(me.modal);
                $compile(me.modal)($scope);

                // Signal animation start to overlay and modal
                $scope.open = true;

                // Set location
                params = $location.search();
                params.modal = modalId;
                $location.search(params);

                delete me.opening;

                // Focus the modal so it can be closed by key press
                $timeout(function () {
                    me.modal.focus();
                }, 100);
            });

            return http;
        };
    };

    return new Modal();
}]);

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
* A service to manage filter profiles.
*
* Filter profiles will be stored in the localStorage.
*/
angular.module('adminNg.services')
.factory('FilterProfiles', ['localStorageService', function (localStorageService) {
    var FilterProfileStorage = function () {
        var me = this;

        this.getFromStorage = function () {
            this.storage = angular.fromJson(localStorageService.get('filterProfiles')) || {};
        };

        this.get = function (namespace) {
            return angular.copy(me.storage[namespace]) || [];
        };

        this.save = function () {
            localStorageService.add('filterProfiles', angular.toJson(me.storage));
        };

        this.set = function (namespace, value) {
            me.storage[namespace] = angular.copy(value);
            me.save();
        };

        this.getFromStorage();
    };
    return new FilterProfileStorage();
}]);

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
.provider('Notifications', function () {
    var notifications = {},
        keyList = {},
        uniqueId = 0;

    this.$get = ['$rootScope', '$injector', function ($rootScope, $injector) {
        var AuthService;

        // notification durations for different log level in seconds
        var notificationDurationError = -1,
            notificationDurationSuccess = 5,
            notificationDurationWarning = 5;

        var scope = $rootScope.$new(),
            initContext = function (context) {
                if (angular.isDefined(keyList[context])) {
                    return notifications[context];
                }

                // initialize the arrays the first time
                keyList[context] = [];
                notifications[context] = {};

                return notifications[context];
            };

        scope.get = function (context) {
            var ADMIN_NOTIFICATION_DURATION_ERROR = 'admin.notification.duration.error',
                ADMIN_NOTIFICATION_DURATION_SUCCESS = 'admin.notification.duration.success',
                ADMIN_NOTIFICATION_DURATION_WARNING = 'admin.notification.duration.warning';

            // We bind to AuthService here to prevent a circular dependency to $http
            if (!AuthService) { AuthService = $injector.get('AuthService'); }

            if (AuthService) {
                AuthService.getUser().$promise.then(function(user) {
                    if (angular.isDefined(user.org.properties[ADMIN_NOTIFICATION_DURATION_ERROR])) {
                        notificationDurationError = user.org.properties[ADMIN_NOTIFICATION_DURATION_ERROR];
                    }
                    if (angular.isDefined(user.org.properties[ADMIN_NOTIFICATION_DURATION_SUCCESS])) {
                        notificationDurationSuccess = user.org.properties[ADMIN_NOTIFICATION_DURATION_SUCCESS];
                    }
                    if (angular.isDefined(user.org.properties[ADMIN_NOTIFICATION_DURATION_WARNING])) {
                        notificationDurationWarning = user.org.properties[ADMIN_NOTIFICATION_DURATION_WARNING];
                    }
                });
            }

            if (!context) {
                context = 'global';
            }
            return notifications[context] || initContext(context);
        };

        scope.remove = function (id, context) {
            var key;
            if (!context) {
                context = 'global';
            }

            if (notifications[context] && notifications[context][id]) {
                // remove from key list
                key = notifications[context][id].key;
                keyList[context].splice(keyList[context].indexOf(key), 1);

                notifications[context][id] = undefined;
                delete notifications[context][id];
                scope.$emit('deleted', context);
            }
        };

        scope.addWithParams = function (type, key, messageParams, context, duration) {
            scope.add(type, key, context, duration, messageParams);
        };

        scope.add = function (type, key, context, duration, messageParams) {
            if (angular.isUndefined(duration)) {
                // fall back to defaults
                switch (type) {
                    case 'error':
                        duration = notificationDurationError;
                        break;
                    case 'success':
                        duration = notificationDurationSuccess;
                        break;
                    case 'warning':
                        duration = notificationDurationWarning;
                        break;
                }
                // default durations are in seconds. duration needs to be in milliseconds
                if (duration > 0) duration *= 1000;
            }

            if (!context) {
                context = 'global';
            }

            if (!messageParams) {
                messageParams = {};
            }

            initContext(context);

            if(keyList[context].indexOf(key) < 0) {
                // only add notification if not yet existent

                // add key to an array
                keyList[context].push(key);

                notifications[context][++uniqueId] = {
                    type       : type,
                    key        : key,
                    message    : 'NOTIFICATIONS.' + key,
                    parameters : messageParams,
                    duration   : duration,
                    id         : uniqueId,
                    hidden     : false
                };

                scope.$emit('added', context);
            } else {
              var notification = _.find(notifications.global, function(a) {return a.key === key});
              if(notification) {
                  notification.hidden = false;
              }
            }
            return uniqueId;
        };

        return scope;
    }];
});

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
.factory('Table', ['$rootScope', '$filter', 'Storage', '$location', '$timeout', 'decorateWithTableRowSelection',
    function ($rootScope, $filter, Storage, $location, $timeout, decorateWithTableRowSelection) {
    var TableService = function () {
        var me = this,
            ASC = 'ASC',
            DESC = 'DESC',
            DEFAULT_REFRESH_DELAY = 5000;

        this.rows = [];
        this.allSelected = false;
        this.sorters = [];
        this.loading = true;

        // Variable related to the pagination
        this.pagination = {
            totalItems  :       100, // the number of items in total
            pages       :        [], // list of pages
            limit       :        10, // the number of items per page
            offset      :         0, // currently selected page
            directAccessibleNo :  3  // number of pages on each side of the current index
        };

        this.updatePagination = function () {
            var p = me.pagination, i, numberOfPages = p.totalItems / p.limit;

            p.pages = [];
            for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
                p.pages.push({
                    number: i,
                    label: (i + 1).toString(),
                    active: i === p.offset
                });
            }
        };

        this.refreshColumns = function () {
            var currentTable, currentPath, localStorageColumnConfig;
            currentPath = $location.path();
            currentTable = currentPath.substring(currentPath.lastIndexOf('/') + 1, currentPath.length);
            localStorageColumnConfig = Storage.get('table_column_visibility', currentTable, currentPath);
            if (!$.isEmptyObject(localStorageColumnConfig)) {
                me.columns = localStorageColumnConfig.columns;
                me.columnsConfiguredFromLocalStorage = true;
            } else {
                me.columnsConfiguredFromLocalStorage = false;
            }
        };

        this.getDirectAccessiblePages = function () {
            var startIndex = me.pagination.offset - me.pagination.directAccessibleNo,
                endIndex = me.pagination.offset + me.pagination.directAccessibleNo,
                retVal = [], i, pageToPush;


            if (startIndex < 0) {
                //adjust window if selected window is to low
                endIndex = endIndex - startIndex; // e.g. startIndex: -2 / endIndex: 1 -> endIndex = 1 - (-2) = 3
                startIndex = 0;
            }

            if (endIndex >= me.pagination.pages.length) {
                //adjust window if selected window is to high
                startIndex = startIndex - (endIndex - me.pagination.pages.length) - 1;
                endIndex = me.pagination.pages.length - 1;
            }
            //set endIndex to the highest possible value
            endIndex = Math.min(me.pagination.pages.length - 1, endIndex);
            startIndex = Math.max(0, startIndex);

            for (i = startIndex; i <= endIndex; i++) {

                if (i === startIndex && startIndex !== 0) {
                    // we take the first item if start index is not 0
                    pageToPush = me.pagination.pages[0];
                }
                else if (i === endIndex && endIndex !== me.pagination.pages.length - 1) {
                    // we add the last item if end index is not the real end
                    pageToPush = me.pagination.pages[me.pagination.pages.length - 1];
                }
                else if ((i === startIndex + 1 && startIndex !== 0) || i === endIndex - 1 && endIndex !== me.pagination.pages.length - 1) {
                    // we add the .. at second position or second last position if start- or  end-index is not 0
                    me.pagination.pages[i].label = '..';
                    pageToPush = me.pagination.pages[i];
                }
                else {
                    pageToPush = me.pagination.pages[i];
                }
                retVal.push(pageToPush);
            }
            if (retVal[endIndex]) {
                me.maxLabel = retVal[endIndex].label;
            }
            return retVal;
        };

        this.goToPage = function (page) {
            me.allSelected = false;
            me.pagination.pages[me.pagination.offset].active = false;
            me.pagination.offset = page;
            me.pagination.pages[me.pagination.offset].active = true;
            Storage.put('pagination', me.resource, 'offset', page);
            me.fetch();
        };

        this.isNavigatePrevious = function () {
            return me.pagination.offset > 0;
        };

        this.isNavigateNext = function () {
            return me.pagination.offset < me.pagination.pages.length - 1;
        };

        /**
         * Changes the number of items on a page to the given value.
         *
         * @param pageSize
         */
        this.updatePageSize = function (pageSize) {
            var p = me.pagination, oldPageSize = p.limit;

            p.limit = pageSize;
            p.offset = 0;

            me.updatePagination();

            Storage.put('pagination', me.resource, 'limit', p.limit);
            Storage.put('pagination', me.resource, 'offset', p.offset);
            if (p.limit !== oldPageSize) {
                me.fetch();
            }
        };

        this.configure = function (options) {
            var pagination;
            me.allSelected = false;
            me.options = options;
            me.refreshColumns();

            if (!me.columnsConfiguredFromLocalStorage) {
                // The user has not configured the columns before, hence we will
                // configure them from scratch
                me.columns = options.columns;
                angular.forEach(me.columns, function (column) {
                    column.deactivated = false;
                });
            }

            me.caption = options.caption;
            me.resource = options.resource;
            me.category = options.category;
            me.apiService = options.apiService;
            me.multiSelect = options.multiSelect;
            me.refreshDelay = options.refreshDelay || DEFAULT_REFRESH_DELAY;
            me.postProcessRow = options.postProcessRow;

            me.predicate = '';
            me.reverse = false;

            me.sorters = [];

            // Load pagination configuration from local storage
            pagination = Storage.get('pagination', me.resource);
            if (angular.isDefined(pagination)) {
                if (angular.isDefined(pagination.limit)) {
                    me.pagination.limit = pagination.limit;
                }

                if (angular.isDefined(pagination.offset)) {
                    me.pagination.offset = pagination.offset;
                }
            }

            me.updatePageSize(me.pagination.limit);

            // Load sorting criteria from local storage
            angular.forEach(Storage.get('sorter', me.resource), function (values, name) {
                me.sorters[values.priority] = {
                    name: name,
                    priority: values.priority,
                    order: values.order
                };
            });

            if (me.sorters.length > 0) {
                me.predicate = me.sorters[0].name;
                me.reverse = me.sorters[0].order === DESC;
            }
            // if no entry in local storage, sort by first sortable column
            else {
                for (var i = 0; i < me.columns.length; i++) {
                    var column = me.columns[i];

                    if (!column.dontSort) {
                        me.sortBy(column);
                        break;
                    }
                }
            }
        };

        this.saveSortingCriteria = function (sorterCriteria) {
            angular.forEach(sorterCriteria, function (values, priority) {
                values.priority = priority;
                Storage.put('sorter', me.resource, values.name, values);
            });
        };

        this.sortBy = function (column) {
            // Avoid sorting by action column
            if (angular.isUndefined(column) || column.dontSort) {
                return;
            }

            var newOrder, values;

            values = Storage.get('sorter', me.resource)[column.name];
            newOrder = (values && values.order && values.order === ASC) ? DESC : ASC;

            if (values && angular.isDefined(values.priority)) {
                me.sorters.splice(values.priority, 1);
            }

            me.sorters.splice(0, 0, {
                name     : column.name,
                priority : 0,
                order    : newOrder
            });

            me.saveSortingCriteria(me.sorters);

            me.predicate = column.name;
            me.reverse = newOrder === DESC;

            me.fetch();
        };

        /**
         * Retrieve data from the defined API with the given filter values.
         */
        this.fetch = function (reset) {
            if (angular.isUndefined(me.apiService)) {
                return;
            }

            if(reset) {
              me.rows = [];
              me.pagination.totalItems = 0;
              me.updatePagination();
              me.updateAllSelected();
            }

            var query = {},
                filters = [],
                sorters = [];

            me.loading = true;

            angular.forEach(Storage.get('filter', me.resource), function (value, filter) {
                filters.push(filter + ':' + value);
            });

            if (filters.length) {
                query.filter = filters.join(',');
            }

            // Limit temporary to sort by one criteria
            if (me.sorters.length > 0) {
                sorters.push(me.sorters[0].name + ':' + me.sorters[0].order);
            }

            query.limit = me.pagination.limit;
            query.offset = me.pagination.offset * me.pagination.limit;

            if (filters.length) {
                query.filter = filters.join(',');
            }

            if (sorters.length) {
                query.sort = sorters.join(',');
            }

            query.limit = me.pagination.limit;
            query.offset = me.pagination.offset * me.pagination.limit;

            (function(resource){
              me.apiService.query(query).$promise.then(function (data) {
                if(resource != me.resource) {
                  return;
                }

                var selected = [];
                angular.forEach(me.rows, function (row) {
                    if (row.selected) {
                        selected.push(row.id);
                    }
                });
                angular.forEach(data.rows, function (row) {
                    if (selected.indexOf(row.id) > -1) {
                        row.selected = true;
                    }
                });
                if (angular.isDefined(me.postProcessRow)) {
                    angular.forEach(data.rows, function(row) {
                        me.postProcessRow(row);
                    });
                }
                me.rows = data.rows;
                me.loading = false;
                me.pagination.totalItems = data.total;

                // If the current offset is not 0 and we have no result, we move to the first page
                if (me.pagination.offset !== 0 && data.count === 0) {
                    me.goToPage(0);
                }

                me.updatePagination();
                me.updateAllSelected();
              });
            })(me.resource);

            if (me.refreshScheduler.on) {
                me.refreshScheduler.newSchedule();
            }
        };

        /**
         * Scheduler for the refresh of the fetch
         */
        this.refreshScheduler = {
            on: true,
            newSchedule: function () {
                me.refreshScheduler.cancel();
                me.refreshScheduler.nextTimeout = $timeout(me.fetch, me.refreshDelay);
            },
            cancel: function () {
                if (me.refreshScheduler.nextTimeout) {
                    $timeout.cancel(me.refreshScheduler.nextTimeout);
                }
            }
        };

        this.addFilterToStorage = function(column, filter) {
          Storage.put('filter', me.resource, column, filter);
        }

        this.addFilterToStorageForResource = function(resource, column, filter) {
          Storage.put('filter', resource, column, filter);
        }

        this.gotoRoute = function(path) {
          $location.path(path);
        }

        // Reload the table if the language is changed
        $rootScope.$on('language-changed', function () {
            if (!me.loading) {
                me.fetch();
            }
        });
    };
    var tableService = new TableService();
    return decorateWithTableRowSelection(tableService);
}]);

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
    .factory('decorateWithTableRowSelection', function () {
        /**
         * A decorator that adds all necessary methods for row selection functionality.
         * Usage: Inject this service and invoke it on the object you want to decorate, note that the object must
         * store its rows in the variable this.rows.
         * @type {{}}
         */
        var decorateWithTableRowSelection = function (target) {
            var each = function (callback) {
                angular.forEach(target.rows, function (row) {
                    callback(row);
                });
            };

            /**
             * Fires when the select all checkbox has changed. This usually works fine with ng-model and ng-change,
             * but in the case of schedule-task-modal.html it didn't!. The model parameter is a workaround that worked.
             *
             * @param model Optional paramater, can be set if ng-change fires before the new value is written to the model.
             */
            target.allSelectedChanged = function (model) {
                var newState = target.allSelected;
                if (angular.isDefined(model)) {
                    newState = model;
                }
                each(function (row) {
                    row.selected = newState;
                });
            };

            target.getSelected = function () {
                var result = [];
                each(function (row) {
                    if (row.selected) {
                        result.push(row);
                    }
                });
                return result;
            };

            target.getSelectedIds = function () {
                var result = [];
                each(function (row) {
                    if(row.selected) {
                        result.push(row.id);
                    }
                });
                return result;
            };

            target.hasAnySelected = function () {
                return target.getSelected().length > 0;
            };

            /**
             * Indicates that one of the rows has changed the selection flag. Reevaluation of the select all flag
             * will be needed.
             *
             * @param rowId
             */
            target.rowSelectionChanged = function (rowId) {
                if (target.rows[rowId].selected !== true) {
                    // untick select all if the new value is undefined or false
                    target.allSelected = false;
                } else if (target.getSelected().length === target.rows.length) {
                    // tick select all if all selected boxes are active now.
                    target.allSelected = true;
                    target.allSelectedChanged();
                }
            };

            /**
             * Looks at all rows and updates the allSelected flag accordingly.
             */
            target.updateAllSelected = function () {
                target.allSelected = target.getSelectedIds().length === target.rows.length;
            };

            target.deselectAll = function () {
                target.allSelected = false;
                each(function (row) {
                    row.selected = false;
                });
            };

            /**
             * Returns copies of the currently selected rows, changing them will not affect the table data.
             *
             * @returns {Array} A copy of the selected rows.
             */
            target.copySelected = function () {
                var copy = [];
                angular.forEach(target.getSelected(), function (row) {
                    copy.push(angular.extend({}, row ));
                });
                return copy;
            };

            if (target.rowsPromise) {
                target.rowsPromise.then(function () {
                    target.allSelected = target.getSelected().length === target.rows.length;
                });
            }

            return target;
        };

        return decorateWithTableRowSelection;
    });

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
.factory('Stats', ['$rootScope', '$filter', 'Storage', '$location', '$timeout',
    function ($rootScope, $filter, Storage, $location, $timeout) {
    var StatsService = function () {
        var me = this,
            DEFAULT_REFRESH_DELAY = 5000;

        this.stats = [];
        this.loading = true;

        this.configure = function (options) {
            me.resource = options.resource;
            me.apiService = options.apiService;
            me.stats = options.stats;
            me.refreshDelay = options.refreshDelay || DEFAULT_REFRESH_DELAY;
        };

        /**
         * Retrieve data from the defined API with the given filter values.
         */
        this.fetch = function () {
            if (angular.isUndefined(me.apiService)) {
                return;
            }

            me.loading = true;
            me.runningQueries = 0;

            angular.forEach(me.stats, function (stat) {
                var query = {};
                var filters = [];
                angular.forEach(stat.filters, function (filter) {
                    filters.push(filter.name + ':' + filter.value);
                });
                if (filters.length) {
                    query.filter = filters.join(',');
                }

                /* Workaround:
                 * We don't want actual data here, but limit 0 does not work (retrieves all data)
                 * See MH-11892 Implement event counters efficiently
                 */
                query.limit = 1;
                me.runningQueries++;

                me.apiService.query(query).$promise.then(function (data) {
                    me.loading = false;
                    stat.counter = data.total;
                    stat.index = me.stats.indexOf(stat);

                    me.runningQueries--;
                    me.refreshScheduler.restartSchedule();
                }, function () {
                    me.runningQueries--;
                    me.refreshScheduler.restartSchedule();
                });
            });
        };

        /**
         * Scheduler for the refresh of the fetch
         */
        this.refreshScheduler = {
            on: true,
            restartSchedule: function () {
              if (me.refreshScheduler.on && (angular.isUndefined(me.runningQueries) || me.runningQueries <= 0)) {
                  me.refreshScheduler.newSchedule();
              }
            },
            newSchedule: function () {
                me.refreshScheduler.cancel();
                me.refreshScheduler.nextTimeout = $timeout(me.fetch, me.refreshDelay);
            },
            cancel: function () {
                if (me.refreshScheduler.nextTimeout) {
                    $timeout.cancel(me.refreshScheduler.nextTimeout);
                }
            }
        };

        // Reload the stats if the language is changed
        $rootScope.$on('language-changed', function () {
            if (!me.loading) {
                me.fetch();
            }
        });

    };
    return new StatsService();
}]);

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
.factory('underscore', function() {
  return window._; // assumes underscore has already been loaded on the page
});

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
.factory('FormNavigatorService', function () {
    var FormNavigator = function () {
        this.navigateTo = function (targetForm, currentForm, requiredForms) {
            var valid = true;
            angular.forEach(requiredForms, function (form) {
                if (!form.$valid) {
                    valid = false;
                }
            });
            if (valid) {
                return targetForm;
            }
            else {
                return currentForm;
            }
        };
    };

    return new FormNavigator();
});

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
 * Utility service bridging video and timeline functionality.
 */
angular.module('adminNg.services')
.factory('VideoService', [
    function () {

    var VideoService = function () {
        this.getCurrentSegment = function (player, video) {
            var matchingSegment,
                position = player.adapter.getCurrentTime() * 1000;
            angular.forEach(video.segments, function (segment) {
                if ((segment.start <= position) && (segment.end >= position)) {
                    matchingSegment = segment;
                }
            });
            return matchingSegment;
        };
        this.getPreviousActiveSegment = function (player, video) {
            var matchingSegment,
                previousSegment,
                position = player.adapter.getCurrentTime() * 1000;
            angular.forEach(video.segments, function (segment) {
                if ((segment.start <= position) && (segment.end >= position)) {
                    matchingSegment = previousSegment;
                }
                if (!segment.deleted) {
                  previousSegment = segment;
                }
            });
            return matchingSegment;
        };
        // get the next active segment including the current segment.
        this.getNextActiveSegment = function (player, video) {
            var matchingSegment,
                foundCurrentSegment,
                position = player.adapter.getCurrentTime() * 1000;
            angular.forEach(video.segments, function (segment) {
                if ((segment.start <= position) && (segment.end >= position)) {
                    foundCurrentSegment = true;
                }
                if (foundCurrentSegment && ! matchingSegment && !segment.deleted) {
                  matchingSegment = segment;
                }
            });
            return matchingSegment;
        };
    };

    return new VideoService();
}]);

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
.service('RegexService', [function () {
    return {
        translateDateFormatToPattern: function(datePattern){
            if (angular.isString(datePattern) && datePattern.toLowerCase() === 'yyyy-mm-dd') {
                return '[0-9]{4}-[0-9]{2}-[0-9]{2}';
            }
        }
    };
}]);

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
.factory('SchedulingHelperService', [function () {
    var SchedulingHelperService = function () {

        var me = this;

        this.parseDate = function(dateStr, hour, minute) {
            var dateArray = dateStr.split("-");
            return new Date(dateArray[0], parseInt(dateArray[1]) - 1, dateArray[2], hour, minute);
        }

        this.getUpdatedEndDateSingle = function(start, end) {
            var startDate = me.parseDate(start.date, start.hour, start.minute);
            if (end.hour < start.hour) {
                startDate.setHours(24);
            }
            return moment(startDate).format('YYYY-MM-DD');
        };

        this.applyTemporalValueChange = function(temporalValues, change, single){
            var startMins = temporalValues.start.hour * 60 + temporalValues.start.minute;
            switch(change) {
                case "duration":
                    // Update end time
                    var durationMins = temporalValues.duration.hour * 60 + temporalValues.duration.minute;
                    temporalValues.end.hour = (Math.floor((startMins + durationMins) / 60)) % 24;
                    temporalValues.end.minute = (startMins + durationMins) % 60;
                    break;
                case "start":
                case "end":
                    // Update duration
                    var endMins = temporalValues.end.hour * 60 + temporalValues.end.minute;
                    if (endMins < startMins) endMins += 24 * 60; // end is on the next day
                    temporalValues.duration.hour = Math.floor((endMins - startMins) / 60);
                    temporalValues.duration.minute = (endMins - startMins) % 60;
                    break;
                default:
                    return;
            }
            if (single) {
                temporalValues.end.date = me.getUpdatedEndDateSingle(temporalValues.start, temporalValues.end);
            }
        };
    };
    return new SchedulingHelperService();
}]);

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
.factory('EventHelperService', function () {
    var EventHelperService = function () {
        var me = this,
            eventId;

        this.reset = function () {
          eventId = undefined;
        }

    };

    return new EventHelperService();
});

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
.factory('HotkeysService', ['$q', 'IdentityResource', 'hotkeys',
        function ($q, IdentityResource, hotkeys) {
    var HotkeysService = function () {
      var me = this,
          identity,
          keyBindings,
          loading;

      this.keyBindings = {};

      this.loadHotkeys = function () {
        return $q(function (resolve, reject) {
          identity = IdentityResource.get();
          identity.$promise.then(function (info) {
            if (info && info.org && info.org.properties && info.org.properties) {
              var properties = info.org.properties;
              angular.forEach(Object.keys(properties), function (key) {
                if (key.indexOf("admin.shortcut.") >= 0) {
                  var keyIdentifier = key.substring(15),
                      value = properties[key];
                  me.keyBindings[keyIdentifier] = value;
                }
              });
              resolve();
            } else {
              reject(); // as no hotkeys have been loaded
            }
          });
        });
      };

      this.activateHotkey = function (scope, keyIdentifier, description, callback) {
        me.loading.then(function () {
          var key = me.keyBindings[keyIdentifier];
          if (key !== undefined) {
            hotkeys.bindTo(scope).add({
              combo: key,
              description: description,
              callback: callback
            });
          }
        });
      }

      this.activateUniversalHotkey = function (keyIdentifier, description, callback) {
        me.loading.then(function () {
          var key = me.keyBindings[keyIdentifier];
          if (key !== undefined) {
            hotkeys.add({
              combo: key,
              description: description,
              callback: callback
            });
          }
        });
      }
      this.loading = this.loadHotkeys();
    };

    return new HotkeysService();
}]);

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
.factory('RestServiceMonitor', ['$http', '$location', 'Storage', function($http, $location, Storage) {
    var Monitoring = {};
    var services = {
        service: {},
        error: false,
        numErr: 0
    };

    var AMQ_NAME = "ActiveMQ";
    var STATES_NAME = "Service States";
    var BACKEND_NAME = "Backend Services";
    var MALFORMED_DATA = "Malformed Data";
    var OK = "OK";
    var SERVICES_FRAGMENT = "/systems/services";
    var SERVICE_NAME_ATTRIBUTE = "service-name";

    Monitoring.run = function() {
      //Clear existing data
      services.service = {};
      services.error = false;
      services.numErr = 0;

      Monitoring.getActiveMQStats();
      Monitoring.getBasicServiceStats();
    };

    Monitoring.getActiveMQStats = function() {
      $http.get('/broker/status')
           .then(function(data) {
             Monitoring.populateService(AMQ_NAME);
             if (data.status === 204) {
               services.service[AMQ_NAME].status = OK;
               services.service[AMQ_NAME].error = false;
             } else {
               services.service[AMQ_NAME].status = data.statusText;
               services.service[AMQ_NAME].error = true;
             }
           }, function(err) {
             Monitoring.populateService(AMQ_NAME);
             services.service[AMQ_NAME].status = err.statusText;
             services.service[AMQ_NAME].error = true;
             services.error = true;
             services.numErr++;
           });
    };

    Monitoring.setError = function(service, text) {
        Monitoring.populateService(service);
        services.service[service].status = text;
        services.service[service].error = true;
        services.error = true;
        services.numErr++;
    };

    Monitoring.getBasicServiceStats = function() {
      $http.get('/services/health.json')
           .then(function(data) {
             if (undefined === data.data || undefined === data.data.health) {
               Monitoring.setError(STATES_NAME, MALFORMED_DATA);
               return;
             }
             var abnormal = 0;
             abnormal = data.data.health['warning'] + data.data.health['error'];
             if (abnormal == 0) {
               Monitoring.populateService(BACKEND_NAME);
               services.service[BACKEND_NAME].status = OK;
             } else {
               Monitoring.getDetailedServiceStats();
             }
           }, function(err) {
             Monitoring.setError(STATES_NAME, err.statusText);
           });
    };

    Monitoring.getDetailedServiceStats = function() {
      $http.get('/services/services.json')
           .then(function(data) {
             if (undefined === data.data || undefined === data.data.services) {
               Monitoring.setError(BACKEND_NAME, MALFORMED_DATA);
               return;
             }
             angular.forEach(data.data.services.service, function(service, key) {
               name = service.type.split('opencastproject.')[1];
               if (service.service_state != "NORMAL") {
                 Monitoring.populateService(name);
                 services.service[name].status = service.service_state;
                 services.service[name].error = true;
                 services.error = true;
                 services.numErr++;
               }
             });
           }, function(err) {
             Monitoring.setError(BACKEND_NAME, err.statusText);
           });
    };

    Monitoring.populateService = function(name) {
        if (services.service[name] === undefined) {
            services.service[name] = {};
        }
    };

    Monitoring.jumpToServices = function(event) {
      var serviceName = null;
      if (event.target.tagName == "a")
        serviceName = event.target.getAttribute(SERVICE_NAME_ATTRIBUTE)
      else
        serviceName = event.target.parentNode.getAttribute(SERVICE_NAME_ATTRIBUTE);

      if (serviceName != AMQ_NAME) {
        Storage.put('filter', 'services', 'actions', 'true');
        $location.path(SERVICES_FRAGMENT).replace();
      }
    };

    Monitoring.getServiceStatus = function() {
        return services;
    };

    return Monitoring;
}]);

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

// Resources registry.
angular.module('adminNg.resources', []);

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

angular.module('adminNg.resources')
.factory('IdentityResource', ['$resource', function ($resource) {
    return $resource('/info/me.json', {}, {
        query: {method: 'GET'}
    });
}]);

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

angular.module('adminNg.resources')
.factory('VersionResource', ['$resource', function ($resource) {
    return $resource('/sysinfo/bundles/version?prefix=opencast', {}, {
        query: { method: 'GET' }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventsResource', ['$resource', 'Language', '$translate', 'ResourceHelper', function ($resource, Language, $translate, ResourceHelper) {

    /*
     * Here's an example for how we can fetch mock data from the server:
     * ...
     * return $resource('events/events.json', {}, {
     * ...,
     * this resource does not have a leading slash, hence the mock-data will be fetched from admin-ng/events/events.json
     *
     * In order to fetch real data, just add a leading slash:
     * ...
     * return $resource('/events/events.json', {}, {
     * ...,
     * then the real data will be fetched from /events/events.json
     */

    // We are live and are getting the real thing.
    return $resource('/admin-ng/event/:id', { id: '@id' }, {
        query: {method: 'GET', params: { id: 'events.json' }, isArray: false, transformResponse: function (data) {
            return ResourceHelper.parseResponse(data, function (r) {
                var row = {};
                row.id = r.id;
                row.title = r.title;
                row.presenter = r.presenters.join(', ');
                row.technical_presenter = r.technical_presenters.join(', ');
                if (angular.isDefined(r.series)) {
                    row.series_name = r.series.title;
                    row.series_id = r.series.id;
                }
                row.review_status = r.review_status;
                row.event_status_raw = r.event_status;
                $translate(r.event_status).then(function (translation) {
                	row.event_status = translation;
                });
                row.source = r.source;
                row.scheduling_status = r.scheduling_status;
                $translate(r.scheduling_status).then(function (translation) {
                    row.scheduling_status = translation;
                });
                row.workflow_state = r.workflow_state;
                row.date = Language.formatDate('short', r.start_date);
                row.technical_date = Language.formatDate('short', r.technical_start);
                row.technical_date_raw = r.technical_start
                row.publications = r.publications;
                if (typeof(r.publications) != 'undefined' && r.publications != null) {
                	var now = new Date();
                	for (var i = 0; i < row.publications.length; i++)
                		if (row.publications[i].id == "engage-live" && 
                				(now < new Date(r.start_date) || now > new Date(r.end_date)))
                			row.publications[i].enabled = false;
                		else row.publications[i].enabled = true;
                }
                row.start_date = Language.formatTime('short', r.start_date);
                row.technical_start = Language.formatTime('short', r.technical_start);
                row.end_date = Language.formatTime('short', r.end_date);
                row.technical_end = Language.formatTime('short', r.technical_end);
                row.has_comments = r.has_comments;
                row.has_open_comments = r.has_open_comments;
                row.needs_cutting = r.needs_cutting;
                row.has_preview = r.has_preview;
                row.location = r.location;
                row.agent_id = r.agent_id;
                row.managed_acl = r.managedAcl;
                row.type = "EVENT";
                return row;
            });

        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('OptoutsResource', ['$resource', function ($resource) {
    var transformRequest = function (data) {
        if (angular.isDefined(data.eventIds)) {
            data.eventIds = '["' + data.eventIds.join('","') + '"]';
        } else if (angular.isDefined(data.seriesIds)) {
            data.seriesIds = '["' + data.seriesIds.join('","') + '"]';
        }
        return $.param(data);
    };
    return $resource('/admin-ng/:resource/optouts', {resource: '@resource'}, {
        save: {
            method: 'POST',
            responseType: 'json',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            transformRequest: transformRequest
        }
    });
}]);


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

angular.module('adminNg.resources')
.factory('OptoutSingleResource', ['$resource', function ($resource) {

    return $resource('/admin-ng/:resource/:id/optout/:optout', {resource: '@resource', id: '@id', optout: '@optout'}, {
        save: {
            method: 'PUT'
        }
    });
}]);


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

angular.module('adminNg.resources')
.factory('SeriesResource', ['$resource', 'Language', 'ResourceHelper', function ($resource, Language, ResourceHelper) {

    return $resource('/admin-ng/series/:id', { id: '@id' }, {
        query: {
            method: 'GET',
            params: { id: 'series.json' },
            isArray: false,
            transformResponse: function (data) {
                return ResourceHelper.parseResponse(data, function (r) {
                    var row = {};
                    row.id = r.id;
                    row.title = r.title;
                    row.creator = r.organizers.join(', ');
                    row.contributors = r.contributors.join(', ');
                    row.createdDateTime = Language.formatDate('short', r.creation_date);
                    row.managed_acl = r.managedAcl;
                    row.type = "SERIES";
                    return row;
                });
            }
        },
        create: {
            method: 'POST',
            params: { id: 'new' },
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }

                return $.param({metadata: angular.toJson(data)});
            },
            transformResponse: function (response) {
                // if this method is missing, the angular default is to interpret the response as JSON
                // in our case, the response is just a uuid string which causes angular to break.
                return response;
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('SeriesAccessResource', ['$resource', function ($resource) {
    var seriesResource,
        managedAclResource,
        accessResource;

    seriesResource = $resource('/admin-ng/series/:id/access.json', { id: '@id' }, {
        get: {  method: 'GET' }
    });

    accessResource = $resource('/admin-ng/series/:id/access', { id: '@id' }, {
        save: { method: 'POST',
                isArray: true,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {

                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    return $.param({
                        acl      : angular.toJson({acl: data.acl}),
                        override : true
                    });
                }
        }
    });

    managedAclResource = $resource('/acl-manager/acl/:id', { id: '@id'}, {
        get: { method: 'GET'}
    });

    return {
        getManagedAcl : managedAclResource.get,
        get           : seriesResource.get,
        save          : accessResource.save,
    };
}]);

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

angular.module('adminNg.resources')
.factory('SeriesMetadataResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
    var transform = function (data) {
        var metadata = {};
        try {
            metadata = JSON.parse(data);
            JsHelper.replaceBooleanStrings(metadata);
        } catch (e) { }
        return { entries: metadata };
    };

    return $resource('/admin-ng/series/:id/metadata:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transform
        },
        save: { method: 'PUT',
            isArray: true,
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (catalog) {
                if (catalog.attributeToSend) {
                    var catalogToSave = {
                        flavor: catalog.flavor,
                        title: catalog.title,
                        fields: []
                    };

                    angular.forEach(catalog.fields, function (entry) {
                        if (entry.id === catalog.attributeToSend) {
                            catalogToSave.fields.push(entry);
                        }
                    });
                    return $.param({metadata: angular.toJson([catalogToSave])});
                }
                return $.param({metadata: angular.toJson([catalog])});
            },
            tranformResponse: transform
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('SeriesParticipationResource', ['$resource', function ($resource) {
    var transformResponse = function (data) {
        var metadata = {};

        try {
            metadata = JSON.parse(data);
            if (metadata.opt_out) {
                metadata.opt_out = 'true';
            } else {
                metadata.opt_out = 'false';
            }
        } catch (e) { }

        return metadata;
    };

    return $resource('/admin-ng/series/:id/participation:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('SeriesEventsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var events = {};
        try {
            events = JSON.parse(data);
        } catch (e) { }
        return { entries: events };
    };

    return $resource('/admin-ng/series/:id/events.json', { id: '@id' }, {
        get: { method: 'GET', transformResponse: transform },
        save: { method: 'PUT', transformRequest: function (data) {
            return JSON.stringify(data.entries);
        }, transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('SeriesThemeResource', ['$resource', function ($resource) {

        return $resource('/admin-ng/series/:id/theme:ext', {id: '@id'}, {
            save: {
            	method: 'PUT',
            	headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                transformRequest: function (data) {
                    var d = {};
                    if (angular.isUndefined(data)) {
                        return data;
                    }
                    d.themeId = data.theme;
                	return $.param(d);
            	}
            },
            get: {params:{'ext':'.json'}, method: 'GET'},
            delete: {params:{'ext':''}, method: 'DELETE'}
        });
}]);

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

angular.module('adminNg.resources')
.factory('EventMetadataResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
    var transformResponse = function (data) {
            var metadata = {};
            try {
                metadata = JSON.parse(data);
                angular.forEach(metadata, function (catalog) {
                    if (angular.isDefined(catalog.locked)) {
                        angular.forEach(catalog.fields, function (field) {
                            field.locked = catalog.locked;
                            field.readOnly = true;
                        });
                    }
                });
                JsHelper.replaceBooleanStrings(metadata);
            } catch (e) { }
            return { entries: metadata };
        },
        transformRequest = function (catalog) {
            if (catalog.attributeToSend) {
                var catalogToSave = {
                    flavor: catalog.flavor,
                    title: catalog.title,
                    fields: []
                };

                angular.forEach(catalog.fields, function (entry) {
                    if (entry.id === catalog.attributeToSend) {
                        catalogToSave.fields.push(entry);
                    }
                });
                return $.param({metadata: angular.toJson([catalogToSave])});
            }
            return $.param({metadata: angular.toJson([catalog])});
        };

    return $resource('/admin-ng/event/:id/metadata:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        },
        save: {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformResponse: transformResponse,
            transformRequest:  transformRequest
        }
    });
}]);

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

/** Upload asset resource
 This resource performs the POST communication with the server
 to start the workflow to process the upload files  */
angular.module('adminNg.resources')
.factory('EventUploadAssetResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/assets', {}, {
        save: {
        method: 'POST',
        // By setting ‘Content-Type’: undefined, the browser sets the
        // Content-Type to multipart/form-data for us and fills in the
        // correct boundary. Manually setting ‘Content-Type’:
        // multipart/form-data will fail to fill in the boundary parameter
        // of the request.
        // multi-part file must be POST (not PUT) per ServletFileUpload.isMultipartContent
        headers: { 'Content-Type': undefined },
        responseType: 'text',
        transformResponse: [],
        transformRequest: function (data) {
                var workflowConfiguration = {};

                if (angular.isUndefined(data)) {
                    return data;
                }
                // The end point expects a multipart request payload with two fields
                // 1. A form field called 'metadata' containing asset data
                // 2. A non-form field which contains a File object
                var fd = new FormData(), assets, tempAssetList = [], flavorList = [];
                var assetMetadata = data['metadata'].assets;
                if (data['upload-asset']) {
                  assets = data['upload-asset'];
                }

                if (assets) {
                  angular.forEach(assets, function(files, name) {
                    angular.forEach(files, function (file, index) {
                      fd.append(name + "." + index, file);
                      tempAssetList.push(name);
                    });
                  });
                }

                // get source flavors
               assetMetadata.forEach(function(dataItem) {
                   if (tempAssetList.indexOf(dataItem.id) >= 0) {
                     flavorList.push(dataItem.flavorType + "/" + dataItem.flavorSubType);
                     // Special case to flag workflow to skip the "search+preview" image operation.
                     // If more than one special case comes up in the future,
                     // consider generalizing variable creation with
                     //   camelCase('uploaded', flavor, subflavor)
                     if (dataItem.flavorSubType == "search+preview") {
                       workflowConfiguration["uploadedSearchPreview"] = "true";
                     }
                   }
                });

                // set workflow boolean param and flavor list param
                if (flavorList.length > 0) {
                    workflowConfiguration["downloadSourceflavorsExist"] = "true";
                    workflowConfiguration["download-source-flavors"] = flavorList.join(", ");
                }

                // Add metadata form field
                fd.append('metadata', JSON.stringify({
                    assets: {
                      options: assetMetadata
                    },
                    processing: {
                      workflow: data['workflow'],
                      configuration: workflowConfiguration
                    }
                }));

                return fd;
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('NewEventMetadataResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var metadata = {}, result = {};
        try {
            metadata = JSON.parse(data);
            angular.forEach(metadata, function (md) {
                result[md.flavor] = md;
                result[md.flavor].title = md.title;
            });
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/event/new/metadata', {}, {
        get: { method: 'GET', transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('NewEventProcessingResource', ['$resource', function ($resource) {
    var transform = function (data) {

        var result = JSON.parse(data);

        return result;

    };

    return $resource('/admin-ng/event/new/processing', {}, {
        get: { method: 'GET', isArray: false, transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('NewEventResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    return $resource('/admin-ng/event/new', {}, {
        save: {
            method: 'POST',

            // By setting ‘Content-Type’: undefined, the browser sets the
            // Content-Type to multipart/form-data for us and fills in the
            // correct boundary. Manually setting ‘Content-Type’:
            // multipart/form-data will fail to fill in the boundary parameter
            // of the request.
            headers: { 'Content-Type': undefined },

            responseType: 'text',

            transformResponse: [],

            transformRequest: function (data) {

                console.log(JSON.stringify(data));

                if (angular.isUndefined(data)) {
                    return data = [];
                }

                // The end point expects a multipart request payload with two fields
                // 1. A form field called 'metadata' containing all userdata
                // 2. A non-form field either called 'presenter', 'presentation' or
                //    'audio' which contains a File object
                // 3. Optional asset config mapping and asset upload files
                var fd = new FormData(), source, sourceType = data.source.type, assetConfig, tempAssetList = [], flavorList = [];

                // If asset upload files exist, add asset mapping defaults
                // IndexServiceImpl processes the catalog or attachment based on the asset metadata map
                if (data['upload-asset'] && data['upload-asset'].assets) {
                  assetConfig = data['upload-asset'].defaults;
                }

                source = {
                    type: sourceType
                };

                if (sourceType !== 'UPLOAD') {
                    source.metadata = {
                        start: JsHelper.toZuluTimeString(data.source[sourceType].start),
                        device: data.source[sourceType].device.name,
                        inputs: (function (inputs) {
                            var result = '';
                            angular.forEach(inputs, function (enabled, inputId) {
                                if (enabled) {
                                    result += inputId + ',';
                                }
                            });
                            // Remove the trailing comma for the last input
                            result = result.substring(0, result.length - 1);
                            return result;
                        })(data.source[sourceType].device.inputMethods)
                    };
                }

                if (sourceType === 'SCHEDULE_SINGLE') {
                    source.metadata.end = JsHelper.toZuluTimeString(data.source.SCHEDULE_SINGLE.start, data.source.SCHEDULE_SINGLE.duration);
                    source.metadata.duration = (
                        parseInt(data.source.SCHEDULE_SINGLE.duration.hour, 10) * 60 * 60 * 1000 +
                        parseInt(data.source.SCHEDULE_SINGLE.duration.minute, 10) * 60 * 1000
                    ).toString();
                }

                if (sourceType === 'SCHEDULE_MULTIPLE') {
                    // We need to set it to the end time and day so the last day will be used in the recurrance and the correct end time is used
                    // for the rest of the recordings.

                    source.metadata.duration = moment.duration(parseInt(data.source.SCHEDULE_MULTIPLE.duration.hour, 10), 'h')
                                                    .add(parseInt(data.source.SCHEDULE_MULTIPLE.duration.minute, 10), 'm')
                                                    .as('ms') + '';

                    source.metadata.end = JsHelper.toZuluTimeString(data.source.SCHEDULE_MULTIPLE.end);

                    source.metadata.rrule = (function (src) {
                        return JsHelper.assembleRrule(src.SCHEDULE_MULTIPLE);
                    })(data.source);
                }

                // Dynamic source config and multiple source per type allowed
                if (sourceType === 'UPLOAD') {
                    if (data.source.UPLOAD.tracks) {
                       angular.forEach(data.source.UPLOAD.tracks, function(files, name) {
                          angular.forEach(files, function (file, index) {
                             fd.append(name + "." + index, file);
                          });
                       });
                    }

                    if (data.source.UPLOAD.metadata.start) {
                        data.metadata[0].fields.push(data.source.UPLOAD.metadata.start);
                    }
                }

                if (assetConfig) {
                    angular.forEach(data['upload-asset'].assets, function(files, name) {
                        angular.forEach(files, function (file, index) {
                            fd.append(name + "." + index, file);
                            tempAssetList.push(name);
                        });
                    });
                    // special case to override creation of search preview when one is uploaded
                    assetConfig["options"].forEach(function(dataItem) {
                        if (tempAssetList.indexOf(dataItem.id) >= 0) {
                           flavorList.push(dataItem.flavorType + "/" + dataItem.flavorSubType);
                           if (dataItem.flavorSubType == "search+preview") {
                               data.processing.workflow.selection.configuration["uploadedSearchPreview"] = "true";
                           }
                        }
                    });
                }

                // set workflow boolean param and flavor list param
                if (flavorList.length > 0) {
                    data.processing.workflow.selection.configuration["downloadSourceflavorsExist"] = "true";
                    data.processing.workflow.selection.configuration["download-source-flavors"] = flavorList.join(", ");
                }

                // Remove useless information for the request
                angular.forEach(data.metadata, function (catalog) {
                    angular.forEach(catalog.fields, function (field) {
                        delete field.collection;
                        delete field.label;
                        delete field.presentableValue;
                        delete field.readOnly;
                        delete field.required;
                    });
                });

               // Add metadata form field
                fd.append('metadata', JSON.stringify({
                    metadata: data.metadata,
                    processing: {
                        workflow: data.processing.workflow.id,
                        configuration: data.processing.workflow.selection.configuration
                    },
                    access: data.access,
                    source: source,
                    assets: assetConfig
                }));

                return fd;
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('NewSeriesMetadataResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var metadata = {}, result = {};
        try {
            metadata = JSON.parse(data);
            angular.forEach(metadata, function (md) {
                result[md.flavor] = md;
                result[md.flavor].title = md.title;
            });
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/series/new/metadata', {}, {
        get: { method: 'GET', transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('NewSeriesThemeResource', ['$resource', function ($resource) {
    var transform = function (data) {
    	var result = {};
        try {
        	result = JSON.parse(data);
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/series/new/themes', {}, {
        get: { method: 'GET', isArray: false, transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('ConflictCheckResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    var transformRequest = function (data) {
        var result = {
                start: JsHelper.toZuluTimeString(data.start),
                device: data.device.id,
                duration: String(moment.duration(parseInt(data.duration.hour, 10), 'h')
                            .add(parseInt(data.duration.minute, 10), 'm')
                            .asMilliseconds())
            };

        if (data.weekdays) {
            result.end = JsHelper.toZuluTimeString({
                date: data.end.date,
                hour: data.start.hour,
                minute: data.start.minute
            }, data.duration);
            result.rrule = JsHelper.assembleRrule(data);
        } else {
            result.end = JsHelper.toZuluTimeString(data.start, data.duration);
        }

        if (data.eventId) {
       	    result.id = data.eventId;
        }

        return $.param({metadata: angular.toJson(result)});
    };

    return $resource('/admin-ng/event/new/conflicts', {}, {
        check: { method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: transformRequest }
    });
}]);

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

angular.module('adminNg.resources')
.factory('BulkDeleteResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/:resource/:endpoint', {resource: '@resource', endpoint: '@endpoint'}, {
        delete: {
            method: 'POST',
            transformRequest: function (data) {
                return '["' + data.eventIds.join('","') + '"]';
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventMediaResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var media = [];
        try {
            media = JSON.parse(data);

            //for every media file item we define the filename
            for(var i = 0; i < media.length; i++){
                var item = media[i];
                var url = item.url;
                item.mediaFileName = url.substring(url.lastIndexOf('/')+1).split('?')[0];
            }

        } catch (e) { }
        return media;
    };

    return $resource('/admin-ng/event/:id0/asset/media/media.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id'}, transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventAttachmentDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/attachment/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventMediaDetailsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var media = {};
        try {
            if (typeof data === 'string') {
                media = JSON.parse(data);
            } else {
                media = data;
            }
            media.video = { previews: [{uri: media.url}] };
            media.url = media.url.split('?')[0];
        } catch (e) {
            console.warn('Unable to parse JSON file: ' + e);
        }
        return media;
    };

    return $resource('/admin-ng/event/:id0/asset/media/:id2.json', {}, {
        get: { method: 'GET', isArray: false, transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventCatalogDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/catalog/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventParticipationResource', ['JsHelper', '$resource', function (JsHelper, $resource) {
    var transformResponse = function (data) {
        var metadata = {};

        try {
            metadata = JSON.parse(data);
            if (!angular.isString(metadata.opt_out)) {
                if (metadata.opt_out) {
                    metadata.opt_out = 'true';
                } else {
                    metadata.opt_out = 'false';
                }
            }
        } catch (e) { }

        return metadata;
    };

    return $resource('/admin-ng/event/:id/participation:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventPublicationDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/publication/:id2.json', {}, {
        get: { method: 'GET', isArray: false }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventPublicationsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/publication/publications.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id' } }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventSchedulingResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    var transformResponse = function (data) {
            var parsedData,
                startDate,
                endDate,
                duration,
                durationHours,
                durationMinutes;

            parsedData = JSON.parse(data);

            startDate = new Date(parsedData.start);
            endDate = new Date(parsedData.end);
            duration = (endDate - startDate) / 1000;
            durationHours = (duration - (duration % 3600)) / 3600;
            durationMinutes = (duration % 3600) / 60;


            parsedData.start = JsHelper.zuluTimeToDateObject(startDate);
            parsedData.end = JsHelper.zuluTimeToDateObject(endDate);
            parsedData.duration = {
                hour: durationHours,
                minute: durationMinutes
            };

            return parsedData;
        },
        transformRequest = function (data) {
            var result = data,
                start,
                end;

            if (angular.isDefined(data)) {
                start = JsHelper.toZuluTimeString(data.entries.start);
                end = JsHelper.toZuluTimeString(data.entries.start, data.entries.duration);
                result = $.param({scheduling: angular.toJson({
                    agentId: data.entries.agentId,
                    start: start,
                    end: end,
                    agentConfiguration: data.entries.agentConfiguration,
                })});
            }

            return result;
        };


    return $resource('/admin-ng/event/:id/scheduling:ext', { id: '@id' }, {
        get: {
            params: { ext: '.json' },
            method: 'GET',
            transformResponse: transformResponse
        },
        save: {
            method: 'PUT',
            responseType: 'text',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: transformRequest
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventWorkflowsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var workflows = {};
        try {
            workflows = JSON.parse(data);
        } catch (e) { }

        if (angular.isDefined(workflows.results)) {
            return {
                        entries: workflows.results,
                        scheduling: false
                    };
        } else {
            return {
                        workflow: workflows,
                        scheduling: true
            };
        }
    };

    return $resource('/admin-ng/event/:id/workflows:ext', { id: '@id' }, {
        get: {
          params: { ext: '.json' },
          method: 'GET',
          transformResponse: transform
        },
        save: {
            method: 'PUT',
            responseType: 'text',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                return $.param({configuration: angular.toJson(data.entries)});
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventTransactionResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var result = {};
        try {
              result.hasActiveTransaction = JSON.parse(data).active;
        } catch (e) { }
        return result;
    };

    return $resource('/admin-ng/event/:id/hasActiveTransaction', {}, {
        hasActiveTransaction: {
            method: 'GET',
            responseType: 'text',
            isArray: false,
            paramDefaults: { id: '@id'},
            transformResponse: transform
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventErrorsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var errors = {};
        try {
            errors = JSON.parse(data);
        } catch (e) { }
        return { entries: errors };
    };

    return $resource('/admin-ng/event/:id0/workflows/:id1/errors.json', {}, {
        get: { method: 'GET', transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventErrorDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/workflows/:id1/errors/:id2.json');
}]);


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

angular.module('adminNg.resources')
.factory('EventWorkflowDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/workflows/:id1:ext', undefined, {
        get: {
          params: { ext: '.json' },
          method: 'GET'
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventWorkflowOperationsResource', ['$resource', function ($resource) {
    var transform = function (data) {
        var operations = {};
        try {
            operations = JSON.parse(data);
        } catch (e) { }
        return { entries: operations };
    };

    /* FIXME Christoph: Change endpoints!!! */
    return $resource('/admin-ng/event/:id0/workflows/:id1/operations.json', {}, {
        get: { method: 'GET', transformResponse: transform },
        save: { method: 'POST', transformRequest: function (data) {
            return JSON.stringify(data.entries);
        }, transformResponse: transform }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventWorkflowOperationDetailsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/workflows/:id1/operations/:id2');
}]);

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

angular.module('adminNg.resources')
.factory('EventWorkflowActionResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/workflows/:wfId/action/:action', { id: '@id', wfId: '@wfId', action: '@action' }, {
    	save: {method:'PUT'}
    });
}]);


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

angular.module('adminNg.resources')
.factory('EventAttachmentsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/attachment/attachments.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id'}}
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventCatalogsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id0/asset/catalog/catalogs.json', {}, {
        get: { method: 'GET', isArray: true, paramDefaults: { id0: '@id' } }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventAssetsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/asset/assets.json', { id: '@id' }, {
        get: {
            method: 'GET',
            isArray: false
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('EventAccessResource', ['$resource', function ($resource) {
    var transform = function (data) {
            var metadata = {};
            try {
                metadata = JSON.parse(data);
            } catch (e) { }
            return metadata;
        },
        eventResource,
        accessResource,
        managedAclResource;

    eventResource = $resource('/admin-ng/event/:id/access.json', { id: '@id' }, {
        get: { method: 'GET', transformResponse: transform }
    });

    accessResource = $resource('/admin-ng/event/:id/access', { id: '@id' }, {
        save: { method: 'POST',
                isArray: true,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {

                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    return $.param({
                        acl      : angular.toJson({acl: data.acl}),
                        override : true
                    });
                }
        }
    });

    managedAclResource = $resource('/acl-manager/acl/:id', { id: '@id'}, {
        get: { method: 'GET'}
    });


    return {
        getManagedAcl : managedAclResource.get,
        get: eventResource.get,
        save: accessResource.save
    };
}]);

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

angular.module('adminNg.resources')
.factory('EventGeneralResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/general.json', { id: '@id' });
}]);

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

angular.module('adminNg.resources')
.factory('EventHasSnapshotsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/event/:id/hasSnapshots.json', { id: '@id' });
}]);

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

angular.module('adminNg.resources')
.factory('ToolsResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    return $resource('/admin-ng/tools/:id/:tool.json', { id: '@id' }, {
        get: {
            method: 'GET',
            transformResponse: function (json) {
                var data = JSON.parse(json);

                // Create a default segment spanning the entire track
                if (data.segments.length === 0) {
                    data.segments.push({
                        start: 0, end: +data.duration
                    });
                }

                // Sort segments by start attribute
                data.segments.sort(function (a, b) {
                    return a.start - b.start;
                });

                // Fill gap until the first segment
                if (data.segments[0].start > 0) {
                    data.segments.splice(0, 0, {
                        start: 0,
                        end:   data.segments[0].start,
                        deleted: true
                    });
                }
                // Fill gap behind the last segment
                if (data.segments[data.segments.length - 1].end < data.duration) {
                    data.segments.splice(data.segments.length, 0, {
                        start: data.segments[data.segments.length - 1].end,
                        end:   data.duration,
                        deleted: true
                    });
                }
                // Fill gaps between segments
                //dont use angular.forEach here see MH-11169
                for (var index=0;index<data.segments.length;index++) {
                    var previous = data.segments[index - 1];
                    var segmentStart=data.segments[index].start-1;
                    if (previous && previous.end < segmentStart) {
                        data.segments.splice(index, 0, {
                            start: previous.end,
                            end: data.segments[index].start,
                            deleted: true
                        });
                    }
                };
                // Select first segment by default
                data.segments[0].selected = true;

                // Add workflow deselector
                if (data.workflows && data.workflows.length) {
                    data.workflows.splice(0, 0, { name: 'No Workflow' });
                }

                return data;
            }
        },
        save: {
            method: 'POST',
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }

                var response = {}, segments = [];
                angular.forEach(data.segments, function (segment) {
                    delete segment.$$hashKey;
                    if (!segment.deleted) {
                        this.push(segment);
                    }
                }, segments);

                response.concat = {
                    segments: segments,
                    tracks:   JsHelper.map(data.tracks, 'id')
                };

                if (data.workflow) {
                    response.workflow = data.workflow;
                }

                return JSON.stringify(response);
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('CommentResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/:resource/:resourceId/:type/:id/:reply', {}, {
        save: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }
                return $.param(data);
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('UsersResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/users/:target', {}, {
        query: {
            method: 'GET',
            isArray: false,
            params : { target: 'users.json' },
            transformResponse: function (data) {
                var result = [], i = 0, parse;
                data = JSON.parse(data);

                parse = function (r) {
                    var row = {};
                    row.id = (angular.isDefined(r.personId) && r.personId !== -1) ? r.personId : r.username;
                    row.name = r.name;
                    row.username = r.username;
                    row.manageable = r.manageable;
                    row.rolesDict = {};
                    var roleNames = [];
                    angular.forEach(r.roles, function(role) {
                        roleNames.push(role.name);
                        row.rolesDict[role.name] = role;
                    });
                    row.roles = roleNames.join(", ");
                    row.provider = r.provider;
                    row.email = r.email;
                    if (!angular.isUndefined(r.blacklist)) {
                        row.blacklist_from = Language.formatDateTime('short', r.blacklist.start);
                        row.blacklist_to   = Language.formatDateTime('short', r.blacklist.end);
                    }
                    row.type = "USER";
                    return row;
                };

                for (i = 0; i < data.results.length; i++) {
                    result.push(parse(data.results[i]));
                }

                return {
                    rows   : result,
                    total  : data.total,
                    offset : data.offset,
                    count  : data.count,
                    limit  : data.limit
                };
            }
        },
        create: {
            params : { target: '' },
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }

                var parameters = {
                    username : data.username,
                    name     : data.name,
                    email    : data.email,
                    password : data.password
                };

                if (angular.isDefined(data.roles)) {
                    parameters.roles = angular.toJson(data.roles);
                }

                return $.param(parameters);
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('UserResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/users/:username.json', { }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        },
        update: {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              var parameters = {
                  username : data.username,
                  name     : data.name,
                  email    : data.email,
                  password : data.password
              };

              if (angular.isDefined(data.roles)) {
                parameters.roles = angular.toJson(data.roles);
              }

              return $.param(parameters);
          }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('GroupsResource', ['$resource', 'ResourceHelper', function ($resource, ResourceHelper) {
    return $resource('/admin-ng/groups/:ext', {}, {
        query: {
          method: 'GET',
          params: { ext: 'groups.json' },
          isArray: false,
          transformResponse: function (json) {
              return ResourceHelper.parseResponse(json, function (r) {
                  var row = {};
                  row.id = r.id;
                  row.description = r.description;
                  row.name = r.name;
                  row.role = r.role;
                  row.type = "GROUP";
                  return row;
              });
          }
        },
        create: {
          params: { ext: '' },
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              var parameters = {
                name  : data.name
              };

              if (angular.isDefined(data.description)) {
                parameters.description = data.description;
              }

              if (angular.isDefined(data.roles)) {
                parameters.roles = data.roles.join(',');
              }

              if (angular.isDefined(data.users)) {
                parameters.users = data.users.join(',');
              }

              return $.param(parameters);
          }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('GroupResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/groups/:id', { id: '@id' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        },
        save: {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              var parameters = {
                name  : data.name
              };

              if (angular.isDefined(data.description)) {
                parameters.description = data.description;
              }

              if (angular.isDefined(data.roles)) {
                parameters.roles = data.roles.join(',');
              }

              if (angular.isDefined(data.users)) {
                parameters.users = data.users.join(',');
              }

              return $.param(parameters);
        }
      }
    });
}]);

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

angular.module('adminNg.resources')
.factory('AclsResource', ['ResourceHelper', '$resource', function (ResourceHelper, $resource) {
    return $resource('/admin-ng/acl/:ext', {}, {
        query: {
            params: { ext: 'acls.json' },
            method: 'GET',
            isArray: false,
            transformResponse: function (data) {
              return ResourceHelper.parseResponse(data, function (r) {
                  var row = {};
                  row.id      = r.id;
                  row.name    = r.name;
                  row.created = 'TBD';
                  row.creator = 'TBD';
                  row.in_use  = 'TBD';
                  row.type    = "ACL";

                  return row;
              });
            }
        },
        create: {
          params: { ext: '' },
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              return $.param({
                  name : data.name,
                  acl  : JSON.stringify({acl: data.acl})
              });
          }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('AclResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/acl/:id', { id: '@id' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
            return JSON.parse(data);
          }
        },
        save: {
          method: 'PUT',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          transformRequest: function (data) {
              if (angular.isUndefined(data)) {
                  return data;
              }

              return $.param({
                  name : data.name,
                  acl  : JSON.stringify({acl: data.acl})
              });
        }
      }
    });
}]);

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

angular.module('adminNg.resources')
.factory('CaptureAgentResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/capture-agents/:name', { name: '@name' }, {
        get: {
          method: 'GET',
          transformResponse: function (data) {
              var raw = JSON.parse(data), agent = {}
              agent.name = raw.Name;
              agent.url = raw.URL;
              agent.status = raw.Status;
              agent.updated =  Language.formatDateTime('short', raw.Update);
              agent.inputs = raw.inputs;
              agent.capabilities = raw.capabilities;
              agent.configuration = raw.configuration;
              return agent;
          }
        }
      }
    );
}]);

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

angular.module('adminNg.resources')
.factory('CaptureAgentsResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/capture-agents/:target', {}, {
        query: {
            method: 'GET',
            isArray: false,
            params: {target: 'agents.json'},
            transformResponse: function (json) {
                var result = [], i = 0, parse, data;
                data = JSON.parse(json);

            parse = function (r) {
                var row = {};
                row.id = r.Name;
                row.status = r.Status;
                row.name = r.Name;
                row.updated = Language.formatDateTime('short', r.Update);
                row.inputs = r.inputs;
                row.roomId = r.roomId;
                row.type = "LOCATION";
                row.removable = ('offline' == r.Status) || ('unknown' == r.Status);
                return row;
            };

                for (; i < data.results.length; i++) {
                    result.push(parse(data.results[i]));
                }

                return {
                    rows: result,
                    total: data.total,
                    offset: data.offset,
                    count: data.count,
                    limit: data.limit
            };
        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('UserRolesResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/ROLES.json', {}, {
        query: {
          method: 'GET',
          isArray: true,
          transformResponse: function (data) {
            var result = [];

            data = JSON.parse(data);

            if (angular.isDefined(data)) {
              angular.forEach(data, function(value, key) {
                result.push({
                  name: key,
                  value: key,
                  type: value
                });
              });
            }

            return result;
          }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('ServersResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    return $resource('/admin-ng/server/servers.json', {}, {
        query: { method: 'GET', isArray: false, transformResponse: function (rawData) {
            var data = JSON.parse(rawData);
            var result = [];
            for (var i = 0; i < data.results.length; i++) {
                var row = data.results[i];
                row.id = row.name;

                row.meanRunTime = JsHelper.secondsToTime(row.meanRunTime);
                row.meanQueueTime = JsHelper.secondsToTime(row.meanQueueTime);

                result.push(row);
            }

            return {
                rows: result,
                total: data.total,
                offset: data.offset,
                count: data.count,
                limit: data.limit
            };
        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('ServicesResource', ['$resource', 'JsHelper', function ($resource, JsHelper) {
    // Note: This is the productive path
    return $resource('/admin-ng/services/services.json', {}, {
        query: { method: 'GET', isArray: false, transformResponse: function (data) {
            var result = [], i = 0, parse, payload;
            data = JSON.parse(data);
            payload = data.results;

            parse = function (r) {
                r.action = '';

                r.meanRunTime = JsHelper.secondsToTime(r.meanRunTime);
                r.meanQueueTime = JsHelper.secondsToTime(r.meanQueueTime);

                return r;
            };

            for (; i < payload.length; i++) {
                result.push(parse(payload[i]));
            }

            return {
                rows: result,
                total: data.total,
                offset: data.offset,
                count: data.count,
                limit: data.limit
            };
        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('ServiceResource', ['$resource', function ($resource) {
    return $resource('/services/:resource', {}, {
        // Parameters:
        // * host: The host name, including the http(s) protocol
        // * maintenance: Whether this host should be put into maintenance mode (true) or not
        setMaintenanceMode: {
            method: 'POST',
            params: { resource: 'maintenance' },
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }
                return $.param(data);
            }
        },

        // Parameters:
        // * host: The host providing the service, including the http(s) protocol
        // * serviceType: The service type identifier
        sanitize: {
            method: 'POST',
            params: { resource: 'sanitize' },
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data)) {
                    return data;
                }
                return $.param(data);
            }
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('JobsResource', ['$resource', 'Language', function ($resource, Language) {
    return $resource('/admin-ng/job/jobs.json', {}, {
        query: { method: 'GET', isArray: false, transformResponse: function (json) {
            var result = [], i = 0, parse, data;
            data = JSON.parse(json);

            parse = function (r) {
                var row = {};
                row.id = r.id
                row.operation = r.operation;
                row.type = r.type;
                row.status = r.status;
                row.submitted = Language.formatDateTime('short', r.submitted);
                row.started = Language.formatDateTime('short', r.started);
                row.creator = r.creator;
                row.processingHost = r.processingHost;
                return row;
            };

            for (; i < data.results.length; i++) {
                result.push(parse(data.results[i]));
            }

            return {
                rows: result,
                total: data.total,
                offset: data.offset,
                count: data.count,
                limit: data.limit
            };
        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('ResourcesFilterResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/:resource/filters.json', {}, {
        get: { method: 'GET', transformResponse: function (data) {
            var filters = {};
            try {
                filters = JSON.parse(data);
                for (var key in filters) {
                    if (!filters[key].options) {
                        continue;
                    }
                    var filterArr = [];
                    var options = filters[key].options;
                    for (var subKey in options) {
                        filterArr.push({value: subKey, label: options[subKey]});
                    }
                    filterArr = filterArr.sort(function(a, b) {
                                    if (a.label.toLowerCase() < b.label.toLowerCase()) return -1;
                                    if (a.label.toLowerCase() > b.label.toLowerCase()) return 1;
                                    return 0;
                                });
                    filters[key].options = filterArr;
                }
            } catch (e) { }
            return { filters: filters };
        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('ResourcesListResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/resources/:resource.json', {}, {
        query: {
            method: 'GET',
            isArray: true,
            transformResponse: function (data) {
              var result = [];

              data = JSON.parse(data);

              if (angular.isDefined(data)) {
                angular.forEach(data, function(value, key) {

                  result.push({
                    name: key,
                    value: value
                  });

                });
              }

              return result;
            }
        }
      });
}]);

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

angular.module('adminNg.resources')
.factory('WorkflowsResource', ['$resource', function ($resource) {
    return $resource('/workflow/definitions.json', {}, {
        get: {method: 'GET', isArray: true, transformResponse: function (data) {
            var parsed = JSON.parse(data);
            if (parsed && parsed.definitions && parsed.definitions.definition) {
                return parsed.definitions.definition;
            }
            return [];
        }}
    });
}]);

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

angular.module('adminNg.resources')
.factory('BlacklistsResource', ['$resource', '$filter', 'Language', 'JsHelper',
function ($resource, $filter, Language, JsHelper) {

    var parse = function (r, type) {
        var row = {};
        row.id            = r.id;
        row.resourceName  = r.resource.name;
        row.resourceId    = r.resource.id;
        row.date_from     = Language.formatDateTime('short', r.start);
        row.date_to       = Language.formatDateTime('short', r.end);
        row.date_from_raw = Language.formatDateTimeRaw('short', r.start);
        row.date_to_raw   = Language.formatDateTimeRaw('short', r.end);
        row.reason        = r.purpose;
        row.comment       = r.comment;
        row.type          = $filter('uppercase')(type);

        return row;
    };

    return function (type) {
        return {
            query: {
                method: 'GET',
                isArray: false,
                params: { type: type, id: 'blacklists.json' },
                transformResponse: function (data) {
                    var result = [];
                    angular.forEach(JSON.parse(data), function (item, type) {
                        result.push(parse(item, type));
                    });

                    return {
                        rows   : result,
                        total  : data.total,
                        offset : data.offset,
                        count  : data.count,
                        limit  : data.limit
                    };
                }
            },
            get: {
                method: 'GET',
                params: { type: type },
                transformResponse: function (data) {
                    return parse(JSON.parse(data));
                }
            },
            save: {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {
                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    var request = {};

                    request.type = type;
                    if (data.items.items.length) {
                        request.blacklistedId = data.items.items[0].id;
                    }
                    request.start = JsHelper.toZuluTimeString({
                        date: data.dates.fromDate,
                        hour: data.dates.fromTime.split(':')[0],
                        minute: data.dates.fromTime.split(':')[1]
                    });
                    request.end = JsHelper.toZuluTimeString({
                        date: data.dates.toDate,
                        hour: data.dates.toTime.split(':')[0],
                        minute: data.dates.toTime.split(':')[1]
                    });
                    request.purpose = data.reason.reason;
                    request.comment = data.reason.comment;

                    return $.param(request);
                }
            },
            update: {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                transformRequest: function (data) {
                    if (angular.isUndefined(data)) {
                        return data;
                    }

                    var request = {};

                    request.type = type;
                    if (data.items.items.length) {
                        request.blacklistedId = data.items.items[0].id;
                    }
                    request.start = JsHelper.toZuluTimeString({
                        date: data.dates.fromDate,
                        hour: data.dates.fromTime.split(':')[0],
                        minute: data.dates.fromTime.split(':')[1]
                    });
                    request.end = JsHelper.toZuluTimeString({
                        date: data.dates.toDate,
                        hour: data.dates.toTime.split(':')[0],
                        minute: data.dates.toTime.split(':')[1]
                    });
                    request.purpose = data.reason.reason;
                    request.comment = data.reason.comment;

                    return $.param(request);
                }
            }
        };
    };
}])
.factory('UserBlacklistsResource', ['$resource', 'BlacklistsResource',
function ($resource, BlacklistsResource) {
    return $resource('/blacklist/:id', {}, BlacklistsResource('person'));
}])
.factory('LocationBlacklistsResource', ['$resource', 'BlacklistsResource',
function ($resource, BlacklistsResource) {
    return $resource('/blacklist/:id', {}, BlacklistsResource('room'));
}]);

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

angular.module('adminNg.resources')
.factory('BlacklistCountResource', ['$resource', function ($resource) {
    return $resource('/blacklist/blacklistCount', {}, {
        save: {
            method: 'GET',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
        }
    });
}]);

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

angular.module('adminNg.resources')

    /**
     *
     */
    .factory('ThemesResource', ['$resource', 'Language', 'ResourceHelper',
        function ($resource, Language, ResourceHelper) {

            var themesConverter = function (r) {
                var row = {};
                row.id = r.id;
                row.name = r.name;
                row.description = r.description;
                row.default = r.default;
                row.creator = r.creator;
                row.creation_date = Language.formatDate('short', r.creationDate);
                row.usage = r.usage;
                row.type = "THEME";
                return row;
            };

            return $resource('/admin-ng/themes/themes.json', {}, {

                /**
                 * Returns a list of themes mainly used by the table implementation.
                 */
                query: {method: 'GET', isArray: false, transformResponse: function (json) {
                    return ResourceHelper.parseResponse(json, themesConverter);
                }}
            });
        }])
    /**
     * Supports queries for series assigned to a given theme.
     */
    .factory('ThemeUsageResource', ['$resource',
        function ($resource) {
            return $resource('/admin-ng/themes/:themeId/usage.json', {});
        }])

    /**
     * Supports CRUD operations on themes.
     */
    .factory('ThemeResource', ['$resource',  'Language', 'ResourceHelper',
        function ($resource) {
            return $resource('/admin-ng/themes/:id:format', {}, {

                /**
                 * Returns a list of themes mainly used by the table implementation.
                 */
                get: {method: 'GET', isArray: false, transformResponse: function (json) {
                    var data = JSON.parse(json), result = {};
                    result.general = {
                        'name': data.name,
                        'description': data.description,
                        'default': data.default
                    };
                    result.bumper = {
                        'active': data.bumperActive,
                        'file': {
                            id: data.bumperFile,
                            name: data.bumperFileName,
                            url: data.bumperFileUrl
                        }
                    };
                    result.trailer = {
                        'active': data.trailerActive,
                        'file': {
                            id: data.trailerFile,
                            name: data.trailerFileName,
                            url: data.trailerFileUrl
                        }
                    };
                    result.titleslide = {
                        active: data.titleSlideActive,
                        file: {
                            id: data.titleSlideBackground,
                            name: data.titleSlideBackgroundName,
                            url: data.titleSlideBackgroundUrl
                        },
                        mode: data.titleSlideBackground ? 'upload' : 'extract'
                    },
                    result.license = {
                        active: data.licenseSlideActive,
                        backgroundImageActive: angular.isDefined(data.licenseSlideBackground),
                        description: data.licenseSlideDescription,
                        file: {
                            id: data.licenseSlideBackground,
                            name: data.licenseSlideBackgroundName,
                            url: data.licenseSlideBackgroundUrl
                        }
                    };
                    result.watermark = {
                        active: data.watermarkActive,
                        file: {
                            id: data.watermarkFile,
                            name: data.watermarkFileName,
                            url: data.watermarkFileUrl
                        },
                        position: data.watermarkPosition
                    };
                    return result;
                }},
                update: {
                    method: 'PUT',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    transformRequest: function (data) {
                        var d = {};
                        if (angular.isUndefined(data)) {
                            return data;
                        }
                        // Temporarily commented out because the backend is not yet ready
                        //d.default = data.general.default.toString();
                        d.description = data.general.description;
                        d.name = data.general.name;

                        d.bumperActive = data.bumper.active.toString();
                        if (data.bumper.active) {
                            d.bumperFile = data.bumper.file.id.toString();
                        }

                        d.trailerActive = data.trailer.active.toString();
                        if (data.trailer.active) {
                            d.trailerFile = data.trailer.file.id.toString();
                        }

                        d.titleSlideActive = data.titleslide.active.toString();
                        if (data.titleslide.active) {
                            if ('upload' === data.titleslide.mode) {
                                d.titleSlideBackground = data.titleslide.file.id.toString();
                            }
                        }

                        d.licenseSlideActive = data.license.active.toString();
                        if (data.license.active) {
                            d.licenseSlideDescription = data.license.description;
                            if (data.license.backgroundImage) {
                                d.licenseSlideBackground = data.license.file.id.toString();
                            }
                        }
                        d.watermarkActive = data.watermark.active.toString();
                        if (data.watermark.active) {
                            d.watermarkFile = data.watermark.file.id.toString();
                            d.watermarkPosition = data.watermark.position;
                        }

                        return $.param(d);

                    }
                }
            });
        }]);




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

angular.module('adminNg.resources')
    .factory('NewThemeResource', ['$resource', function ($resource) {

        return $resource('/admin-ng/themes', {}, {
                save: {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},

                    transformRequest: function (data) {
                        var d = {};
                        if (angular.isUndefined(data)) {
                            return data;
                        }
                        // Temporarily commented out - the backend is not yet ready.
                        //d.default = data.general.default;
                        d.description = data.general.description;
                        d.name = data.general.name;

                        d.bumperActive = data.bumper.active;
                        if (data.bumper.active) {
                            d.bumperFile = data.bumper.file.id;
                        }

                        d.trailerActive = data.trailer.active;
                        if (data.trailer.active) {
                            d.trailerFile = data.trailer.file.id;
                        }

                        d.titleSlideActive = data.titleslide.active;
                        if (data.titleslide.active) {

                            if ('upload' === data.titleslide.mode) {
                                d.titleSlideBackground = data.titleslide.file.id;
                            }
                        }

                        d.licenseSlideActive = data.license.active;
                        if (data.license.active) {
                            d.licenseSlideDescription = data.license.description;
                            if (data.license.backgroundImage) {
                                d.licenseSlideBackground = data.license.file.id;
                            }
                        }
                        d.watermarkActive = data.watermark.active;
                        if (data.watermark.active) {
                            d.watermarkFile = data.watermark.file.id;
                            d.watermarkPosition = data.watermark.position;
                        }

                        return $.param(d);
                    }
                }
            }
        );
    }
    ]);

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

angular.module('adminNg.resources')
.factory('SignatureResource', ['$resource', function ($resource) {
    var transformRequest = function (data) {
        var request = {};
        request.username = data.username;
        request.from_name = request.reply_name = data.name;
        request.from_address = data.replyAddress = data.sender.address;
        request.text = data.signature;
        request.name = data.name;
        return $.param(request);
    };

    return $resource('/admin-ng/user-settings/signature/:id', {id: '@id'}, {
        get: {
            method: 'GET',
            transformResponse: function (data) {
                try {
                    data = JSON.parse(data);
                } catch (e) {
                    return {
                        replyTo: {},
                        existsOnServer: false
                    };
                }
                data.existsOnServer = true;
                return data;
            }
        },
        update: {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            params: {
                id: '@id'
            },
            transformRequest: transformRequest
        },
        save: {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            transformRequest: transformRequest
        }
    });
}]);

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

angular.module('adminNg.resources')
.factory('TaskResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/tasks/new', {}, {
        save: {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            transformRequest: function (data) {
                return $.param({
                    metadata: JSON.stringify(data)
                });
            },
            isArray: true
        }
    });
}]);

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
.factory('NewEventAccess', ['ResourcesListResource', 'EventAccessResource', 'SeriesAccessResource', 'AuthService', 'UserRolesResource', 'Notifications', '$timeout',
    function (ResourcesListResource, EventAccessResource, SeriesAccessResource, AuthService, UserRolesResource, Notifications, $timeout) {
    var Access = function () {

        var roleSlice = 100;
        var roleOffset = 0;
        var loading = false;
        var rolePromise = null;

        var me = this;
        var NOTIFICATION_CONTEXT = 'events-access';
        var createPolicy = function (role) {
            return {
                role  : role,
                read  : false,
                write : false,
                actions : {
                    name : 'new-event-acl-actions',
                    value : []
                }
            };
        };

        var addUserRolePolicy = function (policies) {
            if (angular.isDefined(AuthService.getUserRole())) {
                var currentUserPolicy = createPolicy(AuthService.getUserRole());
                currentUserPolicy.read = true;
                currentUserPolicy.write = true;
                policies.push(currentUserPolicy);
            }
            return policies;
        };

        var changePolicies = function (access, loading) {
            var newPolicies = {},
                foundUserRole = false;
            angular.forEach(access, function (acl) {
                var policy = newPolicies[acl.role];

                if (angular.isUndefined(policy)) {
                    newPolicies[acl.role] = createPolicy(acl.role);
                }

                if (acl.action === 'read' || acl.action === 'write') {
                    newPolicies[acl.role][acl.action] = acl.allow;
                } else if (acl.allow === true || acl.allow === 'true'){
                    // Handle additional ACL actions
                    newPolicies[acl.role].actions.value.push(acl.action);
                }

                if (acl.role === AuthService.getUserRole()) {
                    foundUserRole = true;
                }

                if (angular.isUndefined(me.roles[acl.role])) {
                    me.roles[acl.role] = acl.role;
                }
            });

            me.ud.policies = [];
            // Add user role if not already present in Series ACL
            if (!foundUserRole) {
                me.ud.policies = addUserRolePolicy(me.ud.policies);
            }
            angular.forEach(newPolicies, function (policy) {
                me.ud.policies.push(policy);
            });

            if (!loading) {
                me.ud.accessSave();
            }
        };

        var checkNotification = function () {
            if (me.unvalidRule) {
                if (!angular.isUndefined(me.notificationRules)) {
                    Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                }
                me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRules)) {
                Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                me.notificationRules = undefined;
            }

            if (!me.hasRights) {
                if (!angular.isUndefined(me.notificationRights)) {
                    Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                }
                me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRights)) {
                Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                me.notificationRights = undefined;
            }

            $timeout(function () {
                checkNotification();
             }, 200);
        };

        me.isAccessState = true;
        me.ud = {};
        me.ud.id = {};
        me.ud.policies = [];
        // Add the user's role upon creating the ACL editor
        me.ud.policies = addUserRolePolicy(me.ud.policies);
        me.ud.baseAcl = {};

        this.setMetadata = function (metadata) {
            me.metadata = metadata;
            me.loadSeriesAcl();
        };

        this.loadSeriesAcl = function () {
            angular.forEach(me.metadata.getUserEntries(), function (m) {
                if (m.id === 'isPartOf' && angular.isDefined(m.value) && m.value !== '') {
                    SeriesAccessResource.get({ id: m.value }, function (data) {
                        if (angular.isDefined(data.series_access)) {
                            var json = angular.fromJson(data.series_access.acl);
                            changePolicies(json.acl.ace, true);
                        }
                    });
                }
            });
        };

        this.changeBaseAcl = function () {
            var newPolicies = {};
            me.ud.baseAcl = EventAccessResource.getManagedAcl({id: me.ud.id}, function () {
                angular.forEach(me.ud.baseAcl.acl.ace, function (acl) {
                    var policy = newPolicies[acl.role];

                    if (angular.isUndefined(policy)) {
                        newPolicies[acl.role] = createPolicy(acl.role);
                    }
                    if (acl.action === 'read' || acl.action === 'write') {
                        newPolicies[acl.role][acl.action] = acl.allow;
                    } else if (acl.allow === true || acl.allow === 'true'){
                        newPolicies[acl.role].actions.value.push(acl.action);
                    }
                });

                me.ud.policies = [];
                // After loading an ACL template add the user's role to the top of the ACL list if it isn't included
                if (angular.isDefined(AuthService.getUserRole()) && !angular.isDefined(newPolicies[AuthService.getUserRole()])) {
                    me.ud.policies = addUserRolePolicy(me.ud.policies);
                }
                angular.forEach(newPolicies, function (policy) {
                    me.ud.policies.push(policy);
                });

                me.ud.id = '';
            });
        };

        this.addPolicy = function () {
            me.ud.policies.push(createPolicy());
        };

        this.deletePolicy = function (policyToDelete) {
            var index;

            angular.forEach(me.ud.policies, function (policy, idx) {
                if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
                    index = idx;
                }
            });

            if (angular.isDefined(index)) {
                me.ud.policies.splice(index, 1);
            }
        };

        this.isValid = function () {
             var hasRights = false,
                rulesValid = true;

             angular.forEach(me.ud.policies, function (policy) {
                rulesValid = false;

                if (policy.read && policy.write) {
                    hasRights = true;
                }

                if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
                    rulesValid = true;
                }
             });

            me.unvalidRule = !rulesValid;
            me.hasRights = hasRights;

            return rulesValid && hasRights;
        };

        checkNotification();

        me.acls  = ResourcesListResource.get({ resource: 'ACL' });
        me.actions = {};
        me.hasActions = false;
        ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
            angular.forEach(data, function (value, key) {
                if (key.charAt(0) !== '$') {
                    me.actions[key] = value;
                    me.hasActions = true;
                }
            });
        });

        me.roles = {};

        me.getMoreRoles = function (value) {

            if (me.loading)
                return rolePromise;

            me.loading = true;
            var queryParams = {limit: roleSlice, offset: roleOffset};

            if ( angular.isDefined(value) && (value != "")) {
                //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
                //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
                queryParams["filter"] = "role_name:"+ value +",role_target:ACL";
                queryParams["offset"] = 0;
            } else {
                queryParams["filter"] = "role_target:ACL";
            }
            rolePromise = UserRolesResource.query(queryParams);
            rolePromise.$promise.then(function (data) {
                angular.forEach(data, function (role) {
                    me.roles[role.name] = role.value;
                });
                roleOffset = Object.keys(me.roles).length;
            }).finally(function () {
                me.loading = false;
            });
            return rolePromise;
        };

        me.getMoreRoles();

        this.reset = function () {
            me.ud = {
                id: {},
                policies: []
            };
            // Add user role after reset
            me.ud.policies = addUserRolePolicy(me.ud.policies);
        };

        this.reset();
    };
    return new Access();
}]);

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
.factory('NewEventMetadata', ['NewEventMetadataResource', function (NewEventMetadataResource) {
    var Metadata = function () {
        var me = this, mainMetadataName = 'dublincore/episode', i;
        me.ud = {};
        me.isMetadataState = true;

        this.requiredMetadata = {};

        // As soon as the required metadata fields arrive from the backend,
        // we check which are mandatory.
        // This information will be needed in ordert to tell if we can move
        // on to the next page of the wizard.
        this.findRequiredMetadata = function (data) {
            var mainData = data[mainMetadataName];
            // we go for the regular dublincore metadata here
            me.ud[mainMetadataName] = mainData;
            if (mainData && mainData.fields) {
                for (i = 0; i < mainData.fields.length; i++) {
                    mainData.fields[i].tabindex = i + 1; // just hooking the tab index up here, as this is already running through all elements
                    if (mainData.fields[i].required) {
                        me.requiredMetadata[mainData.fields[i].id] = false;
                        if (mainData.fields[i].type === 'boolean') {
                            // set all boolean fields to false by default
                            mainData.fields[i].value = false;
                            me.requiredMetadata[mainData.fields[i].id] = true;
                        }
                    }
                }
            }
        };

        this.metadata = NewEventMetadataResource.get(this.findRequiredMetadata);

        // Checks if the current state of this wizard is valid and we are
        // ready to move on.
        this.isValid = function () {
            var result = true;
            //FIXME: The angular validation should rather be used,
            // unfortunately it didn't work in this context.
            angular.forEach(me.requiredMetadata, function (item) {
                if (item === false) {
                    result = false;
                }
            });
            return result;
        };

        this.save = function (scope) {
            //FIXME: This should be nicer, rather propagate the id and values
            //instead of looking for them in the parent scope.
            var params = scope.$parent.params,
                fieldId = params.id,
                value = params.value;

            if (params.collection) {
                if (angular.isArray(value)) {
                    var presentableValue = '';

                    angular.forEach(value, function (item, index) {
                        presentableValue += item;
                        if ((index + 1) < value.length) {
                            presentableValue += ', ';
                        }
                    });

                    params.presentableValue = presentableValue;
                } else {
                    params.presentableValue = params.collection[value];
                }
            } else {
                params.presentableValue = value;
            }

            me.ud[mainMetadataName].fields[fieldId] = params;

            if (!angular.isUndefined(me.requiredMetadata[fieldId])) {
                if (angular.isDefined(value) && value.length > 0) {
                    // we have received a required value
                    me.requiredMetadata[fieldId] = true;
                } else {
                    // the user has deleted the value
                    me.requiredMetadata[fieldId] = false;
                }
            }
        };

        this.reset = function () {
            me.ud = {};
            me.metadata = NewEventMetadataResource.get(this.findRequiredMetadata);
        };

        this.getUserEntries = function () {
            if (angular.isDefined(me.ud[mainMetadataName])) {
                return me.ud[mainMetadataName].fields;
            } else {
                return {};
            }
        };
    };

    return new Metadata();
}]);

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
.factory('NewEventMetadataExtended', ['NewEventMetadataResource', function (NewEventMetadataResource) {
    var MetadataExtended = function () {
        var me = this, i;
        this.ud = {};
        this.requiredMetadata = {};

        me.isMetadataExtendedState = true;

        // As soon as the required metadata fields arrive from the backend,
        // we check which are mandatory.
        // This information will be needed in order to tell if we can move
        // on to the next page of the wizard.
        this.postProcessMetadata = function (data) {
            var fields = [], chunk;
            for (chunk in data) {
                if (data.hasOwnProperty(chunk)) {
                    // extended metadata is every object in the returned data which
                    // does not start with a dollar sign and which isn't dublincore/episode
                    if (chunk !== 'dublincore/episode' && chunk.charAt(0) !== '$') {
                        me.ud[chunk] = {fields: data[chunk].fields};
                        me.ud[chunk].flavor = data[chunk].flavor;
                        me.ud[chunk].title = data[chunk].title;
                        fields = fields.concat(data[chunk].fields);
                    }
                }
            }
            // we go for the extended metadata here
            if (fields.length > 0) {
                for (i = 0; i < fields.length; i++) {
                    if (fields[i].required) {
                        me.requiredMetadata[fields[i].id] = false;
                        if (fields[i].type === 'boolean') {
                            // set all boolean fields to false by default
                            fields[i].value = false;
                            // remark: since booleans are always present, we
                            // do not add this property to the required fields.
                        }
                    }
                }
                me.visible = true;
            }
            else {
                me.visible = false;
            }
        };

        this.metadata = NewEventMetadataResource.get(this.postProcessMetadata);

        // Checks if the current state of this wizard is valid and we are
        // ready to move on.
        this.isValid = function () {
            var result = true;
            //FIXME: The angular validation should rather be used,
            // unfortunately it didn't work in this context.
            angular.forEach(me.requiredMetadata, function (item) {
                if (item === false) {
                    result = false;
                }
            });
            return result;
        };

        this.save = function (scope) {
            //FIXME: This should be nicer, rather propagate the id and values
            //instead of looking for them in the parent scope.
            var params = scope.$parent.params,
                target = scope.$parent.target,
                fieldId = params.id,
                value = params.value;

            if (params.collection) {
                if (angular.isArray(value)) {
                    params.presentableValue = value;
                } else {
                    params.presentableValue = params.collection[value];
                }
            } else {
                params.presentableValue = value;
            }

            me.ud[target].fields[fieldId] = params;

            if (!angular.isUndefined(me.requiredMetadata[fieldId])) {
                if (angular.isDefined(value) && value.length > 0) {
                    // we have received a required value
                    me.requiredMetadata[fieldId] = true;
                } else {
                    // the user has deleted the value
                    me.requiredMetadata[fieldId] = false;
                }
            }
        };

        this.reset = function () {
            me.ud = {};
            me.metadata = NewEventMetadataResource.get(me.postProcessMetadata);
        };

        this.getFiledCatalogs = function () {
            var catalogs = [];

            angular.forEach(me.ud, function(catalog) {
                var empty = true;
                angular.forEach(catalog.fields, function (field) {
                    if (angular.isDefined(field.presentableValue) && field.presentableValue !=='') {
                        empty = false;
                    }
                });

                if (!empty) {
                    catalogs.push(catalog);
                }
            });

            return catalogs;
        };

        this.getUserEntries = function () {
            angular.forEach(me.ud, function(catalog) {
                catalog.empty = true;
                angular.forEach(catalog.fields, function (field) {
                    if (angular.isDefined(field.presentableValue) && field.presentableValue !=='') {
                        catalog.empty = false;
                    }
                });
            });

            return me.ud;
        };
    };

    return new MetadataExtended();
}]);

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
.factory('NewEventProcessing', ['$sce', 'NewEventProcessingResource', function ($sce, NewEventProcessingResource) {
    var Processing = function (use) {
        // Update the content of the configuration panel with the given HTML
        var me = this, queryParams,
            updateConfigurationPanel = function (html) {
                if (angular.isUndefined(html)) {
                    html = '';
                }
                me.workflowConfiguration = $sce.trustAsHtml(html);
            },
            isWorkflowSet = function () {
                return angular.isDefined(me.ud.workflow) && angular.isDefined(me.ud.workflow.id);
            },
            idConfigElement = '#new-event-workflow-configuration',
            workflowConfigEl = angular.element(idConfigElement);

        this.isProcessingState = true;
        this.ud = {};
        this.ud.workflow = {};

        // Object used for the workflow configurations
        window.ocWorkflowPanel = {};

        // Load all the workflows definitions
        if (use === 'tasks') {
            queryParams = {
                    tags: 'archive'
                };
        } else if (use === 'delete-event') {
            queryParams = {
                    tags: 'delete'
            };
        } else {
            queryParams = {
                tags: 'upload,schedule'
            };

        }
        NewEventProcessingResource.get(queryParams, function (data) {

            me.changingWorkflow = true;

            me.workflows = data.workflows;
            var default_workflow_id = data.default_workflow_id;

            // set default workflow as selected
            if(angular.isDefined(default_workflow_id)){

                for(var i = 0; i < me.workflows.length; i += 1){
                    var workflow = me.workflows[i];

                    if (workflow.id === default_workflow_id){
                      me.ud.workflow = workflow;
                      updateConfigurationPanel(me.ud.workflow.configuration_panel);
                      me.save();
                      break;
                    }
                }
            }
          me.changingWorkflow = false;

        });

        // Listener for the workflow selection
        this.changeWorkflow = function () {
            me.changingWorkflow = true;
            workflowConfigEl = angular.element(idConfigElement);
            if (angular.isDefined(me.ud.workflow)) {
                updateConfigurationPanel(me.ud.workflow.configuration_panel);
            } else {
                updateConfigurationPanel();
            }
            me.save();
            me.changingWorkflow = false;
        };

        // Get the workflow configuration
        this.getWorkflowConfig = function () {
            var workflowConfig = {}, element, isRendered = workflowConfigEl.find('.configField').length > 0;

            if (!isRendered) {
                element = angular.element(me.ud.workflow.configuration_panel).find('.configField');
            } else {
                element = workflowConfigEl.find('.configField');
            }

            element.each(function (idx, el) {
                var element = angular.element(el);

                if (angular.isDefined(element.attr('id'))) {
                    if (element.is('[type=checkbox]') || element.is('[type=radio]')) {
                        workflowConfig[element.attr('id')] = element.is(':checked') ? 'true' : 'false';
                    } else {
                        workflowConfig[element.attr('id')] = element.val();
                    }
                }
            });

            return workflowConfig;
        };

        this.isValid = function () {
            if (isWorkflowSet()) {
                return true;
            } else {
                return false;
            }
        };

        // Save the workflow configuration
        this.save = function () {

            if (isWorkflowSet()) {
                me.ud.workflow.selection  = {
                    id: me.ud.workflow.id,
                    configuration: me.getWorkflowConfig()
                };
            }
        };

        this.reset = function () {
            me.isProcessingState = true;
            me.ud = {};
            me.ud.workflow = {};
            me.workflows = {};
        };

        this.getUserEntries = function () {
            return me.ud.workflow;
        };
    };

    return {
        get: function (use) {
            return new Processing(use);
        }
    };

}]);

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
.factory('NewEventSource', ['JsHelper', 'CaptureAgentsResource', 'ConflictCheckResource', 'Notifications', 'Language', '$translate', '$filter', 'underscore', '$timeout', 'localStorageService', 'AuthService', 'SchedulingHelperService',
    function (JsHelper, CaptureAgentsResource, ConflictCheckResource, Notifications, Language, $translate, $filter, _, $timeout, localStorageService, AuthService, SchedulingHelperService) {

    // -- constants ------------------------------------------------------------------------------------------------- --

    var NOTIFICATION_CONTEXT = 'events-form';
    var SCHEDULE_SINGLE = 'SCHEDULE_SINGLE';
    var SCHEDULE_MULTIPLE = 'SCHEDULE_MULTIPLE';
    var WEEKDAY_PREFIX = 'EVENTS.EVENTS.NEW.WEEKDAYS.';
    var UPLOAD = 'UPLOAD';

    var WEEKDAYS = {
        'MO': WEEKDAY_PREFIX + 'MO',
        'TU': WEEKDAY_PREFIX + 'TU',
        'WE': WEEKDAY_PREFIX + 'WE',
        'TH': WEEKDAY_PREFIX + 'TH',
        'FR': WEEKDAY_PREFIX + 'FR',
        'SA': WEEKDAY_PREFIX + 'SA',
        'SU': WEEKDAY_PREFIX + 'SU'
    };

    // -- instance -------------------------------------------------------------------------------------------------- --

    var Source = function () {
        var self = this;

        this.save = function () {
            self.ud.UPLOAD.metadata['start'] = self.startDate;
        };

        this.createStartDate = function () {
            self.startDate = {
                "id": "startDate",
                "label": "EVENTS.EVENTS.DETAILS.METADATA.START_DATE",
                "value": new Date(Date.now()).toISOString(),
                "type": "date",
                "readOnly": false,
                "required": false,
                "tabindex": 7
            };
        };

        self.isSourceState = true;

        this.defaultsSet = false;

        this.checkingConflicts = false;
        this.hasConflicts = false;
        this.conflicts = [];
        this.hasConflictingSettings = function () {
            return self.hasConflicts;
        };

        /* Get the current client timezone */
        self.tzOffset = (new Date()).getTimezoneOffset() / -60;
        self.tz = 'UTC' + (self.tzOffset < 0 ? '' : '+') + self.tzOffset;

        this.loadCaptureAgents = function () {
            CaptureAgentsResource.query({inputs: true}).$promise.then(function (data) {
              self.captureAgents = data.rows;
            });
        };
        this.loadCaptureAgents();

        this.reset = function (opts) {

            self.createStartDate();
            self.weekdays = _.clone(WEEKDAYS);
            self.ud = {
                UPLOAD: {
                    tracks: {},
                    metadata: {
                        start: self.startDate
                    }
                },
                SCHEDULE_SINGLE: {
                    device: {
                        inputMethods: {}
                    }
                },

                SCHEDULE_MULTIPLE: {
                    device: {
                        inputMethods: {}
                    },
                    weekdays: {},
                    presentableWeekdays: ''
                },

                type: "UPLOAD"
            };

            if (opts) {
                if (opts.resetDefaults) {
                  self.defaultsSet = !opts.resetDefaults;
                  return;
                }

                var singleKeys = ['duration', 'start', 'end', 'device']; //Only apply these default fields to SCHEDULE_SINGLE

                for (var key in opts) {
                    if (key === 'type') {
                      continue;
                    }

                    self.ud.SCHEDULE_MULTIPLE[key] = angular.copy(opts[key]);

                    if (singleKeys.indexOf(key) > -1) {
                      self.ud.SCHEDULE_SINGLE[key] = angular.copy(opts[key]);
                    }
                }

                if (opts.presentableWeekdays) {
                    self.ud.SCHEDULE_MULTIPLE.weekdays[opts.presentableWeekdays.toUpperCase()] = true;
                }

                if (opts.type) {
                    self.ud.type = opts.type;
                }

                self.checkConflicts();
            }
        };
        this.reset();

        this.changeType = function() {
            localStorageService.set('sourceSticky', getType());
        }

        this.sortedWeekdays = _.map(self.weekdays, function(day, index) {
            return { key: index, translation: day };
        });

        this.hours = JsHelper.initArray(24);
        this.minutes = JsHelper.initArray(60);

        this.roomChanged = function () {
            self.ud[self.ud.type].device.inputMethods = {};

            self.ud[self.ud.type]
                .device.inputs.forEach(function(input) {
                    self.ud[self.ud.type].device.inputMethods[input.id] = true;
                });
        };

        this.toggleWeekday = function (weekday) {
            var weekdays = self.ud[SCHEDULE_MULTIPLE].weekdays;
            if (_.has(weekdays, weekday)) {
                weekdays[weekday] = !weekdays[weekday];
            }
        };

        /*
         * Some internal utilities.
         */
         var fields = [
             'start.date',
             'start.hour',
             'start.minute',
             'duration.hour',
             'duration.minute',
             'end.date',
             'end.hour',
             'end.minute',
             'device.name'];

        var getType = function() { return self.ud.type; };
        var isDefined = function(value) { return !(_.isUndefined(value) || _.isNull(value)); };
        var validators = {
            later: function() { return true; },
            UPLOAD: function() {
                // test for any type of upload source (MH-12085)
                return Object.keys(self.ud.UPLOAD.tracks).length > 0;
            },
            SCHEDULE_SINGLE: function () {
                return !self.hasConflicts && _.every(fields, function(field) {
                    var fieldValue = JsHelper.getNested(self.ud[SCHEDULE_SINGLE], field);
                    return isDefined(fieldValue);
                });
            },
            SCHEDULE_MULTIPLE: function() {
                var isAllFieldsDefined = _.every(fields, function(field) {
                    var fieldValue = JsHelper.getNested(self.ud[SCHEDULE_MULTIPLE], field);
                    return isDefined(fieldValue);
                });

                return !self.hasConflicts && isAllFieldsDefined && self.atLeastOneRepetitionDayMarked();
            }
        };

        this.isScheduleSingle = function() { return getType() === SCHEDULE_SINGLE; };
        this.isScheduleMultiple = function() { return getType() === SCHEDULE_MULTIPLE; };
        this.isUpload = function() { return getType() === UPLOAD; };

        this.atLeastOneInputMethodDefined = function () {
            var inputMethods = self.ud[getType()].device.inputMethods;
            return isDefined(_.find(inputMethods, function(inputMethod) { return inputMethod; }));
        };

        this.atLeastOneRepetitionDayMarked = function () {
            return isDefined(_.find(self.ud[SCHEDULE_MULTIPLE].weekdays, function(weekday) { return weekday; }));
        };

        this.canPollConflicts = function () {
            var data = self.ud[getType()];

            var result = isDefined(data) && isDefined(data.start) &&
                isDefined(data.start.date) && data.start.date.length > 0 &&
                angular.isDefined(data.duration) &&
                angular.isDefined(data.duration.hour) && angular.isDefined(data.duration.minute) &&
                isDefined(data.device) &&
                isDefined(data.device.id) && data.device.id.length > 0;

            if (self.isScheduleMultiple() && result) {
                return angular.isDefined(data.end.date) &&
                    data.end.date.length > 0 &&
                    self.atLeastOneRepetitionDayMarked();
            } else {
                return result;
            }
        };

        // Sort source select options by short title
        this.translatedSourceShortTitle = function(asset) {
            return $filter('translate')(asset.title + '.SHORT');
        }

        // Create the data array for use in the summary view
        this.updateUploadTracksForSummary =  function () {
            self.ud.trackuploadlistforsummary = [];
            var namemap = self.wizard.sharedData.uploadNameMap;
            angular.forEach(self.ud.UPLOAD.tracks, function ( value, key) {
                var item = {};
                var fileNames = [];
                item.id = key;
                item.title = namemap[key].title;
                angular.forEach(value, function (file) {
                    fileNames.push(file.name);
                });
                item.filename =  fileNames.join(", ");
                item.type = namemap[key].type;
                item.flavor = namemap[key].flavorType + "/" + namemap[key].flavorSubType;
                self.ud.trackuploadlistforsummary.push(item);
            });
        };

        this.checkConflicts = function () {

            // -- semaphore ----------------------------------------------------------------------------------------- --

            var acquire = function() { return !self.checkingConflicts && self.canPollConflicts(); };

            var release = function(conflicts) {

              self.hasConflicts = _.size(conflicts) > 0;

              while (self.conflicts.length > 0) { // remove displayed conflicts, existing ones will be added again in
                self.conflicts.pop();             // the next step.
              }

              if (self.hasConflicts) {
                angular.forEach(conflicts, function (d) {
                    self.conflicts.push({
                        title: d.title,
                        start: Language.formatDateTime('medium', d.start),
                        end: Language.formatDateTime('medium', d.end)
                    });
                    console.log ("Conflict: " + d.title + " Start: " + d.start + " End:" + d.end);
                });
              }

              self.updateWeekdays();
              self.checkValidity();
            };

            // -- ajax ---------------------------------------------------------------------------------------------- --

            if (acquire()) {
//                Notifications.remove(self.notification, NOTIFICATION_CONTEXT);

                var onSuccess = function () {
                  if (self.notification) {
                    Notifications.remove(self.notification, NOTIFICATION_CONTEXT);
                    self.notification = undefined;
                  }
                  release();
                };
                var onError = function (response) {

                    if (response.status === 409) {
                        if (!self.notification) {
                            self.notification = Notifications.add('error', 'CONFLICT_DETECTED', NOTIFICATION_CONTEXT, -1);
                        }

                        release(response.data);
                    } else {
                        // todo show general error
                        release();
                    }
                };

                var settings = self.ud[getType()];
                ConflictCheckResource.check(settings, onSuccess, onError);
            }
        };

        this.getStartDate = function() {
            var start = self.ud[getType()].start;
            return SchedulingHelperService.parseDate(start.date, start.hour, start.minute);
        };

        this.checkValidity = function () {
            var data = self.ud[getType()];

            if (self.alreadyEndedNotification) {
                Notifications.remove(self.alreadyEndedNotification, NOTIFICATION_CONTEXT);
            }
            // check if start is in the past and has already ended
            if (angular.isDefined(data.start) && angular.isDefined(data.start.hour)
                && angular.isDefined(data.start.minute) && angular.isDefined(data.start.date)
                && angular.isDefined(data.duration) && angular.isDefined(data.duration.hour)
                && angular.isDefined(data.duration.minute)) {
                var startDate = self.getStartDate();
                var endDate = new Date(startDate.getTime());
                endDate.setHours(endDate.getHours() + data.duration.hour, endDate.getMinutes() + data.duration.minute, 0, 0);
                var nowDate = new Date();
                if (endDate < nowDate) {
                    self.alreadyEndedNotification = Notifications.add('error', 'CONFLICT_ALREADY_ENDED',
                        NOTIFICATION_CONTEXT, -1);
                    self.hasConflicts = true;
                }
            }

            if (self.endBeforeStartNotification) {
                Notifications.remove(self.endBeforeStartNotification, NOTIFICATION_CONTEXT);
            }
            // check if end date is before start date
            if (angular.isDefined(data.start) && angular.isDefined(data.start.date)
                && angular.isDefined(data.end.date)) {
                var startDate = new Date(data.start.date);
                var endDate = new Date(data.end.date);
                if (endDate < startDate) {
                    self.endBeforeStartNotification = Notifications.add('error', 'CONFLICT_END_BEFORE_START',
                        NOTIFICATION_CONTEXT, -1);
                    self.hasConflicts = true;
                }
            }
        };

        /**
         * Update the presentation fo the weekdays for the summary
         */
        this.updateWeekdays = function () {
            var keyWeekdays = [];
            var keysOrder = [];
            var sortDay = function (day1, day2) {
                    return keysOrder[day1] - keysOrder[day2];
                };

            angular.forEach(self.sortedWeekdays, function (day, idx) {
                keysOrder[day.translation] = idx;
            });

            if (self.isScheduleMultiple()) {
                angular.forEach(self.ud.SCHEDULE_MULTIPLE.weekdays, function (weekday, index) {
                    if (weekday) {
                        keyWeekdays.push(self.weekdays[index]);
                    }
                 });
            }

            keyWeekdays.sort(sortDay);

            $translate(keyWeekdays).then(function (translations) {
                var translatedWeekdays = [];

                angular.forEach(translations, function(t) {
                    translatedWeekdays.push(t);
                });

                self.ud[SCHEDULE_MULTIPLE].presentableWeekdays = translatedWeekdays.join(',');
            });
        };

        this.getFormatedStartTime = function () {
            var time;

            if (!self.isUpload()) {
                var hour = self.ud[getType()].start.hour;
                var minute = self.ud[getType()].start.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = JsHelper.humanizeTime(hour, minute);
                }
            }

            return time;
        };

        this.getFormatedDuration = function () {
            var time;

            if (!self.isUpload()) {
                var hour = self.ud[getType()].duration.hour;
                var minute = self.ud[getType()].duration.minute;
                if (angular.isDefined(hour) && angular.isDefined(minute)) {
                    time = JsHelper.secondsToTime(((hour * 60) + minute) * 60);
                }
            }

            return time;
        };

        var getValidatorByType = function() {
            if (_.has(validators, getType())) {
                return validators[getType()];
            }
        };

        // Update summary when exiting this step
        // One-time update prevents an infinite loop in
        // summary's ng-repeat.
        this.ud.trackuploadlistforsummary = [];
        this.getTrackUploadSummary = function() {
            return this.ud.trackuploadlistforsummary;
        }
        this.onExitStep = function () {
            // update summary of selections
            this.updateUploadTracksForSummary();
        };

        this.isValid = function () {
            var validator = getValidatorByType();
            if(isDefined(validator)) {
                return validator();
            }

            return false;
        };

        this.getUserEntries = function () {
            return self.ud;
        };

        this.setDefaultsIfNeeded = function() {
                if (self.defaultsSet) {
                  return;
                }

                var defaults = {};
                AuthService.getUser().$promise.then(function (user) {
                    var orgProperties = user.org.properties;

                    //Variables needed to determine an event's start time
                    var startTime = orgProperties['admin.event.new.start_time'] || '08:00';
                    var endTime = orgProperties['admin.event.new.end_time'] || '20:00';
                    var durationMins = parseInt(orgProperties['admin.event.new.duration'] || (12 * 60));
                    var intervalMins = parseInt(orgProperties['admin.event.new.interval'] || 60);

                    var chosenSlot = moment( moment().format('YYYY-MM-DD') + ' ' + startTime );
                    var endSlot =  moment( moment().format('YYYY-MM-DD') + ' ' + endTime );
                    var dateNow = moment();
                    var timeDiff = dateNow.unix() - chosenSlot.unix();

                    //Find the next available timeslot for an event's start time
                    if (timeDiff > 0) {
                        var multiple = Math.ceil( timeDiff/(intervalMins * 60) );
                        chosenSlot.add(multiple * intervalMins, 'minute');
                        if (chosenSlot.unix() >= endSlot.unix()) {
                            endSlot = moment( chosenSlot ).add(durationMins, 'minutes');
                        }
                        durationMins = endSlot.diff(chosenSlot, 'minutes');
                    }

                    defaults.start = {
                                         date: chosenSlot.format('YYYY-MM-DD'),
                                         hour: parseInt(chosenSlot.format('H')),
                                         minute: parseInt(chosenSlot.format('mm'))
                                     };

                    defaults.duration = {
                                            hour: parseInt(durationMins / 60),
                                            minute: durationMins % 60
                                        };

                    defaults.end = {
                                         date: endSlot.format('YYYY-MM-DD'),
                                         hour: parseInt(endSlot.format('H')),
                                         minute: parseInt(endSlot.format('mm'))
                                     };

                    defaults.presentableWeekdays = chosenSlot.format('dd');

                    if (self.captureAgents.length === 0) {
                        //No capture agents, so user can only upload files
                        defaults.type = UPLOAD;
                    }
                    else if (localStorageService.get('sourceSticky')) {
                        //auto-select previously chosen source
                        defaults.type = localStorageService.get('sourceSticky');
                    }

                    self.reset(defaults);
                    self.defaultsSet = true;
                });
        };

        this.onTemporalValueChange = function(type) {
            SchedulingHelperService.applyTemporalValueChange(self.ud[getType()], type, self.isScheduleSingle() );
            self.checkConflicts();
        }
    };
    return new Source();
}]);

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
.factory('NewEventSummary', [function () {
    var Summary = function () {
        var me = this;
        me.ud = {};
        this.isValid = function () {
            return true;
        };

        this.reset = function () {
            me.ud = {};
        };
    };
    return new Summary();
}]);

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
.factory('NewEventStates', ['NewEventMetadata', 'NewEventMetadataExtended', 'NewEventSource', 'NewEventUploadAsset', 'NewEventAccess', 'NewEventProcessing', 'NewEventSummary',
        function (NewEventMetadata, NewEventMetadataExtended, NewEventSource, NewEventUploadAsset, NewEventAccess, NewEventProcessing, NewEventSummary) {
    return {
        get: function () {
            var states = [
                {
                    translation: 'EVENTS.EVENTS.NEW.METADATA.CAPTION',
                    name: 'metadata',
                    stateController: NewEventMetadata
                },
                {
                    translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
                    name: 'metadata-extended',
                    stateController: NewEventMetadataExtended
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.SOURCE.CAPTION',
                    name: 'source',
                    stateController: NewEventSource
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION',
                    name: 'upload-asset',
                    stateController: NewEventUploadAsset
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.PROCESSING.CAPTION',
                    name: 'processing',
                    // This allows us to reuse the processing functionality in schedule task
                    stateController: NewEventProcessing.get()
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.ACCESS.CAPTION',
                    name: 'access',
                    stateController: NewEventAccess
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.SUMMARY.CAPTION',
                    name: 'summary',
                    stateController: NewEventSummary
                }
            ];
            return states;
        }
    };
}]);

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

// Asset Upload service for New Events (MH-12085)
// This is used in the new events states wizard directive

angular.module('adminNg.services').factory('NewEventUploadAsset',['ResourcesListResource', 'UploadAssetOptions','JsHelper', 'Notifications', '$interval',
function (ResourcesListResource, UploadAssetOptions, JsHelper, Notifications, $interval) {

  // -- constants ------------------------------------------------------------------------------------------------- --
  var NOTIFICATION_CONTEXT = 'upload-asset';

  // Example of JSON used for the option list
  // The true values are retrieved by UploadAssetOptions from
  //  ./etc/listproviders/event.upload.asset.options.properties
  var exampleOfUploadAssetOptions = [
    {"id": "attachment_attachment_notes",
      "title": "class handout notes",
      "flavorType": "attachment",
      "flavorSubType": "notes",
      "type": "attachment"
    }, {"id":"catalog_captions_dfxp",
      "title": "captions DFXP",
      "flavorType": "captions",
      "flavorSubType": "timedtext",
      "type": "catalog"
    },{"id": "attachment_text_webvtt",
      "title": "Captions WebVTT",
      "flavorType": "text",
      "flavorSubType": "webvtt",
      "type": "attachment"
    },{"id":"attachment_presenter_search_preview",
      "title": "video list thumbnail",
      "flavorType": "presenter",
      "flavorSubType": "search+preview",
      "type": "attachment"
    }
  ];

  // -- instance -------------------------------------------------------------------------------------------------- --

  var NewEventUploadAsset = function () {

    var self = this;
    self.requiredMetadata = {};
    self.ud = {};
    self.ud.assets = {};
    self.ud.defaults = {};
    self.ud.namemap = {};
    self.ud.assetlistforsummary = [];
    self.ud.hasNonTrackOptions = false;

    // This is used as the callback from the uploadAssetDirective
    self.onAssetUpdate = function() {
       self.updateAssetsForSummary();
    },

    // Create an array of summary metadata for uploaded assets
    // to be used in the new event summary tab
    self.updateAssetsForSummary =  function () {
      self.ud.assetlistforsummary = [];
      angular.forEach(self.ud.assets, function ( value, key) {
         var item = {};
         var fileNames = [];
         item.id = key;
         item.title = self.ud.namemap[key].title;
         angular.forEach(value, function (file) {
           fileNames.push(file.name);
         });
         item.filename =  fileNames.join(", ");
         item.type = self.ud.namemap[key].type;
         item.flavor = self.ud.namemap[key].flavorType + "/" + self.ud.namemap[key].flavorSubType;
         self.ud.assetlistforsummary.push(item);
      });
    };

    // Retrieve the configured map of asset upload options
    // saved at the wizard level to make them available to all tabs and
    // prevents issues with the option ng-repeat.
    self.addSharedDataPromise = function() {
      UploadAssetOptions.getOptionsPromise().then(function(data){
        self.ud.defaults = data;
        self.visible = false;
        if (!self.wizard.sharedData) {
           self.wizard.sharedData = {};
        }
        self.wizard.sharedData.uploadAssetOptions = data.options;
        // Filter out asset options of type "track" for the asset upload tab
        // Track source options are uploaded on a different tab
        angular.forEach(data.options, function(option) {
          self.ud.namemap[option.id] = option;
          if (option.type !== 'track') {
            self.ud.hasNonTrackOptions = true;
            self.visible = true;
          }
        });
        self.wizard.sharedData.uploadNameMap = self.ud.namemap;
      });
     }

    // This step is visible when event.upload.asset.options.properties
    // listprovider contains options for asset upload.
    self.checkIfVisible = function () {
      // Prohibit uploading assets to scheduled events
      if (self.ud.hasNonTrackOptions && self.wizard.getStateControllerByName("source").isUpload()) {
        self.visible = true;
      } else {
        self.visible = false;
        self.ud.assets = {};
      }
    };

    self.isVisible = $interval(self.checkIfVisible, 1000);

    // Checks if the current state of this wizard is valid and we are
    // ready to move on.
    self.isValid = function () {
      var result = true;
      angular.forEach(self.requiredMetadata, function (item) {
        if (item === false) {
          result = false;
        }
      });
      return result;
    };

    // remove a selected asset
    self.deleteSelection = function (assetToDelete) {
      var index;

      angular.forEach(self.ud.assets, function (asset, idx) {
        if (idx === assetToDelete) {
          delete self.ud.assets[idx];
        }
      });
    };

    self.getUserEntries = function () {
      return self.ud;
   };

    self.getAssetUploadSummary = function() {
      return self.ud.assetlistforsummary;
    }

    self.hasAssetUploads = function () {
      var result = false;
      angular.forEach(self.ud.assets, function (asset, idx) {
          result = true;
       });
      return result;
    };
  };

  return new NewEventUploadAsset();
}]);

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
.factory('NewGroupSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);

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
.factory('NewGroupUsers', ['AuthService', 'ResourcesListResource', '$location', function (AuthService, ResourcesListResource) {
    var Users = function () {
        var me = this;

        var listName = AuthService.getUser().$promise.then(function (current_user) {
            return angular.isDefined(current_user)
                && angular.isDefined(current_user.org)
                && angular.isDefined(current_user.org.properties) ?
                current_user.org.properties['adminui.user.listname'] : undefined;
        });

        this.reset = function () {
            me.users = {
                available: [],
                selected:  [],
                i18n: 'USERS.GROUPS.DETAILS.USERS',
                searchable: true
            };
            listName.then(function (listName) {
                me.users.available = ResourcesListResource.query({ resource: listName || 'USERS.INVERSE.WITH.USERNAME'});
            });
        };

        this.reset();

        this.isValid = function () {
            return true;
        };

        this.getUsersList = function () {
            var list = '';

            angular.forEach(me.users.selected, function (user, index) {
                list += user.name + ((index + 1) === me.users.selected.length ? '' : ', ');
            });

            return list;
        };
    };

    return new Users();
}]);

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
.factory('NewGroupRoles', ['UsersResource', 'UserRolesResource', 'ResourcesListResource', function (UsersResource, UserRolesResource, ResourcesListResource) {
    var Roles = function () {
        var me = this;

        this.reset = function () {
            me.roles = {
                available: UserRolesResource.query({limit: 0, offset: 0, filter: 'role_target:USER'}),
                selected:  [],
                i18n: 'USERS.GROUPS.DETAILS.ROLES',
                searchable: true
            };
        };
        this.reset();

        this.isValid = function () {
            return true;
        };

        this.getRolesList = function () {
            var list = '';

            angular.forEach(me.roles.selected, function (role, index) {
                list += role.name + ((index + 1) === me.roles.selected.length ? '' : ', ');
            });

            return list;
        };

    };
    return new Roles();
}]);

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
.factory('NewGroupStates', ['NewGroupMetadata', 'NewGroupRoles', 'NewGroupUsers', 'NewGroupSummary',
        function (NewGroupMetadata, NewGroupRoles, NewGroupUsers, NewGroupSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.GROUPS.DETAILS.TABS.METADATA',
                name: 'metadata',
                stateController: NewGroupMetadata
            }, {
                translation: 'USERS.GROUPS.DETAILS.TABS.ROLES',
                name: 'roles',
                stateController: NewGroupRoles
            }, {
                translation: 'USERS.GROUPS.DETAILS.TABS.USERS',
                name: 'users',
                stateController: NewGroupUsers
            }, {
                translation: 'USERS.GROUPS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewGroupSummary
            }];
        },
        reset: function () {
            NewGroupMetadata.reset();
            NewGroupRoles.reset();
            NewGroupUsers.reset();
        }
    };
}]);

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
.factory('NewGroupMetadata', [function () {
    var Metadata = function () {
        var me = this;

        this.reset = function () {
            me.metadata = {
                name: '',
                description: ''
            };
        };
        this.reset();

        this.isValid = function () {
            return angular.isDefined(me.metadata) && (me.metadata.name.length > 0);
        };
    };
    return new Metadata();
}]);

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
.factory('NewUserblacklistStates', ['NewUserblacklistItems', 'NewUserblacklistDates', 'NewUserblacklistReason', 'NewUserblacklistSummary',
        function (NewUserblacklistItems, NewUserblacklistDates, NewUserblacklistReason, NewUserblacklistSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.USERS',
                name: 'items',
                stateController: NewUserblacklistItems
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.DATES',
                name: 'dates',
                stateController: NewUserblacklistDates
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.REASON',
                name: 'reason',
                stateController: NewUserblacklistReason
            }, {
                translation: 'USERS.BLACKLISTS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewUserblacklistSummary
            }];
        },
        reset: function () {
            NewUserblacklistItems.reset();
            NewUserblacklistDates.reset();
            NewUserblacklistReason.reset();
        }
    };
}]);

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
.factory('NewUserblacklistItems', ['UsersResource', '$location', function (UsersResource, $location) {
    var Users = function () {
        var me = this;

        this.isEditing = function () {
            var action = $location.search().action;
            return angular.isDefined(action);
        };

        this.reset = function () {
            me.ud = { items: [] };
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.length > 0;
        };

        this.items = UsersResource.query();

        this.addUser = function () {
            var found = false;
            angular.forEach(me.ud.items, function (user) {
                if (user.id === me.ud.userToAdd.id) {
                    found = true;
                }
            });
            if (!found) {
                me.ud.items.push(me.ud.userToAdd);
                me.ud.userToAdd = {};
            }
        };

        // Selecting multiple blacklistedIds is not yet supported by
        // the back end.

        //this.selectAll = function () {
        //    angular.forEach(me.ud.items, function (user) {
        //        user.selected = me.all;
        //    });
        //};

        //this.removeUser = function () {
        //    var items = [];
        //    angular.forEach(me.ud.items, function (user) {
        //        if (!user.selected) {
        //            items.push(user);
        //        }
        //    });
        //    me.ud.items = items;
        //};
    };
    return new Users();
}]);

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
.factory('NewUserblacklistDates', ['BlacklistCountResource', 'NewUserblacklistItems', 'JsHelper',
function (BlacklistCountResource, NewUserblacklistItems, JsHelper) {
    var Dates = function () {
        var me = this;

        this.reset = function () {
            me.ud = { fromDate: null, toDate: null };
        };
        this.reset();

        this.isValid = function () {
            return (me.ud.fromDate && me.ud.toDate && me.ud.fromTime && me.ud.toTime) ? true:false;
        };

        this.updateBlacklistCount = function () {
            if (me.isValid()) {
                var from = JsHelper.toZuluTimeString({
                    date: me.ud.fromDate,
                    hour: me.ud.fromTime.split(':')[0],
                    minute: me.ud.fromTime.split(':')[1]
                }),
                    to = JsHelper.toZuluTimeString({
                    date: me.ud.toDate,
                    hour: me.ud.toTime.split(':')[0],
                    minute: me.ud.toTime.split(':')[1]
                });

                me.blacklistCount = BlacklistCountResource.save({
                    type:          'person',
                    blacklistedId: NewUserblacklistItems.ud.items[0].id,
                    start:         from,
                    end:           to
                });
            }
        };
    };
    return new Dates();
}]);

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
.factory('NewUserblacklistReason', ['ResourcesListResource',
function (ResourcesListResource) {
    var Reason = function () {
        var me = this;

        this.reset = function () {
            me.ud = {};
        };
        this.reset();

        // Hard coded reasons, as requested.
        this.reasons = ResourcesListResource.get({ resource: 'BLACKLISTS.USERS.REASONS' });

        this.isValid = function () {
            return (me.ud.reason ? true:false);
        };
    };
    return new Reason();
}]);

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
.factory('NewUserblacklistSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);

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
.factory('NewLocationblacklistStates', ['NewLocationblacklistItems', 'NewLocationblacklistDates', 'NewLocationblacklistReason', 'NewLocationblacklistSummary',
        function (NewLocationblacklistItems, NewLocationblacklistDates, NewLocationblacklistReason, NewLocationblacklistSummary) {
    return {
        get: function () {
            return [{
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.LOCATIONS',
                name: 'items',
                stateController: NewLocationblacklistItems
            }, {
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.DATES',
                name: 'dates',
                stateController: NewLocationblacklistDates
            }, {
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.REASON',
                name: 'reason',
                stateController: NewLocationblacklistReason
            }, {
                translation: 'RECORDINGS.BLACKLISTS.DETAILS.TABS.SUMMARY',
                name: 'summary',
                stateController: NewLocationblacklistSummary
            }];
        },
        reset: function () {
            NewLocationblacklistItems.reset();
            NewLocationblacklistDates.reset();
            NewLocationblacklistReason.reset();
        }
    };
}]);

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
.factory('NewLocationblacklistItems', ['CaptureAgentsResource', function (CaptureAgentsResource) {
    var Locations = function () {
        var me = this;

        this.reset = function () {
            me.ud = { items: [] };
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.length > 0;
        };

        this.items = CaptureAgentsResource.query();

        this.addItem = function () {
            var found = false;
            angular.forEach(me.ud.items, function (item) {
                if (item.id === me.ud.itemToAdd.id) {
                    found = true;
                }
            });
            if (!found) {
                me.ud.items.push(me.ud.itemToAdd);
                me.ud.itemToAdd = {};
            }
        };

        // Selecting multiple blacklistedIds is not yet supported by
        // the back end.

        //this.selectAll = function () {
        //    angular.forEach(me.ud.items, function (item) {
        //        item.selected = me.all;
        //    });
        //};

        //this.removeItem = function () {
        //    var items = [];
        //    angular.forEach(me.ud.items, function (item) {
        //        if (!item.selected) {
        //            items.push(item);
        //        }
        //    });
        //    me.ud.items = items;
        //};
    };
    return new Locations();
}]);

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
.factory('NewLocationblacklistDates', ['BlacklistCountResource', 'NewLocationblacklistItems', 'JsHelper',
function (BlacklistCountResource, NewLocationblacklistItems, JsHelper) {
    var Dates = function () {
        var me = this;

        this.reset = function () {
            me.ud = { fromDate: null, toDate: null };
        };
        this.reset();

        this.isValid = function () {
            return (me.ud.fromDate && me.ud.toDate && me.ud.fromTime && me.ud.toTime) ? true:false;
        };

        this.updateBlacklistCount = function () {
            if (me.isValid()) {
                var from = JsHelper.toZuluTimeString({
                    date: me.ud.fromDate,
                    hour: me.ud.fromTime.split(':')[0],
                    minute: me.ud.fromTime.split(':')[1]
                }),
                    to = JsHelper.toZuluTimeString({
                    date: me.ud.toDate,
                    hour: me.ud.toTime.split(':')[0],
                    minute: me.ud.toTime.split(':')[1]
                });

                me.blacklistCount = BlacklistCountResource.save({
                    type:          'room',
                    blacklistedId: NewLocationblacklistItems.ud.items[0].id,
                    start:         from,
                    end:           to
                });
            }
        };
    };
    return new Dates();
}]);

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
.factory('NewLocationblacklistReason', ['ResourcesListResource',
function (ResourcesListResource) {
    var Reason = function () {
        var me = this;

        this.reset = function () {
            me.ud = {};
        };
        this.reset();

        // Hard coded reasons, as requested.
        this.reasons = ResourcesListResource.get({ resource: 'BLACKLISTS.LOCATIONS.REASONS' });

        this.isValid = function () {
            return (me.ud.reason ? true:false);
        };
    };
    return new Reason();
}]);

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
.factory('NewLocationblacklistSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);

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
.factory('BulkMessageStates', ['BulkMessageRecipients', 'BulkMessageMessage', 'BulkMessageSummary',
        function (BulkMessageRecipients, BulkMessageMessage, BulkMessageSummary) {
    return {
        get: function () {
            return [{
                translation: 'EVENTS.SEND_MESSAGE.TABS.RECIPIENTS',
                name: 'recipients',
                stateController: BulkMessageRecipients
            }, {
                translation: 'EVENTS.SEND_MESSAGE.TABS.MESSAGE',
                name: 'message',
                stateController: BulkMessageMessage
            }, {
                translation: 'EVENTS.SEND_MESSAGE.TABS.SUMMARY',
                name: 'summary',
                stateController: BulkMessageSummary
            }];
        },
        reset: function () {
            BulkMessageRecipients.reset();
            BulkMessageMessage.reset();
        }
    };
}]);

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
.factory('BulkMessageRecipients', ['RecipientsResource', 'Table',
function (RecipientsResource, Table) {
    var Recipients = function () {
        var me = this;

        this.reset = function () {
            var resource = Table.resource || 'events',
                request = {
                    resource: resource
                };
            me.ud = { items: { recipients: [], recordings: [] } };

            request.category = request.resource === 'events' ? 'event':'series';
            request[request.category + 'Ids'] = Table.getSelected().
                map(function (item) { return item.id; }).join(',');

            RecipientsResource.get(request, function (data) {
                me.ud.items = data;
            });
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.recipients.length > 0;
        };
    };
    return new Recipients();
}]);

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
.factory('BulkMessageMessage', ['EmailPreviewResource', 'EmailVariablesResource', 'EmailTemplatesResource', 'EmailTemplateResource', 'BulkMessageRecipients',
function (EmailPreviewResource, EmailVariablesResource, EmailTemplatesResource, EmailTemplateResource, BulkMessageRecipients) {
    var Message = function () {
        var me = this;

        this.reset = function () {
            me.ud = {};
            this.templates = EmailTemplatesResource.query();
        };
        this.reset();

        this.variables = EmailVariablesResource.query();

        this.applyTemplate = function () {
            EmailTemplateResource.get({ id: me.ud.email_template.id }, function (template) {
                me.ud.message = template.message;
                me.ud.subject = template.subject;
            });
        };

        this.isValid = function () {
            return (angular.isDefined(me.ud) &&
                    angular.isDefined(me.ud.message) &&
                    angular.isDefined(me.ud.subject));
        };

        this.updatePreview = function () {
            if (me.isValid()) {
                me.preview = EmailPreviewResource.save({
                    templateId: me.ud.email_template.id
                }, {
                    recordingIds: BulkMessageRecipients.ud.items.recordings
                        .map(function (item) { return item.id; }).join(','),
                    personIds:    BulkMessageRecipients.ud.items.recipients
                        .map(function (item) { return item.id; }).join(','),
                    signature:    me.ud.include_signature ? true:false,
                    body:         me.ud.message
                });
            }
        };

        this.insertVariable = function (variable) {
            var message  = me.ud.message || '',
                position = angular.element('textarea[ng-model="wizard.step.ud.message"]')[0].selectionStart;

            me.ud.message = message.substr(0, position) + variable + message.substr(position);
        };
    };
    return new Message();
}]);

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
.factory('BulkMessageSummary', [function () {
    var Summary = function () {
        this.isValid = function () {
            return true;
        };
    };
    return new Summary();
}]);

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
.factory('NewSeriesAccess', ['ResourcesListResource', 'SeriesAccessResource', 'AuthService', 'UserRolesResource', 'Notifications', '$timeout',
    function (ResourcesListResource, SeriesAccessResource, AuthService, UserRolesResource, Notifications, $timeout) {
    var Access = function () {

        var roleSlice = 100;
        var roleOffset = 0;
        var loading = false;
        var rolePromise = null;

        var me = this,
            NOTIFICATION_CONTEXT = 'series-acl',
            aclNotification,
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false,
                    actions : {
                        name : 'new-series-acl-actions',
                        value : []
                    }
                };
            },
            checkNotification = function () {
                if (me.unvalidRule) {
                    if (!angular.isUndefined(me.notificationRules)) {
                        Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                    }
                    me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
                } else if (!angular.isUndefined(me.notificationRules)) {
                    Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                    me.notificationRules = undefined;
                }

                if (!me.hasRights) {
                    if (!angular.isUndefined(me.notificationRights)) {
                        Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                    }
                    me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
                } else if (!angular.isUndefined(me.notificationRights)) {
                    Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                    me.notificationRights = undefined;
                }

                $timeout(function () {
                    checkNotification();
                }, 200);
            },
            addUserRolePolicy = function (policies) {
                if (angular.isDefined(AuthService.getUserRole())) {
                    var currentUserPolicy = createPolicy(AuthService.getUserRole());
                    currentUserPolicy.read = true;
                    currentUserPolicy.write = true;
                    policies.push(currentUserPolicy);
                }
                return policies;
            };

        me.ud = {};
        me.ud.id = {};
        me.ud.policies = [];
        // Add the current user's role to the ACL upon the first startup
        me.ud.policies = addUserRolePolicy(me.ud.policies);
        me.ud.baseAcl = {};

        this.changeBaseAcl = function () {
            var newPolicies = {};
            me.ud.baseAcl = SeriesAccessResource.getManagedAcl({id: me.ud.id}, function () {
                angular.forEach(me.ud.baseAcl.acl.ace, function (acl) {
                    var policy = newPolicies[acl.role];

                    if (angular.isUndefined(policy)) {
                        newPolicies[acl.role] = createPolicy(acl.role);
                    }
                    if (acl.action === 'read' || acl.action === 'write') {
                        newPolicies[acl.role][acl.action] = acl.allow;
                    } else if (acl.allow === true || acl.allow === 'true'){
                        newPolicies[acl.role].actions.value.push(acl.action);
                    }
                });

                me.ud.policies = [];
                // After loading an ACL template add the user's role to the top of the ACL list if it isn't included
                if (angular.isDefined(AuthService.getUserRole()) && !angular.isDefined(newPolicies[AuthService.getUserRole()])) {
                    me.ud.policies = addUserRolePolicy(me.ud.policies);
                }

                angular.forEach(newPolicies, function (policy) {
                    me.ud.policies.push(policy);
                });

                me.ud.id = '';
            });
        };

        this.addPolicy = function () {
            me.ud.policies.push(createPolicy());
        };

        this.deletePolicy = function (policyToDelete) {
            var index;

            angular.forEach(me.ud.policies, function (policy, idx) {
                if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
                    index = idx;
                }
            });

            if (angular.isDefined(index)) {
                me.ud.policies.splice(index, 1);
            }
        };

        this.isValid = function () {
             var hasRights = false,
                rulesValid = true;

             angular.forEach(me.ud.policies, function (policy) {
                rulesValid = false;

                if (policy.read && policy.write) {
                    hasRights = true;
                }

                if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
                    rulesValid = true;
                }
             });

            me.unvalidRule = !rulesValid;
            me.hasRights = hasRights;

            if (hasRights && angular.isDefined(aclNotification)) {
                Notifications.remove(aclNotification, 'series-acl');
            }

            if (!hasRights && !angular.isDefined(aclNotification)) {
                aclNotification = Notifications.add('warning', 'SERIES_ACL_MISSING_READWRITE_ROLE', 'series-acl', -1);
            }

            return rulesValid && hasRights;
        };

        checkNotification();

        me.acls  = ResourcesListResource.get({ resource: 'ACL' });
        me.actions = {};
        me.hasActions = false;
        ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
            angular.forEach(data, function (value, key) {
                if (key.charAt(0) !== '$') {
                    me.actions[key] = value;
                    me.hasActions = true;
                }
            });
        });

        me.roles = {};

        me.getMoreRoles = function (value) {

            if (me.loading)
                return rolePromise;

            me.loading = true;

            // the offset should actually be setto roleOffset, but when used doesn't display the correct roles
            var queryParams = {limit: roleSlice, offset: 0};

            if ( angular.isDefined(value) && (value != "")) {
                //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
                //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
                queryParams["filter"] = "role_name:"+ value +",role_target:ACL";
                queryParams["offset"] = 0;
            } else {
                queryParams["filter"] = "role_target:ACL";
            }
            rolePromise = UserRolesResource.query(queryParams);
            rolePromise.$promise.then(function (data) {
                angular.forEach(data, function (role) {
                    me.roles[role.name] = role.value;
                });
                roleOffset = Object.keys(me.roles).length;
            }).finally(function () {
                me.loading = false;
            });
            return rolePromise;
        };

        me.getMoreRoles();

        this.reset = function () {
            me.ud = {
                id: {},
                policies: []
            };
            // Add the user's role upon resetting
            me.ud.policies = addUserRolePolicy(me.ud.policies);
        };

        this.reload = function () {
            me.acls  = ResourcesListResource.get({ resource: 'ACL' });
            me.roles = {};
            me.roleOffset = 0;
            me.getMoreRoles();
        };

        this.reset();
    };

    return new Access();
}]);

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
.factory('NewSeriesMetadata', ['NewSeriesMetadataResource', function (NewSeriesMetadataResource) {
    var Metadata = function () {
        var me = this, mainMetadataName = 'dublincore/series', i;

        this.requiredMetadata = {};

        // As soon as the required metadata fields arrive from the backend,
        // we check which are mandatory.
        // This information will be needed in ordert to tell if we can move
        // on to the next page of the wizard.
        this.findRequiredMetadata = function (data) {
            var mainData = data[mainMetadataName];
            me.ud[mainMetadataName] = mainData;
            if (mainData && mainData.fields) {
                for (i = 0; i < mainData.fields.length; i++) {
                    mainData.fields[i].tabindex = i + 1; // just hooking the tab index up here, as this is already running through all elements
                    if (mainData.fields[i].required) {
                        me.requiredMetadata[mainData.fields[i].id] = false;
                        if (mainData.fields[i].type === 'boolean') {
                            // set all boolean fields to false by default
                            mainData.fields[i].value = false;
                            me.requiredMetadata[mainData.fields[i].id] = true;
                        }
                    }
                }
            }
        };

        // Checks if the current state of this wizard is valid and we are
        // ready to move on.
        this.isValid = function () {
            var result = true;
            //FIXME: The angular validation should rather be used,
            // unfortunately it didn't work in this context.
            angular.forEach(me.requiredMetadata, function (item) {
                if (item === false) {
                    result = false;
                }
            });
            return result;
        };

        this.save = function (scope) {
            //FIXME: This should be nicer, rather propagate the id and values
            //instead of looking for them in the parent scope.
            var params = scope.$parent.params,
                fieldId = params.id,
                value = params.value;

            if (params.collection) {
                if (angular.isArray(value)) {
                    var presentableValue = '';

                    angular.forEach(value, function (item, index) {
                        presentableValue += item;
                        if ((index + 1) < value.length) {
                            presentableValue += ', ';
                        }
                    });

                    params.presentableValue = presentableValue;
                } else {
                    params.presentableValue = params.collection[value];
                }
            } else {
                params.presentableValue = value;
            }

            me.ud[mainMetadataName].fields[fieldId] = params;

            if (!angular.isUndefined(me.requiredMetadata[fieldId])) {
                if (angular.isDefined(value) && value.length > 0) {
                    // we have received a required value
                    me.requiredMetadata[fieldId] = true;
                } else {
                    // the user has deleted the value
                    me.requiredMetadata[fieldId] = false;
                }
            }
        };

        this.reset = function () {
            me.ud = {};
            me.metadata = NewSeriesMetadataResource.get(me.findRequiredMetadata);
        };

        this.getUserEntries = function () {
            if (angular.isDefined(me.ud[mainMetadataName])) {
                return me.ud[mainMetadataName].fields;
            } else {
                return {};
            }
        };

        this.reset();
    };

    return new Metadata();
}]);

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
.factory('NewSeriesMetadataExtended', ['NewSeriesMetadataResource', function (NewSeriesMetadataResource) {
    var MetadataExtended = function () {
        var me = this, i;

        // As soon as the required metadata fields arrive from the backend,
        // we check which are mandatory.
        // This information will be needed in order to tell if we can move
        // on to the next page of the wizard.
        this.postProcessMetadata = function (data) {
            var fields = [], chunk;
            for (chunk in data) {
                if (data.hasOwnProperty(chunk)) {
                    // extended metadata is every object in the returned data which
                    // does not start with a dollar sign and which isn't dublincore/episode
                    if (chunk !== 'dublincore/series' && chunk.charAt(0) !== '$') {
                        me.ud[chunk] = {fields: data[chunk].fields};
                        me.ud[chunk].flavor = data[chunk].flavor;
                        me.ud[chunk].title = data[chunk].title;
                        fields = fields.concat(data[chunk].fields);
                    }
                }
            }
            // we go for the extended metadata here
            if (fields.length > 0) {
                for (i = 0; i < fields.length; i++) {
                    if (fields[i].required) {
                        me.requiredMetadata[fields[i].id] = false;
                        if (fields[i].type === 'boolean') {
                            // set all boolean fields to false by default
                            fields[i].value = false;
                        }
                    }
                }
                me.visible = true;
            }
            else {
                me.visible = false;
            }
        };

        // Checks if the current state of this wizard is valid and we are
        // ready to move on.
        this.isValid = function () {
            var result = true;
            //FIXME: The angular validation should rather be used,
            // unfortunately it didn't work in this context.
            angular.forEach(me.requiredMetadata, function (item) {
                if (item === false) {
                    result = false;
                }
            });
            return result;
        };

        this.save = function (scope) {
            //FIXME: This should be nicer, rather propagate the id and values
            //instead of looking for them in the parent scope.
            var params = scope.$parent.params,
                target = scope.$parent.target,
                fieldId = params.id,
                value = params.value;

            if (params.collection) {
                if (angular.isArray(value)) {
                    params.presentableValue = value;
                } else {
                    params.presentableValue = params.collection[value];
                }
            } else {
                params.presentableValue = value;
            }

            me.ud[target].fields[fieldId] = params;

            if (!angular.isUndefined(me.requiredMetadata[fieldId])) {
                if (angular.isDefined(value) && value.length > 0) {
                    // we have received a required value
                    me.requiredMetadata[fieldId] = true;
                } else {
                    // the user has deleted the value
                    me.requiredMetadata[fieldId] = false;
                }
            }
        };

        this.getFiledCatalogs = function () {
            var catalogs = [];

            angular.forEach(me.ud, function(catalog) {
                var empty = true;
                angular.forEach(catalog.fields, function (field) {
                    if (angular.isDefined(field.presentableValue) && field.presentableValue !=='') {
                        empty = false;
                    }
                });

                if (!empty) {
                    catalogs.push(catalog);
                }
            });

            return catalogs;
        };

        this.reset = function () {
            me.ud = {};
            me.requiredMetadata = {};
            me.metadata = NewSeriesMetadataResource.get(me.postProcessMetadata);
        };

        this.getUserEntries = function () {
            return me.ud;
        };

        this.reset();
    };

    return new MetadataExtended();
}]);

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
.factory('NewSeriesTheme', ['NewSeriesThemeResource', function (NewSeriesThemeResource) {
    var Theme = function () {
        var me = this;

        this.isValid = function () {
            return true;
        };

        this.reset = function () {
            me.ud = {};
            me.ud.theme = {};
        };

        this.reset();

        this.themeSave = function () {
            me.ud.themeDescription = me.themes[me.ud.theme].description;
        };

        me.themes = NewSeriesThemeResource.get();
    };

    return new Theme();
}]);

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
.factory('NewSeriesStates', ['NewSeriesMetadata', 'NewSeriesMetadataExtended', 'NewSeriesAccess', 'NewSeriesTheme', 'NewSeriesSummary',
        function (NewSeriesMetadata, NewSeriesMetadataExtended, NewSeriesAccess, NewSeriesTheme, NewSeriesSummary) {
    return {
        get: function () {
            return [{
                translation: 'EVENTS.SERIES.NEW.METADATA.CAPTION',
                name: 'metadata',
                stateController: NewSeriesMetadata
            }, {
                translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
                name: 'metadata-extended',
                stateController: NewSeriesMetadataExtended
            }, {
                translation: 'EVENTS.SERIES.NEW.ACCESS.CAPTION',
                name: 'access',
                stateController: NewSeriesAccess
            }, {
               translation: 'EVENTS.SERIES.NEW.THEME.CAPTION',
                name: 'theme',
                stateController: NewSeriesTheme
            },
            {
                translation: 'EVENTS.SERIES.NEW.SUMMARY.CAPTION',
                name: 'summary',
                stateController: NewSeriesSummary
            }];
        }
    };
}]);

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
.factory('NewSeriesSummary', [function () {
    var Summary = function () {
        this.ud = {};
        this.isValid = function () {
            return true;
        };
        this.isDisabled = false;
    };
    return new Summary();
}]);

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
.factory('NewAclAccess', ['ResourcesListResource', 'UserRolesResource', 'AclResource', function (ResourcesListResource, UserRolesResource, AclResource) {
    var Access = function () {

        var roleSlice = 100;
        var roleOffset = 0;
        var loading = false;
        var rolePromise = null;

        var me = this,
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false,
                    actions : {
                        name : 'new-acl-actions',
                        value : []
                    }
                };
            };

        me.isAccessState = true;
        me.ud = {};
        me.ud.id = {};
        me.ud.policies = [];
        me.ud.baseAcl = {};

        this.changeBaseAcl = function () {
            var newPolicies = {};
            me.ud.baseAcl = AclResource.get({id: me.ud.id}, function () {
                angular.forEach(me.ud.baseAcl.acl.ace, function (acl) {
                    var policy = newPolicies[acl.role];

                    if (angular.isUndefined(policy)) {
                        newPolicies[acl.role] = createPolicy(acl.role);
                    }
                    if (acl.action === 'read' || acl.action === 'write') {
                        newPolicies[acl.role][acl.action] = acl.allow;
                    } else if (acl.allow === true || acl.allow === 'true'){
                        newPolicies[acl.role].actions.value.push(acl.action);
                    }
                });

                me.ud.policies = [];
                angular.forEach(newPolicies, function (policy) {
                    me.ud.policies.push(policy);
                });

                me.ud.id = '';
            });
        };

        this.addPolicy = function () {
            me.ud.policies.push(createPolicy());
        };

        this.deletePolicy = function (policyToDelete) {
            var index;

            angular.forEach(me.ud.policies, function (policy, idx) {
                if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
                    index = idx;
                }
            });

            if (angular.isDefined(index)) {
                me.ud.policies.splice(index, 1);
            }
        };

        this.isValid = function () {
            // Is always true, the series can have an empty ACL
            return true;
        };

        me.acls  = ResourcesListResource.get({ resource: 'ACL' });
        me.actions = {};
        me.hasActions = false;
        ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
            angular.forEach(data, function (value, key) {
                if (key.charAt(0) !== '$') {
                    me.actions[key] = value;
                    me.hasActions = true;
                }
            });
        });

        me.roles = {};

        me.getMoreRoles = function (value) {

            if (loading)
                return rolePromise;

            loading = true;
            var queryParams = {limit: roleSlice, offset: roleOffset};

            if ( angular.isDefined(value) && (value != "")) {
                //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
                //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
                queryParams["filter"] = "role_name:"+ value +",role_target:ACL";
                queryParams["offset"] = 0;
            } else {
                queryParams["filter"] = "role_target:ACL";
            }
            rolePromise = UserRolesResource.query(queryParams);
            rolePromise.$promise.then(function (data) {
                angular.forEach(data, function (role) {
                    me.roles[role.name] = role.value;
                });
                roleOffset = Object.keys(me.roles).length;
            }).finally(function () {
                loading = false;
            });
            return rolePromise;
        };

        me.getMoreRoles();

        this.reset = function () {
            me.ud = {
                id: {},
                policies: []
            };
        };

        this.reset();
    };
    return new Access();
}]);

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
.factory('NewAclMetadata', [function () {
    var Metadata = function () {
        var me = this;

        this.reset = function () {
            me.metadata = {
                name: ''
            };
        };
        this.reset();

        this.isValid = function () {
            return angular.isDefined(me.metadata) && (me.metadata.name.length > 0);
        };
    };
    return new Metadata();
}]);

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
.factory('NewAclStates', ['NewAclMetadata', 'NewAclAccess', 'NewAclSummary',
        function (NewAclMetadata, NewAclAccess, NewAclSummary) {
    return {
        get: function () {
            return [{
                translation: 'USERS.ACLS.NEW.TABS.METADATA',
                name: 'metadata',
                stateController: NewAclMetadata
            }, {
                translation: 'USERS.ACLS.NEW.TABS.ACCESS',
                name: 'access',
                stateController: NewAclAccess
            }, {
                translation: 'USERS.ACLS.NEW.TABS.SUMMARY',
                name: 'summary',
                stateController: NewAclSummary
            }];
        },
        reset: function () {
            NewAclMetadata.reset();
            NewAclAccess.reset();
        }
    };
}]);

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
.factory('NewAclSummary', [function () {
    var Summary = function () {
        var me = this;
        me.ud = {};
        this.isValid = function () {
            return true;
        };

        this.reset = function () {
            me.ud = {};
        };
    };
    return new Summary();
}]);

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
.factory('PlayerAdapterRepository', ['$injector', function ($injector) {


    /**
     * A repository containing the adapter instances for given adapter type (may be html5, videojs) and given element.
     * The sole purpose of this implementation is to be able to manage adapters on a per instance basis - if there is
     * a better solution for this problem, this class becomes obvious.
     *
     * @constructor
     */
    var PlayerAdapterRepository = function () {
        var adapters = {};

        /**
         * Returns the given adapter instance per adapterType and elementId. If the adapter does not exist,
         * it will be created.
         *
         * @param adapterType
         * @param element of the player
         * @returns {*}
         */
        this.findByAdapterTypeAndElementId = function (adapterType, element) {
            if (typeof adapters[adapterType] === 'undefined') {
                // create entry for adapterType if not existent
                adapters[adapterType] = {};
            }

            if (typeof adapters[adapterType][element.id] === 'undefined') {
                adapters[adapterType][element.id] = this.createNewAdapter(adapterType, element);
            }
            return adapters[adapterType][element.id];
        };

        this.createNewAdapter = function (adapterType, element) {
            var factory, adapter;
            factory = $injector.get('PlayerAdapterFactory' + adapterType);
            adapter = factory.create(element);
            return adapter;
        }

    };

    return new PlayerAdapterRepository();
}]);

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

angular.module('adminNg.services')
.factory('PlayerAdapterFactoryDefault', ['PlayerAdapter', function (PlayerAdapter) {
    var Factory = function () {
        /**
        * A default implementation of an adapter. Its purpose is to
        * track the state and delegate events from / to specific adapter
        * implementations.
        *
        * Usage: Instantiate an instance of this DefaultAdapter and copy
        * its capabilities to your adapter implementation by calling
        * #extend(this).
        */
        var DefaultAdapter = function (targetElement) {
            // keep a reference to this, for callbacks.
            var me = this;

            // The state of adapter implementations must be delegated here.
            this.state = {
                /**
                 * The current player status
                 * @inner
                 * @type {module:player-adapter.PlayerAdapter.STATUS}
                 */
                status: PlayerAdapter.STATUS.INITIALIZING,
                /**
                 * Define if a play request has be done when the player was not ready
                 * @inner
                 * @type {Boolean}
                 */
                waitToPlay: false,
                /**
                 * Define if a the player has been initialized
                 * @inner
                 * @type {Boolean}
                 */
                initialized: false
            };

            /**
             * Registers all default events to the html 5 standard implementations.
             * Just override events from the specific adapter if needed.
             */
            this.registerDefaultListeners = function () {
                // register listeners
                targetElement.addEventListener('canplay', me.canPlay);

                targetElement.addEventListener('durationchange', me.canPlay);

                targetElement.addEventListener('play', function () {
                    if (!me.state.initialized) {
                        return;
                    }

                    me.state.status = PlayerAdapter.STATUS.PLAYING;
                });


                targetElement.addEventListener('playing', function () {
                    me.state.status = PlayerAdapter.STATUS.PLAYING;
                });

                targetElement.addEventListener('pause', function () {
                    if (!me.state.initialized) {
                        return;
                    }

                    me.state.status = PlayerAdapter.STATUS.PAUSED;
                });

                targetElement.addEventListener('ended', function () {
                    me.state.status = PlayerAdapter.STATUS.ENDED;
                });

                targetElement.addEventListener('seeking', function () {
                    me.state.oldStatus = me.state.status;
                    me.state.status = PlayerAdapter.STATUS.SEEKING;
                });

                targetElement.addEventListener('error', function () {
                    me.state.status = PlayerAdapter.STATUS.ERROR_NETWORK;
                });
            };

            // =========================
            // ADAPTER API
            // =========================

            /**
             * Play the video
             */
            this.play = function () {
                // Can the player start now?
                switch (me.state.status) {
                    case PlayerAdapter.STATUS.LOADING:
                        me.state.waitToPlay = true;
                        break;
                    default:
                        // If yes, we play it
                        targetElement.play();
                        me.state.status = PlayerAdapter.STATUS.PLAYING;
                        me.state.status.waitToPlay = false;
                        break;
                }
            };

            this.canPlay = function () {
                // If duration is still not valid
                if (isNaN(me.getDuration()) || targetElement.readyState < 1) {
                    return;
                }

                if (!me.state.initialized) {
                    me.state.initialized = true;
                }

                if (me.state.waitToPlay) {
                    me.play();
                }
            };

            /**
             * Pause the video
             */
            this.pause = function () {
                targetElement.pause();
            };

            /**
             * Set the current time of the video
             * @param {double} time The time to set in seconds
             */
            this.setCurrentTime = function (time) {
                if (time < 0) {
                  time = 0;
                } else if (time > me.getDuration()) {
                  time = me.getDuration();
                }
                targetElement.currentTime = time;
            };

            /**
             * Get the current time of the video
             */
            this.getCurrentTime = function () {
                return targetElement.currentTime;
            };

            /**
             * Takes the player to the next frame. The step is calculated by 1/framerate.
             * @throws Error if called at end of player
             * @throws Error if called in status PLAYING
             */
            this.nextFrame = function () {

                var currentTime = me.getCurrentTime();

                if (me.state.status === PlayerAdapter.STATUS.PLAYING) {
                    throw new Error('In state playing calls to previousFrame() are not possible.');
                }

                if (currentTime >= me.getDuration()) {
                    throw new Error('At end of video calls to nextFrame() are not possible.');
                }

                me.setCurrentTime(currentTime + 1 / me.getFramerate());
            };

            /**
             * Takes the player to the previous frame. The step is calculated by 1/framerate.
             * @throws Error if called at start of player
             * @throws Error if called in status PLAYING
             */
            this.previousFrame = function () {

                var currentTime = me.getCurrentTime();

                if (me.state.status === PlayerAdapter.STATUS.PLAYING) {
                    throw new Error('In state playing calls to previousFrame() are not possible.');
                }

                if (currentTime === 0) {
                    throw new Error('At start of video calls to previosFrame() are not possible.');
                }

                me.setCurrentTime(currentTime - 1 / me.getFramerate());
            };


            /**
             * TODO find a way to find out framerate
             *
             * @returns {number}
             */
            this.getFramerate = function () {
                return 30;
            };


            /**
             * Returns the current time as an object containing hours, minutes, seconds and milliseconds.
             * @returns {{hours: number, minutes: number, seconds: number, milliseconds: number}}
             */
            this.getCurrentTimeObject = function () {
                var currentTimeMillis = this.getCurrentTime() * 1000,
                    hours = Math.floor(currentTimeMillis / (3600 * 1000)),
                    minutes = Math.floor((currentTimeMillis % (3600 * 1000)) / 60000),
                    seconds = Math.floor((currentTimeMillis % (60000) / 1000)),
                    milliseconds = Math.floor((currentTimeMillis % 1000));

                return {
                    hours: hours,
                    minutes: minutes,
                    seconds: seconds,
                    milliseconds: milliseconds
                };
            };

            /**
             * Get the video duration
             */
            this.getDuration = function () {
                return targetElement.duration;
            };

            /**
             * Get the player status
             */
            this.getStatus = function () {
                return me.state.status;
            };

            this.toggleMute = function () {
              me.muted(! me.muted());
            }

            /**
             * Turns audio on or off and returns current status
             * @param {boolean} status if Audio is mute or not, if not set only status is returned
             * @returns {boolean} muted status of the player
             */
            this.muted = function (status) {
                if (status !== undefined) {
                  targetElement.muted = status;
                }
                return targetElement.muted;
            }

            /**
             * Set and get the volume of the player
             * @param {int} volume volume of the player from 0 (mute) to 100 (max),
             *              if not set only returns current volume
             * @returns {int} value from 0 (mute) to 100 (max)
             */
            this.volume = function (volume) {
                if (volume !== undefined) {
                  if (volume === 0) {
                    me.muted(true);
                  } else {
                    me.muted(false);
                  }
                  targetElement.volume = volume / 100.0;
                }
                return parseInt(targetElement.volume * 100);
            }

            /**
             * Copies the API's default implementation methods to the target.
             */
            this.extend = function (target) {
                target.play = me.play;
                target.canPlay = me.canPlay;
                target.pause = me.pause;
                target.setCurrentTime = me.setCurrentTime;
                target.getCurrentTime = me.getCurrentTime;
                target.nextFrame = me.nextFrame;
                target.previousFrame = me.previousFrame;
                target.getFramerate = me.getFramerate;
                target.getCurrentTimeObject  = me.getCurrentTimeObject;
                target.getDuration = me.getDuration;
                target.getStatus = me.getStatus;
                target.muted = me.muted;
                target.volume = me.volume;
              };
            return this;
        };

        this.create = function (targetElement) {
            return new DefaultAdapter(targetElement);
        };
    };
    return new Factory();
}]);

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
.factory('PlayerAdapterFactoryHTML5', ['PlayerAdapter', 'PlayerAdapterFactoryDefault', function (PlayerAdapter, PlayerAdapterFactoryDefault) {

    var PlayerAdapterFactoryHTML5 = function () {

        /**
         * Implementation of the player adapter for the HTML5 native player
         * @constructor
         * @alias module:player-adapter-HTML5.PlayerAdapterHTML5
         * @augments {module:player-adapter.PlayerAdapter}
         * @param {DOMElement} targetElement DOM Element representing the player
         */
        var PlayerAdapterHTML5 = function (targetElement) {
            'use strict';

            var defaultAdapter, self = this;

            this.eventMapping = PlayerAdapter.eventMapping();

            this.eventMapping
                .map(PlayerAdapter.EVENTS.PAUSE, 'pause')
                .map(PlayerAdapter.EVENTS.PLAY, 'play')
                .map(PlayerAdapter.EVENTS.READY, 'ready')
                .map(PlayerAdapter.EVENTS.TIMEUPDATE, 'timeupdate')
                .map(PlayerAdapter.EVENTS.DURATION_CHANGE, 'durationchange')
                .map(PlayerAdapter.EVENTS.CAN_PLAY, 'canplay')
                .map(PlayerAdapter.EVENTS.VOLUMECHANGE, 'volumechange');

            // Check if the given target Element is valid
            if (typeof targetElement === 'undefined' || targetElement === null) {
                throw 'The given target element must not be null and have to be a valid HTMLElement!';
            }

            /**
             * Id of the player adapter
             * @inner
             * @type {String}
             */
            this.id = 'PlayerAdapter' + targetElement.id;

            // =========================
            // INITIALIZATION
            // =========================

            /**
             * Register a listener listening to events of type. The event name will be translated from
             * API event (@see PlayerAdapter) to native events of the player implementation.
             *
             * @param type
             * @param listener
             */
            this.addListener = function (type, listener) {
                targetElement.addEventListener(self.eventMapping.resolveNativeName(type), listener);
            };

            // Instantiate DefaultAdapter and copy its methods to this adapter.
            defaultAdapter = PlayerAdapterFactoryDefault.create(targetElement);
            defaultAdapter.extend(this);
            defaultAdapter.registerDefaultListeners();
        };


        this.create = function (targetElement) {
            return new PlayerAdapterHTML5(targetElement);
        };
    };
    return new PlayerAdapterFactoryHTML5();

}]);

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
.factory('PlayerAdapterFactoryVIDEOJS', ['PlayerAdapter', 'PlayerAdapterFactoryDefault', function (PlayerAdapter, PlayerAdapterFactoryDefault) {

    var PlayerAdapterFactoryVideoJs = function () {

        /**
         * Implementation of the player adapter for the HTML5 native player
         * @constructor
         * @alias module:player-adapter-HTML5.PlayerAdapterVideoJs
         * @augments {module:player-adapter.PlayerAdapter}
         * @param {DOMElement} targetElement DOM Element representing the player
         */
        var PlayerAdapterVideoJs = function (targetElement) {
            'use strict';

            var defaultAdapter, eventMapping = PlayerAdapter.eventMapping();

            eventMapping
                .map(PlayerAdapter.EVENTS.PAUSE, 'pause')
                .map(PlayerAdapter.EVENTS.PLAY, 'play')
                .map(PlayerAdapter.EVENTS.READY, 'ready')
                .map(PlayerAdapter.EVENTS.TIMEUPDATE, 'timeupdate')
                .map(PlayerAdapter.EVENTS.DURATION_CHANGE, 'durationchange')
                .map(PlayerAdapter.EVENTS.CAN_PLAY, 'canplay')
                .map(PlayerAdapter.EVENTS.VOLUMECHANGE, 'volumechange');

            // Check if the given target Element is valid
            if (typeof targetElement === 'undefined' || targetElement === null) {
                throw 'The given target element must not be null and have to be a valid HTMLElement!';
            }

            /**
             * Id of the player adapter
             * @inner
             * @type {String}
             */
            this.id = 'PlayerAdapter' + targetElement.id;

            function initPlayer() {
                var myPlayer = videojs(targetElement);
                myPlayer.controls(false);
                myPlayer.controlBar.hide();
                myPlayer.dimensions('auto', 'auto');
                return myPlayer;
            }

            /**
             * Register a listener listening to events of type. The event name will be translated from
             * API event (@see PlayerAdapter) to native events of the player implementation.
             *
             * @param type
             * @param listener
             */
            this.addListener = function (type, listener) {
                targetElement.addEventListener(eventMapping.resolveNativeName(type), listener);
            };


            // =========================
            // INITIALIZATION
            // =========================

            // Instantiate DefaultAdapter and copy its methods to this adapter.
            defaultAdapter = PlayerAdapterFactoryDefault.create(targetElement);
            defaultAdapter.extend(this);
            defaultAdapter.registerDefaultListeners();

            initPlayer();

        };


        this.create = function (targetElement) {
            return new PlayerAdapterVideoJs(targetElement);
        };
    };
    return new PlayerAdapterFactoryVideoJs();

}]);

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
.service('PlayerAdapter', [function () {

    /**
     * Possible player status
     * @readonly
     * @enum {number}
     */
    this.STATUS = {
        INITIALIZING: 0,
        LOADING: 1,
        SEEKING: 2,
        PAUSED: 3,
        PLAYING: 4,
        ENDED: 5,
        ERROR_NETWORK: 6,
        ERROR_UNSUPPORTED_MEDIA: 7
    };

    /**
     * Player adapter event
     * @readonly
     * @enum {string}
     */
    this.EVENTS = {
        PLAY: 'play',
        PAUSE: 'pause',
        SEEKING: 'seeking',
        READY: 'ready',
        TIMEUPDATE: 'timeupdate',
        ERROR: 'error',
        ENDED: 'ended',
        CAN_PLAY: 'canplay',
        DURATION_CHANGE: 'durationchange',
        VOLUMECHANGE: 'volumechange'
    };

    this.eventMapping = function () {
        var EventMapping = function () {
            var mapping = {};

            this.map = function (apiEvent, nativeEvent) {
                mapping[apiEvent] = nativeEvent;
                return this;
            };

            this.resolveNativeName = function (apiEvent) {
                var nativeEvent = mapping[apiEvent];
                if (nativeEvent === undefined) {
                    throw Error('native event for [' + apiEvent + '] not found');
                }
                return nativeEvent;
            };
        };

        return new EventMapping();
    };
}]);

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

angular.module('adminNg').config(['$provide', '$httpProvider', function ($provide, $httpProvider) {
  
  // Intercept http calls.
  $provide.factory('HttpInterceptor', ['$q', 'Notifications', function ($q, Notifications) {
    var unresponsiveNotifications,
        addNotification = function (message) {
          if (angular.isDefined(unresponsiveNotifications)) {
              Notifications.remove(unresponsiveNotifications);
          }

          unresponsiveNotifications = Notifications.add('error', message, 'global', -1); 
        };


    return {

      response: function (response) {
        if (angular.isDefined(unresponsiveNotifications)) {
          Notifications.remove(unresponsiveNotifications);
          unresponsiveNotifications = undefined;
        }

        return response;
      },
 
      responseError: function (rejection) {
        switch (rejection.status) {
          case -1:
            addNotification('SERVICE_UNAVAILABLE');
            break;
          case 503:
            addNotification('SERVER_UNRESPONSIVE');
            break;
          case 419:
            // Try to access index.html again --> will redirect to the login page
            location.reload(true);
            break;
        }

        return $q.reject(rejection);
      }
    };
  }]);
 
  // Add the interceptor to the $httpProvider.
  $httpProvider.interceptors.push('HttpInterceptor');
 
}]);

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

// Filter modules registry
angular.module('adminNg.filters', []);

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
.filter('localizeDate', ['Language', function (Language) {
    return function (input, type, format) {
        if (angular.isUndefined(format)) {
            format = 'short';
        }

        switch (type) {
            case 'date':
                return Language.formatDate(format, input);
            case 'dateTime':
                return Language.formatDateTime(format, input);
            case 'time':
                if (!angular.isDate(Date.parse(input))) {
                    return input;
                }

                return Language.formatTime(format, input);
            default:
                return input;
        }
    };
}]);

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
.filter('addLeadingZeros', [function () {
    return function (input, digitNumber) {
        var number = parseInt(input, 10);
        digitNumber = parseInt(digitNumber, 10);
        if (isNaN(number) || isNaN(digitNumber)) {
            return number;
        }
        number = '' + number;
        while (number.length < digitNumber) {
            number = '0' + number;
        }
        return number;
    };
}]);

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

/*
 Transforms numbers of bytes into human readable format
 */
angular.module('adminNg.filters')
    .filter('humanBytes', [function () {
        return function (bytesValue) {

            if (angular.isUndefined(bytesValue)) {
                return;
            }

            // best effort, independent on type
            var bytes = parseInt(bytesValue);

            if(isNaN(bytes)) {
                return bytesValue;
            }

            // from http://stackoverflow.com/a/14919494
            var thresh = 1000;
            if (Math.abs(bytes) < thresh) {
                return bytes + ' B';
            }
            var units = ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];
            var u = -1;
            do {
                bytes /= thresh;
                ++u;
            } while (Math.abs(bytes) >= thresh && u < units.length - 1);

            return bytes.toFixed(1) + ' ' + units[u];

        };
    }]);

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
    .filter('humanDuration', [function () {
        return function (durationInMsecValue) {

            if(angular.isUndefined(durationInMsecValue)) {
                return;
            }

            // best effort, independent on type
            var durationInMsec = parseInt(durationInMsecValue);

            if(isNaN(durationInMsec)) {
                return durationInMsecValue;
            }

            var durationInSec = parseInt(durationInMsec / 1000);

            var min = 60;
            var hour = min * min;

            var hours = parseInt(durationInSec / hour);
            var rest = durationInSec % hour;
            var minutes = parseInt(rest / min);
            rest = rest % min;
            var seconds =  parseInt(rest % min);

            if(seconds < 10) {
                // add leading zero
                seconds = '0' + seconds;
            }

            if (hours > 0 && minutes < 10) {
                minutes = '0' + minutes;
            }

            var minWithSec = minutes + ':' + seconds;
            if (hours > 0) {
                return hours + ':' + minWithSec;
            }
            return minWithSec;
        };
    }]);

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
.filter('joinBy', [function () {
    return function (input, delimiter) {
        return (input || []).join(delimiter || ',');
    };
}]);

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
.filter('presentValue', [function () {
    return function (input, delimiter) {
        if (angular.isUndefined(delimiter)) {
            delimiter = ', ';
        }
        if (input instanceof Array) {
            return input.join(delimiter);
        } else {
            return input;
        }
    };
}]);

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
.filter('removeQueryParams', [function () {
    return function (input) {
        if (angular.isUndefined(input)) {
           return input;
        }
        return input.split('?')[0];
    };
}]);

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
.filter('trusted', ['$sce', function ($sce) {
    return function(url) {
        return $sce.trustAsResourceUrl(url);
    };
}]);

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

// Directive modules registry
angular.module('adminNg.directives', []);

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
    .directive('customIcon', function() {
    return function($scope, element, attrs) {
        attrs.$observe('customIcon', function(url) {
            if (angular.isDefined(url) && url !== '') {
                element.css({
                    'background-image': 'url(' + url +')'
                });
            }
        });
    };
});

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
.directive('datepicker', ['Language', function (Language) {

    function getCurrentLanguageCode() {
        var lc = Language.getLanguageCode();
        lc = lc.replace(/\_.*/, ''); // remove long locale, as the datepicker does not support this
        return lc;
    }


    return {
        // Enforce the angularJS default of restricting the directive to // attributes only
        restrict: 'A',
        // Always use along with an ng-model
        require: '?ngModel',
        scope: {
            // This method needs to be defined and
            // passed in to the directive from the view controller
            select: '&' // Bind the select function we refer to the
            // right scope
        },
        link: function (scope, element, attrs, ngModel) {

            var defaultDate;

            if (scope.params) {
                defaultDate = new Date(scope.params);
            } else if (scope.$parent && scope.$parent.params && scope.$parent.params.value) {
                defaultDate = new Date(scope.$parent.params.value.replace(/(\d{2})-(\d{2})-(\d{4})/, '$2/$1/$3'));
            }


            if (ngModel) {

                var updateModel = function (dateTxt) {
                    scope.$apply(function () {
                        // Call the internal AngularJS helper to
                        // update the two-way binding
                        ngModel.$setViewValue(dateTxt);
                    });
                };


                var optionsObj = {};
                optionsObj.defaultDate = defaultDate;
                optionsObj.dateFormat = 'yy-mm-dd';
                optionsObj.onSelect = function (dateTxt, picker) {
                    setTimeout(function(){
                        var year = picker.selectedYear,
                            month = picker.selectedMonth + 1,
                            day = picker.selectedDay;

                        updateModel(year + '-' + ('0' + month).slice(-2) + '-' + ('0' + day).slice(-2));
                        if (scope.select) {
                            scope.$apply(function () {
                                scope.select({date: dateTxt});
                            });
                        }
                    });

                    $.datepicker._hideDatepicker();
                };

                $.datepicker.setDefaults($.datepicker.regional[getCurrentLanguageCode()]);

                element.datepicker(optionsObj);
            }

            scope.$on('$destroy', function () {
                try {
                    element.datepicker('destroy');
                } catch (e) { }
            });
        }
    };
}]);

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
.directive('datetimepicker', ['Language', function (Language) {

    function getCurrentLanguageCode() {
        var lc = Language.getLanguageCode() || 'en';
        lc = lc.replace(/\_.*/, ''); // remove long locale, as the datepicker does not support this
        return lc;
    }


    return {
        // Enforce the angularJS default of restricting the directive to // attributes only
        restrict: 'A',
        // Always use along with an ng-model
        require: '?ngModel',
        scope: {
            // This method needs to be defined and
            // passed in to the directive from the view controller
            select: '&' // Bind the select function we refer to the
            // right scope
        },
        link: function (scope, element, attrs, ngModel) {
            var dateValue;

            if (scope.params) {
                dateValue = new Date(scope.params);
            } else if (scope.$parent && scope.$parent.params && scope.$parent.params.value) {
                dateValue = new Date(scope.$parent.params.value);
            }

            if (ngModel) {

                var updateModel = function (date) {
                    scope.$apply(function () {
                        // Call the internal AngularJS helper to
                        // update the two-way binding
                        ngModel.$setViewValue(date.toISOString());
                    });
                };


                var optionsObj = {};
                optionsObj.timeInput = true;
                optionsObj.showButtonPanel = true;
                optionsObj.onClose = function () {
                    var newDate = element.datetimepicker('getDate');
                    setTimeout(function(){
                        updateModel(newDate);
                        if (scope.select) {
                            scope.$apply(function () {
                                scope.select({date: newDate});
                            });
                        }
                    });
                };

                var lc = getCurrentLanguageCode();
                $.datepicker.setDefaults($.datepicker.regional[lc]);
                $.timepicker.setDefaults($.timepicker.regional[lc === 'en' ? '' : lc]);

                element.datetimepicker(optionsObj);
                element.datetimepicker('setDate', dateValue);
                ngModel.$setViewValue(dateValue.toISOString());
            }

            scope.$on('$destroy', function () {
                try {
                    element.datetimepicker('destroy');
                } catch (e) {}
            });
        }
    };
}]);

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
    .directive('focushere', ['$timeout', function($timeout) {
    return {
      restrict: 'A',
      link : function($scope, $element, $attributes) {
        if ($attributes.focushere === undefined || $attributes.focushere === "1" || $attributes.focushere === false) {
          $timeout(function() {
            if (angular.element($element[0]).hasClass("chosen-container")) {
              angular.element($element[0]).trigger('chosen:activate');
            } else {
              $element[0].focus();
            }
          });
        }
      }
    }
}]);

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
.directive('oldAdminNgDropdown', function () {
    return {
        scope: {
            fn: '='
        },
        link: function ($scope, element) {
            element.on('click', function (event) {
                event.stopPropagation();
                angular.element(this).toggleClass('active');
            });

            $scope.$on('$destroy', function () {
                element.off('click');
            });
        }
    };
});

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
 * @name adminNg.modal.adminNgPwCheck
 * @description
 * Checks a password for correct repetition.
 *
 * Usage: Set this directive in the input element of the password.
 *
 * @example
 * <input admin-ng-pw-check="model.repeatedPassword"/>
 */
angular.module('adminNg.directives')
.directive('adminNgPwCheck', function () {
    return {
        require: 'ngModel',
        link: function (scope, elem, attrs, ctrl) {
            scope.deregisterWatch = scope.$watch(attrs.adminNgPwCheck, function (confirmPassword) {
                var isValid = ctrl.$viewValue === confirmPassword;
                ctrl.$setValidity('pwmatch', isValid);
            });

            scope.$on('$destroy', function () {
                scope.deregisterWatch();
            });
        }
    };
});

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
.directive('adminNgSelectBox', [function () {
    return {
        restrict: 'E',
        templateUrl: 'shared/partials/selectContainer.html',
        scope: {
            resource: '=',
            groupBy:  '@',
            disabled: '=',
            loader: '=',
            ignore: '@',
            height: '@'
        },
        controller: ["$scope", function ($scope) {

            $scope.searchField = '';

            $scope.customFilter = function () {

                return function (item) {

                    var result = true;
                    if (!angular.isUndefined($scope.ignore)) {
                         result = !(item.name.substring(0, ($scope.ignore).length) ===  $scope.ignore)
                    }

                    if (result && ($scope.searchField != '')) {
                        result = (item.name.toLowerCase().indexOf($scope.searchField.toLowerCase()) >= 0);
                    }

                    return result;
                };
            };

            $scope.customSelectedFilter = function () {

                return function (item) {
                    var result = true;

                    if (!angular.isUndefined($scope.ignore)) {
                        result = !(item.name.substring(0, ($scope.ignore).length) ===  $scope.ignore)
                    }

                    return result;
                }
            }

            $scope.move = function (key, from, to) {
                var j = 0;
                for (; j < from.length; j++) {
                    if (from[j].name === key) {
                        to.push(from[j]);
                        from.splice(j, 1);
                        return;
                    }
                }
            };

            $scope.add = function () {
                if (angular.isUndefined(this.markedForAddition)) { return; }

                for (var i = 0; i < this.markedForAddition.length; i++) {
                    this.move(this.markedForAddition[i].name, this.resource.available, this.resource.selected);
                }

                this.markedForAddition.splice(0);
            };

            $scope.remove = function () {
                if (angular.isUndefined(this.markedForRemoval)) { return; }

                for (var i = 0; i < this.markedForRemoval.length; i++) {
                    this.move(this.markedForRemoval[i].name, this.resource.selected, this.resource.available);
                }

                this.markedForRemoval.splice(0);
            };

            $scope.loadMore = function() {
                $scope.loader();
            };

            $scope.getHeight = function() {
                return {height: $scope.resource.searchable ? 'calc('+ $scope.height +' + 3em)' : $scope.height};
            }
        }]
    };
}]);

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
.directive('adminNgNav', ['HotkeysService', function (HotkeysService) {
    return {
        restrict: 'E',
        replace: true,
        templateUrl: 'shared/partials/mainNav.html',
        link: function (scope, element) {
            // Menu roll up
            var menu = element.find('#roll-up-menu'),
                marginTop = element.height() + 1,
                isMenuOpen = false;

            scope.toggleMenu = function () {
                var menuItems = element.find('#nav-container'),
                    mainView = angular.element('.main-view'),
                    mainViewLeft = '130px';
                if (isMenuOpen) {
                    menuItems.animate({opacity: 0}, 50, function () {
                        $(this).css('display', 'none');
                        menu.animate({opacity: 0}, 50, function () {
                            $(this).css('overflow', 'visible');
                            mainView.animate({marginLeft: '20px'}, 100);
                        });
                        isMenuOpen = false;
                    });
                } else {
                    mainView.animate({marginLeft: mainViewLeft}, 100, function () {
                        menu.animate({marginTop: marginTop, opacity: 1}, 50, function () {
                            $(this).css('overflow', 'visible');
                            menuItems.animate({opacity: 1}, 50, function () {
                                $(this).css('display', 'block');
                                menu.css('margin-right', '20px');
                            });
                            isMenuOpen = true;
                        });
                    });
                }
            };

            HotkeysService.activateHotkey(scope, "general.main_menu", "Main Menu", function(event) {
                event.preventDefault();
                scope.toggleMenu();
            });
        }
    };
}]);

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
 * @name adminNg.directives.adminNgTable
 * @description
 * Generates a table from the given resource.
 *
 * The generated table has the following features:
 * * Sorts by column without reloading the resource.
 * * Listens to changes to any filter values (see adminNg.directives.adminNgTableFilter).
 *
 * Future features:
 * * Pagination integration with the resource (records per page and offset).
 *
 * @example
 * <admin-ng-table="" table="table" />
 */
angular.module('adminNg.directives')
.directive('adminNgTable', ['Storage', '$translate', function (Storage, $translate) {
    var calculateWidth, setWidth;
    calculateWidth = function (label, element) {
        var testDiv, width;
        testDiv = element.find('#length-div').append(label).append('<i class="sort"></i>');
        width = testDiv.width();
        testDiv.html('');
        return width;
    };

    setWidth = function (translation, column, element) {
        var width;
        if (angular.isUndefined(translation)) {
            width = calculateWidth(column.label, element);
        } else {
            width = calculateWidth(translation, element);
        }
        column.style = column.style || {};
        column.style['min-width'] = (width + 40) + 'px';
    };

    return {
        templateUrl: 'shared/partials/table.html',
        replace: false,
        scope: {
            table: '='
        },
        link: function (scope, element) {
            scope.table.fetch(true);

            // Deregister change handler
            scope.$on('$destroy', function () {
                scope.deregisterChange();
            });

            // React on filter changes
            scope.deregisterChange = Storage.scope.$on('change', function (event, type) {
                if (type === 'filter') {
                    scope.table.fetch();
                }
                if (type === 'table_column_visibility') {
                    scope.table.refreshColumns();
                    scope.calculateStyles();
                }
            });

            scope.calculateStyles = function () {
                angular.forEach(scope.table.columns, function (column) {
                    if (angular.isDefined(column.width)) {
                        column.style = {'width': column.width};
                    } else {
                        $translate(column.label).then(function (translation) {
                            setWidth(translation, column, element);
                        });
                    }
                });
            };
        }
    };
}]);

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
 * @name adminNg.directives.adminNgStats
 * @description
 * Generates a stats bar from the given resource.
 */
angular.module('adminNg.directives')
.directive('adminNgStats', ['Storage', 'HotkeysService', function (Storage, HotkeysService) {
    var calculateWidth, setWidth;

    calculateWidth = function (label, element) {
        var testDiv, width;
        testDiv = element.find('#length-div').append(label).append('<i class="sort"></i>');
        width = testDiv.width();
        testDiv.html('');
        return width;
    };

    setWidth = function (translation, column, element) {
        var width;
        if (angular.isUndefined(translation)) {
            width = calculateWidth(column.label, element);
        } else {
            width = calculateWidth(translation, element);
        }
        column.style = column.style || {};
        column.style['min-width'] = (width + 22) + 'px';
    };

    return {
        templateUrl: 'shared/partials/stats.html',
        replace: false,
        scope: {
            stats: '='
        },
        link: function (scope) {
            scope.stats.fetch();
            scope.statsFilterNumber = -1;

            scope.showStatsFilter = function (index) {
                var filters = [];

                if (index >= scope.stats.stats.length - 1) {
                  index = scope.stats.stats.length - 1;
                } else if (index < 0) {
                  index = 0;
                }

                scope.statsFilterNumber = index;

                angular.forEach(scope.stats.stats[index].filters, function (filter) {
                  filters.push({namespace: scope.stats.resource, key: filter.name, value: filter.value});
                });
                Storage.replace(filters, 'filter');
            };

            HotkeysService.activateHotkey(scope, "general.select_next_dashboard_filter", "Select Next Dashboard Filter", function(event) {
                event.preventDefault();
                if (scope.statsFilterNumber >= scope.stats.stats.length - 1) {
                  scope.statsFilterNumber = -1;
                }
                scope.showStatsFilter(scope.statsFilterNumber + 1);
            });

            HotkeysService.activateHotkey(scope, "general.select_previous_dashboard_filter", "Select Previous Dashboard Filter", function(event) {
                event.preventDefault();
                if (scope.statsFilterNumber <= 0) {
                  scope.statsFilterNumber = scope.stats.stats.length;
                }
                scope.showStatsFilter(scope.statsFilterNumber - 1);
            });

            HotkeysService.activateHotkey(scope, "general.remove_filters", "Remove Filters", function(event) {
                event.preventDefault();
                Storage.remove('filter');
                scope.statsFilterNumber = -1;
            });
        }
    };
}]);

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
 * @name adminNg.modal.confirmationModal
 * @description
 * Opens the modal with the name specified in the `confirmation-modal` attribute.
 *
 * The success callback name- and id defined in the `callback` and `id` data
 * attributes will be sent to the {@link adminNg.modal.ConfirmationModal service's show method}.
 *
 * @example
 * <a data-confirmation-modal="confirm-deletion-modal"
 *    data-success-callback="delete" data-success-id="82">delete
 * </a>
 */
angular.module('adminNg.directives')
.directive('confirmationModal', ['ConfirmationModal', function (ConfirmationModal) {
    return {
        scope: {
            callback: '=',
            object:   '='
        },
        link: function ($scope, element, attr) {
            element.bind('click', function () {
                ConfirmationModal.show(attr.confirmationModal, $scope.callback, $scope.object);
            });

            $scope.$on('$destroy', function () {
                element.unbind('click');
            });
        }
    };
}]);

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
 * @name adminNg.directives.openModal
 * @description
 * Opens the modal with the name specified directive value.
 *
 * Optionally, a resource ID can be passed to the {@link adminNg.modal.ResourceModal service's show method}.
 * by setting the data attribute `resource-id` on the same element.
 *
 * @example
 * <a data-open-modal="recording-details" data-resource-id="82">details</a>
 */
angular.module('adminNg.directives')
.directive('openModal', ['ResourceModal', function (ResourceModal) {
    return {
        link: function ($scope, element, attr) {
            element.bind('click', function () {
                ResourceModal.show(attr.openModal, attr.resourceId, attr.tab, attr.action);
            });

            $scope.$on('$destroy', function () {
                element.unbind('click');
            });
        }
    };
}]);

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
.directive('adminNgEditable', ['AuthService', 'ResourcesListResource', function (AuthService, ResourcesListResource) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editable.html',
        transclude: true,
        replace: false,
        scope: {
            params: '=',
            save:   '=',
            target: '@', // can be used to further specify how to save data
            requiredRole: '@'
        },
        link: function (scope, element) {
            scope.mixed = false;
            scope.ordered = false;

            if (scope.params === undefined || scope.params.type === undefined) {
              console.warn("Illegal parameters for editable field");
              return;
            }
            if (scope.params.readOnly) {
                scope.mode = 'readOnly';
            } else {
                if (angular.isDefined(scope.requiredRole)) {
                    AuthService.userIsAuthorizedAs(scope.requiredRole, function () { }, function () {
                        scope.mode = 'readOnly';
                    });
                }

                if (scope.mode !== 'readOnly') {
                    if (typeof scope.params.collection === 'string') {
                        scope.collection = ResourcesListResource.get({ resource: scope.params.collection });
                    } else if (typeof scope.params.collection === 'object') {
                        scope.collection = scope.params.collection;
                    }

                    if (scope.params.type === 'boolean') {
                        scope.mode = 'booleanValue';
                    } else if (scope.params.type === 'date') {
                        scope.mode = 'dateValue';
                    } else {
                        if (scope.params.value instanceof Array) {
                            if (scope.collection) {
                                if (scope.params.type === 'mixed_text') {
                                    scope.mixed = true;
                                }
                                scope.mode = 'multiSelect';
                            } else {
                                scope.mode = 'multiValue';
                            }
                        } else {
                            if (scope.collection) {
                                if (scope.params.type === 'ordered_text') {
                                    scope.ordered = true;
                                }
                                scope.mode = 'singleSelect';
                            } else {
                                scope.mode = 'singleValue';
                            }
                        }
                    }
                }
            }

            if (scope.mode !== 'readOnly') {
                element.addClass('editable');
            }
        }
    };
}]);

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
.directive('adminNgEditableBooleanValue', function () {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableBooleanValue.html',
        replace: true,
        scope: {
            params:     '=',
            save:       '='
        },

        link: function (scope) {

            scope.submit = function () {
                // Prevent submission if value has not changed.
                if (scope.params.value === scope.original) { return; }

                scope.save(scope.params.id, function () {
                    scope.original = scope.params.value;
                });
            };
        }
    };
});

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
 * @name ng.directive:adminNgEditableDateValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field
 * which will be supported by a date picker.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`. The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
 <doc:example>
 <doc:source>
 <div admin-ng-editable-date params="params" save="save"></div>
 </doc:source>
 </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableDateValue', ['$timeout', function ($timeout) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableDateValue.html',
        replace: true,
        scope: {
            params: '=',
            save: '='
        },
        link: function (scope, element) {
            scope.enterEditMode = function () {
                // Store the original value for later comparision or undo
                if (!angular.isDefined(scope.original)) {
                    scope.original = scope.params.value;
                }
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                    element.find('input').focus();
                });
            };

            scope.keyUp = function (event) {
                if (event.keyCode === 27) {
                    // Restore original value on ESC
                    scope.params.value = scope.original;
                    scope.editMode = false;
                    // Prevent the modal from closing.
                    event.stopPropagation();
                }
                if (event.keyCode === 13) {
                  scope.submit();
                }
            };

            scope.submit = function () {
                // Prevent submission if value has not changed.
                if (scope.params.value === scope.original) { return; }

                scope.editMode = false;

                scope.save(scope.params.id, function () {
                    scope.original = scope.params.value;
                });
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
        }
    };
}]);

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
 * @name ng.directive:adminNgEditableSingleValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field
 * which will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required`, `value` and optionally `type` (defaults to 'text').
 * The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-single-value params="params" save="save"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableSingleValue', ['$timeout', 'JsHelper', function ($timeout, JsHelper) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableSingleValue.html',
        replace: true,
        scope: {
            params:     '=',
            save:       '='
        },
        link: function (scope, element) {
                // Parse the given time string (HH:mm) to separate the minutes / hours
            var parseTime = function (dateStr) {
                    var date = JsHelper.parseUTCTimeString(dateStr);
                    if (angular.isDate(date)) {
                        scope.params.hours = date.getHours();
                        scope.params.minutes = date.getMinutes();
                    }
                },
                // Check if the given value has two digits otherwise add a 0
                ensureTwoDigits = function (intValue) {
                    return (intValue < 10 ? '0' : '') + intValue;
                },
                // Format the value to be presented as string
                present = function (params) {
                    switch (params.type) {
                        case 'time':
                            if (angular.isUndefined(params.hours)) {
                                parseTime(params.value);
                            }
                            return ensureTwoDigits(params.hours) + ':' + ensureTwoDigits(params.minutes);
                        default:
                            return params.value;
                    }
                };

            scope.editMode = false;

            if (scope.params.type === 'time') {
                scope.hours = JsHelper.initArray(24);
                scope.minutes = JsHelper.initArray(60);
                parseTime(scope.params.value);
            }

            scope.presentableValue = present(scope.params);

            scope.enterEditMode = function () {
                // Store the original value for later comparision or undo
                if (!angular.isDefined(scope.original)) {
                    scope.original = scope.params.value;
                }
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                  if ((scope.params.type === 'text_long') && (element.find('textarea'))) {
                    element.find('textarea').focus();
                  } else if (element.find('input')) {
                    element.find('input').focus();
                  }
                });
            };

            scope.keyDown = function (event) {
		if (event.keyCode === 13 && !event.shiftKey) {
		    event.stopPropagation();
		    event.preventDefault();
		}
	    }


            scope.keyUp = function (event) {
                if (event.keyCode === 13 && !event.shiftKey) {
                    // Submit the form on ENTER
                    // Leaving the edit mode causes a blur which in turn triggers
                    // the submit action.
                    scope.editMode = false;
                    angular.element(event.target).blur();
                } else if (event.keyCode === 27) {
                    // Restore original value on ESC
                    scope.params.value = scope.original;
                    if (scope.params.type === 'time') {
                        parseTime(scope.params.value);
                    }
                    scope.editMode = false;
                    // Prevent the modal from closing.
                    event.stopPropagation();
                }
            };

            scope.submit = function () {
                if (scope.params.type === 'time') {
                    var newDate = new Date(0);
                    newDate.setHours(scope.params.hours, scope.params.minutes);
                    scope.params.value = ensureTwoDigits(newDate.getUTCHours()) + ':' +
                                         ensureTwoDigits(newDate.getUTCMinutes());
                }

                // Prevent submission if value has not changed.
                if (scope.params.value === scope.original) {
                    scope.editMode = false;
                    return;
                 }

                scope.presentableValue = present(scope.params);
                scope.editMode = false;

                if (!_.isUndefined(scope.params)) {
                    scope.save(scope.params.id);
                    scope.original = scope.params.value;
                    if (scope.params.type === 'time') {
                        parseTime(scope.params.value);
                    }
                }
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
       }
    };
}]);

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
 * @name ng.directive:adminNgEditableMultiValue
 *
 * @description
 * Upon click on its label, this directive will display an <input> field and
 * currently selected values. They will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`.
 * The "save" attribute is a reference to a save function used to persist the
 * values.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-multi-value params="params" save="save"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableMultiValue', ['$timeout', function ($timeout) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableMultiValue.html',
        replace: true,
        scope: {
            params:     '=',
            save:       '='
        },
        link: function (scope, element) {
            scope.enterEditMode = function () {
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                    element.find('input').focus();
                });
            };

            scope.leaveEditMode = function () {
                scope.addValue(scope.params.value, scope.value);
                scope.editMode = false;
                scope.value = '';
            };

            scope.addValue = function (model, value) {
                if (value && model.indexOf(value) === -1) {
                    model.push(value);
                    scope.editMode = false;
                }
                scope.submit();
            };

            scope.removeValue = function (model, value) {
                model.splice(model.indexOf(value), 1);
                scope.submit();
            };

            scope.keyUp = function (event) {
                if (event.keyCode === 13) {
                    // ENTER
                    scope.addValue(scope.params.value, scope.value);
                } else if (event.keyCode === 27) {
                    // ESC
                    scope.editMode = false;
                }
                event.stopPropagation();
            };

            scope.submit = function () {
                scope.save(scope.params.id);
                scope.editMode = false;
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
       }
    };
}]);

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
 * @name ng.directive:adminNgEditableSingleSelect
 *
 * @description
 * Upon click on its label, this directive will display an <select> field
 * which will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`.
 * The "collection" attribute contains a hash of objects (or a promise thereof)
 * which maps values to their labels.
 * The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-single-select params="params" save="save" collection="collection"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableSingleSelect', ['$timeout', '$filter', function ($timeout, $filter) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableSingleSelect.html',
        replace: true,
        scope: {
            params:     '=',
            collection: '=',
            ordered:    '=',
            save:       '='
        },
        link: function (scope, element) {

            var mapToArray = function (map, translate) {
                var array = [];

                angular.forEach(map, function (mapValue, mapKey) {

                    array.push({
                        label: translate ? $filter('translate')(mapKey) : mapKey,
                        value: mapValue
                    });
                });

                return $filter('orderBy')(array, 'label');
            }

            var mapToArrayOrdered = function (map, translate) {
                var array = [];

                angular.forEach(map, function (mapValue, mapKey) {
                    var entry = JSON.parse(mapKey);
                    if (entry.selectable || scope.params.value === mapValue) {
                        array.push({
                            label: entry,
                            value: mapValue
                        });
                    }
                });
                array.sort(function(a, b) {
                    return a.label.order - b.label.order;
                });
                return array.map(function (entry) {
                    return {
                        label: translate ? $filter('translate')(entry.label.label) : entry.label.label,
                        value: entry.value
                    };
                });
            }


            //transform map to array so that orderBy can be used
            scope.collection = scope.ordered ? mapToArrayOrdered(scope.collection, scope.params.translatable) :
                mapToArray(scope.collection, scope.params.translatable);

            scope.submit = function () {
                // Wait until the change of the value propagated to the parent's
                // metadata object.
                scope.submitTimer = $timeout(function () {
                    scope.save(scope.params.id);
                });
                scope.editMode = false;
            };

            scope.getLabel = function (searchedValue) {
                var label;

                angular.forEach(scope.collection, function (obj) {
                    if (obj.value === searchedValue) {
                        label = obj.label;
                    }
                });

                return label;
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.submitTimer);
            });

            scope.enterEditMode = function () {
                // Store the original value for later comparision or undo
                if (!angular.isDefined(scope.original)) {
                    scope.original = scope.params.value;
                }
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                  if ($('[chosen]')) {
                    element.find('select').trigger('chosen:activate');
                  }
                });
            };

            scope.leaveEditMode = function () {
                // does not work currently, as angular chose does not support ng-blur yet. But it does not break anything
                scope.editMode = false;
            };
       }
    };
}]);

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
 * @name ng.directive:adminNgEditableMultiSelect
 *
 * @description
 * Upon click on its label, this directive will display an <input> field and
 * currently selected values. They will be transformed back into a label on blur.
 *
 * @element field
 * The "params" attribute contains an object with the attributes `id`,
 * `required` and `value`.
 * The "collection" attribute contains a hash of objects (or a promise thereof)
 * which maps values to their labels.
 * The "save" attribute is a reference to a save function used to persist
 * the value.
 *
 * @example
   <doc:example>
     <doc:source>
      <div admin-ng-editable-multi-select params="params" save="save" collection="collection"></div>
     </doc:source>
   </doc:example>
 */
angular.module('adminNg.directives')
.directive('adminNgEditableMultiSelect', ['$timeout', function ($timeout) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/editableMultiSelect.html',
        replace: true,
        scope: {
            params:     '=',
            collection: '=',
            mixed:      '=',
            save:       '='
        },
        link: function (scope, element) {
            scope.data = {};
            scope.data.list = {};
            if (scope.params.id) {
                scope.data.list.id = scope.params.id;
            } else {
                scope.data.list.id = scope.params.name;
            }

            scope.enterEditMode = function () {
                scope.parseValues();
                scope.editMode = true;
                scope.focusTimer = $timeout(function () {
                    element.find('input').focus();
                });
            };

            scope.leaveEditMode = function () {
                scope.storeValues();
                scope.editMode = false;
                scope.value = '';
            };

            scope.addValue = function (model, value) {
                if (value && model.indexOf(value) === -1) {
                    model.push(value);
                    scope.editMode = false;
                }
                scope.submit();
            };

            scope.removeValue = function (model, value) {
                model.splice(model.indexOf(value), 1);
                scope.submit();
            };

            scope.storeValues = function () {
              scope.parseValues();
              if (scope.mixed || scope.collection[scope.value]) {
                  var newValue = angular.isDefined(scope.collection[scope.value]) ? scope.collection[scope.value] : scope.value;
                  scope.addValue(scope.params.value, newValue);
              }
            };

            scope.keyUp = function (event) {
                var value = event.target.value;
                if (angular.isDefined(scope.value)) {
                    scope.value = scope.value.trim();
                }
                if (event.keyCode === 13) {
                    // ENTER
                    scope.storeValues();
                } else if (event.keyCode === 27) {
                    // ESC
                    scope.editMode = false;
                } else if (value.length >= 2) {
                    // TODO update the collection
                    scope.collection = scope.collection;
                }
                event.stopPropagation();
            };

            scope.getText = function (value) {
                if (angular.isDefined(scope.collection[value])) {
                    return scope.collection[value];
                } else {
                    return value;
                }
            };

            /**
             * This function parses the current values by removing extra whitespace and replacing values with those in the collection.
             */
            scope.parseValues = function () {
                scope.trimValues();
                scope.findCollectionValue();
            };

            /**
             * This function trims the whitespace from all of the values.
             */
            scope.trimValues = function () {
               angular.forEach(scope.params.value, function(value) {
                   scope.params.value[scope.params.value.indexOf(value)] = scope.params.value[scope.params.value.indexOf(value)].trim();
               });
            };

            /**
             * This function replaces all of the current values with those in the collection.
             */
            scope.findCollectionValue = function() {
                angular.forEach(scope.params.value, function(value) {
                    if (angular.isDefined(scope.collection[value])) {
                        scope.params.value[scope.params.value.indexOf(value)] = scope.collection[value];
                    }
                });
            };

            scope.submit = function () {
                scope.parseValues();
                if (angular.isDefined(scope.save)) {
                    scope.save(scope.params.id);
                }
                scope.editMode = false;
            };

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.focusTimer);
            });
       }
    };
}]);

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
.directive('adminNgTimeline', ['AuthService', 'PlayerAdapter', '$document', 'VideoService', '$timeout',
function (AuthService, PlayerAdapter, $document, VideoService, $timeout) {

    return {
        templateUrl: 'shared/partials/timeline.html',
        priority: 0,
        scope: {
            player: '=',
            video: '='
        },
        link: function (scope, element) {
            var replaySegment = {};

            scope.position = 0;
            scope.positionStyle = 0;
            scope.widthPerSegment = 0;

            scope.ZoomSelectOptions = [
                { name: 'All', time: 0 }
                , { name: '10 m', time: 600000 }
                , { name: '5 m', time: 300000 }
                , { name: '1 m', time: 60000 }
                , { name: '30 s', time: 30000 }
            ];

            scope.zoomLevel = 0;
            scope.zoomValue = 0;
            scope.zoomSelected = scope.ZoomSelectOptions[0];
            scope.zoomOffset = 0;
            scope.zoomFieldOffset = 0;

            scope.from = 0;
            scope.to = 0;

            scope.previewMode = true; // in preview mode, deactivated segments are skipped while playing.

            if (AuthService) {
                var ADMIN_EDITOR_PREVIEWMODE_DEFAULT = 'admin.editor.previewmode.default';
                AuthService.getUser().$promise.then(function(user) {
                    if (angular.isDefined(user.org.properties[ADMIN_EDITOR_PREVIEWMODE_DEFAULT])) {
                        scope.previewMode = user.org.properties[ADMIN_EDITOR_PREVIEWMODE_DEFAULT].toUpperCase() === 'TRUE';
                    }
                });
            }

            scope.wrapperClass = ''; // list of border classes for the segment wrapper.

            scope.player.adapter.addListener(PlayerAdapter.EVENTS.DURATION_CHANGE, function () {

                // reset then remove the items that are longer than the video duration
                scope.ZoomSelectOptions = [
                    { name: 'All', time: 0 }
                    , { name: '10 m', time: 600000 }
                    , { name: '5 m', time: 300000 }
                    , { name: '1 m', time: 60000 }
                    , { name: '30 s', time: 30000 }
                ];

                var i = scope.ZoomSelectOptions.length;
                while (i--) {
                    if (scope.ZoomSelectOptions[i].time > (scope.video.duration - 1000)) {
                        scope.ZoomSelectOptions.splice(i, 1);
                    }
                }

                scope.zoomValue = scope.getZoomValue();
                scope.zoomOffset = scope.getZoomOffset();
                scope.zoomFieldOffset = scope.getZoomFieldOffset();
                scope.setWrapperClasses();
            });

            scope.player.adapter.addListener(PlayerAdapter.EVENTS.TIMEUPDATE, function () {
                scope.position = scope.player.adapter.getCurrentTime() * 1000;
                scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';

                var segment = VideoService.getCurrentSegment(scope.player, scope.video);

                // Mark current segment as selected
                scope.selectSegment(segment);

                // Stop play back when switching from a replayed segment to
                // the next.
                var nextActiveSegment = VideoService.getNextActiveSegment(scope.player, scope.video);
                if (replaySegment.replay && !nextActiveSegment.replay) {
                    scope.player.adapter.pause();
                    scope.player.adapter.setCurrentTime(replaySegment.start / 1000);
                    replaySegment.replay = false;
                    return;
                }
                if (segment.replay) {
                    replaySegment = segment;
                }

                // When in preview mode, skip deleted segments while playing
                if (scope.previewMode && segment.deleted && scope.player.adapter.getStatus() === PlayerAdapter.STATUS.PLAYING) {
                    scope.player.adapter.setCurrentTime(segment.end / 1000);
                }
            });

            /**
            * Formats time stamps to HH:MM:SS.sss
            *
            * @param {Number} ms is the time in milliseconds,
            * @param {Boolean} showMilliseconds should the milliseconds be displayed
            * @return {String} Formatted time string
           */
            scope.formatMilliseconds = function (ms, showMilliseconds) {

               if (isNaN(ms)) {
                   return '';
               }

               var date = new Date(ms),
                   pad = function (number, padding) {
                       return (new Array(padding + 1).join('0') + number)
                           .slice(-padding);
                   };

               if (typeof showMilliseconds === 'undefined') {
                   showMilliseconds = true;
               }

               return pad(date.getUTCHours(), 2) + ':' +
                   pad(date.getUTCMinutes(), 2) + ':' +
                   pad(date.getUTCSeconds(), 2) +
                   (showMilliseconds ? '.' + pad(date.getUTCMilliseconds(), 3) : '');
            };

            /**
             * Display the current zoom level ms value into human readable value
             * in the existing drop down > overriding the display HTML
             *
             * @param {Number} ms millisecond value of the current zoom level
             */
            scope.displayZoomLevel = function (ms) {

                var date = new Date(ms),
                    st = '&#8776; ';

                if (date.getUTCHours() > 0) {
                    st += (date.getUTCHours() + (date.getUTCMinutes() >= 30 ? 1 : 0)) + ' h';
                }
                else if (date.getUTCMinutes() > 0) {
                    st += (date.getUTCMinutes() + (date.getUTCSeconds() >= 30 ? 1 : 0)) + ' m';
                } else {
                    st += date.getUTCSeconds() + ' s';
                }

                var dropdown_text = element.find('.zoom-control .chosen-container > a > span'),
                    dropdown = element.find('.zoom-control #zoomSelect');

                if (dropdown_text) {
                    dropdown_text.html(st);
                }

                if (dropdown) {
                    dropdown.attr('data-placeholder', st);
                }
            };

            /**
             * Calculates the relative width of a track segment.
             *
             * @param {Object} segment Segment object
             * @return {String} Width of the segment in percent
             */
            scope.getSegmentWidth = function (segment, dropPercent) {
                if (angular.isUndefined(segment)) {
                    return 0;
                }

                if (angular.isUndefined(scope.video.duration)) {
                    return 0;
                } else{
                    scope.video.duration = parseInt(scope.video.duration, 10);
                }

                var zoom = 1,
                    absoluteSize = segment.end - segment.start,
                    relativeSize = absoluteSize / scope.video.duration,
                    scaledSize = relativeSize * zoom;

                return (scaledSize * 100) + (!dropPercent ? '%' : 0);
            };

            /**
             * Returns the visible amount of milliseconds for the current zoom level.
             *
             * The zoom slider provides values from 0 (zoomed out) to 100
             * (fully zoomed in). When zoomed out, the entire video should
             * be visible whereas when fully zoomed in, 10s should be visible.
             *
             * These constraints can be derived to the following linear equation:
             *
             *                10,000 - duration
             * y(zoomlevel) = ----------------- * zoomlevel + duration
             *                       100
             *
             * @return {Number} Visible interval in milliseconds
             */
            scope.getZoomValue = function () {
                return (10000 - scope.video.duration) / 100 * scope.zoomLevel +
                    scope.video.duration;
            };

            /**
             * Returns the offset for the currently visible portion.
             *
             * Based on the following linear equation.
             *
             *          duration
             * y(pos) = -------- * pos - pos
             *           zoom
             *
             * @return {Number} Relative offset
             */
            scope.getZoomOffset = function () {
                return scope.position * scope.video.duration / scope.zoomValue -
                    scope.position;
            };

            /**
             * On change of zoom range slider updates the appropriate zoom select option
             *
             * @param {Event} event object
             */
            scope.changeZoomLevel = function (event) {

                // Cache the zoom value and position
                scope.zoomValue = scope.getZoomValue();
                scope.zoomOffset = scope.getZoomOffset();
                scope.zoomFieldOffset = scope.getZoomFieldOffset();
                scope.setWrapperClasses();

                if (scope.zoomValue >= 0) {
                    scope.zoomSelected = "";
                    scope.displayZoomLevel(scope.zoomValue);
                } else {
                    scope.zoomSelected = scope.ZoomSelectOptions[0];
                }
            };

            /**
             * On change of the zoom selected drop-down
             *
             * @param {Event} event object
             */
            scope.changeZoomSelected = function (event) {

                if (typeof (scope.zoomSelected) === 'object') {

                    if (scope.zoomSelected.time !== 0) {
                        scope.zoomLevel = (scope.zoomSelected.time - scope.video.duration) / ((10000 - scope.video.duration) / 100);
                    } else {
                        scope.zoomLevel = 0;
                    }

                    // Cache the zoom value and position
                    scope.zoomValue = scope.getZoomValue();
                    scope.zoomOffset = scope.getZoomOffset();
                    scope.zoomFieldOffset = scope.getZoomFieldOffset();
                    scope.setWrapperClasses();

                    var dropdown = element.find('.zoom-control #zoomSelect');

                    if (dropdown) {
                        dropdown.attr('data-placeholder', dropdown.data('data-translated'));
                    }
                }
            }

            /**
             * Sets the classes for the segment wrapper for displaying the correct border colours.
             */
            scope.setWrapperClasses = function () {
                if (angular.isUndefined(scope.video.duration)) {
                    return;
                }

                var classes = [];

                angular.forEach(scope.video.segments, function (segment) {

                    if ((segment.start <= scope.zoomFieldOffset) && (segment.end >= scope.zoomFieldOffset)) {
                        classes[0] = 'left-' + (segment.deleted ? ( segment.selected ? 'deleted-selected' : 'deleted') : ( segment.selected ? 'selected' : 'normal'));
                    }

                    if ((segment.start <=  (scope.zoomFieldOffset+scope.zoomValue)) && (segment.end >=  (scope.zoomFieldOffset+scope.zoomValue))) {
                        classes[1] = 'right-' + (segment.deleted ? ( segment.selected ? 'deleted-selected' : 'deleted') : ( segment.selected ? 'selected' : 'normal'));
                    }
                });

                scope.wrapperClass = classes.join(' ');
            }
            /**
             * Returns a style for the given segment.
             *
             * Applies track background and zoom parameters.
             *
             * @param {Object} track Current track object
             * @return {Object} ng-style compatible hash
             */
            scope.getSegmentStyle = function (track) {
                if (angular.isUndefined(scope.video.duration)) {
                    return {};
                }

                var width = (scope.video.duration * 100 / scope.zoomValue),
                    left = (scope.zoomOffset * -100 / scope.video.duration);

                // if less than possible length then set to possible length
                if (scope.video.duration <= scope.zoomValue) {
                    width = 100;
                }

                var style = {
                    width: width + '%',
                    left:  left + '%',
                    'overflow-x': 'hidden'
                };

                if (track.waveform) {
                    style['background-image'] = 'url(' + track.waveform + ')';

                    var img = '.video-timeline .timeline-control .field-of-vision:before{'
                               + 'background-image: url("'+ track.waveform +'");';

                    if ($('#timeline-header').length) {
                        $('#timeline-header').html(img);
                    } else {
                        angular.element('head').append('<style id="timeline-header">'+ img +'</style>');
                    }
                }

                return style;
            };

            scope.getWaveformBg = function (track) {
                return {
                  'background-image': 'url(' + track.waveform + ')',
                  'background-repeat': 'no-repeat',
                };
            };

            /**
             * Returns an object that describes the css classes for a given segment.
             *
             * @param {Object} segment object
             * @return {Object} object with {class}: {boolean} values for CSS classes.
             */
            scope.getSegmentClass = function (segment) {
                var result = { deleted: segment.deleted, selected: segment.selected, small: false, tiny: false, sliver: false };

                if (angular.isUndefined(scope.video.duration)) {
                    return result;
                }

                var element = angular.element('.segments .segment[data-start='+segment.start +']'),
                    internal_widths = element.find('a').map( function(i,el){ return $(el).outerWidth(); }).toArray();

                try {
                    var total = internal_widths.reduce(function getSum(total, num) { return total + num; }),
                        single = (total / element.find('a').length),
                        segment_width = element.width();

                    if ( segment_width <= (total + 10)) {

                        if ( (single + 10) <= segment_width) {
                            // a single element can be shown
                            result.small = true;
                        }
                        else if (segment_width < 5) {
                            //minimum segment width
                            result.sliver = true;
                        }
                        else {
                            // smaller than a single element > show none
                            result.tiny = true;
                        }
                    }
                }
                catch(e) {

                    // When splitting segments the angular digest updates the segments items,
                    // triggering the ng-class directive but the html does not exist yet and
                    // the internal_widths array is empty - for these cases we return tiny.
                    // the digest will be called again and the class correctly assigned.

                    result.tiny = true;
                }

                return result;
            };

            /**
             * Calculates the offset for the zoom field of vision.
             *
             * Based on the following linear equation:
             *
             *          duration - zoom
             * f(pos) = --------------- * pos
             *             duration
             *
             * @return {Number} Offset in milliseconds
             */
            scope.getZoomFieldOffset = function () {
                return (scope.video.duration - scope.zoomValue) *
                        scope.position / scope.video.duration;
            };

            /**
             * Returns a style for the zoom field of vision.
             *
             * @return {Object} ng-style compatible hash
             */
            scope.getZoomStyle = function () {
                if (angular.isUndefined(scope.video.duration)) {
                    return {};
                }

                // Cache the zoom value and position
                scope.zoomValue = scope.getZoomValue();
                scope.zoomOffset = scope.getZoomOffset();
                scope.zoomFieldOffset = scope.getZoomFieldOffset();

                scope.from = scope.zoomFieldOffset;
                scope.to = scope.zoomFieldOffset + scope.zoomValue;

                var width = (scope.zoomValue * 100 / scope.video.duration),
                    left = (scope.zoomFieldOffset * 100 / scope.video.duration);

                // if less than possible length then set to possible length
                if (scope.video.duration <= scope.zoomValue) {
                    width = 100;
                    left = 0;
                }

                var style = {
                    width:  width + '%',
                    left: left + '%'
                };

                return style;
            };

            /**
             * The display classes to use for the zoom range element.
             *
             * @return {Object} object with {class}: {boolean} values for CSS classes.
             */
            scope.getZoomClass = function () {

                var field = angular.element('.field'),
                    width = field.width();

                return { 'active': (field.data('active') === true), 'small': (width <= 190) };
            };

            /**
             * Removes the given segment.
             *
             * The previous or, failing that, the next segment will take up
             * the space of the given segment.
             *
             * @param {Event} event that triggered the merge action
             * @param {Object} segment Segment object
             */
            scope.mergeSegment = function (event, segment) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (!scope.isRemovalAllowed(segment)) {
                    return;
                }

                var index = scope.video.segments.indexOf(segment);

                if (scope.video.segments[index - 1]) {
                    scope.video.segments[index - 1].end = segment.end;
                    scope.video.segments.splice(index, 1);
                } else if (scope.video.segments[index + 1]) {
                    scope.video.segments[index + 1].start = segment.start;
                    scope.video.segments.splice(index, 1);
                }

                scope.setWrapperClasses();
                scope.$root.$broadcast("segmentTimesUpdated");
            };

            /**
             * Toggle the deleted flag for a segment. Indicating if it should be used or not.
             *
             * @param {Event} event for checkbox link - stop the propogation
             * @param {Object} segment object on which the deleted variable will change
             */
            scope.toggleSegment = function (event, segment) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (!scope.isRemovalAllowed(segment)) {
                    return;
                }

                segment.deleted = !segment.deleted;
                scope.setWrapperClasses();
            };

            /**
             * Split the segment at this position
             *
             * The previous or, failing that, the next segment will take up
             * the space of the given segment.
             */
            scope.splitSegment = function () {
              var segment = VideoService.getCurrentSegment(scope.player, scope.video),
                  position = Math.floor(scope.player.adapter.getCurrentTime() * 1000),
                  newSegment = angular.copy(segment);

              // Shrink original segment
              segment.end = position;

              // Add additional segment containing the second half of the
              // original segment.
              newSegment.start = position;

              // Deselect the previous segment as the cursor is at the start
              // of the new one.
              delete segment.selected;

              // Insert new segment
              scope.video.segments.push(newSegment);

              // Sort array by start attribute
              scope.video.segments.sort(function (a, b) {
                  return a.start - b.start;
              });

              scope.setWrapperClasses();
              scope.$root.$broadcast("segmentTimesUpdated");
            };

            scope.isRemovalAllowed = function(segment) {
                if (!segment) {
                    return false;
                }

                return (segment.deleted || scope.video.segments
                                               .filter(function(seg) {
                                                   return !seg.deleted;
                                               }).length > 1);
            }

            /**
             * Catch all method to track the mouse movement on the page,
             * to calculate the movement of elements properly.
             *
             * @param {Event} e event of the mousemove
             */
            $document.mousemove(function(e) {
                $document.mx = document.all ? window.event.clientX : e.pageX;
                $document.my = document.all ? window.event.clientY : e.pageY;
            });

            /**
             * Mouseup event handler to finish up the move action for:
             * 1. Timeline handle
             * 2. Zoom field
             * 3. Segment start handle
             */
            $document.mouseup(function () {

                // Timeline mouse events
                if (scope.canMoveTimeline) {
                  scope.canMoveTimeline = false;
                  element.find('.field-of-vision .field').removeClass('active');
                  scope.player.adapter.setCurrentTime(scope.position / 1000);
                }

                // Timeline position - handle
                if (scope.canMove) {
                    scope.canMove = false;

                    if (scope.player.adapter.getStatus && scope.player.adapter.getStatus() === PlayerAdapter.STATUS.PLAYING) {

                        var cursor = element.find('#cursor_fake'),
                        handle = element.find('#cursor_fake .handle');

                        cursor.hide();

                        scope.player.adapter.setCurrentTime(handle.data('position') / 1000);
                    } else {

                        element.find('#cursor .handle').data('active', false);
                        scope.player.adapter.setCurrentTime(scope.position / 1000);
                    }

                    if (scope.doSplitSegment) {

                        scope.doSplitSegment = false;
                        scope.splitSegment();
                    }

                    // show small cut button below timeline handle
                    element.find('#cursor .arrow_box').show();

                    if (scope.timer) $timeout.cancel( scope.timer );
                    scope.timer = $timeout(
                        function() {
                            // hide cut window
                            element.find('#cursor .arrow_box').hide();
                        },
                        60000 //  1 min
                    );
                }

                // Segment start handle
                if (scope.movingSegment) {

                    var track = element.find('.segments'),
                        topTrack = track.parent(),
                        segment = scope.movingSegment.data('segment'),
                        index = scope.video.segments.indexOf(segment);

                    var pxPosition = scope.movingSegment.parent().offset().left + parseInt(scope.movingSegment.css('left'),10) - topTrack.offset().left + 3;
                    var position = Math.floor((pxPosition / track.width() * scope.video.duration) + scope.zoomFieldOffset);

                    if (position < 0) position = 0;
                    if (position >= scope.video.duration) position = scope.video.duration;

                    if (position >= segment.end) {
                        // pulled start point of segment past end of start
                        // so we flip it
                        segment.start = segment.end;
                        segment.end = position;
                    } else {
                        segment.start = position;
                    }

                    scope.movingSegment.css('background-position', (-pxPosition + 4) + 'px 0px');

                    // update the segments around the one that was changed
                    if (index - 1 >= 0) {

                        var before = scope.video.segments[index - 1];

                        before.end = segment.start;

                        if (before.end - before.start <= 0) {
                            // empty segment
                            if (!scope.isRemovalAllowed(before)) {
                                before.end = segment.start = before.start + 1;
                            }
                            else {
                                segment.start = before.start;
                                scope.video.segments.splice(index - 1, 1);
                            }
                        }
                    }

                    // Sort array by start attribute
                    scope.video.segments.sort(function (a, b) {
                        return a.start - b.start;
                    });
                    index = scope.video.segments.indexOf(segment);

                    if (index + 1 < scope.video.segments.length) {
                        var after = scope.video.segments[index + 1];

                        after.start = segment.end;

                        if (after.end - after.start <= 0) {
                            // empty segment
                            segment.end = after.end;
                            scope.video.segments.splice(index + 1, 1);
                        }
                    }

                    scope.movingSegment.removeClass('active');
                    scope.movingSegment.css('left', '-4px');
                    scope.movingSegment = null;

                    if (segment.end - segment.start <= 100) {
                        // i'm really small so should probably not exist anymore
                        scope.mergeSegment(null, segment);
                    }

                    // Sort array by start attribute
                    scope.video.segments.sort(function (a, b) {
                        return a.start - b.start;
                    });

                    scope.setWrapperClasses();
                    scope.$root.$broadcast("segmentTimesUpdated");
                }

                // Clean-up of mousemove handlers
                $document.unbind('mousemove', scope.movePlayHead);
                $document.unbind('mousemove', scope.moveTimeline);
                $document.unbind('mousemove', scope.moveSegment);
            });

            /**
             * Clicking on the playtrack moves the timeline handle to that position.
             *
             * @param {Event} event Event that triggered this method.
             */
            scope.clickPlayTrack = function (event) {

                if (event) {
                    event.preventDefault();

                    var el = $(event.target);

                    if (el.attr('id') === 'cursor-track') {

                        var position = (event.clientX - el.offset().left) / el.width() * scope.zoomValue + scope.zoomFieldOffset;

                        // Limit position to the length of the video
                        if (position > scope.video.duration) {
                            position = scope.video.duration;
                        }

                        if (position < 0) {
                            position = 0;
                        }

                        scope.player.adapter.setCurrentTime(position / 1000);
                        scope.setWrapperClasses();

                        // show small cut button below timeline handle
                        element.find('#cursor .arrow_box').show();

                        if (scope.timer) $timeout.cancel( scope.timer );
                        scope.timer = $timeout(
                        function() {
                                // hide cut window
                                element.find('#cursor .arrow_box').hide();
                            },
                            60000 // 1 min
                        );
                    }
                }
            };

            /**
             * Sets the timeline handle cursor position depending on the mouse coordinates.
             *
             * @param {Event} event the mousemove event details.
             */
            scope.movePlayHead = function (event) {
                event.preventDefault();
                if (!scope.canMove) { return; }

                var track = element.find('.timeline-track'),
                    handle = element.find('#cursor .handle'),
                    position_absolute = $document.mx - handle.data('dx') + handle.width() / 2 - track.offset().left,
                    position = position_absolute / track.width() * scope.video.duration;

                // Limit position to the length of the video
                if (position > scope.video.duration) {
                    position = scope.video.duration;
                }
                if (position < 0) {
                    position = 0;
                }

                scope.position = position;
                scope.$apply(function () {
                    scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';
                });

                scope.setWrapperClasses();
            };

            /**
             * Sets the fake timeline handle cursor position depending on the mouse coordinates.
             *
             * @param {Event} event the mousemove event details.
             */
            scope.moveFakePlayHead = function (event) {
                event.preventDefault();
                if (!scope.canMove) { return; }

                var track = element.find('.timeline-track'),
                    cursor = element.find('#cursor_fake'),
                    handle = element.find('#cursor_fake .handle'),
                    position_absolute = $document.mx - handle.data('dx') + handle.width() / 2 - track.offset().left,
                    position = position_absolute / track.width() * scope.video.duration;

                // Limit position to the length of the video
                if (position > scope.video.duration) {
                    position = scope.video.duration;
                }
                if (position < 0) {
                    position = 0;
                }

                handle.data('position', position);
                cursor.css('left', (position * 100 / scope.video.duration) + '%');
            };

            /**
             * The mousedown event handler to initiate the dragging of the
             * timeline handle.
             * Adds a listener on mousemove (movePlayHead)
             *
             * @param {Event} event the mousedown events that inits the mousemove actions.
             */
            scope.dragPlayhead = function (event) {
                event.preventDefault();
                scope.canMove = true;

                var cursor = element.find('#cursor'),
                    handle = element.find('#cursor .handle'),
                    target = $(event.target);

                // true if we clicked on the split button > so do split
                scope.doSplitSegment = target.hasClass('split');

                // We are currently playing - use fake handle
                if (scope.player.adapter.getStatus && scope.player.adapter.getStatus() === PlayerAdapter.STATUS.PLAYING) {

                    var cursorFake = element.find('#cursor_fake'),
                        handle = element.find('#cursor_fake .handle');

                    cursorFake.css('left', cursor.css('left'));
                    cursorFake.show();

                    // calculate initial value for "position" to allow splitting without dragging
                    var track = element.find('.timeline-track'),
                        position_absolute = handle.offset().left + handle.width() / 2 - track.offset().left,
                        position = position_absolute / track.width() * scope.video.duration;

                    handle.data('dx', $document.mx - handle.offset().left);
                    handle.data('dy', $document.my - handle.offset().top);
                    handle.data('position', position);
                    handle.addClass('active');

                    // Register global mouse move callback - Fake Playhead movement
                    $document.mousemove(scope.moveFakePlayHead);
                } else {

                    var handle = element.find('#cursor .handle');

                    handle.data('dx', $document.mx - handle.offset().left);
                    handle.data('dy', $document.my - handle.offset().top);
                    handle.addClass('active');

                    // Register global mouse move callback - Normal Playhead movement
                    $document.mousemove(scope.movePlayHead);
                }
            };

            /**
             * Sets the zoomFieldOffset corresponding to the position of the zoom field
             * in the field-of-vision as the mouse drag event unfolds.
             *
             * @param {Event} event the mousemove event details.
             */
            scope.moveTimeline = function (event) {
                event.preventDefault();
                if (!scope.canMoveTimeline) { return; }

                var track = element.find('.field-of-vision'),
                    shuttle = element.find('.field-of-vision .field'),
                    nx = $document.mx - shuttle.data('dx');

                if (nx <= 0) nx = 0;
                if (nx >= shuttle.data('end')) nx = shuttle.data('end');

                var percentage = nx / shuttle.data('track_width') * 100;

                shuttle.css('left', percentage +'%');
                scope.zoomFieldOffset = (scope.video.duration * percentage) / 100;
                scope.position = (scope.zoomFieldOffset * scope.video.duration) / (scope.video.duration - scope.zoomValue);

                if (isNaN(scope.position) || (scope.position < 0)) scope.position = 0;
                if (scope.position > scope.video.duration) scope.position = scope.video.duration;

                scope.from = scope.zoomFieldOffset;
                scope.to = scope.zoomFieldOffset + scope.zoomValue;
                shuttle.find(':first-child').html( scope.formatMilliseconds(scope.from) );
                shuttle.find(':last-child').html( scope.formatMilliseconds(scope.to) );
                scope.setWrapperClasses();
            };

            /**
             * The mousedown event handler to initiate the dragging of the
             * zoom timeline field.
             * Adds a listener on mousemove (moveTimeline)
             *
             * @param {Event} event the mousedown events that inits the mousemove actions.
             */
            scope.dragTimeline = function (event) {
                event.preventDefault();

                scope.canMoveTimeline = true;

                var track = element.find('.field-of-vision'),
                    shuttle = element.find('.field-of-vision .field');

                shuttle.data('dx', $document.mx - shuttle.offset().left);
                shuttle.data('dy', $document.my - shuttle.offset().top);
                shuttle.data('track_width', track.width());
                shuttle.data('shuttle_width', shuttle.width());
                shuttle.data('end', track.width() - shuttle.width());
                shuttle.data('active', true);

                // Register global mouse move callback
                $document.mousemove(scope.moveTimeline);
            };

            /**
             * Updates the segment start position indicator according to the mouse movement.
             *
             * @param {Event} event the mousemove event details.
             */
            scope.moveSegment= function (event) {
                event.preventDefault();
                if (!scope.movingSegment) { return; }

                var nx = $document.mx - scope.movingSegment.data('dx') - scope.movingSegment.data('px');

                if (nx <= scope.movingSegment.data('track_left')) nx = scope.movingSegment.data('track_left');
                if (nx >= scope.movingSegment.data('end')) nx = scope.movingSegment.data('end');

                scope.movingSegment.css({
                    left: nx,
                    'background-position': (-$document.mx + 37) + 'px'
                });
            };

            /**
             * The mousedown event handler for the segment start handle of a segment.
             * Adds a listener on mousemove (moveSegment)
             *
             * @param {Event} event the mousedown events that inits the mousemove actions.
             * @param {Object} segment describes the values of the current segment
             */
            scope.dragSegement = function (event, segment) {
              event.preventDefault();
              event.stopImmediatePropagation();

              scope.movingSegment = true;

              var handle = $(event.currentTarget),
                  track = element.find('.segments').parent();

              handle.data('dx', ($document.mx || event.pageX) - handle.offset().left);
              handle.data('dy', ($document.my || event.pageY) - handle.offset().top);
              handle.data('px', handle.parent().offset().left);
              handle.data('py', handle.parent().offset().top);
              handle.data('track_left', (handle.data('px') *-1) + track.offset().left - 4);
              handle.data('track_width', track.width());
              handle.data('shuttle_width', handle.width());
              handle.data('end', track.width() + track.offset().left - handle.parent().offset().left);
              handle.data('segment', segment);
              handle.addClass('active');
              handle.css('background-size', track.width() + 'px ' + handle.height() + 'px');

              scope.movingSegment = handle;

              // Register global mouse move callback
              $document.mousemove(scope.moveSegment);
            };

            /**
             * Sets the position marker to the start of the given segment.
             *
             * @param {Event} event details
             * @param {Object} segment Segment object
             */
            scope.skipToSegment = function (event, segment) {
                event.preventDefault();

                if (!segment.selected) {
                    scope.player.adapter.setCurrentTime(segment.start / 1000);
                    scope.position = segment.start;
                    scope.positionStyle = (scope.position * 100 / scope.video.duration) + '%';
                    scope.selectSegment(segment);
                }
            };

            /**
             * Marks the given segment as selected.
             *
             * @param {Object} segment Segment object
             */
            scope.selectSegment = function (segment) {
                angular.forEach(scope.video.segments, function (segment) {
                    segment.selected = false;
                });
                segment.selected = true;
                scope.setWrapperClasses();
            };

            /**
             * Toggles the preview mode. When preview mode is enabled, deactivated segments are skipped while playing.
             */
            scope.togglePreviewMode = function() {
                scope.previewMode = !scope.previewMode;
            };

            /**
             * Remove listeners and timer associated with this directive
             */
            scope.$on('$destroy', function () {
                $document.unbind('mouseup');
                $document.unbind('mousemove');

                // cancel timer for the small cut button below timeline handle
                if (scope.timer) $timeout.cancel( scope.timer );
            });


            scope.setWrapperClasses();
        }
    };
}]);

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
.directive('adminNgSegments', ['PlayerAdapter', '$document', 'VideoService', '$timeout',
function (PlayerAdapter, $document, VideoService, $timeout) {
    return {
        templateUrl: 'shared/partials/segments.html',
        priority: 0,
        scope: {
            player: '=',
            video: '='
        },
        link: function (scope, element) {

            /**
            * Formats time stamps to HH:MM:SS.sss
            *
            * @param {Number} ms is the time in milliseconds,
            * @param {Boolean} showMilliseconds should the milliseconds be displayed
            * @return {String} Formatted time string
           */
            scope.formatMilliseconds = function (ms, showMilliseconds) {

               if (isNaN(ms)) {
                   return '';
               }

               var date = new Date(ms),
                   pad = function (number, padding) {
                       return (new Array(padding + 1).join('0') + number)
                           .slice(-padding);
                   };

               if (typeof showMilliseconds === 'undefined') {
                   showMilliseconds = true;
               }

               return pad(date.getUTCHours(), 2) + ':' +
                   pad(date.getUTCMinutes(), 2) + ':' +
                   pad(date.getUTCSeconds(), 2) +
                   (showMilliseconds ? '.' + pad(date.getUTCMilliseconds(), 3) : '');
            };

            /**
             * Converts a string with a human readable time to ms
             *
             * @param {type} time in the format HH:MM:SS.sss
             * @returns {Number} time in ms
             */
            scope.parseTime = function (time) {
              if ( time !== undefined && time.length === 12) {
                var hours = parseInt(time.substring(0,2)),
                    minutes = parseInt(time.substring(3,5)),
                    seconds = parseInt(time.substring(6,8)),
                    millis = parseInt(time.substring(9));

                return millis + (seconds * 1000) + (minutes * 60000) + (hours * 3600000);
              }

            };

            /**
             * Returns an object that describes the css classes for a given segment.
             *
             * @param {Object} segment object
             * @return {Object} object with {class}: {boolean} values for CSS classes.
             */
            scope.getSegmentClass = function (segment) {
                return { deleted: segment.deleted, selected: segment.selected};
            };

            /**
             * Removes the given segment.
             *
             * The previous or, failing that, the next segment will take up
             * the space of the given segment.
             *
             * @param {Event} event that triggered the merge action
             * @param {Object} segment Segment object
             */
            scope.mergeSegment = function (event, segment) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (!scope.isRemovalAllowed(segment)) {
                    return;
                }

                var index = scope.video.segments.indexOf(segment);

                if (scope.video.segments[index - 1]) {
                    scope.video.segments[index - 1].end = segment.end;
                    scope.video.segments.splice(index, 1);
                } else if (scope.video.segments[index + 1]) {
                    scope.video.segments[index + 1].start = segment.start;
                    scope.video.segments.splice(index, 1);
                }
              scope.$root.$broadcast("segmentTimesUpdated");
            };

            /**
             * Toggle the deleted flag for a segment. Indicating if it should be used or not.
             *
             * @param {Event} event for checkbox link - stop the propogation
             * @param {Object} segment object on which the deleted variable will change
             */
            scope.toggleSegment = function (event, segment) {
                if (event) {
                    event.preventDefault();
                    event.stopPropagation();
                }

                if (angular.isUndefined(segment) || scope.video.segments.indexOf(segment) === -1) {
                    return;
                }

                if (scope.isRemovalAllowed(segment)) {
                    segment.deleted = !segment.deleted;
                }
            };

            /**
             * Sets the position marker to the start of the given segment.
             *
             * @param {Event} event details
             * @param {Object} segment Segment object
             */
            scope.skipToSegment = function (event, segment) {
                event.preventDefault();

                if (!segment.selected) {
                    scope.player.adapter.setCurrentTime(segment.start / 1000);
                }
            };

            scope.isRemovalAllowed = function(segment) {
                if (!segment) {
                    return false;
                }

                return (segment.deleted || scope.video.segments
                                               .filter(function(seg) {
                                                   return !seg.deleted;
                                               }).length > 1);
            };

            /**
             * Sets / Updates the human readable start and end times of the segments.
             */
            scope.setHumanReadableTimes = function () {
              angular.forEach(scope.video.segments, function(segment, key) {
                segment.startTime = scope.formatMilliseconds(segment.start);
                segment.endTime = scope.formatMilliseconds(segment.end);
              });
            };

            /*
             * Make sure that times are updates if needed.
             */
            scope.$root.$on("segmentTimesUpdated", function () {
              scope.setHumanReadableTimes();
              scope.$parent.setChanges(scope.segmentsChanged());
            });

            scope.segmentsChanged = function() {
                if (scope.video.segments.length !== scope.originalSegments.length) return true;
                return !scope.originalSegments.every(function(curr, idx) {
                  return curr.start === scope.video.segments[idx].start
                      && curr.end === scope.video.segments[idx].end
                      && curr.selected === scope.video.segments[idx].selected;
                });
            };

            /**
             * Checks if a time is within the valid boundaries
             * @param {type} time time to check
             * @returns {Boolean} true if time is > 0 and <video duration
             */
            scope.timeValid = function (time) {
              if (time >= 0 && time <= scope.video.duration) {
                return true;
              } else {
                return false;
              }
            };

            /**
             * Set a new Start time of a segment, other segments are changed or deleted accordingly
             * @param {type} segment that should change
             */

            scope.updateStartTime = function (segment) {
              var newTime = scope.parseTime(segment.startTime);
              if (newTime && newTime !== segment.start) {
                if (newTime > segment.end || ! scope.timeValid(newTime)) {
                  segment.startTime = scope.formatMilliseconds(segment.start);
                } else {
                  var previousSegment = scope.getPreviousSegment(segment);
                  var allow = scope.isRemovalAllowed(previousSegment);
                  segment.start = newTime;
                  while (previousSegment && allow && previousSegment.start > newTime) {
                    scope.removeSegment(previousSegment);
                    previousSegment = scope.getPreviousSegment(segment);
                    allow = scope.isRemovalAllowed(previousSegment);
                  }
                  if (!allow && previousSegment) {
                    if (previousSegment.start > newTime) {
                        segment.start = previousSegment.end;
                    }
                    else {
                        var endTime = Math.max(newTime, previousSegment.start + 1);
                        segment.start = previousSegment.end = endTime;
                    }
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                  else if (previousSegment) {
                    previousSegment.end = newTime;
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                }
              }
            };

            /**
             * Sets a new end time of a segment, other segments are changed or deleted accordingly
             * @param {type} segment that should change
             */
            scope.updateEndTime = function (segment) {
              var newTime = scope.parseTime(segment.endTime);
              if (newTime && newTime !== segment.end) {
                if (newTime < segment.start || ! scope.timeValid(newTime)) {
                  segment.endTime = scope.formatMilliseconds(segment.end);
                } else {
                  var nextSegment = scope.getNextSegment(segment);
                  var allow = scope.isRemovalAllowed(nextSegment);
                  segment.end = newTime;
                  while (nextSegment && allow && nextSegment.end < newTime) {
                    scope.removeSegment(nextSegment);
                    nextSegment = scope.getNextSegment(segment);
                    allow = scope.isRemovalAllowed(nextSegment);
                  }
                  if (!allow && nextSegment) {
                    if (nextSegment.end < newTime) {
                      segment.end = nextSegment.start;
                    }
                    else {
                      var startTime = Math.min(newTime, nextSegment.end - 1);
                      segment.end = nextSegment.start = startTime;
                    }
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                  else if (nextSegment) {
                    nextSegment.start = newTime;
                    scope.$root.$broadcast("segmentTimesUpdated");
                  }
                }
              }
            };

            /**
             * Deletes a segment from the segment list. Times of other segments are not changed!
             * @param {type} segment that should be deleted
             */
            scope.removeSegment = function (segment) {
              if (segment) {
                var index = scope.video.segments.indexOf(segment);
                scope.video.segments.splice(index, 1);
              }
            };

            /**
             * Gets the segment previous to the provided segment.
             * @param {type} currentSegment the reference segment
             * @returns {unresolved} the previous segment or "undefinded" if current segment is the first
             */
            scope.getPreviousSegment = function (currentSegment) {
              var index = scope.video.segments.indexOf(currentSegment);
              if (index > 0)
                return scope.video.segments[index - 1];
            };

            /**
             * Gets the next segment to the provided segment
             * @param {type} currentSegment the reference segment
             * @returns {unresolved} the next segment or "undefined" if the current segment is the last.
             */
            scope.getNextSegment = function (currentSegment) {
              var index = scope.video.segments.indexOf(currentSegment);
              if (index < (scope.video.segments.length - 1))
                return scope.video.segments[index + 1];
            };

            scope.video.$promise.then(function () {
              // Take a snapshot of the original segments to track if we have changes
              scope.originalSegments = angular.copy(scope.video.segments);

              scope.$root.$broadcast("segmentTimesUpdated");
            });
        }
    };
}]);

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
.directive('videoPlayer', ['PlayerAdapter', 'PlayerAdapterRepository', 'VideoService', '$timeout', 'HotkeysService',
    function (PlayerAdapter, PlayerAdapterRepository, VideoService, $timeout, HotkeysService) {

    return {
        restrict: 'A',
        priority: 10,
        templateUrl: 'shared/partials/player.html',
        scope: {
            player   : '=',
            video    : '=',
            controls : '@',
            subControls: '@'
        },
        link: function (scope, element, attrs) {
            function loadPlayerAdapter(element) {
                scope.player.adapter = PlayerAdapterRepository.
                    createNewAdapter(
                        attrs.adapter.toUpperCase(),
                        element
                    );

                scope.playing = false;
                scope.positionStart = false;
                scope.positionEnd = false;
                scope.muted = false;
                scope.volume = 100;
                scope.playButtonTooltip = 'VIDEO_TOOL.PLAYER.PLAY';
                scope.time = {
                    hours: 0,
                    minutes: 0,
                    seconds: 0,
                    milliseconds: 0
                };

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.PAUSE, function () {
                    scope.$apply(function () {
                        scope.playing = false;
                        scope.playButtonTooltip = 'VIDEO_TOOL.PLAYER.PLAY';
                    });
                });

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.PLAY, function () {
                    scope.$apply(function () {
                        scope.playing = true;
                        scope.playButtonTooltip = 'VIDEO_TOOL.PLAYER.PAUSE';
                    });
                });

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.TIMEUPDATE, function () {
                    // Expose the time to the view
                    scope.time = scope.player.adapter.getCurrentTimeObject();

                    scope.$apply();
                });

                scope.player.adapter.addListener(PlayerAdapter.EVENTS.VOLUMECHANGE, function () {
                    scope.muted = scope.player.adapter.muted();
                    scope.volume = scope.player.adapter.volume();
                });

                scope.$watch(function (){
                  if (controlsVisible !== scope.controls) {
                    controlsVisible = scope.controls;
                    var videoObj = angular.element("#player")[0];
                    if (videoObj) {
                      if (scope.controls == "true") {
                        videoObj.setAttribute("controls", "controls");
                      } else {
                        if (videoObj.hasAttribute("controls")) {
                          videoObj.removeAttribute("controls");
                        }
                      }
                    }
                  }
                });
            }

            var controlsVisible;

            // Check if the player element is loaded,
            function checkPlayerElement(tries, callback) {
                if (tries > 0) {
                    var element = angular.element('#' + attrs.playerRef)[0];
                    if (element) {
                        callback(element);
                    } else {
                        // Wait 100ms before to retry
                        scope.checkTimeout = $timeout(function() {
                            checkPlayerElement(tries - 1, callback);
                        }, 100);
                    }
                }
            }

            function getTimeInSeconds(time) {
                var millis = time.milliseconds;
                millis += time.seconds * 1000;
                millis += time.minutes * 60 * 1000;
                millis += time.hours * 60 * 60 * 1000;
                return millis / 1000;
            }

            scope.$on('$destroy', function () {
                $timeout.cancel(scope.checkTimeout);
                if (angular.isDefined(scope.player.adapter)) {
                  scope.player.adapter.pause();
                }
            });

            scope.previousFrame = function () {
                scope.player.adapter.previousFrame();
            };

            scope.nextFrame = function () {
                scope.player.adapter.nextFrame();
            };

            scope.previousSegment = function () {
                var segment = VideoService.getCurrentSegment(scope.player, scope.video),
                    index = scope.video.segments.indexOf(segment);
                if (scope.video.segments[index-1]) {
                    segment = scope.video.segments[index-1];
                }
                scope.player.adapter.setCurrentTime(segment.start / 1000 );
            };

            scope.nextSegment = function () {
                var segment = VideoService.getCurrentSegment(scope.player, scope.video);
                scope.player.adapter.setCurrentTime(segment.end / 1000 );
            };

            scope.play = function () {
                if (scope.playing) {
                    scope.player.adapter.pause();
                }
                else {
                    scope.player.adapter.play();
                }
            };

            scope.pause = function () {
                scope.player.adapter.pause();
            }

            scope.changeTime = function (time) {
            	scope.player.adapter.setCurrentTime(getTimeInSeconds(time));
            };

            scope.stepBackward = function () {
              var newTime = scope.player.adapter.getCurrentTime() - 10;
              console.log("setting player to " + newTime);
            	scope.player.adapter.setCurrentTime(newTime);
            };

            scope.stepForward = function () {
              var newTime = scope.player.adapter.getCurrentTime() + 10;
              console.log("setting player to " + newTime);
            	scope.player.adapter.setCurrentTime(newTime);
            };

            scope.toggleMute = function () {
              scope.player.adapter.muted(! scope.player.adapter.muted());
            }

            scope.setVolume = function () {
              scope.player.adapter.volume(scope.volume);
            }

            scope.volumeUp = function () {
              if (scope.volume + 10 <= 100) {
                scope.volume = scope.volume + 10;
              } else {
                scope.volume = 100;
              }
              scope.setVolume();
            }

            scope.volumeDown = function () {
              if (scope.volume - 10 >= 0) {
                scope.volume = scope.volume - 10;
              } else {
                scope.volume = 0;
              }
              scope.setVolume();
            }

            scope.subControls = angular.isDefined(scope.subControls) ? scope.subControls : 'true';

            // Check for the player (10 times) before to load the adapter
            checkPlayerElement(10, loadPlayerAdapter);

            HotkeysService.activateHotkey(scope, "player.play_pause",
              "play / pause video", function(event) {
                  event.preventDefault();
                  scope.play();
            });

            HotkeysService.activateHotkey(scope, "player.previous_frame",
              "Previous Frame", function(event) {
                  event.preventDefault();
                  scope.pause();
                  scope.previousFrame();
            });

            HotkeysService.activateHotkey(scope, "player.next_frame",
              "Next Frame", function(event) {
                  event.preventDefault();
                  scope.pause();
                  scope.nextFrame();
            });

            HotkeysService.activateHotkey(scope, "player.step_backward",
              "Jump 10s back", function(event) {
                  event.preventDefault();
                  scope.stepBackward();
            });

            HotkeysService.activateHotkey(scope, "player.step_forward",
              "Jump 10s forward", function(event) {
                  event.preventDefault();
                  scope.stepForward();
            });

            HotkeysService.activateHotkey(scope, "player.previous_segment",
              "Previous segment", function(event) {
                  event.preventDefault();
                  scope.previousSegment();
            });

            HotkeysService.activateHotkey(scope, "player.next_segment",
              "Next Segment", function(event) {
                  event.preventDefault();
                  scope.nextSegment();
            });

            HotkeysService.activateHotkey(scope, "player.volume_up",
              "Volume up", function(event) {
                  event.preventDefault();
                  scope.volumeUp();
            });

            HotkeysService.activateHotkey(scope, "player.volume_down",
              "Volume down", function(event) {
                  event.preventDefault();
                  scope.volumeDown();
            });

            HotkeysService.activateHotkey(scope, "player.mute",
              "Mute / unmute", function(event) {
                  event.preventDefault();
                  scope.toggleMute();
            });
        }
    };
}]);

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
 * @name adminNg.directives.adminNgHrefRole
 * @description
 * Overrides the 'href' attribute of an element depending on the user's roles.
 *
 * @example
 * <a admin-ng-href-role="{'STUDENT': '/student', 'TEACHER': '/teacher'}" href="/general" />
 */
angular.module('adminNg.directives')
.directive('adminNgHrefRole', ['AuthService', function (AuthService) {
    return {
        scope: {
            rolesHref: '@adminNgHrefRole'
        },
        link: function ($scope, element) {
            var href;
            angular.forEach($scope.$eval($scope.rolesHref), function(value, key) {
                if (AuthService.userIsAuthorizedAs(key) && angular.isUndefined(href)) {
                    href = value;
                }
            });

            if (angular.isDefined(href)) {
                element.attr('href', href);
            }
        }
    };
}]);

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
.directive('adminNgTableFilter', ['Storage', 'FilterProfiles', 'Language', 'underscore', '$translate', '$timeout', function (Storage, FilterProfiles, Language, _, $translate, $timeout) {
    return {
        templateUrl: 'shared/partials/tableFilters.html',
        replace: true,
        scope: {
            filters:   '=',
            namespace: '='
        },
        link: function (scope) {
            scope.formatDateRange = Language.formatDateRange;

            scope.getOptionLabel = function (filter) {
                var optionLabel;

                angular.forEach(filter.options, function (id, label) {
                    if (id === filter.value) {
                        optionLabel = label;
                    }
                });

                return optionLabel;
            };

            scope.displayFilterSelector = function() {
                scope.showFilterSelector = true;
                $timeout(function(){
                    angular.element('.main-filter').trigger('chosen:open');
                })
            }

            scope.initializeMap = function() {
                for (var key in scope.filters.filters) {
                    scope.filters.map[key] = {
                        options: {},
                        type: scope.filters.filters[key].type,
                        label: scope.filters.filters[key].label,
                        translatable: scope.filters.filters[key].translatable
                    };
                    var options = scope.filters.filters[key].options;
                    angular.forEach(options, function(option) {
                         scope.filters.map[key].options[option.value] = option.label;
                    });
                }
            }

            scope.restoreFilters = function () {
                angular.forEach(scope.filters.filters, function (filter, name) {
                    filter.value = Storage.get('filter', scope.namespace)[name];

                    if (scope.filters.map[name]) {
                        scope.filters.map[name].value = filter.value;
                    }
                });
                scope.textFilter = Storage.get('filter', scope.namespace).textFilter;
            };

            scope.filters.$promise.then(function () {
                scope.filters.map = {};
                if (Object.keys(scope.filters.map).length === 0) {
                    scope.initializeMap();
                }

                scope.restoreFilters();
            });

            scope.removeFilters = function () {
                angular.forEach(scope.filters.map, function (filter) {
                    delete filter.value;
                });

                scope.selectedFilter = null;
                scope.showFilterSelector = false;
                Storage.remove('filter');
                angular.element('.main-filter').val('').trigger('chosen:updated');
            };

            scope.removeFilter = function (name, filter) {
                delete filter.value;
                Storage.remove('filter', scope.namespace, name);
            };

            scope.selectFilterTextValue = _.debounce(function (filterName, filterValue) {
                scope.showFilterSelector = false;
                scope.selectedFilter = null;
                scope.addFilterToStorage('filter', scope.namespace, filterName, filterValue);
            }, 250);

            scope.getFilterName = function(){
              if (angular.isDefined(scope.selectedFilter) && angular.isDefined(scope.selectedFilter.label)) {
                for(var i in scope.filters.filters) {
                  if (angular.equals(scope.filters.filters[i].label, scope.selectedFilter.label)) {
                    return i;
                  }
                }
              }
            };

            scope.selectFilterSelectValue = function (filter)  {
                var filterName = scope.getFilterName();
                scope.showFilterSelector = false;
                scope.selectedFilter = null;
                scope.filters.map[filterName].value = filter.value;
                scope.addFilterToStorage('filter', scope.namespace, filterName , filter.value);
            };

            scope.toggleFilterSettings = function () {
                scope.mode = scope.mode ? 0:1;
            };

            scope.selectFilterPeriodValue = function (filter) {
                var filterName = scope.getFilterName();
                // Merge from-to values of period filter)
                if (!filter.period.to || !filter.period.from) {
                    scope.openSecondFilter(filter);
                    return;
                }
                if (filter.period.to && filter.period.from) {
                    var from = new Date(new Date(filter.period.from).setHours(0, 0, 0, 0));
                    var to = new Date(new Date(filter.period.to).setHours(23, 59, 59, 999));
                    filter.value = from.toISOString() + '/' + to.toISOString();
                }

                if (filter.value) {
                    scope.showFilterSelector = false;
                    scope.selectedFilter = null;

                    if (!scope.filters.map[filterName]) {
                      scope.filters.map[filterName] = {};
                    }
                    scope.filters.map[filterName].value = filter.value;
                    scope.addFilterToStorage('filter', scope.namespace, filterName, filter.value);
                }
            };

            scope.addFilterToStorage = function(type, namespace, filterName, filterValue) {
                Storage.put(type, namespace, filterName, filterValue);
                angular.element('.main-filter').val('').trigger('chosen:updated');
            }

            // Restore filter profiles
            scope.profiles = FilterProfiles.get(scope.namespace);

            scope.validateProfileName = function () {
                var profileNames = FilterProfiles.get(scope.namespace).map(function (profile) {
                    return profile.name;
                });
                scope.profileForm.name.$setValidity('uniqueness',
                        profileNames.indexOf(scope.profile.name) <= -1);
            };

            scope.saveProfile = function () {
                if (angular.isDefined(scope.currentlyEditing)) {
                    scope.profiles[scope.currentlyEditing] = scope.profile;
                } else {
                    scope.profile.filter = angular.copy(Storage.get('filter', scope.namespace));
                    scope.activeProfile = scope.profiles.push(scope.profile) - 1;
                }

                FilterProfiles.set(scope.namespace, scope.profiles);
                scope.profile = {};
                scope.mode = 0;
                delete scope.currentlyEditing;
            };

            scope.cancelProfileEditing = function () {
                scope.profiles = FilterProfiles.get(scope.namespace);
                scope.profile = {};
                scope.mode = 1;
                delete scope.currentlyEditing;
            };

            scope.closeProfile = function () {
                scope.mode = 0;
                scope.profile = {};
                delete scope.currentlyEditing;
            };

            scope.removeFilterProfile = function (index) {
                scope.profiles.splice(index, 1);
                FilterProfiles.set(scope.namespace, scope.profiles);
            };

            scope.editFilterProfile = function (index) {
                scope.mode = 2;
                scope.profile = scope.profiles[index];
                scope.currentlyEditing = index;
            };

            scope.loadFilterProfile = function (index) {
                if (FilterProfiles.get(scope.namespace)[index]) {
                  var newFilters = [];
                  var filtersFromProfile = FilterProfiles.get(scope.namespace)[index].filter;
                  angular.forEach(filtersFromProfile, function (fvalue, fkey) {
                    newFilters.push({namespace: scope.namespace, key: fkey, value: fvalue});
                  });
                  Storage.replace(newFilters, 'filter');
                }
                scope.mode = 0;
                scope.activeProfile = index;
            };

            scope.onChangeSelectMainFilter = function(selectedFilter) {
                scope.filter = selectedFilter;
                scope.openSecondFilter(selectedFilter);
            }

            scope.openSecondFilter = function (filter) {

                switch (filter.type) {
                    case 'period':
                        if(!filter.hasOwnProperty('period')){
                            angular.element('.small-search.start-date').datepicker('show');
                        }else if(!filter.period.hasOwnProperty('to')){
                            angular.element('.small-search.end-date').datepicker('show');
                        }
                        break;
                    default:
                        $timeout(function(){
                            angular.element('.second-filter').trigger('chosen:open');
                        })
                        break;
                }
            }

            // Deregister change handler
            scope.$on('$destroy', function () {
                scope.deregisterChange();
            });

            // React on filter changes
            scope.deregisterChange = Storage.scope.$on('change', function (event, type) {
                if (type === 'filter') {
                    scope.restoreFilters();
                }
            });

        }
    };
}]);

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
.directive('adminNgNotifications', ['Notifications', function (Notifications) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/notifications.html',
        replace: true,
        scope: {
            context: '@',
        },
        link: function (scope) {
            var updateNotifications = function (context) {
                if (angular.isUndefined(scope.context)) {
                    scope.context = 'global';
                }

                if (scope.context === context ) {
                    scope.notifications = Notifications.get(scope.context);
                }
            };

            scope.notifications = Notifications.get(scope.context);

            scope.deregisterAdd = Notifications.$on('added', function (event, context) {
                updateNotifications(context);
            });

            scope.deregisterChanged = Notifications.$on('changed', function (event, context) {
                updateNotifications(context);
            });

            scope.deregisterDelete = Notifications.$on('deleted', function (event, context) {
                updateNotifications(context);
            });

            scope.$on('$destroy', function () {
                scope.deregisterAdd();
                scope.deregisterChanged();
                scope.deregisterDelete();
                Notifications.$destroy();
            });
        }
    };
}]);

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
.directive('adminNgNotification', ['Notifications', '$timeout',
function (Notifications, $timeout) {
    return {
        restrict: 'A',
        templateUrl: 'shared/partials/notification.html',
        replace: true,
        scope: {
            id         : '@',
            type       : '@',
            message    : '@',
            parameters : '@',
            show       : '=',
            hidden     : '=',
            duration   : '@'
        },
        link: function (scope, element) {

        	if (angular.isNumber(parseInt(scope.duration)) && parseInt(scope.duration) !== -1) {
                // we fade out the notification if it is not -1 -> therefore -1 means 'stay forever'
                var fadeOutTimer = $timeout(function () {
                    element.fadeOut(function () {
                        Notifications.remove(scope.id, scope.$parent.context);
                    });
                }, scope.duration);

                 scope.$on('$destroy', function () {
                     $timeout.cancel(fadeOutTimer);
                 });
            }
       }
    };
}]);

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
.constant('EVENT_TAB_CHANGE', 'tab_change')
.directive('adminNgWizard', ['EVENT_TAB_CHANGE', function (EVENT_TAB_CHANGE) {
    function createWizard($scope) {
        var currentState = $scope.states[0], step, lookupIndex, lookupState, toTab,
            getCurrentState, getCurrentStateController, getPreviousState, getNextState,
            getCurrentStateName, getCurrentStateIndex, getStateControllerByName, save, isReachable,
            hasPrevious, isCompleted, isLast, getFinalButtonTranslation, sharedData, result;

        // The "sharedData" attribute allows steps to preserve localized ng-repeat data
        // at the wizard scope for cases when then the "save" callback is not sufficient.
        // Specifically, to preserve UI visible "file" selections created within ng-repeat loops.
        // Unlike all other html input fields, the "file" type html input is forbidden to be
        // manually/programatically reset via javascript.
        // This attribute is used by the asset file upload directive.
        $scope.sharedData = {};

        angular.forEach($scope.states, function (state) {
            if (!angular.isDefined(state.stateController.visible)) {
                state.stateController.visible = true;
            }
        });

        // retrieve shared data from the state controllers
        angular.forEach($scope.states, function (state) {
            if (angular.isDefined(state.stateController.addSharedDataPromise)) {
               state.stateController.addSharedDataPromise();
            }
        });

        /* Saves the state of the current view */
        save = function (id, callback) {
            getCurrentState().stateController.save(this, callback);
        };

        step = $scope.states[0].stateController;

        /* Returns the current state object, which allows for specific
         * queries, e.g. the metadata array. */
        getCurrentState = function () {
            return currentState;
        };
        getCurrentStateController = function () {
            return currentState.stateController;
        };
        getPreviousState = function (offset) {
            if (!offset) {
                offset = 1;
            }
            var prevState = $scope.states[getCurrentStateIndex() - offset];
            if (prevState.stateController.visible) {
                return prevState;
            } else {
                return getPreviousState(offset + 1);
            }
        };
        getNextState = function (offset) {
            if (!offset) {
                offset = 1;
            }
            var nextState = $scope.states[getCurrentStateIndex() + offset];
            if (nextState.stateController.visible) {
                return nextState;
            } else {
                return getNextState(offset + 1);
            }
        };
        getCurrentStateName = function () {
            return currentState.name;
        };
        getCurrentStateIndex = function () {
            return lookupIndex(currentState.name);
        };
        getStateControllerByName = function (name) {
            return $scope.states[lookupIndex(name)].stateController;
        };
        hasPrevious = function () {
            return lookupIndex(currentState.name) > 0;
        };
        lookupState = function (name) {
            var index = lookupIndex(name);
            return $scope.states[index];
        };
        lookupIndex = function (name) {
            try {
                for (var i = 0; i < $scope.states.length; i++) {
                    if ($scope.states[i].name === name) {
                        return i;
                    }
                }
            } catch (e) { }
        };

        /* Will switch to the tab denoted in the data-modal-tab attribute of
         * the anchor that was clicked.
         * Prerequisite: All previous steps of the wizard have been passed
         * successfully.
         */
        toTab = function ($event) {
            var targetStepName, targetState;
            targetStepName = $event.target.getAttribute('data-modal-tab');
            if (targetStepName === 'previous') {
                targetState = getPreviousState();
            } else if (targetStepName === 'next') {
                targetState = getNextState();
            } else {
                targetState = lookupState(targetStepName);
            }
            if (angular.isUndefined(targetState)) {
                return;
            }
            // Permission to navigate to a tab is only granted, if the previous tabs are all valid
            if (isReachable(targetState.name)) {
                $scope.$emit(EVENT_TAB_CHANGE, {
                    old: currentState,
                    current: targetState
                });

               // one-time directed call to the current state
               // to allow the state to perform exit cleanup
               if ($scope.wizard.step.onExitStep) {
                 $scope.wizard.step.onExitStep();
               }

                currentState = targetState;
                $scope.wizard.step = targetState.stateController;

                //FIXME: This should rather be a service I guess, so it won't be tied to modals.
                //Its hard to unit test like this also
                $scope.$parent.openTab(targetState.name);
                focus();
            }
        };

        isReachable = function (stateName) {
            var index, currentState;
            index = lookupIndex(stateName) - 1;
            while (index >= 0) {
                currentState = $scope.states[index];
                if (!currentState.stateController.isValid()) {
                    return false;
                }
                index--;
            }
            return true;
        };

        isCompleted = function (stateName) {
            return lookupState(stateName).stateController.isValid();
        };

        isLast = function () {
            return getCurrentStateIndex() === $scope.states.length - 1;
        };

        getFinalButtonTranslation = function () {
            if (angular.isDefined($scope.finalButtonTranslation)) {
                return $scope.finalButtonTranslation;
            } else {
                return 'WIZARD.CREATE';
            }
        };

        focus = function () {
          //make sure the tab index starts again with 1
          angular.forEach(angular.element.find("[focushere]"), function (element) {
            angular.element(element).trigger('chosen:activate').focus();
          })
          var tabindexOne = angular.element($("[tabindex=1]"));
          tabindexOne.focus();
        };

        result = {
            states: $scope.states,
            step: step,
            getCurrentState: getCurrentState,
            getCurrentStateController: getCurrentStateController,
            getCurrentStateName: getCurrentStateName,
            getStateControllerByName: getStateControllerByName,
            save: save,
            sharedData: sharedData,
            toTab: toTab,
            isReachable: isReachable,
            isCompleted: isCompleted,
            isLast: isLast,
            getFinalButtonTranslation: getFinalButtonTranslation,
            hasPrevious: hasPrevious,
            submit: $scope.submit
        };

        angular.forEach($scope.states, function (state) {
            if (!angular.isDefined(state.stateController.visible)) {
                state.stateController.visible = true;
            }
            state.stateController.wizard = result;
        });

        return result;
    }

    return {
        restrict: 'E',
        templateUrl: 'shared/partials/wizard.html',
        scope: {
            submit: '=',
            states: '=',
            name:   '@',
            action: '=',
            finalButtonTranslation: '@'
        },
        link: function (scope) {
            scope.isCurrentTab = function (tab) {
                return scope.wizard.getCurrentStateName() === tab;
            };
            /**
             * Check if the given value is empty or undefined
             */
            scope.isEmpty =  function (value) {
                return angular.isUndefined(value) || (angular.isString(value) && value.length === 0) ||
                        (angular.isObject(value) && JSON.stringify(value).length === 2);
            };
            scope.wizard = createWizard(scope);
            scope.deleted = true;

            scope.keyUp = function (event) {
              if (event.keyCode === 13 || event.keyCode === 32) {
                scope.wizard.toTab(event);
              }
            };

            scope.keyUpSubmit = function (event) {
              if (event.keyCode === 13 || event.keyCode === 32) {
                scope.submit();
              }
            };

        }
    };
}]);

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
 * @name adminNg.directives.withRole
 * @description
 * Remove the element if the current user does not have the given role.
 *
 * @example
 * <a with-role="ROLE_USER">New event</a>
 */
angular.module('adminNg.directives')
.directive('withRole', ['AuthService', function (AuthService) {
    return {
        priority: 1000,
        link: function ($scope, element, attr) {
            element.addClass('hide');

            AuthService.userIsAuthorizedAs(attr.withRole, function () {
                element.removeClass('hide');
            }, function () {
                element.remove();
            });
        }
    };
}]);

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
    .directive('fileUpload', ['$upload', function ($upload) {
        return {
            restrict: 'A',
            templateUrl: 'shared/partials/fileupload.html',
            require: '^ngModel',
            scope: {
                acceptableTypes: '@acceptableTypes',
                descriptionKey: '@descriptionKey',
                buttonKey: '@buttonKey',
                labelKey: '@labelKey',
                existing: '='
            },
            link: function (scope, attrs, elem, ngModel) {
                if (!scope.acceptableTypes) {
                    throw new Error('file-upload directive needs acceptableTypes attribute');
                }

                scope.file = scope.existing || {};
                scope.file.inprogress = false;
                scope.file.progress = {
                    loaded: 0,
                    total: 0,
                    value: 0
                };

                scope.remove = function () {
                    var model = ngModel;
                    scope.file = {
                        inprogress: false,
                        progress: {
                            loaded: 0,
                            total: 0,
                            value: 0
                        }
                    };
                    model.$setViewValue(undefined);
                };

                scope.upload = function (files) {
                    var progressHandler, successHandler, errorHandler, transformRequest, transformResponse, model;

                    progressHandler = function (evt) {

                        var loaded = evt.loaded * 100, total = evt.total, value = loaded / total;
                        scope.file.progress = {loaded: loaded, total: total, value: Math.round(value)};
                        scope.file.inprogress = true;
                    };

                    model = ngModel;
                    successHandler = function (data) {
                        scope.file = {
                            id: data.data.id,
                            name: data.config.file.name,
                            url: data.headers().location
                        };
                        scope.file.inprogress = false;
                        model.$setViewValue(scope.file);
                        model.$setValidity('file-upload', true);
                    };

                    errorHandler = function () {
                        scope.file = {
                            inprogress: false,
                            progress: {
                                loaded: 0,
                                total: 0,
                                value: 0
                            }
                        };

                        model.$setValidity('file-upload', false);
                    };

                    transformResponse = function (response) {
                        return { id: response.trim() };
                    };

                    transformRequest = function () {
                        var fd = new FormData();
                        fd.append('BODY', scope.files[0], scope.files[0].name);
                        return fd;
                    };

                    if (files !== undefined && files[0] !== undefined) {

                        scope.file = files[0];

                        $upload.http({
                            url: '/staticfiles',
                            headers: { 'Content-Type': undefined },
                            data: {},
                            file: scope.file,
                            transformRequest: transformRequest,
                            transformResponse: transformResponse
                        }).then(successHandler, errorHandler, progressHandler);
                    }
                };
            }
        };
    }]);

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

angular.module("adminNg.directives")
.directive("popOver", ["$timeout", function ($timeout) {
    return {
        restrict: "A",
        link: function (scope, element) {
            var callToHide;

            element.mouseenter(function(){
                var popover = element.next(".js-popover");
                // make element visible in DOM,
                // otherwise positioning will not work
                popover.removeClass("is-hidden").removeClass("popover-left").removeClass("popover-right");

                // uses jquery-ui's #position
                popover.position({
                    my: "left+20 top-10",
                    at: "right",
                    of: element
                });

                // add modifier
                if (popover.position().left < 0) {
                    popover.addClass("popover-left");
                } else {
                    popover.addClass("popover-right");
                }

                popover.mouseenter(function(){
                    if(callToHide){
                        // prevent hiding the popover bubble
                        // because user has entered it
                        $timeout.cancel(callToHide);
                    }
                });

                popover.on("mouseleave click", function(){
                    popover.addClass("is-hidden");
                });
            });

            element.mouseleave(function(){
                // don't hide immediately to allow user
                // to reach the popover bubble
                callToHide = $timeout(function(){
                    var popover = element.next(".js-popover");
                    popover.addClass("is-hidden");
                }, 200);
            });

            element.click(function(){
                // no-op
                return false;
            });
        }
    };
}]);

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
.directive('preSelectFrom', [ 'underscore',
function (_) {

    return {
        restrict: 'A',
        require: ['ngModel'],
        template: '',
        replace: false,
        link: function ($scope, $element, $attr, ngModel) {

            if(angular.isUndefined($attr.preSelectFrom)) {
                console.error('directive preSelectFrom requires a value');
            }

            var unregister = $scope.$watch($attr.preSelectFrom, function (options) {
                if (!_.isUndefined(options)) {

                    // fix angular resource objects
                    if (_.has(options, 'toJSON')) {
                        options = options.toJSON();
                    }

                    if (_.size(options) === 1 && _.size(ngModel) > 0) { // supports objects and arrays
                        ngModel[0].$setViewValue(options[_.keys(options)[0]], 'myevent');
                        unregister();
                    }
                }
            });
        }
    };
}]);

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
 * @name adminNg.modal.adminNgLoadingScroller
 * @description
 * Checks for the scoll position of a multi-select element and calls a method to load additional data if needed
 *
 * Usage: Set this directive in the select element you need to load data for
 */
angular.module('adminNg.directives')
.directive('loadingScroller', [function () {
    return {
        restrict: 'A',
        require: 'ngModel',
        link: function (scope, element, attrs, ctrl) {
            var raw = element[0];

            element.bind('scroll', function () {
                if (raw.scrollTop + raw.offsetHeight > raw.scrollHeight) {
                    scope.$apply(attrs.adminNgLoadingScroller);
                }
            });
        }
    };
}]);

/**
 * Copyright (C) 2013 Luegg
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/* Adapted from:
 * angularjs Scroll Glue
 * version 2.0.6
 * https://github.com/Luegg/angularjs-scroll-glue
 * An AngularJs directive that automatically scrolls to the bottom of an element on changes in it's scope.
*/

(function(angular, undefined){
    'use strict';

    function createActivationState($parse, attr, scope){
        function unboundState(initValue){
            var activated = initValue;
            return {
                getValue: function(){
                    return activated;
                },
                setValue: function(value){
                    activated = value;
                }
            };
        }

        return unboundState(true);
    }

    function createDirective(module, attrName, direction){
        module.directive(attrName, ['$parse', '$window', '$timeout', function($parse, $window, $timeout){
            return {
                priority: 1,
                restrict: 'A',
                link: function(scope, $el, attrs){
                    var el = $el[0],
                        scrollPos = 0,
                        activationState = createActivationState($parse, attrs[attrName], scope);


                    function scrollIfGlued() {
                        if(activationState.getValue() && !direction.isAttached(el, scrollPos)){
                            direction.scroll(el, scrollPos);
                        }
                    }

                    function onScroll() {
                        scrollPos = el.scrollTop;
                        activationState.setValue(direction.isAttached(el, scrollPos));
                    }

                    scope.$watch(scrollIfGlued);

                    $timeout(scrollIfGlued, 0, false);

                    $window.addEventListener('resize', scrollIfGlued, false);

                    $el.bind('scroll', onScroll);


                    // Remove listeners on directive destroy
                    $el.on('$destroy', function() {
                        $el.unbind('scroll', onScroll);
                    });

                    scope.$on('$destroy', function() {
                        $window.removeEventListener('resize',scrollIfGlued, false);
                    });
                }
            };
        }]);
    }

    var to = {
        isAttached: function(el, pos){
            return el.scrollTop == pos;
        },
        scroll: function(el, pos){
            el.scrollTop = pos;
        }
    };

    var module = angular.module('opencast.directives', []);

    createDirective(module, 'opencastScrollGlue', to);
}(angular));

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

// Asset upload html UI directive
// Used in new event and existing event asset upload
angular.module('adminNg.directives')
.directive('adminNgUploadAsset', ['$filter', function ($filter) {
    return {
        restrict: 'A',
        scope: {
          assetoptions: "=",
          assets: "=filebindcontainer",
          onexitscope: "="
        },
        templateUrl: 'shared/partials/uploadAsset.html',
        link: function(scope, elem, attrs) {
           scope.removeFile = function(elemId) {
               delete scope.assets[elemId];
               angular.element(document.getElementById(elemId)).val('');
           }
           scope.addFile = function(file) {
               scope.assets[elem.id] = file;
           }
           // Allows sorting list on traslated title/description/caption
           scope.translatedTitle = function(asset) {
               return $filter('translate')(asset.title);
           }
           //The "onexitscope"'s oldValue acts as the callback when the scope of the directive is exited.
           //The callback allow the parent scope to do work (i.e. make a summary map) based on
           //  activies performed in this directive.
           scope.$watch('onexitscope', function(newValue, oldValue) {
                if (oldValue) {
                    oldValue();
                }
           });
        }
    };
}]);

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
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying details of table records.
 */
angular.module('adminNg.services.modal')
.factory('ResourceModal', ['$location', '$compile', '$injector', 'Table', 'Modal', '$timeout',
    function ($location, $compile, $injector, Table, Modal, $timeout) {
    var ResourceModal = function () {
        var me = this,
            DEFAULT_REFRESH_DELAY = 5000;

        /**
         * Scheduler for the refresh of the fetch
         */
        this.refreshScheduler = {
            on: true,
            newSchedule: function () {
                me.refreshScheduler.cancel();
                me.refreshScheduler.nextTimeout = $timeout(me.loadSubNavData, DEFAULT_REFRESH_DELAY);
            },
            cancel: function () {
                if (me.refreshScheduler.nextTimeout) {
                    $timeout.cancel(me.refreshScheduler.nextTimeout);
                }
            }
        };

        /**
         * @ngdoc function
         * @name Modal.openTab
         * @methodOf adminNg.modal.Modal
         * @description
         * Opens the given or first tab and any sub tabs.
         *
         * The tab will be determined either by the Event object (when
         * called from a click event) or from the tabId parameter. All
         * bread crumbs will be deleted unless the `keepBreadcrumbs` flag
         * is true.
         *
         * Updates the URL with the `tab` search parameter.
         *
         * @param {string} tabId Tab ID to open.
         * @param {boolean} keepBreadcrumbs Deletes breadcrumbs if true.
         */
        this.openTab = function (tabId, keepBreadcrumbs) {
            // If no tab is defined, open the first tab by default.
            if (angular.isUndefined(tabId)) {
                tabId = Modal.modal.find('#modal-nav > a').first().data('modal-tab');
            }

            var params = $location.search();

            // Clean up breadcrumbs if appropriate
            if (!keepBreadcrumbs) {
                delete params.breadcrumbs;
                me.$scope.breadcrumbs = [];
            }

            Modal.modal.find('#breadcrumb').hide();
            Modal.modal.find('.modal-content').removeClass('active');
            Modal.modal.find('[data-modal-tab="' + tabId + '"]').addClass('active');
            Modal.modal.find('[data-modal-tab-content="' + tabId + '"]').addClass('active');

            me.$scope.tab = tabId;

            params.tab = tabId;
            $location.search(params);
        };

        /**
         * @ngdoc function
         * @name Modal.openSubTab
         * @methodOf adminNg.modal.Modal
         * @description
         * Switch tabs or open the first one.
         *
         * The tab will be determined either by the Event object (when
         * called from a click event) or from the tabId parameter.
         *
         * Updates the URL with the `tab` search parameter.
         *
         * @param {string} subTabId Sub tab ID to open.
         * @param {string} apiServiceName (optional) Service name of the $resource wrapper to use.
         * @param {Array} subIds (optional) resource sub-IDs of the current sub tab.
         * @param {boolean} sibling (optional) Indicates if this sub tab is a sibling to its parent.
         * @param {string} noSchedule Defines that no schedule should be create to refresh the subtab data if true.
         */
        this.openSubTab = function (subTabId, apiServiceName, subId, sibling, noSchedule) {
            var params = $location.search(), previous;

            // Determine if any sub tabs need to be restored
            try {
                previous = JSON.parse(params.breadcrumbs);
            } catch (e) {}
            me.generateBreadcrumbs(subTabId, previous, apiServiceName, subId, sibling);

            Modal.modal.find('.modal-content').removeClass('active');

            // When displaying a subNavigation Item, we need to visually
            // activate the parent tab as well.
            me.$scope.tab = Modal.modal.find('[data-modal-tab-content="' + subTabId + '"]')
                .addClass('active').data('parent');

            me.$scope.subTab = subTabId;

            if (me.$scope.breadcrumbs.length > 0) {
                me.loadSubNavData();
            }

            params.breadcrumbs = JSON.stringify(me.$scope.breadcrumbs);
            $location.search(params);

            if (me.refreshScheduler.on) {
                me.refreshScheduler.cancel();
                if (!angular.isDefined(noSchedule) || noSchedule === false) {
                    me.refreshScheduler.newSchedule();
                }
            }
        };

        /**
         * @ngdoc function
         * @name Modal.loadSubNavData
         * @methodOf adminNg.modal.Modal
         * @description
         * Finds the service name in the current breadcrumb information and calls its
         * GET method, with the appropriate id.
         *
         * The loaded data is stored in $scope.subNavData.
         */
        this.loadSubNavData = function () {
            var apiService, previousBreadcrumb = {},
                params = [me.$scope.resourceId];

            angular.forEach(me.$scope.breadcrumbs, function (breadcrumb) {
                if (!breadcrumb.sibling) {
                    params.push(breadcrumb.subId);
                }
                apiService = breadcrumb.api;
                previousBreadcrumb = breadcrumb;
            });
            apiService = $injector.get(apiService);

            params = params.reduce(function (prevValue, currentValue, index) {
                prevValue['id' + index] = currentValue;
                return prevValue;
            }, {});

            apiService.get(params, function (data) {
                me.$scope.subNavData = data;
            });

            if (me.refreshScheduler.on) {
                me.refreshScheduler.newSchedule();
            }
        };

        /**
         * @ngdoc function
         * @name Modal.generateBreadcrumbs
         * @methodOf adminNg.modal.Modal
         * @description
         * Render navigation history for sub tabs.
         *
         * Determines all relevant information from the data attributes
         * of the target tab, such as the depth of the sub tab and its
         * label. The label attribute will be translated.
         *
         * If the `previous` parameter contains breadcrumbs they will be
         * restored.
         *
         * @example
         * <div data-modal-tab-content="quick-actions-details" data-level="2" data-label="QUICK_ACTION_DETAILS_TITLE">
         *
         * @param {string} tabId Tab ID of the current tab.
         * @param {Array} previous breadcrumbs to restore.
         * @param {string} api (optional) Service name of the $resource wrapper to use.
         * @param {Array} subIds (optional) resource sub-IDs of the current sub tab.
         * @param {Boolean} sibling (optional) flag indicating that this sub tab is a sibling to the last.
         */
        this.generateBreadcrumbs = function (tabId, previous, api, subId, sibling) {
            var subNavLevel, subNavLabel, tab, subNav;
            tab = Modal.modal.find('[data-modal-tab-content="' + tabId + '"]');
            subNavLevel = tab.data('level');
            subNavLabel = tab.data('label');
            subNav      = Modal.modal.find('#breadcrumb');

            subNav.empty();

            // Restore previous breadcrumbs from URL
            if (previous && previous.length && previous[previous.length - 1].id === tabId) {
                me.$scope.breadcrumbs = previous;
            }

            // Create a new sub tab or navigate back
            if (me.$scope.breadcrumbs.length + 1 < subNavLevel) {
                me.$scope.breadcrumbs.push({
                    level:   subNavLevel,
                    label:   subNavLabel,
                    id:      tabId,
                    api:     api,
                    sibling: sibling,
                    subId:   subId
                });
            } else {
                me.$scope.breadcrumbs.splice(subNavLevel - 1);
            }

            // Populate the breadcrumbs container with links
            angular.forEach(me.$scope.breadcrumbs, function (item) {
                subNav.append('<a ' +
                    'class="breadcrumb-link active" ' +
                    'data-level="' + item.level + '" ' +
                    'ng-click="openSubTab(\'' + item.id + '\')" ' +
                    'translate>' +
                    item.label +
                    '</a>');
            });
            $compile(subNav)(me.$scope);
            subNav.show();
        };

        this.getAdjacentIndex = function (reverse) {
            var adjacentIndex;
            angular.forEach(Table.rows, function (row, index) {
                if (String(row.id) === String(me.$scope.resourceId)) {
                    adjacentIndex = index;
                    return;
                }
            });
            if (reverse) {
                adjacentIndex -= 1;
            } else {
                adjacentIndex += 1;
            }
            return adjacentIndex;
        };

        this.hasAdjacent = function (reverse) {
            return Table.rows[me.getAdjacentIndex(reverse)];
        };

        /**
         * @ngdoc function
         * @name Modal.showAdjacent
         * @methodOf adminNg.modal.Modal
         * @description
         * Determine and set the next or previous resource ID.
         *
         * @param {boolean} reverse Choose the previous instead of the next record.
         */
        this.showAdjacent = function (reverse) {
            var adjacentId,
                adjacentIndex = me.getAdjacentIndex(reverse),
                params = $location.search();

            if (Table.rows[adjacentIndex]) {
                adjacentId = Table.rows[adjacentIndex].id;
                if(angular.isString(me.$scope.resourceId) && !angular.isString(adjacentId)) {
                    adjacentId = adjacentId.toString();
                }
            }

            if (!angular.isUndefined(adjacentId)) {
                me.$scope.resourceId = adjacentId;
                params.resourceId = adjacentId;
                $location.search(params);
                me.$scope.$broadcast('change', adjacentId);
            }
        };

        /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @param {string} resourceId Identifier for the resource used in the modal.
         * @param {string} tab Identifier for the currently active tab in a modal (optional)
         * @param {string} action Name of the type of content (e.g. add, edit)
         */
        this.show = function (modalId, resourceId, tab, action) {
            var $scope, modalNav, params, http = Modal.show(modalId), subTab;

            this.$scope = Modal.$scope;
            $scope = this.$scope;

            if (!http) { return; }

            $scope.hasAdjacent = me.hasAdjacent;
            $scope.showAdjacent = me.showAdjacent;
            $scope.openTab = me.openTab;
            $scope.openSubTab = me.openSubTab;
            $scope.breadcrumbs = [];
            $scope.resourceId = resourceId;
            $scope.action = action;

            http.then(function () {
                params = $location.search();

                // Set content (by tab or otherwise)
                modalNav = Modal.modal.find('#modal-nav > a');
                if (!modalNav.hasClass('active')) {
                    if (modalNav.length === 0) {
                        Modal.modal.find('> .modal-content').show();
                    } else {
                        $scope.openTab(tab, true);
                        try {
                            subTab = JSON.parse(params.breadcrumbs);
                            subTab = subTab[subTab.length - 1];
                            $scope.openSubTab(subTab.id, subTab.api, subTab.subId, subTab.sibling);
                        } catch (e) {}
                    }
                }

                // Set location
                params.resourceId = resourceId;
                params.action = action;
                $location.search(params);
            });
        };
    };

    return new ResourceModal();
}]);

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
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying a confirmation dialog.
 * partial.
 */
angular.module('adminNg.services.modal')
.factory('ConfirmationModal', ['$location', 'Modal', function ($location, Modal) {
    var ConfirmationModal = function () {
        var me = this;

        /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @param {string} callback Name of the function to call when confirmed
         * @param {string} object Hash for the success callback function
         */
        this.show = function (modalId, callback, object) {
            Modal.show(modalId);
            me.$scope = Modal.$scope;

            me.$scope.confirm  = me.confirm;
            me.$scope.callback = callback;
            me.$scope.object   = object;
            me.$scope.name = "undefined";
            me.$scope.type = "UNKNOWN";
            if (!angular.isUndefined(object)) {
                me.$scope.id = object.id;
                if (object.title) {
                    me.$scope.name = object.title;
                } else if (object.name) {
                    me.$scope.name = object.name;
                }
                if (object.type) {
                    me.$scope.type = object.type;
                }
            }
            //64 picked by random experimentation
            if (me.$scope.name.length > 64) {
                me.$scope.name = me.$scope.name.substr(0,61);
                me.$scope.name = me.$scope.name + "...";
            }
        };

        this.confirm = function () {
            me.$scope.callback(me.$scope.object);
            me.$scope.close();
        };
    };

    return new ConfirmationModal();
}]);

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
 * Dynamic Asset Upload Options List Service
 *
 * A service to retrieve and provide a list of available upload options (track, attachment, catalog).
 *
 * The asset upload options list is a customizable list of assets that can be uplaoded via admin-ng UI
 *
 * The production provider list exists at etc/listproviders/event.upload.asset.options.properties
 * The LOCAL Development surrogate testing file is <admin-ng module testing path>/resources/eventUploadAssetOptions.json
 *
 */
angular.module('adminNg.services').service('UploadAssetOptions',[ 'ResourcesListResource', '$q', function (ResourcesListResource, $q) {
  var _uploadOptions = undefined;
  var _result = undefined;
  var service = {};
  var _OptionPrefixSource = "EVENTS.EVENTS.NEW.SOURCE.UPLOAD";
  var _OptionPrefixAsset = "EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION";
  var _WorkflowPrefix = "EVENTS.EVENTS.NEW.UPLOAD_ASSET.WORKFLOWDEFID";

  service.getOptionsPromise = function(){
    // don't retrieve again if alreay retrieved for this session
    if (!_uploadOptions) {
      var deferred = $q.defer();
      ResourcesListResource.get({resource: 'eventUploadAssetOptions'},
      function (data) {
        _result = {};
        _uploadOptions = [];
        angular.forEach(data, function (assetOption, assetKey) {
          if (assetKey.charAt(0) !== '$') {
            if ((assetKey.indexOf(_OptionPrefixAsset)>=0) || (assetKey.indexOf(_OptionPrefixSource)>=0)) {
            // parse upload asset options
              var options = JSON.parse(assetOption);
              options[ 'title'] = assetKey;
              _uploadOptions.push(options);
            } else if (assetKey.indexOf(_WorkflowPrefix)>=0) {
            // parse upload workflow definition id
              _result['workflow'] = assetOption;
            }
          }
        });
        _result['options'] = _uploadOptions;
        deferred.resolve(_result);
      });
      // set _uploadOptions to be a promise until result comeback
      _result = deferred.promise;
    }
    // This resolves immediately if options were already retrieved
    return $q.when(_result);
  }
  // return the AssetUploadOptions service
  return service;
}]);

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

// Controllers registry.
angular.module('adminNg.controllers', []);

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

// A controller for global page navigation
angular.module('adminNg.controllers')
.controller('NavCtrl', ['$scope', '$rootScope', '$location', '$window', '$resource', '$routeParams', 'Language',
    function ($scope, $rootScope, $location, $window, $resource, $routeParams, Language) {
        // FIXME Move this information to the Language service so it can be
        // fetched via Language.getAvailableLanguages().

        $scope.category = $routeParams.category || $rootScope.category;

        $scope.availableLanguages = [];

        $scope.changeLanguage = function (key) {
            Language.changeLanguage(key);
        };

        $rootScope.$on('language-changed', function () {
            $scope.currentLanguageCode = Language.getLanguageCode();
            $scope.currentLanguageName = Language.getLanguage().displayLanguage;
            $scope.availableLanguages = Language.getAvailableLanguages();
        });

        $scope.logout = function () {
            $window.location.href = $window.location.origin + '/j_spring_security_logout';
        };
    }
]);

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

// The main controller that all other scopes inherit from (except isolated scopes).
angular.module('adminNg.controllers')
.controller('ApplicationCtrl', ['$scope', '$rootScope', '$location', '$window', 'AuthService', 'Notifications',
            'ResourceModal', 'VersionResource', 'HotkeysService', '$interval', 'RestServiceMonitor',
    function ($scope, $rootScope, $location, $window, AuthService, Notifications, ResourceModal,
              VersionResource, HotkeysService, $interval, RestServiceMonitor){

        $scope.bodyClicked = function () {
            angular.element('[old-admin-ng-dropdown]').removeClass('active');
        };

        var FEEDBACK_URL_PROPERTY = 'org.opencastproject.admin.feedback.url',
            DOCUMENTATION_URL_PROPERTY = 'org.opencastproject.admin.help.documentation.url',
            RESTDOCS_URL_PROPERTY = 'org.opencastproject.admin.help.restdocs.url',
            MEDIA_MODULE_URL_PROPERTY = 'org.opencastproject.admin.mediamodule.url';

        $scope.currentUser  = null;
        $scope.feedbackUrl = undefined;
        $scope.documentationUrl = undefined;
        $scope.restdocsUrl = undefined;
        $scope.mediaModuleUrl = undefined;
        RestServiceMonitor.run();
        $scope.services = RestServiceMonitor.getServiceStatus();

        AuthService.getUser().$promise.then(function (user) {
            $scope.currentUser = user;

            if (angular.isDefined(user.org.properties[FEEDBACK_URL_PROPERTY])) {
                $scope.feedbackUrl = user.org.properties[FEEDBACK_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[DOCUMENTATION_URL_PROPERTY])) {
                $scope.documentationUrl = user.org.properties[DOCUMENTATION_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[RESTDOCS_URL_PROPERTY])) {
                $scope.restdocsUrl = user.org.properties[RESTDOCS_URL_PROPERTY];
            }

            if (angular.isDefined(user.org.properties[MEDIA_MODULE_URL_PROPERTY])) {
                $scope.mediaModuleUrl = user.org.properties[MEDIA_MODULE_URL_PROPERTY];
            }
        });

        //Running RestService on loop - updating $scope.service
        $interval(function(){
            RestServiceMonitor.run();
            $scope.service = RestServiceMonitor.getServiceStatus();
        }, 60000);

        $scope.toServices = function(event) {
            RestServiceMonitor.jumpToServices(event);
        };

        $scope.toDoc = function () {
            if ($scope.documentationUrl) {
                $window.open ($scope.documentationUrl);
            } else {
                console.warn('Documentation Url is not set.');
            }
        };

        $scope.toRestDoc = function () {
          if ($scope.restdocsUrl) {
              $window.open ($scope.restdocsUrl);
          } else {
              console.warn('REST doc Url is not set.');
          }
        };

        $rootScope.userIs = AuthService.userIsAuthorizedAs;

        // Restore open modals if any
        var params = $location.search();
        if (params.modal && params.resourceId) {
            ResourceModal.show(params.modal, params.resourceId, params.tab, params.action);
        }

        if (angular.isUndefined($rootScope.version)) {
            VersionResource.query(function(response) {
                $rootScope.version = response.version ? response : (angular.isArray(response.versions)?response.versions[0]:{});
                if (!response.consistent) {
                    $rootScope.version.buildNumber = 'inconsistent';
                }
            });
        }

        HotkeysService.activateUniversalHotkey("general.event_view", "Open Events Table", function(event) {
            event.preventDefault();
            $location.path('/events/events').replace();
        });

        HotkeysService.activateUniversalHotkey("general.series_view", "Open Series Table", function(event) {
            event.preventDefault();
            $location.path('/events/series').replace();
        });

        HotkeysService.activateUniversalHotkey("general.new_event", "Create New Event", function(event) {
            event.preventDefault();
            ResourceModal.show("new-event-modal");
        });

        HotkeysService.activateUniversalHotkey("general.new_series", "Create New Series", function(event) {
            event.preventDefault();
            ResourceModal.show("new-series-modal");
        });

        HotkeysService.activateUniversalHotkey("general.help", "Show Help", function(event) {
            event.preventDefault();
            if(angular.element('#help-dd').hasClass('active')) {
              angular.element('#help-dd').removeClass('active');
            } else {
              angular.element('#help-dd').addClass('active');
            }
        })
    }
]);

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

// A controller for global page navigation
angular.module('adminNg.controllers')
.controller('UserPreferencesCtrl', ['$scope', 'SignatureResource', 'IdentityResource',
    function ($scope, SignatureResource, IdentityResource) {
        var persist = function (persistenceMethod) {
            var me = IdentityResource.get();
            me.$promise.then(function (data) {
                $scope.userprefs.username = data.username;
                SignatureResource[persistenceMethod]($scope.userprefs);
            });
        };

        // load the current user preferences
        $scope.userprefs = SignatureResource.get({});

        // perform update
        $scope.update = function () {
            if ($scope.userPrefForm.$valid) {
                persist('update');
                $scope.close();
            }
        };


        // perform save
        $scope.save = function () {
            if ($scope.userPrefForm.$valid) {
                persist('save');
                $scope.close();
            }
        };
    }
]);


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

// A controller for global page navigation
angular.module('adminNg.controllers')
.controller('TablePreferencesCtrl', ['$scope', 'Table', 'Modal', 'Storage', '$translate',
    function ($scope, Table, Modal, Storage, $translate) {
        var filteredColumns, cloneColumns;

        cloneColumns = function () {
            return JSON.parse(JSON.stringify(Table.columns));
        };

        filteredColumns = function (isDeactivated) {
            var result = [];
            angular.forEach($scope.columnsClone, function (column) {
                if (column.deactivated === isDeactivated) {
                    result.push(column);
                }
            });
            return result;
        };

        $scope.table = Table;
        $scope.keyUp = Modal.keyUp;

        $scope.changeColumn = function (column, deactivate) {
            if (deactivate) {
                $scope.activeColumns.splice($scope.activeColumns.indexOf(column), 1);
                $scope.deactivatedColumns.push(column);
            }
            else {
                $scope.deactivatedColumns.splice($scope.deactivatedColumns.indexOf(column), 1);
                $scope.activeColumns.push(column);
            }
            column.deactivated = deactivate;
        };

        $scope.save = function () {
            var type = 'table_column_visibility', namespace = Table.resource, prefs = 'columns',
                settings = $scope.deactivatedColumns.concat($scope.activeColumns);
            Storage.put(type, namespace, prefs, settings);
        };

        $scope.initialize = function () {
            $scope.columnsClone = cloneColumns();
            $scope.deactivatedColumns = filteredColumns(true);
            $scope.activeColumns = filteredColumns(false);
            $translate(Table.caption).then(function (translation) {
                $scope.tableName = translation;
            });
            $translate('RESET').then(function (translation) {
                $scope.resetTranslation = translation;
            });
        };
        $scope.initialize();
    }
]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EventsCtrl', ['$scope', 'Stats', 'Table', 'EventsResource', 'ResourcesFilterResource', 'ResourcesListResource', 'Notifications', 'ResourceModal', 'ConfirmationModal', 'EventHasSnapshotsResource',
    function ($scope, Stats, Table, EventsResource, ResourcesFilterResource, ResourcesListResource, Notifications, ResourceModal, ConfirmationModal, EventHasSnapshotsResource) {
        // Configure the table service
        $scope.dateToFilterValue = function(dateString) {
          var date = new Date(dateString);
          var from = new Date(date.setHours(0, 0, 0, 0));
          var to = new Date(date.setHours(23, 59, 59, 999));
          return from.toISOString() + "/" + to.toISOString();
        };
        $scope.stats = Stats;
        $scope.stats.configure({
            stats: [
            {filters: [{name: 'status',
                        filter: 'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.SCHEDULED'}],
             description: 'DASHBOARD.SCHEDULED'},
            {filters: [{name: 'startDate',
                        filter:'FILTERS.EVENTS.START_DATE',
                        value: $scope.dateToFilterValue(new Date().toISOString())}],
             description: 'DATES.TODAY'},
            {filters: [{name: 'status',
                        filter: 'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.RECORDING'}],
             description: 'DASHBOARD.RECORDING'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSING'}],
             description: 'DASHBOARD.RUNNING'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PAUSED'}],
             description: 'DASHBOARD.PAUSED'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSING_FAILURE'}],
             description: 'DASHBOARD.FAILED'},
            {filters: [{name: 'comments',
                        filter:'FILTERS.EVENTS.COMMENTS.LABEL',
                        value: 'OPEN'},
                       {name: 'status',
                        filter: 'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSED'}],
             description: 'DASHBOARD.FINISHED_WITH_COMMENTS'},
            {filters: [{name: 'status',
                        filter:'FILTERS.EVENTS.STATUS.LABEL',
                        value: 'EVENTS.EVENTS.STATUS.PROCESSED'}],
             description: 'DASHBOARD.FINISHED'}
            ],
            resource:   'events',
            apiService: EventsResource
        });
        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'title',
                label: 'EVENTS.EVENTS.TABLE.TITLE'
            }, {
                name:  'presenter',
                label: 'EVENTS.EVENTS.TABLE.PRESENTERS'
            }, {
                template: 'modules/events/partials/eventsSeriesCell.html',
                name:  'series_name',
                label: 'EVENTS.EVENTS.TABLE.SERIES'
            }, {
                template: 'modules/events/partials/eventsTechnicalDateCell.html',
                name:  'technical_date',
                label: 'EVENTS.EVENTS.TABLE.DATE'
            }, {
                name:  'technical_start',
                label: 'EVENTS.EVENTS.TABLE.START'
            }, {
                name:  'technical_end',
                label: 'EVENTS.EVENTS.TABLE.STOP'
            }, {
                template: 'modules/events/partials/eventsLocationCell.html',
                name:  'location',
                label: 'EVENTS.EVENTS.TABLE.LOCATION'
            }, {
                name:  'published',
                label: 'EVENTS.EVENTS.TABLE.PUBLISHED',
                template: 'modules/events/partials/publishedCell.html',
                dontSort: true
            }, {
                template: 'modules/events/partials/eventsStatusCell.html',
                name:  'event_status',
                label: 'EVENTS.EVENTS.TABLE.SCHEDULING_STATUS'
            }, {
                template: 'modules/events/partials/eventActionsCell.html',
                label:    'EVENTS.EVENTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'EVENTS.EVENTS.TABLE.CAPTION',
            resource:   'events',
            category:   'events',
            apiService: EventsResource,
            multiSelect: true,
            postProcessRow: function (row) {
                angular.forEach(row.publications, function (publication, index) {
                    if (angular.isDefined($scope.publicationChannels[publication.id])) {
                        var record = JSON.parse($scope.publicationChannels[publication.id]);
                        publication.label = record.label ? record.label : publication.name;
                        publication.icon = record.icon;
                        publication.hide = record.hide;
                        publication.description = record.description;
                        publication.order = record.order ? record.order : 999 + index;
                    } else {
                        publication.label = publication.name;
                        publication.order = 999 + index;
                    }
                });
                row.checkedDelete = function() {
                  EventHasSnapshotsResource.get({id: row.id},function(o) {
                    if ((angular.isUndefined(row.publications) || row.publications.length <= 0 || !o.hasSnapshots) && !row.has_preview )
                          // Works, opens simple modal
                          ConfirmationModal.show('confirm-modal',Table.delete,row);
                      else
                          // works, opens retract
                          ResourceModal.show('retract-published-event-modal',row.id);
                  });
                }
            }
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
        $scope.publicationChannels = ResourcesListResource.get({ resource: 'PUBLICATION.CHANNELS' });

        $scope.table.dateToFilterValue = $scope.dateToFilterValue;

        $scope.table.delete = function (row) {
            EventsResource.delete({id: row.id}, function () {
                Table.fetch();
                Notifications.add('success', 'EVENTS_DELETED');
            }, function () {
                Notifications.add('error', 'EVENTS_NOT_DELETED');
            });
        };

        $scope.$on('$destroy', function() {
            // stop polling event stats on an inactive tab
            $scope.stats.refreshScheduler.cancel();
        });
    }
]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EventCtrl', [
    '$scope', 'Notifications', 'EventTransactionResource', 'EventMetadataResource', 'EventAssetsResource',
    'EventCatalogsResource', 'CommentResource', 'EventWorkflowsResource', 'EventWorkflowActionResource', 'EventWorkflowDetailsResource',
    'ResourcesListResource', 'UserRolesResource', 'EventAccessResource', 'EventGeneralResource',
    'OptoutsResource', 'EventParticipationResource', 'EventSchedulingResource', 'NewEventProcessingResource',
    'OptoutSingleResource', 'CaptureAgentsResource', 'ConflictCheckResource', 'Language', 'JsHelper', '$sce', '$timeout', 'EventHelperService',
    'UploadAssetOptions', 'EventUploadAssetResource', 'Table', 'SchedulingHelperService',
    function ($scope, Notifications, EventTransactionResource, EventMetadataResource, EventAssetsResource, EventCatalogsResource, CommentResource,
        EventWorkflowsResource, EventWorkflowActionResource, EventWorkflowDetailsResource, ResourcesListResource, UserRolesResource, EventAccessResource, EventGeneralResource,
        OptoutsResource, EventParticipationResource, EventSchedulingResource, NewEventProcessingResource,
        OptoutSingleResource, CaptureAgentsResource, ConflictCheckResource, Language, JsHelper, $sce, $timeout, EventHelperService, UploadAssetOptions,
        EventUploadAssetResource, Table, SchedulingHelperService) {

        var roleSlice = 100;
        var roleOffset = 0;
        var loading = false;
        var rolePromise = null;

        var saveFns = {},
            me = this,
            NOTIFICATION_CONTEXT = 'events-access',
            SCHEDULING_CONTEXT = 'event-scheduling',
            mainCatalog = 'dublincore/episode',
            idConfigElement = '#event-workflow-configuration',
            workflowConfigEl = angular.element(idConfigElement),
            baseWorkflow,
            createPolicy = function (role) {
                return {
                    role  : role,
                    read  : false,
                    write : false,
                    actions : {
                        name : 'event-acl-actions',
                        value : []
                    }
                };
            },
            findWorkflow = function (id) {
                var workflow;

                angular.forEach($scope.workflowDefinitions, function (w) {
                    if (w.id === id) {
                        workflow = w;
                    }
                });

                if (!angular.isDefined(workflow)) {
                    return baseWorkflow;
                } else {
                    return workflow;
                }

            },
            addChangeDetectionToInputs = function () {
                var element, isRendered = angular.element(idConfigElement).find('.configField').length > 0;
                if (!angular.isDefined($scope.workflows.workflow.configuration_panel) || !$scope.workflows.workflow.configuration_panel.trim()) {
                    // The workflow contains no configuration (it is empty), therefore it is rendered.
                    isRendered = true;
                }
                if (!isRendered) {
                    $timeout(addChangeDetectionToInputs, 200);
                    return;
                } else {
                    element = angular.element(idConfigElement).find('.configField');
                }

                element.each(function (idx, el) {
                    var element = angular.element(el);

                    if (angular.isDefined(element.attr('id'))) {
                        element.change($scope.saveWorkflowConfig);
                    }
                });
            },
            updateConfigurationPanel = function (html) {
                if (angular.isUndefined(html)) {
                    html = undefined;
                }
                $scope.workflowConfiguration = $sce.trustAsHtml(html);
                addChangeDetectionToInputs();
            },
            // Get the workflow configuration
            getWorkflowConfig = function () {
                var workflowConfig = {}, element, isRendered = angular.element(idConfigElement).find('.configField').length > 0;

                if (!angular.isDefined($scope.workflows.workflow.configuration_panel) || !$scope.workflows.workflow.configuration_panel.trim()) {
                    // The workflow contains no configuration (it is empty), therefore it is rendered.
                    isRendered = true;
                }

                if (!isRendered) {
                    element = angular.element($scope.workflows.workflow.configuration_panel).find('.configField');
                } else {
                    element = angular.element(idConfigElement).find('.configField');
                }

                element.each(function (idx, el) {
                    var element = angular.element(el);

                    if (angular.isDefined(element.attr('id'))) {
                        if (element.is('[type=checkbox]') || element.is('[type=radio]')) {
                            workflowConfig[element.attr('id')] = element.is(':checked') ? 'true' : 'false';
                        } else {
                            workflowConfig[element.attr('id')] = element.val();
                        }
                    }
                });

                return workflowConfig;
            },
            setWorkflowConfig = function () {
                var isRendered = angular.element(idConfigElement).find('.configField').length > 0;

                if (!angular.isDefined($scope.workflows.workflow.configuration_panel) || !$scope.workflows.workflow.configuration_panel.trim()) {
                    // The workflow contains no configuration (it is empty), therefore it is rendered.
                    isRendered = true;
                }

                if (!isRendered) {
                    $timeout(setWorkflowConfig, 200);
                } else {
                    angular.forEach(baseWorkflow.configuration, function (value, key) {
                        var el = angular.element(idConfigElement).find('#' + key + '.configField');

                        if (el.length > 0) {
                            if (el.is('[type=checkbox]') || el.is('[type=radio]')) {
                                if (value === 'true' || value === true) {
                                    el.attr('checked','checked');
                                }
                            } else {
                                el.val(value);
                            }
                        }

                    });
                    me.loadingWorkflow = false;
                }
            },
            changePolicies = function (access, loading) {
                var newPolicies = {};
                angular.forEach(access, function (acl) {
                    var policy = newPolicies[acl.role];

                    if (angular.isUndefined(policy)) {
                        newPolicies[acl.role] = createPolicy(acl.role);
                    }
                    if (acl.action === 'read' || acl.action === 'write') {
                        newPolicies[acl.role][acl.action] = acl.allow;
                    } else if (acl.allow === true || acl.allow === 'true'){
                        newPolicies[acl.role].actions.value.push(acl.action);
                    }
                });

                $scope.policies = [];
                angular.forEach(newPolicies, function (policy) {
                    $scope.policies.push(policy);
                });

                if (!loading) {
                    $scope.accessSave();
                }
            },
            checkForActiveTransactions = function () {
                EventTransactionResource.hasActiveTransaction({id: $scope.resourceId }, function (data) {
                    $scope.transactions.read_only = angular.isUndefined(data.hasActiveTransaction) ? true : data.hasActiveTransaction;

                    if ($scope.transactions.read_only) {
                      if (!angular.isUndefined(me.transactionNotification)) {
                          Notifications.remove(me.transactionNotification, NOTIFICATION_CONTEXT);
                      }
                      me.transactionNotification = Notifications.add('warning', 'ACTIVE_TRANSACTION', NOTIFICATION_CONTEXT);
                      $scope.$emit('ACTIVE_TRANSACTION');
                    } else {
                      if (!angular.isUndefined(me.transactionNotification)) {
                          Notifications.remove(me.transactionNotification, NOTIFICATION_CONTEXT);
                      }
                      $scope.$emit('NO_ACTIVE_TRANSACTION');
                    }
                });

                $scope.checkForActiveTransactionsTimer = $timeout(checkForActiveTransactions, 3000);
            },
            updateRoles = function() {
              //MH-11716: We have to wait for both the access (series ACL), and the roles (list of system roles)
              //to resolve before we can add the roles that are present in the series but not in the system
              return ResourcesListResource.get({ resource: 'ROLES' }, function (results) {
                var roles = results;
                return $scope.access.$promise.then(function () {
                    angular.forEach($scope.access.episode_access.privileges, function(value, key) {
                        if (angular.isUndefined(roles[key])) {
                            roles[key] = key;
                        }
                    }, this);
                    return roles;
                });
              }, this);
            },
            cleanupScopeResources = function() {
              $timeout.cancel($scope.checkForActiveTransactionsTimer);
              if ($scope.lastNotificationId) {
                  Notifications.remove($scope.lastNotificationId, 'event-scheduling');
                  $scope.lastNotificationId = undefined;
              }
              me.clearConflicts();
            },
            fetchChildResources = function (id) {

                var general = EventGeneralResource.get({ id: id }, function () {
                    angular.forEach(general.publications, function (publication, index) {
                        publication.label = publication.name;
                        publication.order = 999 + index;
                        var now = new Date();
                        if (publication.id == "engage-live" && 
                        	(now < new Date(general["start-date"]) || now > new Date(general["end-date"])))
                        	publication.enabled = false;
                        else publication.enabled = true;
                    });
                    $scope.publicationChannels = ResourcesListResource.get({ resource: 'PUBLICATION.CHANNELS' }, function() {
                        angular.forEach(general.publications, function (publication) {
                            if(angular.isDefined($scope.publicationChannels[publication.id])) {
                                var record = JSON.parse($scope.publicationChannels[publication.id]);
                                if (record.label) publication.label = record.label;
                                if (record.icon) publication.icon = record.icon;
                                if (record.hide) publication.hide = record.hide;
                                if (record.description) publication.description = record.description;
                                if (record.order) publication.order = record.order;
                            }
                        });
                        // we postpone setting $scope.general until this point to avoid UI "flickering" due to publications changing
                        $scope.general = general;
                    }, function() {
                        $scope.general = general;
                    });
                });

                $scope.metadata =  EventMetadataResource.get({ id: id }, function (metadata) {
                    var episodeCatalogIndex;
                    angular.forEach(metadata.entries, function (catalog, index) {
                        if (catalog.flavor === mainCatalog) {
                            $scope.episodeCatalog = catalog;
                            episodeCatalogIndex = index;
                            var keepGoing = true;
                            var tabindex = 2;
                            angular.forEach(catalog.fields, function (entry) {
                                if (entry.id === 'title' && angular.isString(entry.value)) {
                                    $scope.titleParams = { resourceId: entry.value.substring(0,70) };
                                }
                                if (keepGoing && entry.locked) {
                                    metadata.locked = entry.locked;
                                    keepGoing = false;
                                }
                                entry.tabindex = tabindex ++;
                            });
                        }
                    });

                    if (angular.isDefined(episodeCatalogIndex)) {
                        metadata.entries.splice(episodeCatalogIndex, 1);
                    }
                });

                //<===============================
                // Enable asset upload (catalogs and attachments) to existing events

                // Retrieve option configuration for asset upload
                UploadAssetOptions.getOptionsPromise().then(function(data){
                    if (data) {
                        $scope.assetUploadWorkflowDefId = data.workflow;
                        $scope.uploadAssetOptions = [];
                        // Filter out asset options of type "track".
                        // Not allowing tracks to be added to existing mediapackages
                        // for this iteration of the upload option feature.
                        // TODO: consider enabling track uploads to existing mps.
                        angular.forEach(data.options, function(option) {
                          if (option.type !== 'track') {
                             $scope.uploadAssetOptions.push(option);
                          }
                        });
                        // if no asset options, undefine the option variable
                        $scope.uploadAssetOptions = $scope.uploadAssetOptions.length > 0 ? $scope.uploadAssetOptions : undefined;
                        $scope.newAssets = {};
                    }
                });
                $scope.saveAssetsKeyUp = function (event) {
                    if (event.keyCode === 13 || event.keyCode === 32) {
                        $scope.saveAssets();
                    }
                };

                // Save and start upload asset request and workflow
                $scope.saveAssets = function() {
                    // The transaction becomes read-only if a workflow is running for this event.
                    // Ref endpoint hasActiveTransaction(@PathParam("eventId") String eventId)
                    if ($scope.transactions.read_only) {
                        me.transactionNotification = Notifications.add('warning', 'ACTIVE_TRANSACTION', NOTIFICATION_CONTEXT, 3000);
                        return;
                    }
                    // Verify there are assets to upload
                    if (angular.equals($scope.newAssets, {})) {
                        return;
                    }
                    var userdata = { metadata: {}};

                    // save metadata map (contains flavor mapping used by the server)
                    userdata.metadata["assets"] = ($scope.uploadAssetOptions);

                    // save file assets (passed in a separate request field from its metadata map)
                    userdata["upload-asset"] = $scope.newAssets;

                    // save workflow definition id (defined in the asset upload configuration provided-list)
                    userdata["workflow"] = $scope.assetUploadWorkflowDefId;

                    EventUploadAssetResource.save({id: $scope.resourceId }, userdata, function (data) {
                        me.transactionNotification = Notifications.add('success', 'EVENTS_CREATED', NOTIFICATION_CONTEXT, 6000);
                        $scope.openTab('assets');
                        }, function () {
                        me.transactionNotification = Notifications.add('error', 'EVENTS_NOT_CREATED', NOTIFICATION_CONTEXT, 6000);
                        $scope.openTab('assets');
                    });
                };
                // <==========================

                $scope.acls = ResourcesListResource.get({ resource: 'ACL' });
                $scope.actions = {};
                $scope.hasActions = false;
                ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
                    angular.forEach(data, function (value, key) {
                        if (key.charAt(0) !== '$') {
                            $scope.actions[key] = value;
                            $scope.hasActions = true;
                        }
                    });
                });
                $scope.roles = updateRoles();

                $scope.assets = EventAssetsResource.get({ id: id });

                $scope.participation = EventParticipationResource.get({ id: id }, function (data) {
                    if (data.read_only) {
                        $scope.lastNotificationId = Notifications.add('warning', 'EVENT_PARTICIPATION_STATUS_READONLY', 'event-scheduling', -1);
                    }
                });

                $scope.workflow = {};
                $scope.workflows = EventWorkflowsResource.get({ id: id }, function () {
                    if (angular.isDefined($scope.workflows.workflow)) {
                        baseWorkflow = $scope.workflows.workflow;
                        $scope.workflow.id = $scope.workflows.workflow.workflowId;
                        $scope.workflowDefinitionsObject = NewEventProcessingResource.get({
                            tags: 'schedule'
                        }, function () {
                            $scope.workflowDefinitions = $scope.workflowDefinitionsObject.workflows;
                            $scope.changeWorkflow(true);
                            setWorkflowConfig();
                        });
                    }
                });

                $scope.source = EventSchedulingResource.get({ id: id }, function (source) {
                    source.presenters = angular.isArray(source.presenters) ? source.presenters.join(', ') : '';
                    $scope.scheduling.hasProperties = true;
                    CaptureAgentsResource.query({inputs: true}).$promise.then(function (data) {
                        $scope.captureAgents = data.rows;
                        angular.forEach(data.rows, function (agent) {
                            var inputs;
                            if (agent.id === $scope.source.agentId) {
                                source.device = agent;
                                // Retrieve agent inputs configuration
                                if (angular.isDefined(source.agentConfiguration['capture.device.names'])) {
                                    inputs = source.agentConfiguration['capture.device.names'].split(',');
                                    source.device.inputMethods = {};
                                    angular.forEach(inputs, function (input) {
                                        source.device.inputMethods[input] = true;
                                    });
                                }
                            }
                        });
                    });
                }, function () {
                   $scope.scheduling.hasProperties = false;
                });

                $scope.access = EventAccessResource.get({ id: id }, function (data) {
                    if (angular.isDefined(data.episode_access)) {
                        var json = angular.fromJson(data.episode_access.acl);
                        changePolicies(json.acl.ace, true);
                    }
                });
                $scope.comments = CommentResource.query({ resource: 'event', resourceId: id, type: 'comments' });
            },
            tzOffset = (new Date()).getTimezoneOffset() / -60;


        $scope.getMoreRoles = function (value) {

            if (loading)
                return rolePromise;

            loading = true;
            var queryParams = {limit: roleSlice, offset: roleOffset};

            if ( angular.isDefined(value) && (value != "")) {
                //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
                //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
                queryParams["filter"] = "role_name:"+ value +",role_target:ACL";
                queryParams["offset"] = 0;
            } else {
                queryParams["filter"] = "role_target:ACL";
            }
            rolePromise = UserRolesResource.query(queryParams);
            rolePromise.$promise.then(function (data) {
                angular.forEach(data, function (role) {
                    $scope.roles[role.name] = role.value;
                });
                roleOffset = Object.keys($scope.roles).length;
            }).finally(function () {
                loading = false;
            });
            return rolePromise;
        };

        /**
         * <===============================
         * START Scheduling related resources
         */

        /* Get the current client timezone */
        $scope.tz = 'UTC' + (tzOffset < 0 ? '-' : '+') + tzOffset;

        $scope.scheduling = {};
        $scope.sortedWeekdays = JsHelper.getWeekDays();
        $scope.hours = JsHelper.initArray(24);
        $scope.minutes = JsHelper.initArray(60);

        $scope.conflicts = [];

        this.readyToPollConflicts = function () {
            var data = $scope.source;
            return angular.isDefined(data) &&
                angular.isDefined(data.start) && angular.isDefined(data.start.date) && data.start.date.length > 0 &&
                angular.isDefined(data.duration) &&
                angular.isDefined(data.duration.hour) && angular.isDefined(data.duration.minute) &&
                angular.isDefined(data.device) && angular.isDefined(data.device.id) && data.device.id.length > 0;
        };

        this.clearConflicts = function () {
          $scope.conflicts = [];
          if (me.notificationConflict) {
              Notifications.remove(me.notificationConflict, SCHEDULING_CONTEXT);
              me.notifictationConflict = undefined;
          }
        }

        this.noConflictsDetected = function () {
            me.clearConflicts();
            $scope.checkingConflicts = false;
        };

        this.conflictsDetected = function (response) {
            me.clearConflicts();
            if (response.status === 409) {
                me.notificationConflict = Notifications.add('error', 'CONFLICT_DETECTED', SCHEDULING_CONTEXT);
                angular.forEach(response.data, function (data) {
                    $scope.conflicts.push({
                        title: data.title,
                        start: Language.formatDateTime('medium', data.start),
                        end: Language.formatDateTime('medium', data.end)
                    });
                });
            }
            $scope.checkingConflicts = false;
        };

        $scope.checkConflicts = function () {
            return new Promise(function(resolve, reject) {
                $scope.checkingConflicts = true;
                if (me.readyToPollConflicts()) {
                    ConflictCheckResource.check($scope.source, me.noConflictsDetected, me.conflictsDetected)
                        .$promise.then(function() {
                            resolve();
                        })
                        .catch(function(err) {
                            reject();
                        });
                } else {
                   $scope.checkingConflicts = false;
                   resolve();
                }
            });
        };

        $scope.saveScheduling = function () {
            if (me.readyToPollConflicts()) {
                ConflictCheckResource.check($scope.source, function () {
                    me.clearConflicts();

                    $scope.source.agentId = $scope.source.device.id;
                    $scope.source.agentConfiguration['capture.device.names'] = '';

                    angular.forEach($scope.source.device.inputMethods, function (value, key) {
                        if (value) {
                            if ($scope.source.agentConfiguration['capture.device.names'] !== '') {
                                $scope.source.agentConfiguration['capture.device.names'] += ',';
                            }
                            $scope.source.agentConfiguration['capture.device.names'] += key;
                        }
                    });

                    EventSchedulingResource.save({
                        id: $scope.resourceId,
                        entries: $scope.source
                    });
                }, me.conflictsDetected);
            }
        };

        $scope.onTemporalValueChange = function(type) {
            SchedulingHelperService.applyTemporalValueChange($scope.source, type, true);
            $scope.saveScheduling();
        }

        /**
         * End Scheduling related resources
         * ===============================>
         */

        $scope.policies = [];
        $scope.baseAcl = {};

        $scope.changeBaseAcl = function () {
            $scope.baseAcl = EventAccessResource.getManagedAcl({id: this.baseAclId}, function () {
                changePolicies($scope.baseAcl.acl.ace);
            });
            this.baseAclId = '';
        };

        $scope.addPolicy = function () {
            $scope.policies.push(createPolicy());
        };

        $scope.deletePolicy = function (policyToDelete) {
            var index;

            angular.forEach($scope.policies, function (policy, idx) {
                if (policy.role === policyToDelete.role &&
                    policy.write === policyToDelete.write &&
                    policy.read === policyToDelete.read) {
                    index = idx;
                }
            });

            if (angular.isDefined(index)) {
                $scope.policies.splice(index, 1);
            }

            $scope.accessSave();
        };

        $scope.getPreview = function (url) {
            return [{
                uri: url
            }];
        };

        $scope.updateOptout = function (newBoolean) {

            OptoutSingleResource.save({
                resource: 'event',
                id: $scope.resourceId,
                optout: newBoolean
            }, function () {
                Notifications.add('success', 'EVENT_PARTICIPATION_STATUS_UPDATE_SUCCESS', 'event-scheduling');
            }, function () {
                Notifications.add('error', 'EVENT_PARTICIPATION_STATUS_UPDATE_ERROR', 'event-scheduling');
            });

        };

        me.loadingWorkflow = true;

        // Listener for the workflow selection
        $scope.changeWorkflow = function (noSave) {
            // Skip the changing workflow call if the view is not loaded
            if (me.loadingWorkflow && !noSave) {
                return;
            }

            me.changingWorkflow = true;
            workflowConfigEl = angular.element(idConfigElement);
            if (angular.isDefined($scope.workflow.id)) {
                $scope.workflows.workflow = findWorkflow($scope.workflow.id);
                updateConfigurationPanel($scope.workflows.workflow.configuration_panel);
            } else {
                updateConfigurationPanel();
            }

            if (!noSave) {
                $scope.saveWorkflowConfig();
            }
            me.changingWorkflow = false;
        };

        $scope.saveWorkflowConfig = function () {
            EventWorkflowsResource.save({
                id: $scope.resourceId,
                entries: {
                    id: $scope.workflows.workflow.id,
                    configuration: getWorkflowConfig()
                }
            }, function () {
                baseWorkflow = {
                    workflowId: $scope.workflows.workflow.id,
                    configuration: getWorkflowConfig()
                };
            });
        };

        $scope.replyToId = null; // the id of the comment to which the user wants to reply
        if (! $scope.resourceId) {
            $scope.resourceId = EventHelperService.eventId;
        }
        $scope.title = $scope.resourceId; // if nothing else use the resourceId

        fetchChildResources($scope.resourceId);

        $scope.$on('change', function (event, id) {
            cleanupScopeResources();
            fetchChildResources(id);
            checkForActiveTransactions();
        });

        $scope.transactions = {
            read_only: false
        };

        // Generate proxy function for the save metadata function based on the given flavor
        // Do not generate it
        $scope.getSaveFunction = function (flavor) {
            var fn = saveFns[flavor],
                catalog;

            if (angular.isUndefined(fn)) {
                if ($scope.episodeCatalog.flavor === flavor) {
                    catalog = $scope.episodeCatalog;
                } else {
                    angular.forEach($scope.metadata.entries, function (c) {
                        if (flavor === c.flavor) {
                            catalog = c;
                        }
                    });
                }

                fn = function (id, callback) {
                    $scope.metadataSave(id, callback, catalog);
                };

                saveFns[flavor] = fn;
            }
            return fn;
        };

        $scope.metadataSave = function (id, callback, catalog) {
            catalog.attributeToSend = id;
            EventMetadataResource.save({ id: $scope.resourceId }, catalog,  function () {
                if (angular.isDefined(callback)) {
                    callback();
                }
                // Mark the saved attribute as saved
                angular.forEach(catalog.fields, function (entry) {
                    if (entry.id === id) {
                        entry.saved = true;
                    }
                });
            });
        };

        $scope.components = ResourcesListResource.get({ resource: 'components' });

        $scope.myComment = {};

        $scope.replyTo = function (comment) {
            $scope.replyToId = comment.id;
            $scope.originalComment = comment;
            $scope.myComment.resolved = false;
        };

        $scope.exitReplyMode = function () {
            $scope.replyToId = null;
            $scope.myComment.text = '';
        };

        $scope.comment = function () {
            $scope.myComment.saving = true;
            CommentResource.save({ resource: 'event', resourceId: $scope.resourceId, type: 'comment' },
                { text: $scope.myComment.text, reason: $scope.myComment.reason },
                function () {
                    $scope.myComment.saving = false;
                    $scope.myComment.text = '';

                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }, function () {
                    $scope.myComment.saving = false;
                }
            );
        };

        $scope.reply = function () {
            $scope.myComment.saving = true;
            CommentResource.save({ resource: 'event', resourceId: $scope.resourceId, id: $scope.replyToId, type: 'comment', reply: 'reply' },
                { text: $scope.myComment.text, resolved: $scope.myComment.resolved },
                function () {
                    $scope.myComment.saving = false;
                    $scope.myComment.text = '';

                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }, function () {
                    $scope.myComment.saving = false;
                }

            );
            $scope.exitReplyMode();
        };

        this.accessSaved = function () {
          Notifications.add('info', 'SAVED_ACL_RULES', NOTIFICATION_CONTEXT);
        };

        this.accessNotSaved = function () {
          Notifications.add('error', 'ACL_NOT_SAVED', NOTIFICATION_CONTEXT);

          $scope.access = EventAccessResource.get({ id: $scope.resourceId }, function (data) {
              if (angular.isDefined(data.episode_access)) {
                  var json = angular.fromJson(data.episode_access.acl);
                  changePolicies(json.acl.ace, true);
              }
          });
        };

        $scope.accessChanged = function (role) {
          if (!role) return;
          $scope.accessSave();
        };

        $scope.accessSave = function () {
            var ace = [],
                hasRights = false,
                rulesValid = false;

            angular.forEach($scope.policies, function (policy) {
                rulesValid = false;

                if (policy.read && policy.write) {
                    hasRights = true;
                }

                if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
                    rulesValid = true;

                    if (policy.read) {
                        ace.push({
                            'action' : 'read',
                            'allow'  : policy.read,
                            'role'   : policy.role
                        });
                    }

                    if (policy.write) {
                        ace.push({
                            'action' : 'write',
                            'allow'  : policy.write,
                            'role'   : policy.role
                        });
                    }

                    angular.forEach(policy.actions.value, function(customAction){
                           ace.push({
                                'action' : customAction,
                                'allow'  : true,
                                'role'   : policy.role
                           });
                    });
                }
            });

            me.unvalidRule = !rulesValid;
            me.hasRights = hasRights;

            if (me.unvalidRule) {
                if (!angular.isUndefined(me.notificationRules)) {
                    Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                }
                me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRules)) {
                Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                me.notificationRules = undefined;
            }

            if (!me.hasRights) {
                if (!angular.isUndefined(me.notificationRights)) {
                    Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                }
                me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRights)) {
                Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                me.notificationRights = undefined;
            }

            if (hasRights && rulesValid) {
                EventAccessResource.save({id: $scope.resourceId}, {
                    acl: {
                        ace: ace
                    },
                    override: true
                }, me.accessSaved, me.accessNotSaved);
            }
        };

        $scope.severityColor = function (severity) {
            switch (severity.toUpperCase()) {
                case 'FAILURE':
                    return 'red';
                case 'INFO':
                    return 'green';
                case 'WARNING':
                    return 'yellow';
            }
        };

        $scope.deleteComment = function (id) {
            CommentResource.delete(
                { resource: 'event', resourceId: $scope.resourceId, id: id, type: 'comment' },
                function () {
                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }
            );
        };

        $scope.deleteCommentReply = function (commentId, reply) {
            CommentResource.delete(
                { resource: 'event', resourceId: $scope.resourceId, type: 'comment', id: commentId, reply: reply },
                function () {
                    $scope.comments = CommentResource.query({
                        resource: 'event',
                        resourceId: $scope.resourceId,
                        type: 'comments'
                    });
                }
            );
        };

        checkForActiveTransactions();

        $scope.workflowAction = function (wfId, action) {
            if ($scope.workflowActionInProgress) return;
            $scope.workflowActionInProgress = true;
            EventWorkflowActionResource.save({id: $scope.resourceId, wfId: wfId, action: action}, function () {
                Notifications.add('success', 'EVENTS_PROCESSING_ACTION_' + action);
                $scope.close();
                $scope.workflowActionInProgress = false;
            }, function () {
                Notifications.add('error', 'EVENTS_PROCESSING_ACTION_NOT_' + action, NOTIFICATION_CONTEXT);
                $scope.workflowActionInProgress = false;
            });
        };

        $scope.deleteWorkflow = function (workflowId) {
            if ($scope.deleteWorkflowInProgress) return;
            $scope.deleteWorkflowInProgress = true;
            EventWorkflowDetailsResource.delete({ id0: $scope.resourceId, id1: workflowId },
                function () {
                    Notifications.add('success', 'EVENTS_PROCESSING_DELETE_WORKFLOW', NOTIFICATION_CONTEXT);

                    // We update our client-side model in case of success, so we don't have to send a new request
                    if ($scope.workflows.entries) {
                        $scope.workflows.entries = $scope.workflows.entries.filter(function (wf) {
                            return wf.id !== workflowId;
                        });
                    }

                    $scope.deleteWorkflowInProgress = false;
                }, function () {
                    Notifications.add('error', 'EVENTS_PROCESSING_DELETE_WORKFLOW_FAILED', NOTIFICATION_CONTEXT);
                    $scope.deleteWorkflowInProgress = false;
             });
        };

        $scope.isCurrentWorkflow = function (workflowId) {
            var currentWorkflow = $scope.workflows.entries[$scope.workflows.entries.length -1];
            return currentWorkflow.id === workflowId;
        }

        $scope.$on('$destroy', function () {
            cleanupScopeResources();
        });
    }
]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('EditStatusCtrl', ['$scope', 'Modal', 'Table', 'OptoutsResource', 'Notifications',
    'decorateWithTableRowSelection',
    function ($scope, Modal, Table, OptoutsResource, Notifications, decorateWithTableRowSelection) {

    $scope.rows = Table.copySelected();
    $scope.allSelected = true; // by default, all records are selected

    $scope.changeStatus = function (newStatus) {
        $scope.status = newStatus;
        if (angular.isDefined($scope.status)) {
            $scope.status += '';            
        }
    };

    $scope.valid = function () {
        return $scope.TableForm.$valid && angular.isDefined($scope.status);
    };

    $scope.submit = function () {
        var resource = Table.resource.indexOf('series') >= 0 ? 'series' : 'event',
            sourceNotification = resource === 'series' ? 'SERIES' : 'EVENTS'; 
        if ($scope.valid()) {
            OptoutsResource.save({
                resource: resource,
                eventIds: $scope.getSelectedIds(),
                optout: $scope.status === 'true'
            }, function (data) {
                var nbErrors = data.error ? data.error.length : 0,
                    nbOK = data.ok ? data.ok.length : 0,
                    nbNotFound = data.notFound ? data.notFound.length : 0,
                    nbBadRequest = data.badRequest ? data.badRequest.length : 0;
                Table.deselectAll();
                Modal.$scope.close();
                if (nbErrors === 0 && nbBadRequest === 0 && nbNotFound === 0) {
                    Notifications.add('success', sourceNotification + '_UPDATED_ALL');
                } else {
                    if (nbOK > 0) {
                        Notifications.addWithParams('success', sourceNotification + '_UPDATED_NB', {number : nbOK});
                    }

                    var errors = [];

                    if (data.error) {
                        errors = errors.concat(data.err);
                    }

                    if (data.notFound) {
                        errors = errors.concat(data.notFound);
                    }                    

                    if (data.badRequest) {
                        errors = errors.concat(data.badRequest);
                    }                    

                    if (data.forbidden) {
                        errors = errors.concat(data.forbidden);
                    }                 

                    angular.forEach(errors, function (error) {
                        Notifications.addWithParams('error', sourceNotification + '_NOT_UPDATED_ID', {id: error});
                    });

                }
            }, function () {
                Modal.$scope.close();
                Notifications.add('error', sourceNotification + '_NOT_UPDATED_ALL');
            });
        }
    };
    decorateWithTableRowSelection($scope);
}]);

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

// Controller for creating a new event. This is a wizard, so it implements a state machine design pattern.
angular.module('adminNg.controllers')
.controller('NewEventCtrl', ['$scope', '$timeout', 'Table', 'NewEventStates', 'NewEventResource', 'EVENT_TAB_CHANGE', 'Notifications', 'Modal',
 function ($scope, $timeout, Table, NewEventStates, NewEventResource, EVENT_TAB_CHANGE, Notifications, Modal) {
    $scope.states = NewEventStates.get();
    // This is a hack, due to the fact that we need to read html from the server :(
    // Shall be banished ASAP

    var metadata,
        accessController,
        // Reset all the wizard states
        resetStates = function () {
            angular.forEach($scope.states, function(state)  {
                if (angular.isDefined(state.stateController.reset)) {
                    state.stateController.reset({resetDefaults: true});
                }
            });
        };

    angular.forEach($scope.states, function (state) {
        if (state.stateController.isAccessState) {
            accessController = state.stateController;
        } else if (state.stateController.isMetadataState) {
            metadata = state.stateController;
        }
    });

    if (angular.isDefined(metadata) && angular.isDefined(accessController)) {
        accessController.setMetadata(metadata);
    }

    $scope.$on(EVENT_TAB_CHANGE, function (event, args) {
        if (args.old !== args.current && args.old.stateController.isProcessingState) {
            args.old.stateController.save();
        }

        if (args.current.stateController.isAccessState) {
            args.current.stateController.loadSeriesAcl();
        }

        if (args.current.stateController.isSourceState) {
            if (!args.current.stateController.defaultsSet) {
              args.current.stateController.loadCaptureAgents();
              args.current.stateController.setDefaultsIfNeeded();
            }
        }
    });

    $scope.submit = function () {
        var messageId, userdata = { metadata: []}, ace = [];

        window.onbeforeunload = function (e) {
            var confirmationMessage = 'The file has not completed uploading.';

            (e || window.event).returnValue = confirmationMessage;     //Gecko + IE
            return confirmationMessage;                                //Webkit, Safari, Chrome etc.
        };

        angular.forEach($scope.states, function (state) {

            if (state.stateController.isMetadataState) {
                for (var o in state.stateController.ud) {
                    if (state.stateController.ud.hasOwnProperty(o)) {
                        userdata.metadata.push(state.stateController.ud[o]);
                    }
                }
            } else if (state.stateController.isMetadataExtendedState) {
                for (var o in state.stateController.ud) {
                    if (state.stateController.ud.hasOwnProperty(o)) {
                        userdata.metadata.push(state.stateController.ud[o]);
                    }
                }
            } else if (state.stateController.isAccessState) {
                angular.forEach(state.stateController.ud.policies, function (policy) {
                    if (angular.isDefined(policy.role)) {
                        if (policy.read) {
                            ace.push({
                                'action' : 'read',
                                'allow'  : policy.read,
                                'role'   : policy.role
                            });
                        }

                        if (policy.write) {
                            ace.push({
                                'action' : 'write',
                                'allow'  : policy.write,
                                'role'   : policy.role
                            });
                        }
                    }

                    angular.forEach(policy.actions.value, function(customAction){
                      ace.push({
                        'action' : customAction,
                        'allow'  : true,
                        'role'   : policy.role
                      });
                    });
                });

                userdata.access = {
                    acl: {
                        ace: ace
                    }
                };
            } else {
                userdata[state.name] = state.stateController.ud;
            }
        });

        NewEventResource.save({}, userdata, function () {
            $timeout(function () {
                Table.fetch();
            }, 500);

            Notifications.add('success', 'EVENTS_CREATED');
            Notifications.remove(messageId);
            resetStates();
            window.onbeforeunload = null;
        }, function () {
            Notifications.add('error', 'EVENTS_NOT_CREATED');
            Notifications.remove(messageId);
            resetStates();
            window.onbeforeunload = null;
        });

        Modal.$scope.close();
        // add message that never disappears
        messageId = Notifications.add('success', 'EVENTS_UPLOAD_STARTED', 'global', -1);
    };

    $scope.close = function () {
        resetStates();
        Modal.$scope.close();
    }
}]);

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

// Controller for creating a new event. This is a wizard, so it implements a state machine design pattern.
angular.module('adminNg.controllers')
.controller('NewSeriesCtrl', ['$scope', 'NewSeriesStates', 'SeriesResource', 'ResourcesListResource', 'Notifications', 'Modal', 'Table',
        function ($scope, NewSeriesStates, SeriesResource, ResourcesListResource, Notifications, Modal, Table) {
    $scope.states = NewSeriesStates.get();

    function pushAllPropertiesIntoArray(object, array) {
        for (var o in object) {
            if (object.hasOwnProperty(o)) {
                array.push(object[o]);
            }
        }
    }

    // Post all the information collect by the wizard to create the new series
    $scope.submit = function () {
        var userdata, metadata = [], options = {}, access, theme, ace;
        // assemble the metadata from the two metadata controllers
        pushAllPropertiesIntoArray($scope.states[0].stateController.ud, metadata);
        if ($scope.states[1].name === 'metadata-extended') {
            pushAllPropertiesIntoArray($scope.states[1].stateController.ud, metadata);
        }

        // assemble the access
        if ($scope.states[1].name === 'access') {
            access = $scope.states[1].stateController.ud;
        } else if ($scope.states[2].name === 'access') {
            access = $scope.states[2].stateController.ud;
        }

        ace = [];
        angular.forEach(access.policies, function (policy) {
            if (angular.isDefined(policy.role)) {
                if (policy.read) {
                    ace.push({
                        'action' : 'read',
                        'allow'  : policy.read,
                        'role'   : policy.role
                    });
                }

                if (policy.write) {
                    ace.push({
                        'action' : 'write',
                        'allow'  : policy.write,
                        'role'   : policy.role
                    });
                }

                angular.forEach(policy.actions.value, function(customAction){
                    ace.push({
                        'action' : customAction,
                        'allow'  : true,
                        'role'   : policy.role
                    });
                });
            }

        });

        userdata = {
                metadata: metadata,
                options:  options,
                access: {
                    acl: {
                        ace: ace
                    }
                }
        };

        // lastly, assemble the theme
        if ($scope.states[2].name === 'theme') {
            theme = $scope.states[2].stateController.ud.theme;
        }
        else if($scope.states[3].name === 'theme'){
            theme = $scope.states[3].stateController.ud.theme;
        }
        if (angular.isDefined(theme) && theme !== null && !angular.isObject(theme)) {
            userdata.theme = Number(theme);
        }

        // Disable submit button to avoid multiple submits
        $scope.states[$scope.states.length - 1].stateController.isDisabled = true;
        SeriesResource.create({}, userdata, function () {
            Table.fetch();
            Notifications.add('success', 'SERIES_ADDED');

            // Reset all states
            angular.forEach($scope.states, function(state)  {
                if (angular.isDefined(state.stateController.reset)) {
                    state.stateController.reset();
                }
            });

            Modal.$scope.close();
            // Ok, request is done. Enable submit button.
            $scope.states[$scope.states.length - 1].stateController.isDisabled = false;
        }, function () {
            Notifications.add('error', 'SERIES_NOT_SAVED', 'series-form');
            // Ok, request failed. Enable submit button.
            $scope.states[$scope.states.length - 1].stateController.isDisabled = false;
        });
    };

    // Reload tab resource on tab changes
    $scope.$parent.$watch('tab', function (value) {
        angular.forEach($scope.states, function (state) {
            if (value === state.name && !angular.isUndefined(state.stateController.reload)) {
                state.stateController.reload();
            }
        });
    });

    $scope.components = ResourcesListResource.get({ resource: 'components' });
}]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('SeriesCtrl', ['$scope', 'Table', 'SeriesResource', 'ResourcesFilterResource', 'Notifications',
    function ($scope, Table, SeriesResource, ResourcesFilterResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                template: 'modules/events/partials/seriesTitleCell.html',
                name:  'title',
                label: 'EVENTS.SERIES.TABLE.TITLE'
            }, {
                name:  'creator',
                label: 'EVENTS.SERIES.TABLE.CREATORS'
            }, {
                name:  'contributors',
                label: 'EVENTS.SERIES.TABLE.CONTRIBUTORS'
            }, {
            	name:  'createdDateTime',
            	label: 'EVENTS.SERIES.TABLE.CREATED'
            }, {
                template: 'modules/events/partials/seriesActionsCell.html',
                label:    'EVENTS.SERIES.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'EVENTS.SERIES.TABLE.CAPTION',
            resource: 'series',
            category: 'events',
            apiService: SeriesResource,
            multiSelect: true
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            SeriesResource.delete({id: row.id}, function () {
                Table.fetch();
                Notifications.add('success', 'SERIES_DELETED');
            }, function () {
                Notifications.add('error', 'SERIES_NOT_DELETED');
            });
        };
    }
]);

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

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('SerieCtrl', ['$scope', 'SeriesMetadataResource', 'SeriesEventsResource', 'SeriesAccessResource', 'SeriesThemeResource', 'ResourcesListResource', 'UserRolesResource',
        'Notifications', 'OptoutSingleResource', 'SeriesParticipationResource',
        function ($scope, SeriesMetadataResource, SeriesEventsResource, SeriesAccessResource, SeriesThemeResource, ResourcesListResource, UserRolesResource, Notifications,
            OptoutSingleResource, SeriesParticipationResource) {

    var roleSlice = 100;
    var roleOffset = 0;
    var loading = false;
    var rolePromise = null;

    var saveFns = {}, aclNotification,
        me = this,
        NOTIFICATION_CONTEXT = 'series-acl',
        mainCatalog = 'dublincore/series', fetchChildResources,
        createPolicy = function (role) {
            return {
                role  : role,
                read  : false,
                write : false,
                actions : {
                    name : 'series-acl-actions',
                    value : []
                }
            };
        },
        changePolicies = function (access, loading) {
            var newPolicies = {};
            angular.forEach(access, function (acl) {
                var policy = newPolicies[acl.role];

                if (angular.isUndefined(policy)) {
                    newPolicies[acl.role] = createPolicy(acl.role);
                }
                if (acl.action === 'read' || acl.action === 'write') {
                    newPolicies[acl.role][acl.action] = acl.allow;
                } else if (acl.allow === true || acl.allow === 'true') {
                    newPolicies[acl.role].actions.value.push(acl.action);
                }
            });

            $scope.policies = [];
            angular.forEach(newPolicies, function (policy) {
                $scope.policies.push(policy);
            });

            if (!loading) {
                $scope.accessSave();
            }
        };

    $scope.aclLocked = false,
    $scope.policies = [];
    $scope.baseAcl = {};

    $scope.changeBaseAcl = function () {
        $scope.baseAcl = SeriesAccessResource.getManagedAcl({id: this.baseAclId}, function () {
            changePolicies($scope.baseAcl.acl.ace);
        });
        this.baseAclId = '';
    };

    $scope.addPolicy = function () {
        $scope.policies.push(createPolicy());
    };

    $scope.deletePolicy = function (policyToDelete) {
        var index;

        angular.forEach($scope.policies, function (policy, idx) {
            if (policy.role === policyToDelete.role &&
                policy.write === policyToDelete.write &&
                policy.read === policyToDelete.read) {
                index = idx;
            }
        });

        if (angular.isDefined(index)) {
            $scope.policies.splice(index, 1);
        }

        $scope.accessSave();
    };

    $scope.updateOptout = function (newBoolean) {

        OptoutSingleResource.save({
            resource: 'series',
            id: $scope.resourceId,
            optout: newBoolean
        }, function () {
            Notifications.add('success', 'SERIES_PARTICIPATION_STATUS_UPDATE_SUCCESS', 'series-participation');
        }, function () {
            Notifications.add('error', 'SERIES_PARTICIPATION_STATUS_UPDATE_ERROR', 'series-participation');
        });
    };

    $scope.getMoreRoles = function (value) {

        if (loading)
            return rolePromise;

        loading = true;
        var queryParams = {limit: roleSlice, offset: roleOffset};

        if ( angular.isDefined(value) && (value != "")) {
            //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
            //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
            queryParams["filter"] = "role_name:"+ value +",role_target:ACL";
            queryParams["offset"] = 0;
        } else {
            queryParams["filter"] = "role_target:ACL";
        }
        rolePromise = UserRolesResource.query(queryParams);
        rolePromise.$promise.then(function (data) {
            angular.forEach(data, function (role) {
                $scope.roles[role.name] = role.value;
            });
            roleOffset = Object.keys($scope.roles).length;
        }).finally(function () {
            loading = false;
        });
        return rolePromise;
    };

    fetchChildResources = function (id) {
        $scope.metadata = SeriesMetadataResource.get({ id: id }, function (metadata) {
            var seriesCatalogIndex, keepGoing = true;
            angular.forEach(metadata.entries, function (catalog, index) {
                if (catalog.flavor === mainCatalog) {
                    $scope.seriesCatalog = catalog;
                    seriesCatalogIndex = index;
                    var tabindex = 2;
                    angular.forEach(catalog.fields, function (entry) {
                        if (entry.id === 'title' && angular.isString(entry.value)) {
                            $scope.titleParams = { resourceId: entry.value.substring(0,70) };
                        }
                        if (keepGoing && entry.locked) {
                            metadata.locked = entry.locked;
                            keepGoing = false;
                        }
                        entry.tabindex = tabindex++;
                    });
                }
            });

            if (angular.isDefined(seriesCatalogIndex)) {
                metadata.entries.splice(seriesCatalogIndex, 1);
            }
        });

        $scope.roles = {};

        $scope.access = SeriesAccessResource.get({ id: id }, function (data) {
            if (angular.isDefined(data.series_access)) {
                var json = angular.fromJson(data.series_access.acl);
                changePolicies(json.acl.ace, true);

                $scope.aclLocked = data.series_access.locked;

                if ($scope.aclLocked) {
                    aclNotification = Notifications.add('warning', 'SERIES_ACL_LOCKED', 'series-acl-' + id, -1);
                } else if (aclNotification) {
                    Notifications.remove(aclNotification, 'series-acl');
                }
                angular.forEach(data.series_access.privileges, function(value, key) {
                    if (angular.isUndefined($scope.roles[key])) {
                        $scope.roles[key] = key;
                    }
                });
            }
        });

        $scope.participation = SeriesParticipationResource.get({ id: id });
        $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
        $scope.actions = {};
        $scope.hasActions = false;
        ResourcesListResource.get({ resource: 'ACL.ACTIONS' }, function(data) {
            angular.forEach(data, function (value, key) {
                if (key.charAt(0) !== '$') {
                    $scope.actions[key] = value;
                    $scope.hasActions = true;
                }
            });
        });

        $scope.selectedTheme = {};

        $scope.updateSelectedThemeDescripton = function () {
            if(angular.isDefined($scope.themeDescriptions)) {
                $scope.selectedTheme.description = $scope.themeDescriptions[$scope.selectedTheme.id];
            }
        };

        ResourcesListResource.get({ resource: 'THEMES.NAME' }, function (data) {
            $scope.themes = data;

            //after themes have been loaded we match the current selected
            SeriesThemeResource.get({ id: id }, function (response) {

                //we want to get rid of $resolved, etc. - therefore we use toJSON()
                angular.forEach(data.toJSON(), function (value, key) {

                    if (angular.isDefined(response[key])) {
                        $scope.selectedTheme.id = key;
                        return false;
                    }
                });

                ResourcesListResource.get({ resource: 'THEMES.DESCRIPTION' }, function (data) {
                    $scope.themeDescriptions = data;
                    $scope.updateSelectedThemeDescripton();
                });
            });
        });

        $scope.getMoreRoles();
    };

      // Generate proxy function for the save metadata function based on the given flavor
      // Do not generate it
    $scope.getSaveFunction = function (flavor) {
        var fn = saveFns[flavor],
            catalog;

        if (angular.isUndefined(fn)) {
            if ($scope.seriesCatalog.flavor === flavor) {
                catalog = $scope.seriesCatalog;
            } else {
                angular.forEach($scope.metadata.entries, function (c) {
                    if (flavor === c.flavor) {
                        catalog = c;
                    }
                });
            }

            fn = function (id, callback) {
                $scope.metadataSave(id, callback, catalog);
            };

            saveFns[flavor] = fn;
        }
        return fn;
    };

    $scope.replyToId = null; // the id of the comment to which the user wants to reply

    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
        fetchChildResources(id);
    });

    $scope.metadataSave = function (id, callback, catalog) {
        catalog.attributeToSend = id;

        SeriesMetadataResource.save({ id: $scope.resourceId }, catalog,  function () {
            if (angular.isDefined(callback)) {
                callback();
            }

            // Mark the saved attribute as saved
            angular.forEach(catalog.fields, function (entry) {
                if (entry.id === id) {
                    entry.saved = true;
                }
            });
        });
    };

    $scope.accessChanged = function (role) {
      if (!role) return;
      $scope.accessSave();
    };

    $scope.accessSave = function () {
            var ace = [],
                hasRights = false,
                rulesValid = false;

            angular.forEach($scope.policies, function (policy) {
                rulesValid = false;

                if (policy.read && policy.write) {
                    hasRights = true;
                }

                if ((policy.read || policy.write || policy.actions.value.length > 0) && !angular.isUndefined(policy.role)) {
                    rulesValid = true;

                    if (policy.read) {
                        ace.push({
                            'action' : 'read',
                            'allow'  : policy.read,
                            'role'   : policy.role
                        });
                    }

                    if (policy.write) {
                        ace.push({
                            'action' : 'write',
                            'allow'  : policy.write,
                            'role'   : policy.role
                        });
                    }

                    angular.forEach(policy.actions.value, function(customAction) {
                           ace.push({
                                'action' : customAction,
                                'allow'  : true,
                                'role'   : policy.role
                           });
                    });
                }
            });

            me.unvalidRule = !rulesValid;
            me.hasRights = hasRights;

            if (me.unvalidRule) {
                if (!angular.isUndefined(me.notificationRules)) {
                    Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                }
                me.notificationRules = Notifications.add('warning', 'INVALID_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRules)) {
                Notifications.remove(me.notificationRules, NOTIFICATION_CONTEXT);
                me.notificationRules = undefined;
            }

            if (!me.hasRights) {
                if (!angular.isUndefined(me.notificationRights)) {
                    Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                }
                me.notificationRights = Notifications.add('warning', 'MISSING_ACL_RULES', NOTIFICATION_CONTEXT);
            } else if (!angular.isUndefined(me.notificationRights)) {
                Notifications.remove(me.notificationRights, NOTIFICATION_CONTEXT);
                me.notificationRights = undefined;
            }

            if (hasRights && rulesValid) {
                SeriesAccessResource.save({ id: $scope.resourceId }, {
                    acl: {
                        ace: ace
                    },
                    override: true
                });

                Notifications.add('info', 'SAVED_ACL_RULES', NOTIFICATION_CONTEXT, 1200);
            }
    };

    // Reload tab resource on tab changes
    $scope.$parent.$watch('tab', function (value) {
      switch (value) {
        case 'permissions':
            $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
            $scope.getMoreRoles();
          break;
      }
    });

    $scope.themeSave = function () {
        var selectedThemeID = $scope.selectedTheme.id;
        $scope.updateSelectedThemeDescripton();

        if (angular.isUndefined(selectedThemeID) || selectedThemeID === null) {
            SeriesThemeResource.delete({ id: $scope.resourceId }, { theme: selectedThemeID }, function () {
                Notifications.add('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
            });
        } else {
            SeriesThemeResource.save({ id: $scope.resourceId }, { theme: selectedThemeID }, function () {
                Notifications.add('warning', 'SERIES_THEME_REPROCESS_EXISTING_EVENTS', 'series-theme');
            });
        }
    };

}]);

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

angular.module('adminNg.controllers')
.controller('BulkMessageCtrl', ['$scope', 'BulkMessageStates', 'EmailResource', 'Table', 'Notifications', 'Modal', 'EVENT_TAB_CHANGE',
function ($scope, BulkMessageStates, EmailResource, Table, Notifications, Modal, EVENT_TAB_CHANGE) {
    BulkMessageStates.reset();
    $scope.states = BulkMessageStates.get();

    // Render the preview only when the summary tab has been reached.
    $scope.$on(EVENT_TAB_CHANGE, function (event, args) {
        if (args.current.name === 'summary') {
            $scope.states[1].stateController.updatePreview();
        }
    });

    $scope.submit = function () {
        var userdata = {};
        angular.forEach($scope.states, function (state) {
            userdata[state.name] = state.stateController.ud;
        });

        EmailResource.save({ templateId: userdata.message.email_template.id }, userdata, function () {
            Table.fetch();
            Table.deselectAll();
            Notifications.add('success', 'EMAIL_SENT');
            Modal.$scope.close();
        }, function () {
            Notifications.add('error', 'EMAIL_NOT_SENT', 'bulk-message-form');
        });
    };
}]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('BulkDeleteCtrl', ['$scope', 'Modal', 'FormNavigatorService', 'Table', 'Notifications',
    'BulkDeleteResource', 'NewEventProcessing', 'TaskResource', 'decorateWithTableRowSelection',
        function ($scope, Modal, FormNavigatorService, Table, Notifications, BulkDeleteResource, NewEventProcessing,
                  TaskResource, decorateWithTableRowSelection) {

    var hasPublishedElements = function (currentEvent) {
        var publicationCount = 0;
        angular.forEach(currentEvent.publications, function() {
            publicationCount++;
        });

        if (publicationCount > 0) {
            return true;
        } else {
            return false;
        }
    },
    tableByPublicationStatus = function(isPublished) {
        var result = {
            allSelected: true,
            rows: (function () {
                var result = [];
                angular.forEach($scope.rows, function (row){
                    if (hasPublishedElements(row) === isPublished && row.selected) {
                        result.push(row);
                    }
                });
                return result;
            })()
        };
        decorateWithTableRowSelection(result);
        return result;
    },
    getSelected = function (rows) {
        var result = [];
        angular.forEach(rows, function(row) {
            if (row.selected) {
                result.push(row);
            }
        });
        return result;
    },
    countSelected = function (rows) {
        return getSelected(rows).length;
    };
    $scope.rows = Table.copySelected();
    $scope.allSelected = true; // by default, all records are selected
    $scope.currentForm = 'deleteForm'; // By default start on the delete form
    $scope.processing = NewEventProcessing.get('delete-event');
    $scope.published = tableByPublicationStatus(true);
    $scope.unpublished = tableByPublicationStatus(false);
    $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
        $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
    };

    var getSelectedSeriesIds = function () {
        var result = [];
        angular.forEach($scope.rows, function (row) {
            if (row.selected) {
                result.push(row.id);
            }
        });
        return result;
    };

    $scope.valid = function () {
        var selectedCount;
        if (Table.resource.indexOf('series') >= 0) {
            selectedCount = 0;
            angular.forEach($scope.rows, function (row) {
                if (row.selected) {
                    selectedCount++;
                }
            });
            return selectedCount > 0;
        } else {
            selectedCount = countSelected($scope.unpublished.rows) + countSelected($scope.published.rows);
            if (countSelected($scope.published.rows) > 0) {
                return angular.isDefined($scope.processing.ud.workflow.id) && selectedCount > 0;
            } else {
                return selectedCount > 0;
            }
        }
    };

    $scope.submitButton = false;
    $scope.submit = function () {
        if ($scope.valid()) {
            $scope.submitButton = true;
            var resetSubmitButton = true,
            deleteIds = [],
            resource = Table.resource.indexOf('series') >= 0 ? 'series' : 'event',
            endpoint = Table.resource.indexOf('series') >= 0 ? 'deleteSeries' : 'deleteEvents',
            sourceNotification = resource === 'series' ? 'SERIES' : 'EVENTS';

            if (Table.resource.indexOf('series') >= 0) {
                deleteIds = getSelectedSeriesIds();
            } else {
                angular.forEach(getSelected($scope.unpublished.rows), function (row) {
                    deleteIds.push(row.id);
                });
            }
            if (deleteIds.length > 0) {
                resetSubmitButton = false;
                BulkDeleteResource.delete({}, {
                    resource: resource,
                    endpoint: endpoint,
                    eventIds: deleteIds
                }, function () {
                    $scope.submitButton = false;
                    Table.deselectAll();
                    Notifications.add('success', sourceNotification + '_DELETED');
                    Modal.$scope.close();
                }, function () {
                    $scope.submitButton = false;
                    Notifications.add('error', sourceNotification + '_NOT_DELETED');
                    Modal.$scope.close();
                });
            }
            if (Table.resource.indexOf('series') < 0 && countSelected($scope.published.rows) > 0) {
                var retractEventIds = [], payload;
                angular.forEach($scope.getPublishedEvents(), function (row) {
                    retractEventIds.push(row.id);
                });
                if (retractEventIds.length > 0) {
                    resetSubmitButton = false;
                    payload = {
                        workflow: $scope.processing.ud.workflow.id,
                        configuration: $scope.processing.ud.workflow.selection.configuration,
                        eventIds: retractEventIds
                    };
                    TaskResource.save(payload, $scope.onSuccess, $scope.onFailure);
                }
            }
            if (resetSubmitButton) {
                // in this case, no callback would ever set submitButton to false again
                $scope.submitButton = false;
            }
        }

    };

    $scope.onSuccess = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('success', 'TASK_CREATED');
    };

    $scope.onFailure = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('error', 'TASK_NOT_CREATED', 'global', -1);
    };

    $scope.getPublishedEvents = function () {
        return getSelected($scope.published.rows);
    };

    $scope.getUnpublishedEvents = function () {
        return getSelected($scope.unpublished.rows);
    };

    $scope.toggleAllUnpublishedEvents = function () {
        angular.forEach($scope.rows, function (row) {
            if (!$scope.hasPublishedElements(row)) {
                row.selected = $scope.events.unpublished.selected;
            }
        });
    };

    if (Table.resource.indexOf('series') < 0) {
        $scope.events = {};
        $scope.events.published = {};
        $scope.events.published.has = $scope.published.rows.length > 0;
        $scope.events.published.selected = true;
        $scope.events.unpublished = {};
        $scope.events.unpublished.has = $scope.unpublished.rows.length > 0;
        $scope.events.unpublished.selected = true;
    }
}]);

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

// Controller for retracting a single event that has been published
angular.module('adminNg.controllers')
.controller('RetractEventCtrl', ['$scope', 'NewEventProcessing', 'TaskResource', 'Notifications',
function ($scope, NewEventProcessing, TaskResource, Notifications) {
    var onSuccess, onFailure;

    $scope.currentForm = 'generalForm';
    $scope.processing = NewEventProcessing.get('delete-event');
    $scope.$valid = false;
    $scope.valid = function () {
        $scope.$valid = angular.isDefined($scope.processing.ud.workflow.id);
    };

    $scope.getSubmitButtonState = function() {
      return ($scope.$valid && !$scope.submitButton) ? 'active' : 'disabled';
    };

    onSuccess = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('success', 'TASK_CREATED');
    };

    onFailure = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('error', 'TASK_NOT_CREATED', 'global', -1);
    };

    $scope.submitButton = false;
    $scope.submit = function () {
        $scope.submitButton = true;
        var eventIds = [], payload;
        eventIds.push($scope.$parent.resourceId);
        payload = {
            workflow: $scope.processing.ud.workflow.id,
            configuration: $scope.processing.ud.workflow.selection.configuration,
            eventIds: eventIds
        };
        TaskResource.save(payload, onSuccess, onFailure);
    };
}]);

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

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('ScheduleTaskCtrl', ['$scope', 'Table', 'NewEventProcessing', 'TaskResource',
    'Notifications', 'decorateWithTableRowSelection',
function ($scope, Table, NewEventProcessing, TaskResource, Notifications, decorateWithTableRowSelection) {
    $scope.rows = Table.copySelected();
    $scope.allSelected = true; // by default, all rows are selected
    $scope.test = false;
    $scope.currentForm = 'generalForm';
    $scope.processing = NewEventProcessing.get('tasks');

    $scope.valid = function () {
        return $scope.getSelectedIds().length > 0;
    };

    var onSuccess = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('success', 'TASK_CREATED');
        Table.deselectAll();
    };

    var onFailure = function () {
        $scope.submitButton = false;
        $scope.close();
        Notifications.add('error', 'TASK_NOT_CREATED', 'global', -1);
    };

    $scope.submitButton = false;
    $scope.submit = function () {
        $scope.submitButton = true;
        if ($scope.valid()) {
            var eventIds = $scope.getSelectedIds(), payload;
            payload = {
                workflow: $scope.processing.ud.workflow.id,
                configuration: $scope.processing.getWorkflowConfig(),
                eventIds: eventIds
            };
            TaskResource.save(payload, onSuccess, onFailure);
        }
    };
    decorateWithTableRowSelection($scope);
}]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('ToolsCtrl', ['$scope', '$route', '$location', '$window', 'ToolsResource', 'Notifications', 'EventHelperService',
    function ($scope, $route, $location, $window, ToolsResource, Notifications, EventHelperService) {

        $scope.navigateTo = function (path) {
            $location.path(path).replace();
        };

        $scope.event    = EventHelperService;
        $scope.resource = $route.current.params.resource;
        $scope.tab      = $route.current.params.tab;
        if ($scope.tab === "editor") {
          $scope.area   = "segments";
        } else if ($scope.tab === "playback") {
          $scope.area   = "metadata";
        }
        $scope.id       = $route.current.params.itemId;

        $scope.event.eventId = $scope.id;

        $scope.unsavedChanges = false;

        $scope.setChanges = function(changed) {
            $scope.unsavedChanges = changed;
        };

        $scope.openTab = function (tab) {
            $scope.tab = tab;
            if ($scope.tab === "editor") {
              $scope.area   = "segments";
            } else if ($scope.tab === "playback") {
              $scope.area   = "metadata";
            }

            // This fixes a problem where video playback breaks after switching tabs. Changing the location seems
            // to be destructive to the <video> element working together with opencast's external controls.
            var lastRoute, off;
            lastRoute = $route.current;
            off = $scope.$on('$locationChangeSuccess', function () {
                $route.current = lastRoute;
                off();
            });

            $scope.navigateTo('/events/' + $scope.resource + '/' + $scope.id + '/tools/' + tab);
        };

        $scope.openArea = function (area) {
            $scope.area = area;
        };

        // TODO Move the following to a VideoCtrl
        $scope.player = {};
        $scope.video  = ToolsResource.get({ id: $scope.id, tool: 'editor' });

        $scope.submitButton = false;
        $scope.submit = function () {
            $scope.submitButton = true;
            $scope.video.$save({ id: $scope.id, tool: $scope.tab }, function () {
                $scope.submitButton = false;
                if ($scope.video.workflow) {
                    Notifications.add('success', 'VIDEO_CUT_PROCESSING');
                    $location.url('/events/' + $scope.resource);
                } else {
                    Notifications.add('success', 'VIDEO_CUT_SAVED');
                }
                $scope.unsavedChanges = false;
            }, function () {
                $scope.submitButton = false;
                Notifications.add('error', 'VIDEO_CUT_NOT_SAVED', 'video-tools');
            });
        };
    }
]);

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

// Controller for all event screens.
angular.module('adminNg.controllers')
.controller('VideoEditCtrl', ['$scope', '$translate', 'PlayerAdapter', 'VideoService', 'HotkeysService', 'Notifications',
    function ($scope, $translate, PlayerAdapter, VideoService, HotkeysService, Notifications) {

        var NOTIFICATION_CONTEXT = 'video-editor-event-access';
        var notificationId = 0;

        $scope.split = function () {
            var segment = VideoService.getCurrentSegment($scope.player, $scope.video),
                position = Math.floor($scope.player.adapter.getCurrentTime() * 1000),
                newSegment = angular.copy(segment);

            // Shrink original segment
            segment.end = position;

            // Add additional segment containing the second half of the
            // original segment.
            newSegment.start = position;

            // Deselect the previous segment as the cursor is at the start
            // of the new one.
            delete segment.selected;

            // Insert new segment
            $scope.video.segments.push(newSegment);

            // Sort array by start attribute
            $scope.video.segments.sort(function (a, b) {
                return a.start - b.start;
            });

            // Notify segmentsDirective about changed segment times
            $scope.$root.$broadcast("segmentTimesUpdated");
        };

        $scope.clearSelectedSegment = function () {
            
            angular.forEach($scope.video.segments, function (segment) {
                if (segment.selected) {
                    
                    var index = $scope.video.segments.indexOf(segment);

                    if ($scope.video.segments[index - 1]) {
                        $scope.video.segments[index - 1].end = segment.end;
                        $scope.video.segments.splice(index, 1);
                    } else if ($scope.video.segments[index + 1]) {
                        $scope.video.segments[index + 1].start = segment.start;
                        $scope.video.segments.splice(index, 1);
                    }
                }
            });
        };

        $scope.clearSegments = function () {
            $scope.video.segments.splice(1, $scope.video.segments.length - 1);
            $scope.video.segments[0].end = $scope.video.duration;
        };

        $scope.cut = function () {
            angular.forEach($scope.video.segments, function (segment) {
                if (segment.selected) {
                    segment.deleted = !segment.deleted;
                }
            });
        };

        $scope.replay = function () {
            var segment = VideoService.getCurrentSegment($scope.player, $scope.video);
            segment.replay = true;
            $scope.player.adapter.setCurrentTime(segment.start/1000);
            if ($scope.player.adapter.getStatus() !== PlayerAdapter.STATUS.PLAYING) {
                $scope.player.adapter.play();
            }
        };

        $scope.replayEndOfSegment = function () {
            var segment = VideoService.getCurrentSegment($scope.player, $scope.video);
            segment.replay = true;
            $scope.player.adapter.setCurrentTime((segment.end/1000) - 2);
            if ($scope.player.adapter.getStatus() !== PlayerAdapter.STATUS.PLAYING) {
                $scope.player.adapter.play();
            }
        };

        $scope.replayPreRoll = function () {
            var segment = VideoService.getPreviousActiveSegment($scope.player, $scope.video);
            var currentSegment = VideoService.getCurrentSegment($scope.player, $scope.video);
            currentSegment.replay = true;
            segment.replay = true;
            $scope.player.adapter.setCurrentTime((segment.end/1000) - 2);
            if ($scope.player.adapter.getStatus() !== PlayerAdapter.STATUS.PLAYING) {
                $scope.player.adapter.play();
            }
        };

        HotkeysService.activateHotkey($scope, "editor.split_at_current_time",
          "split the video at current time", function(event) {
              event.preventDefault();
              $scope.split();
        });

        HotkeysService.activateHotkey($scope, "editor.cut_selected_segment",
          "remove current segment", function(event) {
              event.preventDefault();
              $scope.cut();
        });

        HotkeysService.activateHotkey($scope, "editor.play_current_segment",
          "play current segment", function(event) {
              event.preventDefault();
              $scope.replay();
        });

        HotkeysService.activateHotkey($scope, "editor.clear_list",
          "clear segment list", function(event) {
              event.preventDefault();
              $scope.clearSegments();
        });

        HotkeysService.activateHotkey($scope, "editor.play_current_segment_with_pre-roll",
          "Play current segment with pre-roll", function(event) {
              event.preventDefault();
              $scope.replayPreRoll();
        });

        HotkeysService.activateHotkey($scope, "editor.play_ending_of_current_segment",
          "Play end of segment", function(event) {
              event.preventDefault();
              $scope.replayEndOfSegment();
        });

        $scope.$on('ACTIVE_TRANSACTION', function() {
            if (!$scope.submitButton) {
                $scope.submitButton = true;
                notificationId = Notifications.add('warning', 'ACTIVE_TRANSACTION', NOTIFICATION_CONTEXT, -1);
            }
        });

        $scope.$on('NO_ACTIVE_TRANSACTION', function() {
            if ($scope.submitButton) {
                $scope.submitButton = false;
                if (notificationId) {
                    Notifications.remove(notificationId, NOTIFICATION_CONTEXT);
                    notificationId = 0;
                }
            }
        });

        // This shows a confirmation dialog when the user leaves the editor while he has unsaved changes
        $scope.onUnload = function(event) {
            if (!$scope.unsavedChanges) return undefined;
            var answer = confirm(window.unloadConfirmMsg);
            if (!answer) {
               event.preventDefault();
            }
            event.returnValue = window.unloadConfirmMsg;
            return window.unloadConfirmMsg;
        };

        // register listeners to show confirmation dialog when user leaves editor with unsaved changes
        window.addEventListener('beforeunload', $scope.onUnload);
        $scope.$on('$locationChangeStart', function(event) {
            $scope.onUnload(event)
        });
        $scope.$on('$destroy', function() {
            window.removeEventListener('beforeunload', $scope.onUnload);
        });

        $translate('VIDEO_TOOL.WARNING_UNSAVED').then(function(translation) {
            window.unloadConfirmMsg = translation;
        });
    }
]);

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
 * @name adminNg.modules.events.validators.taskStartable
 * @description
 * Checks if the given value equals ARCHIVE.
 *
 */
angular.module('adminNg.directives')
.directive('taskStartable', ['Notifications', function (Notifications) {
    var link = function (scope, elm, attrs, ctrl) {
        scope, elm, attrs, ctrl, Notifications;
        ctrl.$validators.taskStartable = function (modelValue, viewValue) {
            if (viewValue) {
                if (angular.isDefined(attrs.taskStartable) &&
                    attrs.taskStartable.toUpperCase().indexOf('ARCHIVE') === 0) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return true;
            }
        };
    };

    return {
        require: 'ngModel',
        link: link
    };
}]);

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
 * @name adminNg.modules.events.validators.taskStartableValidator
 * @description
 * Checks if the chosen selection is valid. We would have expected ng-required to suffice, but with this chosen component, it doesn't.
 *
 */
angular.module('adminNg.directives')
.directive('notEmptySelection', ['Notifications', function (Notifications) {
    var link = function (scope, elm, attrs, ctrl) {
        scope, elm, attrs, ctrl, Notifications;
        var $scope = scope;
        ctrl.$validators.notEmptySelection = function () {
            var workflow = $scope.processing.ud.workflow;
            if (angular.isDefined(workflow)) {
                if (angular.isObject(workflow.selection)) {
                    return angular.isDefined(workflow.selection.id) && workflow.selection.id.length > 0;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        };
    };

    return {
        require: 'ngModel',
        link: link
    };
}]);

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

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('RecordingCtrl', ['$scope', 'CaptureAgentResource',
    function ($scope, CaptureAgentResource ) {

      var fetchResources = function(id) {
        $scope.agent = CaptureAgentResource.get({name: id});
      };

      $scope.$on('change', function (event, id) {
        fetchResources(id);
      });

     fetchResources($scope.resourceId);
}]);

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

// Controller for all recordings screens.
angular.module('adminNg.controllers')
.controller('RecordingsCtrl', ['$scope', 'Table', 'CaptureAgentsResource', 'ResourcesFilterResource', 'Notifications', 'Modal',
    function ($scope, Table, CaptureAgentsResource, ResourcesFilterResource, Notifications, Modal) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'status',
                template: 'modules/recordings/partials/recordingStatusCell.html',
                label: 'RECORDINGS.RECORDINGS.TABLE.STATUS'
            }, {
                template: 'modules/recordings/partials/recordingsNameCell.html',
                name:  'name',
                label: 'RECORDINGS.RECORDINGS.TABLE.NAME'
            }, {
                name:  'updated',
                label: 'RECORDINGS.RECORDINGS.TABLE.UPDATED'
            //}, {
            //    name:  'blacklist_from',
            //    label: 'USERS.USERS.TABLE.BLACKLIST_FROM'
            //}, {
            //    name:  'blacklist_to',
            //    label: 'USERS.USERS.TABLE.BLACKLIST_TO'
            }, {
                template: 'modules/recordings/partials/recordingActionsCell.html',
                label:    'RECORDINGS.RECORDINGS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'RECORDINGS.RECORDINGS.TABLE.CAPTION',
            resource:   'recordings',
            category:   'recordings',
            apiService: CaptureAgentsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            CaptureAgentsResource.delete({target: row.name}, function () {
                Table.fetch();
                Modal.$scope.close();
                Notifications.add('success', 'LOCATION_DELETED');
            }, function () {
                Notifications.add('error', 'LOCATION_NOT_DELETED');
            });
        };
    }
]);

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

// Controller for all recording blacklists.
angular.module('adminNg.controllers')
.controller('LocationblacklistsCtrl', ['$scope', 'Table', 'Notifications', 'LocationBlacklistsResource', 'ResourcesFilterResource',
    function ($scope, Table, Notifications, LocationBlacklistsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'resourceName',
                label: 'USERS.BLACKLISTS.TABLE.NAME'
            }, {
                name:  'date_from',
                label: 'USERS.BLACKLISTS.TABLE.DATE_FROM'
            }, {
                name:  'date_to',
                label: 'USERS.BLACKLISTS.TABLE.DATE_TO'
            }, {
                name:  'reason',
                label: 'USERS.BLACKLISTS.TABLE.REASON',
                translate: true
            }, {
                template: 'modules/recordings/partials/locationblacklistActionsCell.html',
                label:    'USERS.BLACKLISTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'RECORDINGS.BLACKLISTS.TABLE.CAPTION',
            resource:   'locationblacklists',
            category:   'recordings',
            apiService: LocationBlacklistsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            row.$delete({ id: row.id }, function () {
                Table.fetch();
                Notifications.add('success', 'LOCATION_BLACKLIST_DELETED');
            }, function () {
                Notifications.add('error', 'LOCATION_BLACKLIST_NOT_DELETED');
            });
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('LocationblacklistCtrl', ['$scope', 'NewLocationblacklistStates', 'LocationBlacklistsResource', 'Notifications', 'Modal', 'Table',
        function ($scope, NewLocationblacklistStates, LocationBlacklistsResource, Notifications, Modal, Table) {
    $scope.states = NewLocationblacklistStates.get();
    NewLocationblacklistStates.reset();

    // Populate user data if the blacklist is being edited
    if ($scope.action === 'edit') {
        LocationBlacklistsResource.get({ id: Modal.$scope.resourceId }, function (blacklist) {
            // Populate items step
            $scope.states[0].stateController.ud.items = [{
                id: blacklist.resourceId,
                name: blacklist.resourceName
            }];

            // Populate dates step
            var fromDateTime = new Date(blacklist.date_from_raw),
                toDateTime   = new Date(blacklist.date_to_raw);
            $scope.states[1].stateController.ud.fromDate =
                fromDateTime.toISOString().split('T')[0];
            $scope.states[1].stateController.ud.fromTime =
                fromDateTime.toISOString().split('T')[1].slice(0,5);
            $scope.states[1].stateController.ud.toDate =
                toDateTime.toISOString().split('T')[0];
            $scope.states[1].stateController.ud.toTime =
                toDateTime.toISOString().split('T')[1].slice(0,5);

            // Populate reason step
            $scope.states[2].stateController.ud.reason  = blacklist.reason;
            $scope.states[2].stateController.ud.comment = blacklist.comment;
        });
    }

    $scope.submit = function () {
        var userdata = {};
        angular.forEach($scope.states, function (state) {
            userdata[state.name] = state.stateController.ud;
        });

        if ($scope.action === 'edit') {
            LocationBlacklistsResource.update({ id: Modal.$scope.resourceId }, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'LOCATION_BLACKLIST_SAVED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'LOCATION_BLACKLIST_NOT_SAVED', 'blacklist-form');
            });
        } else {
            LocationBlacklistsResource.save({}, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'LOCATION_BLACKLIST_CREATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'LOCATION_BLACKLIST_NOT_CREATED', 'blacklist-form');
            });
        }
    };
}]);

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

angular.module('adminNg.controllers')
.controller('UsersCtrl', ['$scope', 'Table', 'UsersResource', 'UserResource', 'ResourcesFilterResource', 'Notifications', 'Modal',
    function ($scope, Table, UsersResource, UserResource, ResourcesFilterResource, Notifications, Modal) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'USERS.USERS.TABLE.NAME'
            }, {
                name:  'username',
                label: 'USERS.USERS.TABLE.USERNAME'
            }, {
                name:  'email',
                label: 'USERS.USERS.TABLE.EMAIL'
            }, {
                name:  'roles',
                label: 'USERS.USERS.TABLE.ROLES'
            }, {
                name:  'provider',
                label: 'USERS.USERS.TABLE.PROVIDER'
            }, {
            //     name:  'blacklist_from',
            //     label: 'USERS.USERS.TABLE.BLACKLIST_FROM'
            // }, {
            //     name:  'blacklist_to',
            //     label: 'USERS.USERS.TABLE.BLACKLIST_TO'
            // }, {
               template: 'modules/users/partials/userActionsCell.html',
               label:    'USERS.USERS.TABLE.ACTION',
               dontSort: true
            }],
            caption:    'USERS.USERS.TABLE.CAPTION',
            resource:   'users',
            category:   'users',
            apiService: UsersResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            UserResource.delete({username: row.id}, function () {
                Table.fetch();
                Modal.$scope.close();
                Notifications.add('success', 'USER_DELETED');
            }, function () {
                Notifications.add('error', 'USER_NOT_DELETED');
            });
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('UserCtrl', ['$scope', 'Table', 'UserRolesResource', 'UserResource', 'UsersResource', 'JsHelper', 'Notifications', 'Modal', 'AuthService', 'underscore',
    function ($scope, Table, UserRolesResource, UserResource, UsersResource, JsHelper, Notifications, Modal, AuthService, _) {
        $scope.manageable = true;
        var roleSlice = 100;
        var roleOffset = roleSlice; //Note that the initial offset is the same size as the initial slice so that the *next* slice starts at the right place
        var loading = false;
        var showExternalRoles = false; // Should the External Roles tab be visible

        var EXTERNAL_ROLE_DISPLAY = 'adminui.user.external_role_display';

        AuthService.getUser().$promise.then(function(user) {
            $scope.currentUser = user;

            if (angular.isDefined(user.org.properties[EXTERNAL_ROLE_DISPLAY])) {
                $scope.showExternalRoles = user.org.properties[EXTERNAL_ROLE_DISPLAY] === "true";
            }
        });

        $scope.role = {
            available: UserRolesResource.query({limit: 0, offset: 0, filter: 'role_target:USER'}), // Load all the internal roles
            external: [],
            selected:  [],
            derived: [],
            i18n: 'USERS.USERS.DETAILS.ROLES',
            searchable: true
        };

        $scope.searchFieldExternal = '';
        $scope.searchFieldEffective = '';

        $scope.getMoreRoles = function() {
            if (loading)
                return;

            loading = true;
            UserRolesResource.query({limit: $scope.roleSlice, offset: $scope.roleOffset, filter: 'role_target:USER'}).$promise.then(function (data) {
                angular.extend($scope.role.available, data);
                roleOffset = roleOffset + roleSlice;
            }, this).finally(function () {
                loading = false;
            });
        };

        $scope.groupSort = function(role) {

            var result = 10;

            switch (role.type) {
                case "SYSTEM":      result = 0; break;
                case "GROUP":       result = 1; break;
                case "EXTERNAL_GROUP":    result = 2; break;
                case "INTERNAL":    result = 3; break;
                case "DERIVED":     result = 4; break;
                case "EXTERNAL":    result = 5; break;

                default: result = 10; break;
            }

           return result;
        };

        $scope.clearSearchFieldExternal = function () {
            $scope.searchFieldExternal = '';
        }

        $scope.clearSearchFieldEffective = function () {
            $scope.searchFieldEffective = '';
        }

        $scope.customEffectiveFilter = function () {

            return function (item) {

                var result = true; //(item ? !(item.name.substring(0, ('ROLE_USER_').length) === 'ROLE_USER_') : true);
                if (result && ($scope.searchField != '')) {

                    result = (item.name.toLowerCase().indexOf($scope.searchFieldEffective.toLowerCase()) >= 0);
                }

                return result;
            };
        };

        if ($scope.action === 'edit') {
            $scope.caption = 'USERS.USERS.DETAILS.EDITCAPTION';
            $scope.user = UserResource.get({ username: $scope.resourceId });
            $scope.user.$promise.then(function () {
                $scope.manageable = $scope.user.manageable;
                if (!$scope.manageable) {
                    Notifications.add('warning', 'USER_NOT_MANAGEABLE', 'user-form');
                }

                $scope.role.available.$promise.then(function() {

                    // Now that we have the user roles and the available roles populate the selected and available
                    angular.forEach($scope.user.roles, function (role) {

                        if (role.type == "INTERNAL" || role.type == "GROUP") {
                            $scope.role.selected.push({name: role.name, value: role.name, type: role.type});
                        }

                        if (role.type == "EXTERNAL" || role.type == "EXTERNAL_GROUP") {
                            $scope.role.external.push({name: role.name, value: role.name, type: role.type});
                        }

                        if (role.type == "SYSTEM" || role.type == "DERIVED") {
                            $scope.role.derived.push({name: role.name, value: role.name, type: role.type});
                        }
                    });

                    // Filter the selected from the available list
                    $scope.role.available = _.filter($scope.role.available, function(role){ return !_.findWhere($scope.role.selected, {name: role.name}); });
                });
            });
        }
        else {
            $scope.caption = 'USERS.USERS.DETAILS.NEWCAPTION';
        }


        $scope.submit = function () {
            $scope.user.roles = [];

            angular.forEach($scope.role.selected, function (value) {
              $scope.user.roles.push({"id": value.value, "type": value.type});
            });

            if ($scope.action === 'edit') {
                $scope.user.$update({ username: $scope.user.username }, function () {
                    Notifications.add('success', 'USER_UPDATED');
                    Modal.$scope.close();
                }, function () {
                    Notifications.add('error', 'USER_NOT_SAVED', 'user-form');
                });
            } else {
                UsersResource.create({ }, $scope.user, function () {
                    Table.fetch();
                    Modal.$scope.close();
                    Notifications.add('success', 'USER_ADDED');
                    Modal.$scope.close();
                }, function () {
                    Notifications.add('error', 'USER_NOT_SAVED', 'user-form');
                });
            }
        };

        $scope.getHeight = function () {

            return { height : $scope.role.searchable ? '26em' : '30em' };
        }

        $scope.getSubmitButtonState = function () {
          return $scope.userForm.$valid && $scope.manageable ? 'active' : 'disabled';
        };

        // Retrieve a list of user so the form can be validated for user
        // uniqueness.
        $scope.users = [];
        UsersResource.query(function (users) {
            $scope.users = JsHelper.map(users.rows, 'username');
        });

        $scope.checkUserUniqueness = function () {
            $scope.userForm.username.$setValidity('uniqueness',
                    $scope.users.indexOf($scope.user.username) > -1 ? false:true);
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('GroupsCtrl', ['$scope', 'Table', 'Modal', 'GroupsResource', 'GroupResource', 'ResourcesFilterResource', 'Notifications',
    function ($scope, Table, Modal, GroupsResource, GroupResource, ResourcesFilterResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'USERS.GROUPS.TABLE.NAME'
            }, {
                name:  'description',
                label: 'USERS.GROUPS.TABLE.DESCRIPTION'
            }, {
                name:  'role',
                label: 'USERS.GROUPS.TABLE.ROLE'
            }, {
               template: 'modules/users/partials/groupActionsCell.html',
               label:    'USERS.USERS.TABLE.ACTION',
               dontSort: true
            }],
            caption:    'USERS.GROUPS.TABLE.CAPTION',
            resource:   'groups',
            category:   'users',
            apiService: GroupsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
          GroupResource.delete({id: row.id}, function () {
              Table.fetch();
              Modal.$scope.close();
              Notifications.add('success', 'GROUP_DELETED');
          }, function () {
              Notifications.add('error', 'GROUP_NOT_DELETED');
          });
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('GroupCtrl', ['$scope', 'AuthService', 'UserRolesResource', 'ResourcesListResource', 'GroupResource', 'GroupsResource', 'Notifications', 'Modal',
    function ($scope, AuthService, UserRolesResource, ResourcesListResource, GroupResource, GroupsResources, Notifications, Modal) {

        var reloadSelectedUsers = function () {
            $scope.group.$promise.then(function() {
                $scope.user.all.$promise.then(function() {
                    // Now that we have the user users and the group users populate the selected and available
                    $scope.user.selected = $scope.user.all.filter(function (user) {
                        var foundUser = $scope.group.users.find(function (groupUser) {
                            return groupUser.username === user.value;
                        });
                        return foundUser !== undefined;
                    });
                    $scope.user.available = $scope.user.all.filter(function (user) {
                        var foundUser = $scope.user.selected.find(function (selectedUser) {
                            return selectedUser.value === user.value;
                        });
                        return foundUser === undefined;
                    });
                });
            });
        };

        var reloadSelectedRoles = function () {
            $scope.group.$promise.then(function() {
                $scope.role.available.$promise.then(function() {
                    // Now that we have the user roles and the available roles populate the selected and available
                    $scope.role.selected = [];
                    angular.forEach($scope.group.roles, function (role) {
                        $scope.role.selected.push({name: role, value: role});
                    });
                    // Filter the selected from the available list
                    $scope.role.available = _.filter($scope.role.available, function(role) {
                        return !_.findWhere($scope.role.selected, {name: role.name});
                    });
                });
            });
        };

        var reloadRoles = function () {
          $scope.role = {
              available: UserRolesResource.query({limit: 0, offset: 0, filter: 'role_target:USER'}),
              selected:  [],
              i18n: 'USERS.GROUPS.DETAILS.ROLES',
              searchable: true
          };
          reloadSelectedRoles();
        };

        var reloadUsers = function (current_user) {
          $scope.orgProperties = {};
          if (angular.isDefined(current_user) && angular.isDefined(current_user.org) && angular.isDefined(current_user.org.properties)) {
               $scope.orgProperties = current_user.org.properties;
          }
          $scope.user = {
              all: ResourcesListResource.query({ resource: $scope.orgProperties['adminui.user.listname'] || 'USERS.INVERSE.WITH.USERNAME'}),
              available: [],
              selected:  [],
              i18n: 'USERS.GROUPS.DETAILS.USERS',
              searchable: true
          };
          reloadSelectedUsers();
        };

        if ($scope.action === 'edit') {
            $scope.caption = 'USERS.GROUPS.DETAILS.EDITCAPTION';
            $scope.group = GroupResource.get({ id: $scope.resourceId }, function () {
                reloadSelectedRoles();
                reloadSelectedUsers();
            });
        } else {
            $scope.caption = 'USERS.GROUPS.DETAILS.NEWCAPTION';
        }

        $scope.submit = function () {
            $scope.group.users = [];
            $scope.group.roles = [];

            angular.forEach($scope.user.selected, function (item) {
              $scope.group.users.push(item.value);
            });

            angular.forEach($scope.role.selected, function (item) {
              $scope.group.roles.push(item.name);
            });

          if ($scope.action === 'edit') {
            GroupResource.save({ id: $scope.group.id }, $scope.group, function () {
                Notifications.add('success', 'GROUP_UPDATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
            });
          } else {
            GroupsResources.create($scope.group, function () {
                Notifications.add('success', 'GROUP_ADDED');
                Modal.$scope.close();
            }, function (response) {
                if(response.status === 409) {
                    Notifications.add('error', 'GROUP_CONFLICT', 'group-form');
                } else {
                    Notifications.add('error', 'GROUP_NOT_SAVED', 'group-form');
                }
            });
          }
        };

        $scope.getSubmitButtonState = function () {
          return $scope.groupForm.$valid ? 'active' : 'disabled';
        };

        reloadRoles();
        AuthService.getUser().$promise.then(function(current_user) {
          reloadUsers(current_user);
        });
    }
]);

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

angular.module('adminNg.controllers')
.controller('NewGroupCtrl', ['$scope', '$timeout', 'Table', 'NewGroupStates', 'ResourcesListResource', 'GroupsResource', 'Notifications', 'Modal',
    function ($scope, $timeout, Table, NewGroupStates, ResourcesListResource, GroupsResource, Notifications, Modal) {

        $scope.states = NewGroupStates.get();

        $scope.submit = function () {
          $scope.group = {
            users       : [],
            roles       : [],
            name        : $scope.states[0].stateController.metadata.name,
            description : $scope.states[0].stateController.metadata.description
          };

          angular.forEach($scope.states[2].stateController.users.selected, function (value) {
            $scope.group.users.push(value.value);
          });

          angular.forEach($scope.states[1].stateController.roles.selected, function (value) {
            $scope.group.roles.push(value.name);
          });

          GroupsResource.create($scope.group, function () {
              // Fetching immediately does not work
              $timeout(function () {
                  Table.fetch();
              }, 500);
              Modal.$scope.close();

              // Reset all states
              angular.forEach($scope.states, function(state)  {
                if (angular.isDefined(state.stateController.reset)) {
                    state.stateController.reset();
                }
              });

              Notifications.add('success', 'GROUP_ADDED');

          }, function (response) {
        	  if(response.status === 409) {
                  Notifications.add('error', 'GROUP_CONFLICT', 'add-group-form');
              } else {
                  Notifications.add('error', 'GROUP_NOT_SAVED', 'add-group-form');
              }
          });

        };

        // Reload tab resource on tab changes
        $scope.$parent.$watch('tab', function (value) {
          angular.forEach($scope.states, function (state) {
            if (value === state.name && !angular.isUndefined(state.stateController.reload)) {
              state.stateController.reload();
            }
          });
        });
    }
]);

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

angular.module('adminNg.controllers')
.controller('NewAclCtrl', ['$scope', 'Table', 'NewAclStates', 'ResourcesListResource', 'AclsResource', 'Notifications', 'Modal',
    function ($scope, Table, NewAclStates, ResourcesListResource, AclsResource, Notifications, Modal) {

        $scope.states = NewAclStates.get();

        $scope.submit = function () {
          var access = $scope.states[1].stateController.ud,
              ace = [];

          angular.forEach(access.policies, function (policy) {
              if (angular.isDefined(policy.role)) {
                  if (policy.read) {
                      ace.push({
                          'action' : 'read',
                          'allow'  : policy.read,
                          'role'   : policy.role
                      });
                  }

                  if (policy.write) {
                      ace.push({
                          'action' : 'write',
                          'allow'  : policy.write,
                          'role'   : policy.role
                      });
                  }

                  angular.forEach(policy.actions.value, function(customAction){
                      ace.push({
                          'action' : customAction,
                          'allow'  : true,
                          'role'   : policy.role
                      });
                  });
              }

          });

          $scope.acl = {
            name : $scope.states[0].stateController.metadata.name,
            acl  : {
              ace: ace
            }
          };

          AclsResource.create($scope.acl, function () {
              Table.fetch();
              Modal.$scope.close();

              // Reset all states
              angular.forEach($scope.states, function(state)  {
                  if (angular.isDefined(state.stateController.reset)) {
                      state.stateController.reset();
                  }
              });

              Notifications.add('success', 'ACL_ADDED');
          }, function () {
              Notifications.add('error', 'ACL_NOT_SAVED', 'acl-form');
          });

        };
    }
]);

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

// Controller for all single series screens.
angular.module('adminNg.controllers')
.controller('AclCtrl', ['$scope', 'AclResource', 'UserRolesResource', 'ResourcesListResource', 'Notifications',
        function ($scope, AclResource, UserRolesResource, ResourcesListResource, Notifications) {
    var roleSlice = 100;
    var roleOffset = 0;
    var loading = false;
    var rolePromise = null;

    var createPolicy = function (role) {
            return {
                role  : role,
                read  : false,
                write : false,
                actions : {
                    name : 'edit-acl-actions',
                    value : []
                }
            };
        },
        fetchChildResources,
        changePolicies = function (access, loading) {
            var newPolicies = {};
            angular.forEach(access, function (acl) {
                var policy = newPolicies[acl.role];

                if (angular.isUndefined(policy)) {
                    newPolicies[acl.role] = createPolicy(acl.role);
                }
                if (acl.action === 'read' || acl.action === 'write') {
                    newPolicies[acl.role][acl.action] = acl.allow;
                } else if (acl.allow === true || acl.allow === 'true'){
                    newPolicies[acl.role].actions.value.push(acl.action);
                }
            });

            $scope.policies = [];
            angular.forEach(newPolicies, function (policy) {
                $scope.policies.push(policy);
            });

            if (!loading) {
                $scope.save();
            }
        };

    $scope.policies = [];
    $scope.baseAcl = {};
    $scope.metadata = {};

    $scope.changeBaseAcl = function () {
        $scope.baseAcl = AclResource.get({id: this.baseAclId}, function () {
            changePolicies($scope.baseAcl.acl.ace);
        });
        this.baseAclId = '';
    };

    $scope.addPolicy = function () {
        $scope.policies.push(createPolicy());
    };

    $scope.deletePolicy = function (policyToDelete) {
        var index;

        angular.forEach($scope.policies, function (policy, idx) {
            if (policy.role === policyToDelete.role &&
                policy.write === policyToDelete.write &&
                policy.read === policyToDelete.read) {
                index = idx;
            }
        });

        if (angular.isDefined(index)) {
            $scope.policies.splice(index, 1);
        }

        $scope.save();
    };

    $scope.getMoreRoles = function (value) {

        if (loading)
            return rolePromise;

        loading = true;
        var queryParams = {limit: roleSlice, offset: roleOffset};

        if ( angular.isDefined(value) && (value != "")) {
            //Magic values here.  Filter is from ListProvidersEndpoint, role_name is from RolesListProvider
            //The filter format is care of ListProvidersEndpoint, which gets it from EndpointUtil
            queryParams["filter"] = "role_name:"+ value +",role_target:ACL";
            queryParams["offset"] = 0;
        } else {
            queryParams["filter"] = "role_target:ACL";
        }
        rolePromise = UserRolesResource.query(queryParams);
        rolePromise.$promise.then(function (data) {
            angular.forEach(data, function (role) {
                $scope.roles[role.name] = role.value;
            });
            roleOffset = Object.keys($scope.roles).length;
        }).finally(function () {
            loading = false;
        });
        return rolePromise;
    };

    fetchChildResources = function (id) {
        //NB: roles is updated in both the functions for $scope.acl (MH-11716) and $scope.roles (MH-11715, MH-11717)
        $scope.roles = {};
        $scope.acl = AclResource.get({id: id}, function (data) {
            $scope.metadata.name = data.name;

            if (angular.isDefined(data.acl)) {
                var json = angular.fromJson(data.acl);
                changePolicies(json.ace, true);
            }

            angular.forEach(angular.fromJson(data.acl.ace), function(value, key) {
                var rolename = value["role"];
                if (angular.isUndefined($scope.roles[rolename])) {
                    $scope.roles[rolename] = rolename;
                }
            }, this);
        });

        $scope.acls  = ResourcesListResource.get({ resource: 'ACL' });
        $scope.actions = {};
        $scope.hasActions = false;
        ResourcesListResource.get({ resource: 'ACL.ACTIONS'}, function(data) {
            angular.forEach(data, function (value, key) {
                if (key.charAt(0) !== '$') {
                    $scope.actions[key] = value;
                    $scope.hasActions = true;
                }
            });
        });
        $scope.getMoreRoles();
    };


    fetchChildResources($scope.resourceId);

    $scope.$on('change', function (event, id) {
        fetchChildResources(id);
    });

    $scope.save = function (field) {
        var ace = [];

        if (angular.isDefined(field) && angular.isUndefined(field.role)) {
            return;
        }

        angular.forEach($scope.policies, function (policy) {
            if (angular.isDefined(policy.role)) {
                if (policy.read) {
                    ace.push({
                        'action' : 'read',
                        'allow'  : policy.read,
                        'role'   : policy.role
                    });
                }

                if (policy.write) {
                    ace.push({
                        'action' : 'write',
                        'allow'  : policy.write,
                        'role'   : policy.role
                    });
                }

                angular.forEach(policy.actions.value, function(customAction){
                    ace.push({
                        'action' : customAction,
                        'allow'  : true,
                        'role'   : policy.role
                   });
                });
            }

        });

        AclResource.save({id: $scope.resourceId}, {
            acl: {
                ace: ace
            },
            name: $scope.metadata.name
        }, function () {
            Notifications.add('success', 'ACL_UPDATED', 'acl-form');
        }, function () {
            Notifications.add('error', 'ACL_NOT_SAVED', 'acl-form');
        });
    };
}]);

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

angular.module('adminNg.controllers')
.controller('AclsCtrl', ['$scope', 'Table', 'AclsResource', 'AclResource', 'ResourcesFilterResource', 'Notifications',
    function ($scope, Table, AclsResource, AclResource, ResourcesFilterResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'USERS.ACLS.TABLE.NAME'
            //}, {
            //    name:  'created',
            //    label: 'USERS.ACLS.TABLE.CREATED'
            //}, {
            //    name:  'creator',
            //    label: 'USERS.ACLS.TABLE.CREATOR'
            //}, {
            //    name:  'in_use',
            //    label: 'USERS.ACLS.TABLE.IN_USE'
            }, {
               template: 'modules/users/partials/aclActionsCell.html',
               label:    'USERS.ACLS.TABLE.ACTION',
               dontSort: true
            }],
            caption:    'USERS.ACLS.TABLE.CAPTION',
            resource:   'acls',
            category:   'users',
            apiService: AclsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            AclResource.delete({id: row.id}, function () {
                Table.fetch();
                Notifications.add('success', 'ACL_DELETED');
            }, function () {
                Notifications.add('error', 'ACL_NOT_DELETED');
            });
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('UserblacklistsCtrl', ['$scope', 'Table', 'Notifications', 'UserBlacklistsResource', 'ResourcesFilterResource',
    function ($scope, Table, Notifications, UserBlacklistsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'resourceName',
                label: 'USERS.BLACKLISTS.TABLE.NAME'
            }, {
                name:  'date_from',
                label: 'USERS.BLACKLISTS.TABLE.DATE_FROM',
                dontSort: true
            }, {
                name:  'date_to',
                label: 'USERS.BLACKLISTS.TABLE.DATE_TO',
                dontSort: true
            }, {
                name:  'reason',
                label: 'USERS.BLACKLISTS.TABLE.REASON',
                translate: true
            }, {
                template: 'modules/users/partials/userblacklistActionsCell.html',
                label:    'USERS.BLACKLISTS.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'USERS.BLACKLISTS.TABLE.CAPTION',
            resource:   'userblacklists',
            category:   'users',
            apiService: UserBlacklistsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            row.$delete({ id: row.id }, function () {
                Table.fetch();
                Notifications.add('success', 'USER_BLACKLIST_DELETED');
            }, function () {
                Notifications.add('error', 'USER_BLACKLIST_NOT_DELETED');
            });
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('UserblacklistCtrl', ['$scope', 'NewUserblacklistStates', 'UserBlacklistsResource', 'Notifications', 'Modal', 'Table',
        function ($scope, NewUserblacklistStates, UserBlacklistsResource, Notifications, Modal, Table) {
    $scope.states = NewUserblacklistStates.get();
    NewUserblacklistStates.reset();

    // Populate user data if the blacklist is being edited
    if ($scope.action === 'edit') {
        UserBlacklistsResource.get({ id: Modal.$scope.resourceId }, function (blacklist) {
            // Populate items step
            $scope.states[0].stateController.ud.items = [{
                id: blacklist.resourceId,
                name: blacklist.resourceName
            }];

            // Populate dates step
            var fromDateTime = new Date(blacklist.date_from_raw),
                toDateTime   = new Date(blacklist.date_to_raw);
            $scope.states[1].stateController.ud.fromDate =
                fromDateTime.toISOString().split('T')[0];
            $scope.states[1].stateController.ud.fromTime =
                fromDateTime.toISOString().split('T')[1].slice(0,5);
            $scope.states[1].stateController.ud.toDate =
                toDateTime.toISOString().split('T')[0];
            $scope.states[1].stateController.ud.toTime =
                toDateTime.toISOString().split('T')[1].slice(0,5);

            // Populate reason step
            $scope.states[2].stateController.ud.reason  = blacklist.reason;
            $scope.states[2].stateController.ud.comment = blacklist.comment;
        });
    }

    $scope.submit = function () {
        var userdata = {};
        angular.forEach($scope.states, function (state) {
            userdata[state.name] = state.stateController.ud;
        });

        if ($scope.action === 'edit') {
            UserBlacklistsResource.update({ id: Modal.$scope.resourceId }, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'USER_BLACKLIST_SAVED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'USER_BLACKLIST_NOT_SAVED', 'blacklist-form');
            });
        } else {
            UserBlacklistsResource.save({}, userdata, function () {
                Table.fetch();
                Notifications.add('success', 'USER_BLACKLIST_CREATED');
                Modal.$scope.close();
            }, function () {
                Notifications.add('error', 'USER_BLACKLIST_NOT_CREATED', 'blacklist-form');
            });
        }
    };
}]);

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
 * Controller for the login screen.
 *
 * The actual authentication is handled by the login form submit which
 * POSTs to `j_spring_security_check`. If the request succeeds, the
 * response will cause a redirect to the application. The URL for the
 * redirect is not configured in this Opencast module.
 *
 * If the request fails, the response will redirect to the current
 * page but add a `error` search parameter (without a value).
 */
angular.module('adminNg.controllers')
.controller('LoginCtrl', ['$scope', '$location', 'VersionResource', 'Language', '$rootScope',
        function ($scope, $location, VersionResource, Language, $rootScope) {

            $scope.isError = false;

            $scope.changeLanguage = function (language) {
                Language.changeLanguage(language);
            };


            // If authentication fails, the `error` search parameter will be
            // set.
            if ($location.absUrl().match(/\?error$/)) {
                $scope.isError = true;
            } else {
                VersionResource.query(function(response){
                    $scope.version = response.version ? response : (angular.isArray(response.versions) ? response.versions[0]:{});
                    if (!response.consistent) {
                        $scope.version.buildNumber = 'inconsistent';
                    }
                });
            }

            $rootScope.$on('language-changed', function () {
                $scope.currentLanguageCode = Language.getLanguageCode();
                $scope.currentLanguageName = Language.getLanguage().displayLanguage;
                $scope.availableLanguages = Language.getAvailableLanguages();
            });

            // For the logout please check the navigationController.
        }]);

angular.module('adminNg.controllers')
.controller('ServersCtrl', ['$scope', 'Table', 'ServersResource', 'ServiceResource', 'ResourcesFilterResource',
    function ($scope, Table, ServersResource, ServiceResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:     'online',
                template: 'modules/systems/partials/serverStatusCell.html',
                label:    'SYSTEMS.SERVERS.TABLE.STATUS'
            }, {
                name:  'hostname',
                label: 'SYSTEMS.SERVERS.TABLE.HOST_NAME'
            }, {
                name:  'cores',
                label: 'SYSTEMS.SERVERS.TABLE.CORES'
            }, {
                name:  'completed',
                label: 'SYSTEMS.SERVERS.TABLE.COMPLETED'
            }, {
                name:  'running',
                label: 'SYSTEMS.SERVERS.TABLE.RUNNING'
            }, {
                name:  'queued',
                label: 'SYSTEMS.SERVERS.TABLE.QUEUED'
            }, {
                name:  'meanRunTime',
                label: 'SYSTEMS.SERVERS.TABLE.MEAN_RUN_TIME'
            }, {
                name:  'meanQueueTime',
                label: 'SYSTEMS.SERVERS.TABLE.MEAN_QUEUE_TIME'
            }, {
                name:     'maintenance',
                template: 'modules/systems/partials/serverMaintenanceCell.html',
                label:    'SYSTEMS.SERVERS.TABLE.MAINTENANCE'
            //}, {
            //    template: 'modules/systems/partials/serverActionsCell.html',
            //    label:    'SYSTEMS.SERVERS.TABLE.ACTION',
            //    dontSort: true
            }],
            caption:    'SYSTEMS.SERVERS.TABLE.CAPTION',
            resource:   'servers',
            category:   'systems',
            apiService: ServersResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.setMaintenanceMode = function (host, maintenance) {
            ServiceResource.setMaintenanceMode({
                host: host,
                maintenance: maintenance
            }, function () {
                $scope.table.fetch();
            });
        };
    }
]);

angular.module('adminNg.controllers')
.controller('JobsCtrl', ['$scope', 'Table', 'JobsResource', 'ResourcesFilterResource',
    function ($scope, Table, JobsResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'id',
                label: 'SYSTEMS.JOBS.TABLE.ID'
            }, {
                name:  'status',
                label: 'SYSTEMS.JOBS.TABLE.STATUS'
            }, {
                name:  'operation',
                label: 'SYSTEMS.JOBS.TABLE.OPERATION'
            }, {
                name:  'type',
                label: 'SYSTEMS.JOBS.TABLE.TYPE'
            }, {
                name:  'processingHost',
                label: 'SYSTEMS.JOBS.TABLE.HOST_NAME'
            }, {
                name:  'submitted',
                label: 'SYSTEMS.JOBS.TABLE.SUBMITTED'
            }, {
                name:  'started',
                label: 'SYSTEMS.JOBS.TABLE.STARTED'
            }, {
                name:  'creator',
                label: 'SYSTEMS.JOBS.TABLE.CREATOR'
            //}, {
            //    template: 'modules/systems/partials/jobActionsCell.html',
            //    label:    'SYSTEMS.JOBS.TABLE.ACTION',
            //    dontSort: true
            }],
            caption:    'SYSTEMS.JOBS.TABLE.CAPTION',
            resource:   'jobs',
            category:   'systems',
            apiService: JobsResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });
    }
]);

angular.module('adminNg.controllers')
.controller('ServicesCtrl', ['$scope', 'Table', 'ServicesResource', 'ServiceResource', 'ResourcesFilterResource',
    function ($scope, Table, ServicesResource, ServiceResource, ResourcesFilterResource) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'status',
                label: 'SYSTEMS.SERVICES.TABLE.STATUS'
            }, {
                name:  'name',
                label: 'SYSTEMS.SERVICES.TABLE.NAME'
            }, {
                name:  'hostname',
                label: 'SYSTEMS.SERVICES.TABLE.HOST_NAME'
            }, {
                name:  'completed',
                label: 'SYSTEMS.SERVICES.TABLE.COMPLETED'
            }, {
                name:  'running',
                label: 'SYSTEMS.SERVICES.TABLE.RUNNING'
            }, {
                name:  'queued',
                label: 'SYSTEMS.SERVICES.TABLE.QUEUED'
            }, {
                name:  'meanRunTime',
                label: 'SYSTEMS.SERVICES.TABLE.MEAN_RUN_TIME'
            }, {
                name:  'meanQueueTime',
                label: 'SYSTEMS.SERVICES.TABLE.MEAN_QUEUE_TIME'
            }, {
                template: 'modules/systems/partials/serviceActionsCell.html',
                label:    'SYSTEMS.SERVICES.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'SYSTEMS.SERVICES.TABLE.CAPTION',
            resource:   'services',
            category:   'systems',
            apiService: ServicesResource,
            sorter:     {"sorter":{"services":{"status":{"name":"status","priority":0,"order":"DESC"}}}}
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.sanitize = function (hostname, serviceType) {
            ServiceResource.sanitize({
                host: hostname,
                serviceType: serviceType
            }, function () {
                $scope.table.fetch();
            });
        };
    }
]);

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

angular.module('adminNg.controllers')
.controller('DashboardCtrl', ['$scope',
    function ($scope) {
        $scope.resource = 'dashboard';
    }
]);

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

angular.module('adminNg.controllers')
.controller('ThemesCtrl', ['$scope', 'Table', 'ThemesResource', 'ThemeResource', 'ResourcesFilterResource', 'Notifications',
    function ($scope, Table, ThemesResource, ThemeResource, ResourcesFilterResource, Notifications) {

        $scope.table = Table;
        $scope.table.configure({
            columns: [{
                name:  'name',
                label: 'CONFIGURATION.THEMES.TABLE.NAME'
            }, {
                name:  'description',
                label: 'CONFIGURATION.THEMES.TABLE.DESCRIPTION'
            },  {
                name:  'creator',
                label: 'CONFIGURATION.THEMES.TABLE.CREATOR'
            }, {
                name:  'creation_date',
                label: 'CONFIGURATION.THEMES.TABLE.CREATED'
            },
            /*
             * Temporarily disabled
             *
             *{
             *    name:  'default',
             *    label: 'CONFIGURATION.THEMES.TABLE.DEFAULT'
             *},
             */
            {
                template: 'modules/configuration/partials/themesActionsCell.html',
                label:    'CONFIGURATION.THEMES.TABLE.ACTION',
                dontSort: true
            }],
            caption:    'CONFIGURATION.THEMES.TABLE.CAPTION',
            resource:   'themes',
            category:   'configuration',
            apiService: ThemesResource
        });

        $scope.filters = ResourcesFilterResource.get({ resource: $scope.table.resource });

        $scope.table.delete = function (row) {
            ThemeResource.delete({id: row.id}, function () {
                Table.fetch();
                Notifications.add('success', 'THEME_DELETED');
            }, function () {
                Notifications.add('error', 'THEME_NOT_DELETED', 'user-form');
            });
        };

    }
]);

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

angular.module('adminNg.controllers')
.controller('ThemeFormCtrl', ['$scope', '$timeout', 'FormNavigatorService', 'Notifications', 'ThemeResource', 'NewThemeResource', 'ThemeUsageResource', 'Table',
    function ($scope, $timeout, FormNavigatorService, Notifications, ThemeResource, NewThemeResource, ThemeUsageResource, Table) {
        var action = $scope.$parent.action;

        $scope.currentForm = 'generalForm';

        $scope.navigateTo = function (targetForm, currentForm, requiredForms) {
            // We have to set the currentForm property here in the controller.
            // The reason for that is that the footer sections in the partial are decorated with ng-if, which
            // creates a new scope each time they are activated.
            $scope.currentForm = FormNavigatorService.navigateTo(targetForm, currentForm, requiredForms);
        };

        $scope.cancel = function () {
            $scope.close();
        };

        $scope.valid = function () {
            if (angular.isDefined($scope.themeForm)) {
                return $scope.themeForm.$valid;
            }
            return false;
        };

        if (action === 'add') {
            // Lets set some defaults first
            $scope.general = {
                'default': false
            };

            $scope.bumper = {
                'active': false
            };

            $scope.trailer = {
                'active': false
            };

            $scope.license = {
                'active': false
            };

            $scope.watermark = {
                'active': false,
                position: 'topRight'
            };

            $scope.titleslide = {
                'mode':'extract'
            };
        }

        if (action === 'edit') {
            // load resource
            ThemeResource.get({id: $scope.resourceId, format: '.json'}, function (response) {
                angular.forEach(response, function (obj, name) {
                    $scope[name] = obj;
                });
                $scope.themeLoaded = true;
            });


            $scope.usage = ThemeUsageResource.get({themeId:$scope.resourceId});

        }

        $scope.submit = function () {
            var messageId, userdata = {}, success, failure;
            success = function () {
                Notifications.add('success', 'THEME_CREATED');
                Notifications.remove(messageId);
                $timeout(function () {
                    Table.fetch();
                }, 1000);
            };

            failure = function () {
                Notifications.add('error', 'THEME_NOT_CREATED');
                Notifications.remove(messageId);
            };

            // add message that never disappears
            messageId = Notifications.add('success', 'THEME_UPLOAD_STARTED', 'global', -1);
            userdata = {
                general: $scope.general,
                bumper: $scope.bumper,
                trailer: $scope.trailer,
                license: $scope.license,
                titleslide: $scope.titleslide,
                watermark: $scope.watermark
            };
            if (action === 'add') {
                NewThemeResource.save({}, userdata, success, failure);
            }
            if (action === 'edit') {
                ThemeResource.update({id: $scope.resourceId}, userdata, success, failure);
            }
            // close will not fetch content yet....
            $scope.close(false);
        };

    }]);


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
 * @name adminNg.modules.configuration.validators.uniquerThemeName
 * @description
 * Makes sure that the model name is unique.
 *
 */
angular.module('adminNg.directives')
.directive('uniqueThemeName', ['ThemesResource', 'Notifications', function (ThemesResource, Notifications) {
    var existingModelValue, link;
    link = function (scope, elm, attrs, ctrl) {
        var existingThemes;
        if (angular.isUndefined(existingThemes)) {
            existingThemes = ThemesResource.get();
        }
        ctrl.$validators.uniqueTheme = function (modelValue, viewValue) {
            var result = true;
            if (!ctrl.$dirty) {
                if (angular.isDefined(ctrl.$modelValue)) {
                    existingModelValue = ctrl.$modelValue;
                }
                return true;
            }
            if (ctrl.$isEmpty(viewValue)) {
                Notifications.add('error', 'THEME_NAME_EMPTY', 'new-theme-general');
                // consider empty models to be invalid
                result = false;
            }
            else {
                if (angular.isDefined(existingModelValue)) {
                    if (existingModelValue === viewValue) {
                        return true; // thats ok
                    }
                }
                angular.forEach(existingThemes.results, function (theme) {
                    if (theme.name === viewValue) {
                        Notifications.add('error', 'THEME_NAME_ALREADY_TAKEN', 'new-theme-general');
                        result = false;
                    }
                });
            }

            // it is invalid
            return result;
        };
    };
    return {
        require: 'ngModel',
        link: link
    };
}]);
