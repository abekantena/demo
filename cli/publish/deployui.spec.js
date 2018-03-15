"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const deployui_1 = require("./deployui");
describe('sample test for setting up jest', () => {
    test('null check for deployUI', () => {
        const deployUI = deployui_1.default.instance;
        expect(deployUI).toBeInstanceOf(deployui_1.default);
        deployUI.close();
    });
});
//# sourceMappingURL=deployui.spec.js.map