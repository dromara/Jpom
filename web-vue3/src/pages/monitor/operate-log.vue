<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      @change="changePage"
      bordered
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="监控名称"
            class="search-input-item"
          />
          <a-select v-model:value="listQuery.status" allowClear placeholder="开启状态" class="search-input-item">
            <a-select-option :value="1">开启</a-select-option>
            <a-select-option :value="0">关闭</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch size="small" :checked="text" checked-children="开启" un-checked-children="关闭" />
        </template>
        <!-- <a-switch slot="autoRestart" slot-scope="text" :checked="text" checked-children="是" un-checked-children="否" /> -->
        <!-- <a-switch slot="alarm" slot-scope="text" :checked="text" disabled checked-children="报警中" un-checked-children="未报警" /> -->

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editOperateMonitorVisible"
      width="50vw"
      title="编辑监控"
      @ok="handleEditOperateMonitorOk"
      :maskClosable="false"
    >
      <a-form ref="editMonitorForm" :rules="rules" :model="temp" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="监控名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="监控名称" />
        </a-form-item>
        <a-form-item label="开启状态" name="status">
          <a-switch v-model:checked="temp.start" checked-children="开" un-checked-children="关" />
        </a-form-item>
        <a-form-item label="监控用户" name="monitorUser">
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
        <a-form-item label="监控功能" name="monitorOpt">
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
        <a-form-item label="监控操作" name="monitorOpt">
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
          <template v-slot:label>
            <a-tooltip>
              报警联系人
              <template v-slot:title>
                如果这里的报警联系人无法选择，说明这里面的管理员没有设置邮箱，在右上角下拉菜单里面的用户资料里可以设置。
              </template>
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
          title: '名称',
          dataIndex: 'name'
        },
        {
          title: '开启状态',
          dataIndex: 'status'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser'
        },
        {
          title: '修改时间',
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
          title: '操作',
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
            message: '请输入监控名称',
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
          this.$notification.error({
            message: '请选择监控用户'
          })
          return false
        }
        if (this.methodFeatureKeys.length === 0) {
          this.$notification.error({
            message: '请选择监控操作'
          })
          return false
        }
        if (this.classFeatureKeys.length === 0) {
          this.$notification.error({
            message: '请选择监控的功能'
          })
          return false
        }
        if (this.notifyUserKeys.length === 0) {
          this.$notification.error({
            message: '请选择报警联系人'
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
              this.$notification.success({
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
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除操作监控么？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteMonitorOperate(record.id)
              .then((res) => {
                if (res.code === 200) {
                  this.$notification.success({
                    message: res.msg
                  })
                  this.loadData()
                }
                resolve()
              })
              .catch(reject)
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
