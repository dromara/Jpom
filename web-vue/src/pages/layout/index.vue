<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible class="sider">
      <a-tooltip placement="right" title="点击可以切换开启操作引导">
        <div class="logo" @click="toggleGuide()">
          <img :src="logoUrl" />
          {{ this.subTitle }}
        </div>
      </a-tooltip>
      <side-menu class="side-menu" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="app-header">
        <a-tooltip placement="right" title="折叠左侧菜单栏">
          <a-icon class="icon-btn" :type="collapsed ? 'menu-unfold' : 'menu-fold'" @click="changeCollapsed" />
        </a-tooltip>
        <a-tooltip placement="right" v-if="getTabList.length > 1" title="关闭其他标签，只保留当前的 Tab">
          <a-icon class="icon-btn" style="padding-left: 0" type="close-circle" @click="closeTabs" />
        </a-tooltip>
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
      subTitle: "项目管理",
      logoUrl: "",
    };
  },
  computed: {
    ...mapGetters(["getGuideFlag", "getTabList", "getCollapsed"]),
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    },
  },
  mounted() {
    this.checkSystem();
    this.introGuide();
    this.collapsed = this.getCollapsed == 1;
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
                intro: "<p>不要慌，这是新版本的页面导航系统，如果您不想看到，可以点击<b>空白处</b>直接关闭。</p><p>另外，可以使用键盘<b>左右方向键</b>切换上一步或者下一步哦</p>",
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
                element: document.querySelector(".jpom-workspace"),
                intro: "这里是工作空间,可以自由切换工作空间",
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
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-node-manage-btn"),
                intro: "点击【节点管理】按钮可以进入节点管理",
              },
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-node-manage-add"),
                intro: "如果还没有节点 可以点击【新增】按钮新增节点",
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
        });
        // 重置 GUIDE_HOME_USED_KEY
        localStorage.setItem(GUIDE_HOME_USED_KEY, "false");
      } else {
        this.$notification.success({
          message: "关闭页面导航",
        });
      }
      this.$store.dispatch("toggleGuideFlag");
    },
    // 检查是否需要初始化
    checkSystem() {
      checkSystem().then((res) => {
        if (res.data) {
          window.routerBase = res.data.routerBase || "";
          if (res.data.subTitle) {
            this.subTitle = res.data.subTitle;
          }
          this.logoUrl = ((res.data.routerBase || "") + "/logo_image").replace(new RegExp("//", "gm"), "/");
        }

        if (res.code === 999) {
          //
          this.$router.push("/system/ipAccess");
        } else if (res.code !== 200) {
          this.$notification.warn({
            message: res.msg,
          });
          this.$router.push("/install");
        }
      });
    },
    // 关闭 tabs
    closeTabs() {
      this.$notification.success({
        message: "操作成功",
      });
      this.$store.dispatch("clearTabs");
    },
    changeCollapsed() {
      this.collapsed = !this.collapsed;
      this.$store.dispatch("collapsed", this.collapsed ? 1 : 0);
    },
  },
};
</script>
<style>
#app-layout {
  min-height: 100vh;
}
#app-layout .icon-btn {
  float: left;
  font-size: 18px;
  line-height: 64px;
  padding: 0 14px;
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
  min-height: 100vh;
  overflow-y: auto;
}
.layout-content {
  margin: 10px 10px 0;
  padding: 15px 15px 0;
  background: #fff;
  min-height: 280px;
}
</style>
