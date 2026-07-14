module.exports = {
  root: true,
  env: { browser: true, es2022: true, node: true },
  ignorePatterns: ['dist', 'coverage'],
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module', ecmaFeatures: { jsx: true } },
  plugins: ['@typescript-eslint', 'react-hooks', 'react-refresh'],
  extends: ['eslint:recommended', 'plugin:@typescript-eslint/recommended'],
  rules: {
    ...require('eslint-plugin-react-hooks').configs.recommended.rules,
    // Context modules intentionally export both providers and their consumer hooks.
    'react-refresh/only-export-components': 'off',
    '@typescript-eslint/no-explicit-any': 'off',
  },
}
