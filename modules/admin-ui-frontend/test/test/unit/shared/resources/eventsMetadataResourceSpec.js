describe('Events Metadata API Resource', function () {
    var EventsMetadataResource, $httpBackend, JsHelper;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventsMetadataResource_, _JsHelper_) {
        $httpBackend  = _$httpBackend_;
        EventsMetadataResource = _EventsMetadataResource_;
        JsHelper = _JsHelper_
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var response = {
              "runningWorkflow": [],
              "mergedMetadata": [
                {
                  "readOnly": false,
                  "id": "title",
                  "label": "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
                  "type": "text",
                  "differentValues": true,
                  "value": "",
                  "required": true
                },
                {
                  "readOnly": false,
                  "id": "subject",
                  "label": "EVENTS.EVENTS.DETAILS.METADATA.SUBJECT",
                  "type": "mixed_text",
                  "value": [],
                  "required": false
                }
              ],
              "merged": [
                "6ea5822c-3f19-41d6-b0a2-b484b789a66f",
                "3171935d-a589-4a18-982f-2bc2ed2a2b84"
              ],
              "notFound": []
            }

            $httpBackend.expectPOST('/admin-ng/event/events/metadata.json').respond(JSON.stringify(response));
            var result = EventsMetadataResource.get(
            ["3171935d-a589-4a18-982f-2bc2ed2a2b84","6ea5822c-3f19-41d6-b0a2-b484b789a66f"]);
            $httpBackend.flush();

            expect(result.results).toEqual(response);
        });
    });

    describe('#save', function () {
        it('no content', function () {
            var metadataRequest = {
              eventIds:	["6ea5822c-3f19-41d6-b0a2-b484b789a66f","630f62d0-55b5-4edf-92d5-a9ae7e15dbac"],
              metadata:	[{"id":"startDate","label":"EVENTS.EVENTS.DETAILS.METADATA.START_DATE","type":"date", "value":""}]
            };

            var metadataWrapped = [{flavor: 'dublincore/episode',title:'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
            fields:[{"id":"startDate","label":"EVENTS.EVENTS.DETAILS.METADATA.START_DATE","type":"date", "value":""}]}];

            $httpBackend.expectPUT('/admin-ng/event/events/metadata', function (data) {
                expect(angular.fromJson($.deparam(data).metadata)).toEqual(metadataWrapped);
                expect(angular.fromJson($.deparam(data).eventIds)).toEqual(metadataRequest.eventIds);
                return true;
            }).respond(204);
            var result = EventsMetadataResource.save(metadataRequest);
            $httpBackend.flush();
        }),
        it('server error', function () {
            var metadataRequest = {
              eventIds:	["6ea5822c-3f19-41d6-b0a2-b484b789a66f","630f62d0-55b5-4edf-92d5-a9ae7e15dbac"],
              metadata:	[{"id":"startDate","label":"EVENTS.EVENTS.DETAILS.METADATA.START_DATE","type":"date", "value":""}]
            };

            var serverErrors = {
              "updateFailures": "",
              "notFound": "",
              "updated": ""
            }
            $httpBackend.expectPUT('/admin-ng/event/events/metadata').respond(serverErrors);
            var result = EventsMetadataResource.save(metadataRequest);
            $httpBackend.flush();
            expect(result.errors).toEqual(serverErrors);
        });
    });
});

// TODO test empty reply and non-empty
