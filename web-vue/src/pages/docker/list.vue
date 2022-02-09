<template>
  <div class="node-full-content">
    <!-- <div ref="filter" class="filter">
     
    </div> -->
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" @change="changePage" :pagination="listQuery.total / listQuery.limit > 1 ? pagination : false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery['%name%']" placeholder="名称" class="search-input-item" />
          <a-input v-model="listQuery['%host%']" placeholder="host" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">添加 Docker</a-button>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <template slot="certExist" slot-scope="text, record">
        <template v-if="record.tlsVerify">
          <a-tag v-if="record.certExist" color="green"> 存在 </a-tag>
          <a-tag v-else color="red"> 不存在 </a-tag>
        </template>
        <span v-else>-</span>
      </template>

      <a-tooltip slot="tlsVerify" slot-scope="text, record" placement="topLeft" :title="record.tlsVerify ? '开启 TLS 认证' : '关闭 TLS 认证'">
        <a-switch v-model="record.tlsVerify" :disabled="true" checked-children="开" un-checked-children="关" />
      </a-tooltip>

      <template slot="status" slot-scope="text, record">
        <a-tooltip :title="record.failureMsg || ''">
          <a-switch :checked="parseInt(record.status) === 1" :disabled="true">
            <a-icon slot="checkedChildren" type="check-circle" />
            <a-icon slot="unCheckedChildren" type="warning" />
          </a-switch>
        </a-tooltip>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" :disabled="parseInt(record.status) !== 1" @click="handleConsole(record)">控制台</a-button>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <!--
          <a-button type="primary" @click="handleTemplate(record)">模板</a-button> -->
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editVisible" title="编辑  Docker" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <!-- <a-form-model-item v-if="temp.id" label="容器ID" prop="id">
          <a-input v-model="temp.id" disabled readonly />
        </a-form-model-item> -->
        <a-form-model-item label="容器名称" prop="name">
          <a-input v-model="temp.name" placeholder="容器名称" />
        </a-form-model-item>
        <a-form-model-item label="host" prop="host">
          <a-input v-model="temp.host" placeholder="容器地址 tcp://127.0.0.1:2375" />
        </a-form-model-item>
        <a-form-model-item label="接口版本" prop="apiVersion">
          <a-select show-search option-filter-prop="children" v-model="temp.apiVersion" allowClear placeholder="接口版本">
            <a-select-option v-for="item in apiVersions" :key="item.version">{{ item.webVersion }}</a-select-option>
          </a-select>
        </a-form-model-item>
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
          <template v-for="tag in temp.tagsArray">
            <a-tooltip :key="tag" :title="tag">
              <a-tag :key="tag" :closable="true" @close="() => handleClose(tag)">
                {{ `${tag}` }}
              </a-tag>
            </a-tooltip>
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
    <!-- 控制台 -->
    <a-modal v-model="consoleVisible" :title="`${temp.name} 控制台`" width="80vw" :footer="null" :maskClosable="false">
      <console v-if="consoleVisible" :id="temp.id"></console>
    </a-modal>
  </div>
</template>
<script>
import { dockerList, apiVersions, editDocker, editDockerByFile, deleteDcoker } from "@/api/docker-api";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";
import { parseTime } from "@/utils/time";
import Console from "./console";
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
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "host", dataIndex: "host", ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "状态", dataIndex: "status", ellipsis: true, width: 90, scopedSlots: { customRender: "status" } },
        { title: "TLS 认证", dataIndex: "tlsVerify", width: 90, ellipsis: true, scopedSlots: { customRender: "tlsVerify" } },
        { title: "证书状态", dataIndex: "certExist", width: 90, ellipsis: true, scopedSlots: { customRender: "certExist" } },
        { title: "apiVersion", dataIndex: "apiVersion", width: 100, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "最后修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" } },
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
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 190 },
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: "请填写容器名称", trigger: "blur" }],
        host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],
        tagInput: [
          // { required: true, message: "Please input ID", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: "标签限制为字母数字且长度 1-10" },
        ],
      },
    };
  },
  computed: {
    pagination() {
      return {
        total: this.listQuery.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
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
          this.temp.tags = (this.temp.tagsArray || []).join(",");
          delete this.temp.tagsArray;
          delete this.temp.inputVisible;
          delete this.temp.tagInput;
          editDocker(this.temp).then((res) => {
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
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
    handleClose(removedTag) {
      this.temp.tagsArray = this.temp.tagsArray.filter((tag) => tag !== removedTag);
    },
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
  },
};
</script>
<style scoped></style>
