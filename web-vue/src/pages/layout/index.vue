<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible>
      <div class="logo">Jpom</div>
      <a-menu theme="dark" mode="inline" :default-selected-keys="['1-1']" :default-open-keys="['sub1']">
        <a-sub-menu key="sub1">
          <span slot="title">
            <a-icon type="apartment" />
            <span>节点管理</span>
          </span>
          <a-menu-item key="1-1">
            <span>节点列表</span>
          </a-menu-item>
          <a-menu-item key="1-2">
            <span>SSH 列表</span>
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="sub2">
          <span slot="title"><a-icon type="setting" /><span>Navigation Four</span></span>
          <a-menu-item key="7">
            Option 7
          </a-menu-item>
          <a-menu-item key="8">
            Option 8
          </a-menu-item>
          <a-menu-item key="9">
            Option 9
          </a-menu-item>
          <a-menu-item key="10">
            Option 10
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0">
        <a-icon
          class="trigger"
          :type="collapsed ? 'menu-unfold' : 'menu-fold'"
          @click="() => (collapsed = !collapsed)"
        />
      </a-layout-header>
      <a-layout-content
        :style="{ margin: '24px 16px', padding: '24px', background: '#fff', minHeight: '280px' }"
      >
        Content
        {{ getToken }}
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import { mapGetters } from 'vuex';
export default {
  data() {
    return {
      collapsed: false
    }
  },
  computed: {
    ...mapGetters([
      'getToken'
    ])
  }
}
</script>
<style>
#app-layout {
  height: 100vh;
}
#app-layout .trigger {
  float: left;
  font-size: 18px;
  line-height: 64px;
  padding: 0 24px;
  cursor: pointer;
  transition: color 0.3s;
}

#app-layout .trigger:hover {
  color: #1890ff;
}

#app-layout .logo {
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  margin: 16px;
}
</style>