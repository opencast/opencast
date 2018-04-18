describe('Upload Asset New Event Step in New Event Wizard', function () {
    var NewEventUploadAsset, $httpBackend, $scope, setParams;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            changeLanguage: function () {},
            getLanguageCode: function () { return 'ja_JP'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_NewEventUploadAsset_, _$httpBackend_, $rootScope) {
        NewEventUploadAsset = _NewEventUploadAsset_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/eventUploadAssetOptions.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/resources/eventUploadAssetOptions.json')));
    });

    describe('#isVisible', function () {

        it('is invisible by default', function () {
            NewEventUploadAsset.wizard = {
                getStateControllerByName: function() {
                  return {
                    isUpload: function() {
                      return false;
                    }
                  }
                }
            };
            NewEventUploadAsset.ud.defaults = {};
            NewEventUploadAsset.checkIfVisible();
            expect(NewEventUploadAsset.visible).toBeFalsy();
        });

        it('is still invisible when an asset option is available but source schedule', function () {
            NewEventUploadAsset.wizard = {
                getStateControllerByName:function() {
                  return {
                    isUpload: function() {
                      return false;
                    }
                  }
                }
            };
            NewEventUploadAsset.ud.defaults = {'foo':'bar'};
            NewEventUploadAsset.checkIfVisible();
            expect(NewEventUploadAsset.visible).toBe(false);
        });

        it('is still invisible when source is upload but there are no non-track options', function () {
            NewEventUploadAsset.wizard = {
                getStateControllerByName:function() {
                  return {
                    isUpload: function() {
                      return true;
                    }
                  }
                }
            };
            NewEventUploadAsset.ud.defaults = {'foo':'bar'};
            NewEventUploadAsset.checkIfVisible();
            expect(NewEventUploadAsset.visible).toBe(false);
        });

        it('becomes visible when source is upload and there are non-track options', function () {
            NewEventUploadAsset.wizard = {
                getStateControllerByName:function() {
                  return {
                    isUpload: function() {
                      return true;
                    }
                  }
                }
            };
            NewEventUploadAsset.ud.defaults = {'foo':'bar'};
            NewEventUploadAsset.ud.hasNonTrackOptions = true;
            NewEventUploadAsset.checkIfVisible();
            expect(NewEventUploadAsset.visible).toBe(true);
        });
    });

});
