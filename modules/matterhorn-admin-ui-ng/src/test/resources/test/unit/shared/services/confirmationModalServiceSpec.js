describe('ConfirmationModal service', function () {
    var $rootScope, ConfirmationModal, Modal;

    beforeEach(module('adminNg.services.modal'));

    beforeEach(module(function ($provide) {
        $provide.value('Table', {});
    }));

    beforeEach(inject(function (_$rootScope_, _ConfirmationModal_, _Modal_) {
        $rootScope = _$rootScope_;
        ConfirmationModal = _ConfirmationModal_;
        Modal = _Modal_;
    }));

    it('returns the modal object', function () {
        expect(ConfirmationModal.show).toBeDefined();
    });

    describe('#show', function () {
        beforeEach(function () {
            spyOn(Modal, 'show');
            Modal.$scope = {};
        });

        it('creates a modal', function () {
            ConfirmationModal.show('confirm-modal');
            expect(Modal.show).toHaveBeenCalledWith('confirm-modal');
        });

        it('sets properties', function () {
            ConfirmationModal.show('test-modal', 'callback', 29);

            expect(ConfirmationModal.$scope.callback).toEqual('callback');
            expect(ConfirmationModal.$scope.object).toBe(29);
        });
    });

    describe('#confirm', function () {
        var parentScope, controllerScope;

        beforeEach(function () {
            Modal.$scope = { $destroy: jasmine.createSpy() };
            Modal.modal = { remove: jasmine.createSpy() };
            Modal.overlay = { remove: jasmine.createSpy() };

            ConfirmationModal.show('confirm-modal');
            parentScope = $rootScope.$new();
            controllerScope = parentScope.$new();
            spyOn(Modal.$scope, 'close');
            ConfirmationModal.$scope.callback = jasmine.createSpy();
        });

        it('closes the modal', function () {
            ConfirmationModal.$scope.confirm();

            expect(Modal.$scope.close).toHaveBeenCalled();
        });

        it('calls the callback', function () {
            ConfirmationModal.$scope.object = 918;

            ConfirmationModal.$scope.confirm();
            expect(ConfirmationModal.$scope.callback).toHaveBeenCalledWith(918);
            parentScope.callback = jasmine.createSpy();
        });
    });
});
