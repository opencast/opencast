describe('FilterProfiles', function () {
    var FilterProfiles;

    beforeEach(module('adminNg.services'));
    beforeEach(module('LocalStorageModule'));

    beforeEach(inject(function (_FilterProfiles_) {
        FilterProfiles = _FilterProfiles_;
    }));

    it('provides a constructor', function () {
        expect(FilterProfiles.storage).toBeDefined();
    });
});
