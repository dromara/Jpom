<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible class="sider">
      <div class="logo">
        <img src="../../assets/images/jpom.jpeg"/>
      </div>
      <side-menu />
    </a-layout-sider>
    <a-layout>
      <a-layout-header style="background: #fff; padding: 0">
        <a-icon
          class="trigger"
          :type="collapsed ? 'menu-unfold' : 'menu-fold'"
          @click="() => (collapsed = !collapsed)"
        />
        <top-header />
      </a-layout-header>
      <a-layout-content
        :style="{ margin: '24px 16px', padding: '24px', background: '#fff', minHeight: '280px' }"
      >
        <router-view/>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import SideMenu from './side-menu';
import TopHeader from './top-header';
export default {
  components: {
    SideMenu,
    TopHeader
  },
  data() {
    return {
      collapsed: false
    }
  },
  computed: {
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
  width: 70px;
  height: 32px;
  background: rgba(255, 255, 255, 0.2);
  margin: 16px auto;
}
#app-layout .logo img {
  height: 32px;
}
.sider {
  max-height: 100vh;
  overflow-y: auto;
}
</style>