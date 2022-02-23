<template>
  <a-config-provider :locale="locale">
    <div id="app" :class="`${this.scrollbarFlag ? '' : 'hide-scrollbar'}`">
      <router-view v-if="routerActivation" />
      <template>
        <a-back-top />
      </template>
      <a-spin v-bind="globalLoadingProps">
        <div></div>
      </a-spin>
    </div>
  </a-config-provider>
</template>

<script>
import zhCN from "ant-design-vue/lib/locale-provider/zh_CN";
import Vue from "vue";
import { mapGetters } from "vuex";

export default {
  name: "App",
  data() {
    return {
      locale: zhCN,
      routerActivation: true,
      globalLoadingProps: {
        spinning: false,
        tip: "加载中",
        size: "large",
        delayTime: 500,
      },
    };
  },
  provide() {
    return {
      reload: this.reload,
    };
  },
  components: {},
  computed: {
    ...mapGetters(["getGuideCache"]),
    scrollbarFlag() {
      return this.getGuideCache.scrollbarFlag === undefined ? true : this.getGuideCache.scrollbarFlag;
    },
  },
  created() {
    this.$notification.config({
      top: "100px",
      duration: 4,
    });

    this.$message.config({ duration: 4 });
  },
  methods: {
    reload() {
      this.routerActivation = false;
      this.$nextTick(() => {
        this.routerActivation = true;
      });
    },
  },
  beforeCreate() {
    Vue.prototype.$app = this;
  },
};
</script>

<style lang="stylus">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 0;
}

.full-content {
  min-height calc(100vh - 120px);
}

.node-full-content {
  min-height calc(100vh - 130px) !important;
}

.globalLoading {
  height: 100vh;
  width: 100vw;
  padding: 20vh;
  background-color: rgba(0, 0, 0, 0.7);
  position: fixed !important;
  z-index: 99999;
  top:0;
  bottom:0;
  left:0;
  right:0;
  display:flex;
}
.ant-spin-text{
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
