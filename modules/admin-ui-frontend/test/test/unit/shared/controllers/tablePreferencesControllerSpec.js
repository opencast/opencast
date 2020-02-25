describe('Table Preferences Controller', function () {
    var $scope, Table, Storage, fakeOptions, $controller, $provide;
    fakeOptions = {
        'caption': 'RECORDINGS.RECORDINGS.TABLE.CAPTION',
        'category': 'recordings',
        'columns': [
            {
                'label': 'RECORDINGS.RECORDINGS.TABLE.STATUS',
                'name': 'status'
            },
            {
                'label': 'RECORDINGS.RECORDINGS.TABLE.NAME',
                'name': 'name'
            },
            {
                'label': 'RECORDINGS.RECORDINGS.TABLE.UPDATED',
                'name': 'updated'
            },
            {
                'label': 'USERS.USERS.TABLE.BLACKLIST_FROM',
                'name': 'blacklist_from'
            },
            {
                'label': 'USERS.USERS.TABLE.BLACKLIST_TO',
                'name': 'blacklist_to'
            }
        ],
        'resource': 'recordings',
        apiService: {
            query: function () {
                return {
                    $promise: {
                        then: function () {}
                    }
                };
            }
        }
    };

    beforeEach(module('adminNg'));
    beforeEach(module(function (_$provide_) {
        var locationMock = {
            path: function () {
                return '/events/series';
            },
            search: function () {
                return {};
            }
        };
        $provide = _$provide_;
        $provide.value('$location', locationMock);
    }));
    
    beforeEach(inject(function (_$rootScope_, _$controller_) {
        $controller = _$controller_;
        $scope = _$rootScope_.$new();
    }));

    describe('Basic functionality', function () {

        beforeEach(inject(function (_Table_, _Storage_) {
            Table = _Table_;
            Table.configure(fakeOptions);
            Storage = _Storage_;
            $controller('TablePreferencesCtrl', {$scope: $scope});
        }));

        
        it('defaults all columns to be active', function () {
            angular.forEach(Table.columns, function (column) {
                expect(column.deactivated).toBeFalsy();
            });
        });

        it('correctly subdivides the deactivated from the active columns', function () {
            expect($scope.deactivatedColumns.length).toEqual(0);
            expect($scope.activeColumns.length).toEqual(5);
            $scope.changeColumn(Table.columns[0], true);
            expect(Table.columns[0].deactivated).toBeTruthy();
            expect($scope.deactivatedColumns.length).toEqual(1);
        });

    });

    describe('Local storage persistence', function () {

        beforeEach(inject(function (_Table_, _Storage_) {
            Table = _Table_;
            Table.configure(fakeOptions);
            Storage = _Storage_;
            $controller('TablePreferencesCtrl', {$scope: $scope});
        }));


        it('stores changes to the local storage on save', function () {
            spyOn(Storage, 'put').and.callThrough();
            $scope.save();
            expect(Storage.put).toHaveBeenCalled();
        });

    });
});
