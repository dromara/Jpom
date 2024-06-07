<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :columns="columns"
      size="middle"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="changePage"
    >
      <template #title>
        <a-space>
          <a-input
            v-model:value="listQuery['id']"
            :placeholder="$t('pages.system.workspace-list.844d3ebe')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('pages.system.workspace-list.d83f3e09')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.group"
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
            allow-clear
            :placeholder="$t('pages.system.workspace-list.b1765e98')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.clusterInfoId"
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
            allow-clear
            :placeholder="$t('pages.system.workspace-list.17b26f41')"
            class="search-input-item"
          >
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.system.workspace-list.cb5a8131')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.system.workspace-list.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.system.workspace-list.7d46652a') }}</a-button>
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $t('pages.system.workspace-list.69594ec6') }}</li>
                <li>{{ $t('pages.system.workspace-list.2604809e') }}</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'description'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'clusterInfoId'">
          <a-tooltip
            placement="topLeft"
            :title="
              (clusterList.find((item) => {
                return item.id === text
              }) &&
                clusterList.find((item) => {
                  return item.id === text
                }).name) ||
              ''
            "
          >
            <span>{{
              clusterList.find((item) => {
                return item.id === text
              }) &&
              clusterList.find((item) => {
                return item.id === text
              }).name
            }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEdit(record)">{{
              $t('pages.system.workspace-list.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" @click="configMeun(record)">{{
              $t('pages.system.workspace-list.9850d888')
            }}</a-button>
            <a-button size="small" type="primary" @click="configWhiteDir(record)">{{
              $t('pages.system.workspace-list.fdc31c9f')
            }}</a-button>
            <a-button size="small" type="primary" @click="viewEnvVar(record)">{{
              $t('pages.system.workspace-list.f69fec3b')
            }}</a-button>

            <a-tooltip v-if="record.id === 'DEFAULT'" :title="$t('pages.system.workspace-list.517e0aa2')">
              <a-button size="small" type="primary" danger :disabled="true">{{
                $t('pages.system.workspace-list.bf510d47')
              }}</a-button>
            </a-tooltip>
            <a-button v-else size="small" type="primary" danger @click="handleDelete(record)">{{
              $t('pages.system.workspace-list.bf510d47')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      v-model:open="editVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="$t('pages.system.workspace-list.d2afd162')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-alert :message="$t('pages.system.workspace-list.30376d6d')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('pages.system.workspace-list.6472482b') }}</li>
            <li>{{ $t('pages.system.workspace-list.cafa023a') }}</li>
            <li>{{ $t('pages.system.workspace-list.8fa12a0f') }}</li>
          </ul>
        </template>
      </a-alert>
      <a-form
        ref="editForm"
        :rules="rules"
        :model="temp"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 14 }"
        style="padding-top: 15px"
      >
        <a-form-item :label="$t('pages.system.workspace-list.3e34ec28')" name="name">
          <a-input
            v-model:value="temp.name"
            :max-length="50"
            :placeholder="$t('pages.system.workspace-list.d83f3e09')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.system.workspace-list.1f1383b6')" name="clusterInfoId">
          <a-select
            v-model:value="temp.clusterInfoId"
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
            allow-clear
            :placeholder="$t('pages.system.workspace-list.1f1383b6')"
          >
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('pages.system.workspace-list.b1765e98')" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('pages.system.workspace-list.c50ead9c')"
            :select-placeholder="$t('pages.system.workspace-list.c385f859')"
          >
          </custom-select>
        </a-form-item>

        <a-form-item :label="$t('pages.system.workspace-list.4b2e093e')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="5"
            :placeholder="$t('pages.system.workspace-list.d8b6196d')"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 环境变量 -->
    <a-modal
      v-model:open="envVarListVisible"
      destroy-on-close
      :title="`${temp.name} ${$t('pages.system.workspace-list.58f9d869')}`"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <workspaceEnv v-if="envVarListVisible" ref="workspaceEnv" :workspace-id="temp.id" />
    </a-modal>
    <!-- 工作空间菜单 -->
    <a-modal
      v-model:open="configMenuVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="`${temp.name} ${$t('pages.system.workspace-list.2da38372')}`"
      :mask-closable="false"
      @ok="onSubmitMenus"
    >
      <a-form ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-alert
            :message="`${$t('pages.system.workspace-list.a1f2ceff')}`"
            style="margin-top: 10px; margin-bottom: 20px; width: 100%"
            banner
          />
          <a-col :span="20">
            <a-card :title="$t('pages.system.workspace-list.4a16a53c')" :bordered="true">
              <a-tree
                v-if="menusConfigData.serverMenus"
                v-model:checkedKeys="menusConfigData.serverMenuKeys"
                show-icon
                checkable
                :tree-data="menusConfigData.serverMenus"
                :field-names="replaceFields"
              >
                <template #icon="{ dataRef }">
                  <icon :type="dataRef.icon_v3" />
                </template>
              </a-tree>
            </a-card>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
    <!-- 配置授权目录 -->
    <a-modal
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('pages.system.workspace-list.fcce7810')}`"
      :footer="null"
      width="60vw"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        :workspace-id="temp.id"
        @cancel="
          () => {
            configDir = false
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 删除工作空间检查 -->
    <a-modal
      v-model:open="preDeleteVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="`${$t('pages.system.workspace-list.1c5ce361')}`"
      :mask-closable="false"
      @ok="handleDeleteOk"
      @cancel="
        () => {
          preDeleteVisible = false
        }
      "
    >
      <a-alert :message="$t('pages.system.workspace-list.5bba45b7')" type="error" show-icon>
        <template #description>
          {{ $t('pages.system.workspace-list.8d0c613e') }},{{ $t('pages.system.workspace-list.a87846a2') }}</template
        >
      </a-alert>

      <a-tree :tree-data="treeData" default-expand-all :field-names="preDeleteReplaceFields" :show-line="true">
        <template #title="{ dataRef }">
          <CheckOutlined v-if="dataRef.count === 0" style="color: green" />

          <CloseOutlined v-else style="color: red" />
          {{ dataRef.name }}

          <template v-if="dataRef.count > 0">
            <a-tag color="pink">
              {{ $t('pages.system.workspace-list.194bc8b9') }} {{ dataRef.count }}
              {{ $t('pages.system.workspace-list.804a144d') }}
            </a-tag>

            <a-tag v-if="dataRef.workspaceBind === 2" color="cyan">{{
              $t('pages.system.workspace-list.d6fc71ea')
            }}</a-tag>
            <a-tag v-else-if="dataRef.workspaceBind === 3" color="blue">{{
              $t('pages.system.workspace-list.9f7d21f6')
            }}</a-tag>
            <a-tag v-else color="purple">{{ $t('pages.system.workspace-list.fcda4526') }}</a-tag>
          </template>
        </template>
      </a-tree>
    </a-modal>
  </div>
</template>

<script>
import {
  deleteWorkspace,
  preDeleteWorkspace,
  editWorkSpace,
  getWorkSpaceList,
  getMenusConfig,
  saveMenusConfig,
  getWorkSpaceGroupList
} from '@/api/workspace'
import Icon from '@/components/Icon'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { listClusterAll } from '@/api/system/cluster'
import workspaceEnv from './workspace-env.vue'
import CustomSelect from '@/components/customSelect'
import whiteList from '@/pages/dispatch/white-list.vue'
export default {
  components: {
    workspaceEnv,
    CustomSelect,
    whiteList,
    Icon
    // VNodes: {
    //   props: {
    //     vnodes: {
    //       type: Object,
    //       required: true
    //     }
    //   },
    //   render() {
    //     return this.vnodes
    //   }
    // }
  },
  data() {
    return {
      loading: true,
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editVisible: false,
      envVarListVisible: false,
      temp: {},
      columns: [
        {
          title: this.$t('pages.system.workspace-list.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.system.workspace-list.4b2e093e'),
          dataIndex: 'description',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.system.workspace-list.12d0e469'),
          dataIndex: 'group',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('pages.system.workspace-list.17b26f41'),
          dataIndex: 'clusterInfoId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.system.workspace-list.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('pages.system.workspace-list.f5b90169'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.system.workspace-list.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('pages.system.workspace-list.3bb962bf'),
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',

          width: '320px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('pages.system.workspace-list.afb740ab'), trigger: 'blur' }],
        description: [{ required: true, message: this.$t('pages.system.workspace-list.fa2192f9'), trigger: 'blur' }],
        clusterInfoId: [{ required: true, message: this.$t('pages.system.workspace-list.34b97d6a'), trigger: 'blur' }]
      },
      configMenuVisible: false,
      replaceFields: { children: 'childs', title: 'title', key: 'id' },
      menusConfigData: {},
      groupList: [],
      configDir: false,
      preDeleteVisible: false,
      preDeleteReplaceFields: {
        children: 'children',
        title: 'name',
        key: 'id'
      },
      treeData: [],
      clusterList: [],
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
    this.loadClusterList()
  },
  methods: {
    // 获取所有集群
    loadClusterList() {
      return new Promise((resolve) => {
        listClusterAll().then((res) => {
          if (res.data && res.code === 200) {
            this.clusterList = res.data || []
            resolve()
          }
        })
      })
    },
    // 获取所有的分组
    loadGroupList() {
      getWorkSpaceGroupList().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      getWorkSpaceList(this.listQuery)
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

    viewEnvVar(record) {
      this.temp = Object.assign({}, record)
      // this.envTemp = {
      //   workspaceId: this.temp.id,
      // };
      // this.envVarListQuery.workspaceId = record.id;
      this.envVarListVisible = true
      this.$nextTick(() => {
        this.$refs.workspaceEnv.loadDataEnvVar()
      })
    },
    handleAdd() {
      this.loadGroupList()
      this.temp = {}
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
      this.loadClusterList().then(() => {
        if (this.clusterList.length === 1) {
          this.temp = { ...this.temp, clusterInfoId: this.clusterList[0].id }
        }
        this.editVisible = true
      })
    },
    handleEdit(record) {
      this.loadGroupList()
      this.$refs['editForm'] && this.$refs['editForm'].resetFields()
      this.loadClusterList().then(() => {
        const defData = {}
        if (this.clusterList.length === 1) {
          defData.clusterInfoId = this.clusterList[0].id
        }
        this.temp = Object.assign({}, record, defData)
        this.editVisible = true
      })
    },
    handleEditOk() {
      this.$refs['editForm'].validate().then(() => {
        editWorkSpace(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })

            this.editVisible = false
            this.loadData()
          }
        })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 删除
    handleDelete(record) {
      this.temp = { ...record }

      preDeleteWorkspace(this.temp.id).then((res) => {
        this.treeData = res.data?.children || []
        this.preDeleteVisible = true
      })
    },
    handleDeleteOk() {
      // 删除
      this.confirmLoading = true
      deleteWorkspace(this.temp.id)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.preDeleteVisible = false
            this.loadData()
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    configMeun(record) {
      this.temp = Object.assign({}, record)

      // 加载菜单配置信息
      // loadMenusConfig(id) {},
      getMenusConfig({
        workspaceId: record.id
      }).then((res) => {
        if (res.code !== 200) {
          return
        }
        this.menusConfigData = res.data

        this.menusConfigData.serverMenus = this.menusConfigData?.serverMenus.map((item) => {
          // item.scopedSlots = { icon: 'custom' }
          item.childs?.map((item2) => {
            item2.id = item.id + ':' + item2.id
            return item2
          })
          return item
        })

        if (!this.menusConfigData?.serverMenuKeys) {
          //
          const serverMenuKeys = []
          this.menusConfigData.serverMenus.forEach((item) => {
            serverMenuKeys.push(item.id)
            if (item.childs) {
              item.childs.forEach((item2) => {
                serverMenuKeys.push(item2.id)
              })
            }
          })
          this.menusConfigData = {
            ...this.menusConfigData,
            serverMenuKeys: serverMenuKeys
          }
        }

        this.configMenuVisible = true
      })
    },
    onSubmitMenus() {
      this.confirmLoading = true
      saveMenusConfig({
        serverMenuKeys: this.menusConfigData.serverMenuKeys.join(','),

        workspaceId: this.temp.id
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.configMenuVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 配置节点授权
    configWhiteDir(record) {
      this.temp = Object.assign({}, record)
      this.configDir = true
    }
  }
}
</script>
