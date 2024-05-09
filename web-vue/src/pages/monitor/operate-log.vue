<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.monitorName')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$tl('c.status')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.on') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.off') }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.quickReturn')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.add') }}</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch size="small" :checked="text" :checked-children="$tl('c.on')" :un-checked-children="$tl('c.off')" />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editOperateMonitorVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50vw"
      :title="$tl('p.editMonitor')"
      :mask-closable="false"
      @ok="handleEditOperateMonitorOk"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item :label="$tl('c.monitorName')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.monitorName')" />
        </a-form-item>
        <a-form-item :label="$tl('c.status')" name="status">
          <a-switch
            v-model:checked="temp.start"
            :checked-children="$tl('p.open')"
            :un-checked-children="$tl('p.close')"
          />
        </a-form-item>
        <a-form-item :label="$tl('p.monitorUser')" name="monitorUser">
          <a-transfer
            :data-source="monitorUserList"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="monitorUserKeys"
            :render="(item) => item.title"
            @change="handleMonitorUserChange"
          />
        </a-form-item>
        <a-form-item :label="$tl('p.monitorFunction')" name="monitorOpt">
          <a-transfer
            :data-source="classFeature"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="classFeatureKeys"
            :render="(item) => item.title"
            @change="handleClassFeatureChange"
          />
        </a-form-item>
        <a-form-item :label="$tl('p.monitorOperation')" name="monitorOpt">
          <a-transfer
            :data-source="methodFeature"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="methodFeatureKeys"
            :render="(item) => item.title"
            @change="handleMethodFeatureChange"
          />
        </a-form-item>
        <a-form-item name="notifyUser" class="jpom-monitor-notify">
          <template #label>
            <a-tooltip>
              {{ $tl('p.alarmContact') }}
              <template #title> {{ $tl('p.noEmailSet') }} </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="notifyUserKeys"
            :render="(item) => item.title"
            @change="handleNotifyUserChange"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import {
  deleteMonitorOperate,
  editMonitorOperate,
  getMonitorOperateLogList,
  getMonitorOperateTypeList
} from '@/api/monitor'
import { getUserListAll } from '@/api/user/user'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],

      classFeature: [],
      methodFeature: [],
      userList: [],
      monitorUserList: [],
      temp: {},
      notifyUserKeys: [],
      monitorUserKeys: [],
      classFeatureKeys: [],
      methodFeatureKeys: [],
      editOperateMonitorVisible: false,
      columns: [
        {
          title: this.$tl('p.name'),
          dataIndex: 'name'
        },
        {
          title: this.$tl('c.status'),
          dataIndex: 'status'
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser'
        },
        {
          title: this.$tl('p.modifiedTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            if (!text || text === '0') {
              return ''
            }
            return parseTime(text)
          },
          width: 180
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          align: 'center',
          fixed: 'right',
          width: '120px'
        }
      ],
      rules: {
        name: [
          {
            required: true,
            message: this.$tl('p.pleaseInputMonitorName'),
            trigger: 'blur'
          }
        ]
      },
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  watch: {},
  created() {
    this.loadData()
    this.loadOptTypeData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.monitor.operateLog.${key}`, ...args)
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getMonitorOperateLogList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载操作类型数据
    loadOptTypeData() {
      // this.optTypeList = [];
      getMonitorOperateTypeList().then((res) => {
        if (res.code === 200) {
          this.methodFeature = res.data.methodFeature.map((element) => {
            return { key: element.value, title: element.title, disabled: false }
          })
          this.classFeature = res.data.classFeature.map((element) => {
            return { key: element.value, title: element.title, disabled: false }
          })
        }
      })
    },
    // 加载用户列表
    loadUserList() {
      // this.userList = [];
      getUserListAll().then((res) => {
        if (res.code === 200) {
          // res.data.forEach((element) => {
          //   this.userList.push({ key: element.value, title: element.title, disabled: element.disabled || false });
          // });
          this.userList = res.data.map((element) => {
            let canUse = element.email || element.dingDing || element.workWx
            return { key: element.id, title: element.name, disabled: !canUse }
          })
          this.monitorUserList = res.data.map((element) => {
            return { key: element.id, title: element.name }
          })
        }
      })
    },
    // 新增
    handleAdd() {
      this.temp = {
        start: false
      }
      this.notifyUserKeys = []
      this.classFeatureKeys = []
      this.methodFeatureKeys = []
      this.monitorUserKeys = []
      this.loadUserList()
      this.editOperateMonitorVisible = true
    },
    // 修改
    handleEdit(record) {
      this.loadUserList()
      this.temp = Object.assign({}, record)
      this.temp = {
        ...this.temp,
        start: this.temp.status
      }
      this.notifyUserKeys = JSON.parse(this.temp.notifyUser)
      this.classFeatureKeys = JSON.parse(this.temp.monitorFeature)
      this.methodFeatureKeys = JSON.parse(this.temp.monitorOpt)
      this.monitorUserKeys = JSON.parse(this.temp.monitorUser)
      this.editOperateMonitorVisible = true
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleNotifyUserChange(targetKeys) {
      this.notifyUserKeys = targetKeys
    },
    // 穿梭框 change
    handleMethodFeatureChange(targetKeys) {
      this.methodFeatureKeys = targetKeys
    },
    handleClassFeatureChange(targetKeys) {
      this.classFeatureKeys = targetKeys
    },

    // 穿梭框 change
    handleMonitorUserChange(targetKeys) {
      this.monitorUserKeys = targetKeys
    },
    // 提交
    handleEditOperateMonitorOk() {
      // 检验表单
      this.$refs['editMonitorForm'].validate().then(() => {
        if (this.monitorUserKeys.length === 0) {
          $notification.error({
            message: this.$tl('p.pleaseSelectMonitorUser')
          })
          return false
        }
        if (this.methodFeatureKeys.length === 0) {
          $notification.error({
            message: this.$tl('p.pleaseSelectMonitorOperation')
          })
          return false
        }
        if (this.classFeatureKeys.length === 0) {
          $notification.error({
            message: this.$tl('p.pleaseSelectMonitorFunction')
          })
          return false
        }
        if (this.notifyUserKeys.length === 0) {
          $notification.error({
            message: this.$tl('p.pleaseSelectAlarmContact')
          })
          return false
        }
        // 设置参数
        this.temp.monitorUser = JSON.stringify(this.monitorUserKeys)
        this.temp.monitorOpt = JSON.stringify(this.methodFeatureKeys)
        this.temp.monitorFeature = JSON.stringify(this.classFeatureKeys)
        this.temp.notifyUser = JSON.stringify(this.notifyUserKeys)
        this.temp.start ? (this.temp.status = 'on') : (this.temp.status = 'no')
        this.confirmLoading = false
        editMonitorOperate(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editMonitorForm'].resetFields()
              this.editOperateMonitorVisible = false
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
        onOk: () => {
          return deleteMonitorOperate(record.id).then((res) => {
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
    }
  }
}
</script>
