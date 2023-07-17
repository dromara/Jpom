<template>
  <div>
    <!-- 数据表格 -->
    <a-table :data-source="envVarList" size="middle" :loading="envVarLoading" :columns="envVarColumns"
      :pagination="envVarPagination" @change="changeListeEnvVar" bordered rowKey="id">

      <template #title>
        <a-space>
          <a-input v-model:value="envVarListQuery['%name%']" placeholder="名称" @pressEnter="loadDataEnvVar" allowClear
            class="search-input-item" />
          <a-input v-model:value="envVarListQuery['%value%']" placeholder="值" @pressEnter="loadDataEnvVar" allowClear
            class="search-input-item" />
          <a-input v-model:value="envVarListQuery['%description%']" placeholder="描述" @pressEnter="loadDataEnvVar"
            allowClear class="search-input-item" />
          <a-button type="primary" @click="loadDataEnvVar">搜索</a-button>
          <a-button type="primary" @click="addEnvVar">新增</a-button>
        </a-space>
      </template>

      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'value'">
          <a-tooltip slot="value" :title="record.privacy === 1 ? '隐私字段' : text">
            <a-icon v-if="record.privacy === 1" type="eye-invisible" />
            <span v-else>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'name'">
          <a-tooltip :title="text">{{ text }}</a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'description'">
          <a-tooltip :title="text">{{ text }}</a-tooltip>
        </template>

        <template v-if="column.dataIndex === 'workspaceId'">
          <span>{{ text === "GLOBAL" ? "全局" : "当前工作空间" }}</span>
        </template>

        <template v-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEnvEdit(record)">编辑</a-button>
            <a-button size="small" type="danger" @click="handleEnvDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 环境变量编辑区 -->
    <a-modal v-model:visible="editEnvVisible" title="编辑环境变量" width="50vw" @ok="handleEnvEditOk" :maskClosable="false">
      <a-form ref="editEnvForm" :rules="rulesEnv" :model="envTemp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="envTemp.name" :maxLength="50" placeholder="变量名称" />
        </a-form-item>
        <a-form-item label="值" :name="`${envTemp.privacy === 1 ? '' : 'value'}`">
          <a-input v-model:value="envTemp.value" type="textarea" :rows="5" placeholder="变量值" />
        </a-form-item>
        <a-form-item label="描述" name="description">
          <a-input v-model:value="envTemp.description" :maxLength="200" type="textarea" :rows="5" placeholder="变量描述" />
        </a-form-item>
        <a-form-item name="privacy">
          <template #label>
            隐私变量
            <a-tooltip v-show="!envTemp.id">
              <template #title> 隐私变量是指一些密码字段或者关键密钥等重要信息，隐私字段只能修改不能查看（编辑弹窗中无法看到对应值）。 隐私字段一旦创建后将不能切换为非隐私字段 </template>
              <!-- <a-icon type="question-circle" theme="filled" /> -->
              <question-circle-outlined />
            </a-tooltip>
          </template>
          <a-switch :checked="envTemp.privacy === 1" @change="(checked: boolean) => {
            envTemp = { ...envTemp, privacy: checked ? 1 : 0 };
          }
            " :disabled="envTemp.id !== undefined" checked-children="隐私" un-checked-children="非隐私" />
        </a-form-item>
        <a-form-item>
          <template #label>
            分发节点
            <a-tooltip v-show="!envTemp.id">
              <template #title> 分发节点是指将变量同步到对应节点，在节点脚本中也可以使用当前变量</template>
              <!-- <a-icon type="question-circle" theme="filled" /> -->
              <question-circle-outlined />
            </a-tooltip>
          </template>
          <a-select show-search option-filter-prop="children" placeholder="请选择分发到的节点" mode="multiple"
            v-model:value="envTemp.chooseNode">
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script setup lang="ts">
import { deleteWorkspaceEnv, editWorkspaceEnv, getWorkspaceEnvList } from "@/api/workspace";
import { getNodeListByWorkspace } from "@/api/node";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from "@/utils/const";
import { IPageQuery } from "@/interface/common";

const props = defineProps<{
  workspaceId: string
  global: boolean
}>()

const editEnvForm = ref();
const envVarList = ref([]);
const envVarListQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY });
const editEnvVisible = ref(false);

const nodeList = ref<any[]>([]);
const envTemp = ref<{
  id?: string
  workspaceId: string
  name: string
  value: string
  description: string
  privacy: 0 | 1
  chooseNode: string[]
  nodeIds: string
}>({
  workspaceId: props.workspaceId,
  name: '',
  value: '',
  description: '',
  privacy: 0,
  chooseNode: [],
  nodeIds: ''
});

const envVarColumns = [
  { title: "名称", dataIndex: "name", ellipsis: true },
  { title: "值", dataIndex: "value", ellipsis: true },
  { title: "描述", dataIndex: "description", ellipsis: true, },
  { title: "修改人", dataIndex: "modifyUser", ellipsis: true, width: 120 },
  { title: "作用域", dataIndex: "workspaceId", ellipsis: true, width: 120 },
  {
    title: "修改时间",
    dataIndex: "modifyTimeMillis",
    customRender: (text: string) => parseTime(text),
    sorter: true,
    width: "180px",
  },
  { title: "操作", dataIndex: "operation", align: "center", width: 120 },
];


const rulesEnv = {
  name: [{ required: true, message: "请输入变量名称", }],
  description: [{ required: true, message: "请输入变量描述", }],
  value: [{ required: true, message: "请输入变量值", }],
};

const envVarPagination = computed(() => COMPUTED_PAGINATION(envVarListQuery));

const envVarLoading = ref(false);
const loadDataEnvVar = (pointerEvent?: any) => {
  envVarLoading.value = true;

  envVarListQuery.value.workspaceId = props.workspaceId + (props.global ? ",GLOBAL" : "");
  envVarListQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : envVarListQuery.value.page;
  getWorkspaceEnvList(envVarListQuery.value).then((res) => {
    if (res.code === 200) {
      envVarList.value = res.data.result;
      envVarListQuery.value.total = res.data.total;
    }
    envVarLoading.value = false;
  });
};

const addEnvVar = () => {

  editEnvVisible.value = true;
  getAllNodeList(envTemp.value.workspaceId);
};

const handleEnvEdit = (record: any) => {
  envTemp.value = { ...record };
  envTemp.value.workspaceId = props.workspaceId;
  envTemp.value = { ...envTemp.value, chooseNode: record.nodeIds ? record.nodeIds.split(",") : [] };
  editEnvVisible.value = true;
  getAllNodeList(envTemp.value.workspaceId);
};

const handleEnvEditOk = () => {
  editEnvForm.value?.validate.then(() => {
    envTemp.value.nodeIds = envTemp.value?.chooseNode?.join(",");
    editWorkspaceEnv(envTemp.value).then((res) => {
      if (res.code === 200) {
        // 成功
        $notification.success({
          message: res.msg,
        });
        editEnvForm.value?.resetFields();
        editEnvVisible.value = false;
        loadDataEnvVar();
      }
    });
  });
};

const handleEnvDelete = (record: any) => {
  $confirm({
    title: "系统提示",
    content: "真的删除当前变量吗？",
    okText: "确认",
    cancelText: "取消",
    onOk: () => {
      // 删除
      deleteWorkspaceEnv({
        id: record.id,
        workspaceId: props.workspaceId,
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg,
          });
          loadDataEnvVar();
        }
      });
    },
  });
};

const getAllNodeList = (workspaceId: string) => {
  getNodeListByWorkspace({
    workspaceId: workspaceId,
  }).then((res) => {
    nodeList.value = res.data || [];
  });
};

const changeListeEnvVar = (pagination: any, _filters: any, sorter: any) => {
  envVarListQuery.value = CHANGE_PAGE(envVarListQuery, { pagination, sorter });
  loadDataEnvVar();
};

onMounted(() => {
  loadDataEnvVar();
});

</script>
