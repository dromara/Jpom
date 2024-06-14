<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserList"
      :empty-description="$t('pages.user.index.738924e5')"
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
            :placeholder="$t('pages.user.index.8384e057')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.user.index.c28c6dc1')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-tooltip :title="$t('pages.user.index.c52a67d7')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.user.index.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.user.index.7d46652a') }}</a-button>
          <a-button type="primary" @click="systemNotificationOpen = true">{{
            $t('pages.user.index.862bf72d')
          }}</a-button>
        </a-space>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.user.index.64603c01')
            }}</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()"> {{ $t('pages.user.index.6e071067') }} <DownOutlined /> </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="handleDelete(record)"
                      >{{ $t('pages.user.index.dd20d11c') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.pwdErrorCount === 0"
                      @click="handleUnlock(record)"
                      >{{ $t('pages.user.index.8dd61d74') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.parent === 'sys'"
                      @click="restUserPwdHander(record)"
                      >{{ $t('pages.user.index.e9feb431') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.twoFactorAuthKey ? false : true"
                      @click="handleCloseMfa(record)"
                      >{{ $t('pages.user.index.77fa96cb') }}</a-button
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
            :checked-children="$t('pages.user.index.c48393b5')"
            :un-checked-children="$t('pages.user.index.3cae7889')"
            disabled
            :checked="record.systemUser == 1"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-switch
            size="small"
            :checked-children="$t('pages.user.index.e6a65361')"
            :un-checked-children="$t('pages.user.index.bd324b84')"
            disabled
            :checked="record.status != 0"
          />
        </template>

        <template v-else-if="column.dataIndex === 'twoFactorAuthKey'">
          <a-switch
            size="small"
            :checked-children="$t('pages.user.index.30c72f5d')"
            :un-checked-children="$t('pages.user.index.abe04b8e')"
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
      :title="$t('pages.user.index.974ecc00')"
      :mask-closable="false"
      @ok="handleEditUserOk"
    >
      <a-alert
        v-if="!permissionGroup || !permissionGroup.length"
        :message="$t('pages.user.index.7e2364d6')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $t('pages.user.index.9c3b3ac6') }}</template>
      </a-alert>
      <a-form ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.user.index.ef10fb2a')" name="id">
          <a-input
            v-model:value="temp.id"
            :max-length="50"
            :placeholder="$t('pages.user.index.afeb1567')"
            :disabled="createOption == false"
            @change="checkTipUserName"
          />
        </a-form-item>

        <a-form-item :label="$t('pages.user.index.57c3af39')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.user.index.57c3af39')" />
        </a-form-item>
        <a-form-item name="systemUser">
          <template #label>
            <a-tooltip>
              {{ $t('pages.user.index.54b8172b') }}
              <template #title> {{ $t('pages.user.index.a967c24a') }} </template>
              <QuestionCircleOutlined v-if="createOption" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-tooltip :title="$t('pages.user.index.a967c24a')">
                <a-switch
                  :checked="temp.systemUser == 1"
                  :disabled="temp.parent === 'sys'"
                  :checked-children="$t('pages.user.index.c48393b5')"
                  :un-checked-children="$t('pages.user.index.3cae7889')"
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
                <template #title> {{ $t('pages.user.index.9c8c1216') }} </template>
                <QuestionCircleOutlined v-if="createOption" />
                {{ $t('pages.user.index.8542216f') }}
              </a-tooltip>
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch
                  :checked="temp.status != 0"
                  :disabled="temp.parent === 'sys'"
                  :checked-children="$t('pages.user.index.e6a65361')"
                  :un-checked-children="$t('pages.user.index.bd324b84')"
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
        <a-form-item :label="$t('pages.user.index.eddf3185')" name="permissionGroup">
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
            :placeholder="$t('pages.user.index.3c2dec3c')"
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
      :title="$t('pages.user.index.7c2c151')"
      :mask-closable="false"
      :footer="null"
    >
      <a-result status="success" :title="temp.title">
        <template #subTitle>
          <div>
            {{ $t('pages.user.index.4cb8151c') }}
            <a-typography-paragraph :copyable="{ tooltip: false, text: temp.randomPwd }">
              <b style="color: red; font-size: 20px">
                {{ temp.randomPwd }}
              </b>
            </a-typography-paragraph>
            {{ $t('pages.user.index.72f1baab') }}
          </div>
          <div style="color: red">{{ $t('pages.user.index.e3c8dd83') }}</div>
        </template>
      </a-result>
    </a-modal>
    <!-- 系统公告  -->
    <a-modal
      v-model:open="systemNotificationOpen"
      destroy-on-close
      :title="$t('pages.user.index.712db580')"
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
        { title: this.$t('pages.user.index.57c3af39'), dataIndex: 'name', ellipsis: true, width: 100 },
        {
          title: this.$t('pages.user.index.54b8172b'),
          dataIndex: 'systemUser',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('pages.user.index.9c32c887'),
          dataIndex: 'status',
          align: 'center',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('pages.user.index.80647404'),
          dataIndex: 'twoFactorAuthKey',
          align: 'center',
          ellipsis: true,
          width: 90
        },

        {
          title: this.$t('pages.user.index.6d12e07a'),
          dataIndex: 'email',
          ellipsis: true,
          width: 100
        },
        {
          title: this.$t('pages.user.index.b8b8d2e8'),
          dataIndex: 'source',
          ellipsis: true,
          width: 90
        },
        {
          title: this.$t('pages.user.index.3714ae65'),
          dataIndex: 'pwdErrorCount',
          ellipsis: true,
          width: 90
        },
        { title: this.$t('pages.user.index.db3c9202'), dataIndex: 'parent', ellipsis: true, width: 150 },

        {
          title: this.$t('pages.user.index.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => {
            return parseTime(text)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.user.index.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          customRender: ({ text, record }) => {
            return parseTime(text || record.optTime)
          },
          width: '170px'
        },
        {
          title: this.$t('pages.user.index.3bb962bf'),
          align: 'center',
          dataIndex: 'operation',
          fixed: 'right',
          width: '120px'
        }
      ],

      // 表单校验规则
      rules: {
        id: [{ required: true, message: this.$t('pages.user.index.c3e73623'), trigger: 'blur' }],
        name: [{ required: true, message: this.$t('pages.user.index.d2ea3987'), trigger: 'blur' }],
        permissionGroup: [{ required: true, message: this.$t('pages.user.index.aebe7c26'), trigger: 'blur' }]
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
            message: this.$t('pages.user.index.73725eb')
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
                  title: this.$t('pages.user.index.d25103f6'),
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
        title: this.$t('pages.user.index.d3367221'),
        content: this.$t('pages.user.index.62970d3a'),
        zIndex: 1009,
        okText: this.$t('pages.user.index.7da4a591'),
        cancelText: this.$t('pages.user.index.43105e21'),
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
        title: this.$t('pages.user.index.d3367221'),
        content: this.$t('pages.user.index.2d3daa3d'),
        zIndex: 1009,
        okText: this.$t('pages.user.index.7da4a591'),
        cancelText: this.$t('pages.user.index.43105e21'),
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
        title: this.$t('pages.user.index.d3367221'),
        content: this.$t('pages.user.index.85e362f2'),
        zIndex: 1009,
        okText: this.$t('pages.user.index.7da4a591'),
        cancelText: this.$t('pages.user.index.43105e21'),
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
          title: this.$t('pages.user.index.d3367221'),
          zIndex: 1009,
          content: `demo ${this.$t('pages.user.index.8424b740')},${this.$t('pages.user.index.439c765a')}`,
          okText: this.$t('pages.user.index.7da4a591'),
          cancelText: this.$t('pages.user.index.43105e21'),

          onCancel: () => {
            this.temp.id = ''
          }
        })
      }
    },
    //
    restUserPwdHander(record) {
      $confirm({
        title: this.$t('pages.user.index.d3367221'),
        zIndex: 1009,
        content: this.$t('pages.user.index.79b6f4f6'),
        okText: this.$t('pages.user.index.7da4a591'),
        cancelText: this.$t('pages.user.index.43105e21'),
        onOk: () => {
          return restUserPwd(record.id).then((res) => {
            if (res.code === 200) {
              this.temp = {
                title: this.$t('pages.user.index.7e8456dd'),
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
