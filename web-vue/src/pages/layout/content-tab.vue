<template>
  <a-tabs v-model="activeKey" class="my-tabs" hide-add type="editable-card" @edit="onEdit" @change="changeTab">
    <a-tab-pane v-for="tab in getTabList" :key="tab.key" :tab="tab.title" :closeable="tab.closeable">。。。</a-tab-pane>
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
        if (this.getTabList.length === 1) {
          this.$notification.warn({
            message: '不能关闭了',
            duration: 2
          });
          return;
        }
        this.$store.dispatch('removeTab', key).then(() => {
          const index = this.getTabList.findIndex(ele => ele.key === this.activeKey);
          const activeTab = this.getTabList[index];
          this.$router.push(activeTab.path);
        });
      }
    },
    // 改变 Tab
    changeTab(activekey) {
      const index = this.getTabList.findIndex(ele => ele.key === activekey);
      const currentTab = this.getTabList[index];
      this.$store.dispatch('activeMenu', currentTab.id);
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
  max-width: calc(100vw - 600px);
}
</style>