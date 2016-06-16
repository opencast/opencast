describe('Themes Overview Controller', function () {
    var $scope, $controller, $parentScope, Table,
        ThemeResourceMock, NotificationsMock;

    ThemeResourceMock = {
        delete: jasmine.createSpy('delete')
    };

    NotificationsMock = {
        add: jasmine.createSpy('add')
    };

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        $provide.value('ThemeResource', ThemeResourceMock);
        $provide.value('Notifications', NotificationsMock);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _Table_) {
        $controller = _$controller_;
        Table = _Table_;

        $parentScope = $rootScope.$new();
        spyOn(Table, 'configure').and.callThrough();
        spyOn(Table, 'fetch').and.callThrough();
        $scope = $parentScope.$new();
        $controller('ThemesCtrl', {$scope: $scope});
    }));

    it('instantiates', function () {
        expect($scope.table).toBeDefined();
        expect($scope.table.delete).toBeDefined();
        expect(Table.configure).toHaveBeenCalled();
    });

    describe('#delete', function () {

        it('delegates the delete to the resource', function () {
            $scope.table.delete(12);
            expect(ThemeResourceMock.delete).toHaveBeenCalledWith({id: 12}, jasmine.any(Function), jasmine.any(Function));
        });

        it('reacts correctly upon success', function () {
            $scope.table.delete(12);
            ThemeResourceMock.delete.calls.mostRecent().args[1].call();
            expect(Table.fetch).toHaveBeenCalled();
            expect(NotificationsMock.add).toHaveBeenCalledWith('success', 'THEME_DELETED');
        });

        it('reacts correctly upon failure', function () {
            $scope.table.delete(12);
            ThemeResourceMock.delete.calls.mostRecent().args[2].call();
            expect(NotificationsMock.add).toHaveBeenCalledWith('error', 'THEME_NOT_DELETED', 'user-form');
        });
    });
});
