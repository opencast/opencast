describe('adminNg.directives.adminNgPwCheck', function () {
    var $compile, $rootScope, element, NgModelController;

    beforeEach(module('adminNg.directives'));

    beforeEach(inject(function (_$rootScope_, _$compile_, _ngModelDirective_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;

        // Set up a spy on the required model controller
        var ngModel = _ngModelDirective_[0];
        ngModel.link = function () {};
        ngModel.controller = function () {
            this.$setValidity = jasmine.createSpy();
            this.$$setOptions = jasmine.createSpy();
            this.$formatters = [];
            this.$viewValue = 'foo';
            NgModelController = this;
        };
    }));

    beforeEach(function () {
        $rootScope.user = { password: 'foo' };
        element = $compile('<input ng-model="user.password" admin-ng-pw-check="user.passwordConfirmation"/>')($rootScope);
    });

    describe('when the passwords match', function () {
        beforeEach(function () {
            $rootScope.user.passwordConfirmation = 'foo';
            $rootScope.$digest();
        });

        it('sets validity of model controller to true', function () {
            expect(NgModelController.$setValidity).toHaveBeenCalledWith('pwmatch', true);
        });
    });

    describe('when the passwords do not match', function () {
        beforeEach(function () {
            $rootScope.user.passwordConfirmation = 'bar';
            $rootScope.$digest();
        });

        it('sets validity of model controller to false', function () {
            expect(NgModelController.$setValidity).toHaveBeenCalledWith('pwmatch', false);
        });
    });
});
