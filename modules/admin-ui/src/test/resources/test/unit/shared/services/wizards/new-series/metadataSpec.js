describe('Metadata Step in New Series Wizard', function () {
    var NewSeriesMetadata, $httpBackend, $scope, setParams;

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

    beforeEach(inject(function (_NewSeriesMetadata_, _$httpBackend_, $rootScope) {
        NewSeriesMetadata = _NewSeriesMetadata_;
        $httpBackend = _$httpBackend_;
        $scope = $rootScope.$new();
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/series/new/metadata').respond(JSON.stringify(getJSONFixture('admin-ng/series/new/metadata')));
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/components.json')));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond(JSON.stringify(getJSONFixture('admin-ng/capture-agents/agents.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond(JSON.stringify(getJSONFixture('admin-ng/resources/ACL.json')));
        $httpBackend.whenGET('/workflow/definitions.json').respond(JSON.stringify(getJSONFixture('workflow/definitions.json')));
        $httpBackend.flush();
    });

    it('fetches the metadata for the metadata tab', function () {
        expect(NewSeriesMetadata).toBeDefined();
        expect(NewSeriesMetadata.metadata['dublincore/series'].fields.length).toEqual(7);
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
            setParams(NewSeriesMetadata, 'testid', 'testvalue');
            NewSeriesMetadata.save($scope);
            expect(NewSeriesMetadata.ud['dublincore/series'].fields.testid.value).toEqual('testvalue');
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
                    NewSeriesMetadata.save($scope);
                    expect(NewSeriesMetadata.ud['dublincore/series'].fields.testid.value).toEqual(['a', 'b']);
                });
            });

            describe('with a string value', function () {
                beforeEach(function () {
                    $scope.$parent.params.value = 'a';
                });

                it('assign the entire values array', function () {
                    NewSeriesMetadata.save($scope);
                    expect(NewSeriesMetadata.ud['dublincore/series'].fields.testid.value).toEqual('a');
                });
            });
        });
    });

    describe('#isValid', function () {

        it('is invalid if no user entries are made', function () {
            expect(NewSeriesMetadata.isValid()).toBeFalsy();
        });

        it('invalidates if a required field is deleted', function () {
            setParams(NewSeriesMetadata, 'title', 'testTitle');
            setParams(NewSeriesMetadata, 'presenters', '[Mark Twain]');
            setParams(NewSeriesMetadata, 'subject', 'test subject');
            expect(NewSeriesMetadata.isValid()).toBeTruthy();
            setParams(NewSeriesMetadata, 'title', '');
            expect(NewSeriesMetadata.isValid()).toBeFalsy();
        });

        it('becomes valid when all required fields are set', function () {
            setParams(NewSeriesMetadata, 'title', 'testTitle');
            setParams(NewSeriesMetadata, 'presenters', '[Mark Twain]');
            setParams(NewSeriesMetadata, 'subject', 'test subject');
            expect(NewSeriesMetadata.isValid()).toBeTruthy();
        });
    });

    describe('#findRequiredMetadata', function () {

        describe('without metadata', function () {
            beforeEach(function () {
                NewSeriesMetadata.requiredMetadata = { a: 'b' };
            });

            it('does not set required metadata', function () {
                NewSeriesMetadata.findRequiredMetadata({});
                expect(NewSeriesMetadata.requiredMetadata).toEqual({ a: 'b' });
            });
        });
    });

    describe('#getUserEntries', function () {
        beforeEach(function () {
            NewSeriesMetadata.ud['dublincore/series'].fields = { foo: 'bar' };
        });

        it('returns the user data', function () {
            expect(NewSeriesMetadata.getUserEntries()).toEqual({ foo: 'bar' });
        });
    });
});
