describe('Event controller', function () {
    var $scope, $httpBackend, $controller, UsersResource, EventAccessResource, EventMetadataResource;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _UsersResource_, _EventAccessResource_, _EventMetadataResource_) {
        $scope = $rootScope.$new();
        $scope.resourceId = '40518';
        $controller = _$controller_;
        UsersResource = _UsersResource_;
        EventAccessResource = _EventAccessResource_;
        EventMetadataResource = _EventMetadataResource_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.whenGET('/admin-ng/event/40518/comments')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/comments')));
        $httpBackend.whenGET('/admin-ng/event/40518/metadata.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/metadata.json')));
        $httpBackend.whenGET('/admin-ng/event/40518/general.json').respond({});
        $httpBackend.whenGET('/admin-ng/event/40518/media.json').respond({});
        $httpBackend.whenGET('/admin-ng/event/40518/attachments.json').respond([]);
        $httpBackend.whenGET('/admin-ng/event/40518/workflows.json').respond({});
        $httpBackend.whenGET('/admin-ng/event/40518/access.json').respond({});
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond({});
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond({});
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json').respond({});

        $controller('EventCtrl', {$scope: $scope});
    });

    it('retrieves records from the server when the resource ID changes', function () {
        spyOn(EventMetadataResource, 'get');
        $scope.$emit('change', 7);
        expect(EventMetadataResource.get).toHaveBeenCalledWith({ id: 7 }, jasmine.any(Function));
    });

    describe('deleting a comment', function () {
        it('sends a DELETE request', function () {
            $httpBackend.expectDELETE('/admin-ng/event/40518/comment/2').respond({});
            $scope.deleteComment('2');
            $httpBackend.flush();
        });
    });

    describe('creating a new comment', function () {
        beforeEach(function () {
            $scope.myComment.text = 'Please help';
            $scope.myComment.reason = 'Emergency';
            $httpBackend.flush();
        });

        it('sends a POST request', function () {
            $httpBackend.expectPOST('/admin-ng/event/40518/comment', function (data) {
                if (data === $.param({ text: 'Please help', reason: 'Emergency' })) {
                    return true;
                } else {
                    return false;
                }
            }).respond({});

            $scope.comment();
            $httpBackend.flush();
        });

        it('updates existing comments', function () {
            $httpBackend.whenPOST('/admin-ng/event/40518/comment').respond({});
            $httpBackend.expectGET('/admin-ng/event/40518/comments')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/40518/comments')));
            $scope.comment();
            $httpBackend.flush();
        });
    });

    describe('replying to a comment', function () {
        beforeEach(function () {
            $scope.replyTo(getJSONFixture('admin-ng/event/40518/comments')[0]);
            $scope.myComment.text = 'My response';
        });

        it('enters reply mode', function () {
            expect($scope.replyToId).toBe(1);
        });

        it('allows exiting the reply mode', function () {
            $scope.exitReplyMode();
            expect($scope.replyToId).toBeNull();
        });

        it('sends a POST request resolving the issue', function () {
            $httpBackend.expectPOST('/admin-ng/event/40518/comment/1/reply', function (data) {
                if (data === $.param({ text: 'My response', resolved: false })) {
                    return true;
                } else {
                    return false;
                }
            }).respond({});

            $scope.reply();
            $httpBackend.flush();
        });

        it('sends a POST request keeing the issue unresolved', function () {
            $scope.myComment.resolved = false;

            $httpBackend.expectPOST('/admin-ng/event/40518/comment/1/reply', function (data) {
                if (data === $.param({ text: 'My response', resolved: false })) {
                    return true;
                } else {
                    return false;
                }
            }).respond({});

            $scope.reply();
            $httpBackend.flush();
        });
    });


    describe('retrieving metadata catalogs', function () {
        var catalogs;

        beforeEach(function () {
            catalogs = getJSONFixture('admin-ng/event/40518/metadata.json');
            $httpBackend.flush();
            $scope.$broadcast('change', 40518);
        });

        it('isolates dublincore/episode catalog', function () {
            $scope.$watch('episodeCatalog', function (newCatalog) {
                expect(newCatalog.length).toEqual(catalogs[0].length);
            });
        });

        it('prepares the extended-metadata catalogs', function () {
            $scope.metadata.$promise.then(function () {
                expect($scope.metadata.entries.length).toBe(2);
                angular.forEach($scope.metadata.entries, function (catalog, index)  {
                    expect(catalog).toEqual(catalogs[index + 1]);
                });
            });
        });

        it('realizes that a lock is active and keeps its translation key', function () {
            $scope.metadata.$promise.then(function () {
                expect($scope.metadata.locked).toEqual('EVENTS.EVENTS.DETAILS.METADATA.LOCKED.RUNNING');
            });
        });

        afterEach(function () {
            $httpBackend.flush();
        });
    });

    describe('#metadataSave', function () {
        var catalog = {
                flavor: 'dublincore/episode',
                fields: [{
                    id: 'title'
                }]
            };

        it('saves the event record', function () {
            spyOn(EventMetadataResource, 'save');
            $scope.metadataSave('title', undefined, catalog);

            expect(EventMetadataResource.save)
                .toHaveBeenCalledWith({ id: '40518'}, catalog, jasmine.any(Function));
        });

        it('marks the saved attribute as saved', function () {
            $scope.metadataSave('title', undefined, catalog);
            $httpBackend.whenPUT('/admin-ng/event/40518/metadata')
                .respond(JSON.stringify(
                    [{'flavor': 'dublincore/episode',
                         'title': 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
                         'fields': [{id: 'title'
                         }, {id: 'series'
                         }]
                    }]
                ));
            $httpBackend.flush();

            expect(catalog.fields[0].saved).toBe(true);
        });

        it('calls the provided callback on success', function () {
            var callback = jasmine.createSpy();
            $scope.metadataSave('title', callback, catalog);
            $httpBackend.whenPUT('/admin-ng/event/40518/metadata').respond(200);
            $httpBackend.flush();

            expect(callback).toHaveBeenCalled();
        });

        describe('getSaveFn', function () {
            var fn, callbackObject = {
                    callback: function () {}
                };

            beforeEach(function () {
                $httpBackend.flush();
                spyOn($scope, 'metadataSave').and.callThrough();
                spyOn(callbackObject, 'callback');
                spyOn(EventMetadataResource, 'save').and.callThrough();
                $httpBackend.expectPUT('/admin-ng/event/40518/metadata').respond(200);
            });

            it('saves fields in the dublincore/series catalog', function () {
                fn = $scope.getSaveFunction('dublincore/episode'),
                fn('title', callbackObject.callback);
                $httpBackend.flush();
                expect($scope.metadataSave).toHaveBeenCalledWith('title', callbackObject.callback, $scope.episodeCatalog);
                expect(callbackObject.callback).toHaveBeenCalled();
            });

            it('saves fields in the dublincore/extended-1 catalog', function () {
                fn = $scope.getSaveFunction('dublincore/extended-1'),
                fn('title', callbackObject.callback);
                $httpBackend.flush();
                expect($scope.metadataSave).toHaveBeenCalledWith('title', callbackObject.callback, $scope.metadata.entries[0]);
                expect(callbackObject.callback).toHaveBeenCalled();
            });
        });
    });

    describe('#accessSave', function () {

        it('saves the event access', function () {

            this.role = 'ROLE_TEST';

            $scope.access = {
                episode_access: {
                    current_acl: 123
                }
            };

            spyOn(EventAccessResource, 'save');
            $scope.accessSave.call(this);

            expect(EventAccessResource.save).toHaveBeenCalledWith({
                id: $scope.resourceId,
            }, {
                acl: { ace: [] },
                override: true
            });
        });
    });

    describe('#severityColor', function () {

        it('returns a color for each severity level', function () {
            expect($scope.severityColor('failure')).toEqual('red');
            expect($scope.severityColor('info')).toEqual('green');
            expect($scope.severityColor('warning')).toEqual('yellow');
        });
    });
});
