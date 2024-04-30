<template>
  <div>
    <template v-if="useSuggestions">
      <a-result :title="$tl('p.noDockerInWorkspace')" :sub-title="$tl('p.goToDockerManagement')">
        <template #extra>
          <router-link to="/system/assets/docker-list">
            <a-button key="console" type="primary">{{ $tl('p.goToNow') }}</a-button></router-link
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
      :empty-description="$tl('p.noDocker')"
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
            :placeholder="$tl('c.name')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$tl('p.quickReturn')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button
            type="primary"
            :disabled="!tableSelections || !tableSelections.length"
            @click="syncToWorkspaceShow"
            >{{ $tl('p.workspaceSync') }}</a-button
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
            <a-tag v-if="record.machineDocker.status === 1" color="green">{{ $tl('p.statusNormal') }}</a-tag>
            <a-tooltip v-else :title="record.machineDocker.failureMsg">
              <a-tag color="red">{{ $tl('p.unableToConnect') }}</a-tag>
            </a-tooltip>
          </template>

          <a-tooltip v-else :title="$tl('p.dockerInfoLost')">
            <a-tag color="red">{{ $tl('p.infoLost') }}</a-tag>
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
              >{{ $tl('p.console') }}</a-button
            >
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.editDocker')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.containerName')" name="name">
          <a-input v-model:value="temp.name" :placeholder="$tl('c.containerName')" />
        </a-form-item>

        <a-form-item
          :label="$tl('c.label')"
          name="tagInput"
          :help="`${$tl('c.label')} 用于容器构建选择容器功能（fromTag）`"
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
              :placeholder="$tl('p.enterTagName')"
              @blur="handleInputConfirm"
              @keyup.enter="handleInputConfirm"
            />
            <template v-else>
              <a-tag
                v-if="!temp.tagsArray || temp.tagsArray.length < 10"
                style="borderstyle: dashed"
                @click="showInput"
              >
                <PlusOutlined /> {{ $tl('p.add') }}
              </a-tag>
            </template>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>

    <console
      v-if="consoleVisible"
      :id="temp.id"
      :visible="consoleVisible"
      url-prefix="/docker"
      @close="onClose"
    ></console>
    <!-- </a-drawer> -->
    <!-- 同步到其他工作空间 -->
    <a-modal
      v-model:open="syncToWorkspaceVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$tl('p.syncToOtherWorkspace')"
      :mask-closable="false"
      @ok="handleSyncToWorkspace"
    >
      <a-alert :message="$tl('p.warmTip')" type="warning">
        <template #description>
          <ul>
            <li>{{ $tl('p.syncMechanism') }}</li>
            <li>{{ $tl('p.createNewDocker') }}</li>
            <li>{{ $tl('p.syncDockerInfo') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item :label="$tl('p.chooseWorkspace')" name="workspaceId">
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
            :placeholder="$tl('c.selectWorkspace')"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id" :disabled="getWorkspaceId() === item.id">{{
              item.name
            }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
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
          title: this.$tl('c.name'),
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
          title: this.$tl('p.status'),
          dataIndex: ['machineDocker', 'status'],
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: `docker${this.$tl('p.version')}`,
          dataIndex: ['machineDocker', 'dockerVersion'],
          ellipsis: true,
          width: '120px',
          tooltip: true
        },
        {
          title: this.$tl('c.label'),
          dataIndex: 'tags',
          width: 100,
          ellipsis: true
        },
        {
          title: this.$tl('p.lastModifiedBy'),
          dataIndex: 'modifyUser',
          width: '120px',
          ellipsis: true
        },
        {
          title: this.$tl('p.creationTime'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.action'),
          dataIndex: 'operation',

          fixed: 'right',
          align: 'center',
          width: '190px'
        }
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: this.$tl('p.enterContainerName'), trigger: 'blur' }],
        host: [{ required: true, message: this.$tl('p.enterContainerAddress'), trigger: 'blur' }],
        tagInput: [
          // { required: true, message: "Please input ID", trigger: "blur" },
          { pattern: /^\w{1,10}$/, message: this.$tl('c.labelRestriction') }
        ],

        tag: [
          { required: true, message: this.$tl('p.enterAssociatedContainerTag'), trigger: 'blur' },
          { pattern: /^\w{1,10}$/, message: this.$tl('c.labelRestriction') }
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
    $tl(key, ...args) {
      return this.$t(`pages.docker.list.${key}`, ...args)
    },
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
        title: this.$tl('p.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmDeletion'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
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
          message: this.$tl('c.selectWorkspace')
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
