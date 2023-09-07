<template>
  <div class="full-content">
    <div class="search-wrapper">
      <a-space>
        <a-input v-model:value="listQuery['%name%']" @pressEnter="loadData" placeholder="请输入备份名称"
          class="search-input-item" />
        <a-input v-model:value="listQuery['%version%']" @pressEnter="loadData" placeholder="请输入版本"
          class="search-input-item" />
        <a-select v-model:value="listQuery.backupType" allowClear placeholder="请选择备份类型" class="search-input-item">
          <a-select-option v-for="backupType in backupTypeList" :key="backupType.key">{{
            backupType.value
          }}</a-select-option>
        </a-select>
        <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
          <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
        </a-tooltip>
        <a-button type="primary" @click="handleAdd">创建备份</a-button>
        <a-button type="primary" @click="handleSqlUpload">导入备份</a-button>
      </a-space>
    </div>
    <!-- 表格 -->
    <a-table size="middle" :columns="columns" :data-source="list" bordered rowKey="id" @change="changePage"
      :pagination="pagination">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'backupType'">
          <a-tooltip :title="text">
            <span>{{ backupTypeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'baleTimeStamp'">
          {{ parseTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          {{ parseTime(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-typography-paragraph v-if="record.fileExist" :copyable="{ text: record.filePath }">
            <template #copyableTooltip>复制文件路径</template>
            {{ backupStatusMap[text] }}
          </a-typography-paragraph>
          <a-tooltip v-else :title="`备份文件不存在:${record.filePath}`">
            <warning-outlined />
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'fileSize'">
          <a-tooltip :title="renderSize(text) + ' ' + record.sha1Sum">
            <a-tag color="#108ee9">{{ renderSize(text) }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" :disabled="!record.fileExist || record.status !== 1"
              @click="handleDownload(record)">下载</a-button>
            <a-button size="small" type="danger" :disabled="!record.fileExist || record.status !== 1"
              @click="handleRestore(record)">还原</a-button>
            <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>


    <!-- 创建备份信息区 -->
    <a-modal destroyOnClose v-model:visible="createBackupVisible" title="创建备份信息" @ok="handleCreateBackupOk" width="600px"
      :maskClosable="false">
      <a-form ref="editBackupForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="备份类型" prop="backupType">
          <a-radio-group v-model:value="temp.backupType" name="backupType">
            <a-radio v-for="item in backupTypeList" v-show="!item.disabled" :key="item.key" :value="item.key">{{
              item.value
            }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <!-- 部分备份 -->
        <a-form-item v-if="temp.backupType === 1" label="勾选数据表" prop="tableNameList" class="feature jpom-role">
          <a-transfer :data-source="tableNameList" show-search :filter-option="filterOption" :target-keys="targetKeys"
            :render="(item) => item.title" @change="handleChange" />
        </a-form-item>
      </a-form>
    </a-modal>


    <!-- 上传 SQL 备份文件 -->
    <a-modal destroyOnClose v-model:visible="uploadSqlFileVisible" width="300px" title="上传 SQL 文件" :footer="null"
      :maskClosable="true">
      <a-upload :file-list="uploadFileList" :remove="handleSqlRemove" :before-upload="beforeSqlUpload" accept=".sql">
        <a-button>
          <template #icon>
            <upload-outlined />
          </template>
          选择 SQL 文件
        </a-button>
      </a-upload>
      <br />
      <a-progress v-if="percentage" :percent="percentage" status="success"></a-progress>
      <br />
      <a-space>
        <a-button type="primary" :disabled="fileUploadDisabled" @click="startSqlUpload">开始上传</a-button>
        <a-tag color="green" :visible="successSize !== 0" :closable="true" class="successTag">
          上传成功: {{ successSize }} 个文件!
        </a-tag>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {
  backupStatusMap,
  backupTypeArray,
  backupTypeMap,
  createBackup,
  deleteBackup,
  downloadBackupFile,
  getBackupList,
  getTableNameList,
  restoreBackup,
  uploadBackupFile
} from '@/api/backup-info'
import { IPageQuery } from '@/interface/common';
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize } from '@/utils/const'
import { WarningOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { FormInstance } from 'ant-design-vue';
import { createVNode } from 'vue';

// const backupTypeMap = backupTypeMap;
// const backupStatusMap = backupStatusMap;
const loading = ref(false);
const listQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY });
const backupTypeList = backupTypeArray;
const list = ref([]);
const total = ref(0);

const targetKeys = ref([]);
const uploadFileList = ref([]);

const createBackupVisible = ref(false);
const uploadSqlFileVisible = ref(false);
const uploading = ref(false);
const percentage = ref(0);
const backupType = ref(0);
const successSize = ref(0);
const rules = {
  name: [{ required: true, message: 'Please input build name', trigger: 'blur' }],
  script: [{ required: true, message: 'Please input build script', trigger: 'blur' }],
  resultDirFile: [{ required: true, message: 'Please input build target path', trigger: 'blur' }],
  releasePath: [{ required: true, message: 'Please input release path', trigger: 'blur' }]
};
let timer = null;

const columns = [
  { title: '备份名称', dataIndex: 'name', ellipsis: true, scopedSlots: { customRender: 'name' } },
  {
    title: '打包时间',
    width: 170,
    dataIndex: 'baleTimeStamp',
    // ellipsis: true,
    sorter: true,
  },
  {
    title: '版本',
    dataIndex: 'version',
    width: 100,
    // ellipsis: true,
  },
  {
    title: '备份类型',
    dataIndex: 'backupType',
    width: 100,
    ellipsis: true,
  },
  {
    title: '文件大小',
    dataIndex: 'fileSize',
    width: 100,
  },
  { title: '状态', dataIndex: 'status', width: 120, },
  // {
  //   title: "文件地址",
  //   dataIndex: "filePath",
  //   // width: 150,
  //   ellipsis: true,
  //   scopedSlots: { customRender: "filePath" },
  // },
  {
    title: '修改人',
    dataIndex: 'modifyUser',
    ellipsis: true,
    width: 120
  },
  {
    title: '备份时间',
    dataIndex: 'createTimeMillis',
    sorter: true,
    width: '170px'
  },
  {
    title: '操作',
    dataIndex: 'operation',
    width: '180px',
    align: 'center',
    fixed: 'right'
  }
]

// 格式化文件大小
// const renderSizeFormat = (value) => renderSize(value);

const fileUploadDisabled = computed(() => {
  return uploadFileList.value.length === 0 || uploading.value
});

const pagination = computed(() => {
  return COMPUTED_PAGINATION(listQuery.value)
});



// 加载数据
const loadData = (pointerEvent?: any) => {
  loading.value = true;
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page;

  getBackupList(listQuery.value).then((res) => {
    if (res.code === 200) {
      list.value = res.data.result;
      total.value = res.data.total;
    }
    loading.value = false;
  });
};

const tableNameList = ref<{ key: string, title: string }[]>([]);
// 加载数据库表名列表
const loadTableNameList = () => {
  tableNameList.value = [];
  getTableNameList().then((res) => {
    if (res.code === 200) {
      res.data.forEach((element: any) => {
        tableNameList.value.push({ key: element.tableName, title: element.tableDesc });
      });
    }
  });
};

const filterOption = (inputValue, option) => option.title.indexOf(inputValue) > -1;

const handleChange = (newTargetKeys) => {
  targetKeys.value = newTargetKeys;
};

// 新增
const temp = reactive({
  backupType: 0
});
const handleAdd = () => {
  targetKeys.value = [];
  temp.backupType = 0;
  loadTableNameList();
  createBackupVisible.value = true;
};

// 确认新增
const editBackupForm = ref<FormInstance>()
const handleCreateBackupOk = () => {
  editBackupForm.value?.validate().then(() => {

    createBackup(targetKeys.value).then((res) => {
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        });
        editBackupForm.value?.resetFields();
        createBackupVisible.value = false;
        loadData();
      }
    });
  });
};

const handleDownload = (record: any) => {
  window.open(downloadBackupFile(record.id), '_blank');
};

const handleDelete = (record: any) => {
  $confirm({
    title: '系统提示',
    content: '真的要删除备份信息么？',
    okText: '确认',
    cancelText: '取消',
    onOk: () => {
      deleteBackup(record.id).then((res) => {
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

// 还原备份
const handleRestore = (record: any) => {
  const html =
    "真的要还原备份信息么？<ul style='color:red;'>" +
    '<li>建议还原和当前版本一致的文件或者临近版本的文件</li>' +
    '<li>如果版本相差大需要重新初始化数据来保证和当前程序里面字段一致</li>' +
    '<li>重置初始化在启动时候传入参数 <b> --rest:load_init_db </b> </li>' +
    ' </ul>还原过程中不能操作哦...';
  $confirm({
    title: '系统提示',
    content: createVNode('div', null, [createVNode('p', { innerHTML: html }, null)]),
    okText: '确认',
    cancelText: '取消',
    width: 600,
    onOk: () => {
      restoreBackup(record.id).then((res) => {
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

// 上传压缩文件
const handleSqlUpload = () => {
  successSize.value = 0;
  uploadSqlFileVisible.value = true;
  clearInterval(timer);
  percentage.value = 0;
  uploadFileList.value = [];
  uploading.value = false;
};

const handleSqlRemove = () => {
  handleSqlUpload();
};


const beforeSqlUpload = (file) => {
  uploadFileList.value = [file];
  return false;
};


// 开始上传 SQL 文件
const startSqlUpload = () => {
  $notification.info({
    message: '正在上传文件，请稍后...'
  });

  uploading.value = true;
  timer = setInterval(() => {
    percentage.value = percentage.value > 99 ? 99 : percentage.value + 1;
  }, 1000);

  const file = uploadFileList.value[0];
  const formData = new FormData();
  formData.append('file', file);
  formData.append('backupType', backupType.value);

  uploadBackupFile(formData).then((res) => {
    if (res.code === 200) {
      $notification.success({
        message: res.msg
      });
      successSize.value++;
      percentage.value = 100;

      setTimeout(() => {
        handleSqlRemove();
        loadData();
        uploadSqlFileVisible.value = false;
      }, 1000);
    }
  });
};

const changePage = (pagination, filters, sorter) => {
  listQuery.value = CHANGE_PAGE(listQuery.value, { pagination, sorter });
  loadData();
};


onMounted(() => {
  loadData();
})

onUnmounted(() => {
  timer && clearTimeout(timer)
})

</script>

<style scoped></style>
