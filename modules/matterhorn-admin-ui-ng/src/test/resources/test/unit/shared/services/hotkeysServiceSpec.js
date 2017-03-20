describe('HotkeysService', function () {
    var $httpBackend, HotkeysService;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('cfp.hotkeys'));

    beforeEach(inject(function (_$httpBackend_, _HotkeysService_) {
        $httpBackend = _$httpBackend_;

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));

        HotkeysService = _HotkeysService_;
    }));

    it('instantiates', function () {
        expect(HotkeysService.keyBindings).toBeDefined();
    });

    describe('#activateHotkey', function () {

        it('sets a hotkey with angular-hotkeys with provided scope', function () {
            //To be done, no idea how to test it
        });
    });

    describe('#activateUniversalHotkey', function () {

        it('sets a hotkey with angular-hotkeys without a scope', function () {
            //To be done, no idea how to test it
        });
    });

});
