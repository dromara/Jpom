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
            :placeholder="$t('i18n_66e9ea5488')"
            class="search-input-item"
            @press-enter="loadData"
          />

          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
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
            <a-button type="primary" size="small" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button type="primary" size="small" @click="handleLogRead(record)">{{ $t('i18n_607e7a4f37') }}</a-button>
            <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
              $t('i18n_2f4aaddde3')
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
      :title="$t('i18n_7e58312632')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_66e9ea5488')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_7c9bb61536')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_83f25dbaa0')" required>
          <a-space direction="vertical" style="width: 100%">
            <a-row v-for="(item, index) in temp.projectList" :key="index">
              <a-col :span="11">
                <span>{{ $t('i18n_9b280a6d2d') }} </span>
                <a-select
                  v-model:value="item.nodeId"
                  style="width: 80%"
                  :placeholder="$t('i18n_f8a613d247')"
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
                <span>{{ $t('i18n_8198e4461a') }} </span>
                <a-select
                  v-model:value="item.projectId"
                  :disabled="!item.nodeId"
                  style="width: 80%"
                  :placeholder="`${$t('i18n_9fc2e26bfa')}`"
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

            <a-button type="primary" @click="() => temp.projectList.push({})">{{ $t('i18n_66ab5e9f24') }}</a-button>
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
        {{ $t('i18n_bfda12336c') }}
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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },

        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          align: 'center',
          tooltip: true,
          width: 120
        },
        {
          title: this.$t('i18n_1303e638b5'),
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
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          ellipsis: true,

          width: 180,
          align: 'center'
        }
      ],

      rules: {
        name: [{ required: true, message: this.$t('i18n_679de60f71'), trigger: 'blur' }]
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
            message: this.$t('i18n_1a56bb2237')
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_9bbb6b5b75'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
