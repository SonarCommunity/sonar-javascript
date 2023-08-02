module.exports = {
  collectCoverageFrom: ['packages/**/*.ts'],
  globals: {
    'ts-jest': {
      tsconfig: 'tests/tsconfig.json',
    },
  },
  moduleFileExtensions: ['js', 'ts', 'json'],
  moduleDirectories: [
    'node_modules',
    '<rootDir>/packages/legacy/src',
    '<rootDir>/tests/**/fixtures',
  ],
  moduleNameMapper: {
    '^@sonar/css$': '<rootDir>/packages/css/src',
    '^@sonar/jsts$': '<rootDir>/packages/jsts/src',
    '^server$': '<rootDir>/packages/server',
    '^routing/(.*)$': '<rootDir>/packages/bridge/src/$1',
    '^@sonar/css/(.*)$': '<rootDir>/packages/css/src/$1',
    '^@sonar/jsts/(.*)$': '<rootDir>/packages/jsts/src/$1',
  },
  modulePathIgnorePatterns: [
    '<rootDir>/tests/linting/eslint/rules/fixtures/no-implicit-dependencies/bom-package-json-project/package.json',
  ],
  testResultsProcessor: 'jest-sonar-reporter',
  transform: {
    '^.+\\.ts$': 'ts-jest',
  },
  testMatch: ['<rootDir>/tests/**/*.test.ts'],
  testTimeout: 20000,
};
