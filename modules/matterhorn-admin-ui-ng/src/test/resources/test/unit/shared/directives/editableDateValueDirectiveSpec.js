describe('adminNg.directives.adminNgEditableSingleValue', function () {
    var $compile, $rootScope, $timeout, element;

    var originalValue = '2015-12-05';

    beforeEach(module('adminNg.directives'));
    beforeEach(module('adminNg.filters'));
    beforeEach(module('shared/partials/editableDateValue.html'));

    beforeEach(module(function ($provide) {
        $provide.value('Language', {
            getLanguageCode: function(){
                return 'de';
            },
            formatDate: function(format, input){
                return ('short' === format && input === '2015-12-05') ? '05.12.2015' : '';
            }
        });
    }));

    beforeEach(inject(function (_$rootScope_, _$timeout_, _$compile_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $timeout = _$timeout_;
    }));

    beforeEach(function () {
        $rootScope.params = { value: originalValue };
        $rootScope.save = jasmine.createSpy();
        element = $compile('<div admin-ng-editable-date-value save="save" params="params"></div>')($rootScope);
        $rootScope.$digest();
    });

    it('displays the date in german format', function () {
        expect(element.html()).toContain('05.12.2015');
    });

    it('is not editable by default', function () {
        expect(element.find('input')).toHaveClass('ng-hide');
        expect(element.find('span')).not.toHaveClass('ng-hide');
    });

    it('becomes editable on click', function(){

        //when
        element.click();
        $timeout.flush();

        //then
        expect(element.find('input')).not.toHaveClass('ng-hide');
        expect(element.find('span')).toHaveClass('ng-hide');
    });

    it('does not save when no changes have been made', function () {
        element.click();
        element.find('input').blur();
        expect($rootScope.save).not.toHaveBeenCalled();
    });

});
