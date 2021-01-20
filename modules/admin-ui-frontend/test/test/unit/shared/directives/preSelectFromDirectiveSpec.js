describe('adminNg.directives.preSelectFrom', function () {
    var $httpBackend,
        $compile,
        $rootScope,
        samples;

    beforeEach(module('adminNg'));
    beforeEach(module('adminNg.directives'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            getLanguageCode: function () { return 'en'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$httpBackend_, _$rootScope_, _$compile_) {
        $rootScope = _$rootScope_;
        $compile = _$compile_;
        $httpBackend = _$httpBackend_;
    }));

    beforeEach(inject(function () {
        samples = {
            length1: [{name:'hans'}],
            length3: [{name:'hans'},{name:'paul'},{name:'guenther'}],
            length1obj: {1: {name:'hans'}},
            length3obj: {1: {name:'hans'}, 2:{name:'paul'}, 3:{name:'guenther'}}
        };
    }));

    beforeEach(function () {
        $httpBackend.whenGET('modules/events/partials/index.html').respond('');
    });

    describe('#non-nested-field', function() {

        var element, $scope;
        beforeEach(inject(function () {
            element = $compile('<select pre-select-from="myArray" ng-model="selectedElement"/>')($rootScope);
            $scope = element.scope();
            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();
        }));

        // -- simple arrays ----------------------------------------------------------------------------------------- --

        it('automatically assign value to model, cause length(array) == 1', function () {

            $scope.myArray = samples.length1;

            $scope.$digest();

            // head of array has been picked
            expect($scope.selectedElement).toBeDefined();
        });

        it('do not assign value to model, cause length(array) > 1', function () {

            $scope.myArray = samples.length3;

            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();
        });

        // -- reassignments ----------------------------------------------------------------------------------------- --

        it('watch array change and automatically assign value to model', function () {

            $scope.myArray = samples.length3;
            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();

            // pick a len(array) == 1 array

            $scope.myArray = samples.length1;
            $scope.$digest();

            expect($scope.selectedElement).toBeDefined();
        });

        // -- simple object ----------------------------------------------------------------------------------------- --

        it('automatically assign value to model, cause length(object) == 1', function () {

            $scope.myArray = samples.length1obj;

            $scope.$digest();

            // head of array has been picked
            expect($scope.selectedElement).toBeDefined();
        });

        it('do not assign value to model, cause length(object) > 1', function () {

            $scope.myArray = samples.length3obj;

            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();
        });

        // -- reassignments ----------------------------------------------------------------------------------------- --

        it('watch object change and automatically assign value to model', function () {

            $scope.myArray = samples.length3obj;
            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();

            // pick a len(array) == 1 array

            $scope.myArray = samples.length1obj;
            $scope.$digest();

            // directive does not watch array any more
            expect($scope.selectedElement).toBeDefined();
        });

    });

    describe('#nested-field', function() {

        var element, $scope;
        beforeEach(inject(function () {
            element = $compile('<select pre-select-from="my.nestedArray" ng-model="selectedElement"/>')($rootScope);
            $scope = element.scope();
            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();
            $scope.my = {};
        }));

        // -- nested arrays ----------------------------------------------------------------------------------------- --

        it('automatically assign value to model, cause length(array) == 1', function () {

            $scope.my.nestedArray = samples.length1;

            $scope.$digest();

            // head of array has been picked
            expect($scope.selectedElement).toBeDefined();
        });

        it('do not assign value to model, cause length(array) > 1', function () {

            $scope.my.nestedArray = samples.length3;

            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();
        });

        // -- nested object ----------------------------------------------------------------------------------------- --

        it('automatically assign value to model, cause length(object) == 1', function () {

            $scope.my.nestedArray = samples.length1obj;

            $scope.$digest();

            // head of array has been picked
            expect($scope.selectedElement).toBeDefined();
        });

        it('do not assign value to model, cause length(object) > 1', function () {

            $scope.my.nestedArray = samples.length3obj;

            $scope.$digest();

            expect($scope.selectedElement).toBeUndefined();
        });

    });

});
