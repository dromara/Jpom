module.exports = {
  singleQuote: true,
  trailingComma: 'all',
  printWidth: 200,
  proseWrap: 'never',
  endOfLine: 'lf',
  overrides: [
    {
      files: '.prettierrc',
      options: {
        parser: 'json',
      },
    }
  ],
};
