describe('playerAdapterRepository', function () {
    var PlayerAdapterRepository;

    beforeEach(module('adminNg.services'));
    beforeEach(inject(function (_PlayerAdapterRepository_) {
        PlayerAdapterRepository = _PlayerAdapterRepository_;
    }));

    it('instantiates', function () {
        expect(PlayerAdapterRepository).toBeDefined();
    });

    it('finds my adapter', function () {
        var element, adapter;
        element = $('<video id="heinz"/>')[0];
        adapter = PlayerAdapterRepository.findByAdapterTypeAndElementId('VIDEOJS', element);
        expect(adapter).toBeDefined();
    });
});
