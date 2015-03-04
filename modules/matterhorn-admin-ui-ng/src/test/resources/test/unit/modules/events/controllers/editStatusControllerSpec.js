describe('Edit Status controller', function () {
    var $scope, optoutsResource, _Notifications, successfulSubmit, table, modal;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        optoutsResource = {
            save: function (what, success, failure) {
                if (successfulSubmit) {
                    success();
                } else {
                    failure();
                }
            }
        };
        $provide.value('OptoutsResource', optoutsResource);

        table = {
            resource: 'events',
            getSelected: function () {
                return [{id: 1}, {id: 2}];
            },
            toggleAllSelectionFlags: function () {}
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
        expect($scope.toggleSelectAll).toBeDefined();
    });

    it('toggles the optout status', function () {
        $scope.changeStatus(true);
        expect($scope.status).toBeTruthy();
        $scope.changeStatus(false);
        expect($scope.status).toBeFalsy();
    });

    it('toggles the table selection', function () {
        spyOn(table, 'toggleAllSelectionFlags').and.callThrough();
        $scope.toggleSelectAll();
        expect(table.toggleAllSelectionFlags).toHaveBeenCalled();
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
        successfulSubmit = true;
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
        expect(_Notifications.add).toHaveBeenCalledWith('success', 'EVENTS_UPDATED');
    });


    it('notifies the user upon failure', function () {
        successfulSubmit = false;
        spyOn(_Notifications, 'add').and.returnValue(true);
        $scope.submit();
        expect(_Notifications.add).toHaveBeenCalledWith('error', 'EVENTS_NOT_UPDATED');
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
});
