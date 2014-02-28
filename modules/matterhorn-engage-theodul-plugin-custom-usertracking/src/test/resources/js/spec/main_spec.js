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

  });
});