const { i18nTools } = require('jpom-i18n')
const path = require('path')
const fs = require('fs')
const dotEnv = require('dotenv')
dotEnv.config()

let prettierrCconfig
try {
  const prettierrcPath = path.join(process.cwd(), '.prettierrc.json')
  prettierrCconfig = fs.readFileSync(prettierrcPath, 'utf-8')
} catch (e) {
  //
}
prettierrCconfig = prettierrCconfig
  ? JSON.parse(prettierrCconfig)
  : {
      semi: false,
      singleQuote: true,
      endOfLine: 'auto',
      proseWrap: 'never',
      printWidth: 120,
      trailingComma: 'none'
    }

const main = async () => {
  const config = {
    projectPath: process.cwd(),
    globalPath: 'src',
    includeDir: ['api', 'components', 'pages', 'stores', 'utils', 'App.vue'],
    excludeFileOrDir: [],
    exts: ['.vue', '.ts', '.js'],
    prettier: prettierrCconfig,
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
