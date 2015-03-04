describe('ResourceModal service', function () {
    var $location, $rootScope, ResourceModal, Modal, EventMediaDetailsResource, Table;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services.modal'));
    beforeEach(module('ngResource'));
    beforeEach(module('LocalStorageModule'));

    beforeEach(inject(function (_$location_, _$rootScope_, _ResourceModal_, _Modal_, _EventMediaDetailsResource_, _Table_) {
        $rootScope = _$rootScope_;
        $location = _$location_;
        ResourceModal = _ResourceModal_;
        EventMediaDetailsResource = _EventMediaDetailsResource_;
        Modal = _Modal_;
        Table = _Table_;
    }));

    it('returns the modal object', function () {
        expect(ResourceModal.show).toBeDefined();
    });

    describe('#show', function () {

        describe('without a resource', function () {
            beforeEach(function () {
                Modal.modal = $('<div></div>');
                spyOn(Modal, 'show').and.returnValue(null);
            });

            it('does nothing', function () {
                ResourceModal.show('test-modal', 72, 'main', 'edit');

                expect(ResourceModal.$scope).toBeUndefined();
            });
        });

        describe('with a resource', function () {
            beforeEach(function () {
                Modal.modal = $('<div></div>');
                Modal.$scope = {};
                spyOn(Modal, 'show').and.returnValue({ then: function (callback) {
                    callback.apply(ResourceModal);
                }});
            });

            it('creates a modal', function () {
                ResourceModal.show('test-modal', 72, 'main', 'edit');
                expect(Modal.show).toHaveBeenCalledWith('test-modal');
            });

            it('sets properties', function () {
                ResourceModal.show('test-modal', 72, 'main', 'edit');

                expect(Modal.$scope.resourceId).toBe(72);
                expect(Modal.$scope.action).toEqual('edit');
            });

            it('sets the location', function () {
                spyOn($location, 'search').and.returnValue({});
                ResourceModal.show('test-modal', 72, 'main', 'edit');
                expect($location.search).toHaveBeenCalledWith({ resourceId: 72, action: 'edit' });
            });

            describe('with tabs', function () {
                beforeEach(function () {
                    Modal.modal = $('<div><div id="modal-nav"><a href="dummy">dummy</a></div></div>');
                });

                it('opens the default tab', function () {
                    spyOn(ResourceModal, 'openTab');
                    ResourceModal.show('test-modal', 72, 'main', 'edit');

                    expect(ResourceModal.openTab).toHaveBeenCalledWith('main', true);
                });

                describe('and with breadcrumbs', function () {
                    beforeEach(function () {
                        $location.search({
                            breadcrumbs: JSON.stringify([{
                                level:   2,
                                label:   'FIRST_TITLE',
                                id:      'workflow-details',
                                api:     'EventWorkflowsResource',
                                subId:   92371,
                                sibling: false
                            }])
                        });
                        spyOn(ResourceModal, 'openSubTab');
                    });

                    it('restores sub tabs', function () {
                        ResourceModal.show('test-modal', 72, 'main', 'edit');
                        expect(ResourceModal.openSubTab).toHaveBeenCalledWith('workflow-details', 'EventWorkflowsResource', 92371, false);

                    });
                });
            });

            describe('with an already active tab', function () {
                beforeEach(function () {
                    Modal.modal = $('<div><div id="modal-nav"><a class="active" href="dummy">dummy</a></div></div>');
                });

                it('does not switch tabs', function () {
                    spyOn(ResourceModal, 'openTab');
                    ResourceModal.show('test-modal', 72, 'main', 'edit');

                    expect(ResourceModal.openTab).not.toHaveBeenCalled();
                });
            });
        });
    });

    describe('#openTab', function () {
        var newTab, oldTab;

        beforeEach(function () {
            Modal.modal = $('<div></div>');
            ResourceModal.$scope = { breadcrumbs: [] };
            oldTab = $('<div id="modal-nav"><a data-modal-tab="2" class="active"></a></div>');
            newTab = $('<div id="modal-nav"><a data-modal-tab="4"></a></div>');
            Modal.modal.append(newTab);
            Modal.modal.append(oldTab);
        });

        it('activates the new tab', function () {
            ResourceModal.openTab(4);
            expect(ResourceModal.$scope.tab).toBe(4);
        });

        it('determines the tab ID from the DOM if none is given', function () {
            expect(newTab.find('a')).not.toHaveClass('active');
            ResourceModal.openTab();
            expect(newTab.find('a')).toHaveClass('active');
        });
    });

    describe('#openSubTab', function () {
        var newTab, oldTab;

        beforeEach(function () {
            spyOn(ResourceModal, 'generateBreadcrumbs');
            ResourceModal.$scope = { breadcrumbs: [] };
            Modal.modal = $('<div></div>');
            oldTab = $('<div data-modal-tab-content="2" class="modal-content active" data-level="2" data-label="FIRST_TITLE">');
            newTab = $('<div data-modal-tab-content="4" class="modal-content" data-level="3" data-label="SECOND_TITLE">');
            Modal.modal.append(newTab);
            Modal.modal.append(oldTab);
        });

        it('activates the new sub tab', function () {
            expect(newTab).not.toHaveClass('active');
            ResourceModal.openSubTab(4);
            expect(newTab).toHaveClass('active');
        });

        it('deactivates the old sub tab', function () {
            expect(oldTab).toHaveClass('active');
            ResourceModal.openSubTab(4);
            expect(oldTab).not.toHaveClass('active');
        });

        describe('with breadcrumbs', function () {
            beforeEach(function () {
                ResourceModal.$scope.breadcrumbs = [{
                    level:   2,
                    label:   'FIRST_TITLE',
                    id:      'workflow-details',
                    api:     'EventWorkflowsResource',
                    subId:   92371,
                    sibling: false
                }];
                spyOn(ResourceModal, 'loadSubNavData');
            });

            it('restores sub tabs', function () {
                ResourceModal.openSubTab(4);
                expect(ResourceModal.loadSubNavData).toHaveBeenCalled();
            });
        });
    });

    describe('#showAdjacent', function () {
        beforeEach(function () {
            ResourceModal.$scope = { $broadcast: jasmine.createSpy() };
            Table.rows = [{
                id: 1
            }, {
                id: 2
            }, {
                id: 3
            }];
        });

        it('broadcasts an event', function () {
            ResourceModal.$scope.resourceId = 2;
            ResourceModal.showAdjacent();
            expect(ResourceModal.$scope.$broadcast).toHaveBeenCalledWith('change', 3);
        });

        it('sets the next adjacent id', function () {
            ResourceModal.$scope.resourceId = 2;
            ResourceModal.showAdjacent();
            expect(ResourceModal.$scope.resourceId).toBe(3);
        });

        it('sets the previous adjacnet id', function () {
            ResourceModal.$scope.resourceId = 2;
            ResourceModal.showAdjacent(true);
            expect(ResourceModal.$scope.resourceId).toBe(1);
        });

        it('does not change the id if there is no adjacent recrod', function () {
            ResourceModal.$scope.resourceId = 3;
            ResourceModal.showAdjacent();
            expect(ResourceModal.$scope.resourceId).toBe(3);
        });
    });

    describe('#generateBreadcrumbs', function () {
        beforeEach(function () {
            Modal.modal = $('<div></div>');
            ResourceModal.$scope = { breadcrumbs: [] };
            Modal.modal.append('<nav id="breadcrumb"></nav>');
            Modal.modal.append('<div data-modal-tab-content="workflow-details" data-level="2" data-label="FIRST_TITLE">');
            Modal.modal.append('<div data-modal-tab-content="workflow-operations" data-level="3" data-label="SECOND_TITLE">');
        });

        describe('without existing breadcrumbs', function () {
            beforeEach(function () {
                ResourceModal.generateBreadcrumbs('workflow-details');
            });

            it('appends a new breadcrumb', function () {
                expect(ResourceModal.$scope.breadcrumbs.length).toBe(1);
                expect(Modal.modal.find('#breadcrumb a'))
                    .toHaveText('FIRST_TITLE');
            });
        });

        describe('with existing breadcrumbs', function () {
            beforeEach(function () {
                ResourceModal.$scope.breadcrumbs.push({
                    level: 2,
                    label: 'FIRST_TITLE',
                    id: 'workflow-details'
                });
            });

            it('appends a new breadcrumb', function () {
                ResourceModal.generateBreadcrumbs('workflow-operations');

                expect(ResourceModal.$scope.breadcrumbs.length).toBe(2);
                expect(Modal.modal.find('#breadcrumb a:first'))
                    .toHaveText('FIRST_TITLE');
                expect(Modal.modal.find('#breadcrumb a:last'))
                    .toHaveText('SECOND_TITLE');
            });

            describe('navigating back', function () {
                beforeEach(function () {
                    ResourceModal.$scope.breadcrumbs.push({
                        level: 3,
                        label: 'SECOND_TITLE',
                        id: 'workflow-operations'
                    });
                });

                it('reduces the breadcrumbs accordingly', function () {
                    ResourceModal.generateBreadcrumbs('workflow-details');

                    expect(ResourceModal.$scope.breadcrumbs.length).toBe(1);
                    expect(Modal.modal.find('#breadcrumb a'))
                        .toHaveText('FIRST_TITLE');
                });
            });
        });

        describe('restoring breadcrumbs from the URL', function () {
            beforeEach(function () {
                var previous = [{
                    level: 2,
                    label: 'FIRST_TITLE',
                    id: 'workflow-details'
                }, {
                    level: 3,
                    label: 'SECOND_TITLE',
                    id: 'workflow-operations'
                }];
                ResourceModal.generateBreadcrumbs('workflow-operations', previous);
            });

            it('restores the breadcrumbs', function () {
                expect(ResourceModal.$scope.breadcrumbs.length).toBe(2);
                expect(Modal.modal.find('#breadcrumb a:first'))
                    .toHaveText('FIRST_TITLE');
                expect(Modal.modal.find('#breadcrumb a:last'))
                    .toHaveText('SECOND_TITLE');
            });
        });
    });

    describe('#loadSubNavData', function () {

        describe('with nested breadcrumbs', function () {
            beforeEach(function () {
                ResourceModal.$scope = { breadcrumbs: [] };
                ResourceModal.$scope.resourceId = 712;
                ResourceModal.$scope.breadcrumbs.push({
                    level: 2,
                    label: 'FIRST_TITLE',
                    id: 'workflow-details',
                    subId: 81
                }, {
                    level: 3,
                    label: 'SECOND_TITLE',
                    id: 'workflow-operations',
                    api: 'EventMediaDetailsResource',
                    subId: 216
                });
                spyOn(EventMediaDetailsResource, 'get');
            });

            it('loads the last resource specified in the breadcrumbs', function () {
                ResourceModal.loadSubNavData();
                expect(EventMediaDetailsResource.get).toHaveBeenCalledWith({ id0: 712, id1: 81, id2: 216 });
            });
        });

        describe('with sibling breadcrumbs', function () {
            beforeEach(function () {
                ResourceModal.$scope = { breadcrumbs: [] };
                ResourceModal.$scope.resourceId = 712;
                ResourceModal.$scope.breadcrumbs.push({
                    level: 2,
                    label: 'FIRST_TITLE',
                    id: 'workflow-details',
                    subId: 81
                }, {
                    level: 3,
                    label: 'SECOND_TITLE',
                    id: 'workflow-operations',
                    api: 'EventMediaDetailsResource',
                    subId: 216,
                    sibling: true
                });
                spyOn(EventMediaDetailsResource, 'get');
            });

            it('loads the last resource specified in the breadcrumbs', function () {
                ResourceModal.loadSubNavData();
                expect(EventMediaDetailsResource.get).toHaveBeenCalledWith({ id0: 712, id1: 81 });
            });
        });
    });
});
