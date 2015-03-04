define(['engage/models/engage'], function (EngageModel) {
  describe("EngageModel", function () {
    var engageModel;

    beforeEach(function () {
      //create a model to test
      engageModel = new EngageModel;
    });

    it("should have a pluginsInfo model", function () {
      expect(engageModel.get("pluginsInfo")).toBeDefined();
    });
    it("should have a URL parameter list", function () {
      expect(engageModel.get("urlParameters")).toBeDefined();
    });
    it("should have a me info model", function () {
      expect(engageModel.get("meInfo")).toBeDefined();
    });

  });
});
