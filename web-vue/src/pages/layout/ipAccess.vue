<template>
  <defaultBg>
    <template #content>
      <a-card hoverable class="card">
        <a-card-meta>
          <template #description>
            <a-result status="error" :title="$t('i18n_2245cf01a3')">
              <template #extra>
                <a-button> {{ $t('i18n_bb9ef827bf') }} </a-button>
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
  methods: {}
}
</script>
