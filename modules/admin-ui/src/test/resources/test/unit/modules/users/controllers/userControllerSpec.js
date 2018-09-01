describe('User controller', function () {
    var $scope, $controller, $httpBackend, UserResource, UsersResource, Notifications, Modal;

    beforeEach(module('adminNg'));
    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate:     function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; },
            formatTime:     function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _UserResource_, _UsersResource_, _Notifications_, _Modal_) {
        $controller = _$controller_;
        UserResource = _UserResource_;
        UsersResource = _UsersResource_;
        $httpBackend = _$httpBackend_;
        Notifications = _Notifications_;
        Modal = _Modal_;
        $scope = $rootScope.$new();
    }));

    beforeEach(function () {
        $scope.action = 'edit';
        $scope.resourceId = 'matterhorn_system_account';

        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
        $httpBackend.whenGET('/admin-ng/users/users.json').respond(JSON.stringify(getJSONFixture('admin-ng/users/users.json')));
        $httpBackend.whenGET('/roles/roles.json').respond(JSON.stringify(getJSONFixture('roles/roles.json')));
        $httpBackend.whenGET('/admin-ng/users/matterhorn_system_account.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/users/matterhorn_system_account.json')));
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json?filter=role_target:USER&limit=100&offset=0').respond('{}');
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
        $controller('UserCtrl', {$scope: $scope});
        $httpBackend.flush();
    });

    it('instantiates', function () {
        expect($scope.role).toBeDefined();
    });

    it('retrieves a list of users', function () {
        expect($scope.users).toContain('admin');
    });

    describe('when editing', function () {

        describe('with an unmanageable user', function () {

            it('fetches the user', function () {
                expect($scope.user).toBeDefined();
            });

            it('adds a notification', function () {
                expect(Object.keys(Notifications.get('user-form')).length).toBe(1);
            });
        });

        describe('with a manageable user', function () {

            beforeEach(function () {
                $scope.resourceId = 'admin';
                $httpBackend.whenGET('/admin-ng/users/admin.json')
                    .respond(JSON.stringify(getJSONFixture('admin-ng/users/admin.json')));
                $controller('UserCtrl', {$scope: $scope});
                spyOn(Notifications, 'add');
                $httpBackend.flush();
            });

            it('does not add a notification', function () {
                expect(Notifications.add).not.toHaveBeenCalled();
            });
        });
    });

    describe('when creating', function () {
        beforeEach(function () {
            $scope.action = 'add';
            $controller('UserCtrl', {$scope: $scope});
        });

        it('sets the appropriate caption', function () {
            expect($scope.caption).toContain('NEW');
        });
    });

    describe('#submit', function () {

        beforeEach(function () {
            Modal.$scope = { close: jasmine.createSpy() };
            spyOn(Notifications, 'add');
        });

        describe('when editing', function () {
            beforeEach(function () {
                $scope.action = 'edit';
            });

            describe('with success', function () {
                beforeEach(function () {
                    $httpBackend.whenPUT('/admin-ng/users/test.json')
                        .respond(200);
                    $scope.user.username = 'test';
                    $scope.submit();
                    $httpBackend.flush();
                });

                it('sets the success notification', function () {
                    expect(Notifications.add).toHaveBeenCalledWith('success', 'USER_UPDATED');
                });

                it('closes the modal', function () {
                    expect(Modal.$scope.close).toHaveBeenCalled();
                });
            });

            describe('with an error', function () {
                beforeEach(function () {
                    $httpBackend.whenPUT('/admin-ng/users/test.json')
                        .respond(404);
                    $scope.user.username = 'test';
                    $scope.submit();
                    $httpBackend.flush();
                });

                it('sets the error notification', function () {
                    expect(Notifications.add).toHaveBeenCalledWith('error', 'USER_NOT_SAVED', 'user-form');
                });
            });
        });

        describe('when creating', function () {
            beforeEach(function () {
                $scope.action = 'add';
            });

            describe('with success', function () {
                beforeEach(function () {
                    $httpBackend.whenPOST('/admin-ng/users')
                        .respond(200);
                    $scope.submit();
                    $httpBackend.flush();
                });

                it('sets the success notification', function () {
                    expect(Notifications.add).toHaveBeenCalledWith('success', 'USER_ADDED');
                });

                it('closes the modal', function () {
                    expect(Modal.$scope.close).toHaveBeenCalled();
                });
            });

            describe('with an error', function () {
                beforeEach(function () {
                    $httpBackend.whenPOST('/admin-ng/users')
                        .respond(404);
                    $scope.submit();
                    $httpBackend.flush();
                });

                it('sets the error notification', function () {
                    expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String), jasmine.any(String));
                });
            });
            it('saves the user', function () {
                spyOn(UsersResource, 'create');
                $scope.submit();
                expect(UsersResource.create).toHaveBeenCalled();
            });
        });
    });

    describe('#checkUserUniqueness', function () {
        beforeEach(function () {
            $scope.userForm = {
                username: {
                    $setValidity: jasmine.createSpy()
                }
            };
            $scope.user = {};
        });

        describe('with a unique username', function () {
            beforeEach(function () {
                $scope.user.username = 'newadmin';
                $scope.checkUserUniqueness();
            });

            it('accepts validity of the form field', function () {
                expect($scope.userForm.username.$setValidity).toHaveBeenCalledWith('uniqueness', true);
            });
        });

        describe('with an existing username', function () {
            beforeEach(function () {
                $scope.user.username = 'admin';
                $scope.checkUserUniqueness();
            });

            it('rejects validity of the form field', function () {
                expect($scope.userForm.username.$setValidity).toHaveBeenCalledWith('uniqueness', false);
            });
        });
    });
});
