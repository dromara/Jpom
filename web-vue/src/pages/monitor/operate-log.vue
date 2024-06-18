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
            :placeholder="$t('i18n_f976e8fcf4')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('i18n_a4f5cae8d2')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('i18n_cc42dd3170') }}</a-select-option>
            <a-select-option :value="0">{{ $t('i18n_b15d91274e') }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
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
            :checked-children="$t('i18n_cc42dd3170')"
            :un-checked-children="$t('i18n_b15d91274e')"
          />
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editOperateMonitorVisible"
      v-model:open="editOperateMonitorVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="50vw"
      :title="$t('i18n_ebc2a1956b')"
      :mask-closable="false"
      @ok="handleEditOperateMonitorOk"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item :label="$t('i18n_f976e8fcf4')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_f976e8fcf4')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_a4f5cae8d2')" name="status">
          <a-switch
            v-model:checked="temp.start"
            :checked-children="$t('i18n_8493205602')"
            :un-checked-children="$t('i18n_d58a55bcee')"
          />
        </a-form-item>
        <a-form-item :label="$t('i18n_5e46f842d8')" name="monitorUser">
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
        <a-form-item :label="$t('i18n_5cb39287a8')" name="monitorOpt">
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
        <a-form-item :label="$t('i18n_3e7ef69c98')" name="monitorOpt">
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
              {{ $t('i18n_09723d428d') }}
              <template #title> {{ $t('i18n_067eb0fa04') }} </template>
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
    </CustomModal>
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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name'
        },
        {
          title: this.$t('i18n_a4f5cae8d2'),
          dataIndex: 'status'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser'
        },
        {
          title: this.$t('i18n_1303e638b5'),
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
          title: this.$t('i18n_2b6bc0f293'),
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
            message: this.$t('i18n_c68dc88c51'),
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
            message: this.$t('i18n_83c61f7f9e')
          })
          return false
        }
        if (this.methodFeatureKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_fabc07a4f1')
          })
          return false
        }
        if (this.classFeatureKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_c6e4cddba0')
          })
          return false
        }
        if (this.notifyUserKeys.length === 0) {
          $notification.error({
            message: this.$t('i18n_d02a9a85df')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_b63c057330'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
