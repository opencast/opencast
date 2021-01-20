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

describe('Modal service', function () {
    var Modal, $httpBackend, $timeout;

    beforeEach(module('adminNg.services.modal'));

    beforeEach(module(function ($provide) {
        $provide.value('Table', {fetch: function () {}});
    }));

    beforeEach(inject(function (_$httpBackend_, _$timeout_, _Modal_) {
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        $httpBackend.when('GET', /shared\/partials\/modals\/.*\.html/).respond(201, '<h1>Test Modal</h1>');
        Modal = _Modal_;
    }));

    afterEach(function () {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it('returns the modal object', function () {
        expect(Modal.show).toBeDefined();
    });

    describe('#show', function () {
        it('sets the guard', function () {
            Modal.show('test-modal');

            expect(Modal.opening).toBe(true);

            $httpBackend.flush();
        });

        it('does nothing if another modal is opening at the same time', function () {
            Modal.opening = true;
            Modal.show('test-modal');

            expect(Modal.opening).toBe(true);
        });

        it('focuses the modal', function () {
            spyOn($.fn, 'focus');
            Modal.show('test-modal');
            $httpBackend.flush();
            $timeout.flush();

            expect($.fn.focus).toHaveBeenCalled();
        });

        it('removes any previous modals', function () {
            Modal.modal = { remove: jasmine.createSpy() };
            Modal.overlay = { remove: jasmine.createSpy() };
            Modal.$scope = { $destroy: jasmine.createSpy() };

            Modal.show('test-modal');

            expect(Modal.modal.remove).toHaveBeenCalled();
            expect(Modal.overlay.remove).toHaveBeenCalled();

            $httpBackend.flush();
        });

        it('fetches the partial with the given file name', function () {
            $httpBackend.expectGET('shared/partials/modals/test-modal.html');
            Modal.show('test-modal');
            $httpBackend.flush();
        });

        it('draws an overlay', function () {
            Modal.show('test-modal');
            $httpBackend.flush();

            expect(Modal.overlay).toBeDefined();
            expect(Modal.overlay).toBeVisible();
        });

        it('draws the modal', function () {
            Modal.show('test-modal');
            $httpBackend.flush();

            expect(Modal.modal).toHaveText('Test Modal');
            expect(Modal.modal).toBeVisible();
        });

        it('sets the open flag to true', function () {
            Modal.show('test-modal');
            $httpBackend.flush();

            expect(Modal.$scope.open).toBeTruthy();
        });
    });

    describe('#close', function () {
        beforeEach(function () {
            Modal.show('test-modal');
            $httpBackend.flush();
        });

        it('sets the open flag to false', function () {
            expect(Modal.$scope.open).toBeTruthy();
            Modal.$scope.close();
            expect(Modal.$scope.open).toBeFalsy();
        });
    });

    describe('#keyUp', function () {
        beforeEach(function () {
            Modal.show('test-modal');
            $httpBackend.flush();
            spyOn(Modal.$scope, 'close');
        });

        it('closes the modal when ESC has been pressed', function () {
            Modal.$scope.keyUp({ keyCode: 27 });
            expect(Modal.$scope.close).toHaveBeenCalled();
        });
    });
});
