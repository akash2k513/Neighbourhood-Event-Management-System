module.exports = {
    testEnvironment: "jsdom",

    transform: {
        "^.+\\.[jt]sx?$": "babel-jest"
    },

    moduleNameMapper: {
        "\\.(css|less|scss)$": "<rootDir>/src/__mocks__/fileMock.js"
    },

    moduleFileExtensions: ["js", "jsx"],

    testMatch: ["**/?(*.)+(test).[jt]s?(x)"],

    setupFilesAfterEnv: ["<rootDir>/src/setupTests.js"]
};
