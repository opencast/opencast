window.playerAdapterDefaultTests = function (PlayerAdapterFactory) {
    describe('DefaultAdapter', function () {
        var PlayerAdapter, playerAdapter,
            targetElement = {
                id: "heinz-das-video",
                observers: {},
                duration: 300,
                currentSrc: 'http://example.com/video.mp4',
                play: function () {},
                pause: function () {},
                getCurrentTime: function () {},
                addEventListener: function (name, callback) {
                    this.observers[name] = this.observers[name] || [];
                    this.observers[name].push(callback);
                },
                executeCallback: function (name) {
                    this.observers[name].forEach(function (observer) { observer.call(); });
                }
            };
        beforeEach(module('adminNg.services'));
        beforeEach(inject(function (_PlayerAdapter_) {
            PlayerAdapter = _PlayerAdapter_;
            playerAdapter = PlayerAdapterFactory.create(targetElement);
        }));

        it('instantiates', function () {
            expect(playerAdapter).toBeDefined();
        });

        it('only works with a target element', function () {
            expect(function () {
                PlayerAdapterFactory.create(null);
            }).toThrow('The given target element must not be null and have to be a valid HTMLElement!');
        });

        it('sets the id', function () {
            expect(playerAdapter.id).toEqual('PlayerAdapterheinz-das-video');
        });

        it('registers the default listeners', function () {
            playerAdapter.registerDefaultListeners();
            var expectedCalls = ['canplay', 'durationchange', 'play', 'playing', 'pause', 'ended', 'seeking', 'seeked', 'playing', 'error'],
            i = 0;
            spyOn(targetElement, 'addEventListener').and.callThrough();
            angular.forEach(targetElement.addEventListener.calls.all(), function (call) {
                expect(call.args[0]).toEqual(expectedCalls[i++]);
            });
        });

        it('knows its status', function () {
            playerAdapter.state.status = 'heinz';
            expect(playerAdapter.getStatus()).toEqual('heinz');
        });

        it('calculates a current time object', function () {
            var time = playerAdapter.getCurrentTimeObject();
            expect(time.hours).not.toBeUndefined();
            expect(time.minutes).not.toBeUndefined();
            expect(time.seconds).not.toBeUndefined();
            expect(time.milliseconds).not.toBeUndefined();
        });

        describe('[event forwarding]', function () {
            beforeEach(function () {
                playerAdapter.registerDefaultListeners();
            });

            it('play gets executed if initialized', function () {
                playerAdapter.state.initialized = true;
                targetElement.executeCallback('play');
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
            });

            it('play is not executed if not initialized', function () {
                playerAdapter.state.initialized = false;
                targetElement.executeCallback('play');
                expect(playerAdapter.state.status).not.toEqual(PlayerAdapter.STATUS.PLAYING);
            });

            it('playing changes state to playing', function () {
                targetElement.executeCallback('playing');
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
            });

            it('pause does not execute unless initialized', function () {
                targetElement.executeCallback('pause');
                expect(playerAdapter.state.status).not.toEqual(PlayerAdapter.STATUS.PAUSED);
            });

            it('pause does execute if initialized', function () {
                playerAdapter.state.initialized = true;
                targetElement.executeCallback('pause');
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.PAUSED);
            });

            it('ended', function () {
                targetElement.executeCallback('ended');
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.ENDED);
            });

            it('seeking', function () {
                var currentStatus = playerAdapter.state.status;
                targetElement.executeCallback('seeking');
                expect(playerAdapter.state.oldStatus).toEqual(currentStatus);
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.SEEKING);
            });

            it('playing updates the state', function () {
                targetElement.executeCallback('playing');
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
            });

            it('error updates the state', function () {
                targetElement.executeCallback('error');
                expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.ERROR_NETWORK);
            });
        });

        describe('api functions', function () {
            beforeEach(function () {
                playerAdapter.registerDefaultListeners();
            });

            describe('#ready()', function () {
                it('is only ready after complete initialization', function () {
                    expect(playerAdapter.ready()).toBeFalsy();
                    targetElement.duration = 300;
                    targetElement.readyState = 3;
                    playerAdapter.addListener(PlayerAdapter.EVENTS.CAN_PLAY, function () {
                        expect(playerAdapter.ready()).toBeTruthy();
                    });
                    targetElement.executeCallback("canplay");
                    expect(playerAdapter.ready()).toBeTruthy();
                });
            });

            describe('#play()', function () {
                it('does not play if still loading', function () {
                    playerAdapter.state.status = PlayerAdapter.STATUS.LOADING;
                    playerAdapter.play();
                    expect(playerAdapter.state.waitToPlay).toBeTruthy();
                });

                it('does play when finished', function () {
                    targetElement.play = function () {};
                    spyOn(targetElement, 'play').and.callThrough();
                    playerAdapter.state.status = PlayerAdapter.STATUS.ENDED;
                    playerAdapter.play();
                    expect(playerAdapter.state.waitToPlay).toBeFalsy();
                    expect(playerAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
                    expect(targetElement.play).toHaveBeenCalled();
                });
            });

            describe('#canPlay', function () {
                it('canPlay() sets initialized to true', function () {
                    playerAdapter.state.status = PlayerAdapter.STATUS.LOADING;
                    targetElement.readyState = 3;
                    targetElement.duration = 300;
                    playerAdapter.canPlay();
                    expect(playerAdapter.state.initialized).toBeTruthy();
                });

                it('canPlay() starts play if it is waiting', function () {
                    spyOn(targetElement, 'play').and.callThrough();
                    playerAdapter.state.status = PlayerAdapter.STATUS.INITIALIZED;
                    playerAdapter.state.waitToPlay = true;
                    targetElement.readyState = 3;
                    targetElement.duration = 300;
                    playerAdapter.canPlay();
                    expect(targetElement.play).toHaveBeenCalled();
                });

                it('canPlay() does not play if duration is not set', function () {
                    spyOn(targetElement, 'play').and.callThrough();
                    playerAdapter.state.status = PlayerAdapter.STATUS.INITIALIZED;
                    playerAdapter.state.waitToPlay = true;
                    targetElement.readyState = 3;
                    targetElement.duration = NaN;
                    playerAdapter.canPlay();
                    expect(targetElement.play).not.toHaveBeenCalled();
                });
            });


            it('#pause', function () {
                targetElement.pause = function () {};
                spyOn(targetElement, 'pause').and.callThrough();
                playerAdapter.pause();
                expect(targetElement.pause).toHaveBeenCalled();
            });

            it('#setCurrentTime', function () {
                playerAdapter.setCurrentTime(99);
                expect(targetElement.currentTime).toEqual(99);
            });

            it('#getCurrentTime', function () {
                playerAdapter.setCurrentTime(88);
                expect(playerAdapter.getCurrentTime()).toEqual(88);
            });

            it('#getCurrentSource', function () {
                expect(playerAdapter.getCurrentSource()).toEqual('');
            });
        });

        it('extends target object', function () {
            var target = {};
            playerAdapter.extend(target);
            expect(target.ready).toBeDefined();
            expect(target.play).toBeDefined();
            expect(target.canPlay).toBeDefined();
            expect(target.pause).toBeDefined();
            expect(target.setCurrentTime).toBeDefined();
            expect(target.getCurrentTime).toBeDefined();
            expect(target.getCurrentTimeObject).toBeDefined();
            expect(target.getDuration).toBeDefined();
            expect(target.getStatus).toBeDefined();
        });

    });
};
