<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$t('pages.docker.list.dd2184f3')" :sub-title="$t('pages.docker.list.8fe35bdb')">
        <template #extra>
          <router-link to="/system/assets/docker-list">
            <a-button key="console" type="primary">{{ $t('pages.docker.list.b533d598') }}</a-button></router-link
          >
        </template>
      </a-result>
    </template>
    <!-- 数据表格 -->
    <CustomTable
      v-else
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="docker-list"
      :empty-description="$t('pages.docker.list.a3546162')"
      :active-page="activePage"
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.docker.list.3e34ec28')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$t('pages.docker.list.5da9e22d')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.docker.list.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $t('pages.docker.list.ff284043') }}</a-button
          >
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex instanceof Array && column.dataIndex.includes('status')">
          <template v-if="record.machineDocker">
            <a-tag v-if="record.machineDocker.status === 1" color="green">{{ $t('pages.docker.list.52d29b16') }}</a-tag>
            <a-tooltip v-else :title="record.machineDocker.failureMsg">
              <a-tag color="red">{{ $t('pages.docker.list.921c8b81') }}</a-tag>
            </a-tooltip>
          </template>

          <a-tooltip v-else :title="$t('pages.docker.list.c69c4c30')">
            <a-tag color="red">{{ $t('pages.docker.list.4f21c0a4') }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'tags'">
          <a-tooltip
            :title="
              (text || '')
                .split(':')
                .filter((item) => item)
                .join(',')
            "
          >
            <a-tag v-for="item in (text || '').split(':').filter((item) => item)" :key="item"> {{ item }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button
              size="small"
              type="primary"
              :disabled="!record.machineDocker || record.machineDocker.status !== 1"
              @click="handleConsole(record)"
              >{{ $t('pages.docker.list.5139b7d7') }}</a-button
            >
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.docker.list.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.docker.list.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.docker.list.2e8ce846')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.docker.list.cddbe6bd')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('pages.docker.list.cddbe6bd')" />
        </a-form-item>

        <a-form-item
          :label="$t('pages.docker.list.d2114ab5')"
          name="tagInput"
          :help="
            $t('pages.docker.list.3727734b', {
              slot1: $t('pages.docker.list.d2114ab5')
            })
          "
        >
          <a-space direction="vertical" style="width: 100%">
            <div>
              <a-tooltip v-for="(tag, index) in temp.tagsArray" :key="index" :title="tag">
                <a-tag
                  :key="tag"
                  :closable="true"
                  @close="
                    () => {
                      temp.tagsArray = temp.tagsArray.filter((removedTag) => tag !== removedTag)
                    }
                  "
                >
                  {{ `${tag}` }}
                </a-tag>
              </a-tooltip>
            </div>

            <a-input
              v-if="temp.inputVisible"
              ref="tagInput"
              v-model:value="temp.tagInput"
              type="text"
              size="small"
              :placeholder="$t('pages.docker.list.2749e827')"
              @blur="handleInputConfirm"
              @keyup.enter="handleInputConfirm"
            />
            <template v-else>
              <a-tag
                v-if="!temp.tagsArray || temp.tagsArray.length < 10"
                style="borderstyle: dashed"
                @click="showInput"
              >
                <PlusOutlined /> {{ $t('pages.docker.list.7d46652a') }}
              </a-tag>
            </template>
          </a-space>
        </a-form-item>
      </a-form>
    </CustomModal>

    <console
      v-if="consoleVisible"
      :id="temp.id"
      :visible="consoleVisible"
      url-prefix="/docker"
      @close="onClose"
    ></console>
    <!-- </a-drawer> -->
    <!-- 同步到其他工作空间 -->
    <CustomModal
      v-if="syncToWorkspaceVisible"
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.docker.list.6cc42b32')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('pages.docker.list.d5e02e8a')" type="warning">
        <template #description>
          <ul>
            <li>{{ $t('pages.docker.list.b74cd503') }}</li>
            <li>{{ $t('pages.docker.list.f8158ed2') }}</li>
            <li>{{ $t('pages.docker.list.a7db4ba1') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('pages.docker.list.bc069d5d')" name="workspaceId">
          <a-select
            v-model:value="temp.workspaceId"
            show-search
            :filter-option="
              (input, option) => {
                const children = option.children && option.children()
                return (
                  children &&
                  children[0].children &&
                  children[0].children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                )
              }
            "
            :placeholder="$t('pages.docker.list.3a321a02')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id" :disabled="getWorkspaceId() === item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { deleteDcoker, dockerList, editDocker, syncToWorkspace } from '@/api/docker-api'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import Console from './console'
import { mapState } from 'pinia'
import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    Console
  },
  props: {},
  data() {
    return {
      loading: true,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      list: [],

      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,

      columns: [
        {
          title: this.$t('pages.docker.list.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          width: 100
        },
        {
          title: 'host',
          dataIndex: ['machineDocker', 'host'],
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.docker.list.9c32c887'),
          dataIndex: ['machineDocker', 'status'],
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: `docker${this.$t('pages.docker.list.d826aba2')}`,
          dataIndex: ['machineDocker', 'dockerVersion'],
          ellipsis: true,
          width: '120px',
          tooltip: true
        },
        {
          title: this.$t('pages.docker.list.d2114ab5'),
          dataIndex: 'tags',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.list.7e27adb6'),
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('pages.docker.list.f06e8846'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.docker.list.61164914'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.docker.list.a0fe2109'),
          dataIndex: 'operation',

          fixed: 'right',
          align: 'center',
          width: '190px'
        }
      ],

      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$t('pages.docker.list.ce3b796b'), trigger: 'blur' }],
        host: [{ required: true, message: this.$t('pages.docker.list.116c39f3'), trigger: 'blur' }],
        tagInput: [
          // { required: true, message: "Please input ID", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: this.$t('pages.docker.list.a4c276e2') }
        ],

        tag: [
          { required: true, message: this.$t('pages.docker.list.d6fbcd84'), trigger: 'blur' },
          { pattern: /^\w{1,10}$/, message: this.$t('pages.docker.list.a4c276e2') }
        ]
      },
      workspaceList: [],
      tableSelections: [],
      syncToWorkspaceVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useUserStore, ['getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    useSuggestions() {
      if (this.loading) {
        // 加载中不提示
        return false
      }
      if (!this.getUserInfo || !this.getUserInfo.systemUser) {
        // 没有登录或者不是超级管理员
        return false
      }
      if (this.listQuery.page !== 1 || this.listQuery.total > 0) {
        // 不是第一页 或者总记录数大于 0
        return false
      }
      // 判断是否存在搜索条件
      const nowKeys = Object.keys(this.listQuery)
      const defaultKeys = Object.keys(PAGE_DEFAULT_LIST_QUERY)
      const dictOrigin = nowKeys.filter((item) => !defaultKeys.includes(item))
      return dictOrigin.length === 0
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
          const dockerId = this.$route.query.dockerId
          this.list.map((item) => {
            if (dockerId === item.id) {
              this.handleConsole(item)
            }
          })
        }
        this.loading = false
      })
    },
    // 新增
    handleAdd() {
      this.temp = {}
      this.editVisible = true

      this.$refs['editForm']?.resetFields()
    },
    // 控制台
    handleConsole(record) {
      this.temp = { ...record }
      this.consoleVisible = true

      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'docker' }
        })
      }
    },
    // 关闭抽屉层
    onClose() {
      this.consoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = { ...record }
      this.editVisible = true

      let tagsArray = (record.tags || '').split(':')
      // console.log(tagsArray);
      tagsArray = tagsArray.filter((item) => item.length)
      this.temp = { ...this.temp, tagsArray: tagsArray }
      //.tags = (this.temp.tagsArray || []).join(",");
      this.$refs['editForm']?.resetFields()
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        const temp = Object.assign({}, this.temp)

        temp.tags = (temp.tagsArray || []).join(',')
        delete temp.tagsArray
        delete temp.inputVisible
        delete temp.tagInput
        this.confirmLoading = true
        editDocker(temp)
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
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('pages.docker.list.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.docker.list.e51646aa'),
        okText: this.$t('pages.docker.list.e8e9db25'),
        cancelText: this.$t('pages.docker.list.b12468e9'),
        onOk: () => {
          return deleteDcoker({
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

    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // handleClose(removedTag) {},
    showInput() {
      this.temp = { ...this.temp, inputVisible: true }
      this.$nextTick(function () {
        this.$refs.tagInput.focus()
      })
    },
    handleInputConfirm() {
      this.$refs['editForm'].validateFields('tagInput').then(() => {
        const inputValue = this.temp.tagInput
        let tags = this.temp.tagsArray || []
        if (inputValue && tags.indexOf(inputValue) === -1) {
          tags = [...tags, inputValue]
        }

        this.temp = {
          ...this.temp,
          tagsArray: tags,
          tagInput: '',
          inputVisible: false
        }
      })
      // .catch((error) => {
      //   console.log(error)
      //   if (errmsgs) {
      //     $notification.warn({
      //       message: errmsgs
      //     })
      //     return false
      //   }
      // })
    },

    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      this.temp = {
        workspaceId: undefined
      }
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('pages.docker.list.3a321a02')
        })
        return false
      }
      // 同步
      this.confirmLoading = true
      syncToWorkspace({
        ids: this.tableSelections.join(','),
        toWorkspaceId: this.temp.workspaceId
      })
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    }
  }
}
</script>
