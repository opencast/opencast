describe('PlayerAdapterHtml5', function () {
    var PlayerAdapterFactoryHTML5, PlayerAdapterHtml5, targetElement = $('<video id="heinz-das-video"/>')[0];
    beforeEach(module('adminNg.services'));
    beforeEach(inject(function (_PlayerAdapterFactoryHTML5_) {
        PlayerAdapterFactoryHTML5 = _PlayerAdapterFactoryHTML5_;
        PlayerAdapterHtml5 = PlayerAdapterFactoryHTML5.create(targetElement);
    }));

    it('sets the id', function () {
        expect(PlayerAdapterHtml5.id).toEqual('PlayerAdapterheinz-das-video');
    });

    it('initializes the player', function () {
        var createCallback = function () {
            PlayerAdapterFactoryHTML5.create(null);
        };

        expect(createCallback).toThrow('The given target element must not be null and have to be a valid HTMLElement!');
    });
});
