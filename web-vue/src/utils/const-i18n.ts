import { t } from '@/i18n'

/**
 * mfa app 应用举例
 */
export const MFA_APP_TIP_ARRAY = [
  t('utils.const.3d7f1632'),
  t('utils.const.ee0d1cb6'),
  t('utils.const.316edf4e'),
  t('utils.const.571efa83'),
  t('utils.const.4c20714f'),
  t('utils.const.c0f69a6e')
]

/**
 * 项目 DSL 示例
 */
export const PROJECT_DSL_DEFATUL =
  t('utils.const.ba767044') +
  '\n' +
  t('utils.const.95d754a0') +
  '\n' +
  t('utils.const.59e55b4') +
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
  t('utils.const.42cca3ee') +
  '#  execPath: ./\r\n' +
  'file:\r\n' +
  t('utils.const.5b2ef0bb') +
  '#  backupCount: 5\r\n' +
  t('utils.const.c4682b3f') +
  "#  backupSuffix: [ '.jar','.html','^.+\\.(?i)(txt)$' ]\r\n" +
  t('utils.const.1f958da6') +
  '#  backupPath: /data/jpom_backup\r\n' +
  'config:\r\n' +
  t('utils.const.ae41f11b') +
  '#  autoBackToFile: true\r\n' +
  '\r\n'

/**
 * 定时 cron 默认提示
 *
 * https://www.npmjs.com/package/cron-parser
 */
export const CRON_DATA_SOURCE = [
  {
    title: t('utils.const.1397b7fa'),
    options: [
      {
        title: '',
        value: ''
      }
    ]
  },
  {
    title: t('utils.const.f397fdb9'),
    options: [
      {
        title: t('utils.const.19a1647f'),
        value: '0 0/1 * * * ?'
      },
      {
        title: t('utils.const.edee406c'),
        value: '0 0/5 * * * ?'
      },
      {
        title: t('utils.const.eff3c3f'),
        value: '0 0/10 * * * ?'
      },
      {
        title: t('utils.const.4cda3b42'),
        value: '0 0/30 * * * ?'
      }
    ]
  },
  {
    title: t('utils.const.5057c1d0'),
    options: [
      {
        title: t('utils.const.f00f01ca'),
        value: '0 0 0/1 * * ?'
      }
    ]
  },
  {
    title: t('utils.const.f661cf9a'),
    options: [
      {
        title: t('utils.const.597995d3'),
        value: '0 0 0,12 * * ?'
      },
      {
        title: t('utils.const.c9219cc4'),
        value: '0 0 0 * * ?'
      }
    ]
  },
  {
    title: t('utils.const.c2d566f2'),
    options: [
      {
        title: t('utils.const.a5ef245b'),
        value: '0/5 * * * * ?'
      },
      {
        title: t('utils.const.222fa259'),
        value: '0/10 * * * * ?'
      },
      {
        title: t('utils.const.7a431b98'),
        value: '0/30 * * * * ?'
      }
    ]
  }
]
