describe('JsHelper service', function () {
    var JsHelper, obj;

    beforeEach(function () {
        obj = {
            outer: {
                inner: 25
            }
        };
    });
    beforeEach(module('adminNg.services'));
    beforeEach(inject(function (_JsHelper_) {
        JsHelper = _JsHelper_;
    }));

    describe('#getNested', function () {

        it('handles one nesting level', function () {
            expect(JsHelper.getNested({ inner: 'value' }, 'inner')).toEqual('value');
        });

        it('handles deeply nested properties', function () {
            expect(JsHelper.getNested({ main: { sub: 'value' } }, 'main.sub'))
                .toEqual('value');
        });

        it('handles non existing nested properties', function () {
            expect(JsHelper.getNested({}, 'doesntExist')).toBe(null);
        });

        it('throws an exception whithout a target object', function () {
            expect(function () { JsHelper.getNested(); })
                .toThrow('illegal method call, I need at least two arguments');
        });
    });

    describe('#isEmpty', function () {

        it('handles really empty objects', function () {
            expect(JsHelper.isEmpty({})).toBeTruthy();
        });

        it('handles non empty objects', function () {
            expect(JsHelper.isEmpty({'prop': 'value'})).toBeFalsy();
        });
    });

    describe('#isPropertyDefined', function () {

        it('finds a nested property', function () {
            expect(JsHelper.isPropertyDefined(obj, 'outer.inner')).toBeTruthy();
        });

        it('finds out that a property does not exist', function () {
            expect(JsHelper.isPropertyDefined(obj, 'some.non.existing.path')).toBeFalsy();
        });
    });

    describe('#calculateStopTime', function () {
        it('calculates the stop time correctly', function () {
            var duration = '138000', // 2min 18s
            actual = JsHelper.calculateStopTime('2010-09-29T15:59:00Z', duration);
            expect(actual).toBe('2010-09-29T16:01:18.000Z');
        });

        it('returns an empty string for an empty duration', function () {
            var actual = JsHelper.calculateStopTime('2010-09-29T15:59:00Z');
            expect(actual).toBe('');
        });
    });

    describe('#secondsToTime', function () {

        it('formats seconds to hours, minutes and seconds', function () {
            expect(JsHelper.secondsToTime((3600 * 11) + (60 * 11) + 11))
                .toEqual('11:11:11');
            expect(JsHelper.secondsToTime(3661)).toEqual('01:01:01');
        });
    });

    describe('#timeToSeconds', function () {
        var offset = (new Date()).getTimezoneOffset(),
            zero = offset * 60 * 1000;

        it('returns 0 in case of new Date(0)', function(){
            expect(JsHelper.timeToSeconds(new Date(zero))).toEqual(0);
        });

        it('returns 3600 in case of new Date(3600000)', function(){
            expect(JsHelper.timeToSeconds(new Date(zero +  3600 * 1000))).toEqual(3600);
        });

    });

    describe('#toZuluTime', function () {
        var toZuluTimeString = function (date) {
                return JsHelper.toZuluTimeString({
                    date: date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate(),
                    hour:  date.getHours(),
                    minute: date.getMinutes()
                });
        };
        
        it('assembles a zulu time string', function () {
            expect(toZuluTimeString(new Date ('2014-07-17T02:00:00Z'))).toEqual('2014-07-17T02:00:00Z');
        });

        it('works overnight', function () {
            expect(toZuluTimeString(new Date ('2014-07-17T00:00:00Z'))).toEqual('2014-07-17T00:00:00Z');
        });

        it('adds a duration', function () {
            var result,
                date = new Date ('2014-07-17T02:00:00Z'),
                input = {
                    date: date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate(),
                    hour:  date.getHours(),
                    minute: date.getMinutes()
                },
                duration = {
                    hour   : '02',
                    minute : '10'
                };
            result = JsHelper.toZuluTimeString(input, duration);
            expect(result).toEqual('2014-07-17T04:10:00Z');
        });

        it('copes with timeless arguments', function () {
            var result,
                 input = {
                    date: '2014-07-17',
                    hour:  undefined,
                    minute: undefined
                };
            result = JsHelper.toZuluTimeString(input);
            expect(result).toEqual('2014-07-17T00:00:00Z');
        });
    });

    describe('#assembleRrule', function () {
        var testData;
        beforeEach(function () {
            jasmine.getJSONFixtures().fixturesPath = 'base/test/unit/fixtures';
            testData = getJSONFixture('newEventMultipleFixture.json');
        });

        it('functions as expected', function () {
            var date = new Date(JsHelper.toZuluTimeString({
                date   : '2014-07-17',
                hour   : '10',
                minute : '0'
            }));
            expect(JsHelper.assembleRrule(testData.source.SCHEDULE_MULTIPLE))
                .toEqual('FREQ=WEEKLY;BYDAY=MO,TU,WE;BYHOUR=' + date.getUTCHours() + ';BYMINUTE=0');
        });
    });
});
