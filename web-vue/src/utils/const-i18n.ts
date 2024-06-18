import { t } from '@/i18n'

/**
 * mfa app 应用举例
 */
export const MFA_APP_TIP_ARRAY = [
  t('i18n.01c75fe99e'),
  t('i18n.0acd6920d0'),
  t('i18n.746d21e9e2'),
  t('i18n.a6772ad348'),
  t('i18n.d6de7836f8'),
  t('i18n.1643f8ab1b')
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
