describe('adminNg.filters.localizeDate', function () {
    var $filter, localizeDateFilter;

    beforeEach(module('adminNg'));

    beforeEach(module(function ($provide) {
        var service = {
            configureFromServer: function () {},
            getLanguageCode: function () { return 'en'; },
            formatDate: function (format, input) { return input + '-localized'; }
        };
        $provide.value('Language', service);
    }));

    beforeEach(inject(function (_$filter_, _localizeDateFilter_) {
        $filter = _$filter_;
        localizeDateFilter = _localizeDateFilter_;
    }));

    it('gets exposed', function () {
        expect($filter('localizeDate')).not.toBeNull();
    });

    describe('with an unknown type', function () {

        it('does nothing', function () {
            expect(localizeDateFilter('foo', 'random')).toEqual('foo');
        });
    });

    describe('with a date type', function () {

        it('does nothing', function () {
            expect(localizeDateFilter('foo', 'date')).toEqual('foo-localized');
        });
    });
});
