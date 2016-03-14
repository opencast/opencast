angular.module('adminNg.filters')
.filter('sortMapByValue', [function () {
    /**
     * Sort a map by values and transform it into an array
     * to avoid angular sorting it again by keys
     */
    return function(input) {
       if (!angular.isObject(input)) {
            return input;
       }

        var array = [];

        angular.forEach(input, function (value, index) {
            array.push({
                key: index,
                label: value
            });
        });


        array.sort(function(a, b){
            if (a.value > b.value) {
                return 1;
            } else if (a.value < b.value) {
                return -1;
            } else {
                return 0;
            }
        });

        return array;
    };
}]);
