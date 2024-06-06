<template>
  <defaultBg>
    <template #content>
      <a-card hoverable class="card">
        <a-card-meta>
          <template #description>
            <a-result status="error" :title="$t('pages.layout.ipAccess.af7fa63c')">
              <template #extra>
                <a-button> {{ $t('pages.layout.ipAccess.3676f786') }} </a-button>
              </template>
            </a-result>
          </template>
        </a-card-meta>
      </a-card>
    </template>
  </defaultBg>
</template>
<script>
import defaultBg from '@/pages/layout/default-bg.vue'
export default {
  components: {
    defaultBg
  },
  data() {
    return {}
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.layout.ipAccess.${key}`, ...args)
    }
  }
}
</script>
