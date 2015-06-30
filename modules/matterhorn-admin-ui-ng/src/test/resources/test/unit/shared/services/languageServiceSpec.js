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

describe('adminNg.services.language', function () {
    var $httpBackend, $q, Language, JsHelper;

    beforeEach(module('adminNg'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.services.language'));

    beforeEach(inject(function (_$httpBackend_, _$q_, _Language_, _JsHelper_) {
        $httpBackend = _$httpBackend_;
        $q = _$q_;
        Language = _Language_;
        JsHelper = _JsHelper_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.expectGET('/i18n/languages.json').respond(getJSONFixture('i18n/languages.json'));
        $httpBackend.whenGET('public/org/opencastproject/adminui/languages/lang-en_US.json').respond('');
        $httpBackend.flush();
    });

    afterEach(function () {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('Sets and gets the fallback language', function () {
        Language.setFallbackLanguage('en_US');
        expect(Language.getFallbackLanguage()).toEqual('en_US');
    });

    it('Sets the available languages', function () {
        Language.setAvailableLanguages(['en_US', 'de_DE']);
        expect(Language.getAvailableLanguages(true)).toEqual(['en_US', 'de_DE']);
    });

    describe('#getLanguage', function () {

        it('Sets the date Formats', function () {
            expect(Language.getLanguage().dateFormats.dateTime).toBeDefined();
            expect(Language.getLanguage().dateFormats.time).toBeDefined();
            expect(Language.getLanguage().dateFormats.date).toBeDefined();
        });

        it('Sets and gets the current language', function () {
            Language.setLanguage('de_DE');
            expect(Language.getLanguage().code).toEqual('de_DE');
        });
    });

    it('retrieves available languages as an Array of Strings', function () {
        Language.setAvailableLanguages([{ code: 'de_DE' }, { code: 'en_US' }]);
        expect(Language.getAvailableLanguageCodes()).toEqual(['de', 'en']);
    });

    describe('#formatTime', function () {

        it('localizes a date correctly', function () {
            expect(Language.formatTime('short', '2010-09-29T15:59:00')).toEqual('3:59 PM');
        });

        it('copes with empty date strings when formatting', function () {
            expect(Language.formatTime('short', undefined)).toEqual('');
        });
    });

    describe('#getLanguageCode', function () {

        it('returns the correct language code', function () {
            expect(Language.getLanguageCode()).toEqual('en');
        });

        describe('without a configured language', function () {
            beforeEach(function () {
                delete Language.currentLanguage;
            });

            it('returns nothing', function () {
                expect(Language.getLanguageCode()).toBeUndefined();
            });
        });

        describe('with a valid language', function () {
            beforeEach(function () {
                delete Language.currentLanguage;
                spyOn(Language, 'setLanguage').and.callFake(function () {
                    Language.currentLanguage = 'en';
                });
            });

            it('returns nothing', function () {
                expect(Language.getLanguageCode()).toBeUndefined();
            });
        });
    });

    describe('#formatDate', function () {
        var day;

        it('applies the locale to a date string', function () {
            expect(Language.formatDate('short', '2012-12-01')).toEqual('12/1/12');
        });

        describe('without a date string', function () {

            it('returns an empty string', function () {
                expect(Language.formatDate('short')).toEqual('');
            });
        });

        describe('with a date of one day ago', function() {
            beforeEach(function () {
                day = new Date();
                day.setDate(day.getDate() - 1);
            });

            it('returns an i18n key for yesterday', function () {
                expect(Language.formatDate('short', day.toISOString()))
                    .toEqual('DATES.YESTERDAY');
            });
        });

        describe('with a date of in one day', function() {
            beforeEach(function () {
                day = new Date();
                day.setDate(day.getDate() + 1);
            });

            it('returns an i18n key for tomorrow', function () {
                expect(Language.formatDate('short', day.toISOString()))
                    .toEqual('DATES.TOMORROW');
            });
        });

        describe('with a date of today', function() {
            beforeEach(function () {
                day = new Date();
            });

            it('returns an i18n key for today', function () {
                expect(Language.formatDate('short', day.toISOString()))
                    .toEqual('DATES.TODAY');
            });
        });
    });

    describe('#formatDateRaw', function () {
        var day;

        it('applies the locale to a date string', function () {
            expect(Language.formatDateRaw('short', '2012-12-01')).toEqual('12/1/12');
        });

        describe('without a date string', function () {

            it('returns an empty string', function () {
                expect(Language.formatDateRaw('short')).toEqual('');
            });
        });

        describe('with a date of one day ago', function() {
            beforeEach(function () {
                day = new Date();
                day.setDate(day.getDate() - 1);
            });

            it('does not return an i18n key for yesterday', function () {
                expect(Language.formatDateRaw('short', day.toISOString()))
                    .not.toEqual('DATES.YESTERDAY');
            });
        });
    });

    describe('#formatDateTime', function () {
        var day;

        it('applies the locale to a date string', function () {
            var newDate = JsHelper.toZuluTimeString({
                date   : '2012-12-01',
                hour   :  6,
                minute : '30'
            });

            expect(Language.formatDateTime('short', newDate))
                .toEqual('12/1/12 6:30 AM');
        });

        describe('without a date string', function () {

            it('returns an empty string', function () {
                expect(Language.formatDateTime('short')).toEqual('');
            });
        });

        describe('with a date of one day ago', function() {
            beforeEach(function () {
                day = new Date();
                day.setDate(day.getDate() - 1);
            });

            it('returns an i18n key for yesterday', function () {
                expect(Language.formatDateTime('short', day.toISOString()))
                    .toEqual('DATETIMES.YESTERDAY');
            });
        });

        describe('with a date of in one day', function() {
            beforeEach(function () {
                day = new Date();
                day.setDate(day.getDate() + 1);
            });

            it('returns an i18n key for tomorrow', function () {
                expect(Language.formatDateTime('short', day.toISOString()))
                    .toEqual('DATETIMES.TOMORROW');
            });
        });

        describe('with a date of today', function() {
            beforeEach(function () {
                day = new Date();
            });

            it('returns an i18n key for today', function () {
                expect(Language.formatDateTime('short', day.toISOString()))
                    .toEqual('DATETIMES.TODAY');
            });
        });
    });

    describe('#formatDateTimeRaw', function () {
        var day;

        it('applies the locale to a date string', function () {
            var newDate = JsHelper.toZuluTimeString({
                date   : '2012-12-01',
                hour   :  6,
                minute : '30'
            });

            expect(Language.formatDateTimeRaw('short', newDate))
                .toEqual('12/1/12 6:30 AM');
        });

        describe('without a date string', function () {

            it('returns an empty string', function () {
                expect(Language.formatDateTimeRaw('short')).toEqual('');
            });
        });

        describe('with a date of one day ago', function() {
            beforeEach(function () {
                day = new Date();
                day.setDate(day.getDate() - 1);
            });

            it('does not return an i18n key for yesterday', function () {
                expect(Language.formatDateTimeRaw('short', day.toISOString()))
                    .not.toEqual('DATETIMES.YESTERDAY');
            });
        });
    });

    describe('#formatDateRange', function () {

        it('applies the locale to a date string', function () {
            expect(Language.formatDateRange('short', '2012-12-01/2013-02-18')).toEqual('12/1/12 - 2/18/13');
        });

        describe('without a date string', function () {

            it('returns an empty string', function () {
                expect(Language.formatDateRange('short')).toEqual('');
            });
        });
    });

    describe('#setLanguages', function () {

        it('applies JSON data', function () {
            Language.setLanguages({
                bestLanguage: 'best',
                fallbackLanguage: 'fallback',
                availableLanguages: 'available',
                dateFormats: 'formats'
            });
            expect(Language.currentLanguage).toEqual('best');
            expect(Language.fallbackLanguage).toEqual('fallback');
            expect(Language.availableLanguages).toEqual('available');
            expect(Language.dateFormats).toEqual('formats');
        });
    });

    describe('#loadLanguageFromServer', function () {

        it('rejects on errors', function () {
            var deferred = $q.defer();
            spyOn(deferred, 'reject');
            $httpBackend.whenGET('public/org/opencastproject/adminui/languages/lang-ja_JP.json').respond(404);
            Language.loadLanguageFromServer('ja_JP', deferred);
            $httpBackend.flush();
            expect(deferred.reject).toHaveBeenCalled();
        });
    });

    describe('#changeLanguage', function () {
        beforeEach(function () {
            $httpBackend.expectGET('/i18n/languages.json').respond(getJSONFixture('i18n/languages.json'));
        });

        afterEach(function () {
            window.localStorage.clear();
        });

        it('requests the given language from the server', function () {
            $httpBackend.whenGET('public/org/opencastproject/adminui/languages/lang-de_DE.json').respond('');
            Language.changeLanguage('de_DE');
            $httpBackend.flush(1);
            expect(Language.getLanguage().code).toEqual('de_DE');
            $httpBackend.flush();
        });
    });

    describe('#configureFromServer', function () {

        describe('on success', function () {
            beforeEach(function () {
                $httpBackend.expectGET('/i18n/languages.json')
                    .respond(getJSONFixture('i18n/languages.json'));
                $httpBackend.expectGET('public/org/opencastproject/adminui/languages/lang-en_US.json');
            });

            it('fetches the languages from the server', function () {
                Language.configureFromServer($q.defer());
                $httpBackend.flush();
            });
        });

        describe('on error', function () {
            beforeEach(function () {
                $httpBackend.whenGET('/i18n/languages.json').respond(500);
            });

            it('rejects the promise', function () {
                var promise = $q.defer();
                spyOn(promise, 'reject');
                Language.configureFromServer(promise);
                $httpBackend.flush();
                expect(promise.reject).toHaveBeenCalled();
            });
        });
    });

    describe('#$convertLanguageToCode', function () {

        describe('with a language string', function () {

            it('returns the language code', function () {
                expect(Language.$convertLanguageToCode('de_DE')).toEqual('de');
            });
        });

        describe('with a language code', function () {

            it('returns the language code', function () {
                expect(Language.$convertLanguageToCode('jp')).toEqual('jp');
            });
        });
    });

    describe('#toLocalTime', function () {      

        it('converts a zulu time string back to local time', function () {
            var localTime = JsHelper.toZuluTimeString({
                    date   : '2014-07-12',
                    hour   : 12,
                    minute : 0
                });

            expect(Language.toLocalTime(localTime)).toEqual('Sat Jul 12 12:00:00 2014');
        });
    });
});
