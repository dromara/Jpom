<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
          this.loadData()
        }
      "
      :scroll="{
        x: 'max-content'
      }"
      rowKey="id"
    >
      <template #title>
        <a-space direction="vertical">
          <div>
            <template v-for="(val, key) in groupMap" :key="key">
              <span>{{ key }}：</span>
              <template v-for="(tag, index) in val" :key="`${tag.id}_${key}`">
                <a-tag :color="`${index === 0 ? 'blue' : 'orange'}`">
                  {{ tag.name }}
                </a-tag>
              </template>
            </template>
          </div>
          <div v-if="groupList.filter((item) => !groupMap[item]).length">
            未绑定集群的分组：
            <template v-for="(item, index) in groupList">
              <a-tag v-if="!groupMap[item]">{{ item }}</a-tag>
            </template>
          </div>
          <a-space>
            <a-input
              v-model:value="listQuery['%name%']"
              @pressEnter="loadData"
              placeholder="集群名称"
              allowClear
              class="search-input-item"
            />
            <a-input
              v-model:value="listQuery['%url%']"
              @pressEnter="loadData"
              placeholder="集群地址"
              allowClear
              class="search-input-item"
            />
            <a-input
              v-model:value="listQuery['%localHostName%']"
              @pressEnter="loadData"
              placeholder="主机名"
              allowClear
              class="search-input-item"
            />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>

            <a-tooltip>
              <template #title>
                <ul>
                  <li>
                    集群不能手动创建，创建需要多个服务端使用通一个数据库，并且配置不同的集群 id 来自动创建集群信息
                  </li>
                  <li>新集群需要手动配置集群管理资产分组、集群访问地址</li>
                  <li>新机器还需要绑定工作空间，因为我们建议将不同集群资源分配到不同的工作空间来管理</li>
                </ul>
              </template>
              <QuestionCircleOutlined />
            </a-tooltip>
          </a-space>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'url'">
          <a-tooltip
            :title="`集群名称：${record.name || ''}/地址：${record.url || ''}/状态消息：${record.statusMsg || ''}`"
          >
            <a-button v-if="record.url" type="link" @click="openUrl(record.url)" size="small">
              {{ text }}
            </a-button>
            <span v-else>{{ record.statusMsg }}</span>
            <!-- -->
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      title="编辑集群"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="temp.name" :maxLength="50" placeholder="工作空间名称" />
        </a-form-item>
        <a-form-item label="关联分组" name="linkGroups">
          <template #help>
            关联分组主要用于资产监控来实现不同服务端执行不同分组下面的资产监控
            <div style="color: red">注意：同一个分组不建议被多个集群绑定</div>
          </template>
          <a-select
            show-search
            mode="multiple"
            option-filter-prop="children"
            v-model:value="temp.linkGroups"
            allowClear
            placeholder="关联分组"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="集群地址" name="url">
          <template #help> 集群地址主要用于切换工作空间自动跳转到对应的集群 </template>
          <a-input v-model:value="temp.url" placeholder="集群访问地址" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getClusterList, deleteCluster, listLinkGroups, editCluster } from '@/api/system/cluster'
export default {
  data() {
    return {
      loading: false,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        {
          title: '安装ID',
          dataIndex: 'id',
          ellipsis: true,
          width: '100px',
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '集群ID',
          dataIndex: 'clusterId',
          ellipsis: true,
          width: '100px',
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 200,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '集群地址',
          dataIndex: 'url',
          ellipsis: true,
          width: 200,
          scopedSlots: { customRender: 'url' }
        },
        {
          title: '集群主机名',
          dataIndex: 'localHostName',
          ellipsis: true,
          width: '100px',
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '版本号',
          dataIndex: 'jpomVersion',
          ellipsis: true,
          width: '100px',
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '最后心跳时间',
          dataIndex: 'lastHeartbeat',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyUser' },
          width: 120
        },

        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',

          width: '120px'
        }
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: '请输入集群名称', trigger: 'blur' }],
        linkGroups: [{ required: true, message: '请输入选择关联分组', trigger: 'blur' }]
        // url: [{ required: true, message: "请输入集群访问地址", trigger: "blur" }],
      },
      editVisible: false,
      temp: {},
      groupList: [],
      groupMap: {},
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    parseTime,
    CHANGE_PAGE,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getClusterList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除该集群信息么？1',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            deleteCluster(record.id)
              .then((res) => {
                if (res.code === 200) {
                  this.$notification.success({
                    message: res.msg
                  })
                  this.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 获取所有的分组
    loadGroupList() {
      listLinkGroups().then((res) => {
        if (res.data) {
          this.groupList = res.data.linkGroups || []
          this.groupMap = res.data.groupMap || {}
        }
      })
    },
    // 编辑
    handleEdit(record) {
      this.loadGroupList()
      this.temp = Object.assign({}, record, {
        linkGroups: (record.linkGroup || '').split(',').filter((item) => item)
      })
      this.editVisible = true
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        const newData = { ...this.temp }
        const linkGroups = newData.linkGroups
        if (!linkGroups) {
          $notification.error({
            message: '请选择集群关联分组'
          })
          return false
        }
        delete newData.linkGroups
        newData.linkGroup = linkGroups.join(',')
        this.confirmLoading = true
        editCluster(newData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editForm'].resetFields()
              this.editVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    //
    openUrl(url) {
      window.open(url)
    }
  }
}
</script>
