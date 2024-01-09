<template>
  <defaultBg #content>
    <a-card hoverable class="card">
      <a-card-meta>
        <template #description>
          <a-result status="error" title="您没有权限访问">
            <template #extra>
              <a-button> 禁止访问 </a-button>
            </template>
          </a-result></template
        ></a-card-meta
      ></a-card
    >
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
  }
}
</script>
