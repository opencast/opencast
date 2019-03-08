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

describe('adminNg.services.ProgressBar', function () {
  var $httpBackend, $translate, $timeout, cfpLoadingBar, ProgressBar;

  beforeEach(module('ngResource'));
  beforeEach(module('adminNg.services'));
  beforeEach(module('adminNg.resources'));
  beforeEach(module('pascalprecht.translate'));
  beforeEach(module('cfp.loadingBar'));

  beforeEach(inject(function (_$httpBackend_, _$translate_, _$timeout_, _cfpLoadingBar_, _ProgressBar_) {
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    $translate = _$translate_;
    cfpLoadingBar = _cfpLoadingBar_;
    ProgressBar = _ProgressBar_;
  }));

  it('Has the three primary interfaces', function () {
    expect(ProgressBar.start).toBeDefined();
    expect(ProgressBar.onUploadFileProgress).toBeDefined();
    expect(ProgressBar.complete).toBeDefined();
  });

  describe('#start', function () {

    it('starts the CFP progress bar', function () {
      expect(cfpLoadingBar.status()).toBe(0);
      ProgressBar.start();
      $timeout.flush();
      expect(cfpLoadingBar.status()).toBeGreaterThan(0);
    });
  });

  describe('#onUploadFileProgress', function () {

    it('sets progress bar progress status ratio', function () {
      ProgressBar.start();
      $timeout.flush();
      ProgressBar.onUploadFileProgress({
        'loaded': 2, "total": 4
      });
      $timeout.flush();
      // precision 1, for example: 0.5226440368941985 to be close to 0.5
      expect(cfpLoadingBar.status()).toBeCloseTo(0.5, 1);
    });
  });

  describe('#complete', function () {

    it('completes the progress bar', function () {
      ProgressBar.start();
      $timeout.flush();
      expect(cfpLoadingBar.status()).toBeGreaterThan(0);
      expect(cfpLoadingBar.status()).toBeLessThan(1);
      ProgressBar.complete();
      $timeout.flush();
      // Complete causes the cfpLoadingBar status go to 100%, then it animates itself close
      expect(cfpLoadingBar.status()).toBe(1);

    });
  });
});
