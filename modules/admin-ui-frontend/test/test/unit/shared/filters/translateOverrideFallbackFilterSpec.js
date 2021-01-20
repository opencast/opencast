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
describe('adminNg.filters.translateOverrideFallback', function () {
    var $translate, $translateProvider;
    var overrideTitle = "My override title",
    fallbackTitle = "My fallback title",
    overrideDetail = "My override details text",
    fallbackDetail = "My fallback details text",
    audioTranslated = "ήχου";

    var myAssetFallback = {
        "title": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.FOOBAR",
        "displayFallback.SHORT": fallbackTitle,
        "displayFallback.DETAIL": fallbackDetail
    };
    var myAssetOverride = {
        "title": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.FOOBAR",
        "displayOverride.SHORT": overrideTitle,
        "displayOverride.DETAIL": overrideDetail
    };
    var myDontHaveSubTitle = {
        "title": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.FOOBAR",
        "displayOverride": overrideTitle,
        "displayFallback": fallbackTitle
    };
    var myAssetAudio = {
        "title": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.AUDIO",
        "displayFallback.SHORT": fallbackTitle
    };

    beforeEach(module('adminNg', function ($translateProvider) {
        $translateProvider.translations('en', {
            'EVENTS.EVENTS.NEW.SOURCE.UPLOAD.AUDIO': audioTranslated
        }).preferredLanguage('en');
    }));

    beforeEach(inject(function (_$translate_) {
        $translate = _$translate_;
    }));

    it('has this filter', inject(function ($filter) {
        expect($filter('translateOverrideFallback')).not.toBeNull();
    }));

    it('should return the override', inject(function (translateOverrideFallbackFilter) {
        expect(translateOverrideFallbackFilter(myAssetOverride, 'SHORT')).toEqual(overrideTitle);
        expect(translateOverrideFallbackFilter(myAssetOverride, 'DETAIL')).toEqual(overrideDetail);
    }));

    it('should return the fallback', inject(function (translateOverrideFallbackFilter) {
        expect(translateOverrideFallbackFilter(myAssetFallback, 'SHORT')).toEqual(fallbackTitle);
        expect(translateOverrideFallbackFilter(myAssetFallback, 'DETAIL')).toEqual(fallbackDetail);
    }));

    it('should return the override with no type needed', inject(function (translateOverrideFallbackFilter) {
        expect(translateOverrideFallbackFilter(myDontHaveSubTitle)).toEqual(overrideTitle);
    }));

    it('should return the translated title', inject(function (translateOverrideFallbackFilter) {
        expect(translateOverrideFallbackFilter(myAssetAudio)).toEqual(audioTranslated);
    }));
});
