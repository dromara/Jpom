<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible class="sider">
      <div class="logo" @click="toggleGuide()">
        <img src="/logo_image" />
        {{ this.subName }}
      </div>
      <side-menu class="side-menu" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="app-header">
        <a-icon class="trigger" :type="collapsed ? 'menu-unfold' : 'menu-fold'" @click="() => (collapsed = !collapsed)" />
        <content-tab />
        <user-header />
      </a-layout-header>
      <a-layout-content class="layout-content">
        <keep-alive>
          <router-view />
        </keep-alive>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>
<script>
import { mapGetters } from "vuex";
import SideMenu from "./side-menu";
import UserHeader from "./user-header";
import ContentTab from "./content-tab";
import { checkSystem } from "@/api/install";
import { GUIDE_HOME_USED_KEY } from "@/utils/const";
export default {
  components: {
    SideMenu,
    UserHeader,
    ContentTab,
  },
  data() {
    return {
      collapsed: false,
      subName: "项目管理",
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag"]),
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    },
  },
  mounted() {
    this.checkSystem();
    this.introGuide();
  },
  methods: {
    // 页面引导
    introGuide() {
      const used = localStorage.getItem(GUIDE_HOME_USED_KEY) === "true";
      // 如果要显示引导并且没有使用过
      if (this.getGuideFlag && !used) {
        this.$introJs()
          .setOptions({
            hidePrev: true,
            steps: [
              {
                title: "Jpom 导航助手",
                intro: "<p>不要慌，这是新版本的页面导航系统，如果你不想看到，可以点击<b>空白处</b>直接关闭。</p><p>另外，可以使用键盘<b>左右方向键</b>切换上一步或者下一步哦</p>",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".logo"),
                intro: "点击这里可以切换是否开启导航",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".side-menu"),
                intro: "这里是侧边栏菜单区域",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".app-header"),
                intro: "这是页面头部，会出现多个 Tab 标签页，以及个人信息等操作按钮",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-close-tabs"),
                intro: "这里的关闭 Tab 按钮只会保留当前激活的 Tab",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-user-operation"),
                intro: "这里可以设置当前管理员的邮箱或者其他信息，当然还有退出登录",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".layout-content"),
                intro: "这里是主要的内容展示区域",
              },
            ],
          })
          .start()
          .onexit(() => {
            localStorage.setItem(GUIDE_HOME_USED_KEY, "true");
          });
        return false;
      }
      this.$introJs().exit();
    },
    // 切换引导
    toggleGuide() {
      if (!this.getGuideFlag) {
        this.$notification.success({
          message: "开启页面导航",
          duration: 2,
        });
        // 重置 GUIDE_HOME_USED_KEY
        localStorage.setItem(GUIDE_HOME_USED_KEY, "false");
      } else {
        this.$notification.success({
          message: "关闭页面导航",
          duration: 2,
        });
      }
      this.$store.dispatch("toggleGuideFlag");
    },
    // 检查是否需要初始化
    checkSystem() {
      checkSystem().then((res) => {
        if (res.data) {
          window.routerBase = res.data.routerBase || "";
          if (res.data.subName) {
            this.subName = res.data.subName;
          }
        }

        if (res.code === 999) {
          //
          this.$router.push("/system/ipAccess");
        } else if (res.code !== 200) {
          this.$notification.warn({
            message: res.msg,
            duration: 2,
          });
          this.$router.push("/install");
        }
      });
    },
  },
};
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
  width: 100%;
  cursor: pointer;
  height: 32px;
  margin: 20px 0 12px;
  font-size: 20px;
  color: #fff;
  font-weight: bold;
  overflow: hidden;
  padding: 0 16px;
}
#app-layout .logo img {
  height: 26px;
  vertical-align: sub;
  margin-right: 6px;
}
.app-header {
  display: flex;
  background: #fff;
  padding: 0;
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
