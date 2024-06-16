<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      :data-source="envVarList"
      size="middle"
      :loading="envVarLoading"
      :columns="envVarColumns"
      :pagination="envVarPagination"
      bordered
      :scroll="{
        x: 'max-content'
      }"
      @change="changeListeEnvVar"
    >
      <template #title>
        <a-space>
          <a-input
            v-model:value="envVarListQuery['%name%']"
            :placeholder="$t('pages.system.workspace-env.3e34ec28')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-input
            v-model:value="envVarListQuery['%value%']"
            :placeholder="$t('pages.system.workspace-env.c1c14269')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-input
            v-model:value="envVarListQuery['%description%']"
            :placeholder="$t('pages.system.workspace-env.4b2e093e')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-button type="primary" @click="loadDataEnvVar">{{ $t('pages.system.workspace-env.53c2763c') }}</a-button>
          <a-button type="primary" @click="addEnvVar">{{ $t('pages.system.workspace-env.7d46652a') }}</a-button>
          <a-tooltip>
            <template #title>
              <div>{{ $t('pages.system.workspace-env.e188b304') }}</div>
              <div>{{ $t('pages.system.workspace-env.1601450d') }}</div>
              <div>{{ $t('pages.system.workspace-env.3abecbf2') }}</div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'value'">
          <a-tooltip
            placement="topLeft"
            :title="record.privacy === 1 ? $t('pages.system.workspace-env.882745f1') : text"
          >
            <EyeInvisibleOutlined v-if="record.privacy === 1" />
            <span v-else>{{ text }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'workspaceId'">
          <span>{{
            text === 'GLOBAL' ? $t('pages.system.workspace-env.b21bb9ac') : $t('pages.system.workspace-env.919267cc')
          }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEnvEdit(record)">{{
              $t('pages.system.workspace-env.64603c01')
            }}</a-button>
            <a-button size="small" type="primary" :disabled="record.privacy === 1" @click="handleTrigger(record)">{{
              $t('pages.system.workspace-env.e81c0988')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleEnvDelete(record)">{{
              $t('pages.system.workspace-env.dd20d11c')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 环境变量编辑区 -->
    <CustomModal
      v-if="editEnvVisible"
      v-model:open="editEnvVisible"
      :confirm-loading="confirmLoading"
      :title="$t('pages.system.workspace-env.f48b76ed')"
      width="50vw"
      :mask-closable="false"
      @ok="handleEnvEditOk"
    >
      <a-form ref="editEnvForm" :rules="rulesEnv" :model="envTemp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.system.workspace-env.3e34ec28')" name="name">
          <a-input
            v-model:value="envTemp.name"
            :max-length="50"
            :placeholder="$t('pages.system.workspace-env.ea01237a')"
          />
        </a-form-item>
        <a-form-item
          :label="$t('pages.system.workspace-env.c1c14269')"
          :prop="`${envTemp.privacy === 1 ? '' : 'value'}`"
        >
          <a-textarea
            v-model:value="envTemp.value"
            :rows="5"
            :placeholder="$t('pages.system.workspace-env.4470477e')"
          />
        </a-form-item>
        <a-form-item :label="$t('pages.system.workspace-env.4b2e093e')" name="description">
          <a-textarea
            v-model:value="envTemp.description"
            :max-length="200"
            :rows="5"
            :placeholder="$t('pages.system.workspace-env.bf97f128')"
          />
        </a-form-item>
        <a-form-item name="privacy">
          <template #label>
            <a-tooltip>
              {{ $t('pages.system.workspace-env.42a4f580') }}
              <template #title> {{ $t('pages.system.workspace-env.a7685d3b') }} </template>
              <QuestionCircleOutlined v-show="!envTemp.id" />
            </a-tooltip>
          </template>
          <a-switch
            :checked="envTemp.privacy === 1"
            :disabled="envTemp.id !== undefined"
            :checked-children="$t('pages.system.workspace-env.a4be6169')"
            :un-checked-children="$t('pages.system.workspace-env.93b5d49f')"
            @change="
              (checked) => {
                envTemp = { ...envTemp, privacy: checked ? 1 : 0 }
              }
            "
          />
        </a-form-item>
        <a-form-item>
          <template #label>
            <a-tooltip>
              {{ $t('pages.system.workspace-env.bcd969cb') }}
              <template #title> {{ $t('pages.system.workspace-env.dfe01b33') }}</template>
              <QuestionCircleOutlined v-show="!envTemp.id" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="envTemp.chooseNode"
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
            :placeholder="$t('pages.system.workspace-env.580e6c10')"
            mode="multiple"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </CustomModal>
    <!-- 触发器 -->
    <CustomModal
      v-if="triggerVisible"
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$t('pages.system.workspace-env.e81c0988')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('pages.system.workspace-env.5c0bb1c0')">
              <a-button type="primary" size="small" @click="resetTrigger">{{
                $t('pages.system.workspace-env.da1d2343')
              }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('pages.system.workspace-env.7d6738bd')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('pages.system.workspace-env.8179a313')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('pages.system.workspace-env.fb6b3414') }}</li>
                    <li>{{ $t('pages.system.workspace-env.1483947f') }}</li>
                    <li>{{ $t('pages.system.workspace-env.a8213c8f') }}</li>
                    <li>PUT {{ $t('pages.system.workspace-env.a32ecd9') }}</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }"
                    >{{ $t('pages.system.workspace-env.d93f2ea1') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                </template>
              </a-alert>

              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrlPost }">
                    {{ $t('pages.system.workspace-env.9f70798a') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>POST</a-tag> <span>{{ temp.triggerUrlPost }} </span>
                </template>
              </a-alert>
              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }">
                    {{ $t('pages.system.workspace-env.9f70798a') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>PUT</a-tag> <span>{{ temp.triggerUrl }} </span>
                </template>
              </a-alert>
            </a-space>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </CustomModal>
  </div>
</template>
<script>
import { deleteWorkspaceEnv, editWorkspaceEnv, getWorkspaceEnvList, getTriggerUrlWorkspaceEnv } from '@/api/workspace'
import { getNodeListAll } from '@/api/node'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

export default {
  props: {
    workspaceId: {
      type: String,
      default: ''
    },
    global: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      envVarLoading: false,
      envVarList: [],
      nodeList: [],
      envVarListQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editEnvVisible: false,
      envTemp: {},
      temp: {},
      envVarColumns: [
        {
          title: this.$t('pages.system.workspace-env.3e34ec28'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.workspace-env.c1c14269'),
          dataIndex: 'value',
          ellipsis: true
        },

        {
          title: this.$t('pages.system.workspace-env.4b2e093e'),
          dataIndex: 'description',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.system.workspace-env.916db24b'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: 120
        },
        {
          title: this.$t('pages.system.workspace-env.8a2fc9dd'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('pages.system.workspace-env.a2b40316'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          sorter: true,
          width: '180px'
        },
        {
          title: this.$t('pages.system.workspace-env.3bb962bf'),
          dataIndex: 'operation',
          align: 'center',

          width: '200px'
        }
      ],

      // 表单校验规则
      rulesEnv: {
        name: [{ required: true, message: this.$t('pages.system.workspace-env.95105178'), trigger: 'blur' }],
        description: [{ required: true, message: this.$t('pages.system.workspace-env.c086832a'), trigger: 'blur' }],
        value: [{ required: true, message: this.$t('pages.system.workspace-env.aa013720'), trigger: 'blur' }]
      },
      triggerVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    envVarPagination() {
      return COMPUTED_PAGINATION(this.envVarListQuery)
    }
  },
  mounted() {
    this.loadDataEnvVar()
  },
  methods: {
    loadDataEnvVar(pointerEvent) {
      this.envVarLoading = true

      this.envVarListQuery.workspaceId = this.workspaceId + (this.global ? ',GLOBAL' : '')
      this.envVarListQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.envVarListQuery.page
      getWorkspaceEnvList(this.envVarListQuery).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data.result
          this.envVarListQuery.total = res.data.total
        }
        this.envVarLoading = false
      })
    },
    addEnvVar() {
      this.envTemp = {
        workspaceId: this.workspaceId
      }

      this.editEnvVisible = true
      this.$refs['editEnvForm'] && this.$refs['editEnvForm'].resetFields()
      this.getAllNodeList(this.envTemp.workspaceId)
    },
    handleEnvEdit(record) {
      this.envTemp = Object.assign({}, record)
      this.envTemp.workspaceId = this.workspaceId
      this.envTemp = {
        ...this.envTemp,
        chooseNode: record.nodeIds ? record.nodeIds.split(',') : []
      }
      this.editEnvVisible = true
      this.getAllNodeList(this.envTemp.workspaceId)
    },
    handleEnvEditOk() {
      this.$refs['editEnvForm'].validate().then(() => {
        this.envTemp.nodeIds = this.envTemp?.chooseNode?.join(',')
        this.confirmLoading = true
        editWorkspaceEnv(this.envTemp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editEnvForm'].resetFields()
              this.editEnvVisible = false
              this.loadDataEnvVar()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    //
    handleEnvDelete(record) {
      $confirm({
        title: this.$t('pages.system.workspace-env.2c32d62b'),
        zIndex: 1009,
        content: this.$t('pages.system.workspace-env.987c2cd6'),
        okText: this.$t('pages.system.workspace-env.e8e9db25'),
        cancelText: this.$t('pages.system.workspace-env.b12468e9'),
        onOk: () => {
          return deleteWorkspaceEnv({
            id: record.id,
            workspaceId: this.workspaceId
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadDataEnvVar()
            }
          })
        }
      })
    },
    // 获取所有节点
    getAllNodeList(workspaceId) {
      getNodeListAll({
        workspaceId: workspaceId
      }).then((res) => {
        this.nodeList = res.data || []
      })
    },
    changeListeEnvVar(pagination, filters, sorter) {
      this.envVarListQuery = CHANGE_PAGE(this.envVarListQuery, {
        pagination,
        sorter
      })

      this.loadDataEnvVar()
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign({}, record)

      getTriggerUrlWorkspaceEnv({
        id: record.id,
        workspaceId: record.workspaceId
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrlWorkspaceEnv({
        id: this.temp.id,
        rest: 'rest',
        workspaceId: this.temp.workspaceId
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerUrl = `${location.protocol}//${location.host}${res.data.triggerUrl}`
      this.temp.triggerUrlPost = `${this.temp.triggerUrl}?value=xxxxx`
      this.temp = { ...this.temp }
    }
  }
}
</script>
