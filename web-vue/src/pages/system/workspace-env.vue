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
            :placeholder="$tl('c.name')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-input
            v-model:value="envVarListQuery['%value%']"
            :placeholder="$tl('c.value')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-input
            v-model:value="envVarListQuery['%description%']"
            :placeholder="$tl('c.description')"
            allow-clear
            class="search-input-item"
            @press-enter="loadDataEnvVar"
          />
          <a-button type="primary" @click="loadDataEnvVar">{{ $tl('p.search') }}</a-button>
          <a-button type="primary" @click="addEnvVar">{{ $tl('p.add') }}</a-button>
          <a-tooltip>
            <template #title>
              <div>{{ $tl('p.envDescription') }}</div>
              <div>{{ $tl('p.envUsage') }}</div>
              <div>{{ $tl('p.scopeNote') }}</div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'value'">
          <a-tooltip placement="topLeft" :title="record.privacy === 1 ? $tl('p.privacyField') : text">
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
          <span>{{ text === 'GLOBAL' ? $tl('p.globalScope') : $tl('p.currentWorkspace') }}</span>
        </template>

        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleEnvEdit(record)">{{ $tl('p.edit') }}</a-button>
            <a-button size="small" type="primary" :disabled="record.privacy === 1" @click="handleTrigger(record)">{{
              $tl('c.trigger')
            }}</a-button>
            <a-button size="small" type="primary" danger @click="handleEnvDelete(record)">{{
              $tl('p.delete')
            }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 环境变量编辑区 -->
    <a-modal
      v-model:open="editEnvVisible"
      :confirm-loading="confirmLoading"
      :title="$tl('p.editEnv')"
      width="50vw"
      :mask-closable="false"
      @ok="handleEnvEditOk"
    >
      <a-form ref="editEnvForm" :rules="rulesEnv" :model="envTemp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('c.name')" name="name">
          <a-input v-model:value="envTemp.name" :max-length="50" :placeholder="$tl('p.varName')" />
        </a-form-item>
        <a-form-item :label="$tl('c.value')" :prop="`${envTemp.privacy === 1 ? '' : 'value'}`">
          <a-textarea v-model:value="envTemp.value" :rows="5" :placeholder="$tl('p.varValue')" />
        </a-form-item>
        <a-form-item :label="$tl('c.description')" name="description">
          <a-textarea v-model:value="envTemp.description" :max-length="200" :rows="5" :placeholder="$tl('p.varDesc')" />
        </a-form-item>
        <a-form-item name="privacy">
          <template #label>
            <a-tooltip>
              {{ $tl('p.privacyVar') }}
              <template #title> {{ $tl('p.privacyVarDesc') }} </template>
              <QuestionCircleOutlined v-show="!envTemp.id" />
            </a-tooltip>
          </template>
          <a-switch
            :checked="envTemp.privacy === 1"
            :disabled="envTemp.id !== undefined"
            :checked-children="$tl('p.privacy')"
            :un-checked-children="$tl('p.nonPrivacy')"
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
              {{ $tl('p.distributeNode') }}
              <template #title> {{ $tl('p.distributeNodeDesc') }}</template>
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
            :placeholder="$tl('p.selectNode')"
            mode="multiple"
          >
            <a-select-option v-for="item in nodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 触发器 -->
    <a-modal
      v-model:open="triggerVisible"
      destroy-on-close
      :title="$tl('c.trigger')"
      width="50%"
      :footer="null"
      :mask-closable="false"
    >
      <a-form ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-tabs default-active-key="1">
          <template #rightExtra>
            <a-tooltip :title="$tl('p.resetToken')">
              <a-button type="primary" size="small" @click="resetTrigger">{{ $tl('p.reset') }}</a-button>
            </a-tooltip>
          </template>
          <a-tab-pane key="1" :tab="$tl('p.get')">
            <a-space direction="vertical" style="width: 100%">
              <a-alert :message="$tl('p.tips')" type="warning">
                <template #description>
                  <ul>
                    <li>{{ $tl('p.contentType') }}</li>
                    <li>{{ $tl('p.successCode') }}</li>
                    <li>{{ $tl('p.modifySuccess') }}</li>
                    <li>PUT {{ $tl('p.requestBodyType') }}</li>
                  </ul>
                </template>
              </a-alert>

              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }"
                    >{{ $tl('p.getAddress') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>GET</a-tag> <span>{{ temp.triggerUrl }} </span>
                </template>
              </a-alert>

              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrlPost }">
                    {{ $tl('c.modifyAddress') }}
                  </a-typography-paragraph>
                </template>
                <template #description>
                  <a-tag>POST</a-tag> <span>{{ temp.triggerUrlPost }} </span>
                </template>
              </a-alert>
              <a-alert type="info">
                <template #message>
                  <a-typography-paragraph :copyable="{ text: temp.triggerUrl }">
                    {{ $tl('c.modifyAddress') }}
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
    </a-modal>
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
          title: this.$tl('c.name'),
          dataIndex: 'name',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('c.value'),
          dataIndex: 'value',
          ellipsis: true
        },

        {
          title: this.$tl('c.description'),
          dataIndex: 'description',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          ellipsis: true,
          tooltip: true,
          width: 120
        },
        {
          title: this.$tl('p.scope'),
          dataIndex: 'workspaceId',
          ellipsis: true,

          width: '120px'
        },
        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeMillis',
          customRender: ({ text }) => {
            return parseTime(text)
          },
          sorter: true,
          width: '180px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          align: 'center',

          width: '200px'
        }
      ],
      // 表单校验规则
      rulesEnv: {
        name: [{ required: true, message: this.$tl('p.inputName'), trigger: 'blur' }],
        description: [{ required: true, message: this.$tl('p.inputDesc'), trigger: 'blur' }],
        value: [{ required: true, message: this.$tl('p.inputValue'), trigger: 'blur' }]
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
    $tl(key, ...args) {
      return this.$t(`pages.system.workspaceEnv.${key}`, ...args)
    },
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
        title: this.$tl('p.systemTips'),
        zIndex: 1009,
        content: this.$tl('p.confirmDelete'),
        okText: this.$tl('p.confirm'),
        cancelText: this.$tl('p.cancel'),
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
