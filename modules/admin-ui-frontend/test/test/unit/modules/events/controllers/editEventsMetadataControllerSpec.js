describe('Edit events controller', function () {
    var $scope, $controller, $httpBackend, TableServiceMock, NotificationsMock, $timeout;

    beforeEach(module('adminNg'));

    // initiate mocks
    TableServiceMock = jasmine.createSpyObj('TableService', ['copySelected', 'deselectAll']);
    NotificationsMock = jasmine.createSpyObj('Notifications', ['add']);
    var selectedRows = [{id: "6ea5822c-3f19-41d6-b0a2-b484b789a66f", selected: true, series_id: "4581", title: "haha"},
                        {id: "3171935d-a589-4a18-982f-2bc2ed2a2b84", selected: true, series_id: "4581", title: "hoho"},
                        {id: "17d2696b-0185-48df-a9fb-91f0ea1b7064", selected: true, series_id: "4581", title: "hoho"}
                       ];
    TableServiceMock.copySelected.and.returnValue(selectedRows);

    beforeEach(module(function ($provide) {
        $provide.value('Notifications', NotificationsMock);
        $provide.value('Table', TableServiceMock);
    }));

    beforeEach(inject(function ($rootScope, _$controller_, _$timeout_, _$httpBackend_) {
        $controller = _$controller_;
        $scope = $rootScope.$new();
        $scope.close = jasmine.createSpy('close');
        $timeout = _$timeout_;
        $httpBackend = _$httpBackend_;
    }));

      beforeEach(function () {
          $controller('EditEventsMetadataCtrl', {$scope: $scope});
          jasmine.getJSONFixtures().fixturesPath = 'base/app/GET';
          $httpBackend.whenGET('modules/events/partials/index.html').respond('');
          $httpBackend.whenGET('/i18n/languages.json').respond(JSON.stringify(getJSONFixture('i18n/languages.json')));
          $httpBackend.whenGET('public/org/opencastproject/adminui/languages/lang-en_US.json').respond('');
      });

    describe('no request errors', function () {

        beforeEach(function () {
            $httpBackend.expectPOST('/admin-ng/event/events/metadata.json').respond(JSON.stringify(
              getJSONFixture('admin-ng/event/events/metadata.json')));
            $httpBackend.flush();
            spyOn($scope, 'deselectAndClose');
        });

        it('instantiation', function () {
            var results = getJSONFixture('admin-ng/event/events/metadata.json');

            expect(TableServiceMock.copySelected).toHaveBeenCalled();
            expect($scope.selectedRows).toEqual(selectedRows);
            expect($scope.rows.length).toEqual(results.metadata.length);
            var firstField = {
                                 "readOnly": false,
                                 "id": "title",
                                 "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
                                 "type": "text",
                                 "differentValues": true,
                                 "value": "",
                                 "required": true,
                                 "tabindex": 2,
                                 "selected": false,
                                 "saved": false
                             };
            expect($scope.rows[0]).toEqual(firstField);
            expect($scope.eventIdsToUpdate).toEqual(["6ea5822c-3f19-41d6-b0a2-b484b789a66f",
                                                     "3171935d-a589-4a18-982f-2bc2ed2a2b84",
                                                     "17d2696b-0185-48df-a9fb-91f0ea1b7064"
                                                     ]);
            expect($scope.eventsToUpdate.length).toEqual(3);
            expect($scope.eventsToUpdate[0]).toEqual(selectedRows[0]);
            expect($scope.currentForm).toEqual('editMetadataForm');
        });

        it('submit', function () {
            $scope.rows[0].selected = true;
            $httpBackend.whenPUT('/admin-ng/event/events/metadata').respond(204);
            $scope.submit();
            $httpBackend.flush();
            expect(NotificationsMock.add).toHaveBeenCalled();
            expect($scope.deselectAndClose).toHaveBeenCalled();
            expect($scope.submitting).toEqual(false);
        });

        it('server error without data', function() {
            $scope.rows[0].selected = true;
            $httpBackend.whenPUT('/admin-ng/event/events/metadata').respond(500);
            $scope.submit();
            $httpBackend.flush();
            expect(NotificationsMock.add).toHaveBeenCalled();
            expect($scope.submitting).toEqual(false);
            expect($scope.close).toHaveBeenCalled();
        })

        it('some updates failed', function() {
            $scope.rows[0].selected = true;
            $httpBackend.whenPUT('/admin-ng/event/events/metadata').respond(500,
            {notFound: ["6ea5822c-3f19-41d6-b0a2-b484b789a66f"],
            updateFailure: ["3171935d-a589-4a18-982f-2bc2ed2a2b84"]});
            $scope.submit();
            $httpBackend.flush();
            expect($scope.someUpdatedSuccessfully).toEqual(true);
            expect($scope.currentForm).toEqual('updateErrorsForm');
            expect($scope.submitting).toEqual(false);
        })

        it('all updates failed', function() {
            $scope.rows[0].selected = true;
            $httpBackend.whenPUT('/admin-ng/event/events/metadata').respond(500,
            {notFound: ["6ea5822c-3f19-41d6-b0a2-b484b789a66f", "3171935d-a589-4a18-982f-2bc2ed2a2b84",
                        "17d2696b-0185-48df-a9fb-91f0ea1b7064"],
            updateFailure: []});
            $scope.submit();
            $httpBackend.flush();
            expect($scope.someUpdatedSuccessfully).toEqual(false);
            expect($scope.currentForm).toEqual('updateErrorsForm');
            expect($scope.submitting).toEqual(false);
        })
    });

    describe('request errors', function () {

      beforeEach(function () {
          $httpBackend.expectPOST('/admin-ng/event/events/metadata.json').respond(JSON.stringify(getJSONFixture('admin-ng/event/events/metadataErrors2.json')));
          $httpBackend.flush();
      });

      it('instantiation', function () {
          expect(TableServiceMock.copySelected).toHaveBeenCalled();
          expect($scope.noEventsToEdit).toEqual(false);
          expect($scope.hasRequestErrors).toEqual(true);
          expect($scope.currentForm).toEqual('requestErrorsForm');
      })
    });

    describe('none found', function () {

      beforeEach(function () {
          $httpBackend.expectPOST('/admin-ng/event/events/metadata.json').respond(404, JSON.stringify(getJSONFixture('admin-ng/event/events/metadataErrors.json')));
          $httpBackend.flush();
      });

      it('instantiation', function () {
          expect(TableServiceMock.copySelected).toHaveBeenCalled();
          expect($scope.noEventsToEdit).toEqual(true);
          expect($scope.hasRequestErrors).toEqual(true);
          expect($scope.currentForm).toEqual('requestErrorsForm');
      })
    });

});
