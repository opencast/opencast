describe('PlayerAdapterVideoJs', function () {
    var PlayerAdapterFactoryVIDEOJS, PlayerAdapterVideoJs, targetElement = $('<video id="heinz-das-video"/>')[0];
    beforeEach(module('adminNg.services'));
    beforeEach(inject(function (_PlayerAdapterFactoryVIDEOJS_) {
        PlayerAdapterFactoryVIDEOJS = _PlayerAdapterFactoryVIDEOJS_;
        PlayerAdapterVideoJs = PlayerAdapterFactoryVIDEOJS.create(targetElement);
    }));

    it('sets the id', function () {
        expect(PlayerAdapterVideoJs.id).toEqual('PlayerAdapterheinz-das-video');
    });

    it('initializes the player', function () {
        var createCallback = function () {
            PlayerAdapterFactoryVIDEOJS.create(null);
        };

        expect(createCallback).toThrow('The given target element must not be null and have to be a valid HTMLElement!');
    });
});
