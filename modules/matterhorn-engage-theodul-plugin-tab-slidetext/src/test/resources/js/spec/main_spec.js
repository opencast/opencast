/*global define, describe, beforeEach, it, expect, jasmine*/
define(['main'], function (Main) {
  describe("Main", function () {

    describe("should be", function () {
      it("defined", function () {
        expect(Main).toBeDefined();
      });

      it("not be null", function () {
        expect(Main).not.toBeNull();
      });

      it("an object", function () {
        expect(Main).toEqual(jasmine.any(Object));
      });
    });

    describe("should have a property", function () {
      describe("name", function () {

        it("defined", function () {
          expect(Main.name).toBeDefined();
        });

        it("that not should be null", function () {
          expect(Main.name).not.toBeNull();
        });

        it("with a minimum length of 1", function () {
          expect(Main.name.length).toBeGreaterThan(0);
        });

        it("with a maximum length of 255", function () {
          expect(Main.name.length).toBeLessThan(255);
        });

        it("that is of the type String", function () {
          expect(Main.name).toEqual(jasmine.any(String));
        });

      });

      describe("type", function () {
        it("defined", function () {
          expect(Main.type).toBeDefined();
        });

        it("that not should be null", function () {
          expect(Main.type).not.toBeNull();
        });

        it("with a minimum length of 1", function () {
          expect(Main.type.length).toBeGreaterThan(0);
        });

        it("with a maximum length of 255", function () {
          expect(Main.type.length).toBeLessThan(255);
        });

        it("that is of the type String", function () {
          expect(Main.type).toEqual(jasmine.any(String));
        });
      });

      describe("version", function () {
        it("defined", function () {
          expect(Main.version).toBeDefined();
        });

        it("that not should be null", function () {
          expect(Main.version).not.toBeNull();
        });

        it("with a minimum length of 1", function () {
          expect(Main.version.length).toBeGreaterThan(0);
        });

        it("with a maximum length of 255", function () {
          expect(Main.version.length).toBeLessThan(255);
        });

        it("that is of the type String", function () {
          expect(Main.version).toEqual(jasmine.any(String));
        });
      });

      describe("styles", function () {
        it("defined", function () {
          expect(Main.styles).toBeDefined();
        });

        it("that not should be null", function () {
          expect(Main.styles).not.toBeNull();
        });

        it("which is an array with a minimum length of 0", function () {
          expect(Main.styles.length).toBeGreaterThan(-1);
        });

        it("which is an array with a maximum length of 255", function () {
          expect(Main.styles.length).toBeLessThan(255);
        });

        it("that is of the type Object", function () {
          expect(Main.styles).toEqual(jasmine.any(Object)); // Type of array is object!
        });
      });

      describe("template", function () {
        it("defined", function () {
          expect(Main.version).toBeDefined();
        });

        it("that not should be null", function () {
          expect(Main.version).not.toBeNull();
        });

        it("with a minimum length of 1", function () {
          expect(Main.version.length).toBeGreaterThan(0);
        });

        it("with a maximum length of 255", function () {
          expect(Main.version.length).toBeLessThan(255);
        });

        it("that is of the type String", function () {
          expect(Main.version).toEqual(jasmine.any(String));
        });
      });
    });

    describe("should should calculate seconds of given time string", function () {
      it("00:00:00 should be 0", function () {
        expect(Main.timeStrToSeconds("00:00:00")).toEqual(0);
      });
      it("00:00:59 should be 59", function () {
        expect(Main.timeStrToSeconds("00:00:59")).toEqual(59);
      });
      it("00:01:01 should be 61", function () {
        expect(Main.timeStrToSeconds("00:01:01")).toEqual(61);
      });
      it("00:10:01 should be 601", function () {
        expect(Main.timeStrToSeconds("00:10:01")).toEqual(601);
      });
      it("00:59:59 should be 3599", function () {
        expect(Main.timeStrToSeconds("00:59:59")).toEqual(3599);
      });
      it("01:01:01 should be 3661", function () {
        expect(Main.timeStrToSeconds("01:01:01")).toEqual(3661);
      });
      it("23:59:59 should be 86399", function () {
        expect(Main.timeStrToSeconds("23:59:59")).toEqual(86399);
      });
    });
  });
});