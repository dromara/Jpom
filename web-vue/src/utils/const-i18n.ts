import { t } from '@/i18n'

const mfa1 = t('i18n_252706a112')
const mfa2 = t('i18n_1b963fd303')
const mfa3 = t('i18n_4a00d980d5')
const mfa4 = t('i18n_2cdcfcee15')
const mfa5 = t('i18n_bef1065085')
const mfa6 = t('i18n_8b73b025c0')
const mfa7 = t('i18n_a98233b321')
const mfa8 = t('i18n_b399058f25')

/**
 * mfa app 应用举例
 */

export const MFA_APP_TIP_ARRAY = [
  `<strong${mfa1}></strong$>`,
  `<strong>${mfa2}</strong> ${mfa3} <a href="https://a.app.qq.com/o/simple.jsp?pkgname=com.tencent.authenticator">Android</a>`,
  `<strong>Authy</strong> ${mfa4} <a href="https://authy.com/download/">iOS/Android/Windows/Mac/Linux</a> &nbsp; <a href="https://chrome.google.com/webstore/detail/authy/gaedmjdfmmahhbjefcbgaolhhanlaolb?hl=cn">${mfa5}</a>`,
  `<strong>Google Authenticator</strong> ${mfa6}  <a href="https://apps.apple.com/us/app/google-authenticator/id388497605">iOS</a> <a href="https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2&amp;hl=cn">Android</a>`,
  `<strong>Microsoft Authenticator</strong> ${mfa7} <a href="https://www.microsoft.com/zh-cn/account/authenticator">iOS/Android</a>`,
  `<strong>1Password</strong> ${mfa8}<a href="https://1password.com/zh-cn/downloads/">iOS/Android/Windows/Mac/Linux/ChromeOS</a>`
]

/**
 * 项目 DSL 示例
 */
export const PROJECT_DSL_DEFATUL =
  t('i18n_3f5af13b4b') +
  '\n' +
  t('i18n_13c76c38b7') +
  '\n' +
  'description:' +
  t('i18n_db06c78d1e') +
  '\n' +
  'run:\r\n' +
  '  start:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: start\r\n' +
  '    scriptEnv:\r\n' +
  '      "boot_active": test\r\n' +
  '  status:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: status\r\n' +
  '  stop:\r\n' +
  '#    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '    scriptId: \r\n' +
  '    scriptArgs: stop\r\n' +
  '#  restart:\r\n' +
  '##    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '#    scriptId: \r\n' +
  '#    scriptArgs: restart\r\n' +
  '#    scriptEnv:\r\n' +
  '#      "boot_active": test\r\n' +
  '#  reload:\r\n' +
  '##    scriptId: project.sh\r\n' +
  '#    scriptId: G@xxxx\r\n' +
  '#    scriptId: \r\n' +
  '#    scriptArgs: reload\r\n' +
  '#    scriptEnv:\r\n' +
  '#      "boot_active": test\r\n' +
  '#  fileChangeReload: true\r\n' +
  t('i18n_8d6d47fbed') +
  '#  execPath: ./\r\n' +
  'file:\r\n' +
  t('i18n_0eccc9451d') +
  '#  backupCount: 5\r\n' +
  t('i18n_8ba977b4b7') +
  "#  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]\r\n" +
  t('i18n_7b61408779') +
  '#  backupPath: /data/jpom_backup\r\n' +
  'config:\r\n' +
  t('i18n_0d467f7889') +
  '#  autoBackToFile: true\r\n' +
  '\r\n'

/**
 * 定时 cron 默认提示
 *
 * https://www.npmjs.com/package/cron-parser
 */
export const CRON_DATA_SOURCE = [
  {
    title: t('i18n_6948363f65'),
    options: [
      {
        title: '',
        value: ''
      }
    ]
  },
  {
    title: t('i18n_d5d46dd79b'),
    options: [
      {
        title: t('i18n_76ebb2be96'),
        value: '0 0/1 * * * ?'
      },
      {
        title: t('i18n_b2f296d76a'),
        value: '0 0/5 * * * ?'
      },
      {
        title: t('i18n_3bdab2c607'),
        value: '0 0/10 * * * ?'
      },
      {
        title: t('i18n_751a79afde'),
        value: '0 0/30 * * * ?'
      }
    ]
  },
  {
    title: t('i18n_99b3c97515'),
    options: [
      {
        title: t('i18n_860c00f4f7'),
        value: '0 0 0/1 * * ?'
      }
    ]
  },
  {
    title: t('i18n_15fa91e3ab'),
    options: [
      {
        title: t('i18n_616879745d'),
        value: '0 0 0,12 * * ?'
      },
      {
        title: t('i18n_8844085e15'),
        value: '0 0 0 * * ?'
      }
    ]
  },
  {
    title: t('i18n_8da42dd738'),
    options: [
      {
        title: t('i18n_6334eec584'),
        value: '0/5 * * * * ?'
      },
      {
        title: t('i18n_14a25beebb'),
        value: '0/10 * * * * ?'
      },
      {
        title: t('i18n_354a3dcdbd'),
        value: '0/30 * * * * ?'
      }
    ]
  }
]
