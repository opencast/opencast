exports.httpBackendMock = function () {
    $.getScript('lib/angular/angular-mocks.js', function () {
        angular.module('httpBackendMock', ['ngMockE2E'])
        .run(function ($httpBackend) {
            var workflows = {
                results: [{
                    id:       '8695',
                    mimetype: 'image\/png',
                    tags:     'archive, engage',
                    type:     'Attachment',
                    url:      'http:\/\/localhost:8080\/archive\/archive\/mediapackage\/597d0b42-5af6-450e-ac0b-c2cb619fc2be\/cover\/0\/attachment.png'
                }, {
                    id:       '8696',
                    mimetype: 'image\/png',
                    tags:     'archive, engage',
                    type:     'Attachment',
                    url:      'http:\/\/localhost:8080\/archive\/archive\/mediapackage\/597d0b42-5af6-450e-ac0b-c2cb619fc2be\/cover\/0\/attachment.png'
                }]
            },
            media = [{
                id       : 'track-1',
                type     : 'presenter/source',
                mimetype : 'video/mp4',
                url      : 'http://localhost:8080/archive/archive/mediapackage/8564f058-43ac-48e2-a5bc-f00d771bc2e0/track-1/0/track.mp4'
            }, {
                id       : '370597b6-35ec-45de-9101-ccb87b873ee7',
                type     : 'presenter/work',
                mimetype : 'video/mp4',
                url      : 'http://localhost:8080/archive/archive/mediapackage/8564f058-43ac-48e2-a5bc-f00d771bc2e0/370597b6-35ec-45de-9101-ccb87b873ee7/0/track.mp4'
            }],
            track = {
                id:          '370597b6-35ec-45de-9101-ccb87b873ee7',
                description: '',
                flavor:      'presenter\/work',
                mimetype:    'video\/mp4',
                tags:        'archive',
                type:        'Track',
                streams: {
                    audio: [{
                        id: 'audio-1',
                        bitrate: '126694.0',
                        channels: '2',
                        type: 'AAC'
                    }],
                    video: [{
                        id: 'video-1',
                        bitrate: 2108424.0,
                        resolution: '1920x1080',
                        type: 'AVC',
                        framerate: '24.0'
                    }]
                }
            };

            $httpBackend.whenGET('/admin-ng/event/40518/workflows')
            .respond(JSON.stringify(workflows));

            $httpBackend.whenGET('/admin-ng/event/40518/media')
            .respond(JSON.stringify(media));

            $httpBackend.whenGET('/admin-ng/event/40518/media/track-1')
            .respond(JSON.stringify(track));

            $httpBackend.whenGET(/.*/).passThrough();
        });
    });
};
