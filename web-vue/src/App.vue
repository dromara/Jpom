<template>
  <a-config-provider :locale="locale">
  <div id="app">
    <router-view />
  </div>
  </a-config-provider>
</template>

<script>

import zhCN from 'ant-design-vue/lib/locale-provider/zh_CN';
export default {
  name: 'App',
  data() {
    return {
      locale: zhCN,
    };
  },
  components: {
  },
  created() {
    this.$notification.config({
      top: '100px',
      duration: 2,
    });
  }
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 0;
  padding: 0;
}
</style>
