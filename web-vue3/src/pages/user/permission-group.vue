<template>
  <div class="full-content">
    <div class="search-wrapper">
      <a-space>
        <a-input v-model:value="listQuery['%name%']" @pressEnter="loadData" placeholder="名称" class="search-input-item" />
        <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
          <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
        </a-tooltip>
        <a-button type="primary" @click="handleAdd">新增</a-button>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :columns="columns" :pagination="pagination" @change="changePage" bordered
      rowKey="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model:visible="editVisible" destroyOnClose width="60vw" title="编辑" @ok="handleEditUserOk"
      :maskClosable="false">
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="名称" />
        </a-form-item>
        <a-form-item name="workspace">
          <template #label>
            工作空间
            <a-tooltip v-if="!temp.id">
              <template #title> 配置工作空间权限,用户限制用户只能对应的工作空间里面操作对应的功能</template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <transfer ref="transferRef" :tree-data="workspaceList" :editKey="temp.targetKeys" />
        </a-form-item>
        <a-form-item name="prohibitExecute">
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
                <a-range-picker style="width: 100%" v-model="item.moments" :disabled-date="(current) => {
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
                }" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" valueFormat="YYYY-MM-DD HH:mm:ss"
                  :placeholder="['开始时间', '结束时间']" />
              </div>
              <div>
                <a-input v-model:value="item.reason" placeholder="禁用原因" allow-clear />
              </div>
            </div>
            <div class="item-icon" @click="() => {
              temp.prohibitExecuteArray?.splice(index, 1)
            }">
              <minus-circle-filled style="color: #ff0000" />
              <!-- <a-icon type="minus-circle" style="color: #ff0000" /> -->
            </div>
          </div>
          <a-button type="primary" @click="() => {
            temp.prohibitExecuteArray?.push({})
          }
            ">添加</a-button>
        </a-form-item>
        <a-form-item name="allowExecute">
          <template #label>
            允许时段
            <a-tooltip v-if="!temp.id">
              <template #title>
                优先判断禁用时段,再判断允许时段。配置允许时段后用户只能在对应的时段执行相应功能的操作</template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <div v-for="(item, index) in temp.allowExecuteArray" :key="item.key">
            <div class="item-info">
              <div>
                <a-select placeholder="请选择可以执行的星期" v-model="item.week" mode="multiple" style="width: 100%">
                  <a-select-option v-for="weekItem in weeks" :key="weekItem.value" :disabled="temp.allowExecuteArray.filter((arrayItem, arrayIndex) => {
                    if (arrayIndex === index) {
                      return false
                    }
                    return arrayItem.week && arrayItem.week.includes(weekItem.value)
                  }).length > 0">
                    {{ weekItem.name }}
                  </a-select-option>
                </a-select>
              </div>
              <div>
                <a-space>
                  <a-time-picker placeholder="开始时间" v-model="item.startTime" valueFormat="HH:mm:ss"
                    :default-open-value="moment('00:00:00', 'HH:mm:ss')" />
                  <a-time-picker placeholder="结束时间" v-model="item.endTime" valueFormat="HH:mm:ss"
                    :default-open-value="moment('23:59:59', 'HH:mm:ss')" />
                </a-space>
              </div>
            </div>
            <div class="item-icon" @click="() => {
              temp.allowExecuteArray?.splice(index, 1)
            }">
              <!-- <a-icon type="minus-circle" style="color: #ff0000" /> -->
              <minus-circle-filled style="color: #ff0000" />
            </div>
          </div>
          <a-button type="primary" @click="() => {
            temp.allowExecuteArray?.push({})
          }">添加</a-button>
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-input v-model:value="temp.description" :maxLength="200" type="textarea" :rows="5" placeholder="描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>


<script setup lang="ts">
import { workspaceList as fetchWorkspaceList } from '@/api/user/user'
import { getList, editPermissionGroup, deletePermissionGroup } from '@/api/user/user-permission'
import { getWorkSpaceListAll } from '@/api/workspace'
import { getMonitorOperateTypeList } from '@/api/monitor'
import moment from 'moment'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import Transfer from '@/components/compositionTransfer/composition-transfer.vue'
import { FormInstance } from 'ant-design-vue'
import { MinusCircleFilled } from '@ant-design/icons-vue'

const loading = ref(false);
const list = ref([]);


const listQuery = reactive(Object.assign({}, PAGE_DEFAULT_LIST_QUERY));
const columns = [
  { title: '名称', dataIndex: 'name', ellipsis: true },
  { title: '描述', dataIndex: 'description', ellipsis: true },
  { title: '修改人', dataIndex: 'modifyUser', ellipsis: true, width: 150 },
  {
    title: '修改时间',
    dataIndex: 'modifyTimeMillis',
    sorter: true,
    ellipsis: true,
    customRender: ({ text }) => parseTime(text),
    width: 170
  },
  {
    title: '操作',
    align: 'center',
    dataIndex: 'operation',
    width: 120
  }
];
const rules = {
  name: [{ required: true, message: '请输入权限组名称', trigger: 'blur' }]
};

const pagination = COMPUTED_PAGINATION(listQuery);

const loadData = (pointerEvent?: any) => {
  loading.value = true;
  listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.page;
  getList(listQuery).then((res) => {
    if (res.code === 200) {
      list.value = res.data.result;
      listQuery.total = res.data.total;
    }
    loading.value = false;
  });
};


const changePage = (pagination, filters, sorter) => {
  listQuery = CHANGE_PAGE(listQuery, { pagination, sorter });
  loadData();
};



// 获取操作类型
const methodFeature = ref<{ title: string, value: string, }[]>([]);
const loadOptTypeData = () => {
  getMonitorOperateTypeList().then((res) => {
    if (res.code === 200) {
      methodFeature.value = res.data.methodFeature;
    }
  });
};

// 获取工作空间
type WorkspaceType = { key: string, title: string, parentId: number | string, children?: WorkspaceType[] };
const workspaceList = ref<WorkspaceType[]>([]);
const loadWorkSpaceListAll = async () => {
  workspaceList.value = [];
  try {
    const result = await getWorkSpaceListAll()
    if (result.code === 200) {
      result.data.forEach((element: any) => {
        const children = methodFeature.value.map((item) => {
          return {
            key: element.id + '-' + item.value,
            title: item.title + '权限',
            parentId: element.id
          };
        });
        children.push({ key: element.id + '-systemUser', title: '节点管理员', parentId: element.id });
        children.push({
          key: element.id + '-sshCommandNotLimited',
          title: 'SSH 终端命令无限制',
          parentId: element.id
        });
        workspaceList.value.push({
          key: element.id,
          title: element.name,
          children: children,
          parentId: 0
        });
      });
    }
  } catch (err) {
    console.log(err)
  }
};


// 新增编辑
type FormSateType = {
  id?: string,
  name?: string,
  description?: string,
  prohibitExecute?: string,
  allowExecute?: string,
  workspace?: string,
  prohibitExecuteArray?: any[],
  allowExecuteArray?: any[],
  targetKeys?: string[]
}
const editForm = ref<FormInstance>()
const temp = reactive<FormSateType>({
  prohibitExecuteArray: [],
  allowExecuteArray: [],
  targetKeys: []
});
const editVisible = ref(false);

const handleAdd = async () => {
  temp.prohibitExecuteArray = [];
  temp.allowExecuteArray = [];
  temp.targetKeys = [];

  await loadWorkSpaceListAll();
  editVisible.value = true;
  editForm.value?.resetFields();
};


const weeks = [
  { value: 1, name: '周一' },
  { value: 2, name: '周二' },
  { value: 3, name: '周三' },
  { value: 4, name: '周四' },
  { value: 5, name: '周五' },
  { value: 6, name: '周六' },
  { value: 7, name: '周日' }
];

const handleEdit = (record: any) => {
  fetchWorkspaceList(record.id).then((res) => {

    loadWorkSpaceListAll().then(() => {
      temp.targetKeys = res.data.map((element: any) => {
        return element.workspaceId;
      });
      temp.prohibitExecuteArray = JSON.parse(record.prohibitExecute).map((item: any) => {
        return {
          reason: item.reason,
          moments: [item.startTime, item.endTime]
        };
      });
      temp.id = record.id
      temp.name = record.name
      temp.description = record.description
      temp.allowExecuteArray = JSON.parse(record.allowExecute);
      delete temp.prohibitExecute, delete temp.allowExecute;
      editVisible.value = true;
    });
  });
};

const transferRef = ref<any>(null)
const handleEditUserOk = () => {
  editForm.value?.validate().then(() => {

    const emitKeys = transferRef.value?.emitKeys;
    const tempCopy = { ...temp };

    tempCopy.prohibitExecute = JSON.stringify(
      (tempCopy.prohibitExecuteArray || []).map((item) => {
        return {
          startTime: item.moments && item.moments[0],
          endTime: item.moments && item.moments[1],
          reason: item.reason
        };
      })
    );
    delete tempCopy.prohibitExecuteArray;

    tempCopy.allowExecute = JSON.stringify(
      (tempCopy.allowExecuteArray || []).map((item) => {
        return {
          endTime: item.endTime,
          startTime: item.startTime,
          week: item.week
        };
      })
    );
    delete tempCopy.allowExecuteArray;

    if (!emitKeys || emitKeys.length <= 0) {
      $notification.error({
        message: '请选择工作空间'
      });
      return false;
    }

    tempCopy.workspace = JSON.stringify(emitKeys);
    console.log(tempCopy, 'tempCopy')

    editPermissionGroup(tempCopy).then((res) => {
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        });
        editForm.value?.resetFields();
        editVisible.value = false;
        loadData();
      }
    });
  });
};

const handleDelete = (record: any) => {
  $confirm({
    title: '系统提示',
    content: '真的要删除权限组么？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      deletePermissionGroup(record.id).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          });
          loadData();
        }
      });
    }
  });
};

onMounted(() => {
  loadOptTypeData()
  loadData()
})
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
