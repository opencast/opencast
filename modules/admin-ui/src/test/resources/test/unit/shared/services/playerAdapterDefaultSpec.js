describe('DefaultAdapter', function () {
    var PlayerAdapter, PlayerAdapterFactoryDefault, defaultAdapter,
        targetElement = {
            observers: {},
            play: function () {},
            pause: function () {},
            getCurrentTime: function () {},
            addEventListener: function (name, callback) {
                this.observers[name] = callback;
            },
            executeCallback: function (name) {
                this.observers[name].call();
            }
        };
    beforeEach(module('adminNg.services'));
    beforeEach(inject(function (_PlayerAdapter_, _PlayerAdapterFactoryDefault_) {
        PlayerAdapterFactoryDefault = _PlayerAdapterFactoryDefault_;
        PlayerAdapter = _PlayerAdapter_;
        defaultAdapter = PlayerAdapterFactoryDefault.create(targetElement);
    }));

    it('instantiates', function () {
        expect(defaultAdapter).toBeDefined();
    });

    it('registers the default listeners', function () {
        defaultAdapter.registerDefaultListeners();
        var expectedCalls = ['canplay', 'durationchange', 'play', 'playing', 'pause', 'ended', 'seeking', 'seeked', 'playing', 'error'],
        i = 0;
        spyOn(targetElement, 'addEventListener').and.callThrough();
        angular.forEach(targetElement.addEventListener.calls.all(), function (call) {
            expect(call.args[0]).toEqual(expectedCalls[i++]);
        });
    });

    it('knows its status', function () {
        defaultAdapter.state.status = 'heinz';
        expect(defaultAdapter.getStatus()).toEqual('heinz');
    });

    it('calculates a current time object', function () {
        var time = defaultAdapter.getCurrentTimeObject();
        expect(time.hours).not.toBeUndefined();
        expect(time.minutes).not.toBeUndefined();
        expect(time.seconds).not.toBeUndefined();
        expect(time.milliseconds).not.toBeUndefined();
    });

    describe('[event forwarding]', function () {
        beforeEach(function () {
            defaultAdapter.registerDefaultListeners();
        });

        it('play gets executed if initialized', function () {
            defaultAdapter.state.initialized = true;
            targetElement.executeCallback('play');
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
        });

        it('play is not executed if not initialized', function () {
            defaultAdapter.state.initialized = false;
            targetElement.executeCallback('play');
            expect(defaultAdapter.state.status).not.toEqual(PlayerAdapter.STATUS.PLAYING);
        });

        it('playing changes state to playing', function () {
            targetElement.executeCallback('playing');
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
        });

        it('pause does not execute unless initialized', function () {
            targetElement.executeCallback('pause');
            expect(defaultAdapter.state.status).not.toEqual(PlayerAdapter.STATUS.PAUSED);
        });

        it('pause does execute if initialized', function () {
            defaultAdapter.state.initialized = true;
            targetElement.executeCallback('pause');
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.PAUSED);
        });

        it('ended', function () {
            targetElement.executeCallback('ended');
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.ENDED);
        });

        it('seeking', function () {
            var currentStatus = defaultAdapter.state.status;
            targetElement.executeCallback('seeking');
            expect(defaultAdapter.state.oldStatus).toEqual(currentStatus);
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.SEEKING);
        });

        it('playing updates the state', function () {
            targetElement.executeCallback('playing');
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
        });

        it('error updates the state', function () {
            targetElement.executeCallback('error');
            expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.ERROR_NETWORK);
        });
    });

    describe('api functions', function () {
        beforeEach(function () {
            defaultAdapter.registerDefaultListeners();
        });

        describe('#play()', function () {
            it('does not play if still loading', function () {
                defaultAdapter.state.status = PlayerAdapter.STATUS.LOADING;
                defaultAdapter.play();
                expect(defaultAdapter.state.waitToPlay).toBeTruthy();
            });

            it('does play when finished', function () {
                targetElement.play = function () {};
                spyOn(targetElement, 'play').and.callThrough();
                defaultAdapter.state.status = PlayerAdapter.STATUS.ENDED;
                defaultAdapter.play();
                expect(defaultAdapter.state.waitToPlay).toBeFalsy();
                expect(defaultAdapter.state.status).toEqual(PlayerAdapter.STATUS.PLAYING);
                expect(targetElement.play).toHaveBeenCalled();
            });
        });

        describe('#canPlay', function () {
            it('canPlay() sets initialized to true', function () {
                defaultAdapter.state.status = PlayerAdapter.STATUS.LOADING;
                targetElement.readyState = 3;
                // fake getDuration
                defaultAdapter.getDuration = function () {
                    return 300;
                };
                defaultAdapter.canPlay();
                expect(defaultAdapter.state.initialized).toBeTruthy();
            });

            it('canPlay() starts play if it is waiting', function () {
                spyOn(targetElement, 'play').and.callThrough();
                defaultAdapter.state.status = PlayerAdapter.STATUS.INITIALIZED;
                defaultAdapter.state.waitToPlay = true;
                targetElement.readyState = 3;
                // fake getDuration
                defaultAdapter.getDuration = function () {
                    return 300;
                };
                defaultAdapter.canPlay();
                expect(targetElement.play).toHaveBeenCalled();
            });

            it('canPlay() does not play if duration is not set', function () {
                spyOn(targetElement, 'play').and.callThrough();
                defaultAdapter.state.status = PlayerAdapter.STATUS.INITIALIZED;
                defaultAdapter.state.waitToPlay = true;
                targetElement.readyState = 3;
                // fake getDuration
                defaultAdapter.getDuration = function () {
                    return NaN;
                };
                defaultAdapter.canPlay();
                expect(targetElement.play).not.toHaveBeenCalled();
            });
        });


        it('#pause', function () {
            targetElement.pause = function () {};
            spyOn(targetElement, 'pause').and.callThrough();
            defaultAdapter.pause();
            expect(targetElement.pause).toHaveBeenCalled();
        });

        it('#setCurrentTime', function () {
            defaultAdapter.setCurrentTime(99);
            expect(targetElement.currentTime).toEqual(99);
        });

        it('#getCurrentTime', function () {
            defaultAdapter.setCurrentTime(88);
            expect(defaultAdapter.getCurrentTime()).toEqual(88);
        });
    });

    it('extends target object', function () {
        var target = {};
        defaultAdapter.extend(target);
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
