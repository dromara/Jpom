import { t } from '@/i18n'

const mfa1 = '【推荐】微信小程序搜索 数盾OTP'
const mfa2 = '【推荐】腾讯身份验证码'
const mfa3 = '简单好用'
const mfa4 = '功能丰富 专为两步验证码'
const mfa5 = 'Chrome 扩展'
const mfa6 = '简单易用，但不支持密钥导出备份'
const mfa7 = '使用微软全家桶的推荐'
const mfa8 = '强大安全的密码管理付费应用'

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
  t('i18n.3f5af13b4b') +
  '\n' +
  t('i18n.13c76c38b7') +
  '\n' +
  'description:' +
  t('i18n.db06c78d1e') +
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
  t('i18n.8d6d47fbed') +
  '#  execPath: ./\r\n' +
  'file:\r\n' +
  t('i18n.0eccc9451d') +
  '#  backupCount: 5\r\n' +
  t('i18n.8ba977b4b7') +
  "#  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]\r\n" +
  t('i18n.7b61408779') +
  '#  backupPath: /data/jpom_backup\r\n' +
  'config:\r\n' +
  t('i18n.0d467f7889') +
  '#  autoBackToFile: true\r\n' +
  '\r\n'

/**
 * 定时 cron 默认提示
 *
 * https://www.npmjs.com/package/cron-parser
 */
export const CRON_DATA_SOURCE = [
  {
    title: t('i18n.6948363f65'),
    options: [
      {
        title: '',
        value: ''
      }
    ]
  },
  {
    title: t('i18n.d5d46dd79b'),
    options: [
      {
        title: t('i18n.76ebb2be96'),
        value: '0 0/1 * * * ?'
      },
      {
        title: t('i18n.b2f296d76a'),
        value: '0 0/5 * * * ?'
      },
      {
        title: t('i18n.3bdab2c607'),
        value: '0 0/10 * * * ?'
      },
      {
        title: t('i18n.751a79afde'),
        value: '0 0/30 * * * ?'
      }
    ]
  },
  {
    title: t('i18n.99b3c97515'),
    options: [
      {
        title: t('i18n.860c00f4f7'),
        value: '0 0 0/1 * * ?'
      }
    ]
  },
  {
    title: t('i18n.15fa91e3ab'),
    options: [
      {
        title: t('i18n.616879745d'),
        value: '0 0 0,12 * * ?'
      },
      {
        title: t('i18n.8844085e15'),
        value: '0 0 0 * * ?'
      }
    ]
  },
  {
    title: t('i18n.8da42dd738'),
    options: [
      {
        title: t('i18n.6334eec584'),
        value: '0/5 * * * * ?'
      },
      {
        title: t('i18n.14a25beebb'),
        value: '0/10 * * * * ?'
      },
      {
        title: t('i18n.354a3dcdbd'),
        value: '0/30 * * * * ?'
      }
    ]
  }
]
