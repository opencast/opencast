describe('Event Media API Resource', function () {
    var EventMediaResource, $httpBackend;

    beforeEach(module('adminNg.resources'));
    beforeEach(module('ngResource'));

    beforeEach(inject(function (_$httpBackend_, _EventMediaResource_) {
        $httpBackend  = _$httpBackend_;
        EventMediaResource = _EventMediaResource_;
    }));

    describe('#get', function () {
        it('provides nested JSON data', function () {
            var result, mediaResponse = [{
                id: 'title'
            }, {
                id: 'series'
            }];
            $httpBackend.expectGET('/admin-ng/event/40518/media.json').respond(JSON.stringify(mediaResponse));
            result = EventMediaResource.get({ id: 40518 });
            $httpBackend.flush();
            expect(result.entries.length).toBe(2);
        });
    });

    describe('#save', function () {
        it('sends an array of media', function () {
            var mediaRequest = {
                entries: [{ id: 'title' }, { id: 'series' }]
            };
            $httpBackend.expectPOST('/admin-ng/event/40518/media.json', mediaRequest.entries).respond(200);
            EventMediaResource.save({ id: '40518' }, mediaRequest);
            $httpBackend.flush();
        });
    });

    describe('#mediaFileName', function(){

        it('returns the last path of the url', function(){
            $httpBackend.expectGET('/admin-ng/event/40518/media.json').respond(JSON.stringify([
                {
                    'id'       : 'track-1',
                    'type'     : 'presenter/source',
                    'mimetype' : 'video/mp4',
                    'url'      : 'http://localhost:8080/archive/archive/mediapackage/8564f058-43ac-48e2-a5bc-f00d771bc2e0/track-1/0/track.mp4'
                }
            ]));

            var data = EventMediaResource.get({ id: '40518'});
            $httpBackend.flush();

            expect(data.entries[0].mediaFileName).toEqual('track.mp4');
        });

        it('returns the full name if url is a filename only', function(){
            $httpBackend.expectGET('/admin-ng/event/40518/media.json').respond(JSON.stringify([
                {
                    'id'       : 'track-1',
                    'type'     : 'presenter/source',
                    'mimetype' : 'video/mp4',
                    'url'      : 'track.mp4'
                }
            ]));

            var data = EventMediaResource.get({ id: '40518'});
            $httpBackend.flush();

            expect(data.entries[0].mediaFileName).toEqual('track.mp4');
        });

        it('returns an empty string if url is undefined', function(){
            $httpBackend.expectGET('/admin-ng/event/40518/media.json').respond(JSON.stringify([
                {
                    'id'       : 'track-1',
                    'type'     : 'presenter/source',
                    'mimetype' : 'video/mp4',
                    'url'      : ''
                }
            ]));

            var data = EventMediaResource.get({ id: '40518'});
            $httpBackend.flush();

            expect(data.entries[0].mediaFileName).toEqual('');
        });

    });
});
