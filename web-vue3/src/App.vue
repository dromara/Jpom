<template>
  <a-config-provider :locale="locale">
    <div id="app" :class="`${scrollbarFlag ? '' : 'hide-scrollbar'}`">
      <router-view v-if="routerActivation" />
      <template>
        <a-back-top />
      </template>
    </div>
  </a-config-provider>
</template>

<script>
import zhCN from 'ant-design-vue/es/locale/zh_CN'
// import { mapGetters } from "vuex";
// import store from "@/store/index";
// import guideStore from '@/stores/guide'
import { notification, message } from 'ant-design-vue'
import { onMounted, ref, provide, computed } from 'vue'

const routerActivation = ref(true)
const locale = ref(zhCN)

const scrollbarFlag = computed(() => {
  // return guideStore.scrollbarFlag === undefined ? true : guideStore.scrollbarFlag
})

onMounted(() => {
  notification.config({
    top: '100px',
    duration: 4,
  })

  message.config({ duration: 4 })
})

const reload = () => {
  routerActivation.value = false
  // 刷新菜单
  // store.dispatch("restLoadSystemMenus").then(() => {
  //   //
  // });
  this.$nextTick(() => {
    routerActivation.value = true
  })
}

provide({
  reload: reload,
})

// export default {
//   name: 'App',
//   data() {
//     return {
//       locale: zhCN,
//       routerActivation: true,
//     }
//   },
//   provide() {
//     return
//   },
//   components: {},
//   // computed: {
//   //   ...mapGetters(["getGuideCache"]),
//   //   scrollbarFlag() {
//   //     return this.getGuideCache.scrollbarFlag === undefined ? true : this.getGuideCache.scrollbarFlag;
//   //   },
//   // },
//   created() {},
//   methods: {},
// }
</script>

<style lang="less">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 0;
}

.full-content {
  min-height: calc(100vh - 120px);
  padding-bottom: 20px;
}

.node-full-content {
  min-height: calc(100vh - 130px) !important;
}

.globalLoading {
  height: 100vh;
  width: 100vw;
  padding: 20vh;
  background-color: rgba(0, 0, 0, 0.7);
  position: fixed !important;
  z-index: 99999;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
}
.ant-spin-text {
  text-shadow: 0 0 black !important;
}
</style>

<style>
.hide-scrollbar *::-webkit-scrollbar {
  width: 0 !important;
  display: none;
}

.hide-scrollbar * {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.hide-scrollbar pre::-webkit-scrollbar {
  width: 0 !important;
  display: none;
}

.hide-scrollbar pre {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
