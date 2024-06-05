<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="certificate-list"
      :empty-description="$tl('p.noCert')"
      :data-source="list"
      size="middle"
      :loading="loading"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-space>
            <a-input
              v-model:value="listQuery['%issuerDnName%']"
              allow-clear
              class="search-input-item"
              :placeholder="$tl('c.issue')"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%subjectDnName%']"
              allow-clear
              class="search-input-item"
              :placeholder="$tl('c.subject')"
              @press-enter="loadData"
            />
            <a-tooltip :title="$tl('p.quickBack')">
              <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleAdd">{{ $tl('c.importCert') }}</a-button>
          </a-space>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'serialNumberStr'">
          <a-popover :title="$tl('c.certDesc')">
            <template #content>
              <p>{{ $tl('p.desc') }}{{ record.description }}</p>
            </template>
            <!-- {{ text }} -->
            <a-button type="link" style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</a-button>
          </a-popover>
        </template>
        <template v-else-if="column.dataIndex === 'fileExists'">
          <a-tag v-if="text" color="green">{{ $tl('p.exist') }}</a-tag>
          <a-tag v-else color="red">{{ $tl('p.lost') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $tl('c.global') }}</a-tag>
          <a-tag v-else>{{ $tl('p.workspace') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDeployFile(record)">{{ $tl('p.deploy') }}</a-button>
            <a-button size="small" type="primary" @click="handleDownload(record)">{{ $tl('p.export') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 导入 -->
    <a-modal
      v-model:open="editCertVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="700px"
      :title="$tl('c.importCert')"
      :mask-closable="false"
      @ok="handleEditCertOk"
    >
      <a-form ref="importCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.certType')" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="pkcs12"> pkcs12(pfx) </a-radio>
            <a-radio value="JKS"> JKS </a-radio>
            <a-radio value="X.509"> X.509(pem、key、crt、cer) </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$tl('p.certFile')" name="file">
          <a-upload
            v-if="temp.type"
            :file-list="uploadFileList"
            :before-upload="
              (file) => {
                uploadFileList = [file]
                return false
              }
            "
            :accept="typeAccept[temp.type]"
            @remove="
              () => {
                uploadFileList = []
                return true
              }
            "
          >
            <a-button><UploadOutlined />{{ $tl('p.selectFile') }}</a-button>
          </a-upload>
          <template v-else>{{ $tl('p.selectType') }}</template>
        </a-form-item>
        <a-form-item
          v-if="temp.type && temp.type !== 'X.509'"
          :label="$tl('c.certPwd')"
          name="password"
          :help="$tl('p.parseTxt')"
        >
          <a-input v-model:value="temp.password" :placeholder="$tl('c.certPwd')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 编辑证书 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.editCert')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('p.certShare')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $tl('c.global') }} </a-radio>
            <a-radio :value="false"> {{ $tl('p.currentWorkspace') }} </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$tl('c.certDesc')" name="description">
          <a-textarea v-model:value="temp.description" :placeholder="$tl('p.enterDesc')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 发布文件 -->
    <a-modal
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.deployCert')"
      width="60%"
      :mask-closable="false"
      @ok="releaseFileOk()"
    >
      <a-alert :message="$tl('p.uploadZip')" type="info" show-icon style="margin-bottom: 10px" />
      <releaseFile v-if="releaseFileVisible" ref="releaseFile" @commit="handleCommitTask"></releaseFile>
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
  emits: ['confirm'],
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
          title: this.$tl('p.serialNumber'),
          dataIndex: 'serialNumberStr',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$tl('c.certType'),
          dataIndex: 'keyType',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: this.$tl('p.fileStatus'),
          dataIndex: 'fileExists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$tl('p.share'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$tl('c.issue'),
          dataIndex: 'issuerDnName',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$tl('c.subject'),
          dataIndex: 'subjectDnName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.keyAlgo'),
          dataIndex: 'sigAlgName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$tl('p.algoOid'),
          dataIndex: 'sigAlgOid',
          ellipsis: true,
          width: 150,
          tooltip: true
        },

        {
          title: this.$tl('p.effectiveTime'),
          dataIndex: 'effectiveTime',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$tl('p.expiryTime'),
          dataIndex: 'expirationTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.version'),
          dataIndex: 'certVersion',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: this.$tl('p.creator'),
          dataIndex: 'createUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
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
        type: [{ required: true, message: this.$tl('p.selectCertType'), trigger: 'blur' }]
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
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
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
    $tl(key, ...args) {
      return this.$t(`pages.certificate.list.${key}`, ...args)
    },
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
            message: this.$tl('p.selectCertFile')
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
      $confirm({
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDelete'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
        onOk() {
          return deleteCert({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
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
        $notification.error({
          message: this.$tl('p.selectCert')
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
