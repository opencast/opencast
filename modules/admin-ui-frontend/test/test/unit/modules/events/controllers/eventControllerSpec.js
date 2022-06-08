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

    beforeEach(inject(function ($rootScope, _$controller_, _$httpBackend_, _UsersResource_, _EventAccessResource_,
      _EventMetadataResource_, _Notifications_) {
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

        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comments')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata.json')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/publications.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/media/media.json')
            .respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/attachment/attachments.json')
            .respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/catalog/catalogs.json')
            .respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/publication/publications.json')
            .respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/asset/assets.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/scheduling.json')
            .respond(JSON.stringify({"metadata": {"start":"","end":""}}));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/workflows.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/access.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/access.json')));
        $httpBackend.whenGET('/admin-ng/resources/components.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/eventUploadAssetOptions.json')
            .respond(JSON.stringify(getJSONFixture('admin-ng/resources/eventUploadAssetOptions.json')));
        $httpBackend.whenGET('/admin-ng/resources/ACL.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ACL.ACTIONS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/ACL.DEFAULTS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/acl/roles.json?target=ACL&limit=-1').respond('[]');
        $httpBackend.whenGET('/admin-ng/resources/PUBLICATION.CHANNELS.json').respond('{}');
        $httpBackend.whenGET('/admin-ng/event/new/processing?tags=schedule')
            .respond(JSON.stringify(getJSONFixture('admin-ng/event/new/processing')));
        $httpBackend.whenGET('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/hasActiveTransaction')
            .respond('false');
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json').respond(JSON.stringify({"results":[],"total":0}));
        $httpBackend.whenGET('/admin-ng/capture-agents/agents.json?inputs=true')
            .respond(JSON.stringify({"results":[],"total":0}));
        $httpBackend.whenGET('/info/me.json').respond(JSON.stringify(getJSONFixture('info/me.json')));
        // Until we're actually testing the statistics endpoint, just return an empty set here
        $httpBackend.whenGET(/\/admin-ng\/statistics.*/).respond('[]');
        $httpBackend.whenPOST(/\/admin-ng\/statistics.*/).respond('[]');


        $controller('EventCtrl', {$scope: $scope});
    });

    it('fetches event metadata', function () {
        expect($scope.metadata.entries).toBeUndefined();
        expect($scope.commonMetadataCatalog).toBeUndefined();
        expect($scope.extendedMetadataCatalogs).toBeUndefined();
        $httpBackend.flush();
        expect($scope.metadata.entries).toBeDefined();
        expect($scope.metadata.entries.length).toBe(3);
        expect($scope.commonMetadataCatalog).toBeDefined();
        expect($scope.extendedMetadataCatalogs).toBeDefined();
        expect($scope.extendedMetadataCatalogs.length).toBe(2);
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
            $httpBackend.expectPOST('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment/1902/reply',
              function (data) {
                if (data === $.param({ text: 'My response', resolved: false })) {
                    return true;
                } else {
                    return false;
                }
            }).respond('{}');

            $scope.reply();
            $httpBackend.flush();
        });

        it('sends a POST request keeping the issue unresolved', function () {
            $scope.myComment.resolved = false;

            $httpBackend.expectPOST('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/comment/1902/reply',
              function (data) {
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

        it('extracts the common metadata catalog', function () {
            $scope.$watch('commonMetadataCatalog', function (newCatalog) {
                expect(newCatalog.flavor).toEqual(catalogs[0].flavor);
            });
        });

        it('extracts the extended metadata catalogs', function () {
            $scope.metadata.$promise.then(function () {
                expect($scope.extendedMetadataCatalogs.length).toBe(2);
                angular.forEach($scope.extendedMetadataCatalogs, function (newCatalog, index)  {
                    expect(newCatalog.flavor).toEqual(catalogs[index + 1].flavor);
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

    describe('#unsavedChanges', function() {
        it('has unsaved changes', function () {

            expect($scope.unsavedChanges([{fields: [{dirty: true}]}])).toBe(true);
            expect($scope.unsavedChanges([{fields: [{dirty: false}, {dirty: true}]},
              {fields: [{dirty: false}]}])).toBe(true);
           expect($scope.unsavedChanges([{fields: [{dirty: true}]},
              {fields: [{dirty: true}]}])).toBe(true);
        });

        it('doesn\'t have unsaved changes', function () {
            expect($scope.unsavedChanges([{fields: [{dirty: false}]}])).toBe(false);
            expect($scope.unsavedChanges([{fields: [{dirty: false}, {dirty: false}]},
              {fields: [{dirty: false}]}])).toBe(false);
        });
    });

    describe('#metadataChanged', function () {
        var fn, callbackObject = {
            callback: function () {}
        };

        beforeEach(function () {
            spyOn($scope, 'metadataChanged').and.callThrough();
            spyOn(callbackObject, 'callback');
            $httpBackend.flush();
        });


        it('does\'t mark fields dirty when value hasn\'t changed', function () {
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(false);
            fn = $scope.getMetadataChangedFunction('dublincore/episode'),
            fn('title', callbackObject.callback);
            expect($scope.metadataChanged).toHaveBeenCalledWith('title', callbackObject.callback,
              $scope.commonMetadataCatalog);
            expect(callbackObject.callback).toHaveBeenCalled();
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(false);
            expect($scope.commonMetadataCatalog.fields[0].dirty).toBe(false);
        });

        it('marks field in the common metadata catalog as dirty', function () {
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(false);
            $scope.commonMetadataCatalog.fields[0].value = "New Title";
            fn = $scope.getMetadataChangedFunction('dublincore/episode'),
            fn('title', callbackObject.callback);
            expect($scope.metadataChanged).toHaveBeenCalledWith('title', callbackObject.callback,
              $scope.commonMetadataCatalog);
            expect(callbackObject.callback).toHaveBeenCalled();
            expect($scope.unsavedChanges([$scope.commonMetadataCatalog])).toBe(true);
            expect($scope.commonMetadataCatalog.fields[0].dirty).toBe(true);
        });

        it('marks field in the extended metadata catalog as dirty', function () {
            expect($scope.unsavedChanges($scope.extendedMetadataCatalogs)).toBe(false);
            $scope.extendedMetadataCatalogs[0].fields[0].value = "New Title";
            fn = $scope.getMetadataChangedFunction('dublincore/extended-1'),
            fn('title', callbackObject.callback);
            expect($scope.metadataChanged).toHaveBeenCalledWith('title', callbackObject.callback,
              $scope.extendedMetadataCatalogs[0]);
            expect(callbackObject.callback).toHaveBeenCalled();
            expect($scope.unsavedChanges($scope.extendedMetadataCatalogs)).toBe(true);
            expect($scope.extendedMetadataCatalogs[0].fields[0].dirty).toBe(true);
        });
    });

    describe('#metadataSave', function () {

        beforeEach(function () {
            spyOn(EventMetadataResource, 'save').and.callThrough();
        });

        it('doesn\'t save when no field marked as dirty', function () {
            var catalog = {fields: [{ dirty: false }]};
            $scope.metadataSave([catalog]);
            expect(EventMetadataResource.save).not.toHaveBeenCalled();
        });

        it('saves when field marked as dirty', function () {
            var catalog = {fields: [{ dirty: true }]};
            $scope.metadataSave([catalog]);
            expect(EventMetadataResource.save).toHaveBeenCalledWith({id: '1a2a040b-ef73-4323-93dd-052b86036b75'},
              catalog, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].dirty).toBe(false);
        });

        it('resets old value when saving', function () {
            var catalog = {fields: [{ dirty: true, value: 'blub', oldValue: 'blah' }]};
            $scope.metadataSave([catalog]);
            expect(EventMetadataResource.save).toHaveBeenCalledWith({id: '1a2a040b-ef73-4323-93dd-052b86036b75'},
              catalog, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].value).toBe('blub');
            expect(catalog.fields[0].oldValue).toBe('blub');
        });

        it('saves multiple catalogs', function () {
            var catalog = {fields: [{ dirty: true }]};
            var catalog2 = {fields: [{ dirty: true }]};
            $scope.metadataSave([catalog, catalog2]);
            expect(EventMetadataResource.save).toHaveBeenCalledWith({id: '1a2a040b-ef73-4323-93dd-052b86036b75'},
              catalog, jasmine.any(Function));
            expect(EventMetadataResource.save).toHaveBeenCalledWith({id: '1a2a040b-ef73-4323-93dd-052b86036b75'},
              catalog2, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
            $httpBackend.expectPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].dirty).toBe(false);
            expect(catalog2.fields[0].dirty).toBe(false);
        });

        it('saves one of multiple catalogs', function () {
            var catalog = {fields: [{ dirty: true, value: 'blub', oldValue: 'blah' }]};
            var catalog2 = {fields: [{ dirty: false, value: 'blub', oldValue: 'blah' }]};
            $scope.metadataSave([catalog, catalog2]);
            expect(EventMetadataResource.save).toHaveBeenCalledWith({id: '1a2a040b-ef73-4323-93dd-052b86036b75'},
              catalog, jasmine.any(Function));
            expect(EventMetadataResource.save).not.toHaveBeenCalledWith({id: '1a2a040b-ef73-4323-93dd-052b86036b75'},
              catalog2, jasmine.any(Function));
            $httpBackend.expectPUT('/admin-ng/event/1a2a040b-ef73-4323-93dd-052b86036b75/metadata').respond(200);
            $httpBackend.flush();
            expect(catalog.fields[0].dirty).toBe(false);
            expect(catalog.fields[0].oldValue).toBe('blub');
            expect(catalog2.fields[0].oldValue).toBe('blah');
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
                acl: { ace: [ { action : 'read', allow : true, role : 'admin' },
                  { action : 'write', allow : true, role : 'admin' } ] },
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
