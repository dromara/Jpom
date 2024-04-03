import { useI18n } from 'vue-i18n'

/**
 * @name useI18nPage
 * @description
 */
export const useI18nPage = <T = any>(pagePath: string) => {
  const { t: $t } = useI18n()
  const $tPage = $t(pagePath) as T
  return {
    $t,
    $tPage
  }
}
