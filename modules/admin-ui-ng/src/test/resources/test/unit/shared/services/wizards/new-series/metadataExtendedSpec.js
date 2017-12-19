describe('Metadata Extended Step in New Series Wizard', function () {
    var NewSeriesMetadataExtended, $httpBackend, $scope, withoutExtendedMetadata, withExtendedMetadata, setParams;

    withoutExtendedMetadata = [{
        'flavor': 'dublincore/series',
        'title': 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
        'fields': [
                {
                    'id': 'title',
                    'label': 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
                    'value': '',
                    'required': 'true',
                    'type': 'text',
                    'readOnly': false
                }
            ]
        }];

    withExtendedMetadata = [{
        'flavor': 'dublincore/series',
        'title': 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
        'fields': [
                {
                    'id': 'title',
                    'label': 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
                    'value': '',
                    'required': 'true',
                    'type': 'text',
                    'readOnly': false
                }
            ]
        },
        {
            'flavor': 'dublincore/extended-1',
            'title': 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
            'fields': [
                    {
                        'id': 'title',
                        'label': 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
                        'value': '',
                        'required': 'true',
                        'type': 'text',
                        'readOnly': false
                    },
                    {
                        'id': 'theTruth',
                        'label': 'EVENTS.EVENTS.DETAILS.METADATA.BOOLEAN_FIELD',
                        'value': '',
                        'required': 'true',
                        'type': 'boolean',
                        'readOnly': false
                    }

                ]
            }];

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

    beforeEach(inject(function (_NewSeriesMetadataExtended_, _$httpBackend_, $rootScope) {
        NewSeriesMetadataExtended = _NewSeriesMetadataExtended_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app';
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/components.json')));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/ACL.json')));
        $httpBackend.whenGET('/workflow/definitions.json').respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));
    });

    setParams = function (ctrl, id, value) {
        $scope.$parent.target = 'dublincore/extended-1';
        $scope.$parent.params = {
            id: id,
            value: value
        };
        ctrl.save($scope);
    };

    it('is not visible if there is no extended metadata available', function () {
        $httpBackend.whenGET('/admin-ng/series/new/metadata').respond(JSON.stringify(withoutExtendedMetadata));
        $httpBackend.flush();
        expect(NewSeriesMetadataExtended.visible).toBeFalsy();
    });

    it('is visible if there is extended metadata available', function () {
        $httpBackend.whenGET('/admin-ng/series/new/metadata').respond(JSON.stringify(withExtendedMetadata));
        $httpBackend.flush();
        expect(NewSeriesMetadataExtended.visible).toBeTruthy();
    });

    it('is invalid if the required values are missing', function () {
        $httpBackend.whenGET('/admin-ng/series/new/metadata').respond(JSON.stringify(withExtendedMetadata));
        $httpBackend.flush();
        expect(NewSeriesMetadataExtended.isValid()).toBeFalsy();
    });

    it('is valid as soon as all required values are entered', function () {
        $httpBackend.whenGET('/admin-ng/series/new/metadata').respond(JSON.stringify(withExtendedMetadata));
        $httpBackend.flush();
        setParams(NewSeriesMetadataExtended, 'title', 'heinz the title');
        setParams(NewSeriesMetadataExtended, 'theTruth', 'is important');
        expect(NewSeriesMetadataExtended.isValid()).toBeTruthy();
    });
});

