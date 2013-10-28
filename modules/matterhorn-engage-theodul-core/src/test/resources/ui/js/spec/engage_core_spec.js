/*global define, describe, beforeEach, it, expect*/
define(['engage/engage_core'], function (EngageCore) {
  describe("EngageCore", function () {
    var engageCore;

    beforeEach(function () {
      engageCore = EngageCore;
    });

    it("should have a model", function () {
      expect(engageCore.model).toBeDefined();
    });

  });
});
