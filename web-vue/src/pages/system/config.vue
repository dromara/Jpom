<template>
  <a-tabs default-active-key="1" @change="tabChange">
    <a-tab-pane key="1">
      <span slot="tab">
        <a-icon type="setting" />
        服务端系统配置
      </span>
      <a-alert v-if="temp.file" :message="`配置文件路径:${temp.file}`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-form-model ref="editForm" :model="temp">
        <a-form-model-item class="config-editor">
          <code-editor v-model="temp.content" :options="{ mode: 'yaml', tabSize: 2 }"></code-editor>
        </a-form-model-item>
        <a-form-model-item :wrapper-col="{ span: 14, offset: 2 }">
          <a-space>
            <a-button type="primary" class="btn" @click="onSubmit(false)">保存</a-button>
            <a-button type="primary" class="btn" @click="onSubmit(true)">保存并重启</a-button>
          </a-space>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <a-tab-pane key="2" class="ip-config-panel">
      <span slot="tab">
        <a-icon type="lock" />
        服务端IP白名单配置
      </span>
      <a-alert :message="`当前访问IP：${ipTemp.ip}`" type="success" />
      <a-alert
        message="请仔细确认后配置，ip配置后立即生效。配置时需要保证当前ip能访问！127.0.0.1 该IP不受访问限制.支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
        style="margin-top: 10px"
        banner
      />
      <a-alert message="如果配置错误需要重启服务端并添加命令行参数 --rest:ip_config 将恢复默认配置" style="margin-top: 10px" banner />
      <a-form-model style="margin-top: 10px" ref="editIpConfigForm" :model="temp" :label-col="{ span: 2 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item prop="content">
          <template slot="label">
            <a-space align="center">
              <a-tooltip>
                <template slot="title">禁止访问的 IP 地址 </template>
                <a-icon type="stop" theme="twoTone" />
                IP黑名单
              </a-tooltip>
            </a-space>
          </template>
          <a-input v-model="ipTemp.prohibited" type="textarea" :rows="8" class="ip-list-config" placeholder="请输入IP黑名单,多个使用换行,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24" />
        </a-form-model-item>
        <a-form-model-item prop="content">
          <template slot="label">
            <a-space align="center">
              <a-tooltip>
                <template slot="title"> 只允许访问的 IP 地址 </template>
                <a-icon type="check-circle" theme="twoTone" />
                IP白名单
              </a-tooltip>
            </a-space>
          </template>
          <a-input
            v-model="ipTemp.allowed"
            type="textarea"
            :rows="8"
            class="ip-list-config"
            placeholder="请输入IP白名单,多个使用换行,0.0.0.0 是开放所有IP,支持配置IP段 192.168.1.1/192.168.1.254,192.168.1.0/24"
          />
        </a-form-model-item>

        <a-form-model-item :wrapper-col="{ offset: 10 }" class="ip-config-button">
          <a-button type="primary" class="btn" @click="onSubmitIp()">保存</a-button>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <!-- 节点白名单分发 -->
    <a-tab-pane key="3">
      <span slot="tab">
        <a-icon type="folder" />
        节点白名单分发
      </span>
      <a-alert :message="`一键分发同步多个节点的白名单配置,不用挨个配置。配置后会覆盖之前的配置,一般用于节点环境一致的情况`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-form-model ref="editWhiteForm" :model="tempWhite" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item label="分发节点">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            placeholder="请选择分发到的节点"
            mode="multiple"
            v-model="tempWhite.chooseNode"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="项目路径" prop="project">
          <a-input v-model="tempWhite.project" type="textarea" :rows="5" style="resize: none" placeholder="请输入项目存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径" />
        </a-form-model-item>
        <a-form-model-item label="证书路径" prop="certificate">
          <a-input
            v-model="tempWhite.certificate"
            type="textarea"
            :rows="5"
            style="resize: none"
            placeholder="请输入证书存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"
          />
        </a-form-model-item>
        <a-form-model-item label="Nginx 路径" prop="nginx">
          <a-input
            v-model="tempWhite.nginx"
            type="textarea"
            :rows="5"
            style="resize: none"
            placeholder="请输入 nginx 存放路径白名单，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"
          />
        </a-form-model-item>
        <a-form-model-item label="远程下载安全HOST" prop="allowRemoteDownloadHost">
          <a-input
            v-model="tempWhite.allowRemoteDownloadHost"
            type="textarea"
            :rows="5"
            style="resize: none"
            placeholder="请输入远程下载安全HOST，回车支持输入多个路径，示例 https://www.test.com 等"
          />
        </a-form-model-item>
        <a-form-model-item label="文件后缀" prop="allowEditSuffix">
          <a-input
            v-model="tempWhite.allowEditSuffix"
            type="textarea"
            :rows="5"
            style="resize: none"
            placeholder="请输入允许编辑文件的后缀及文件编码，不设置编码则默认取系统编码，示例：设置编码：txt@utf-8， 不设置编码：txt"
          />
        </a-form-model-item>
        <a-form-model-item>
          <a-row type="flex" justify="center">
            <a-button type="primary" :disabled="this.nodeList.length <= 0" @click="onSubmitWhitelist">提交</a-button>
          </a-row>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <!-- 节点系统配置分发 -->
    <a-tab-pane key="4">
      <span slot="tab">
        <a-icon type="gateway" />
        节点系统配置分发
      </span>
      <a-alert :message="`一键分发同步多个节点的系统配置,不用挨个配置。配置后会覆盖之前的配置,一般用于节点环境一致的情况`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-form-model layout="inline" ref="editNodeConfigForm" :model="tempNodeConfig">
        <a-row type="flex" justify="center">
          <a-col :span="11">
            <a-row type="flex" justify="center">
              <a-form-model-item label="模版节点">
                <a-select
                  :getPopupContainer="
                    (triggerNode) => {
                      return triggerNode.parentNode || document.body;
                    }
                  "
                  style="width: 30vw"
                  show-search
                  @change="changeTemplateNode"
                  option-filter-prop="children"
                  placeholder="请选择模版节点"
                  v-model="tempNodeConfig.templateNodeId"
                >
                  <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-model-item>
            </a-row>
          </a-col>
          <a-col :span="2" style="text-align: center; line-height: 39.9999px">
            <a-icon type="arrow-right" />
          </a-col>
          <a-col :span="11">
            <a-row type="flex" justify="center">
              <a-form-model-item label="分发节点">
                <a-select
                  :getPopupContainer="
                    (triggerNode) => {
                      return triggerNode.parentNode || document.body;
                    }
                  "
                  style="width: 30vw"
                  show-search
                  option-filter-prop="children"
                  placeholder="请选择分发到的节点"
                  mode="multiple"
                  v-model="tempNodeConfig.chooseNode"
                >
                  <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
                    {{ item.name }}
                  </a-select-option>
                </a-select>
              </a-form-model-item>
            </a-row>
          </a-col>
        </a-row>
        <a-row type="flex" justify="center">
          <a-col :span="24">
            <a-form-model-item class="config-editor" :wrapper-col="{ span: 24 }">
              <code-editor v-model="tempNodeConfig.content" :options="{ mode: 'yaml', tabSize: 2 }"></code-editor>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row type="flex" justify="center" :offset="6">
          <a-form-model-item>
            <a-space>
              <a-button
                type="primary"
                :disabled="this.nodeList.length <= 0 || !tempNodeConfig.content || !tempNodeConfig.templateNodeId || tempNodeConfig.chooseNode.length <= 0"
                @click="onNodeSubmit(false)"
                >保存</a-button
              >
              <a-button
                type="primary"
                :disabled="this.nodeList.length <= 0 || !tempNodeConfig.content || !tempNodeConfig.templateNodeId || tempNodeConfig.chooseNode.length <= 0"
                @click="onNodeSubmit(true)"
                >保存并重启</a-button
              >
            </a-space>
          </a-form-model-item>
        </a-row>
      </a-form-model>
    </a-tab-pane>
    <!-- 菜单配置 -->
    <a-tab-pane key="5">
      <span slot="tab">
        <a-icon type="menu" />
        菜单配置
      </span>
      <a-alert :message="`菜单配置只对非超级管理员生效,当前配置对当前工作空间生效,其他工作空间请切换后配置`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-form-model ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-col :span="12">
            <a-card title="服务端菜单" :bordered="false">
              <a-tree show-icon v-if="menusConfigData.serverMenus" checkable :tree-data="menusConfigData.serverMenus" :replaceFields="replaceFields" v-model="menusConfigData.serverMenuKeys">
                <a-icon slot="switcherIcon" type="down" />

                <template slot="custom" slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="节点菜单" :bordered="false">
              <a-tree show-icon v-if="menusConfigData.nodeMenus" checkable :tree-data="menusConfigData.nodeMenus" :replaceFields="replaceFields" v-model="menusConfigData.nodeMenuKeys">
                <a-icon slot="switcherIcon" type="down" />

                <template slot="custom" slot-scope="{ dataRef }">
                  <a-icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
        </a-row>
        <a-form-model-item>
          <a-row type="flex" justify="center">
            <a-button type="primary" @click="onSubmitMenus">保存</a-button>
          </a-row>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
    <!-- 全局代理 -->
    <a-tab-pane key="6">
      <span slot="tab">
        <a-icon type="api" />
        全局代理
      </span>
      <a-alert :message="`全局代理配置后将对服务端的网络生效，代理实现方式：ProxySelector`" style="margin-top: 10px; margin-bottom: 20px" banner />
      <a-row justify="center" type="flex">
        <a-form-model layout="inline" ref="editProxyForm" :model="proxyConfigData">
          <a-row v-for="(item, index) in proxyConfigData.globalProxy" :key="index">
            <a-form-model-item label="通配符" prop="pattern">
              <a-input style="width: 30vw" :maxLength="200" v-model="item.pattern" placeholder="地址通配符,* 表示所有地址都将使用代理"> </a-input>
            </a-form-model-item>
            <a-form-model-item label="代理" prop="httpProxy">
              <a-input style="width: 30vw" v-model="item.proxyAddress" placeholder="代理地址 (127.0.0.1:8888)">
                <a-select slot="addonBefore" v-model="item.proxyType" style="width: 100px">
                  <a-select-option value="HTTP">HTTP</a-select-option>
                  <a-select-option value="SOCKS">SOCKS</a-select-option>
                  <a-select-option value="DIRECT">DIRECT</a-select-option>
                </a-select>
              </a-input>
            </a-form-model-item>
            <a-form-model-item>
              <a-button
                type="danger"
                @click="
                  () => {
                    proxyConfigData.globalProxy && proxyConfigData.globalProxy.splice(index, 1);
                  }
                "
                size="small"
                :disabled="proxyConfigData.globalProxy && proxyConfigData.globalProxy.length <= 1"
              >
                删除
              </a-button>
            </a-form-model-item>
          </a-row>
          <a-row type="flex" justify="center">
            <a-form-model-item>
              <a-space>
                <a-button
                  type="primary"
                  @click="
                    () => {
                      proxyConfigData = {
                        ...proxyConfigData,
                        globalProxy: [
                          ...proxyConfigData.globalProxy,
                          {
                            proxyType: 'HTTP',
                          },
                        ],
                      };
                    }
                  "
                  >添加</a-button
                >
                <a-button type="primary" @click="saveProxyConfigHannder">保存</a-button>
              </a-space>
            </a-form-model-item>
          </a-row>
        </a-form-model>
      </a-row>
    </a-tab-pane>
  </a-tabs>
</template>
<script>
import {
  editConfig,
  editIpConfig,
  getConfigData,
  getIpConfigData,
  getMenusConfig,
  getNodeConfig,
  getProxyConfig,
  getWhitelist,
  saveMenusConfig,
  saveNodeConfig,
  saveProxyConfig,
  saveWhitelist,
  systemInfo,
} from "@/api/system";
import codeEditor from "@/components/codeEditor";
import {RESTART_UPGRADE_WAIT_TIME_COUNT} from "@/utils/const";
import Vue from "vue";
import {getNodeListAll} from "@/api/node";

export default {
  components: {
    codeEditor,
  },
  data() {
    return {
      temp: {
        content: "",
      },
      ipTemp: {
        allowed: "",
        prohibited: "",
      },
      tempWhite: {},
      tempNodeConfig: {},
      nodeList: [],
      checkCount: 0,
      menusConfigData: {},
      replaceFields: { children: "childs", title: "title", key: "id" },
      proxyConfigData: {
        globalProxy: [
          {
            proxyType: "HTTP",
          },
        ],
      },
    };
  },
  mounted() {
    this.tabChange("1");
  },
  methods: {
    // load data
    loadConfitData() {
      getConfigData().then((res) => {
        if (res.code === 200) {
          this.temp.content = res.data.content;
          this.temp.file = res.data.file;
        }
      });
    },
    // 加载 ip 白名单配置
    loadIpConfigData() {
      getIpConfigData().then((res) => {
        if (res.code === 200) {
          if (res.data) {
            this.ipTemp = res.data;
          }
        }
      });
    },
    // 加载节点白名单分发配置
    loadWhitelistData() {
      getWhitelist().then((res) => {
        if (res.code === 200) {
          this.tempWhite = res.data;
          this.tempWhite = { ...this.tempWhite, chooseNode: res.data.nodeIds ? res.data.nodeIds.split(",") : [] };
        }
      });
    },
    // 加载节点系统配置分发
    loadNodeConfig() {
      getNodeConfig().then((res) => {
        this.tempNodeConfig = { ...this.tempNodeConfig, chooseNode: res.data.nodeIds ? res.data.nodeIds.split(",") : [], templateNodeId: res.data.templateNodeId };
        if (this.tempNodeConfig.templateNodeId) {
          this.changeTemplateNode(this.tempNodeConfig.templateNodeId);
        }
      });
    },
    // 加载菜单配置信息
    loadMenusConfig() {
      getMenusConfig().then((res) => {
        if (res.code !== 200) {
          return;
        }
        this.menusConfigData = res.data;

        this.menusConfigData.serverMenus = this.menusConfigData?.serverMenus.map((item) => {
          item.scopedSlots = { icon: "custom" };
          item.childs?.map((item2) => {
            item2.id = item.id + ":" + item2.id;
            return item2;
          });
          return item;
        });
        this.menusConfigData.nodeMenus = this.menusConfigData?.nodeMenus.map((item) => {
          item.scopedSlots = { icon: "custom" };
          item.childs?.map((item2) => {
            item2.id = item.id + ":" + item2.id;
            return item2;
          });
          return item;
        });
        if (!this.menusConfigData?.serverMenuKeys) {
          //
          const serverMenuKeys = [];
          this.menusConfigData.serverMenus.forEach((item) => {
            serverMenuKeys.push(item.id);
            if (item.childs) {
              item.childs.forEach((item2) => {
                serverMenuKeys.push(item2.id);
              });
            }
          });
          this.menusConfigData = { ...this.menusConfigData, serverMenuKeys: serverMenuKeys };
        }

        if (!this.menusConfigData?.nodeMenuKeys) {
          //
          const nodeMenuKeys = [];
          this.menusConfigData.nodeMenus.forEach((item) => {
            nodeMenuKeys.push(item.id);
            if (item.childs) {
              item.childs.forEach((item2) => {
                nodeMenuKeys.push(item2.id);
              });
            }
          });
          this.menusConfigData = { ...this.menusConfigData, nodeMenuKeys: nodeMenuKeys };
        }
      });
    },
    // submit
    onSubmit(restart) {
      this.$confirm({
        title: "系统提示",
        content: "真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.temp.restart = restart;
          editConfig(this.temp).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
              if (this.temp.restart) {
                this.startCheckRestartStatus(res.msg);
              }
              //
            }
          });
        },
      });
    },
    startCheckRestartStatus(msg) {
      this.checkCount = 0;
      Vue.prototype.$setLoading({
        spinning: true,
        tip: (msg || "重启中，请稍候...") + ",请耐心等待暂时不用刷新页面,重启成功后会自动刷新",
      });
      setTimeout(() => {
        //
        this.timer = setInterval(() => {
          systemInfo()
            .then((res) => {
              if (res.code === 200) {
                clearInterval(this.timer);
                Vue.prototype.$setLoading(false);
                this.$notification.success({
                  message: "重启成功",
                });

                setTimeout(() => {
                  location.reload();
                }, 1000);
              } else {
                if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                  this.$notification.warning({
                    message: "未重启成功：" + (res.msg || ""),
                  });
                  Vue.prototype.$setLoading(false);
                  clearInterval(this.timer);
                }
              }
            })
            .catch((error) => {
              console.error(error);
              if (this.checkCount > RESTART_UPGRADE_WAIT_TIME_COUNT) {
                Vue.prototype.$setLoading(false);
                this.$notification.error({
                  message: "重启超时,请去服务器查看控制台日志排查问题",
                });
                clearInterval(this.timer);
              }
            });
          this.checkCount = this.checkCount + 1;
        }, 2000);
      }, 6000);
    },
    // submit ip config
    onSubmitIp() {
      this.$confirm({
        title: "系统提示",
        content: "真的要保存当前配置吗？IP 白名单请慎重配置奥( 白名单是指只允许访问的 IP ),配置后立马生效 如果配置错误将出现无法访问的情况,需要手动恢复奥！！！",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          editIpConfig(this.ipTemp).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
            }
          });
        },
      });
    },
    onSubmitWhitelist() {
      this.tempWhite.nodeIds = this.tempWhite?.chooseNode?.join(",");
      saveWhitelist(this.tempWhite).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 获取所有节点
    getAllNodeList() {
      getNodeListAll().then((res) => {
        this.nodeList = res.data || [];
      });
    },
    // 修改模版节点
    changeTemplateNode(nodeId) {
      getConfigData(nodeId).then((res) => {
        if (res.code === 200) {
          this.tempNodeConfig = { ...this.tempNodeConfig, content: res.data.content };
        }
      });
    },
    // submit
    onNodeSubmit(restart) {
      this.$confirm({
        title: "系统提示",
        content: restart
          ? "真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！ 保存成功后请及时关注重启状态！！"
          : "真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.tempNodeConfig.restart = restart;
          this.tempNodeConfig.nodeIds = this.tempNodeConfig?.chooseNode?.join(",");
          saveNodeConfig(this.tempNodeConfig).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
            }
          });
        },
      });
    },
    onSubmitMenus() {
      saveMenusConfig({
        serverMenuKeys: this.menusConfigData.serverMenuKeys.join(","),
        nodeMenuKeys: this.menusConfigData.nodeMenuKeys.join(","),
      }).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 加载
    loadProxyConfig() {
      getProxyConfig().then((res) => {
        if (res.data && res.data.length) {
          this.proxyConfigData = { globalProxy: res.data };
        }
      });
    },
    // 保存
    saveProxyConfigHannder() {
      saveProxyConfig(this.proxyConfigData.globalProxy).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 切换
    tabChange(activeKey) {
      if (activeKey === "1") {
        this.loadConfitData();
      } else if (activeKey === "2") {
        this.loadIpConfigData();
      } else if (activeKey === "3") {
        this.getAllNodeList();
        this.loadWhitelistData();
      } else if (activeKey === "4") {
        this.loadNodeConfig();
        this.getAllNodeList();
      } else if (activeKey === "5") {
        this.loadMenusConfig();
      } else if (activeKey === "6") {
        //
        this.loadProxyConfig();
      }
    },
  },
};
</script>
<style scoped>
textarea {
  resize: none;
}
.config-editor {
  height: calc(100vh - 300px);
  width: 100%;
  overflow-y: scroll;
  border: 1px solid #d9d9d9;
}
</style>
