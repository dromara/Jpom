<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserList"
      :empty-description="$t('i18n.0f189dbaa4')"
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
            :placeholder="$t('i18n.1c9d3cb687')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n.819767ada1')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('i18n.4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('i18n.e5f71fc31e')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n.66ab5e9f24') }}</a-button>
          <a-button type="primary" @click="systemNotificationOpen = true">{{
            $t('i18n.7c223eb6e9')
          }}</a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('i18n.95b351c862')
            }}</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()"> {{ $t('i18n.0ec9eaf9c3') }} <DownOutlined /> </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="handleDelete(record)"
                      >{{ $t('i18n.2f4aaddde3') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.pwdErrorCount === 0"
                      @click="handleUnlock(record)"
                      >{{ $t('i18n.fa7ffa2d21') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="restUserPwdHander(record)"
                      >{{ $t('i18n.0719aa2bb0') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.twoFactorAuthKey ? false : true"
                      @click="handleCloseMfa(record)"
                      >{{ $t('i18n.0703877167') }}</a-button
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
            :checked-children="$t('i18n.0a60ac8f02')"
            :un-checked-children="$t('i18n.c9744f45e7')"
            disabled
            :checked="record.systemUser == 1"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            size="small"
            :checked-children="$t('i18n.7854b52a88')"
            :un-checked-children="$t('i18n.710ad08b11')"
            disabled
            :checked="record.status != 0"
          />
        </template>

        <template v-else-if="column.dataIndex === 'twoFactorAuthKey'">
          <a-switch
            size="small"
            :checked-children="$t('i18n.8493205602')"
            :un-checked-children="$t('i18n.d58a55bcee')"
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
    <CustomModal
      v-if="editUserVisible"
      v-model:open="editUserVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="$t('i18n.5a0346c4b1')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <a-alert
        v-if="!permissionGroup || !permissionGroup.length"
        :message="$t('i18n.4b027f3979')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $t('i18n.d9531a5ac3') }}</template>
      </a-alert>
      <a-form ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n.878aebf9b2')" name="id">
          <a-input
            v-model:value="temp.id"
            :max-length="50"
            :placeholder="$t('i18n.f175274df0')"
            :disabled="createOption == false"
            @change="checkTipUserName"
          />
        </a-form-item>

        <a-form-item :label="$t('i18n.23eb0e6024')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n.23eb0e6024')" />
        </a-form-item>
        <a-form-item name="systemUser">
          <template #label>
            <a-tooltip>
              {{ $t('i18n.b1dae9bc5c') }}
              <template #title> {{ $t('i18n.b328609814') }} </template>
              <QuestionCircleOutlined v-if="createOption" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-tooltip :title="$t('i18n.b328609814')">
                <a-switch
                  :checked="temp.systemUser == 1"
                  :disabled="temp.parent === 'sys'"
                  :checked-children="$t('i18n.0a60ac8f02')"
                  :un-checked-children="$t('i18n.c9744f45e7')"
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
                <template #title> {{ $t('i18n.fa624c8420') }} </template>
                <QuestionCircleOutlined v-if="createOption" />
                {{ $t('i18n.bec98b4d6a') }}
              </a-tooltip>
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch
                  :checked="temp.status != 0"
                  :disabled="temp.parent === 'sys'"
                  :checked-children="$t('i18n.7854b52a88')"
                  :un-checked-children="$t('i18n.710ad08b11')"
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
        <a-form-item :label="$t('i18n.f49dfdace4')" name="permissionGroup">
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
            :placeholder="$t('i18n.72d14a3890')"
            mode="multiple"
          >
            <a-select-option v-for="item in permissionGroup" :key="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
    <CustomModal
      v-if="showUserPwd"
      v-model:open="showUserPwd"
      destroy-on-close
      :title="$t('i18n.318ce9ea8b')"
      :mask-closable="false"
      :footer="null"
    >
      <a-result status="success" :title="temp.title">
        <template #subTitle>
          <div>
            {{ $t('i18n.5684fd7d3d') }}
            <a-typography-paragraph :copyable="{ tooltip: false, text: temp.randomPwd }">
              <b style="color: red; font-size: 20px">
                {{ temp.randomPwd }}
              </b>
            </a-typography-paragraph>
            {{ $t('i18n.12d2c0aead') }}
          </div>
          <div style="color: red">{{ $t('i18n.c7e0803a17') }}</div>
        </template>
      </a-result>
    </CustomModal>
    <!-- 系统公告  -->
    <CustomModal
      v-if="systemNotificationOpen"
      v-model:open="systemNotificationOpen"
      destroy-on-close
      :title="$t('i18n.6428be07e9')"
      :mask-closable="false"
      width="50vw"
      :footer="null"
    >
      <notification />
    </CustomModal>
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
        { title: this.$t('i18n.23eb0e6024'), dataIndex: 'name', ellipsis: true, width: 100 },
        {
          title: this.$t('i18n.b1dae9bc5c'),
          dataIndex: 'systemUser',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('i18n.3fea7ca76c'),
          dataIndex: 'status',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('i18n.dbad1e89f7'),
          dataIndex: 'twoFactorAuthKey',
          align: 'center',
          ellipsis: true,
          width: 90
        },

        {
          title: this.$t('i18n.3bc5e602b2'),
          dataIndex: 'email',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('i18n.26ca20b161'),
          dataIndex: 'source',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('i18n.b6076a055f'),
          dataIndex: 'pwdErrorCount',
          ellipsis: true,
          width: 90
        },
        { title: this.$t('i18n.95a43eaa59'), dataIndex: 'parent', ellipsis: true, width: 150 },

        {
          title: this.$t('i18n.1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n.eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, record }) => {
            return parseTime(text || record.optTime)
          },
          width: '170px'
        },
        {
          title: this.$t('i18n.2b6bc0f293'),
          align: 'center',
          dataIndex: 'operation',
          fixed: 'right',
          width: '120px'
        }
      ],

      // 表单校验规则
      rules: {
        id: [{ required: true, message: this.$t('i18n.693a06987c'), trigger: 'blur' }],
        name: [{ required: true, message: this.$t('i18n.c00fb0217d'), trigger: 'blur' }],
        permissionGroup: [{ required: true, message: this.$t('i18n.e8073b3843'), trigger: 'blur' }]
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
            message: this.$t('i18n.d4744ce461')
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
                  title: this.$t('i18n.2d2238d216'),
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
        title: this.$t('i18n.c4535759ee'),
        content: this.$t('i18n.45f8d5a21d'),
        zIndex: 1009,
        okText: this.$t('i18n.e83a256e4f'),
        cancelText: this.$t('i18n.625fb26b4b'),
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
        title: this.$t('i18n.c4535759ee'),
        content: this.$t('i18n.bc2f1beb44'),
        zIndex: 1009,
        okText: this.$t('i18n.e83a256e4f'),
        cancelText: this.$t('i18n.625fb26b4b'),
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
        title: this.$t('i18n.c4535759ee'),
        content: this.$t('i18n.b8915a4933'),
        zIndex: 1009,
        okText: this.$t('i18n.e83a256e4f'),
        cancelText: this.$t('i18n.625fb26b4b'),
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
          title: this.$t('i18n.c4535759ee'),
          zIndex: 1009,
          content: `demo ${this.$t('i18n.a8f44c3188')},${this.$t('i18n.c5f9a96133')}`,
          okText: this.$t('i18n.e83a256e4f'),
          cancelText: this.$t('i18n.625fb26b4b'),

          onCancel: () => {
            this.temp.id = ''
          }
        })
      }
    },
    //
    restUserPwdHander(record) {
      $confirm({
        title: this.$t('i18n.c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n.be2109e5b1'),
        okText: this.$t('i18n.e83a256e4f'),
        cancelText: this.$t('i18n.625fb26b4b'),
        onOk: () => {
          return restUserPwd(record.id).then((res) => {
            if (res.code === 200) {
              this.temp = {
                title: this.$t('i18n.2c5b0e86e6'),
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
