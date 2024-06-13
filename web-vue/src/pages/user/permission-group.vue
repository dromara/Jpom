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
            :placeholder="$t('pages.user.permission-group.3e34ec28')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('pages.user.permission-group.554d1b95')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.user.permission-group.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.user.permission-group.ccffbfda') }}</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.user.permission-group.e1224c34')
            }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('pages.user.permission-group.dd20d11c')
            }}</a-button>
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
      :title="$t('pages.user.permission-group.e1224c34')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.user.permission-group.3e34ec28')" name="name">
          <a-input
            v-model:value="temp.name"
            :max-length="50"
            :placeholder="$t('pages.user.permission-group.3e34ec28')"
          />
        </a-form-item>
        <a-form-item name="workspace">
          <template #label>
            <a-tooltip>
              {{ $t('pages.user.permission-group.afacc4cb') }}
              <template #title>
                {{ $t('pages.user.permission-group.bd82e6b0') }},{{
                  $t('pages.user.permission-group.751c9372')
                }}</template
              >
              <QuestionCircleOutlined v-if="!temp.id" />
            </a-tooltip>
          </template>
          <transfer ref="transferRef" :tree-data="workspaceList" :edit-key="temp.targetKeys" />
        </a-form-item>
        <a-form-item name="prohibitExecute">
          <template #label>
            <a-tooltip>
              {{ $t('pages.user.permission-group.aaad8c31') }}
              <template #title> {{ $t('pages.user.permission-group.c0b6d654') }}</template>
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
                    :placeholder="[
                      $t('pages.user.permission-group.9b8a573f'),
                      $t('pages.user.permission-group.63bed29')
                    ]"
                  />

                  <div>
                    <a-input
                      v-model:value="item.reason"
                      :placeholder="$t('pages.user.permission-group.54ad1af3')"
                      allow-clear
                    />
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
              >{{ $t('pages.user.permission-group.ccffbfda') }}
            </a-button>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item name="allowExecute">
          <template #label>
            <a-tooltip>
              {{ $t('pages.user.permission-group.f7840bbf') }}
              <template #title>
                {{ $t('pages.user.permission-group.cc634bca') }},{{ $t('pages.user.permission-group.54683eab') }}
              </template>
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
                      :placeholder="$t('pages.user.permission-group.2f160681')"
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
                        :placeholder="$t('pages.user.permission-group.9b8a573f')"
                        value-format="HH:mm:ss"
                      />
                      <a-time-picker
                        v-model:value="item.endTime"
                        :placeholder="$t('pages.user.permission-group.63bed29')"
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
              >{{ $t('pages.user.permission-group.ccffbfda') }}
            </a-button>
          </a-form-item-rest>
        </a-form-item>

        <a-form-item :label="$t('pages.user.permission-group.4b2e093e')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="5"
            :placeholder="$t('pages.user.permission-group.4b2e093e')"
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
        { value: 1, name: this.$t('pages.user.permission-group.38e08092') },
        { value: 2, name: this.$t('pages.user.permission-group.8b27c898') },
        { value: 3, name: this.$t('pages.user.permission-group.b746d1ae') },
        { value: 4, name: this.$t('pages.user.permission-group.7425f91a') },
        { value: 5, name: this.$t('pages.user.permission-group.57801bdb') },
        { value: 6, name: this.$t('pages.user.permission-group.2ab8dd00') },
        { value: 7, name: this.$t('pages.user.permission-group.2b6c6c5a') }
      ],

      editVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: 'id', dataIndex: 'id', ellipsis: true },
        { title: this.$t('pages.user.permission-group.3e34ec28'), dataIndex: 'name', ellipsis: true },
        { title: this.$t('pages.user.permission-group.4b2e093e'), dataIndex: 'description', ellipsis: true },

        {
          title: this.$t('pages.user.permission-group.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          width: 150
        },
        {
          title: this.$t('pages.user.permission-group.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: 170
        },
        {
          title: this.$t('pages.user.permission-group.3bb962bf'),
          align: 'center',
          dataIndex: 'operation',

          width: 120
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('pages.user.permission-group.d9245f83'), trigger: 'blur' }]
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
                  title: item.title + this.$t('pages.user.permission-group.88b61996'),
                  parentId: element.id
                }
              })
              children.push({
                key: element.id + '-sshCommandNotLimited',
                title: `SSH ${this.$t('pages.user.permission-group.8d1812cd')}`,
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
            message: this.$t('pages.user.permission-group.7ef9d8fb')
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
        title: this.$t('pages.user.permission-group.d3367221'),
        zIndex: 1009,
        content: this.$t('pages.user.permission-group.987c2cd6'),
        okText: this.$t('pages.user.permission-group.7da4a591'),
        cancelText: this.$t('pages.user.permission-group.43105e21'),
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
          title: this.$t('pages.user.permission-group.d3367221'),
          zIndex: 1009,
          content: `demo ${this.$t('pages.user.permission-group.8424b740')},${this.$t(
            'pages.user.permission-group.f382dae4'
          )}`,
          okText: this.$t('pages.user.permission-group.7da4a591'),
          cancelText: this.$t('pages.user.permission-group.43105e21'),

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
