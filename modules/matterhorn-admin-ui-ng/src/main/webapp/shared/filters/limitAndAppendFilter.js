angular.module('adminNg.filters')
.filter('limitAppend', [ function () {
    return function (input, limit, begin, append) {
 
 /** params:
             input ... input text
             limit ... number of characters to keep 
                             positve  ... counting forward from given position (begin)
                             negative ... counting backward from given position (begin)
             begin ... position to start to cut the string 
                             positive ... counting forward from the beginning of the string
                             negative ... counting backward from the end of the string
             append ... string to be appended if and where the input string was cut 
                        (number of characters do not count into the limit)
                        
*/
 
        var prepend = '';
        if (angular.isUndefined(begin) || isNaN(begin)) {
            begin = 0; 
        }
        if (angular.isUndefined(append)) {
            append = ' ... '; 
        }
        
        if (isNaN(limit)) {
            return input;
        }
       
        
        if (!angular.isString(input)) {
            return input;
        }
        
        if (begin < 0) {
            prepend = append;
        }
     
        begin = (begin < 0) ? Math.max(0, input.length + begin) : begin;
        if (begin < 0) {
            prepend = append;
        }

        begin = (begin < 0) ? Math.max(0, input.length + begin) : begin;

        if (limit >= 0) {
          if ((begin + limit) >= input.length) {
               append = '';
          }
          return prepend + input.slice(begin, begin + limit) + append;
        } else {
            if (begin === 0) { 
                append = '';
                if ((input.length + limit) > 0) {
                    prepend=append;
                }

                return prepend + input.slice(limit, input.length) +append;
            } else {
                var lim = Math.max(0, begin + limit);
                if (lim === 0) {
                     prepend = '';
                }
                return prepend + input.slice(lim, begin) +append;
            }
        }
        return prepend + input.slice(begin, begin+limit) + append;
    };     
}]);
