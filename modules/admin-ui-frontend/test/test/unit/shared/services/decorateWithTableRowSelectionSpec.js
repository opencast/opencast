describe('decorateWithTableRowSelection', function () {
    var decorateWithTableRowSelection;

    beforeEach(module('adminNg'));

    beforeEach(inject(function (_decorateWithTableRowSelection_) {
        decorateWithTableRowSelection = _decorateWithTableRowSelection_;
    }));

    describe('#row selection decoration', function () {
        var testObject, setRowsValue =  function(indexes, newValue) {
            angular.forEach(indexes, function (index) {
                testObject.rows[index].selected = newValue;
            });
        };

        beforeEach(function () {
            testObject = {
                rows: [{ id: 0, selected: false }, { id: 1, selected: false }, { id: 2, selected: false },
                    { id: 3, selected: false }, { id: 4, selected: false }]
            };
            decorateWithTableRowSelection(testObject);
        });

        it('is applied', function () {
            var simpleTestObject = {};
            decorateWithTableRowSelection(simpleTestObject);
            expect(simpleTestObject.allSelectedChanged).toBeDefined();
            expect(simpleTestObject.getSelected).toBeDefined();
            expect(simpleTestObject.getSelectedIds).toBeDefined();
            expect(simpleTestObject.rowSelectionChanged).toBeDefined();
            expect(simpleTestObject.deselectAll).toBeDefined();
            expect(simpleTestObject.hasAnySelected()).toBeDefined();
        });

        it('toggles all selection flags', function () {
            testObject.allSelected = true;
            testObject.allSelectedChanged();
            angular.forEach(testObject.rows, function (row) {
                expect(row.selected).toBeTruthy();
            });
        });

        it('gets selected rows', function () {
            setRowsValue([1,3], true);
            var result = testObject.getSelected();
            expect(result.length).toEqual(2);
            expect(result[0].id).toEqual(1);
            expect(result[1].id).toEqual(3);
        });

        it('knows if it has any selected rows', function () {
            expect(testObject.hasAnySelected()).toBeFalsy();
            setRowsValue([1,3], true);
            expect(testObject.hasAnySelected()).toBeTruthy();
        });

        it('gets selected rows ids', function () {
            setRowsValue([0,2], true);
            var result = testObject.getSelectedIds();
            expect(result[0]).toEqual(0);
            expect(result[1]).toEqual(2);
        });

        it('reacts to row selection change', function () {
            testObject.allSelected = true;
            testObject.allSelectedChanged();
            setRowsValue([0], false);
            testObject.rowSelectionChanged(0);
            expect(testObject.allSelected).toBeFalsy();
        });

        it('deselects all', function () {
            setRowsValue([0,1,2,3,4], true);
            testObject.deselectAll();
            angular.forEach(testObject.rows, function (row) {
                expect(row.selected).toBeFalsy();
            });
        });

        it('automatically checks the all checkbox', function () {
            setRowsValue([0,1,2,3,4], true);
            // we pretend that a user has just activated row 0
            testObject.rowSelectionChanged(0);
            expect(testObject.allSelected).toBeTruthy();
        });

        it('updates the all checkbox on request', function () {
            setRowsValue([0,1,2,3,4], true);
            testObject.updateAllSelected();
            expect(testObject.allSelected).toBeTruthy();
            setRowsValue([0,1,2,3,4], false);
            testObject.updateAllSelected();
            expect(testObject.allSelected).toBeFalsy();
        });
    });
});
