<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible class="sider">
      <div class="logo" @click="toggleGuide()">
        <img src="../../assets/images/jpom.jpeg"/>
      </div>
      <side-menu class="side-menu" />
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
import { mapGetters } from 'vuex';
import SideMenu from './side-menu';
import UserHeader from './user-header';
import ContentTab from './content-tab';
import { checkSystem } from '../../api/install';
export default {
  components: {
    SideMenu,
    UserHeader,
    ContentTab
  },
  data() {
    return {
      collapsed: false,
    }
  },
  computed: {
    ...mapGetters([
      'getGuideFlag'
    ])
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    }
  },
  mounted() {
    this.checkSystem();
    this.introGuide();
    this.$introJs().start();
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs().setOptions({
          steps: [{
            title: '页面导航系统',
            intro: '不要慌，这是新版本的页面导航系统，如果你不想看到，可以点击关闭'
          },{
            element: document.querySelector('.logo'),
            intro: '点击这里可以切换是否开启导航'
          },
          {
            element: document.querySelector('.side-menu'),
            intro: '这里是侧边栏菜单区域'
          },
          {
            element: document.querySelector('.app-header'),
            intro: '这是页面头部，会出现多个 Tab 标签页，以及个人信息等操作按钮'
          },{
            element: document.querySelector('.layout-content'),
            intro: '这里是主要的内容展示区域'
          }]
        }).start();
      }
    },
    // 切换引导
    toggleGuide() {
      console.log(this.getGuideFlag)
      if (!this.getGuideFlag) {
        this.$notification.success({
          message: '开启页面导航',
          duration: 2
        });
      } else {
        this.$notification.success({
          message: '关闭页面导航',
          duration: 2
        });
      }
      this.$store.dispatch('toggleGuideFlag');
    },
    // 检查是否需要初始化
    checkSystem() {
      checkSystem().then(res => {
        if (res.code !== 200) {
          this.$notification.warn({
            message: res.msg,
            duration: 2
          });
          this.$router.push('/install');
        }
      })
    }
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
  cursor: pointer;
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