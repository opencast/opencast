describe('Metadata Step in New Event Wizard', function () {
    var NewEventMetadata, $httpBackend, $scope, setParams;

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

    beforeEach(inject(function (_NewEventMetadata_, _$httpBackend_, $rootScope) {
        NewEventMetadata = _NewEventMetadata_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/event/new/metadata').respond(JSON.stringify(getJSONFixture('admin-ng/event/new/metadata')));
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/components.json')));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/ACL.json')));
        $httpBackend.whenGET('/workflow/definitions.json').respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));
        $httpBackend.flush();
    });

    it('fetches the metadata for the metadata tab', function () {
        expect(NewEventMetadata).toBeDefined();
        expect(NewEventMetadata.metadata['dublincore/episode'].fields.length).toEqual(9);
    });

    setParams = function (ctrl, id, value) {
        $scope.$parent.params = {
            id: id,
            value: value
        };
        ctrl.save($scope);
    };

    describe('#save', function () {

        it('saves new values', function () {
            setParams(NewEventMetadata, 'testid', 'testvalue');
            NewEventMetadata.save($scope);
            expect(NewEventMetadata.ud['dublincore/episode'].fields.testid.value).toEqual('testvalue');
        });

        describe('with collection', function () {
            beforeEach(function () {
                $scope.$parent.params = { collection: [], id: 'testid' };
            });

            describe('with an array of values', function () {
                beforeEach(function () {
                    $scope.$parent.params.value = ['a', 'b'];
                });

                it('assign the entire values array', function () {
                    NewEventMetadata.save($scope);
                    expect(NewEventMetadata.ud['dublincore/episode'].fields.testid.value).toEqual(['a', 'b']);
                });
            });

            describe('with a string value', function () {
                beforeEach(function () {
                    $scope.$parent.params.value = 'a';
                });

                it('assign the entire values array', function () {
                    NewEventMetadata.save($scope);
                    expect(NewEventMetadata.ud['dublincore/episode'].fields.testid.value).toEqual('a');
                });
            });
        });
    });

    describe('#isValid', function () {

        it('is invalid if no user entries are made', function () {
            expect(NewEventMetadata.isValid()).toBeFalsy();
        });

        it('invalidates if a required field is deleted', function () {
            setParams(NewEventMetadata, 'title', 'testTitle');
            setParams(NewEventMetadata, 'presenters', '[Mark Twain]');
            setParams(NewEventMetadata, 'subject', 'test subject');
            expect(NewEventMetadata.isValid()).toBeTruthy();
            setParams(NewEventMetadata, 'title', '');
            expect(NewEventMetadata.isValid()).toBeFalsy();
        });

        it('becomes valid when all required fields are set', function () {
            setParams(NewEventMetadata, 'title', 'testTitle');
            setParams(NewEventMetadata, 'presenters', '[Mark Twain]');
            setParams(NewEventMetadata, 'subject', 'test subject');
            expect(NewEventMetadata.isValid()).toBeTruthy();
        });
    });

    describe('#findRequiredMetadata', function () {

        describe('without metadata', function () {
            beforeEach(function () {
                NewEventMetadata.requiredMetadata = { a: 'b' };
            });

            it('does not set required metadata', function () {
                NewEventMetadata.findRequiredMetadata({});
                expect(NewEventMetadata.requiredMetadata).toEqual({ a: 'b' });
            });
        });
    });

    describe('#getUserEntries', function () {
        beforeEach(function () {
            NewEventMetadata.ud['dublincore/episode'].fields = { foo: 'bar' };
        });

        it('returns the user data', function () {
            expect(NewEventMetadata.getUserEntries()).toEqual({ foo: 'bar' });
        });
    });
});
