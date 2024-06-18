<template>
  <defaultBg>
    <template #content>
      <a-card hoverable class="card">
        <a-card-meta>
          <template #description>
            <a-result status="404" title="404" :sub-title="$t('i18n_603dc06c4b')">
              <a-space>
                <a-button type="primary" @click="goHome">
                  <HomeOutlined />
                  <span>{{ $t('i18n_5a1367058c') }}</span>
                </a-button>
                <!-- <a-button type="primary" @click="goBack">
                  <left-outlined />
                  <span>返回上一页</span>
                </a-button> -->
              </a-space>
            </a-result>
          </template>
        </a-card-meta>
      </a-card>
    </template>
  </defaultBg>
</template>
<script lang="ts" setup>
import defaultBg from '@/pages/layout/default-bg.vue'
import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const router = useRouter()
// const goBack = () => {
//   router.back()
// }
const goHome = () => {
  router.replace({ path: '/' })
}
</script>
<style scoped>
.card {
  width: 400px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}
</style>
