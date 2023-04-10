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
      <template slot="title">
        <a-space>
          <a-input v-model="listQuery.id" @pressEnter="loadData" placeholder="用户名ID" class="search-input-item" />
          <a-input
            v-model="listQuery['%name%']"
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
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="danger" size="small" :disabled="record.parent === 'sys'" @click="handleDelete(record)"
                  >删除</a-button
                >
              </a-menu-item>
              <a-menu-item>
                <a-button
                  type="danger"
                  size="small"
                  :disabled="record.pwdErrorCount === 0"
                  @click="handleUnlock(record)"
                  >解锁</a-button
                >
              </a-menu-item>
              <a-menu-item>
                <a-button
                  type="danger"
                  size="small"
                  :disabled="record.parent === 'sys'"
                  @click="restUserPwdHander(record)"
                  >重置密码</a-button
                >
              </a-menu-item>
              <a-menu-item>
                <a-button
                  type="danger"
                  size="small"
                  :disabled="record.twoFactorAuthKey ? false : true"
                  @click="handleCloseMfa(record)"
                  >关闭MFA</a-button
                >
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
      <template slot="systemUser" slot-scope="text, record">
        <a-switch
          size="small"
          checked-children="是"
          un-checked-children="否"
          disabled
          :checked="record.systemUser == 1"
        />
      </template>
      <template slot="status" slot-scope="text, record">
        <a-switch
          size="small"
          checked-children="启用"
          un-checked-children="禁用"
          disabled
          :checked="record.status != 0"
        />
      </template>

      <template slot="twoFactorAuthKey" slot-scope="text, record">
        <a-switch
          size="small"
          checked-children="开"
          un-checked-children="关"
          disabled
          :checked="record.twoFactorAuthKey ? true : false"
        />
      </template>

      <a-tooltip slot="id" slot-scope="text" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>

      <a-tooltip slot="email" slot-scope="text" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      v-model="editUserVisible"
      width="60vw"
      title="编辑用户"
      @ok="handleEditUserOk"
      :maskClosable="false"
    >
      <a-form ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="登录名称" prop="id">
          <a-input
            @change="checkTipUserName"
            :maxLength="50"
            v-model="temp.id"
            placeholder="登录名称,账号,创建之后不能修改"
            :disabled="createOption == false"
          />
        </a-form-item>

        <a-form-item label="昵称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="昵称" />
        </a-form-item>
        <a-form-item prop="systemUser">
          <template slot="label">
            管理员
            <a-tooltip v-if="createOption">
              <template slot="title"> 管理员拥有：管理服务端的部分权限 </template>
              <a-icon type="question-circle" theme="filled" />
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
                <template slot="title"> 禁用后该用户不能登录平台 </template>
                <a-icon v-if="createOption" type="question-circle" theme="filled" />
                状态：
              </a-tooltip>
            </a-col>
            <a-col :span="4">
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
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item label="权限组" prop="permissionGroup">
          <a-select
            show-search
            option-filter-prop="children"
            placeholder="请选择用户的权限组"
            v-model="temp.permissionGroup"
            mode="multiple"
          >
            <a-select-option v-for="item in permissionGroup" :key="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <a-modal destroyOnClose v-model="showUserPwd" title="用户密码提示" :maskClosable="false" :footer="null">
      <a-result status="success" :title="this.temp.title">
        <template #subTitle>
          账号新密码为：
          <b
            style="color: red; font-size: 20px"
            v-clipboard:copy="temp.randomPwd"
            v-clipboard:success="
              () => {
                tempVue.prototype.$notification.success({
                  message: '复制成功'
                })
              }
            "
            v-clipboard:error="
              () => {
                tempVue.prototype.$notification.error({
                  message: '复制失败'
                })
              }
            "
          >
            {{ temp.randomPwd }}
            <a-icon type="copy" />
          </b>
          请将此密码复制告知该用户
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
      tempVue: null,
      createOption: true,
      editUserVisible: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      permissionGroup: [],
      columns: [
        { title: 'ID', dataIndex: 'id', ellipsis: true, scopedSlots: { customRender: 'id' } },
        { title: '昵称', dataIndex: 'name', ellipsis: true },
        {
          title: '管理员',
          dataIndex: 'systemUser',
          align: 'center',
          ellipsis: true,
          width: 90,
          scopedSlots: { customRender: 'systemUser' }
        },
        {
          title: '状态',
          dataIndex: 'status',
          align: 'center',
          ellipsis: true,
          width: 90,
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '两步验证',
          dataIndex: 'twoFactorAuthKey',
          align: 'center',
          ellipsis: true,
          width: 90,
          scopedSlots: { customRender: 'twoFactorAuthKey' }
        },

        { title: '邮箱', dataIndex: 'email', ellipsis: true, scopedSlots: { customRender: 'email' } },
        { title: '创建人', dataIndex: 'parent', ellipsis: true, width: 150 },
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
        id: [{ required: true, message: '请填写用户账号', trigger: 'blur' }],
        name: [{ required: true, message: '请填写用户昵称', trigger: 'blur' }],
        permissionGroup: [{ required: true, message: '请选择用户权限组', trigger: 'blur' }]
      },
      showUserPwd: false
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
          this.$notification.warn({
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
      this.$refs['editUserForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        const paramsTemp = Object.assign({}, this.temp)

        paramsTemp.type = this.createOption ? 'add' : 'edit'
        paramsTemp.permissionGroup = (paramsTemp.permissionGroup || []).join('@')

        // 需要判断当前操作是【新增】还是【修改】
        editUser(paramsTemp).then((res) => {
          if (res.code === 200) {
            if (paramsTemp.type === 'add') {
              this.temp = {
                title: '账号添加成功',
                randomPwd: res.data.randomPwd
              }
              this.tempVue = Vue
              this.showUserPwd = true
            } else {
              this.$notification.success({
                message: res.msg
              })
            }

            this.editUserVisible = false
            this.loadData()
          }
        })
      })
    },
    // 删除用户
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        content: '真的要删除用户么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteUser(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
      this.$confirm({
        title: '系统提示',
        content: '真的要解锁用户么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 解锁用户
          unlockUser(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
      this.$confirm({
        title: '系统提示',
        content: '真的关闭当前用户的两步验证么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 解锁用户
          closeUserMfa(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
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
        this.$confirm({
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
    },
    //
    restUserPwdHander(record) {
      this.$confirm({
        title: '系统提示',
        content: '确定要重置用户密码吗？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 解锁用户
          restUserPwd(record.id).then((res) => {
            if (res.code === 200) {
              this.temp = {
                title: '用户密码重置成功',
                randomPwd: res.data.randomPwd
              }
              this.tempVue = Vue
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
