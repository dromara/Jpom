<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible class="sider">
      <div class="logo">
        <img src="../../assets/images/jpom.jpeg"/>
      </div>
      <side-menu />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="app-header">
        <a-icon
          class="trigger"
          :type="collapsed ? 'menu-unfold' : 'menu-fold'"
          @click="() => (collapsed = !collapsed)"
        />
        <content-tab />
        <user-header />
      </a-layout-header>
      <a-layout-content class="layout-content">
        <keep-alive>
          <router-view/>
        </keep-alive>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import SideMenu from './side-menu';
import UserHeader from './user-header';
import ContentTab from './content-tab';
export default {
  components: {
    SideMenu,
    UserHeader,
    ContentTab
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
.app-header {
  display: flex;
  background: #fff;
  padding: 0
}
.sider {
  max-height: 100vh;
  overflow-y: auto;
}
.layout-content {
  margin: 10px;
  padding: 15px;
  background: #fff;
  min-height: 280px;
}
</style>