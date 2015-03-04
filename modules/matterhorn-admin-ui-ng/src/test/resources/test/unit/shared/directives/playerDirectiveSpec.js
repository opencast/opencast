describe('adminNg.directives.playerDirective', function () {

    var $compile, $rootScope, $httpBackend, $timeout, element, repository, adapter = {
        play: function () {
        },
        pause: function () {
        },
        addListener: function () {
        },
        previousFrame: function () {
        },
        nextFrame: function () {
        }
    };

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {
            },
            formatDate: function (val, date) {
                return date;
            },
            formatTime: function (val, date) {
                return date;
            },
            getLanguageCode: function () {
                return 'en';
            }
        };
        $provide.value('Language', service);
    }));

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg'));
    beforeEach(module('shared/partials/player.html'));


    beforeEach(inject(function (_$rootScope_, _$compile_, _$httpBackend_, _$timeout_, _PlayerAdapterRepository_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        $httpBackend = _$httpBackend_;
        $timeout = _$timeout_;
        repository = _PlayerAdapterRepository_;
    }));

    beforeEach(function () {
        spyOn(adapter, 'play');
        spyOn(adapter, 'pause');
        spyOn(adapter, 'previousFrame');
        spyOn(adapter, 'nextFrame');

        // mock angular.element(..)
        spyOn(angular, 'element').and.returnValue($('<video id=player/>'));

        // mock repository.create(..)
        spyOn(repository, 'findByAdapterTypeAndElementId').and.returnValue(adapter);


        $httpBackend.whenGET('/i18n/languages.json')
            .respond('{"fallbackLanguage":{"code":"en_US"},"bestLanguage":{"code":"en_US"}}');
        $rootScope.player = {};
        element = $compile('<div video-player data-player="player" data-video="video" data-player-ref="player" data-x="{{playing}}" data-adapter="html5"/>')($rootScope);
        $rootScope.$digest();
    });

    describe('html', function () {

        it('contains video-player', function () {
            expect(element.find('div.video-player')).toExist();
        });

        it('contains video-controls', function () {
            expect(element.find('div.video-controls')).toExist();
        });

        it('contains timecode-controls', function () {
            expect(element.find('div.timecode-controls')).toExist();
        });

        it('contains playback-controls', function () {
            expect(element.find('div.playback-controls')).toExist();
        });

        it('contains volume-controls', function () {
            expect(element.find('div.playback-controls')).toExist();
        });
    });

    describe('html', function () {

        it('delegates to adapter.previousFrame()', function () {
            element.find('div.playback-controls-wrapper > div:nth-child(2)').click();
            expect(adapter.previousFrame).toHaveBeenCalled();
        });

        it('delegates to adapter.play() if playing is false', function () {
            element.find('div.playback-controls-wrapper > div:nth-child(3)').click();
            expect(adapter.play).toHaveBeenCalled();
        });

        it('delegates to adapter.pause() if playing is true', function () {
            element.isolateScope().playing = true;
            element.find('div.playback-controls-wrapper > div:nth-child(3)').click();
            expect(adapter.pause).toHaveBeenCalled();
        });

        it('delegates to adapter.pause() if playing is true', function () {
            element.find('div.playback-controls-wrapper > div:nth-child(4)').click();
            expect(adapter.nextFrame).toHaveBeenCalled();
        });
    });
});
