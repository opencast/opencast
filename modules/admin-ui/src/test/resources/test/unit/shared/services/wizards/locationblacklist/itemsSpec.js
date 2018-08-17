describe('Items Step in Location Blacklist Wizard', function () {
    var NewLocationblacklistItems, CaptureAgentsResource, $httpBackend;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            configureFromServer: function () {}
        });
    }));

    beforeEach(inject(function (_NewLocationblacklistItems_, _CaptureAgentsResource_, _$httpBackend_) {
        NewLocationblacklistItems = _NewLocationblacklistItems_;
        CaptureAgentsResource = _CaptureAgentsResource_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond('{"results":[]}');
    });

    describe('#isValid', function () {

        it('is valid when locations are set', function () {
            expect(NewLocationblacklistItems.isValid()).toBe(false);
            NewLocationblacklistItems.ud.items = [{ id: 81294 }];
            expect(NewLocationblacklistItems.isValid()).toBe(true);
        });
    });

    describe('#addItem', function () {
        beforeEach(function () {
            NewLocationblacklistItems.ud.items.push({ id: 81294 });
        });

        describe('with a new locations', function () {

            it('selects the location', function () {
                expect(NewLocationblacklistItems.ud.items.length).toBe(1);

                NewLocationblacklistItems.ud.itemToAdd = { id: 7231 };
                NewLocationblacklistItems.addItem();

                expect(NewLocationblacklistItems.ud.items).toContain({ id: 7231 });
                expect(NewLocationblacklistItems.ud.items.length).toBe(2);
            });
        });

        describe('with an already selected location', function () {
            beforeEach(function () {
                NewLocationblacklistItems.ud.items.push({ id: 7231 });
            });

            it('does not select the location twice', function () {
                expect(NewLocationblacklistItems.ud.items.length).toBe(2);

                NewLocationblacklistItems.ud.itemToAdd = { id: 7231 };
                NewLocationblacklistItems.addItem();

                expect(NewLocationblacklistItems.ud.items.length).toBe(2);
            });
        });
    });
});
