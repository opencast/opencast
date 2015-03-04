describe('Storage', function () {
    var Storage, localStorageService, $location;

    beforeEach(module('adminNg.services'));
    beforeEach(module('LocalStorageModule'));

    beforeEach(inject(function (_Storage_, _localStorageService_, _$location_) {
        Storage = _Storage_;
        localStorageService = _localStorageService_;
        $location = _$location_;
    }));

    afterEach(function () {
        Storage.storage = {};
    });

    it('adds values', function () {
        Storage.put('typeA', 'namespace1', 'attributeOne', 'valueOne');
        Storage.put('typeA', 'namespace1', 'attributeTwo', 'valueTwo');
        expect(Storage.get('typeA', 'namespace1').attributeOne).toEqual('valueOne');
        expect(Storage.get('typeA', 'namespace1').attributeTwo).toEqual('valueTwo');
    });

    describe('#get', function () {

        it('returns the stored value', function () {
            Storage.put('typeA', 'namespace1', 'attributeOne', 'valueOne');
            Storage.put('typeA', 'namespace1', 'attributeTwo', 'valueTwo');
            expect(Storage.get('typeA', 'namespace1')).toEqual({'attributeOne': 'valueOne', 'attributeTwo': 'valueTwo'});
        });

        it('returns an empty hash if the namespace is empty', function () {
            Storage.put('typeA', 'namespace1', 'attributeOne', 'valueOne');
            expect(Storage.get('typeA', 'namespace2')).toEqual({});
        });
    });

    describe('#remove', function () {

        it('removes a single value', function () {
            Storage.put('typeA', 'namespace1', 'attributeOne', 'valueOne');
            Storage.put('typeA', 'namespace1', 'attributeTwo', 'valueTwo');
            Storage.remove('typeA', 'namespace1', 'attributeTwo');
            expect(Storage.get('typeA', 'namespace1').attributeOne).toBeDefined();
            expect(Storage.get('typeA', 'namespace1').attributeTwo).toBeUndefined();
        });

        it('removes all values of a certain type', function () {
            Storage.put('typeA', 'namespace1', 'attributeOne', 'valueOne');
            Storage.put('typeB', 'namespace1', 'attributeTwo', 'valueTwo');
            Storage.remove('typeA');
            expect(Storage.storage.typeA).toBeUndefined();
            expect(Storage.storage.typeB).toBeDefined();
        });

        it('removes all values of a certain namespace', function () {
            Storage.put('typeA', 'namespace1', 'attributeOne', 'valueOne');
            Storage.put('typeA', 'namespace2', 'attributeOne', 'valueOne');
            Storage.remove('typeA', 'namespace1');
            expect(Storage.storage.typeA.namespace1).toBeUndefined();
            expect(Storage.storage.typeA.namespace2).toBeDefined();
        });
    });

    describe('#getFromStorage', function () {

        describe('with values in the URL parameter', function () {
            beforeEach(function () {
                $location.search('storage', '{"typeA":{"namespace2":{"attributeTwo":"valueTwo"}}}');
            });

            it('retrieves values from the URL', function () {
                spyOn(localStorageService, 'get');
                Storage.getFromStorage();

                expect(localStorageService.get).not.toHaveBeenCalled();
                expect(Storage.get('typeA', 'namespace2').attributeTwo).toEqual('valueTwo');
            });
        });

        describe('with values only in localStorage', function () {
            beforeEach(function () {
                localStorageService.add('storage',
                    { typeA: { namespace1: { attributeOne: 'valueOne' } } });
            });

            it('retrieves values from localStorage', function () {
                Storage.getFromStorage();
                expect(Storage.get('typeA', 'namespace1').attributeOne).toEqual('valueOne');
            });
        });
    });
});
