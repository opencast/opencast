describe('adminNg.directives.openModal', function () {
    var $compile, $rootScope, ResourceModal, element;

    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.services.modal'));
    beforeEach(module('adminNg.directives'));
    beforeEach(module('LocalStorageModule'));
    beforeEach(module('shared/partials/modals/series-details.html'));

    beforeEach(inject(function (_$rootScope_, _$timeout_, _$compile_, _ResourceModal_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        ResourceModal = _ResourceModal_;
    }));

    beforeEach(function () {
        element = $compile('<a data-open-modal="series-details" data-resource-id="8"></a>')($rootScope);
        $rootScope.$digest();
    });

    it('opens the modal when clicked', function () {
        spyOn(ResourceModal, 'show');
        element.click();
        expect(ResourceModal.show).toHaveBeenCalledWith('series-details', '8', undefined, undefined);
    });
});
