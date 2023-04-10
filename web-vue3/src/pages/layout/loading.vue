<template>
  <div style="display: flex; align-items: center; justify-content: center; width: 100vw;height: 100vh; ">loading</div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/stores/user';
import { onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';

const router = useRouter();
const route = useRoute();

onMounted(() => {
  const userStore = useUserStore();

  if (userStore.userInfo && userStore.token) {
    router.push({ path: "/node/list" });
  } else {
    // 将参数携带进去
    router.push({
      path: "/login",
      query: route.query,
    });
  }
});
</script>
