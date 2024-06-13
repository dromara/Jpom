const { i18nTools } = require('jpom-i18n')
const path = require('path')
const dotEnv = require('dotenv')
dotEnv.config()
const main = async () => {
  const config = {
    projectPath: process.cwd(),
    globalPath: 'src',
    includeDir: ['pages'],
    exts: ['.vue'],
    prettier: {
      semi: false,
      singleQuote: true,
      endOfLine: 'auto',
      proseWrap: 'never',
      printWidth: 120,
      trailingComma: 'none'
    },
    lang: {
      primaryFile: {
        lang: 'zh',
        path: path.join(process.cwd(), 'src', 'i18n', 'locales', 'zh_cn.json')
      },
      syncFile: [
        {
          lang: 'en',
          path: path.join(process.cwd(), 'src', 'i18n', 'locales', 'en_us.json')
        }
      ]
    },
    translate: {
      bd: {
        clientId: process.env.BAIDU_FY_ID,
        clientSecret: process.env.BAIDU_FY_SECRET,
        grantType: process.env.BAIDU_FY_GRANT_TYPE
      }
    }
  }
  await i18nTools(config)
}
main()
