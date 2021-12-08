<template>
  <a-config-provider :locale="locale">
    <div id="app">
      <router-view v-if="routerActivation" />
    </div>
  </a-config-provider>
</template>

<script>
import zhCN from "ant-design-vue/lib/locale-provider/zh_CN";

export default {
  name: "App",
  data() {
    return {
      locale: zhCN,
      routerActivation: true,
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
  },
  methods: {
    reload() {
      this.routerActivation = false;
      this.$nextTick(() => {
        this.routerActivation = true;
      });
    },
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
  min-height calc(100vh - 130px);
}
</style>
