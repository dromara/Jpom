<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="certificate-list"
      :empty-description="$t('i18n_8c2da7cce9')"
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
              :placeholder="$t('i18n_f0aba63ae7')"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%subjectDnName%']"
              allow-clear
              class="search-input-item"
              :placeholder="$t('i18n_9970ad0746')"
              @press-enter="loadData"
            />
            <a-tooltip :title="$t('i18n_4838a3bd20')">
              <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleAdd">{{ $t('i18n_c1690fcca5') }}</a-button>
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
          <a-popover :title="$t('i18n_5dc1f36a27')">
            <template #content>
              <p>{{ $t('i18n_4a4e3b5ae4') }}{{ record.description }}</p>
            </template>
            <!-- {{ text }} -->
            <a-button type="link" style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</a-button>
          </a-popover>
        </template>
        <template v-else-if="column.dataIndex === 'fileExists'">
          <a-tag v-if="text" color="green">{{ $t('i18n_df9497ea98') }}</a-tag>
          <a-tag v-else color="red">{{ $t('i18n_162e219f6d') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $t('i18n_2be75b1044') }}</a-tag>
          <a-tag v-else>{{ $t('i18n_98d69f8b62') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDeployFile(record)">{{
              $t('i18n_a9f94dcd57')
            }}</a-button>
            <a-button size="small" type="primary" @click="handleDownload(record)">{{ $t('i18n_55405ea6ff') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 导入 -->
    <CustomModal
      v-if="editCertVisible"
      v-model:open="editCertVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="700px"
      :title="$t('i18n_c1690fcca5')"
      :mask-closable="false"
      @ok="handleEditCertOk"
    >
      <a-form ref="importCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_929e857766')" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="pkcs12"> pkcs12(pfx) </a-radio>
            <a-radio value="JKS"> JKS </a-radio>
            <a-radio value="X.509"> X.509(pem、key、crt、cer) </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('i18n_94aa195397')" name="file">
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
            <a-button><UploadOutlined />{{ $t('i18n_fd7e0c997d') }}</a-button>
          </a-upload>
          <template v-else>{{ $t('i18n_c3512a3d09') }}</template>
        </a-form-item>
        <a-form-item
          v-if="temp.type && temp.type !== 'X.509'"
          :label="$t('i18n_45028ad61d')"
          name="password"
          :help="$t('i18n_e8f07c2186')"
        >
          <a-input v-model:value="temp.password" :placeholder="$t('i18n_45028ad61d')" />
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 编辑证书 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_d47ea92b3a')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('i18n_e475e0c655')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $t('i18n_2be75b1044') }} </a-radio>
            <a-radio :value="false"> {{ $t('i18n_691b11e443') }} </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('i18n_5dc1f36a27')" name="description">
          <a-textarea v-model:value="temp.description" :placeholder="$t('i18n_066431a665')" />
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 发布文件 -->
    <CustomModal
      v-if="releaseFileVisible"
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('i18n_9ae40638d2')"
      width="70%"
      :mask-closable="false"
      @ok="releaseFileOk()"
    >
      <a-alert :message="$t('i18n_a62fa322b4')" type="info" show-icon style="margin-bottom: 10px" />
      <releaseFile v-if="releaseFileVisible" ref="releaseFile" @commit="handleCommitTask"></releaseFile>
    </CustomModal>
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
          title: this.$t('i18n_30aaa13963'),
          dataIndex: 'serialNumberStr',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('i18n_929e857766'),
          dataIndex: 'keyType',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: this.$t('i18n_a3d0154996'),
          dataIndex: 'fileExists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('i18n_fffd3ce745'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('i18n_f0aba63ae7'),
          dataIndex: 'issuerDnName',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$t('i18n_9970ad0746'),
          dataIndex: 'subjectDnName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_3a6c2962e1'),
          dataIndex: 'sigAlgName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('i18n_4f08d1ad9f'),
          dataIndex: 'sigAlgOid',
          ellipsis: true,
          width: 150,
          tooltip: true
        },

        {
          title: this.$t('i18n_fc92e93523'),
          dataIndex: 'effectiveTime',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_22e888c2df'),
          dataIndex: 'expirationTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_d0b2958432'),
          dataIndex: 'certVersion',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: this.$t('i18n_95a43eaa59'),
          dataIndex: 'createUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
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
        type: [{ required: true, message: this.$t('i18n_ac408e4b03'), trigger: 'blur' }]
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
            message: this.$t('i18n_4244830033')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_efe9d26148'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_94ca71ae7b')
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
