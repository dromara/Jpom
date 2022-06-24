<template>
  <a-layout class="node-layout" ref="nodeLayout" id="nodeLayout">
    <!-- 侧边栏 节点管理菜单 -->
    <a-layout-sider theme="light" :class="`node-sider jpom-node-sider ${this.fullScreenFlag ? 'sider-scroll' : 'sider-full-screen'}`">
      <a-menu theme="light" mode="inline" @openChange="openChange" :default-selected-keys="selectedKeys" :openKeys="openKey">
        <template v-for="(menu, index) in nodeMenuList">
          <a-sub-menu v-if="menu.childs" :key="menu.id" :class="menu.id">
            <span slot="title">
              <a-icon :type="menu.icon_v3" />
              <span>{{ menu.title }}</span>
            </span>
            <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" @click="handleMenuClick(subMenu.id, subMenu.pId)" :class="subMenu.id">
              <span>{{ subMenu.title }}</span>
            </a-menu-item>
          </a-sub-menu>
          <a-menu-item v-else :key="menu.id + index" @click="handleMenuClick(menu.id)">
            <a-icon :type="menu.icon_v3" />
            <span>{{ menu.title }}</span>
          </a-menu-item>
        </template>
      </a-menu>
    </a-layout-sider>
    <!-- 节点管理的各个组件 -->
    <!-- class="layout-content jpom-node-content drawer-layout-content" -->
    <a-layout-content :class="`layout-content jpom-node-content ${this.fullScreenFlag ? 'layout-content-scroll' : 'layout-content-full-screen'}`">
      <welcome v-if="currentId === 'welcome'" :node="node" />
      <project-list v-if="currentId === 'manageList'" :node="node" />
      <jdk-list v-if="currentId === 'jdkList'" :node="node" />
      <recover v-if="currentId === 'projectRecover'" :node="node" />
      <tomcat v-if="currentId === 'tomcatManage'" :node="node" />
      <script-template v-if="currentId === 'script'" :node="node" />
      <script-log v-if="currentId === 'script-log'" :nodeId="node.id" />
      <nginx-list v-if="currentId === 'nginxList'" :node="node" />
      <cert v-if="currentId === 'certificate'" :node="node" />
      <white-list v-if="currentId === 'whitelistDirectory'" :node="node" />
      <cache v-if="currentId === 'cacheManage'" :node="node" />
      <log v-if="currentId === 'logManage'" :node="node" />
      <upgrade v-if="currentId === 'update'" :node="node" />
      <config-file v-if="currentId === 'sysConfig'" :node="node" />
    </a-layout-content>
  </a-layout>
</template>
<script>
import {getNodeMenu} from "@/api/menu";
import Welcome from "@/pages/node/node-layout/welcome";
import ProjectList from "@/pages/node/node-layout/project/project-list";
import JdkList from "@/pages/node/node-layout/project/jdk-list";
import Recover from "@/pages/node/node-layout/project/recover-list";
import Tomcat from "@/pages/node/node-layout/other/tomcat-list";
import ScriptTemplate from "@/pages/node/node-layout/other/script-list";
import ScriptLog from "@/pages/node/node-layout/other/script-log";
import NginxList from "@/pages/node/node-layout/nginx/list";
import Cert from "@/pages/node/node-layout/nginx/cert";
import WhiteList from "@/pages/node/node-layout/system/white-list.vue";
import Cache from "@/pages/node/node-layout/system/cache";
import Log from "@/pages/node/node-layout/system/log.vue";
import Upgrade from "@/pages/node/node-layout/system/upgrade.vue";
import ConfigFile from "@/pages/node/node-layout/system/config-file.vue";
import {mapGetters} from "vuex";

export default {
  components: {
    Welcome,
    ProjectList,
    JdkList,
    Recover,
    Tomcat,
    ScriptTemplate,
    NginxList,
    Cert,
    WhiteList,
    Cache,
    Log,
    Upgrade,
    ConfigFile,
    ScriptLog,
  },
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      nodeMenuList: [],
      selectedKeys: [this.$route.query.id || "welcome"],
      openKey: [],
    };
  },
  computed: {
    ...mapGetters(["getGuideCache"]),
    fullScreenFlag() {
      return this.getGuideCache.fullScreenFlag === undefined ? true : this.getGuideCache.fullScreenFlag;
    },
    currentId() {
      return this.selectedKeys[0];
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag;
    },
  },
  watch: {},
  created() {
    let keyList = [];
    if (this.$route.query.pId) {
      // 打开对应的父级菜单
      keyList = [this.$route.query.pId, "systemConfig"];
    }
    this.openKey = keyList;
    this.loadNodeMenu();
    setTimeout(() => {
      this.introGuide();
    }, 1000);
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "node-index",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".ant-drawer-title"),
              intro: "这里是这个节点的名称和节点地址",
            },
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-sider"),
              intro: "这里是这个节点的侧边栏菜单",
            },
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-content"),
              intro: "这里是这个节点的主要内容展示区",
            },
            {
              title: "导航助手",
              element: document.querySelector(".whitelistDirectory"),
              intro: "白名单目录是一个配置型菜单，里面配置的内容将会在</p><p><b>项目列表</b></br><b>Nginx 列表</b></br><b>证书管理</b></p> 【系统管理】->【白名单配置】。",
            },
          ],
        },
      });
    },
    // 加载菜单
    loadNodeMenu() {
      getNodeMenu(this.node.id).then((res) => {
        if (res.code === 200) {
          const data = res.data.map((item) => {
            const childs =
              item.childs &&
              item.childs.map((sub) => {
                return { ...sub, pId: item.id };
              });
            return { ...item, childs };
          });
          this.nodeMenuList = data;
        }
      });
    },
    // 点击菜单
    handleMenuClick(id, pId) {
      this.selectedKeys = [id];
      this.$router.push({
        query: {
          ...this.$route.query,
          pId: pId,
          id: id,
        },
      });
    },
    openChange(keys) {
      if (keys.length && !this.menuMultipleFlag) {
        // 保留一个打开
        keys = [keys[keys.length - 1]];
      }
      this.openKey = keys;
    },
  },
};
</script>
<style scoped lang="stylus">

.sider-scroll {
  min-height: calc(100vh -85px);
  overflow-y: auto;
}

.layout-content-scroll {
  min-height: calc(100vh - 85px)
  overflow-y: auto;
}

.sider-full-screen {
  height: calc(100vh - 75px);
  overflow-y: scroll;
}

.layout-content-full-screen {
 height: calc(100vh - 85px);
  overflow-y: scroll;
}
</style>
