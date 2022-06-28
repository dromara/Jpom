<template>
  <div class="full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" @change="changePage" :pagination="listQuery.total / listQuery.limit > 1 ? pagination : false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
          <a-input v-model="listQuery['%host%']" @pressEnter="loadData" placeholder="host" class="search-input-item" />
          <a-input v-model="listQuery['%swarmId%']" @pressEnter="loadData" placeholder="集群ID" class="search-input-item" />

          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">添加</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <!-- <template slot="certExist" slot-scope="text, record">
        <template v-if="record.tlsVerify">
          <a-tag v-if="record.certExist" color="green"> 存在 </a-tag>
          <a-tag v-else color="red"> 不存在 </a-tag>
        </template>
        <span v-else>-</span>
      </template> -->

      <a-tooltip slot="tlsVerify" slot-scope="text, record" placement="topLeft" :title="record.tlsVerify ? '开启 TLS 认证' : '关闭 TLS 认证'">
        <template v-if="record.tlsVerify">
          <template v-if="record.certExist"> <a-switch size="small" v-model="record.tlsVerify" :disabled="true" checked-children="开" un-checked-children="关" /> </template>
          <a-tag v-else color="red"> 证书丢失 </a-tag>
        </template>
        <template v-else> <a-switch size="small" v-model="record.tlsVerify" :disabled="true" checked-children="开" un-checked-children="关" /> </template>
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <a-tooltip :title="`${parseInt(record.status) === 1 ? '运行中' : record.failureMsg || ''}`">
          <a-switch size="small" :checked="parseInt(record.status) === 1" :disabled="true">
            <a-icon slot="checkedChildren" type="check-circle" />
            <a-icon slot="unCheckedChildren" type="warning" />
          </a-switch>
        </a-tooltip>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" :disabled="parseInt(record.status) !== 1" @click="handleConsole(record)">控制台</a-button>
          <template v-if="!record.swarmId && parseInt(record.status) === 1">
            <a-popover title="集群操作">
              <template slot="content">
                <p><a-button size="small" type="primary" @click="initSwarm(record)">创建集群</a-button></p>
                <p><a-button size="small" type="primary" @click="joinSwarm(record)">加入集群</a-button></p>
              </template>
              <a-button size="small" type="primary">集群</a-button>
            </a-popover>
          </template>
          <template v-else>
            <a-tooltip>
              <a-tooltip :title="`${parseInt(record.status) !== 1 ? '已经离线' : '已经在集群啦'}`">
                <a-button size="small" disabled type="primary">集群</a-button>
              </a-tooltip>
            </a-tooltip>
          </template>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleLeaveForce(record)">强制退出集群</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑  Docker" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <!-- <a-form-model-item v-if="temp.id" label="容器ID" prop="id">
          <a-input v-model="temp.id" disabled readOnly />
        </a-form-model-item> -->
        <a-space direction="vertical">
          <a-alert banner>
            <template slot="message">
              系统使用 docker http 接口实现和 docker 通讯和管理，但是默认<b style="color: red">没有开启任何认证</b> 这样使得 <b style="color: red">docker 极不安全</b>，如果端口暴露到公网很<b
                style="color: red"
              >
                容易出现挖矿情况
              </b>
              所以这里 我们<b style="color: red">强烈建议您使用 TLS 证书</b>（证书生成方式可以参考文档）来连接 docker 提升安全性（如果端口<b style="color: red">保证在内网中使用可以忽略 TLS 证书</b>）
            </template>
          </a-alert>
          <div></div>
        </a-space>
        <a-form-model-item label="容器名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>
        <a-form-model-item label="host" prop="host">
          <a-input v-model="temp.host" placeholder="容器地址 tcp://127.0.0.1:2375" />
        </a-form-model-item>
        <!-- <a-form-model-item label="接口版本" prop="apiVersion">
          <a-select
          :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            " show-search option-filter-prop="children" v-model="temp.apiVersion" allowClear placeholder="接口版本">
            <a-select-option v-for="item in apiVersions" :key="item.version">{{ item.webVersion }}</a-select-option>
          </a-select>
        </a-form-model-item> -->
        <a-form-model-item label="TLS 认证" prop="tlsVerify">
          <a-row>
            <a-col :span="5">
              <a-switch v-model="temp.tlsVerify" checked-children="开" un-checked-children="关" />
            </a-col>
            <a-col :span="16" v-if="temp.tlsVerify">
              证书文件:
              <a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" :accept="'.zip'">
                <a-button><a-icon type="upload" />选择文件</a-button>
              </a-upload>
            </a-col>
          </a-row>
        </a-form-model-item>

        <a-form-model-item label="心跳超时" prop="heartbeatTimeout">
          <a-input-number style="width: 100%" v-model="temp.heartbeatTimeout" placeholder="心跳超时 单位秒" />
        </a-form-model-item>
        <a-form-model-item label="标签" prop="tagInput">
          <template>
            <div>
              <a-tooltip :key="index" :title="tag" v-for="(tag, index) in temp.tagsArray">
                <a-tag
                  :key="tag"
                  :closable="true"
                  @close="
                    () => {
                      temp.tagsArray = temp.tagsArray.filter((removedTag) => tag !== removedTag);
                    }
                  "
                >
                  {{ `${tag}` }}
                </a-tag>
              </a-tooltip>
            </div>
          </template>
          <a-input
            v-if="temp.inputVisible"
            ref="tagInput"
            type="text"
            size="small"
            placeholder="请输入标签名 字母数字 长度 1-10"
            v-model="temp.tagInput"
            @blur="handleInputConfirm"
            @keyup.enter="handleInputConfirm"
          />
          <template v-else>
            <a-tag v-if="!temp.tagsArray || temp.tagsArray.length < 10" style="background: #fff; borderstyle: dashed" @click="showInput"> <a-icon type="plus" /> 添加 </a-tag>
          </template>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 创建集群 -->
    <a-modal v-model="initSwarmVisible" title="创建 Docker 集群" @ok="handleSwarm" :maskClosable="false">
      <a-form-model ref="initForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="集群名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>

        <a-form-model-item label="标签" prop="tag"><a-input v-model="temp.tag" placeholder="关联容器标签" /> </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 加入集群 -->
    <a-modal v-model="joinSwarmVisible" title="加入 Docker 集群" @ok="handleSwarmJoin" :maskClosable="false">
      <a-form-model ref="joinForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="选择集群" prop="swarmId">
          <a-select
            show-search
            option-filter-prop="children"
            @change="
              (v) => {
                tempList = swarmList.filter((item) => {
                  return item.id === v;
                });
                if (tempList.length) {
                  temp = { ...temp, remoteAddr: tempList[0].nodeAddr };
                } else {
                  temp = { ...temp, remoteAddr: '' };
                }
              }
            "
            v-model="temp.swarmId"
            allowClear
            placeholder="加入到哪个集群"
          >
            <a-select-option v-for="item in swarmList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item v-if="temp.remoteAddr" label="集群IP" prop="remoteAddr"><a-input v-model="temp.remoteAddr" placeholder="关联容器标签" /> </a-form-model-item>

        <a-form-model-item label="角色" prop="role">
          <a-radio-group name="role" v-model="temp.role">
            <a-radio value="worker"> 工作节点</a-radio>
            <a-radio value="manager"> 管理节点 </a-radio>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 控制台 -->
    <a-drawer
      :title="`${temp.name} 控制台`"
      placement="right"
      :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :visible="consoleVisible"
      @close="
        () => {
          this.consoleVisible = false;
        }
      "
    >
      <console v-if="consoleVisible" :visible="consoleVisible" :id="temp.id"></console>
    </a-drawer>
  </div>
</template>
<script>
import {apiVersions, dcokerSwarmLeaveForce, deleteDcoker, dockerList, editDocker, editDockerByFile} from "@/api/docker-api";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {dockerSwarmListAll, initDockerSwarm, joinDockerSwarm} from "@/api/docker-swarm";
import {parseTime} from "@/utils/time";
import Console from "./console";
import {mapGetters} from "vuex";

export default {
  components: {
    Console,
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      apiVersions: [],
      list: [],
      uploadFileList: [],
      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,
      initSwarmVisible: false,
      joinSwarmVisible: false,
      swarmList: [],
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "host", dataIndex: "host", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", dataIndex: "status", ellipsis: true, align: "center", width: 80, scopedSlots: { customRender: "status" } },
        { title: "TLS 认证", dataIndex: "tlsVerify", width: 100, align: "center", ellipsis: true, scopedSlots: { customRender: "tlsVerify" } },
        { title: "集群ID", dataIndex: "swarmId", ellipsis: true, align: "center", scopedSlots: { customRender: "tooltip" } },
        // { title: "apiVersion", dataIndex: "apiVersion", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "最后修改人", dataIndex: "modifyUser", width: 120, ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, align: "center", width: 190 },
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: "请填写容器名称", trigger: "blur" }],
        host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],
        tagInput: [
          // { required: true, message: "Please input ID", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: "标签限制为字母数字且长度 1-10" },
        ],
        swarmId: [{ required: true, message: "请选择要加入到哪个集群", trigger: "blur" }],
        role: [{ required: true, message: "请选择节点角色", trigger: "blur" }],
        remoteAddr: [
          { required: true, message: "请填写集群IP", trigger: "blur" },
          {
            pattern: /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/,
            message: "填写正确的IP地址",
          },
        ],
        tag: [
          { required: true, message: "请填写关联容器标签", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: "标签限制为字母数字且长度 1-10" },
        ],
      },
    };
  },
  computed: {
    ...mapGetters(["getCollapsed"]),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    this.loadApiVersions();
    this.loadData();
  },
  methods: {
    //
    loadApiVersions() {
      apiVersions().then((res) => {
        this.apiVersions = res.data;
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.editVisible = true;
      this.uploadFileList = [];
      this.$refs["editForm"]?.resetFields();
    },
    // 控制台
    handleConsole(record) {
      this.temp = record;
      this.consoleVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = record;
      this.editVisible = true;
      this.uploadFileList = [];
      let tagsArray = (record.tags || "").split(":");
      // console.log(tagsArray);
      tagsArray = tagsArray.filter((item) => item.length);
      this.temp = { ...this.temp, tagsArray: tagsArray };
      //.tags = (this.temp.tagsArray || []).join(",");
      this.$refs["editForm"]?.resetFields();
    },
    handleRemove() {
      // const index = this.uploadFileList.indexOf(file);
      // const newFileList = this.uploadFileList.slice();
      // newFileList.splice(index, 1);
      this.uploadFileList = [];
    },
    beforeUpload(file) {
      this.uploadFileList = [...this.uploadFileList, file];
      return false;
    },
    // 提交  数据
    handleEditOk() {
      // 检验表单
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        if (this.uploadFileList.length) {
          const formData = new FormData();
          formData.append("file", this.uploadFileList[0]);
          formData.append("id", this.temp.id || "");
          formData.append("name", this.temp.name || "");
          formData.append("tlsVerify", this.temp.tlsVerify || "");
          formData.append("host", this.temp.host || "");
          formData.append("apiVersion", this.temp.apiVersion || "");
          formData.append("heartbeatTimeout", this.temp.heartbeatTimeout || "");
          // 提交数据
          editDockerByFile(formData).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });

              this.editVisible = false;
              this.loadData();
            }
          });
        } else {
          const temp = Object.assign({}, this.temp);

          temp.tags = (temp.tagsArray || []).join(",");
          delete temp.tagsArray;
          delete temp.inputVisible;
          delete temp.tagInput;
          editDocker(temp).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
              this.editVisible = false;
              this.loadData();
            }
          });
        }
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除该记录么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          deleteDcoker(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 强制解绑
    handleLeaveForce(record) {
      const html =
        "<h1 style='color:red;'>真的要强制退出集群吗？</h1> " +
        "<h3 style='color:red;'>如果当前集群还存在可能出现数据不一致问题奥</h3> " +
        "<ul style='color:red;'>" +
        "<li>请提前备份数据再操作奥</li>" +
        "<li>操作不能撤回奥</li>" +
        " </ul>";
      const h = this.$createElement;
      this.$confirm({
        title: "系统提示",
        content: h("div", null, [h("p", { domProps: { innerHTML: html } }, null)]),
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 组装参数
          const params = {
            id: record.id,
          };
          dcokerSwarmLeaveForce(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // handleClose(removedTag) {},
    showInput() {
      this.temp = { ...this.temp, inputVisible: true };
      this.$nextTick(function () {
        this.$refs.tagInput.focus();
      });
    },
    handleInputConfirm() {
      this.$refs["editForm"].validateField("tagInput", (errmsg) => {
        if (errmsg) {
          // console.log(err);
          this.$notification.warn({
            message: errmsg,
          });
          return false;
        }
        const inputValue = this.temp.tagInput;
        let tags = this.temp.tagsArray || [];
        if (inputValue && tags.indexOf(inputValue) === -1) {
          tags = [...tags, inputValue];
        }

        this.temp = { ...this.temp, tagsArray: tags, tagInput: "", inputVisible: false };
      });
    },
    // 创建集群
    initSwarm(record) {
      this.temp = {
        dockerId: record.id,
      };
      this.initSwarmVisible = true;
      this.$refs["initForm"]?.resetFields();
    },
    // 加入集群
    joinSwarm(record) {
      dockerSwarmListAll().then((res) => {
        this.swarmList = res.data;
        this.temp = {
          dockerId: record.id,
        };
        this.joinSwarmVisible = true;
        this.$refs["joinForm"]?.resetFields();
      });
    },
    handleSwarm() {
      this.$refs["initForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        initDockerSwarm(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.initSwarmVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 处理加入集群
    handleSwarmJoin() {
      this.$refs["joinForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        this.temp.id = this.temp.swarmId;
        joinDockerSwarm(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.joinSwarmVisible = false;
            this.loadData();
          }
        });
      });
    },
  },
};
</script>
<style scoped></style>
