<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter"></div> -->
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      @change="changePage"
      bordered
      :rowKey="(record, index) => index"
    >
      <template #title>
        <a-space>
          <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space></template
      >
      <template #operation slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      v-model="editVisible"
      width="60vw"
      title="编辑"
      @ok="handleEditUserOk"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="名称" />
        </a-form-item>
        <a-form-item prop="workspace">
          <template #label>
            工作空间
            <a-tooltip v-if="!temp.id">
              <template #title> 配置工作空间权限,用户限制用户只能对应的工作空间里面操作对应的功能</template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <transfer ref="transferRef" :tree-data="workspaceList" :editKey="temp.targetKeys" />
        </a-form-item>
        <a-form-item prop="prohibitExecute">
          <template #label>
            禁用时段
            <a-tooltip v-if="!temp.id">
              <template #title> 配置后可以控制想要在某个时间段禁止用户操作某些功能，优先判断禁用时段</template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <div v-for="(item, index) in temp.prohibitExecuteArray" :key="item.key">
            <div class="item-info">
              <div>
                <a-range-picker
                  style="width: 100%"
                  v-model="item.moments"
                  :disabled-date="
                    (current) => {
                      if (current < moment().subtract(1, 'days')) {
                        return true
                      }

                      return temp.prohibitExecuteArray.filter((arrayItem, arrayIndex) => {
                        if (arrayIndex === index) {
                          return false
                        }
                        if (arrayItem.moments && arrayItem.moments.length === 2) {
                          if (current > arrayItem.moments[0] && current < arrayItem.moments[1]) {
                            return true
                          }
                        }
                        return false
                      }).length
                    }
                  "
                  :show-time="{ format: 'HH:mm:ss' }"
                  format="YYYY-MM-DD HH:mm:ss"
                  valueFormat="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']"
                />
              </div>
              <div>
                <a-input v-model="item.reason" placeholder="禁用原因" allow-clear />
              </div>
            </div>
            <div
              class="item-icon"
              @click="
                () => {
                  temp.prohibitExecuteArray.splice(index, 1)
                }
              "
            >
              <a-icon type="minus-circle" style="color: #ff0000" />
            </div>
          </div>
          <a-button
            type="primary"
            @click="
              () => {
                temp.prohibitExecuteArray.push({})
              }
            "
            >添加</a-button
          >
        </a-form-item>
        <a-form-item prop="allowExecute">
          <template #label>
            允许时段
            <a-tooltip v-if="!temp.id">
              <template #title>
                优先判断禁用时段,再判断允许时段。配置允许时段后用户只能在对应的时段执行相应功能的操作</template
              >
              <question-circle-filled />
            </a-tooltip>
          </template>
          <div v-for="(item, index) in temp.allowExecuteArray" :key="item.key">
            <div class="item-info">
              <div>
                <a-select placeholder="请选择可以执行的星期" v-model="item.week" mode="multiple" style="width: 100%">
                  <a-select-option
                    v-for="weekItem in weeks"
                    :key="weekItem.value"
                    :disabled="
                      temp.allowExecuteArray.filter((arrayItem, arrayIndex) => {
                        if (arrayIndex === index) {
                          return false
                        }
                        return arrayItem.week && arrayItem.week.includes(weekItem.value)
                      }).length > 0
                    "
                  >
                    {{ weekItem.name }}
                  </a-select-option>
                </a-select>
              </div>
              <div>
                <a-space>
                  <a-time-picker
                    placeholder="开始时间"
                    v-model="item.startTime"
                    valueFormat="HH:mm:ss"
                    :default-open-value="moment('00:00:00', 'HH:mm:ss')"
                  />
                  <a-time-picker
                    placeholder="结束时间"
                    v-model="item.endTime"
                    valueFormat="HH:mm:ss"
                    :default-open-value="moment('23:59:59', 'HH:mm:ss')"
                  />
                </a-space>
              </div>
            </div>
            <div
              class="item-icon"
              @click="
                () => {
                  temp.allowExecuteArray.splice(index, 1)
                }
              "
            >
              <a-icon type="minus-circle" style="color: #ff0000" />
            </div>
          </div>
          <a-button
            type="primary"
            @click="
              () => {
                temp.allowExecuteArray.push({})
              }
            "
            >添加</a-button
          >
        </a-form-item>

        <a-form-item label="描述" prop="description">
          <a-input v-model="temp.description" :maxLength="200" type="textarea" :rows="5" placeholder="描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script>
import { workspaceList } from '@/api/user/user'
import { getList, editPermissionGroup, deletePermissionGroup } from '@/api/user/user-permission'
import { getWorkSpaceListAll } from '@/api/workspace'
import { getMonitorOperateTypeList } from '@/api/monitor'
import moment from 'moment'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import Transfer from '@/components/compositionTransfer/composition-transfer.vue'

export default {
  components: {
    Transfer
  },
  data() {
    return {
      loading: false,
      list: [],
      workspaceList: [],

      methodFeature: [],
      temp: {},
      weeks: [
        { value: 1, name: '周一' },
        { value: 2, name: '周二' },
        { value: 3, name: '周三' },
        { value: 4, name: '周四' },
        { value: 5, name: '周五' },
        { value: 6, name: '周六' },
        { value: 7, name: '周日' }
      ],
      editVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: '名称', dataIndex: 'name', ellipsis: true },
        { title: '描述', dataIndex: 'description', ellipsis: true },

        { title: '修改人', dataIndex: 'modifyUser', ellipsis: true, width: 150 },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text)
          },
          width: 170
        },
        {
          title: '操作',
          align: 'center',
          dataIndex: 'operation',
          scopedSlots: { customRender: 'operation' },
          width: 120
        }
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: '请输入权限组名称', trigger: 'blur' }]
      }
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
    moment,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      return new Promise((callback) => {
        this.workspaceList = []
        getWorkSpaceListAll().then((res) => {
          if (res.code === 200) {
            res.data.forEach((element) => {
              const children = this.methodFeature.map((item) => {
                return {
                  key: element.id + '-' + item.value,
                  title: item.title + '权限',
                  parentId: element.id
                }
              })
              children.push({ key: element.id + '-systemUser', title: '节点管理员', parentId: element.id })
              children.push({
                key: element.id + '-sshCommandNotLimited',
                title: 'SSH 终端命令无限制',
                parentId: element.id
              })
              this.workspaceList.push({
                key: element.id,
                title: element.name,
                children: children,
                parentId: 0
              })
            })
            callback()
          }
        })
      })
    },
    // 加载操作类型数据
    loadOptTypeData() {
      getMonitorOperateTypeList().then((res) => {
        if (res.code === 200) {
          this.methodFeature = res.data.methodFeature
        }
      })
    },

    // 新增权限组
    handleAdd() {
      this.temp = { prohibitExecuteArray: [], allowExecuteArray: [], targetKeys: [] }

      this.loadWorkSpaceListAll()
      this.editVisible = true
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
    },
    // 修改权限组
    handleEdit(record) {
      workspaceList(record.id).then((res) => {
        this.loadWorkSpaceListAll().then(() => {
          this.temp = {
            ...record,
            targetKeys: res.data.map((element) => {
              return element.workspaceId
            }),
            prohibitExecuteArray: JSON.parse(record.prohibitExecute).map((item) => {
              return {
                reason: item.reason,
                moments: [item.startTime, item.endTime]
              }
            }),
            allowExecuteArray: JSON.parse(record.allowExecute)
          }
          delete this.temp.prohibitExecute, delete this.temp.allowExecute
          this.editVisible = true
          console.log(this.temp)
        })
      })
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs['editForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        const transferRef = this.$refs.transferRef
        const emitKeys = transferRef && transferRef.emitKeys
        const temp = { ...this.temp }
        //
        temp.prohibitExecute = JSON.stringify(
          (temp.prohibitExecuteArray || []).map((item) => {
            return {
              startTime: item.moments && item.moments[0],
              endTime: item.moments && item.moments[1],
              reason: item.reason
            }
          })
        )
        delete temp.prohibitExecuteArray
        //
        temp.allowExecute = JSON.stringify(
          (temp.allowExecuteArray || []).map((item) => {
            return {
              endTime: item.endTime,
              startTime: item.startTime,
              week: item.week
            }
          })
        )
        delete temp.allowExecuteArray
        if (!emitKeys || emitKeys.length <= 0) {
          $notification.error({
            message: '请选择工作空间'
          })
          return false
        }
        //
        temp.workspace = JSON.stringify(emitKeys)
        console.log(temp, emitKeys)
        // 需要判断当前操作是【新增】还是【修改】
        editPermissionGroup(temp).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.$refs['editForm'].resetFields()
            this.editVisible = false
            this.loadData()
          }
        })
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: '系统提示',
        content: '真的要删除权限组么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deletePermissionGroup(record.id).then((res) => {
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
    checkTipUserName() {
      if (this.temp?.id === 'demo') {
        $confirm({
          title: '系统提示',
          content:
            'demo 账号是系统特定演示使用的账号,系统默认将对 demo 账号限制很多权限。非演示场景不建议使用 demo 账号',
          okText: '确认',
          cancelText: '取消',
          onOk: () => {},
          onCancel: () => {
            this.temp.id = ''
          }
        })
      }
    }
  }
}
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
} */
.item-info {
  display: inline-block;
  width: 90%;
}

.item-icon {
  display: inline-block;
  width: 10%;
  text-align: center;
}
</style>
