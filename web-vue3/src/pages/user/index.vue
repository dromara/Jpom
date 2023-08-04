<template>
  <div class="full-content">
    <div class="search-wrapper">
      <a-space>
        <a-input v-model:value:value="listQuery.id" @pressEnter="loadData" placeholder="用户名ID"
          class="search-input-item" />
        <a-input v-model:value="listQuery['%name%']" @pressEnter="loadData" placeholder="用户名" class="search-input-item" />
        <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
          <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
        </a-tooltip>
        <a-button type="primary" @click="handleAdd">新增</a-button>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="pagination" @change="changePage" bordered
      rowKey="id">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'systemUser'">
          <a-switch size="small" checked-children="是" un-checked-children="否" disabled
            :checked="record.systemUser == 1" />
        </template>

        <template v-if="column.dataIndex === 'status'">
          <a-switch size="small" checked-children="启用" un-checked-children="禁用" disabled :checked="record.status != 0" />
        </template>

        <template v-if="column.dataIndex === 'twoFactorAuthKey'">
          <a-switch size="small" checked-children="开" un-checked-children="关" disabled
            :checked="record.twoFactorAuthKey ? true : false" />
        </template>

        <template v-if="column.dataIndex === 'id'">
          <a-tooltip :title="text">
            {{ text }}
          </a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'email'">
          <a-tooltip :title="text">
            {{ text }}
          </a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
              <a-menu #overlay>
                <a-menu-item>
                  <a-button type="danger" size="small" :disabled="record.parent === 'sys'"
                    @click="handleDelete(record)">删除</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button type="danger" size="small" :disabled="record.pwdErrorCount === 0"
                    @click="handleUnlock(record)">解锁</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button type="danger" size="small" :disabled="record.parent === 'sys'"
                    @click="restUserPwdHander(record)">重置密码</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button type="danger" size="small" :disabled="record.twoFactorAuthKey ? false : true"
                    @click="handleCloseMfa(record)">关闭MFA</a-button>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model:value="editUserVisible" width="60vw" title="编辑用户" @ok="handleEditUserOk"
      :maskClosable="false">
      <a-from ref="editUserForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-from-item label="登录名称" name="id">
          <a-input @change="checkTipUserName" :maxLength="50" v-model:value="temp.id" placeholder="登录名称,账号,创建之后不能修改"
            :disabled="createOption == false" />
        </a-from-item>

        <a-from-item label="昵称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="昵称" />
        </a-from-item>
        <a-from-item name="systemUser">
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
                <a-switch :checked="temp.systemUser == 1" @change="(checked: boolean) => {
                  temp.systemUser = checked ? 1 : 0;
                }
                  " :disabled="temp.parent === 'sys'" checked-children="是" un-checked-children="否" default-checked />
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
              <a-switch :checked="temp.status != 0" @change="(checked: boolean) => {
                temp.status = checked ? 1 : 0;
              }
                " :disabled="temp.parent === 'sys'" checked-children="启用" un-checked-children="禁用" default-checked />
            </a-col>
          </a-row>
        </a-from-item>
        <a-from-item label="权限组" name="permissionGroup">
          <a-select show-search option-filter-prop="children" placeholder="请选择用户的权限组" v-model:value="temp.permissionGroup"
            mode="multiple">
            <a-select-option v-for="item in permissionGroup" :key="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-from-item>
      </a-from>
    </a-modal>
    <a-modal destroyOnClose v-model:value="showUserPwd" title="用户密码提示" :maskClosable="false" :footer="null">
      <a-result status="success" :title="temp.title">
        <template #subTitle>
          账号新密码为：
          <a-typography-paragraph copyable strong>{{ temp.randomPwd }}</a-typography-paragraph>
          请将此密码复制告知该用户
        </template>
      </a-result>
    </a-modal>
  </div>
</template>


<script setup lang="ts">
import { closeUserMfa, deleteUser, editUser, getUserList, unlockUser, restUserPwd } from '@/api/user/user';
import { getUserPermissionListAll } from '@/api/user/user-permission';
import { IPageQuery } from '@/interface/common';
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const';

const loading = ref(false);
const list = ref([]);
const temp = ref<any>({});
const createOption = ref(true);
const editUserVisible = ref(false);
const listQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY });
const permissionGroup = ref<any[]>([]);
const showUserPwd = ref(false);
const editUserForm = ref();

const columns = [
  { title: 'ID', dataIndex: 'id', ellipsis: true, scopedSlots: { customRender: 'id' } },
  { title: '昵称', dataIndex: 'name', ellipsis: true },
  { title: '管理员', dataIndex: 'systemUser', align: 'center', ellipsis: true, width: 90, scopedSlots: { customRender: 'systemUser' } },
  { title: '状态', dataIndex: 'status', align: 'center', ellipsis: true, width: 90, scopedSlots: { customRender: 'status' } },
  { title: '两步验证', dataIndex: 'twoFactorAuthKey', align: 'center', ellipsis: true, width: 90, scopedSlots: { customRender: 'twoFactorAuthKey' } },
  { title: '邮箱', dataIndex: 'email', ellipsis: true, scopedSlots: { customRender: 'email' } },
  { title: '创建人', dataIndex: 'parent', ellipsis: true, width: 150 },
  {
    title: '修改时间',
    dataIndex: 'modifyTimeMillis',
    sorter: true,
    ellipsis: true,
    customRender: ({ text }) => parseTime(text),
    width: 170,
  },
  { title: '操作', align: 'center', dataIndex: 'operation', scopedSlots: { customRender: 'operation' }, width: 120 },
];

const rules = {
  id: [{ required: true, message: '请填写用户账号', trigger: 'blur' }],
  name: [{ required: true, message: '请填写用户昵称', trigger: 'blur' }],
  permissionGroup: [{ required: true, message: '请选择用户权限组', trigger: 'blur' }],
};

const pagination = COMPUTED_PAGINATION(listQuery.value);

const loadData = (pointerEvent?: any) => {
  loading.value = true;
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page;
  getUserList(listQuery.value).then((res) => {
    if (res.code === 200) {
      list.value = res.data.result;
      listQuery.value.total = res.data.total;
    }
    loading.value = false;
  });
};

const handleAdd = () => {
  temp.value = { systemUser: 0 };
  createOption.value = true;
  listUserPermissionListAll();
  editUserVisible.value = true;
  editUserForm.value?.resetFields();
};

const listUserPermissionListAll = () => {
  getUserPermissionListAll().then((res) => {
    if (res.code === 200 && res.data) {
      permissionGroup.value = res.data;
    }
    if (!permissionGroup.value || permissionGroup.value.length <= 0) {
      $notification.warn({
        message: '还没有配置权限组，不能创建用户',
      });
    }
  });
};

const handleEdit = (record: any) => {
  createOption.value = false;
  temp.value = {
    ...record,
    permissionGroup: (record.permissionGroup || '').split('@').filter((item: any) => item),
    status: record.status === undefined ? 1 : record.status,
  };
  listUserPermissionListAll();
  editUserVisible.value = true;
  editUserForm.value?.resetFields();
};

const handleEditUserOk = () => {
  editUserForm.value?.validate.then(() => {
    const paramsTemp = { ...temp.value };
    paramsTemp.type = createOption.value ? 'add' : 'edit';
    paramsTemp.permissionGroup = (paramsTemp.permissionGroup || []).join('@');
    editUser(paramsTemp).then((res) => {
      if (res.code === 200) {
        if (paramsTemp.type === 'add') {
          temp.value = {
            title: '账号添加成功',
            randomPwd: res.data.randomPwd,
          };
          showUserPwd.value = true;
        } else {
          $notification.success({
            message: res.msg,
          });
        }
        editUserVisible.value = false;
        loadData();
      }
    });
  });
};

const handleDelete = (record: any) => {
  $confirm({
    title: '系统提示',
    content: '真的要删除用户么？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      deleteUser(record.id).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg,
          });
          loadData();
        }
      });
    },
  });
};

const handleUnlock = (record: any) => {
  $confirm({
    title: '系统提示',
    content: '真的要解锁用户么？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      unlockUser(record.id).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg,
          });
          loadData();
        }
      });
    },
  });
};

const handleCloseMfa = (record: any) => {
  $confirm({
    title: '系统提示',
    content: '真的关闭当前用户的两步验证么？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      closeUserMfa(record.id).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg,
          });
          loadData();
        }
      });
    },
  });
};

const changePage = (pagination: any, filters: any, sorter: any) => {
  listQuery.value = CHANGE_PAGE(listQuery.value, { pagination, sorter });
  loadData();
};

const checkTipUserName = () => {
  if (temp.value?.id === 'demo') {
    $confirm({
      title: '系统提示',
      content: 'demo 账号是系统特定演示使用的账号，系统默认将对 demo 账号限制很多权限。非演示场景不建议使用 demo 账号',
      okText: '确认',
      cancelText: '取消',
      onOk: () => { },
      onCancel: () => {
        temp.value.id = '';
      },
    });
  }
};

const restUserPwdHander = (record: any) => {
  $confirm({
    title: '系统提示',
    content: '确定要重置用户密码吗？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      restUserPwd(record.id).then((res) => {
        if (res.code === 200) {
          temp.value = {
            title: '用户密码重置成功',
            randomPwd: res.data.randomPwd,
          };
          showUserPwd.value = true;
        }
      });
    },
  });
};

onMounted(loadData);
</script>
