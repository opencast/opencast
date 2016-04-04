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

            return this.$convertLanguageToCode(me.currentLanguage.code);
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
                result.push(this.$convertLanguageToCode(value.code));
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
            return $filter('date')(zuluTimeString, 'EEE MMM dd HH:mm:ss yyyy');
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

        /**
        * @ngdoc function
        * @name Language.$convertLanguageToCode
        * @description
        * Converts a language string (e.g. 'de_DE') to a language code (e.g. 'de').
        * @returns {string} The language code for the input value.
        */
        this.$convertLanguageToCode = function (language) {
            var languageCode = language, index = languageCode.indexOf('_');
            if (index > 0) {
                return languageCode.substring(0, index);
            }
            return languageCode;
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
