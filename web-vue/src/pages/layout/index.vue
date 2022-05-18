<template>
  <a-layout id="app-layout">
    <a-layout-sider v-model="collapsed" :trigger="null" collapsible :class="`${this.fullScreenFlag ? 'sider-scroll' : 'sider-full-screen'}`">
      <a-tooltip placement="right" title="点击可以折叠左侧菜单栏">
        <div class="logo" @click="changeCollapsed()">
          <img :src="logoUrl" />
          {{ this.subTitle }}
        </div>
      </a-tooltip>
      <side-menu class="side-menu" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="app-header">
        <!-- <a-tooltip placement="right" title="折叠左侧菜单栏">
          <a-icon class="icon-btn" :type="collapsed ? 'menu-unfold' : 'menu-fold'" @click="changeCollapsed" />
        </a-tooltip> -->
        <!-- <a-tooltip placement="right" v-if="getTabList.length > 1" title="关闭其他标签，只保留当前的 Tab">
          <a-icon class="icon-btn" style="padding-left: 0" type="close-circle" @click="closeTabs" />
        </a-tooltip> -->
        <content-tab />
        <!-- <user-header /> -->
      </a-layout-header>
      <!-- class="layout-content" -->
      <a-layout-content :class="`layout-content ${this.fullScreenFlag ? 'layout-content-scroll' : 'layout-content-full-screen'}`">
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
// import UserHeader from "./user-header";
import ContentTab from "./content-tab";
import { checkSystem } from "@/api/install";

export default {
  components: {
    SideMenu,
    // UserHeader,
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
    ...mapGetters(["getCollapsed", "getGuideCache"]),
    fullScreenFlag() {
      return this.getGuideCache.fullScreenFlag === undefined ? true : this.getGuideCache.fullScreenFlag;
    },
  },
  watch: {},
  mounted() {
    this.checkSystem();

    this.collapsed = this.getCollapsed ? true : false;
  },
  methods: {
    // 页面引导
    introGuide() {
      // 如果要显示引导并且没有使用过
      this.$store.dispatch("tryOpenGuide", {
        key: "index",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              intro:
                "<p>不要慌，这是页面导航系统,介绍界面上的一些基本信息.</br>" +
                '<span style="color:red;"><b>第一次使用本系统强烈建议您简单看看引导</b></span></br>' +
                "如果您不想看到，可以点击<b>空白处</b>直接关闭。</p><p>另外，可以使用键盘<b>左右方向键</b>切换上一步或者下一步哦</p>",
            },
            {
              title: "导航助手",
              element: document.querySelector(".logo"),
              intro: "点击这里可以折叠切换左侧菜单栏",
            },
            {
              title: "导航助手",
              element: document.querySelector(".side-menu"),
              intro: "这里是侧边栏菜单区域，温馨提醒系统中还存在【节点管理】导航哟，期待您挖掘",
            },
            {
              title: "导航助手",
              element: document.querySelector(".jpom-workspace"),
              intro: "这里是工作空间,可以自由切换工作空间",
            },
            {
              title: "导航助手",
              element: document.querySelector(".jpom-user-operation"),
              intro: "这里可以设置当前管理员的邮箱或者其他信息，开启关闭导航，重置导航等，当然还有退出登录",
            },
            {
              title: "导航助手",
              element: document.querySelector(".app-header"),
              intro: "这是页面头部，会出现多个 Tab 标签页，以及个人信息等操作按钮",
            },
            {
              title: "导航助手",
              element: document.querySelector(".ant-tabs-nav-wrap"),
              intro: "这里是打开的选项卡，选项卡支持右键菜单哟(关闭其他,关闭左侧,关闭右侧)",
            },
            {
              title: "导航助手",
              element: document.querySelector(".layout-content"),
              intro: "这里是主要的内容展示区域",
            },
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-manage-add"),
              intro: "如果节点列表中还没有节点 可以点击【新增】按钮新增节点",
            },
          ],
        },
      });
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

          // 禁用导航
          this.$store.dispatch("disabledGuide", res.data.disabledGuide);
          this.$notification.config({
            placement: res.data.notificationPlacement ? res.data.notificationPlacement : "topRight",
          });
        }
        if (res.code !== 200) {
          this.$notification.warn({
            message: res.msg,
          });
        } else {
          this.introGuide();
        }
        if (res.code === 999) {
          this.$router.push("/system/ipAccess");
        } else if (res.code === 222) {
          this.$router.push("/install");
        }
      });
    },
    changeCollapsed() {
      this.collapsed = !this.collapsed;
      this.$store.dispatch("collapsed", this.collapsed ? 1 : 0);
    },
  },
};
</script>
<style scoped lang="stylus">
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
  padding: 10px 10px 0;
  height: auto;
}
.sider-scroll {
  min-height: 100vh;
  overflow-y: auto;
}
.sider-full-screen {
  height: 100vh;
  overflow-y: scroll;
}

.layout-content-scroll {
  overflow-y: auto;
}
.layout-content-full-screen {
  height calc(100vh - 120px);
  overflow-y: scroll;
}
</style>

<style>
.layout-content {
  margin: 0;
  padding: 15px 15px 0;
  background: #fff;
  /* min-height: 280px; */
}
.drawer-layout-content {
  min-height: calc(100vh - 85px);
  overflow-y: auto;
}
</style>
