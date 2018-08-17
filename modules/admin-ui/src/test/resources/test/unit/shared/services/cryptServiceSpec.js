describe('CryptService', function () {

    var CryptService;

    beforeEach(module('ngResource'));
    beforeEach(module('adminNg.services'));
    beforeEach(module('adminNg.resources'));
    beforeEach(module('angular-md5'));

    beforeEach(inject(function (_CryptService_) {
        CryptService = _CryptService_;
    }));

    describe('check valid createHashFromPasswortAndSalt output', function () {

        it('fetches authentication information from the server', function () {
            // expects the identical hashes as in JpaUserProviderTest
            expect(CryptService.createHashFromPasswortAndSalt('a', 'b')).toEqual('731eca2082f7b35876f319b1f36c7774');
            expect(CryptService.createHashFromPasswortAndSalt('c', 'd')).toEqual('23bf404be18db23b7710899cf11c58e7');
            expect(CryptService.createHashFromPasswortAndSalt('e', 'f')).toEqual('03e2109f0c3b5f4be916e8b18694d145');
        });
    });

});
