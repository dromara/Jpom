<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.dispatch.logRead.2ee8e7cc')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$t('pages.dispatch.logRead.3371c7a0')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.dispatch.logRead.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.dispatch.logRead.52ca17c1') }}</a-button>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">{{
              $t('pages.dispatch.logRead.64603c01')
            }}</a-button>
            <a-button type="primary" size="small" @click="handleLogRead(record)">{{
              $t('pages.dispatch.logRead.1ba84995')
            }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('pages.dispatch.logRead.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60%"
      :title="$t('pages.dispatch.logRead.4621082b')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.dispatch.logRead.2ee8e7cc')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.dispatch.logRead.ce9b8b40')" />
        </a-form-item>
        <a-form-item :label="$t('pages.dispatch.logRead.89553def')" required>
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in temp.projectList" :key="index">
              <a-col :span="11">
                <span>{{ $t('pages.dispatch.logRead.602a0a5e') }} </span>
                <a-select
                  v-model:value="item.nodeId"
                  style="width: 80%"
                  :placeholder="$t('pages.dispatch.logRead.2c33c91c')"
                  @change="
                    () => {
                      temp = {
                        ...temp,
                        projectList: temp.projectList.map((item, index1) => {
                          if (index1 === index && item.projectId) {
                            return Object.assign(item, { projectId: undefined })
                          }
                          return item
                        })
                      }
                    }
                  "
                >
                  <a-select-option
                    v-for="nodeItem in nodeList"
                    :key="nodeItem.id"
                    :disabled="
                      !nodeProjectList[nodeItem.id] ||
                      !nodeProjectList[nodeItem.id].projects ||
                      nodeItem.openStatus !== 1
                    "
                  >
                    {{ nodeItem.name }}
                  </a-select-option>
                </a-select>
              </a-col>
              <a-col :span="11">
                <span>{{ $t('pages.dispatch.logRead.4889a88f') }} </span>
                <a-select
                  v-model:value="item.projectId"
                  :disabled="!item.nodeId"
                  style="width: 80%"
                  :placeholder="`${$t('pages.dispatch.logRead.bf2bdd20')}`"
                >
                  <!-- <a-select-option value=""> 请先选择节点</a-select-option> -->
                  <template v-if="nodeProjectList[item.nodeId]">
                    <a-select-option
                      v-for="project in nodeProjectList[item.nodeId].projects"
                      :key="project.projectId"
                      :disabled="
                        temp.projectList.filter((item, nowIndex) => {
                          return (
                            item.nodeId === project.nodeId && item.projectId === project.projectId && nowIndex !== index
                          )
                        }).length > 0
                      "
                    >
                      {{ project.name }}
                    </a-select-option>
                  </template>
                </a-select>
              </a-col>
              <a-col :span="2">
                <a-button type="primary" danger @click="() => temp.projectList.splice(index, 1)"
                  ><DeleteOutlined
                /></a-button>
              </a-col>
            </a-row>

            <a-button type="primary" @click="() => temp.projectList.push({})">{{
              $t('pages.dispatch.logRead.52ca17c1')
            }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 实时阅读 -->
    <CustomDrawer
      v-if="logReadVisible"
      destroy-on-close
      placement="right"
      :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :open="logReadVisible"
      @close="
        () => {
          logReadVisible = false
          loadData()
        }
      "
    >
      <template #title>
        {{ $t('pages.dispatch.logRead.a602ffb5') }}
        {{ temp.cacheData && temp.cacheData.logFile ? ':' + temp.cacheData.logFile : '' }}
      </template>
      <logReadView
        v-if="logReadVisible"
        :data="temp"
        @change-title="
          (logFile) => {
            const cacheData = { ...temp.cacheData, logFile: logFile }
            temp = { ...temp, cacheData: cacheData }
          }
        "
      ></logReadView>
    </CustomDrawer>
  </div>
</template>
<script>
import { deleteLogRead, editLogRead, getLogReadList } from '@/api/log-read'
import { getNodeListAll, getProjectListAll } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, itemGroupBy, parseTime } from '@/utils/const'
import { useGuideStore } from '@/stores/guide'
import { mapState } from 'pinia'
import logReadView from './logReadView'

export default {
  components: {
    logReadView
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      nodeList: [],
      nodeName: {},
      nodeProjectList: [],
      logReadVisible: false,
      temp: {},
      editVisible: false,
      columns: [
        {
          title: this.$t('pages.dispatch.logRead.bb769c1d'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('pages.dispatch.logRead.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          align: 'center',
          tooltip: true,
          width: 120
        },
        {
          title: this.$t('pages.dispatch.logRead.61164914'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => {
            if (!text || text === '0') {
              return ''
            }
            return parseTime(text)
          },
          width: 180
        },
        {
          title: this.$t('pages.dispatch.logRead.3bb962bf'),
          dataIndex: 'operation',
          ellipsis: true,

          width: 180,
          align: 'center'
        }
      ],

      rules: {
        name: [{ required: true, message: this.$t('pages.dispatch.logRead.b3c4e04b'), trigger: 'blur' }]
      },
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed']),
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
      getLogReadList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 加载节点以及项目
    loadNodeList() {
      return new Promise((resolve) => {
        this.loadNodeList2().then(() => {
          this.getProjectListAll().then(() => {
            resolve()
          })
        })
      })
    },
    // 加载节点以及项目
    loadNodeList2() {
      return new Promise((resolve) => {
        getNodeListAll().then((res) => {
          if (res.code === 200) {
            this.nodeList = res.data
            this.nodeName = res.data.groupBy((item) => item.id)
            resolve()
          }
        })
      })
    },
    // 加载用户列表
    getProjectListAll() {
      return new Promise((resolve) => {
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            this.nodeProjectList = itemGroupBy(res.data, 'nodeId', 'id', 'projects').groupBy((item) => item.id)
            resolve()
            // console.log(this.nodeList);
            // console.log(this.nodeProjectList);
          }
        })
      })
    },
    // 新增
    handleAdd() {
      this.temp = {
        projectList: []
      }
      this.loadNodeList().then(() => {
        this.editVisible = true
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record, {
        projectList: JSON.parse(record.nodeProject)
      })

      this.loadNodeList().then(() => {
        this.editVisible = true
      })
    },
    handleEditOk() {
      // 检验表单
      this.$refs['editForm'].validate().then(() => {
        const temp = Object.assign({}, this.temp)
        temp.projectList = temp.projectList?.filter((item) => {
          return item.nodeId && item.projectId
        })
        if (!temp.projectList || !temp.projectList.length) {
          $notification.warn({
            message: this.$t('pages.dispatch.logRead.6d8491c8')
          })
          return false
        }
        // console.log(temp);
        this.confirmLoading = true
        editLogRead(temp)
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
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('pages.dispatch.logRead.b22d55a0'),
        zIndex: 1009,
        content: this.$t('pages.dispatch.logRead.347d0d00'),
        okText: this.$t('pages.dispatch.logRead.e8e9db25'),
        cancelText: this.$t('pages.dispatch.logRead.b12468e9'),
        onOk: () => {
          return deleteLogRead(record.id).then((res) => {
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
    // 打开阅读
    handleLogRead(record) {
      // console.log(record);
      this.temp = Object.assign({}, record, {
        projectList: JSON.parse(record.nodeProject),
        cacheData: JSON.parse(record.cacheData || '{}')
      })
      this.logReadVisible = true
      //
    }
  }
}
</script>
