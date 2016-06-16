describe('Edit Status controller', function () {
    var $scope, optoutsResource, _Notifications, successfulSubmit, table, modal, rows;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        optoutsResource = {
            save: function (what, success, failure) {
                if (successfulSubmit) {
                    success({
                        error: [],
                        ok: ['1234']
                    });
                } else {
                    failure();
                }
            }
        };
        $provide.value('OptoutsResource', optoutsResource);

        rows = [
            {id: 1, selected: true},
            {id: 2, selected: true}
        ];
        table = {
            resource: 'events',
            getSelected: function () {
                return rows;
            },
            copySelected: function () {
                return rows;
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

    beforeEach(inject(function ($rootScope, $controller, Notifications) {
        $scope = $rootScope.$new();
        $scope.TableForm = {
            $valid: true
        };
        $controller('EditStatusCtrl', {$scope: $scope});
        $scope.changeStatus(true);
        _Notifications = Notifications;
    }));

    it('instantiates', function () {
        expect($scope.rows).toBeDefined();
        expect($scope.rows.length).toEqual(2);
        expect($scope.changeStatus).toBeDefined();
        expect($scope.valid).toBeDefined();
        expect($scope.submit).toBeDefined();
        expect($scope.allSelectedChanged).toBeDefined();
    });

    it('toggles the optout status', function () {
        $scope.changeStatus(true);
        expect($scope.status).toBeTruthy();
        $scope.changeStatus(false);
        expect($scope.status).toBe('false');
    });

    it('checks the validity of the request', function () {
        $scope.status = undefined;
        expect($scope.valid()).toBeFalsy();
        $scope.changeStatus(true);
        expect($scope.valid()).toBeTruthy();
        $scope.changeStatus(undefined);
        expect($scope.valid()).toBeFalsy();
        $scope.changeStatus(false);
        expect($scope.valid()).toBeTruthy();
    });


    it('submits bulk edit requests', function () {
        successfulSubmit = 'true';
        spyOn(optoutsResource, 'save').and.callThrough();
        $scope.submit();
        var expectedArgument = {
            resource: 'event',
            eventIds: [1, 2],
            optout: true
        }, actualArguments = optoutsResource.save.calls.mostRecent();
        expect(actualArguments.args[0]).toEqual(expectedArgument);
    });

    it('notifies the user upon success', function () {
        successfulSubmit = true;
        spyOn(_Notifications, 'add').and.returnValue(true);
        $scope.submit();
        expect(_Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_UPDATED_ALL');
    });


    it('notifies the user upon failure', function () {
        successfulSubmit = false;
        spyOn(_Notifications, 'add').and.returnValue(true);
        $scope.submit();
        expect(_Notifications.add).toHaveBeenCalledWith('error', 'EVENTS_NOT_UPDATED_ALL');
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

    it('deselects all table rows after success', function () {
        spyOn(table, 'deselectAll');
        successfulSubmit = true;
        $scope.submit();
        expect(table.deselectAll).toHaveBeenCalled();
    });
});
