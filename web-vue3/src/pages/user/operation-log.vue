<template>
  <div class="full-content">
    <div class="search-wrapper">
      <a-space>
        <a-select show-search option-filter-prop="children" v-model:value="listQuery.userId" allowClear
          placeholder="请选择操作者" class="search-input-item">
          <a-select-option v-for="item in userList" :key="item.id">{{ item.name }}</a-select-option>
        </a-select>
        <a-select show-search option-filter-prop="children" v-model:value="listQuery.nodeId" allowClear
          placeholder="请选择节点" class="search-input-item">
          <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
        </a-select>
        <a-select show-search option-filter-prop="children" v-model:value="listQuery.classFeature" allowClear
          placeholder="操作功能" class="search-input-item">
          <a-select-option v-for="item in classFeature" :key="item.value">{{ item.title }}</a-select-option>
        </a-select>
        <a-select show-search option-filter-prop="children" v-model:value="listQuery.methodFeature" allowClear
          placeholder="操作方法" class="search-input-item">
          <a-select-option v-for="item in methodFeature" :key="item.value">{{ item.title }}</a-select-option>
        </a-select>
        <a-range-picker class="search-input-item" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss"
          @change="onChangeTime" />
        <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
          <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
        </a-tooltip>
      </a-space>
    </div>
    <!-- 数据表格 -->
    <a-table size="middle" :data-source="list" :columns="columns" :pagination="pagination" bordered rowKey="index"
      @change="change">
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'nodeId'">
          <a-tooltip placement="topLeft" :title="nodeMap[text]">
            <span>{{ nodeMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'classFeature'">
          <a-tooltip placement="topLeft" :title="classFeatureMap[text]">
            <span>{{ classFeatureMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'methodFeature'">
          <a-tooltip placement="topLeft" :title="methodFeatureMap[text]">
            <span>{{ methodFeatureMap[text] }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'username'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text || record.userId }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'optStatus'">
          <a-tooltip placement="topLeft" :title="`默认状态码为 200 表示执行成功,部分操作状态码可能为 0,状态码为 0 的操作大部分为没有操作结果或者异步执行`">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'createTimeMillis'">
          {{ parseTime(text || record.optTime) }}
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-button size="small" type="primary" @click="handleDetail(record)">详情</a-button>
        </template>
        <template v-else>
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
      </template>
    </a-table>
    <!-- 详情区 -->
    <a-modal v-model:visible="detailVisible" destroyOnClose width="600px" title="详情信息" :footer="null">
      <a-list item-layout="horizontal" :data-source="detailData">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta v-if="!item.json" :description="item.description">
              <template #title>
                <h4>{{ item.title }}</h4>
              </template>
            </a-list-item-meta>
            <template v-else>
              <h4>{{ item.title }}</h4>
              <json-viewer :value="item.value" :expand-depth="4" copyable sort></json-viewer>
            </template>

          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </div>
</template>



<script setup lang="ts">
import JsonViewer from 'vue3-json-viewer'
import { getOperationLogList } from '@/api/operation-log'
import { getMonitorOperateTypeList } from '@/api/monitor'
import { getNodeListAll } from '@/api/node'
import { getUserListAll } from '@/api/user/user'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { IPageQuery } from '@/interface/common';

const loading = ref(false);
const list = ref<any[]>([]);
const nodeList = ref<any[]>([]);
const nodeMap = ref<Record<string, any>>({});
const userList = ref<any[]>([]);
const listQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY });
const methodFeature = ref<{ title: string; value: string }[]>([]);
const classFeature = ref<{ title: string; value: string }[]>([]);
const methodFeatureMap = ref<Record<string, string>>({});
const classFeatureMap = ref<Record<string, string>>({});
const detailVisible = ref(false);
const detailData = ref([]);


const columns = [
  {
    title: '操作者',
    dataIndex: 'username',
    ellipsis: true,
  },
  { title: 'IP', dataIndex: 'ip' },
  {
    title: '节点',
    dataIndex: 'nodeId',
    width: 120,
    ellipsis: true,
  },
  {
    title: '数据名称',
    dataIndex: 'dataName',
    ellipsis: true,
  },
  {
    title: '工作空间名',
    dataIndex: 'workspaceName',
    ellipsis: true,

  },
  {
    title: '操作功能',
    dataIndex: 'classFeature',
    ellipsis: true,

  },
  {
    title: '操作方法',
    dataIndex: 'methodFeature',
    ellipsis: true,
  },
  {
    title: '状态码',
    dataIndex: 'optStatus',
    width: 90,
    scopedSlots: { customRender: 'optStatus' }
  },
  {
    title: '操作时间',
    dataIndex: 'createTimeMillis',
    sorter: true,
    width: '170px'
  },
  {
    title: '操作',
    align: 'center',
    dataIndex: 'operation',
    width: 80
  }
];


// 加载 node
getNodeListAll().then((res) => {
  if (res.code === 200) {
    nodeList.value = res.data;
    res.data.forEach((item: { id: string | number; name: any; }) => {
      nodeMap.value[item.id] = item.name;
    });
  }
});


// 加载数据
const loadData = (pointerEvent?: { altKey: any; ctrlKey: any; } | undefined) => {
  loading.value = true;
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page;
  getOperationLogList(listQuery.value).then((res) => {
    if (res.code === 200) {
      list.value = res.data.result;
      listQuery.value.total = res.data.total;
    }
    loading.value = false;
  });
};

// 加载用户列表
const loadUserList = () => {
  getUserListAll().then((res) => {
    if (res.code === 200) {
      userList.value = res.data;
    }
  });
};

// 查看详情
const handleDetail = (record: any) => {
  detailData.value = [];
  detailVisible.value = true;
  const temp = { ...record };
  try {
    temp.reqData = JSON.parse(temp.reqData);
  } catch (e) {
    console.error(e);
  }
  try {
    temp.resultMsg = JSON.parse(temp.resultMsg);
  } catch (e) {
    console.error(e);
  }
  detailData.value.push({ title: '数据Id', description: temp.dataId });
  detailData.value.push({ title: '浏览器标识', description: temp.userAgent });
  detailData.value.push({ title: '请求参数', json: true, value: temp.reqData });
  detailData.value.push({ title: '响应结果', json: true, value: temp.resultMsg });
};

// 初始化加载数据
onMounted(() => {
  loadData();
  loadUserList();

  getMonitorOperateTypeList().then((res) => {
    methodFeature.value = res.data.methodFeature;
    classFeature.value = res.data.classFeature;
    res.data.methodFeature.forEach((item: { value: string | number; title: any; }) => {
      methodFeatureMap.value[item.value] = item.title;
    });
    res.data.classFeature.forEach((item: { value: string | number; title: any; }) => {
      classFeatureMap.value[item.value] = item.title;
    });
  });
});


const pagination = COMPUTED_PAGINATION(listQuery.value);


const onChangeTime = (value, dateString) => {
  listQuery.value.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`;
};

// 分页、排序、筛选变化时触发
const change = (pagination: any, filters: any, sorter: any) => {
  listQuery.value = CHANGE_PAGE(listQuery.value, { pagination, sorter });
  loadData();
};



</script>
<style scoped></style>
