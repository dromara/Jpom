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
            :placeholder="$t('i18n_d7ec2d3fea')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-input
            v-model:value="envVarListQuery['%value%']"
            :placeholder="$t('i18n_fe7509e0ed')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-input
            v-model:value="envVarListQuery['%description%']"
            :placeholder="$t('i18n_3bdd08adab')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-button type="primary" @click="loadDataEnvVar">{{ $t('i18n_e5f71fc31e') }}</a-button>
          <a-button type="primary" @click="addEnvVar">{{ $t('i18n_66ab5e9f24') }}</a-button>
          <a-tooltip>
            <template #title>
              <div>{{ $t('i18n_969098605e') }}</div>
              <div>{{ $t('i18n_a34b91cdd7') }}</div>
              <div>{{ $t('i18n_102dbe1e39') }}</div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'value'">
          <a-tooltip placement="topLeft" :title="record.privacy === 1 ? $t('i18n_b12d003367') : text">
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
          <span>{{ text === 'GLOBAL' ? $t('i18n_2be75b1044') : $t('i18n_691b11e443') }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEnvEdit(record)">{{ $t('i18n_95b351c862') }}</a-button>
            <a-button size="small" type="primary" :disabled="record.privacy === 1" @click="handleTrigger(record)">{{
              $t('i18n_4696724ed3')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleEnvDelete(record)">{{
              $t('i18n_2f4aaddde3')
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
      :title="$t('i18n_a421ec6187')"
      width="50vw"
      :mask-closable="false"
      @ok="handleEnvEditOk"
    >
      <a-form ref="editEnvForm" :rules="rulesEnv" :model="envTemp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_d7ec2d3fea')" name="name">
          <a-input v-model:value="envTemp.name" :max-length="50" :placeholder="$t('i18n_7cb8d163bb')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_fe7509e0ed')" :prop="`${envTemp.privacy === 1 ? '' : 'value'}`">
          <a-textarea v-model:value="envTemp.value" :rows="5" :placeholder="$t('i18n_9a2ee7044f')" />
        </a-form-item>
        <a-form-item :label="$t('i18n_3bdd08adab')" name="description">
          <a-textarea
            v-model:value="envTemp.description"
            :max-length="200"
            :rows="5"
            :placeholder="$t('i18n_bcf83722c4')"
          />
        </a-form-item>
        <a-form-item name="privacy">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_03dcdf92f5') }}
              <template #title> {{ $t('i18n_cc617428f7') }} </template>
              <QuestionCircleOutlined v-show="!envTemp.id" />
            </a-tooltip>
          </template>
          <a-switch
            :checked="envTemp.privacy === 1"
            :disabled="envTemp.id !== undefined"
            :checked-children="$t('i18n_6d802636ab')"
            :un-checked-children="$t('i18n_a5f84fd99c')"
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
              {{ $t('i18n_6a6c857285') }}
              <template #title> {{ $t('i18n_6ffa21d235') }}</template>
              <QuestionCircleOutlined v-show="!envTemp.id" />
            </a-tooltip>
          </template>
          <template #help>{{ $t('i18n_267bf4bf76') }}</template>
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
            :placeholder="$t('i18n_a03ea1e864')"
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
      :title="$t('i18n_4696724ed3')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$t('i18n_01ad26f4a9')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$t('i18n_8dc09ebe97')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$t('i18n_947d983961')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_4ee2a8951d') }}</li>
                    <li>{{ $t('i18n_74c5c188ae') }}</li>
                    <li>{{ $t('i18n_15c46f7681') }}</li>
                    <li>PUT {{ $t('i18n_901de97cdb') }}</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }"
                    >{{ $t('i18n_4b386a7209') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                </template>
              </a-alert>

              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrlPost }">
                    {{ $t('i18n_db094db837') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>POST</a-tag> <span>{{ temp.triggerUrlPost }} </span>
                </template>
              </a-alert>
              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }">
                    {{ $t('i18n_db094db837') }}
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
          title: this.$t('i18n_d7ec2d3fea'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_fe7509e0ed'),
          dataIndex: 'value',
          ellipsis: true
        },

        {
          title: this.$t('i18n_3bdd08adab'),
          dataIndex: 'description',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('i18n_9baca0054e'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: 120
        },
        {
          title: this.$t('i18n_4705b88497'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          sorter: true,
          width: '180px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          dataIndex: 'operation',
          align: 'center',

          width: '200px'
        }
      ],

      // 表单校验规则
      rulesEnv: {
        name: [{ required: true, message: this.$t('i18n_e6e453d730'), trigger: 'blur' }],
        description: [{ required: true, message: this.$t('i18n_884ea031d3'), trigger: 'blur' }],
        value: [{ required: true, message: this.$t('i18n_85451d2eb5'), trigger: 'blur' }]
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
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_543a5aebc8'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
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
