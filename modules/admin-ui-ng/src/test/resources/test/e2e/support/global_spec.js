beforeEach(function () {
    browser.clearMockModules();
});

afterEach(function () {
    browser.executeScript('window.sessionStorage.clear();window.localStorage.clear();');
});
