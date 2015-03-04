angular.module('adminNg.resources')
    .factory('NewThemeResource', ['$resource', function ($resource) {

        return $resource('/admin-ng/themes', {}, {
                save: {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},

                    transformRequest: function (data) {
                        var d = {};
                        if (angular.isUndefined(data)) {
                            return data;
                        }
                        // Temporarily commented out - the backend is not yet ready.
                        //d.default = data.general.default;
                        d.description = data.general.description;
                        d.name = data.general.name;

                        d.bumperActive = data.bumper.active;
                        if (data.bumper.active) {
                            d.bumperFile = data.bumper.file.id;
                        }

                        d.trailerActive = data.trailer.active;
                        if (data.trailer.active) {
                            d.trailerFile = data.trailer.file.id;
                        }

                        d.titleSlideActive = data.titleslide.active;
                        if (data.titleslide.active) {

                            if ('upload' === data.titleslide.mode) {
                                d.titleSlideBackground = data.titleslide.file.id;
                            }
                        }

                        d.licenseSlideActive = data.license.active;
                        if (data.license.active) {
                            d.licenseSlideDescription = data.license.description;
                            if (data.license.backgroundImage) {
                                d.licenseSlideBackground = data.license.file.id;
                            }
                        }
                        d.watermarkActive = data.watermark.active;
                        if (data.watermark.active) {
                            d.watermarkFile = data.watermark.file.id;
                            d.watermarkPosition = data.watermark.position;
                        }

                        return $.param(d);
                    }
                }
            }
        );
    }
    ]);
