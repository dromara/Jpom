<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      @change="changePage"
      bordered
      rowKey="id"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery.id"
            @pressEnter="loadData"
            placeholder="用户名ID"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="用户名"
            class="search-input-item"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
        </a-space></template
      >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()"> 更多 <DownOutlined /> </a>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="handleDelete(record)"
                      >删除</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.pwdErrorCount === 0"
                      @click="handleUnlock(record)"
                      >解锁</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="restUserPwdHander(record)"
                      >重置密码</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.twoFactorAuthKey ? false : true"
                      @click="handleCloseMfa(record)"
                      >关闭MFA</a-button
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
            checked-children="是"
            un-checked-children="否"
            disabled
            :checked="record.systemUser == 1"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            size="small"
            checked-children="启用"
            un-checked-children="禁用"
            disabled
            :checked="record.status != 0"
          />
        </template>

        <template v-else-if="column.dataIndex === 'twoFactorAuthKey'">
          <a-switch
            size="small"
            checked-children="开"
            un-checked-children="关"
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
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editUserVisible"
      width="60vw"
      title="编辑用户"
      @ok="handleEditUserOk"
      :maskClosable="false"
    >
      <a-alert
        v-if="!permissionGroup || !permissionGroup.length"
        message="提醒"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>还没有配置权限组不能创建用户奥</template>
      </a-alert>
      <a-form ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="登录名称" name="id">
          <a-input
            @change="checkTipUserName"
            :maxLength="50"
            v-model:value="temp.id"
            placeholder="登录名称,账号,创建之后不能修改"
            :disabled="createOption == false"
          />
        </a-form-item>

        <a-form-item label="昵称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="昵称" />
        </a-form-item>
        <a-form-item name="systemUser">
          <template v-slot:label>
            <a-tooltip>
              管理员
              <template v-slot:title> 管理员拥有：管理服务端的部分权限 </template>
              <QuestionCircleOutlined v-if="createOption" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-tooltip title="管理员拥有：管理服务端的部分权限">
                <a-switch
                  :checked="temp.systemUser == 1"
                  @change="
                    (checked) => {
                      temp.systemUser = checked ? 1 : 0
                    }
                  "
                  :disabled="temp.parent === 'sys'"
                  checked-children="是"
                  un-checked-children="否"
                  default-checked
                />
              </a-tooltip>
            </a-col>
            <a-col :span="4" style="text-align: right">
              <a-tooltip>
                <template v-slot:title> 禁用后该用户不能登录平台 </template>
                <QuestionCircleOutlined v-if="createOption" />
                状态：
              </a-tooltip>
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch
                  :checked="temp.status != 0"
                  @change="
                    (checked) => {
                      temp.status = checked ? 1 : 0
                    }
                  "
                  :disabled="temp.parent === 'sys'"
                  checked-children="启用"
                  un-checked-children="禁用"
                  default-checked
                />
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item label="权限组" name="permissionGroup">
          <a-select
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
            placeholder="请选择用户的权限组"
            v-model:value="temp.permissionGroup"
            mode="multiple"
          >
            <a-select-option v-for="item in permissionGroup" :key="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal destroyOnClose v-model:open="showUserPwd" title="用户密码提示" :maskClosable="false" :footer="null">
      <a-result status="success" :title="this.temp.title">
        <template #subTitle>
          <div>
            账号新密码为：
            <a-typography-paragraph :copyable="{ tooltip: false, text: temp.randomPwd }">
              <b style="color: red; font-size: 20px">
                {{ temp.randomPwd }}
              </b>
            </a-typography-paragraph>
            请将此密码复制告知该用户
          </div>
          <div style="color: red">密码只会出现一次，关闭窗口后无法再次查看密码</div>
        </template>
      </a-result>
    </a-modal>
  </div>
</template>

<script>
import { closeUserMfa, deleteUser, editUser, getUserList, unlockUser, restUserPwd } from '@/api/user/user'
import { getUserPermissionListAll } from '@/api/user/user-permission'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
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
          ellipsis: true
        },
        { title: '昵称', dataIndex: 'name', ellipsis: true },
        {
          title: '管理员',
          dataIndex: 'systemUser',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: '状态',
          dataIndex: 'status',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: '两步验证',
          dataIndex: 'twoFactorAuthKey',
          align: 'center',
          ellipsis: true,
          width: 90
        },

        {
          title: '邮箱',
          dataIndex: 'email',
          ellipsis: true
        },
        { title: '创建人', dataIndex: 'parent', ellipsis: true, width: 150 },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: '操作',
          align: 'center',
          dataIndex: 'operation',
          fixed: 'right',
          width: '120px'
        }
      ],
      // 表单校验规则
      rules: {
        id: [{ required: true, message: '请填写用户账号', trigger: 'blur' }],
        name: [{ required: true, message: '请填写用户昵称', trigger: 'blur' }],
        permissionGroup: [{ required: true, message: '请选择用户权限组', trigger: 'blur' }]
      },
      showUserPwd: false,
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
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getUserList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
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
            message: '还没有配置权限组,不能创建用户'
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
                  title: '账号添加成功',
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
      const that = this
      this.$confirm({
        title: '系统提示',
        content: '真的要删除用户么？',
        zIndex: 1009,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteUser(record.id)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 解锁
    handleUnlock(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        content: '真的要解锁用户么？',
        zIndex: 1009,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 解锁用户
            unlockUser(record.id)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    //
    handleCloseMfa(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        content: '真的关闭当前用户的两步验证么？',
        zIndex: 1009,
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 解锁用户
            closeUserMfa(record.id)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
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
    },
    //
    checkTipUserName() {
      if (this.temp?.id === 'demo') {
        this.$confirm({
          title: '系统提示',
          zIndex: 1009,
          content:
            'demo 账号是系统特定演示使用的账号,系统默认将对 demo 账号限制很多权限。非演示场景不建议使用 demo 账号',
          okText: '确认',
          cancelText: '取消',

          onCancel: () => {
            this.temp.id = ''
          }
        })
      }
    },
    //
    restUserPwdHander(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '确定要重置用户密码吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 解锁用户
            restUserPwd(record.id)
              .then((res) => {
                if (res.code === 200) {
                  that.temp = {
                    title: '用户密码重置成功',
                    randomPwd: res.data.randomPwd
                  }

                  that.showUserPwd = true
                }
                resolve()
              })
              .catch(reject)
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
