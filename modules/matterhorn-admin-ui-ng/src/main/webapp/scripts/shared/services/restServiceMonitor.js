angular.module('adminNg.services')
.factory('RestServiceMonitor', ['$http', function($http){
    var Monitoring = {};
    var service = {
        name : null,
        status : '',
        error: false,
        numErr: 1
    };

    Monitoring.setService = function(name){
        service.name = name;
        Monitoring.run();
    };

    Monitoring.run = function(){
        $http.get('/broker/status')
            .then(function(data){
                if(data.status === 204){
                    service.status = 'OK';
                }else{
                    service.status = data.statusText;
                }
                service.error = false;
            }, function(err){
                service.status = err.statusText;
                service.error = true;
            });
    };

    Monitoring.getServiceStatus = function(){
        return service;
    };

    return Monitoring;
}]);
