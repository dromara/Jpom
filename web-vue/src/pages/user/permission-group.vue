<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('c.name')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$tl('p.quickBack')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('c.new') }}</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('c.edit') }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{ $tl('p.delete') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$tl('c.edit')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.name')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.name')" />
        </a-form-item>
        <a-form-item name="workspace">
          <template #label>
            <a-tooltip>
              {{ $tl('p.workspace') }}
              <template #title> {{ $tl('p.configWorkspacePermission') }},{{ $tl('p.userRestriction') }}</template>
              <QuestionCircleOutlined v-if="!temp.id" />
            </a-tooltip>
          </template>
          <transfer ref="transferRef" :tree-data="workspaceList" :edit-key="temp.targetKeys" />
        </a-form-item>
        <a-form-item name="prohibitExecute">
          <template #label>
            <a-tooltip>
              {{ $tl('p.disablePeriod') }}
              <template #title> {{ $tl('p.disableControl') }}</template>
              <QuestionCircleOutlined v-if="!temp.id" />
            </a-tooltip>
          </template>
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <div v-for="(item, index) in temp.prohibitExecuteArray" :key="item.key">
                <a-space direction="vertical" class="item-info" style="width: 100%">
                  <a-range-picker
                    v-model:value="item.moments"
                    style="width: 100%"
                    :disabled-date="
                      (current) => {
                        if (current < dayjs().subtract(1, 'days')) {
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
                    value-format="YYYY-MM-DD HH:mm:ss"
                    :placeholder="[$tl('c.startTime'), $tl('c.endTime')]"
                  />

                  <div>
                    <a-input v-model:value="item.reason" :placeholder="$tl('p.disableReason')" allow-clear />
                  </div>
                </a-space>

                <div
                  class="item-icon"
                  @click="
                    () => {
                      temp.prohibitExecuteArray.splice(index, 1)
                    }
                  "
                >
                  <MinusCircleOutlined style="color: #ff0000" />
                </div>
              </div>
            </a-space>
            <a-button
              type="primary"
              @click="
                () => {
                  temp.prohibitExecuteArray.push({})
                }
              "
              >{{ $tl('c.new') }}
            </a-button>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item name="allowExecute">
          <template #label>
            <a-tooltip>
              {{ $tl('p.allowPeriod') }}
              <template #title> {{ $tl('p.priorityDisable') }},{{ $tl('p.allowOperation') }} </template>
              <QuestionCircleOutlined v-if="!temp.id" />
            </a-tooltip>
          </template>
          <a-form-item-rest>
            <a-space direction="vertical" style="width: 100%">
              <div v-for="(item, index) in temp.allowExecuteArray" :key="item.key">
                <a-space direction="vertical" class="item-info" style="width: 100%">
                  <div>
                    <a-select
                      v-model:value="item.week"
                      :placeholder="$tl('p.selectableWeekdays')"
                      mode="multiple"
                      style="width: 100%"
                    >
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
                        v-model:value="item.startTime"
                        :placeholder="$tl('c.startTime')"
                        value-format="HH:mm:ss"
                      />
                      <a-time-picker
                        v-model:value="item.endTime"
                        :placeholder="$tl('c.endTime')"
                        value-format="HH:mm:ss"
                      />
                    </a-space>
                  </div>
                </a-space>
                <div
                  class="item-icon"
                  @click="
                    () => {
                      temp.allowExecuteArray.splice(index, 1)
                    }
                  "
                >
                  <MinusCircleOutlined style="color: #ff0000" />
                </div>
              </div>
            </a-space>
            <a-button
              type="primary"
              @click="
                () => {
                  temp.allowExecuteArray.push({})
                }
              "
              >{{ $tl('c.new') }}
            </a-button>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$tl('c.description')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="5"
            :placeholder="$tl('c.description')"
          />
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
import dayjs from 'dayjs'
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
        { value: 1, name: this.$tl('p.monday') },
        { value: 2, name: this.$tl('p.tuesday') },
        { value: 3, name: this.$tl('p.wednesday') },
        { value: 4, name: this.$tl('p.thursday') },
        { value: 5, name: this.$tl('p.friday') },
        { value: 6, name: this.$tl('p.saturday') },
        { value: 7, name: this.$tl('p.sunday') }
      ],
      editVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: 'id', dataIndex: 'id', ellipsis: true },
        { title: this.$tl('c.name'), dataIndex: 'name', ellipsis: true },
        { title: this.$tl('c.description'), dataIndex: 'description', ellipsis: true },

        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: 170
        },
        {
          title: this.$tl('p.operation'),
          align: 'center',
          dataIndex: 'operation',

          width: 120
        }
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$tl('p.permissionGroupName'), trigger: 'blur' }]
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
      return this.$t(`pages.user.permissionGroup.${key}`, ...args)
    },
    dayjs,
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
                  title: item.title + this.$tl('p.permission'),
                  parentId: element.id
                }
              })
              children.push({
                key: element.id + '-sshCommandNotLimited',
                title: `SSH ${this.$tl('p.terminalUnlimited')}`,
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
      this.temp = {
        prohibitExecuteArray: [],
        allowExecuteArray: [],
        targetKeys: []
      }

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
        })
      })
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
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
            message: this.$tl('p.selectWorkspace')
          })
          return false
        }
        //
        temp.workspace = JSON.stringify(emitKeys)
        delete temp.targetKeys
        // console.log(temp, emitKeys)
        // 需要判断当前操作是【新增】还是【修改】
        this.confirmLoading = true
        editPermissionGroup(temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.$refs['editForm'].resetFields()
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
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: this.$tl('p.confirmDelete'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deletePermissionGroup(record.id).then((res) => {
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
          title: this.$tl('c.systemTip'),
          zIndex: 1009,
          content: `demo ${this.$tl('p.demoAccountTip')},${this.$tl('p.demoAccountRestriction')}`,
          okText: this.$tl('c.confirm'),
          cancelText: this.$tl('c.cancel'),

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
.item-info {
  /* display: inline-block; */
  width: 90%;
}
.item-icon {
  display: inline-block;
  width: 10%;
  text-align: center;
}
</style>
