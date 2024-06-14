<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="certificate-list"
      :empty-description="$t('pages.certificate.list.f2b0d2c7')"
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
              :placeholder="$t('pages.certificate.list.ce1b3963')"
              @press-enter="loadData"
            />
            <a-input
              v-model:value="listQuery['%subjectDnName%']"
              allow-clear
              class="search-input-item"
              :placeholder="$t('pages.certificate.list.9b938af')"
              @press-enter="loadData"
            />
            <a-tooltip :title="$t('pages.certificate.list.554d1b95')">
              <a-button type="primary" :loading="loading" @click="loadData">{{
                $t('pages.certificate.list.53c2763c')
              }}</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleAdd">{{ $t('pages.certificate.list.66e7821f') }}</a-button>
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
          <a-popover :title="$t('pages.certificate.list.d4f87c9d')">
            <template #content>
              <p>{{ $t('pages.certificate.list.eee04e4f') }}{{ record.description }}</p>
            </template>
            <!-- {{ text }} -->
            <a-button type="link" style="padding: 0" size="small" @click="handleEdit(record)">{{ text }}</a-button>
          </a-popover>
        </template>
        <template v-else-if="column.dataIndex === 'fileExists'">
          <a-tag v-if="text" color="green">{{ $t('pages.certificate.list.a78774e2') }}</a-tag>
          <a-tag v-else color="red">{{ $t('pages.certificate.list.a51066b1') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'workspaceId'">
          <a-tag v-if="text === 'GLOBAL'">{{ $t('pages.certificate.list.fd0310d0') }}</a-tag>
          <a-tag v-else>{{ $t('pages.certificate.list.afacc4cb') }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDeployFile(record)">{{
              $t('pages.certificate.list.75099561')
            }}</a-button>
            <a-button size="small" type="primary" @click="handleDownload(record)">{{
              $t('pages.certificate.list.a5bebb0f')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.certificate.list.dd20d11c')
            }}</a-button>
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
      :title="$t('pages.certificate.list.66e7821f')"
      :mask-closable="false"
      @ok="handleEditCertOk"
    >
      <a-form ref="importCertForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.certificate.list.539387e0')" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="pkcs12"> pkcs12(pfx) </a-radio>
            <a-radio value="JKS"> JKS </a-radio>
            <a-radio value="X.509"> X.509(pem、key、crt、cer) </a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('pages.certificate.list.9f4bdc32')" name="file">
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
            <a-button><UploadOutlined />{{ $t('pages.certificate.list.51eeb245') }}</a-button>
          </a-upload>
          <template v-else>{{ $t('pages.certificate.list.51afd37c') }}</template>
        </a-form-item>
        <a-form-item
          v-if="temp.type && temp.type !== 'X.509'"
          :label="$t('pages.certificate.list.ac2ef19e')"
          name="password"
          :help="$t('pages.certificate.list.f13a934d')"
        >
          <a-input v-model:value="temp.password" :placeholder="$t('pages.certificate.list.ac2ef19e')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 编辑证书 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.certificate.list.16773a22')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$t('pages.certificate.list.3a750854')" name="global">
          <a-radio-group v-model:value="temp.global">
            <a-radio :value="true"> {{ $t('pages.certificate.list.fd0310d0') }} </a-radio>
            <a-radio :value="false"> {{ $t('pages.certificate.list.919267cc') }} </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="$t('pages.certificate.list.d4f87c9d')" name="description">
          <a-textarea v-model:value="temp.description" :placeholder="$t('pages.certificate.list.5e6ba316')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 发布文件 -->
    <a-modal
      v-model:open="releaseFileVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.certificate.list.25775bd2')"
      width="60%"
      :mask-closable="false"
      @ok="releaseFileOk()"
    >
      <a-alert :message="$t('pages.certificate.list.f2e66875')" type="info" show-icon style="margin-bottom: 10px" />
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
          title: this.$t('pages.certificate.list.72cebb96'),
          dataIndex: 'serialNumberStr',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('pages.certificate.list.539387e0'),
          dataIndex: 'keyType',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: this.$t('pages.certificate.list.34e8b7bd'),
          dataIndex: 'fileExists',
          ellipsis: true,

          width: '80px'
        },
        {
          title: this.$t('pages.certificate.list.65860154'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '90px'
        },
        {
          title: this.$t('pages.certificate.list.ce1b3963'),
          dataIndex: 'issuerDnName',
          ellipsis: true,
          width: 200,
          tooltip: true
        },
        {
          title: this.$t('pages.certificate.list.9b938af'),
          dataIndex: 'subjectDnName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.certificate.list.4ac82fe2'),
          dataIndex: 'sigAlgName',
          ellipsis: true,
          width: 150,
          tooltip: true
        },
        {
          title: this.$t('pages.certificate.list.65d0dc35'),
          dataIndex: 'sigAlgOid',
          ellipsis: true,
          width: 150,
          tooltip: true
        },

        {
          title: this.$t('pages.certificate.list.bdfd1242'),
          dataIndex: 'effectiveTime',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('pages.certificate.list.19741306'),
          dataIndex: 'expirationTime',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.certificate.list.d826aba2'),
          dataIndex: 'certVersion',
          ellipsis: true,
          width: '80px',
          tooltip: true
        },
        {
          title: this.$t('pages.certificate.list.db3c9202'),
          dataIndex: 'createUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('pages.certificate.list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('pages.certificate.list.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.certificate.list.3bb962bf'),
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
        type: [{ required: true, message: this.$t('pages.certificate.list.71717caf'), trigger: 'blur' }]
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
            message: this.$t('pages.certificate.list.71301d96')
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
        title: this.$t('pages.certificate.list.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.certificate.list.987c2cd6'),
        okText: this.$t('pages.certificate.list.e8e9db25'),
        cancelText: this.$t('pages.certificate.list.b12468e9'),
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
          message: this.$t('pages.certificate.list.d1dba43e')
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
