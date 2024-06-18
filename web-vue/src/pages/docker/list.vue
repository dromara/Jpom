<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$t('i18n_f9cea44f02')" :sub-title="$t('i18n_56469e09f7')">
        <template #extra>
          <router-link to="/system/assets/docker-list">
            <a-button key="console" type="primary">{{ $t('i18n_6dcf6175d8') }}</a-button></router-link
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
      :empty-description="$t('i18n_4188f4101c')"
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
            :placeholder="$t('i18n_d7ec2d3fea')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $t('i18n_398ce396cd') }}</a-button
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
            <a-tag v-if="record.machineDocker.status === 1" color="green">{{ $t('i18n_fd6e80f1e0') }}</a-tag>
            <a-tooltip v-else :title="record.machineDocker.failureMsg">
              <a-tag color="red">{{ $t('i18n_757a730c9e') }}</a-tag>
            </a-tooltip>
          </template>

          <a-tooltip v-else :title="$t('i18n_33675a9bb3')">
            <a-tag color="red">{{ $t('i18n_5169b9af9d') }}</a-tag>
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
              >{{ $t('i18n_b5c3770699') }}</a-button
            >
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
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
      :title="$t('i18n_657969aa0f')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_a51cd0898f')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$t('i18n_a51cd0898f')" />
        </a-form-item>

        <a-form-item
          :label="$t('i18n_14d342362f')"
          name="tagInput"
          :help="$t('i18n_05b52ae2db', { slot1: $t('i18n_14d342362f') })"
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
              :placeholder="$t('i18n_baef58c283')"
              @blur="handleInputConfirm"
              @keyup.enter="handleInputConfirm"
            />
            <template v-else>
              <a-tag
                v-if="!temp.tagsArray || temp.tagsArray.length < 10"
                :style="{
                  borderStyle: dashed
                }"
                @click="showInput"
              >
                <PlusOutlined /> {{ $t('i18n_66ab5e9f24') }}
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
      :title="$t('i18n_1a44b9e2f7')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$t('i18n_947d983961')" type="warning">
        <template #description>
          <ul>
            <li>{{ $t('i18n_af7c96d2b9') }}</li>
            <li>{{ $t('i18n_4c7e4dfd33') }}</li>
            <li>{{ $t('i18n_b7df1586a9') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$t('i18n_b4a8c78284')" name="workspaceId">
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
            :placeholder="$t('i18n_b3bda9bf9e')"
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
          title: this.$t('i18n_d7ec2d3fea'),
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
          title: this.$t('i18n_3fea7ca76c'),
          dataIndex: ['machineDocker', 'status'],
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: `docker${this.$t('i18n_fe2df04a16')}`,
          dataIndex: ['machineDocker', 'dockerVersion'],
          ellipsis: true,
          width: '120px',
          tooltip: true
        },
        {
          title: this.$t('i18n_14d342362f'),
          dataIndex: 'tags',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$t('i18n_3bcc1c7a20'),
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',

          fixed: 'right',
          align: 'center',
          width: '190px'
        }
      ],

      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$t('i18n_f63870fdb0'), trigger: 'blur' }],
        host: [{ required: true, message: this.$t('i18n_3604566503'), trigger: 'blur' }],
        tagInput: [
          // { required: true, message: "Please input ID", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: this.$t('i18n_89944d6ccb') }
        ],

        tag: [
          { required: true, message: this.$t('i18n_3b9418269c'), trigger: 'blur' },
          { pattern: /^\w{1,10}$/, message: this.$t('i18n_89944d6ccb') }
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_1593dc4920'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
          message: this.$t('i18n_b3bda9bf9e')
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
