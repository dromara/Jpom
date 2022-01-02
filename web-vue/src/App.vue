<template>
  <a-config-provider :locale="locale">
    <div id="app">
      <a-spin v-bind="globalLoadingProps">
        <div></div>
      </a-spin>
      <router-view v-if="routerActivation" />
      <template>
        <a-back-top />
      </template>
    </div>
  </a-config-provider>
</template>

<script>
import zhCN from "ant-design-vue/lib/locale-provider/zh_CN";
import Vue from "vue";

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
  background-color: rgba(0, 0, 0, 0.05);
  position: fixed !important;
  z-index: 99999;
  top:0;
  bottom:0;
  left:0;
  right:0;
  display:flex;
}
</style>
