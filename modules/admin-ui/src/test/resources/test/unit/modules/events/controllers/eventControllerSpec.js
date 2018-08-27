describe('Event controller', function () {
    var $scope, $httpBackend, $controller, UsersResource, EventAccessResource, EventMetadataResource, Notifications;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; },
            formatDateTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _UsersResource_, _EventAccessResource_, _EventMetadataResource_, _Notifications_) {
        $scope = $rootScope.$new();
        $scope.resourceId = '1a2a040b-ef73-4323-93dd-052b86036b75';
        $controller = _$controller_;
        UsersResource = _UsersResource_;
        EventAccessResource = _EventAccessResource_;
        EventMetadataResource = _EventMetadataResource_;
        Notifications = _Notifications_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(function () {
        jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';

        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata.json')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/publications.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/media/media.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/attachment/attachments.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/catalog/catalogs.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/assets.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/scheduling.json').respond(JSON.stringify({"metadata": {"start":"","end":""}}));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/workflows.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/access.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/access.json')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/participation.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/eventUploadAssetOptions.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/resources/eventUploadAssetOptions.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ACL.ACTIONS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ROLES.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/new/processing?tags=schedule')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/new/processing')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/hasActiveTransaction').respond('false');
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond(JSON.stringify({"results":[],"total":0}));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json?inputs=true')
            .respond(JSON.stringify({"results":[],"total":0}));
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));

        $controller('EventCtrl', {$scope: $scope});
    });

    it('retrieves records from the server when the resource ID changes', function () {
        spyOn(EventMetadataResource, 'get');
        $scope.$emit('change', 7);
        expect(EventMetadataResource.get).toHaveBeenCalledWith({ id: 7 }, jasmine.any(Function));
    });

    describe('deleting a comment', function () {
        it('sends a DELETE request', function () {
            $httpBackend.expectDELETE('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment/2').respond('{}');
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
            $httpBackend.expectPOST('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment', function (data) {
                if (data === $.param({ text: 'Please help', reason: 'Emergency' })) {
                    return true;
                } else {
                    return false;
                }
            }).respond('{}');

            $scope.comment();
            $httpBackend.flush();
        });

        it('updates existing comments', function () {
            $httpBackend.whenPOST('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment').respond('{}');
            $httpBackend.expectGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')
                .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')));
            $scope.comment();
            $httpBackend.flush();
        });
    });

    describe('replying to a comment', function () {
        beforeEach(function () {
            $scope.replyTo(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')[0]);
            $scope.myComment.text = 'My response';
        });

        it('enters reply mode', function () {
            expect($scope.replyToId).toBe(1902);
        });

        it('allows exiting the reply mode', function () {
            $scope.exitReplyMode();
            expect($scope.replyToId).toBeNull();
        });

        it('sends a POST request resolving the issue', function () {
            $httpBackend.expectPOST('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment/1902/reply', function (data) {
                if (data === $.param({ text: 'My response', resolved: false })) {
                    return true;
                } else {
                    return false;
                }
            }).respond('{}');

            $scope.reply();
            $httpBackend.flush();
        });

        it('sends a POST request keeing the issue unresolved', function () {
            $scope.myComment.resolved = false;

            $httpBackend.expectPOST('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment/1902/reply', function (data) {
                if (data === $.param({ text: 'My response', resolved: false })) {
                    return true;
                } else {
                    return false;
                }
            }).respond('{}');
            $scope.reply();
            $httpBackend.flush();
        });
    });


    describe('retrieving metadata catalogs', function () {
        var catalogs;

        beforeEach(function () {
            catalogs = getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata.json');
            $httpBackend.flush();
            $scope.$broadcast('change', '1a2a040b-ef73-4323-93dd-052b86036b75');
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
                .toHaveBeenCalledWith({ id: '1a2a040b-ef73-4323-93dd-052b86036b75'}, catalog, jasmine.any(Function));
        });

        it('marks the saved attribute as saved', function () {
            $scope.metadataSave('title', undefined, catalog);
            $httpBackend.whenPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata')
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
            $httpBackend.whenPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
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
                $httpBackend.expectPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
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

            $scope.policies = [];
            $scope.policies[0] = {
                role  : 'admin',
                read  : true,
                write : true,
                actions : {
                    value : []
                }
            };

            spyOn(EventAccessResource, 'save');
            $scope.accessSave.call(this);

            expect(EventAccessResource.save).toHaveBeenCalledWith({
                id: $scope.resourceId,
            }, {
                acl: { ace: [ { action : 'read', allow : true, role : 'admin' }, { action : 'write', allow : true, role : 'admin' } ] },
                override: true
            }, jasmine.any(Function), jasmine.any(Function));
        });
    });

    describe('#severityColor', function () {

        it('returns a color for each severity level', function () {
            expect($scope.severityColor('failure')).toEqual('red');
            expect($scope.severityColor('info')).toEqual('green');
            expect($scope.severityColor('warning')).toEqual('yellow');
        });
    });

    describe('#workflowAction', function () {
        beforeEach(function () {
            spyOn(Notifications, 'add');
            $scope.close = jasmine.createSpy();
        });

        describe('on success', function () {
            beforeEach(function () {
            	$httpBackend.expectPUT(/\/admin-ng\/event\/.+\/workflows\/.+\/action\/.+/g).respond(200, '{}');
            });

            it('resumes workflow, shows notification, closes', function () {
                $scope.workflowAction(1234, 'RETRY'); // wfId
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
                expect($scope.close).toHaveBeenCalled();
            });

            it('aborts workflow, shows notification, closes', function () {
                $scope.workflowAction(1234, 'NONE'); // wfId
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('success', jasmine.any(String));
                expect($scope.close).toHaveBeenCalled();
            });
        });

        describe('on error', function () {
            beforeEach(function () {
            	$httpBackend.expectPUT(/\/admin-ng\/event\/.+\/workflows\/.+\/action\/.+/g).respond(500, '{}');
            });

            it('shows notification', function () {
                $scope.workflowAction(1234, 'RETRY'); // wfId
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String), 'events-access');
            });

            it('shows notification', function () {
                $scope.workflowAction(1234, 'NONE'); // wfId
                $httpBackend.flush();

                expect(Notifications.add).toHaveBeenCalledWith('error', jasmine.any(String), 'events-access');
            });
        });
    });

});
