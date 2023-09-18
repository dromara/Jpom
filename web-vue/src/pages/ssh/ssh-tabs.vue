<template>
  <a-layout style="padding: 5px 0; background: #fff">
    <a-layout-sider width="200" :style="{ background: '#fff', height: `calc(100vh - 10px)`, borderRight: '1px solid #e8e8e8', overflowX: 'scroll' }">
      <a-directory-tree v-if="treeList.length" multiple default-expand-all :treeData="treeList" :replaceFields="replaceFields" @select="select"> </a-directory-tree>
      <a-empty v-else></a-empty>
    </a-layout-sider>
    <a-layout-content :style="{ padding: '0 5px', height: `calc(100vh - 10px)` }">
      <a-tabs v-if="selectPanes.length" v-model="activeKey" type="editable-card" hide-add @edit="onEdit" @change="change">
        <template slot="tabBarExtraContent">
          <a-button type="primary" :disabled="!activeKey" @click="changeFileVisible(activeKey, true)"> 文件管理 </a-button>
        </template>
        <a-tab-pane v-for="pane in selectPanes" :key="pane.id" :tab="pane.name" :closable="true" :ref="`pene-${pane.id}`">
          <div :id="`paneDom${pane.id}`">
            <div v-if="pane.open" :style="{ height: `calc(100vh - 70px) ` }">
              <terminal :sshId="pane.id" />
            </div>
            <a-result v-else status="warning" title="未开启当前终端">
              <template #extra>
                <a-button type="primary" @click="open(pane.id)"> 打开终端 </a-button>
              </template>
            </a-result>
            <!-- 文件管理 -->
            <a-drawer
              v-if="pane.openFile"
              :getContainer="`#paneDom${pane.id}`"
              :title="`${pane.name}文件管理`"
              placement="right"
              width="90vw"
              :visible="pane.fileVisible"
              @close="changeFileVisible(pane.id, false)"
            >
              <ssh-file v-if="pane.openFile" :sshId="pane.id" />
            </a-drawer>
          </div>
        </a-tab-pane>
      </a-tabs>
      <a-empty v-else description="未选择ssh"></a-empty>
    </a-layout-content>
  </a-layout>
</template>
<script>
import { mapGetters } from "vuex";
import { getSshListTree } from "@/api/ssh";
import terminal from "./terminal";
import SshFile from "@/pages/ssh/ssh-file";

export default {
  components: {
    terminal,
    SshFile,
  },
  data() {
    return {
      activeKey: "",
      selectPanes: [],
      treeList: [],
      replaceFields: {
        children: "children",
        title: "name",
        key: "id",
      },
    };
  },
  computed: {
    ...mapGetters(["getWorkspaceId"]),
  },
  created() {
    this.listData();
  },
  methods: {
    findItemById(list, id) {
      // 每次进来使用find遍历一次
      let res = list.find((item) => item.id == id);

      if (res) {
        return res;
      } else {
        for (let i = 0; i < list.length; i++) {
          if (list[i].children instanceof Array && list[i].children.length > 0) {
            res = this.findItemById(list[i].children, id);

            if (res) return res;
          }
        }
        return null;
      }
    },
    // 查询树
    listData() {
      getSshListTree().then((res) => {
        if (res.code == 200 && res.data) {
          this.treeList = res.data.children || [];
          try {
            const cache = JSON.parse(localStorage.getItem("ssh-tabs-cache") || "{}");
            const cacheIds = (cache.selectPanes || []).map((item) => item.id);
            this.selectPanes =
              cacheIds
                .map((item) => {
                  return this.findItemById(this.treeList, item);
                })
                .filter((item) => item)
                .map((item) => {
                  // 默认关闭
                  item.open = false;
                  return item;
                }) || [];

            const activeKey = this.selectPanes.find((item) => item.id === cache.activeKey);
            if (activeKey) {
              this.activeKey = activeKey.id;
            } else if (this.selectPanes.length) {
              this.activeKey = this.selectPanes[0].id;
            }
          } catch (e) {
            console.error(e);
          }
        }
      });
    },
    // 编辑 tabs
    onEdit(targetKey, action) {
      if (action === "remove") {
        this.selectPanes = this.selectPanes.filter((pane) => pane.id !== targetKey);
        if (this.activeKey === targetKey) {
          this.activeKey = this.selectPanes[0] && this.selectPanes[0].id;
        }
        this.cache();
      }
    },
    // 切换
    change() {
      this.cache();
    },
    open(activeKey) {
      this.selectPanes = this.selectPanes.map((item) => {
        if (item.id === activeKey) {
          item.open = true;
        }
        return item;
      });
    },
    select(selectedKeys, { node }) {
      if (!node.dataRef.isLeaf) {
        return;
      }
      const findPane = this.selectPanes.find((item) => item.id === node.dataRef.id);
      if (findPane) {
        this.activeKey = findPane.id;
      } else {
        const data = { ...node.dataRef, open: true };
        this.selectPanes.push(data);
        this.activeKey = node.dataRef.id;
      }
      this.cache();
    },
    cache() {
      localStorage.setItem(
        "ssh-tabs-cache",
        JSON.stringify({
          activeKey: this.activeKey,
          selectPanes: this.selectPanes,
        })
      );
    },
    // 文件管理状态切换
    changeFileVisible(activeKey, value) {
      this.selectPanes = this.selectPanes.map((item) => {
        if (item.id === activeKey) {
          item.fileVisible = value;
          if (value && !item.openFile) {
            item.openFile = true;
          }
        }
        return item;
      });
    },
  },
};
</script>

<style></style>
