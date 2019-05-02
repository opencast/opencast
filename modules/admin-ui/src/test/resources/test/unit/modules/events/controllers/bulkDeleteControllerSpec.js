describe('Bulk Delete controller', function () {
    var $scope, $httpBackend, bulkDeleteResource, _Notifications, successfulSubmit, table, modal, sampleRows,
        prepareForSubmit;

    // Both published and unpublished selected, workflow selected, is valid
    sampleRows = function () {
       return [ { id: 1, selected: true, 'publications': [{ 'id': 'engage-player', 'url': 'http://engage.localdomain' }] },
            { id: 2, selected: true },
            { id: 3, selected: false, 'publications': [{ 'id': 'engage-player', 'url': 'http://engage.localdomain' }] },
            { id: 4, selected: false }];
    };

    prepareForSubmit = function () {
        $scope.rows = sampleRows;
    };

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        bulkDeleteResource = {
            delete: function (empty, what, success, failure) {
                if (successfulSubmit) {
                    success();
                } else {
                    failure();
                }
            }

        };
        $provide.value('BulkDeleteResource', bulkDeleteResource);

        table = {
            resource: 'events',
            copySelected: function () {
                return sampleRows();
            },
            getSelected: function () {
                return sampleRows();
            },
            allSelectedChanged: function () {},
            deselectAll: function () {}
        };
        $provide.value('Table', table);

        modal = {
            $scope: {
                close: function () {}
            }
        };
        $provide.value('Modal', modal);
    }));

    beforeEach(inject(function ($rootScope, $controller, _$httpBackend_, Notifications) {
        $scope = $rootScope.$new();
        $scope.close = function () {};
        $controller('BulkDeleteCtrl', {$scope: $scope});
        _Notifications = Notifications;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $httpBackend.whenGET('/i18n/languages.json').respond(JSON.stringify(getJSONFixture('i18n/languages.json')));
        $httpBackend.whenGET('public/org/opencastproject/adminui/languages/lang-en_US.json').respond(JSON.stringify({}));

        //TODO get JSON String instead of JSON Object
    });

    it('instantiates', function () {
        expect($scope.table.rows).toBeDefined();
        expect($scope.submit).toBeDefined();
        expect($scope.table.allSelectedChanged).toBeDefined();
    });

    describe('#validity', function () {
        it('is true in case everything is right', function () {
            expect($scope.table.rows.length).toEqual(4);
            expect($scope.valid()).toBeTruthy();
        });

        it('is false if no rows are selected', function () {
            $scope.table.rows[0].selected = false;
            $scope.table.rows[1].selected = false;
            expect($scope.valid()).toBeFalsy();
        });

    });

    describe('#submit', function () {

        it('submits bulk delete requests', function () {
            successfulSubmit = true;
            spyOn(bulkDeleteResource, 'delete').and.callThrough();
            $scope.submit();
            var expectedArgument = {resource: 'event', endpoint: 'deleteEvents', eventIds: [1, 2]},
                actualArguments = bulkDeleteResource.delete.calls.mostRecent();
            expect(actualArguments.args[1]).toEqual(expectedArgument);
        });

        it('notifies the user upon success', function () {
            successfulSubmit = true;
            spyOn(_Notifications, 'add').and.returnValue(true);
            $scope.submit();
            expect(_Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_DELETED');
        });

        it('notifies the user upon failure', function () {
            successfulSubmit = false;
            spyOn(_Notifications, 'add').and.returnValue(true);
            $scope.submit();
            expect(_Notifications.add).toHaveBeenCalledWith('error', 'EVENTS_NOT_DELETED');
        });

        it('closes the modal window after success', function () {
            spyOn(modal.$scope, 'close');
            successfulSubmit = true;
            $scope.submit();
            expect(modal.$scope.close).toHaveBeenCalled();
        });

        it('closes the modal window after failure', function () {
            spyOn(modal.$scope, 'close');
            successfulSubmit = false;
            $scope.submit();
            expect(modal.$scope.close).toHaveBeenCalled();
        });

        it('deselects all rows after success', function () {
            spyOn(table, 'deselectAll');
            prepareForSubmit();
            successfulSubmit = true;
            $scope.submit();
            $httpBackend.flush();
            expect(table.deselectAll).toHaveBeenCalled();
        });
    });

});
