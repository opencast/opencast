describe('adminNg.directives.adminNgNav', function () {
    var $compile, $rootScope, $httpBackend, $timeout, element;

    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/mainNav.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$rootScope_, _$compile_, _$httpBackend_, _$timeout_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
    }));

    beforeEach(function () {
        // Fake jQuery animate callback. Usually setting $.fx.off = true is
        // sufficient. However, as this directive operates on elements outside
        // of its partial, the animation is never triggered (because the
        // originating element cannot be found). This could be solved by
        // a) Using a more sophisticated HTML fixture setup.
        // or
        // b) Fixing the directive which should not handle DOM elements
        //    outside of its partial.
        spyOn($.fn, 'animate').and.callFake(function (prop, speed, callback) {
            if (!callback) { return; }
            element.find('#roll-up-menu').css(prop);
            callback.apply(element.find('#roll-up-menu'));
        });

        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
        $httpBackend.whenGET('/sysinfo/bundles/version?prefix=matterhorn').respond(201, {});
        $rootScope.userIs = function () { return true; };
        element = $compile('<admin-ng-nav /><div class="main-view"></div>')($rootScope);
        $rootScope.$digest();
    });

    it('creates a navigation menu', function () {
        expect(element).toBeMatchedBy('div.menu-top');
    });

    it('opens the menu when it is closed', function () {
        element.click();
        expect(element.find('#roll-up-menu')).toHaveCss({ opacity: '1' });
    });
});
