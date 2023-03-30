<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :loading="loading"
      :columns="columns"
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
          this.loadData();
        }
      "
      :row-selection="rowSelection"
      bordered
      rowKey="id"
    >
      <template slot="title">
        <a-space>
          <a-space>
            <a-input allowClear class="search-input-item" @pressEnter="loadData" v-model="listQuery['%issuerDnName%']" placeholder="颁发者" />
            <a-input allowClear class="search-input-item" @pressEnter="loadData" v-model="listQuery['%subjectDnName%']" placeholder="主题" />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleAdd">导入证书</a-button>
          </a-space>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-popover slot="name" slot-scope="text, item" title="证书描述">
        <template slot="content">
          <p>描述：{{ item.description }}</p>
        </template>
        <!-- {{ text }} -->
        {{ text }}
      </a-popover>
      <template slot="fileExists" slot-scope="text">
        <a-tag v-if="text" color="green">存在</a-tag>
        <a-tag v-else color="red">丢失</a-tag>
      </template>
      <template slot="global" slot-scope="text">
        <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
        <a-tag v-else>工作空间</a-tag>
      </template>
    </a-table>
    <!-- 导入 -->
    <a-modal destroyOnClose v-model="editCertVisible" width="700px" title="导入证书" @ok="handleEditCertOk" :maskClosable="false">
      <a-form-model ref="importCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="证书文件" prop="file" help="请上传 zip 压缩包,并且包里面必须包含：ca.pem、key.pem、cert.pem 三个文件">
          <a-upload
            :file-list="uploadFileList"
            :remove="
              () => {
                uploadFileList = [];
              }
            "
            :before-upload="
              (file) => {
                this.uploadFileList = [file];
                return false;
              }
            "
            accept=".zip"
          >
            <a-button><a-icon type="upload" />选择文件</a-button>
          </a-upload>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <div style="padding: 40px">
      <div
        :style="{
          position: 'absolute',
          right: 0,
          bottom: 0,
          width: '100%',
          borderTop: '1px solid #e9e9e9',
          padding: '10px 16px',
          background: '#fff',
          textAlign: 'right',
          zIndex: 1,
        }"
      >
        <a-space>
          <a-button
            @click="
              () => {
                this.$emit('cancel');
              }
            "
          >
            取消
          </a-button>
          <a-button type="primary" @click="handerConfirm"> 确定 </a-button>
        </a-space>
      </div>
    </div>
  </div>
</template>
<script>
import { dockerImportTls } from "@/api/system/assets-docker";
import { certListAll } from "@/api/tools/certificate";
import { parseTime, CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from "@/utils/const";

export default {
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],
      uploadFileList: [],

      temp: {},
      editCertVisible: false,

      columns: [
        { title: "序列号 (SN)", dataIndex: "serialNumberStr", ellipsis: true, width: 150, scopedSlots: { customRender: "name" } },
        { title: "证书类型", dataIndex: "keyType", ellipsis: true, width: "80px", scopedSlots: { customRender: "tooltip" } },
        { title: "文件状态", dataIndex: "fileExists", ellipsis: true, scopedSlots: { customRender: "fileExists" }, width: "80px" },
        { title: "共享", dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "global" }, width: "90px" },
        { title: "颁发者", dataIndex: "issuerDnName", ellipsis: true, width: 200, scopedSlots: { customRender: "tooltip" } },
        { title: "主题", dataIndex: "subjectDnName", ellipsis: true, width: 150, scopedSlots: { customRender: "tooltip" } },
        { title: "密钥算法", dataIndex: "sigAlgName", ellipsis: true, width: 150, scopedSlots: { customRender: "tooltip" } },
        { title: "算法 OID", dataIndex: "sigAlgOid", ellipsis: true, width: 150, scopedSlots: { customRender: "tooltip" } },

        {
          title: "生效时间",
          dataIndex: "effectiveTime",
          customRender: (text) => parseTime(text),
          sorter: true,
          width: "160px",
        },
        {
          title: "到期时间",
          dataIndex: "expirationTime",
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "160px",
        },
        { title: "版本号", dataIndex: "certVersion", ellipsis: true, width: "80px", scopedSlots: { customRender: "tooltip" } },
        { title: "创建人", dataIndex: "createUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: "120px" },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: "120px" },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "160px",
        },
      ],
      rules: {},
      tableSelections: [],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys;
        },
        selectedRowKeys: this.tableSelections,
        type: "radio",
      };
    },
  },
  mounted() {
    this.loadData();
  },
  methods: {
    CHANGE_PAGE,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      certListAll(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },

    // 添加
    handleAdd() {
      this.editCertVisible = true;
      this.uploadFileList = [];
      this.$refs["importCertForm"]?.resetFields();
    },

    // 提交 Cert 数据
    handleEditCertOk() {
      // 检验表单
      this.$refs["importCertForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        if (this.uploadFileList.length === 0) {
          this.$notification.error({
            message: "请选择证书文件",
          });
          return false;
        }
        const formData = new FormData();
        formData.append("file", this.uploadFileList[0]);

        // 提交数据
        dockerImportTls(formData).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.editCertVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        this.$notification.warning({
          message: "请选择要使用的证书",
        });
        return;
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0];
      })[0];

      this.$emit("confirm", `${selectData.serialNumberStr}:${selectData.keyType}`);
    },
  },
};
</script>
<style scoped></style>
