angular.module('adminNg.filters')
.filter('limitAppend', [ function () {
    return function (input, limit, begin, append) {
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
