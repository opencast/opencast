describe('adminNg.directives.adminNgEditable', function () {
    var $compile, $rootScope, $httpBackend, element;

    beforeEach(module('adminNg'));
    beforeEach(module('adminNg.filters'));
    beforeEach(module('shared/partials/editable.html'));
    beforeEach(module('shared/partials/editableBooleanValue.html'));
    beforeEach(module('shared/partials/editableSingleValue.html'));
    beforeEach(module('shared/partials/editableMultiValue.html'));
    beforeEach(module('shared/partials/editableSingleSelect.html'));
    beforeEach(module('shared/partials/editableMultiSelect.html'));
    beforeEach(module('shared/partials/editableDateValue.html'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            formatDateTime: function (val, dt) { return dt; },
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
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
    });

    describe('for a read-only value', function () {
        beforeEach(function () {
            $rootScope.params = {
                type: 'text',
                value: 'A Title',
                readOnly: true
            };
            element = $compile('<div admin-ng-editable="" params="params"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('outputs the value in a span', function () {
            expect(element.find('span').html()).toEqual('A Title');
        });
    });

    describe('for a value with insufficient rights', function () {
        beforeEach(function () {
            $rootScope.params = {
                type: 'text',
                value: 'A Title',
                readOnly: false
            };
            element = $compile('<div admin-ng-editable="" params="params" requiredRole="ROLE_NOOP"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('outputs the value in a span', function () {
            expect(element.find('span').html()).toEqual('A Title');
        });
    });

    describe('for a single value', function () {
        beforeEach(function () {
            $rootScope.params = {
                id:   'title',
                type: 'text',
                label: 'TITLE',
                value: 'A Title',
                type:  'text'
            };
            element = $compile('<div admin-ng-editable="" params="params"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('sets the mode and the class accordingly', function () {
            expect(element.find('div').scope().mode).toEqual('singleValue');
            expect(element.hasClass('editable')).toEqual(true);
        });
    });

    describe('for a date value', function(){

        beforeEach(function(){
            $rootScope.params = {
                id: 'date',
                value: '2015-12-05',
                type:'date',
                mode:'dateValue'
            };

            element = $compile('<div admin-ng-editable="" params="params"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('displays a datetimepicker', function(){
            expect(element.find('input')).toHaveAttr('datetimepicker');
        });
    });

    describe('for a single select', function () {
        beforeEach(function () {
            $rootScope.params = {
                id:         'series',
                label:      'SERIES',
                value:      'phy325',
                type:       'text',
                collection: { phy325: 'Physics325', edu123: 'Education123' }
            };
            element = $compile('<div admin-ng-editable="" params="params"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('sets the mode and the class accordingly', function () {
            expect(element.find('div').scope().mode).toEqual('singleSelect');
            expect(element.hasClass('editable')).toEqual(true);
        });
    });

    describe('for a multi value', function () {
        beforeEach(function () {
            $rootScope.params = {
                id:    'subject',
                label: 'SUBJECT',
                value: ['First Subject', 'Second Subject'],
                type:  'text'
            };
            element = $compile('<div admin-ng-editable="" params="params"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('sets the mode and the class accordingly', function () {
            expect(element.find('div').scope().mode).toEqual('multiValue');
            expect(element.hasClass('editable')).toEqual(true);
        });
    });

    describe('for a multi select', function () {
        beforeEach(function () {
            $httpBackend.whenGET('/admin-ng/resources/users.json').respond(JSON.stringify({
                'matt.smith': 'Matt Smith',
                'chuck.norris': 'Chuck Norris',
                'franz.kafka': 'Franz Kafka',
                'a.morris': 'A. Morris'
            }));
            $rootScope.params = {
                id:         'presenters',
                type:       'multiselect',
                label:      'PRESENTER',
                value:      ['matt.smith', 'chuck.norris'],
                collection: 'users'
            };
            element = $compile('<div admin-ng-editable="" params="params"></div>')($rootScope);
            $rootScope.$digest();
        });

        it('sets the mode and the class accordingly', function () {
            expect(element.find('div').scope().mode).toEqual('multiSelect');
            expect(element.hasClass('editable')).toEqual(true);
        });
    });
});
