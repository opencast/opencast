define(['engage/engage_desktop_view'], function (EngageDesktopView) {
  describe("EngageDesktopView", function () {
    var engageDesktopView;

    beforeEach(function () {
      engageDesktopView = EngageDesktopView;
    });

    it("should have a initView function", function () {
      expect(engageDesktopView.initView).toBeDefined();
    });

    it("should have a insertPlugin function", function () {
      expect(engageDesktopView.insertPlugin).toBeDefined();
    });

    it("should have a allPluginsLoaded function", function () {
      expect(engageDesktopView.allPluginsLoaded).toBeDefined();
    });

  });
});