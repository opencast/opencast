angular.module('adminNg.services')
.factory('CryptService', ['md5', function (md5) {
    var CryptService = function () {
        this.createHashFromPasswortAndSalt = function (password, username) {
            return md5.createHash(password + '{' + username + '}');
        };
    };

    return new CryptService();
}]);
