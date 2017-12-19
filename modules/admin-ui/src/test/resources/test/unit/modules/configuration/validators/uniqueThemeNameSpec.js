describe('adminNg.modules.configuration.validators.uniqueThemeName', function () {
    var $compile, $rootScope, $httpBackend, element;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));
    beforeEach(inject(function (_$rootScope_, _$compile_, _$httpBackend_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        $httpBackend.whenGET('/admin-ng/themes/themes.json').respond(JSON.stringify(getJSONFixture('admin-ng/themes/themes.json')));
        element = '<form name="form"><input type="text" name="themename" ng-model="model.name" unique-theme-name/></form>';
        $compile(element)($rootScope);
    });

    it('does not validate until themes are loaded', function () {
        $rootScope.$digest();
        $httpBackend.flush();
        expect($rootScope.form.themename.$valid).toBeTruthy();
    });

    it('validates legal value', function () {
        $httpBackend.flush();
        $rootScope.form.themename.$setViewValue('a legal title');
        $rootScope.$digest();
        expect($rootScope.model.name).toEqual('a legal title');
        expect($rootScope.form.themename.$valid).toBeTruthy();
    });

    it('validates illegal value', function () {
        $httpBackend.flush();
        $rootScope.form.themename.$setViewValue('Entwine Private');
        $rootScope.$digest();
        expect($rootScope.form.themename.$invalid).toBeTruthy();
    });

    it('validates illegal empty value', function () {
        $httpBackend.flush();
        $rootScope.form.themename.$setViewValue('');
        $rootScope.$digest();
        expect($rootScope.form.themename.$invalid).toBeTruthy();
    });

    it('knows the initial name and does not complain about it', function () {
        $rootScope.model = { name: 'Entwine Private' };
        $httpBackend.flush();
        expect($rootScope.form.themename.$valid).toBeTruthy();
        $rootScope.form.themename.$setViewValue('Der Heinz 2');
        $rootScope.form.themename.$setViewValue('Entwine Private');
        expect($rootScope.form.themename.$valid).toBeTruthy();
    });
});
