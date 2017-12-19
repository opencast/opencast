describe('Metadata Extended Step in New Event Wizard', function () {
    var NewEventMetadataExtended, $httpBackend, $scope, withoutExtendedMetadata, withExtendedMetadata;

    withoutExtendedMetadata = [{
        'flavor': 'dublincore/episode',
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
        'flavor': 'dublincore/episode',
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

    beforeEach(inject(function (_NewEventMetadataExtended_, _$httpBackend_, $rootScope) {
        NewEventMetadataExtended = _NewEventMetadataExtended_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/components.json')));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/ACL.json')));
        $httpBackend.whenGET('/workflow/definitions.json').respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));
    });

    it('is not visible if there is no extended metadata available', function () {
        $httpBackend.whenGET('/admin-ng/event/new/metadata').respond(JSON.stringify(withoutExtendedMetadata));
        $httpBackend.flush();
        expect(NewEventMetadataExtended.visible).toBeFalsy();
    });

    it('is visible if there is extended metadata available', function () {
        $httpBackend.whenGET('/admin-ng/event/new/metadata').respond(JSON.stringify(withExtendedMetadata));
        $httpBackend.flush();
        expect(NewEventMetadataExtended.visible).toBeTruthy();
    });

});

