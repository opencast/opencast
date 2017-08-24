angular.module('adminNg.services')
.factory('RestServiceMonitor', ['$http', function($http){
    var Monitoring = {};
    var services = {
        service: {},
        error: false,
        numErr: 0
    };

    Monitoring.run = function(){
	//Clear existing data
        services.service = {};
        services.error = false;
        services.numErr = 0;

        var amqName = "ActiveMQ";
        var statesName = "Service States";
	var backendName = "Backend Services";
	var ok = "OK";

        $http.get('/broker/status')
            .then(function(data){

                Monitoring.populateService(amqName);
                if(data.status === 204){
                    services.service[amqName].status = ok;
                    services.service[amqName].error = false;
                }else{
                    services.service[amqName].status = data.statusText;
                    services.service[amqName].error = true;
                }
            }, function(err){
                Monitoring.populateService(amqName);
                services.service[amqName].status = err.statusText;
                services.service[amqName].error = true;
                services.error = true;
                services.numErr++;
            });
        $http.get('/services/services.json')
            .then(function(data){
                angular.forEach(data.data.services.service, function(service, key) {
                  name = service.type.split('opencastproject.')[1];
                  if (service.service_state != "NORMAL") {
                    Monitoring.populateService(name);
                    services.service[name].status = service.service_state;
                    services.service[name].error = true;
                    services.error = true;
                    services.numErr++;
                  }
                });
                if (!services.error) {
                  Monitoring.populateService(backendName);
                  services.service[backendName].status = ok;
                }
            }, function(err){
                Monitoring.populateService(statesName);
                services.service[statesName].status = err.statusText;
                services.service[statesName].error = true;
                services.error = true;
                services.numErr++;
            });
    };

    Monitoring.populateService = function(name) {
        if (services.service[name] === undefined) {
            services.service[name] = {};
        }
    };

    Monitoring.getServiceStatus = function(){
        return services;
    };

    return Monitoring;
}]);
