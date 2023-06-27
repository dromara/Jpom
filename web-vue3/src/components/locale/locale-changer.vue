<template>
  <a-dropdown>
    <a class='ant-dropdown-link' @click='(e) => e.preventDefault()'>
      {{ this.language }}
      <DownOutlined />
    </a>
    <template #overlay>
      <a-menu>
        <a-menu-item @click="handleLocaleChange('zh')"> 中文</a-menu-item>
        <a-menu-item @click="handleLocaleChange('en')"> English</a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script lang='ts'>
import { defineComponent } from 'vue'
import { DownOutlined } from '@ant-design/icons-vue'
import { useLocaleStore } from '@/stores/locale.js'

const localeStore = useLocaleStore()

export default defineComponent({
  components: {
    DownOutlined
  },
  computed: {
    language() {
      switch (localeStore.getLocale) {
        case 'en':
          return 'English'
        case 'zh':
        default:
          return '中文'
      }
    }
  },
  inject: ['reload'],
  methods: {
    handleLocaleChange(locale) {
      localeStore.changeLocale(locale)
      this.$i18n.locale = locale
    }
  }
})
</script>
