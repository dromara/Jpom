<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-button type="primary" @click="handleAdd">导入证书</a-button>
          <a-button type="primary" @click="loadData">刷新</a-button>
        </a-space>
      </template>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="domain" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button size="small" type="primary" @click="handleDownload(record)">导出</a-button>
          <a-button size="small" type="primary" @click="handleTemplate(record)">模板</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editCertVisible" title="编辑 Cert" @ok="handleEditCertOk" :maskClosable="false">
      <a-form-model ref="editCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="证书 ID" prop="id">
          <a-input v-model="temp.id" :disabled="temp.type === 'edit'" placeholder="证书 ID" />
        </a-form-model-item>
        <a-form-model-item label="证书名称" prop="name">
          <a-input v-model="temp.name" placeholder="证书名称" />
        </a-form-model-item>
        <a-form-model-item label="证书路径" prop="path">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.path"
            :disabled="temp.type === 'edit'"
            placeholder="请选择证书路径"
          >
            <a-select-option v-for="element in whiteList" :key="element">{{ element }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item v-if="temp.type === 'add'" label="证书文件" prop="file">
          <a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" :accept="'.zip'">
            <a-button><a-icon type="upload" />选择文件</a-button>
          </a-upload>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 模板 -->
    <a-modal v-model="templateVisible" title="Cert 配置模板" :footer="null" :maskClosable="true">
      <pre class="config">
      ssl	on;
      listen	443 ssl;
      server_name	{{ temp.domain }};
      ssl_certificate	{{ temp.cert }};
      ssl_certificate_key	{{ temp.key }};
      ssl_session_cache	shared:SSL:1m;
      ssl_session_timeout	5m;
      ssl_ciphers	ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;
      ssl_protocols	TLSv1 TLSv1.1 TLSv1.2;
      ssl_prefer_server_ciphers	on;
      </pre>
    </a-modal>
  </div>
</template>
<script>
import {deleteCert, downloadCert, editCert, getCertList, getCertWhiteList} from "../../../../api/node-nginx";
import {parseTime} from "../../../../utils/time";

export default {
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      tableHeight: "70vh",
      whiteList: [],
      list: [],
      uploadFileList: [],
      temp: {},
      editCertVisible: false,
      templateVisible: false,
      columns: [
        { title: "ID", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "域名", dataIndex: "domain", ellipsis: true, scopedSlots: { customRender: "domain" } },
        {
          title: "生效时间",
          dataIndex: "effectiveTime",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        {
          title: "到期时间",
          dataIndex: "expirationTime",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 220 },
      ],
      rules: {
        id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: "Please input name", trigger: "blur" }],
        path: [{ required: true, message: "Please select path", trigger: "blur" }],
      },
    };
  },
  mounted() {
    // this.calcTableHeight();
    this.loadData();
    this.loadCertWhiteList();
  },
  methods: {
    // 计算表格高度
    // calcTableHeight() {
    //   this.$nextTick(() => {
    //     this.tableHeight = window.innerHeight - this.$refs["filter"].clientHeight - 155;
    //   });
    // },
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id,
      };
      getCertList(params).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },
    // 加载 cert 白名单
    loadCertWhiteList() {
      const params = {
        nodeId: this.node.id,
      };
      getCertWhiteList(params).then((res) => {
        if (res.code === 200) {
          this.whiteList = res.data;
        }
      });
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: "add",
      };
      this.editCertVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      this.temp.type = "edit";
      this.temp.path = this.temp.whitePath;
      this.editCertVisible = true;
    },
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file);
      const newFileList = this.uploadFileList.slice();
      newFileList.splice(index, 1);
      this.uploadFileList = newFileList;
    },
    beforeUpload(file) {
      this.uploadFileList = [...this.uploadFileList, file];
      return false;
    },
    // 提交 Cert 数据
    handleEditCertOk() {
      // 检验表单
      this.$refs["editCertForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        if (this.temp.type === "add" && this.uploadFileList.length === 0) {
          this.$notification.error({
            message: "请选择证书文件",
          });
          return false;
        }
        const formData = new FormData();
        formData.append("file", this.uploadFileList[0]);
        formData.append("nodeId", this.node.id);
        formData.append("data", JSON.stringify(this.temp));
        // 提交数据
        editCert(formData).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editCertForm"].resetFields();
            this.editCertVisible = false;
            this.loadData();
          }
        });
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
            nodeId: this.node.id,
            id: record.id,
          };
          deleteCert(params).then((res) => {
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
    // 下载证书文件
    handleDownload(record) {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.node.id,
        id: record.id,
      };
      // 请求接口拿到 blob
      downloadCert(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", record.domain + ".zip");
        document.body.appendChild(link);
        link.click();
      });
    },
    // 显示模板
    handleTemplate(record) {
      this.temp = Object.assign({}, record);
      this.templateVisible = true;
    },
  },
};
</script>
<style scoped>
.config {
  background-color: black;
  color: #fff;
}
</style>
