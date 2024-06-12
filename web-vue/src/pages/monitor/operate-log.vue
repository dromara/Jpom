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
            :placeholder="$t('pages.monitor.operate-log.102669ac')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('pages.monitor.operate-log.6e06fe4f')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.monitor.operate-log.726bd72c') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.monitor.operate-log.8ca0a6dc') }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.monitor.operate-log.5da9e22d')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.monitor.operate-log.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.monitor.operate-log.7d46652a') }}</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            size="small"
            :checked="text"
            :checked-children="$t('pages.monitor.operate-log.726bd72c')"
            :un-checked-children="$t('pages.monitor.operate-log.8ca0a6dc')"
          />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.monitor.operate-log.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.monitor.operate-log.dd20d11c')
            }}</a-button>
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
      :title="$t('pages.monitor.operate-log.d6117b62')"
      :mask-closable="false"
      @ok="handleEditOperateMonitorOk"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item :label="$t('pages.monitor.operate-log.102669ac')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.monitor.operate-log.102669ac')" />
        </a-form-item>
        <a-form-item :label="$t('pages.monitor.operate-log.6e06fe4f')" name="status">
          <a-switch
            v-model:checked="temp.start"
            :checked-children="$t('pages.monitor.operate-log.412561bf')"
            :un-checked-children="$t('pages.monitor.operate-log.99811dca')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.monitor.operate-log.ca5b0bf7')" name="monitorUser">
          <a-transfer
            :data-source="monitorUserList"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="monitorUserKeys"
            @change="handleMonitorUserChange"
          >
            <template #render="item">
              <a-tooltip :title="item.title">{{ item.title }} </a-tooltip>
            </template>
          </a-transfer>
        </a-form-item>
        <a-form-item :label="$t('pages.monitor.operate-log.59c90082')" name="monitorOpt">
          <a-transfer
            :data-source="classFeature"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="classFeatureKeys"
            @change="handleClassFeatureChange"
          >
            <template #render="item">
              <a-tooltip :title="item.title">{{ item.title }} </a-tooltip>
            </template>
          </a-transfer>
        </a-form-item>
        <a-form-item :label="$t('pages.monitor.operate-log.42cc6f66')" name="monitorOpt">
          <a-transfer
            :data-source="methodFeature"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="methodFeatureKeys"
            @change="handleMethodFeatureChange"
          >
            <template #render="item">
              <a-tooltip :title="item.title">{{ item.title }} </a-tooltip>
            </template>
          </a-transfer>
        </a-form-item>
        <a-form-item name="notifyUser" class="jpom-monitor-notify">
          <template #label>
            <a-tooltip>
              {{ $t('pages.monitor.operate-log.2e5898f2') }}
              <template #title> {{ $t('pages.monitor.operate-log.4bff4306') }} </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-transfer
            :data-source="userList"
            :lazy="false"
            show-search
            :filter-option="filterOption"
            :target-keys="notifyUserKeys"
            @change="handleNotifyUserChange"
          >
            <template #render="item">
              <a-tooltip :title="item.title">{{ item.title }} </a-tooltip>
            </template>
          </a-transfer>
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
          title: this.$t('pages.monitor.operate-log.bb769c1d'),
          dataIndex: 'name'
        },
        {
          title: this.$t('pages.monitor.operate-log.6e06fe4f'),
          dataIndex: 'status'
        },
        {
          title: this.$t('pages.monitor.operate-log.916db24b'),
          dataIndex: 'modifyUser'
        },
        {
          title: this.$t('pages.monitor.operate-log.fd921623'),
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
          title: this.$t('pages.monitor.operate-log.3bb962bf'),
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
            message: this.$t('pages.monitor.operate-log.a530a71d'),
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
            message: this.$t('pages.monitor.operate-log.328111cb')
          })
          return false
        }
        if (this.methodFeatureKeys.length === 0) {
          $notification.error({
            message: this.$t('pages.monitor.operate-log.45c25649')
          })
          return false
        }
        if (this.classFeatureKeys.length === 0) {
          $notification.error({
            message: this.$t('pages.monitor.operate-log.3a343686')
          })
          return false
        }
        if (this.notifyUserKeys.length === 0) {
          $notification.error({
            message: this.$t('pages.monitor.operate-log.1cf3e6f')
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
        title: this.$t('pages.monitor.operate-log.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.monitor.operate-log.987c2cd6'),
        okText: this.$t('pages.monitor.operate-log.e8e9db25'),
        cancelText: this.$t('pages.monitor.operate-log.b12468e9'),
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
