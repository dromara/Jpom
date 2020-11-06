<template>
  <a-tabs v-model="activeKey" class="my-tabs" hide-add type="editable-card" @edit="onEdit" @change="changeTab">
    <a-tab-pane v-for="tab in getTabList" :key="tab.key" :tab="tab.key" :closeable="tab.closeable">。。。</a-tab-pane>
  </a-tabs>
</template>
<script>
import { mapGetters } from 'vuex';
export default {
  data() {
    return {
    }
  },
  computed: {
    ...mapGetters([
      'getActiveTabKey',
      'getTabList'
    ]),
    activeKey: {
      get() {
        return this.getActiveTabKey;
      },
      set(value) {
        const index = this.getTabList.findIndex(ele => ele.key === value);
        const activeTab = this.getTabList[index];
        this.$router.push(activeTab.path);
      }
    }
  },
  methods: {
    // 编辑 Tab
    onEdit(key, action) {
      if (action === 'remove') {
        this.$store.dispatch('removeTab', key).then(() => {
          const index = this.getTabList.findIndex(ele => ele.key === this.activeKey);
          const activeTab = this.getTabList[index];
          this.$router.push(activeTab.path);
        });
      }
    },
    // 改变 Tab
    changeTab(activekey) {
      console.log(activekey);
      // 如果跳转路由跟当前一致
      // if (to.path === from.path) {
      //   notification.warn({
      //     message: '已经在当前页面了',
      //     duration: 2
      //   });
      //   return;
      // }
      // this.$router.push('/node/list');
    }
  }
}
</script>
<style scoped>
.my-tabs {
  flex: auto;
  margin-right: 20px;
  align-self: center;
  height: 40px;
}
</style>