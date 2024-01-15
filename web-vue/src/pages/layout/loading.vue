<template>
  <div style="display: flex; align-items: center; justify-content: center; width: 100vw; height: 100vh">loading</div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()

onMounted(() => {
  const userStore = useUserStore()

  if (userStore.userInfo && userStore.getToken()) {
    router.push({ path: '/overview' })
  } else {
    // 将参数携带进去
    router.push({
      path: '/login',
      query: route.query
    })
  }
})
</script>
