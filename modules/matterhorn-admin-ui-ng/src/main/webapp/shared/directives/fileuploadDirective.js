angular.module('adminNg.directives')
    .directive('fileUpload', ['$upload', function ($upload) {
        return {
            restrict: 'A',
            templateUrl: 'shared/partials/fileupload.html',
            require: '^ngModel',
            scope: {
                acceptableTypes: '@acceptableTypes',
                descriptionKey: '@descriptionKey',
                buttonKey: '@buttonKey',
                labelKey: '@labelKey',
                existing: '='
            },
            link: function (scope, attrs, elem, ngModel) {
                if (!scope.acceptableTypes) {
                    throw new Error('file-upload directive needs acceptableTypes attribute');
                }

                scope.file = scope.existing || {};
                scope.file.inprogress = false;
                scope.file.progress = {
                    loaded: 0,
                    total: 0,
                    value: 0
                };

                scope.remove = function () {
                    var model = ngModel;
                    scope.file = {
                        inprogress: false,
                        progress: {
                            loaded: 0,
                            total: 0,
                            value: 0
                        }
                    };
                    model.$setViewValue(undefined);
                };

                scope.upload = function (files) {
                    var progressHandler, successHandler, errorHandler, transformRequest, transformResponse, model;

                    progressHandler = function (evt) {

                        var loaded = evt.loaded * 100, total = evt.total, value = loaded / total;
                        scope.file.progress = {loaded: loaded, total: total, value: Math.round(value)};
                        scope.file.inprogress = true;
                    };

                    model = ngModel;
                    successHandler = function (data) {
                        scope.file = {
                            id: data.data.id,
                            name: data.config.file.name,
                            url: data.headers().location
                        };
                        scope.file.inprogress = false;
                        model.$setViewValue(scope.file);
                        model.$setValidity('file-upload', true);
                    };

                    errorHandler = function () {
                        scope.file = {
                            inprogress: false,
                            progress: {
                                loaded: 0,
                                total: 0,
                                value: 0
                            }
                        };

                        model.$setValidity('file-upload', false);
                    };

                    transformResponse = function (response) {
                        return { id: response.trim() };
                    };

                    transformRequest = function () {
                        var fd = new FormData();
                        fd.append('BODY', scope.files[0], scope.files[0].name);
                        return fd;
                    };

                    if (files !== undefined && files[0] !== undefined) {

                        scope.file = files[0];

                        $upload.http({
                            url: '/staticfiles',
                            headers: { 'Content-Type': undefined },
                            data: {},
                            file: scope.file,
                            transformRequest: transformRequest,
                            transformResponse: transformResponse
                        }).then(successHandler, errorHandler, progressHandler);
                    }
                };
            }
        };
    }]);
