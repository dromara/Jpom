<template>
  <defaultBg #content>
    <a-card hoverable class="card">
      <a-card-meta>
        <template #description>
          <a-result status="404" title="404" sub-title="您访问的页面不存在">
            <a-space>
              <a-button type="primary" @click="goHome">
                <HomeOutlined />
                <span>回到主页</span>
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
  </defaultBg>
</template>
<script lang="ts" setup>
import defaultBg from '@/pages/layout/default-bg.vue'
const router = useRouter()
const goBack = () => {
  router.back()
}
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
