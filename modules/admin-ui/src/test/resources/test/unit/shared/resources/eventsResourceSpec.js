describe('Events API Resource', function () {
    var EventsResource, $httpBackend, $translate;


    beforeEach(module('adminNg.resources'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('ngResource'));
    beforeEach(module('pascalprecht.translate'));

    beforeEach(module(function ($provide) {
        var service = {
            formatDate: function (val, date) { return date; },
            formatTime: function (val, date) { return date; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$httpBackend_, _EventsResource_, _$translate_) {
        $httpBackend  = _$httpBackend_;
        EventsResource = _EventsResource_;
        $translate = _$translate_;
    }));

    describe('#query', function () {
        var sampleJSON = {
            results: [{
                source: 'archive',
                id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a',
                title: 'Test Title',
                presenters: ['Matt Smith', 'Chuck Norris'],
                technical_presenters: ['Matt Smith', 'Chuck Norris'],
                scheduling_status: 'SCHEDULED',
                review_status: 'REVIEWED',
                workflow_state: 'processing',
                series: {
                    id: '78753d04-18a4-4327-9a61-b6d93816a7d2',
                    title: 'Physics325'
                },
                location: 'Room 2',
                agentId: 'Room 2',
                start_date: '2012-12-02T10:00:00Z',
                technical_start_date: '2012-12-02T10:00:00Z',
                end_date: '2012-12-02T11:15:00Z',
                technical_end_date: '2012-12-02T11:15:00Z',
                status: 'processing',
                comments: {
                    resolved: 0,
                    unresolved: 2
                },
                publications: {
                    Engage: 'http://localhost:8080/Engage/play.html?id=32423445354'
                }
            }, {
                source: 'archive',
                id: '40519',
                title: 'Test Title 2',
                presenters: ['Matt Smith'],
                technical_presenters: ['Matt Smith'],
                scheduling_status: 'SCHEDULED',
                review_status: 'REVIEWED',
                workflow_state: 'processing',
                series: {
                    id: '78753d08-18a4-4327-9a61-b6d93816a7d2',
                    title: 'Physics326'
                },
                location: 'Room 3',
                agentId: 'Room 3',
                start_date: '2012-12-01T08:59:00Z',
                technical_start_date: '2012-12-01T08:59:00Z',
                end_date: '2012-12-01T08:59:00Z',
                technical_end_date: '2012-12-01T08:59:00Z',
                status: 'processed',
                comments: {
                    resolved: 1,
                    unresolved: 0
                },
                publications: {
                    Engage: 'http://localhost:8080/Engage/play.html?id=42423445354'
                }
            }]
        };

        it('calls the events service', function () {
            $httpBackend.expectGET('/admin-ng/event/events.json')
                .respond(JSON.stringify(sampleJSON));
            EventsResource.query();
            $httpBackend.flush();
        });

        it('flattens the JSON data received', function () {
            $httpBackend.expectGET('/admin-ng/event/events.json')
                .respond(JSON.stringify(sampleJSON));
            var data = EventsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(2);
            expect(data.rows[0].title).toBe(sampleJSON.results[0].title);
            expect(data.rows[0].presenter).toEqual(sampleJSON.results[0].presenters.join(', '));
            expect(data.rows[0].date).toBe(sampleJSON.results[0].start_date);
            expect(data.rows[0].start_date).toBe('2012-12-02T10:00:00Z');
            expect(data.rows[0].end_date).toBe('2012-12-02T11:15:00Z');
        });

        it('handles empty payload', function () {
            $httpBackend.expectGET('/admin-ng/event/events.json').respond('{ "results": []}');
            var data = EventsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(0);
        });

        it('handles payloads with one item', function () {
            $httpBackend.whenGET('/admin-ng/event/events.json').respond(JSON.stringify({
                results: {
                    source: 'archive',
                    id: 'c3a4f68d-14d4-47e2-8981-8eb2fb300d3a',
                    title: 'Test Title',
                    presenters: ['Matt Smith', 'Chuck Norris'],
                    technical_presenters: ['Matt Smith', 'Chuck Norris'],
                    series: {
                        id: '78753d04-18a4-4327-9a61-b6d93816a7d2',
                        title: 'Physics325'
                    },
                    location: 'Room 2',
                    scheduling_status: 'SCHEDULED',
                    review_status: 'REVIEWED',
                    workflow_state: 'processing',
                    creation_date: '2012-12-01T08:59:00Z',
                    modification_date: '2012-12-01T08:59:00Z',
                    comments: {
                        resolved: 0,
                        unresolved: 2
                    },
                    publications: {
                        Engage: 'http://localhost:8080/Engage/play.html?id=32423445354'
                    }
                }
            }));
            var data = EventsResource.query();
            $httpBackend.flush();
            expect(data.rows.length).toBe(1);
        });
    });
});
