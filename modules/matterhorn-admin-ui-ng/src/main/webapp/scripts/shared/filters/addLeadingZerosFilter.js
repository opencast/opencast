angular.module('adminNg.filters')
.filter('addLeadingZeros', [function () {
    return function (input, digitNumber) {
        var number = parseInt(input, 10);
        digitNumber = parseInt(digitNumber, 10);
        if (isNaN(number) || isNaN(digitNumber)) {
            return number;
        }
        number = '' + number;
        while (number.length < digitNumber) {
            number = '0' + number;
        }
        return number;
    };
}]);
