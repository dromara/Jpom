<template>
  <div>
    <a-modal
      destroy-on-close
      :open="true"
      :title="$tl('p.editScript')"
      :confirm-loading="confirmLoading"
      :mask-closable="false"
      width="80vw"
      @ok="handleEditScriptOk"
      @cancel="
        () => {
          $emit('close')
        }
      "
    >
      <a-alert
        v-if="!nodeList || !nodeList.length"
        :message="$tl('p.reminder')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $tl('p.noLogicNodeError') }}</template>
      </a-alert>
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-alert
          v-if="temp.scriptType === 'server-sync'"
          :message="$tl('c.serverScriptModificationForbidden')"
          banner
        />
        <a-form-item :label="$tl('p.selectNode')">
          <a-select
            v-model:value="temp.nodeId"
            :disabled="!!temp.nodeId"
            allow-clear
            :placeholder="$tl('p.pleaseSelectNode')"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="temp.nodeId">
          <a-form-item :label="$tl('p.scriptName')" name="name">
            <a-input v-model:value="temp.name" :placeholder="$tl('p.name')" />
          </a-form-item>
          <a-form-item :label="$tl('p.scriptContent')" name="context">
            <a-form-item-rest>
              <code-editor v-model:content="temp.context" height="40vh" :options="{ mode: 'shell' }"></code-editor>
            </a-form-item-rest>
          </a-form-item>
          <!-- <a-form-item label="默认参数" name="defArgs">
            <a-input v-model="temp.defArgs" placeholder="默认参数" />
          </a-form-item> -->
          <a-form-item :label="$tl('p.defaultParams')">
            <a-space style="width: 100%" direction="vertical">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space style="width: 100%" direction="vertical">
                    <a-input
                      v-model:value="item.desc"
                      :addon-before="$tl('p.paramDescriptionTemplate', { index: index + 1 })"
                      :placeholder="`${$tl('p.paramDescription')},${$tl('p.paramDescriptionNote')},${$tl('p.paramDescriptionHint')}`"
                    />
                    <a-input
                      v-model:value="item.value"
                      :addon-before="$tl('p.paramValueTemplate', { index: index + 1 })"
                      :placeholder="`${$tl('p.paramValue')}${$tl('p.newParamValueNote')}`"
                    />
                  </a-space>
                </a-col>
                <a-col :span="2">
                  <a-row type="flex" justify="center" align="middle">
                    <a-col>
                      <MinusCircleOutlined style="color: #ff0000" @click="() => commandParams.splice(index, 1)" />
                    </a-col>
                  </a-row>
                </a-col>
              </a-row>
              <a-button type="primary" @click="() => commandParams.push({})">{{ $tl('p.addNewParam') }}</a-button>
            </a-space>
          </a-form-item>
          <a-form-item :label="$tl('p.sharing')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $tl('p.globalScope') }}</a-radio>
              <a-radio :value="false"> {{ $tl('p.currentWorkspace') }}</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$tl('p.scheduledExecution')" name="autoExecCron">
            <a-auto-complete
              v-model:value="temp.autoExecCron"
              :placeholder="$tl('p.cronExpressionNote')"
              :options="CRON_DATA_SOURCE"
            >
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item :label="$tl('p.description')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :rows="3"
              style="resize: none"
              :placeholder="$tl('p.detailedDescription')"
            />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import codeEditor from '@/components/codeEditor'
import { editScript, itemScript } from '@/api/node-other'
import { CRON_DATA_SOURCE } from '@/utils/const'
import { getNodeListAll } from '@/api/node'
export default {
  components: {
    codeEditor
  },
  props: {
    nodeId: {
      type: String,
      default: undefined
    },
    scriptId: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      temp: {},
      CRON_DATA_SOURCE,
      commandParams: [],
      nodeList: [],
      rules: {
        name: [{ required: true, message: this.$tl('p.pleaseInputScriptName'), trigger: 'blur' }],
        context: [{ required: true, message: this.$tl('p.pleaseInputScriptContent'), trigger: 'blur' }]
      },
      confirmLoading: false
    }
  },
  mounted() {
    // 查询节点
    getNodeListAll().then((res) => {
      if (res.code === 200 && res.data) {
        this.nodeList = res.data
        // res.data.forEach((item) => {
        //   this.nodeMap[item.id] = item.name;
        // });
      }
      this.handleEdit()
    })
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.node.scriptEdit.${key}`, ...args)
    },
    // 修改
    handleEdit() {
      this.$refs['editScriptForm']?.resetFields()
      if (this.scriptId && this.nodeId) {
        itemScript({
          id: this.scriptId,
          nodeId: this.nodeId
        }).then((res) => {
          if (res.code === 200) {
            this.temp = Object.assign({}, res.data, {
              global: res.data.workspaceId === 'GLOBAL',
              workspaceId: ''
            })
            this.temp.nodeId = this.nodeId
            this.commandParams = this.temp.defArgs ? JSON.parse(this.temp.defArgs) : []
            //
          }
        })
      } else {
        this.temp = { global: false, type: 'add', nodeId: this.nodeId }
      }
    },
    // 提交 Script 数据
    handleEditScriptOk() {
      if (this.temp.scriptType === 'server-sync') {
        $notification.warning({
          message: this.$tl('c.serverScriptModificationForbidden')
        })
        return
      }
      if (!this.temp.nodeId) {
        $notification.warning({
          message: this.$tl('p.noNodeSelectedError')
        })
        return
      }
      // 检验表单
      this.$refs['editScriptForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$tl('p.fillParamDescriptionPrefix') + (i + 1) + this.$tl('p.paramDescriptionSuffix')
              })
              return false
            }
          }
          this.temp.defArgs = JSON.stringify(this.commandParams)
        } else {
          this.temp.defArgs = ''
        }
        // 提交数据
        this.confirmLoading = true
        editScript(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })

              this.$emit('close')
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    }
  }
}
</script>
