<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserList"
      :empty-description="$tl('p.noUser')"
      :loading="loading"
      :data-source="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
      @refresh="loadData"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery.id"
            :placeholder="$tl('p.userId')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$tl('p.username')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$tl('p.firstPageTip')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.add') }}</a-button>
          <a-button type="primary" @click="systemNotificationOpen = true">{{ $tl('p.publishNotice') }}</a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()"> {{ $tl('p.more') }} <DownOutlined /> </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="handleDelete(record)"
                      >{{ $tl('p.delete') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.pwdErrorCount === 0"
                      @click="handleUnlock(record)"
                      >{{ $tl('p.unlock') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="restUserPwdHander(record)"
                      >{{ $tl('p.resetPassword') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.twoFactorAuthKey ? false : true"
                      @click="handleCloseMfa(record)"
                      >{{ $tl('p.closeMfa') }}</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
        <template v-else-if="column.dataIndex === 'systemUser'">
          <a-switch
            size="small"
            :checked-children="$tl('c.isEnable')"
            :un-checked-children="$tl('c.isDisable')"
            disabled
            :checked="record.systemUser == 1"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            size="small"
            :checked-children="$tl('c.enable')"
            :un-checked-children="$tl('c.disable')"
            disabled
            :checked="record.status != 0"
          />
        </template>

        <template v-else-if="column.dataIndex === 'twoFactorAuthKey'">
          <a-switch
            size="small"
            :checked-children="$tl('p.on')"
            :un-checked-children="$tl('p.off')"
            disabled
            :checked="record.twoFactorAuthKey ? true : false"
          />
        </template>

        <template v-else-if="column.dataIndex === 'id'">
          <a-tooltip :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'email'">
          <a-tooltip :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
      </template>
    </CustomTable>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editUserVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$tl('p.editUser')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <a-alert
        v-if="!permissionGroup || !permissionGroup.length"
        :message="$tl('p.remind')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $tl('p.noPermissionGroup') }}</template>
      </a-alert>
      <a-form ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('p.loginName')" name="id">
          <a-input
            v-model:value="temp.id"
            :max-length="50"
            :placeholder="$tl('p.loginNameTip')"
            :disabled="createOption == false"
            @change="checkTipUserName"
          />
        </a-form-item>

        <a-form-item :label="$tl('c.nickname')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.nickname')" />
        </a-form-item>
        <a-form-item name="systemUser">
          <template #label>
            <a-tooltip>
              {{ $tl('c.admin') }}
              <template #title> {{ $tl('c.adminDescription') }} </template>
              <QuestionCircleOutlined v-if="createOption" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-tooltip :title="$tl('c.adminDescription')">
                <a-switch
                  :checked="temp.systemUser == 1"
                  :disabled="temp.parent === 'sys'"
                  :checked-children="$tl('c.isEnable')"
                  :un-checked-children="$tl('c.isDisable')"
                  default-checked
                  @change="
                    (checked) => {
                      temp.systemUser = checked ? 1 : 0
                    }
                  "
                />
              </a-tooltip>
            </a-col>
            <a-col :span="4" style="text-align: right">
              <a-tooltip>
                <template #title> {{ $tl('p.disableUserTip') }} </template>
                <QuestionCircleOutlined v-if="createOption" />
                {{ $tl('p.statusLabel') }}
              </a-tooltip>
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch
                  :checked="temp.status != 0"
                  :disabled="temp.parent === 'sys'"
                  :checked-children="$tl('c.enable')"
                  :un-checked-children="$tl('c.disable')"
                  default-checked
                  @change="
                    (checked) => {
                      temp.status = checked ? 1 : 0
                    }
                  "
                />
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item :label="$tl('p.permissionGroup')" name="permissionGroup">
          <a-select
            v-model:value="temp.permissionGroup"
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
            :placeholder="$tl('p.selectPermissionGroup')"
            mode="multiple"
          >
            <a-select-option v-for="item in permissionGroup" :key="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal
      v-model:open="showUserPwd"
      destroy-on-close
      :title="$tl('p.passwordTip')"
      :mask-closable="false"
      :footer="null"
    >
      <a-result status="success" :title="temp.title">
        <template #subTitle>
          <div>
            {{ $tl('p.newPassword') }}
            <a-typography-paragraph :copyable="{ tooltip: false, text: temp.randomPwd }">
              <b style="color: red; font-size: 20px">
                {{ temp.randomPwd }}
              </b>
            </a-typography-paragraph>
            {{ $tl('p.informUser') }}
          </div>
          <div style="color: red">{{ $tl('p.passwordViewTip') }}</div>
        </template>
      </a-result>
    </a-modal>
    <!-- 系统公告  -->
    <a-modal
      v-model:open="systemNotificationOpen"
      destroy-on-close
      :title="$tl('p.configNotice')"
      :mask-closable="false"
      width="50vw"
      :footer="null"
    >
      <notification />
    </a-modal>
  </div>
</template>

<script>
import { closeUserMfa, deleteUser, editUser, getUserList, unlockUser, restUserPwd } from '@/api/user/user'
import { getUserPermissionListAll } from '@/api/user/user-permission'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import notification from './notification.vue'
export default {
  components: {
    notification
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},

      createOption: true,
      editUserVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      permissionGroup: [],
      columns: [
        {
          title: 'ID',
          dataIndex: 'id',
          ellipsis: true,
          width: 100
        },
        { title: this.$tl('c.nickname'), dataIndex: 'name', ellipsis: true, width: 100 },
        {
          title: this.$tl('c.admin'),
          dataIndex: 'systemUser',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$tl('p.status'),
          dataIndex: 'status',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$tl('p.twoStepVerification'),
          dataIndex: 'twoFactorAuthKey',
          align: 'center',
          ellipsis: true,
          width: 90
        },

        {
          title: this.$tl('p.email'),
          dataIndex: 'email',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$tl('p.source'),
          dataIndex: 'source',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$tl('p.loginFail'),
          dataIndex: 'pwdErrorCount',
          ellipsis: true,
          width: 90
        },
        { title: this.$tl('p.creator'), dataIndex: 'parent', ellipsis: true, width: 150 },

        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, record }) => {
            return parseTime(text || record.optTime)
          },
          width: '170px'
        },
        {
          title: this.$tl('p.operation'),
          align: 'center',
          dataIndex: 'operation',
          fixed: 'right',
          width: '120px'
        }
      ],
      // 表单校验规则
      rules: {
        id: [{ required: true, message: this.$tl('p.fillAccount'), trigger: 'blur' }],
        name: [{ required: true, message: this.$tl('p.fillNickname'), trigger: 'blur' }],
        permissionGroup: [{ required: true, message: this.$tl('p.selectPermission'), trigger: 'blur' }]
      },
      showUserPwd: false,
      confirmLoading: false,
      systemNotificationOpen: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  watch: {},
  created() {
    this.loadData()
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.user.index.${key}`, ...args)
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getUserList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
          }
        })
        .finally(() => {
          this.loading = false
        })
    },

    // 新增用户
    handleAdd() {
      this.temp = { systemUser: 0 }
      this.createOption = true
      this.listUserPermissionListAll()
      this.editUserVisible = true
      this.$refs['editUserForm'] && this.$refs['editUserForm'].resetFields()
    },
    //
    listUserPermissionListAll() {
      getUserPermissionListAll().then((res) => {
        if (res.code === 200 && res.data) {
          this.permissionGroup = res.data
        }
        if (!this.permissionGroup || this.permissionGroup.length <= 0)
          $notification.warn({
            message: this.$tl('p.noPermissionCreateUser')
          })
      })
    },
    // 修改用户
    handleEdit(record) {
      this.createOption = false
      this.temp = {
        ...record,
        permissionGroup: (record.permissionGroup || '').split('@').filter((item) => item),
        status: record.status === undefined ? 1 : record.status
      }
      this.listUserPermissionListAll()
      this.editUserVisible = true
      this.$refs['editUserForm'] && this.$refs['editUserForm'].resetFields()
    },
    // 提交用户数据
    handleEditUserOk() {
      // 检验表单
      this.$refs['editUserForm'].validate().then(() => {
        const paramsTemp = Object.assign({}, this.temp)

        paramsTemp.type = this.createOption ? 'add' : 'edit'
        paramsTemp.permissionGroup = (paramsTemp.permissionGroup || []).join('@')

        // 需要判断当前操作是【新增】还是【修改】
        this.confirmLoading = true
        editUser(paramsTemp)
          .then((res) => {
            if (res.code === 200) {
              if (paramsTemp.type === 'add') {
                this.temp = {
                  title: this.$tl('p.accountAdded'),
                  randomPwd: res.data.randomPwd
                }

                this.showUserPwd = true
              } else {
                $notification.success({
                  message: res.msg
                })
              }

              this.editUserVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除用户
    handleDelete(record) {
      $confirm({
        title: this.$tl('c.systemTip'),
        content: this.$tl('p.confirmDeleteUser'),
        zIndex: 1009,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return deleteUser(record.id).then((res) => {
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
    // 解锁
    handleUnlock(record) {
      $confirm({
        title: this.$tl('c.systemTip'),
        content: this.$tl('p.confirmUnlockUser'),
        zIndex: 1009,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return unlockUser(record.id).then((res) => {
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
    //
    handleCloseMfa(record) {
      $confirm({
        title: this.$tl('c.systemTip'),
        content: this.$tl('p.confirmCloseMfa'),
        zIndex: 1009,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return closeUserMfa(record.id).then((res) => {
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
    //
    checkTipUserName() {
      if (this.temp?.id === 'demo') {
        $confirm({
          title: this.$tl('c.systemTip'),
          zIndex: 1009,
          content: `demo ${this.$tl('p.demoAccountTip')},${this.$tl('p.demoAccountLimit')}`,
          okText: this.$tl('c.confirm'),
          cancelText: this.$tl('c.cancel'),

          onCancel: () => {
            this.temp.id = ''
          }
        })
      }
    },
    //
    restUserPwdHander(record) {
      $confirm({
        title: this.$tl('c.systemTip'),
        zIndex: 1009,
        content: this.$tl('p.confirmResetPassword'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return restUserPwd(record.id).then((res) => {
            if (res.code === 200) {
              this.temp = {
                title: this.$tl('p.passwordResetSuccess'),
                randomPwd: res.data.randomPwd
              }
              this.showUserPwd = true
            }
          })
        }
      })
    }
  }
}
</script>

<style scoped>
/* .filter {
  margin-bottom: 10px;
} */
</style>
