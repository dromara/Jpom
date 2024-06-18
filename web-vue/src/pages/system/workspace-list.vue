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
            :placeholder="$t('i18n_ab7f78ba4c')"
            allow-clear
            class="search-input-item"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            :placeholder="$t('i18n_6a588459d0')"
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
            :placeholder="$t('i18n_829abe5a8d')"
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
            :placeholder="$t('i18n_85fe5099f6')"
            class="search-input-item"
          >
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('i18n_4838a3bd20')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $t('i18n_da509a213f') }}</li>
                <li>{{ $t('i18n_97cb3c4b2e') }}</li>
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
            <a-button size="small" type="primary" @click="handleEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" @click="configMeun(record)">{{ $t('i18n_4ccbdc5301') }}</a-button>
            <a-button size="small" type="primary" @click="configWhiteDir(record)">{{ $t('i18n_3d48c9da09') }}</a-button>
            <a-button size="small" type="primary" @click="viewEnvVar(record)">{{ $t('i18n_ddc7d28b7b') }}</a-button>

            <a-tooltip v-if="record.id === 'DEFAULT'" :title="$t('i18n_0c0633c367')">
              <a-button size="small" type="primary" danger :disabled="true">{{ $t('i18n_2f4aaddde3') }}</a-button>
            </a-tooltip>
            <a-button v-else size="small" type="primary" danger @click="handleDelete(record)">{{
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
      :title="$t('i18n_fa8e673c50')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <a-alert :message="$t('i18n_c8c6e37071')" type="info" show-icon>
        <template #description>
          <ul>
            <li>{{ $t('i18n_a89646d060') }}</li>
            <li>{{ $t('i18n_207243d77a') }}</li>
            <li>{{ $t('i18n_67aa2d01b9') }}</li>
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
        <a-form-item :label="$t('i18n_d7ec2d3fea')" name="name">
          <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_6a588459d0')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_9b5f172ebe')" name="clusterInfoId">
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
            :placeholder="$t('i18n_9b5f172ebe')"
          >
            <a-select-option v-for="item in clusterList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('i18n_829abe5a8d')" name="group">
          <custom-select
            v-model:value="temp.group"
            :data="groupList"
            :input-placeholder="$t('i18n_bd0362bed3')"
            :select-placeholder="$t('i18n_9cac799f2f')"
          >
          </custom-select>
        </a-form-item>

        <a-form-item :label="$t('i18n_3bdd08adab')" name="description">
          <a-textarea
            v-model:value="temp.description"
            :max-length="200"
            :rows="5"
            :placeholder="$t('i18n_4645575b77')"
          />
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 环境变量 -->
    <CustomModal
      v-if="envVarListVisible"
      v-model:open="envVarListVisible"
      destroy-on-close
      :title="`${temp.name} ${$t('i18n_f7e8d887d6')}`"
      width="80vw"
      :footer="null"
      :mask-closable="false"
    >
      <workspaceEnv v-if="envVarListVisible" ref="workspaceEnv" :workspace-id="temp.id" />
    </CustomModal>
    <!-- 工作空间菜单 -->
    <CustomModal
      v-if="configMenuVisible"
      v-model:open="configMenuVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="`${temp.name} ${$t('i18n_25182fb439')}`"
      :mask-closable="false"
      @ok="onSubmitMenus"
    >
      <a-form ref="editWhiteForm" :model="menusConfigData">
        <a-row type="flex" justify="center">
          <a-alert
            :message="`${$t('i18n_6228294517')}`"
            style="margin-top: 10px; margin-bottom: 20px; width: 100%"
            banner
          />
          <a-col :span="20">
            <a-card :title="$t('i18n_a75f781415')" :bordered="true">
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
    </CustomModal>
    <!-- 配置授权目录 -->
    <CustomModal
      v-if="configDir"
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('i18n_eee6510292')}`"
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
    </CustomModal>
    <!-- 删除工作空间检查 -->
    <CustomModal
      v-if="preDeleteVisible"
      v-model:open="preDeleteVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :title="`${$t('i18n_aec7b550e2')}`"
      :mask-closable="false"
      @ok="handleDeleteOk"
      @cancel="
        () => {
          preDeleteVisible = false
        }
      "
    >
      <a-alert :message="$t('i18n_a35740ae41')" type="error" show-icon>
        <template #description> {{ $t('i18n_6b46e2bfae') }},{{ $t('i18n_86b7eb5e83') }}</template>
      </a-alert>

      <a-tree :tree-data="treeData" default-expand-all :field-names="preDeleteReplaceFields" :show-line="true">
        <template #title="{ dataRef }">
          <CheckOutlined v-if="dataRef.count === 0" style="color: green" />

          <CloseOutlined v-else style="color: red" />
          {{ dataRef.name }}

          <template v-if="dataRef.count > 0">
            <a-tag color="pink">
              {{ $t('i18n_df9497ea98') }} {{ dataRef.count }}
              {{ $t('i18n_f932eff53e') }}
            </a-tag>

            <a-tag v-if="dataRef.workspaceBind === 2" color="cyan">{{ $t('i18n_686a19db6a') }}</a-tag>
            <a-tag v-else-if="dataRef.workspaceBind === 3" color="blue">{{ $t('i18n_9c3a3e1b03') }}</a-tag>
            <a-tag v-else color="purple">{{ $t('i18n_ab006f89e7') }}</a-tag>
          </template>
        </template>
      </a-tree>
    </CustomModal>
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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_3bdd08adab'),
          dataIndex: 'description',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_b37b786351'),
          dataIndex: 'group',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: this.$t('i18n_85fe5099f6'),
          dataIndex: 'clusterInfoId',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,

          width: 120
        },
        {
          title: this.$t('i18n_eca37cb072'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          fixed: 'right',
          align: 'center',

          width: '320px'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_b153126fc2'), trigger: 'blur' }],
        description: [{ required: true, message: this.$t('i18n_36b5d427e4'), trigger: 'blur' }],
        clusterInfoId: [{ required: true, message: this.$t('i18n_aad7450231'), trigger: 'blur' }]
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
