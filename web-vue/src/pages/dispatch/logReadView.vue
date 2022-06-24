<template>
  <div>
    <!-- 布局 -->
    <a-layout class="file-layout node-full-content">
      <!-- 目录树 -->
      <a-layout-sider theme="light" class="sider" width="25%">
        <div class="dir-container">
          <template v-if="temp.projectList && temp.cacheData">
            <div>
              节点：
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                :value="`${temp.cacheData.useNodeId},${temp.cacheData.useProjectId}`"
                style="width: 200px"
                @change="nodeChange"
                placeholder="请选择节点"
              >
                <a-select-option v-for="item in temp.projectList" :key="`${item.nodeId},${item.projectId}`">
                  {{ nodeName[item.nodeId] && nodeName[item.nodeId].name }}
                </a-select-option>
              </a-select>
            </div>
          </template>
          <!-- <a-button size="small" type="primary" @click="loadFileData">刷新目录</a-button> -->
        </div>

        <a-directory-tree :replace-fields="treeReplaceFields" @select="nodeClick" :loadData="onTreeData" :treeData="treeList"></a-directory-tree>
      </a-layout-sider>
      <!-- 表格 -->
      <a-layout-content class="file-content">
        <div class="log-filter" v-if="temp.cacheData">
          <a-space direction="vertical">
            <!-- direction="vertical" -->
            <a-space>
              <div>
                <!-- 关键词： -->
                <!-- ^.*\d+.*$ -->
                <!-- .*(0999996|0999995).*   .*(a|b).* -->
                <a-tooltip placement="right" title="关键词高亮,支持正则(正则可能影响性能请酌情使用)">
                  <a-input placeholder="关键词,支持正则" :style="`width: 250px`" v-model="temp.cacheData.keyword" @pressEnter="sendSearchLog"> </a-input>
                </a-tooltip>
              </div>
              <div>
                显示前N行
                <a-input-number id="inputNumber" v-model="temp.cacheData.beforeCount" :min="0" :max="1000" @pressEnter="sendSearchLog" />
              </div>
              <div>
                显示后N行
                <a-input-number id="inputNumber" v-model="temp.cacheData.afterCount" :min="0" :max="1000" @pressEnter="sendSearchLog" />
              </div>
              <a-popover title="正则语法参考">
                <template slot="content">
                  <ul>
                    <li><b>^.*\d+.*$</b> - 匹配包含数字的行</li>
                    <li><b>.*(a|b).*</b> - 匹配包含 a 或者 b 的行</li>
                    <li><b>.*(异常).*</b> - 匹配包含 异常 的行</li>
                  </ul>
                </template>
                <a-button type="link" style="padding: 0" icon="unordered-list"><span style="margin-left: 2px">语法参考</span></a-button>
              </a-popover>
            </a-space>
            <a-space>
              <div>
                <!-- 搜索模式 -->
                <a-tooltip placement="right" title="搜索模式,默认查看文件最后多少行，从头搜索指从指定行往下搜索，从尾搜索指从文件尾往上搜索多少行">
                  <a-select
                    :style="`width: 250px`"
                    :value="temp.cacheData.first"
                    @change="
                      (value) => {
                        const cacheData = { ...temp.cacheData, first: value };
                        temp = { ...temp, cacheData: cacheData };
                        sendSearchLog();
                      }
                    "
                  >
                    <a-select-option value="false">从尾搜索</a-select-option>
                    <a-select-option value="true">从头搜索 </a-select-option>
                  </a-select>
                </a-tooltip>
              </div>
              <div>
                文件前N行
                <a-input-number id="inputNumber" v-model="temp.cacheData.head" :min="0" :max="1000" @pressEnter="sendSearchLog" />
              </div>
              <div>
                文件后N行
                <a-input-number id="inputNumber" v-model="temp.cacheData.tail" :min="0" :max="1000" @pressEnter="sendSearchLog" />
              </div>
              <a-popover title="搜索配置参考">
                <template slot="content">
                  <ul>
                    <li><b>从尾搜索、文件前0行、文件后3行</b> - 在文件最后 3 行中搜索</li>
                    <li><b>从头搜索、文件前0行、文件后3行</b> - 在文件第 3 - 2147483647 行中搜索</li>
                    <li><b>从尾搜索、文件前2行、文件后3行</b> - 在文件第 1 - 2 行中搜索</li>
                    <li><b>从尾搜索、文件前100行、文件后100行</b> - 在文件第 1 - 100 行中搜索</li>
                    <li><b>从头搜索、文件前2行、文件后3行</b> - 在文件第 2 - 2 行中搜索</li>
                    <li><b>从尾搜索、文件前20行、文件后3行</b> - 在文件第 17 - 20 行中搜索</li>
                    <li><b>从头搜索、文件前20行、文件后3行</b> - 在文件第 3 - 20 行中搜索</li>
                  </ul>
                </template>
                <a-button type="link" style="padding: 0" icon="unordered-list"><span style="margin-left: 2px">搜索参考</span></a-button>
              </a-popover>
            </a-space>
          </a-space>
        </div>

        <a-tabs v-if="temp.cacheData" v-model="activeTagKey" :tabBarStyle="{ marginBottom: 0 }">
          <template v-for="item in temp.projectList">
            <a-tab-pane forceRender v-if="nodeName[item.nodeId]" :key="`${item.nodeId},${item.projectId}`" :tab="nodeName[item.nodeId] && nodeName[item.nodeId].name">
              <viewPre
                :searchReg="searchReg"
                :ref="`pre-dom-${item.nodeId},${item.projectId}`"
                :id="`pre-dom-${item.nodeId},${item.projectId}`"
                height="calc(100vh - 80px - 85px - 43px)"
                :config="{
                  logScroll: true,
                  logShowLine: 5000,
                  searchValue: '',
                }"
              ></viewPre>
            </a-tab-pane>
          </template>
        </a-tabs>
      </a-layout-content>
    </a-layout>
  </div>
</template>
<script>
import {getNodeListAll, getProjectListAll} from "@/api/node";
import {itemGroupBy} from "@/utils/time";
import {getFileList} from "@/api/node-project";
import {getWebSocketUrl} from "@/utils/const";
import {mapGetters} from "vuex";
import viewPre from "@/components/logView/view-pre";
import {updateCache} from "@/api/log-read";

export default {
  components: {
    viewPre,
  },
  props: {
    data: Object,
  },
  data() {
    return {
      treeReplaceFields: {
        title: "filename",
        isLeaf: "isDirectory",
      },
      tempNode: {},
      tempFileNode: {},
      treeList: [],
      activeTagKey: "",

      nodeProjectList: [],
      nodeList: [],
      nodeName: {},
      temp: {},
      socketCache: {},
    };
  },
  computed: {
    ...mapGetters(["getLongTermToken"]),
    selectPath() {
      if (!Object.keys(this.tempNode).length) {
        return "";
      }
      if (this.tempNode.level === 1) {
        return "";
      } else {
        return (this.tempNode.levelName || "") + "/" + this.tempNode.filename;
      }
    },
    selectFilePath() {
      if (!Object.keys(this.tempFileNode).length) {
        return "";
      }
      if (this.tempFileNode.level === 1) {
        return "";
      } else {
        return (this.tempFileNode.levelName || "") + "/" + this.tempFileNode.filename;
      }
    },
    searchReg() {
      try {
        return this.temp?.cacheData?.keyword ? new RegExp("(" + this.temp.cacheData.keyword + ")", "ig") : null;
      } catch (e) {
        // console.error(e);
        return null;
      }
    },
  },
  created() {
    this.temp = { ...this.data };

    const cacheData = this.temp.cacheData;
    cacheData.useProjectId = this.temp.projectList[0].projectId;
    cacheData.useNodeId = this.temp.projectList[0].nodeId;
    cacheData.beforeCount = cacheData.beforeCount || 0;
    cacheData.afterCount = cacheData.afterCount || 10;
    cacheData.head = cacheData.head || 0;
    cacheData.tail = cacheData.tail || 100;
    cacheData.first = cacheData.first === undefined ? "false" : cacheData.first + "";
    this.temp = { ...this.temp, cacheData: cacheData };
    this.loadNodeList().then(() => {
      this.loadFileData();
      //   console.log(this.nodeProjectList);
      this.temp.projectList.forEach((item) => {
        const itemProjectData = this.nodeProjectList[item.nodeId].projects.filter((projectData) => {
          return item.projectId === projectData.projectId;
        })[0];
        const socketUrl = getWebSocketUrl("/socket/console", `userId=${this.getLongTermToken}&id=${itemProjectData.id}&nodeId=${item.nodeId}&type=console&copyId=`);
        const domId = `pre-dom-${item.nodeId},${item.projectId}`;
        this.socketCache = { ...this.socketCache, [domId]: {} };
        const socket = this.initWebSocket(domId, socketUrl);

        this.socketCache = { ...this.socketCache, [domId]: { socket: socket, projectId: item.projectId, nodeId: item.nodeId } };

        // 连接成功后
        socket.onopen = () => {
          if (cacheData.logFile) {
            // 之前已经打开的
            this.sendMsg(domId, "showlog", this.temp.cacheData);
          }
        };
      });
      //
    });
    this.activeTagKey = this.temp.cacheData.useNodeId + "," + this.temp.cacheData.useProjectId;
    // console.log(cacheData);
  },
  methods: {
    initWebSocket(id, url) {
      let socket;
      if (!socket || socket.readyState !== socket.OPEN || socket.readyState !== socket.CONNECTING) {
        socket = new WebSocket(url);
      }

      socket.onerror = (err) => {
        console.error(err);
        this.$notification.error({
          key: "log-read-error",
          message: "web socket 错误,请检查是否开启 ws 代理",
        });
        clearInterval(this.socketCache[id]);
      };
      socket.onclose = (err) => {
        //当客户端收到服务端发送的关闭连接请求时，触发onclose事件
        console.error(err);
        this.$notification.info({
          key: "log-read-close",
          message: "会话已经关闭",
        });
        clearInterval(this.socketCache[id]);
      };
      socket.onmessage = (msg) => {
        // console.log(msg);
        this.$refs[id][0].appendLine(msg.data);

        clearInterval(this.socketCache[id].heart);
        // 创建心跳，防止掉线
        this.socketCache[id].heart = setInterval(() => {
          this.sendMsg(id, "heart");
          // this.loadFileSize();
        }, 5000);
      };

      return socket;
    },
    // 发送消息
    sendMsg(id, op, other) {
      const cacheData = this.socketCache[id];
      //   console.log(cacheData, id);
      const data = {
        op: op,
        projectId: cacheData.projectId,
        search: true,
      };
      cacheData.socket.send(JSON.stringify(Object.assign({}, data, other)));
    },
    // appendLine(id, data) {},
    // 加载节点以及项目
    loadNodeList() {
      return new Promise((resolve) => {
        this.loadNodeList2().then(() => {
          this.getProjectListAll().then(() => {
            resolve();
          });
        });
      });
    },
    // 加载节点以及项目
    loadNodeList2() {
      return new Promise((resolve) => {
        getNodeListAll().then((res) => {
          if (res.code === 200) {
            this.nodeList = res.data;
            this.nodeName = res.data.groupBy((item) => item.id);
            resolve();
          }
        });
      });
    },
    // 加载用户列表
    getProjectListAll() {
      return new Promise((resolve) => {
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            this.nodeProjectList = itemGroupBy(res.data, "nodeId", "id", "projects").groupBy((item) => item.id);
            resolve();
            // console.log(this.nodeList);
            // console.log(this.nodeProjectList);
          }
        });
      });
    },
    nodeChange(value) {
      const keyArray = value.split(",");

      const cacheData = { ...this.temp.cacheData, useNodeId: keyArray[0], useProjectId: keyArray[1] };
      this.temp = { ...this.temp, cacheData: cacheData };
      this.loadFileData();
      //
      this.activeTagKey = value;
    },
    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        this.tempNode = node.dataRef;
        //this.loadFileList();
      } else {
        this.tempFileNode = node.dataRef;
        // let cacheData = ;
        const cacheData = { ...this.temp.cacheData, logFile: this.selectFilePath };
        this.temp = { ...this.temp, cacheData: cacheData };
        this.$emit("changeTitle", this.selectFilePath);
        //
        this.sendSearchLog();
      }
    },
    onTreeData(treeNode) {
      return new Promise((resolve) => {
        if (treeNode.dataRef.children || !treeNode.dataRef.isDirectory) {
          resolve();
          return;
        }
        this.loadNode(treeNode.dataRef, resolve);
      });
    },
    // 加载数据
    loadFileData() {
      const key = "root-" + new Date().getTime();
      const temp = this.temp;
      const projectName = this.nodeProjectList[temp.cacheData.useNodeId].projects.filter((item) => item.projectId === temp.cacheData.useProjectId)[0].name;
      this.treeList = [
        {
          filename: projectName,
          level: 1,
          isDirectory: true,
          key: key,
          isLeaf: false,
        },
      ];

      // 设置默认展开第一个
      setTimeout(() => {
        const node = this.treeList[0];
        this.tempNode = node;
        this.expandKeys = [key];
        //this.loadFileList();
      }, 1000);
    },
    // 加载子节点
    loadNode(data, resolve) {
      this.tempNode = data;
      // 如果是目录
      if (data.isDirectory) {
        setTimeout(() => {
          // 请求参数
          const cacheData = this.temp.cacheData;

          const params = {
            nodeId: cacheData.useNodeId,
            id: cacheData.useProjectId,
            path: this.selectPath,
          };

          // 加载文件
          getFileList(params).then((res) => {
            if (res.code === 200) {
              const treeData = res.data.map((ele) => {
                ele.isLeaf = !ele.isDirectory;
                ele.key = ele.filename + "-" + new Date().getTime();
                return ele;
              });
              data.children = treeData;

              this.treeList = [...this.treeList];
              resolve();
            } else {
              resolve();
            }
          });
        }, 500);
      } else {
        resolve();
      }
    },
    sendSearchLog() {
      if (this.temp?.cacheData?.logFile) {
        Object.keys(this.socketCache).forEach((item) => {
          this.$refs[item][0].clearLogCache();
          this.sendMsg(item, "showlog", this.temp.cacheData);
        });
        //
        updateCache(Object.assign({}, this.temp.cacheData, { id: this.temp.id })).then();
      }
      //
    },
  },
};
</script>

<style scoped>
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 80px);
  overflow-y: auto;
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.file-content {
  height: calc(100vh - 80px);
  overflow-y: auto;
  /* margin: 10px 10px 0; */
  padding: 0 10px;
  background-color: #fff;
}
.log-filter {
  /* margin-top: -22px; */
  /* margin-bottom: 10px; */
  padding: 0 10px;
  padding-top: 0;
  padding-bottom: 10px;
  line-height: 0;
  border-bottom: 1px solid #eee;
}
</style>
