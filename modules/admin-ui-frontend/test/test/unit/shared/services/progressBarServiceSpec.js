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
  var $httpBackend, ProgressBar;

  beforeEach(module('ngResource'));
  beforeEach(module('adminNg.services'));
  beforeEach(module('adminNg.resources'));
  beforeEach(module('pascalprecht.translate'));

  beforeEach(inject(function (_$httpBackend_, _ProgressBar_) {
    $httpBackend = _$httpBackend_;
    ProgressBar = _ProgressBar_;
  }));

  it('has the two public methods', function () {
    expect(ProgressBar.onUploadFileProgress).toBeDefined();
    expect(ProgressBar.reset).toBeDefined();
  });

  describe('#onUploadFileProgress', function () {
    it('updates the progress status ratio', function () {
      expect(ProgressBar.getProgress()).toBe(0);
      ProgressBar.onUploadFileProgress({
        'loaded': 2, "total": 4
      });
      // precision 1, for example: 0.5226440368941985 to be close to 0.5
      expect(ProgressBar.getProgress()).toBeCloseTo(0.5, 1);
    });
  });

  describe('#reset', function () {
    it('resets progress variables', function () {
      expect(ProgressBar.getProgress()).toBe(0);
      ProgressBar.onUploadFileProgress({
        'loaded': 2, "total": 4
      });
      expect(ProgressBar.getProgress()).toBeGreaterThan(0);
      expect(ProgressBar.getProgress()).toBeLessThan(1);
      ProgressBar.reset();
      expect(ProgressBar.getProgress()).toBe(0);
    });
  });
});
