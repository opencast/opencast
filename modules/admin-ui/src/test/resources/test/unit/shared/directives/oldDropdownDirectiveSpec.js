describe('adminNg.directives.oldAdminNgDropdown', function () {
    var $compile, $rootScope, element, $httpBackend;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {}
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$httpBackend_, _$rootScope_, _$compile_) {
        $httpBackend = _$httpBackend_;
        $compile = _$compile_;
        $rootScope = _$rootScope_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        element = $compile('<div old-admin-ng-dropdown=""><ul><li><a></a></li></ul></div>')($rootScope);
        $rootScope.$digest();
    });

    it('creates a dropdown menu when clicked', function () {
        expect(element).not.toHaveClass('active');
        element.click();
        expect(element).toHaveClass('active');
    });

    it('closes the dropdown menu after clicking an item', function () {
        element.click();
        expect(element).toHaveClass('active');
        element.find('a').click();
        expect(element).not.toHaveClass('active');
    });
});
