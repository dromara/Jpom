<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible class="sider">
      <div class="logo">Jpom</div>
      <side-menu />
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0">
        <a-icon
          class="trigger"
          :type="collapsed ? 'menu-unfold' : 'menu-fold'"
          @click="() => (collapsed = !collapsed)"
        />
        <span>{{ getUserName }}</span>
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
import SideMenu from './side-menu';
export default {
  components: {
    SideMenu
  },
  data() {
    return {
      collapsed: false
    }
  },
  computed: {
    ...mapGetters([
      'getToken',
      'getUserName'
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
.sider {
  max-height: 100vh;
  overflow-y: auto;
}
</style>