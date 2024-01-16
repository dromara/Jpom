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
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
          this.loadData()
        }
      "
      bordered
      rowKey="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-space>
            <a-input
              allowClear
              class="search-input-item"
              @pressEnter="loadData"
              v-model:value="listQuery['%issuerDnName%']"
              placeholder="颁发者"
            />
            <a-input
              allowClear
              class="search-input-item"
              @pressEnter="loadData"
              v-model:value="listQuery['%subjectDnName%']"
              placeholder="主题"
            />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleAdd">导入证书</a-button>
          </a-space>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'serialNumberStr'">
          <a-popover title="证书描述">
            <template v-slot:content>
              <p>描述：{{ record.description }}</p>
            </template>
            <!-- {{ text }} -->
            <a-button type="link" style="padding: 0" @click="handleEdit(record)" size="small">{{ text }}</a-button>
          </a-popover>
        </template>
        <template v-else-if="column.dataIndex === 'fileExists'">
          <a-tag v-if="text" color="green">存在</a-tag>
          <a-tag v-else color="red">丢失</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
          <a-tag v-else>工作空间</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDeployFile(record)">部署</a-button>
            <a-button size="small" type="primary" @click="handleDownload(record)">导出</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 导入 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editCertVisible"
      width="700px"
      title="导入证书"
      @ok="handleEditCertOk"
      :maskClosable="false"
    >
      <a-form ref="importCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="证书类型" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="pkcs12"> pkcs12(pfx) </a-radio>
            <a-radio value="JKS"> JKS </a-radio>
            <a-radio value="X.509"> X.509(pem、key、crt、cer) </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="证书文件" name="file">
          <a-upload
            v-if="temp.type"
            :file-list="uploadFileList"
            @remove="
              () => {
                uploadFileList = []
                return true
              }
            "
            :before-upload="
              (file) => {
                this.uploadFileList = [file]
                return false
              }
            "
            :accept="typeAccept[temp.type]"
          >
            <a-button><UploadOutlined />选择文件</a-button>
          </a-upload>
          <template v-else>请选选择类型</template>
        </a-form-item>
        <a-form-item
          v-if="temp.type && temp.type !== 'X.509'"
          label="证书密码"
          name="password"
          help="如果未填写将解析压缩包里面的 txt"
        >
          <a-input v-model:value="temp.password" placeholder="证书密码" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 编辑证书 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      :title="`编辑证书`"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="证书共享" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> 全局 </a-radio>
            <a-radio :value="false"> 当前工作空间 </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="证书描述" name="description">
          <a-textarea v-model:value="temp.description" placeholder="请输入证书描述" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 发布文件 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="releaseFileVisible"
      title="部署证书"
      width="50%"
      :maskClosable="false"
      @ok="releaseFileOk()"
    >
      <a-alert message="证书将打包成 zip 文件上传到对应的文件夹" type="info" show-icon style="margin-bottom: 10px" />
      <releaseFile ref="releaseFile" v-if="releaseFileVisible" @commit="handleCommitTask"></releaseFile>
    </a-modal>
  </div>
</template>

<script>
import {
  certificateImportFile,
  certList,
  deleteCert,
  downloadCert,
  certificateEdit,
  certificateDeploy,
  certListAll
} from '@/api/tools/certificate'
import { parseTime, CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY } from '@/utils/const'
import releaseFile from '@/pages/file-manager/fileStorage/releaseFile.vue'
export default {
  components: {
    releaseFile
  },
  props: {
    showAll: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],
      uploadFileList: [],
      typeAccept: {
        pkcs12: '.pfx,.zip',
        JKS: '.jks,.zip',
        'X.509': '.zip'
      },
      temp: {},
      editCertVisible: false,

      columns: [
        {
          title: '序列号 (SN)',
          dataIndex: 'serialNumberStr',
          ellipsis: true,
          width: 150
        },
        {
          title: '证书类型',
          dataIndex: 'keyType',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: '文件状态',
          dataIndex: 'fileExists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: '共享',
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: '颁发者',
          dataIndex: 'issuerDnName',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: '主题',
          dataIndex: 'subjectDnName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '密钥算法',
          dataIndex: 'sigAlgName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: '算法 OID',
          dataIndex: 'sigAlgOid',
          ellipsis: true,
          width: 150,
          tooltip: true
        },

        {
          title: '生效时间',
          dataIndex: 'effectiveTime',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: '到期时间',
          dataIndex: 'expirationTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '版本号',
          dataIndex: 'certVersion',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: '创建人',
          dataIndex: 'createUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',

          width: '180px'
        }
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        // name: [{ required: true, message: "Please input name", trigger: "blur" }],
        // path: [{ required: true, message: "Please select path", trigger: "blur" }],
        type: [{ required: true, message: '请选择证书类型', trigger: 'blur' }]
      },
      releaseFileVisible: false,
      editVisible: false,
      confirmLoading: false,
      tableSelections: []
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: 'radio'
      }
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    CHANGE_PAGE,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.loading = true
      const api = this.showAll ? certListAll : certList
      api(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },

    // 新增
    handleAdd() {
      this.temp = {}
      this.editCertVisible = true
      this.uploadFileList = []
      this.$refs['importCertForm']?.resetFields()
    },
    // // 修改
    // handleEdit(record) {
    //   this.temp = Object.assign({}, record);
    //   this.uploadFileList = [];
    //   this.editCertVisible = true;
    // },

    // 提交 Cert 数据
    handleEditCertOk() {
      // 检验表单
      this.$refs['importCertForm'].validate().then(() => {
        if (this.uploadFileList.length === 0) {
          $notification.error({
            message: '请选择证书文件'
          })
          return false
        }
        const formData = new FormData()
        formData.append('file', this.uploadFileList[0])
        formData.append('type', this.temp.type)
        formData.append('password', this.temp.password || '')

        // 提交数据
        this.confirmLoading = true
        certificateImportFile(formData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editCertVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDelete(record) {
      const that = this
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除该证书么，删除会将证书文件一并删除奥？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 组装参数
            const params = {
              id: record.id
            }
            deleteCert(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 下载证书文件
    handleDownload(record) {
      // 请求参数
      const params = {
        id: record.id
      }
      // 请求接口拿到 blob
      window.open(downloadCert(params), '_blank')
    },
    // 编辑
    handleEdit(item) {
      this.temp = {
        ...item,
        global: item.workspaceId === 'GLOBAL',
        workspaceId: ''
      }
      this.editVisible = true
      this.$refs['editForm']?.resetFields()
    },
    // 编辑确认
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        this.confirmLoading = true
        certificateEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    handleDeployFile(record) {
      this.releaseFileVisible = true
      this.temp = { id: record.id }
    },

    handleCommitTask(data) {
      this.confirmLoading = true
      certificateDeploy({ ...data, id: this.temp.id })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

            this.releaseFileVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },

    releaseFileOk() {
      this.$refs.releaseFile?.tryCommit()
    },
    // 确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notificationing({
          message: '请选择要使用的证书'
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return item.id === this.tableSelections[0]
      })[0]

      this.$emit('confirm', `${selectData.serialNumberStr}:${selectData.keyType}`)
    }
  }
}
</script>
