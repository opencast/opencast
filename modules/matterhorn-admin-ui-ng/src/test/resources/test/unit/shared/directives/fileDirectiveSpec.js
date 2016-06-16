describe('adminNg.directives.adminNgFileUpload', function () {
    var $compile, $rootScope, $timeout, $parse, element;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.services.modal'));
    beforeEach(module('adminNg.directives'));

    beforeEach(inject(function (_$rootScope_, _$timeout_, _$compile_, _$parse_) {
        $compile = _$compile_;
        $timeout = _$timeout_;
        $rootScope = _$rootScope_;
        $parse = _$parse_;
    }));

    beforeEach(function () {
        element = $compile('<input admin-ng-file-upload type="file" file="file"/>')($rootScope);
        $rootScope.$digest();
    });

    it('applies the scope when the file changes', function () {
        spyOn(element.scope(), '$apply');
        element.trigger($.Event('change'));

        expect(element.scope().$apply).toHaveBeenCalled();
    });

    it('assigns the model when the file changes', function () {
        $rootScope.file = {
            name: 'foo.bar'
        };
        element.trigger($.Event('change'));

        expect($rootScope.file).toBeUndefined();
    });
});
