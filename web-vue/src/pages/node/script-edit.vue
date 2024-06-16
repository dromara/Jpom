<template>
  <div>
    <CustomModal
      destroy-on-close
      :open="true"
      :title="$t('pages.node.script-edit.c05890d1')"
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
        :message="$t('pages.node.script-edit.7ae524f6')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $t('pages.node.script-edit.a3a86f30') }}</template>
      </a-alert>
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-alert v-if="temp.scriptType === 'server-sync'" :message="$t('pages.node.script-edit.250185dc')" banner />
        <a-form-item :label="$t('pages.node.script-edit.580e6c10')">
          <a-select
            v-model:value="temp.nodeId"
            :disabled="!!temp.nodeId"
            allow-clear
            :placeholder="$t('pages.node.script-edit.2c33c91c')"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="temp.nodeId">
          <a-form-item :label="$t('pages.node.script-edit.db9bba81')" name="name">
            <a-input v-model:value="temp.name" :placeholder="$t('pages.node.script-edit.bb769c1d')" />
          </a-form-item>
          <a-form-item :label="$t('pages.node.script-edit.709314dd')" name="context">
            <template #help>{{ $t('pages.node.script-edit.2cc2c670') }}</template>
            <a-form-item-rest>
              <code-editor v-model:content="temp.context" height="40vh" :show-tool="true" :options="{ mode: 'shell' }">
                <template #tool_before>
                  <a-button type="link" @click="scriptLibraryVisible = true">{{
                    $t('pages.node.script-edit.b52fd8d6')
                  }}</a-button>
                </template>
              </code-editor>
            </a-form-item-rest>
          </a-form-item>

          <a-form-item :label="$t('pages.node.script-edit.337801c6')">
            <a-space style="width: 100%" direction="vertical">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space style="width: 100%" direction="vertical">
                    <a-input
                      v-model:value="item.desc"
                      :addon-before="$t('pages.node.script-edit.275d1e37', { index: index + 1 })"
                      :placeholder="`${$t('pages.node.script-edit.b31dbb3')},${$t(
                        'pages.node.script-edit.b01bb0b5'
                      )},${$t('pages.node.script-edit.4b6049f2')}`"
                    />
                    <a-input
                      v-model:value="item.value"
                      :addon-before="$t('pages.node.script-edit.9a17bb8e', { index: index + 1 })"
                      :placeholder="`${$t('pages.node.script-edit.5b6ebe20')}${$t('pages.node.script-edit.969ca97b')}`"
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
              <a-button type="primary" @click="() => commandParams.push({})">{{
                $t('pages.node.script-edit.5cfe25ce')
              }}</a-button>
            </a-space>
          </a-form-item>
          <a-form-item :label="$t('pages.node.script-edit.c9bb9409')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $t('pages.node.script-edit.b21bb9ac') }}</a-radio>
              <a-radio :value="false"> {{ $t('pages.node.script-edit.919267cc') }}</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$t('pages.node.script-edit.cab13d5c')" name="autoExecCron">
            <a-auto-complete
              v-model:value="temp.autoExecCron"
              :placeholder="$t('pages.node.script-edit.235e51dd')"
              :options="CRON_DATA_SOURCE"
            >
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item :label="$t('pages.node.script-edit.42e3c32a')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :rows="3"
              style="resize: none"
              :placeholder="$t('pages.node.script-edit.419e634e')"
            />
          </a-form-item>
        </template>
      </a-form>
    </CustomModal>
    <!-- pages.node.script-edit.a36f20d3 -->
    <CustomDrawer
      v-if="scriptLibraryVisible"
      destroy-on-close
      :title="$t('pages.node.script-edit.a36f20d3')"
      placement="right"
      :open="scriptLibraryVisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          scriptLibraryVisible = false
        }
      "
    >
      <ScriptLibraryNoPermission
        v-if="scriptLibraryVisible"
        ref="scriptLibraryRef"
        @script-confirm="
          (script) => {
            temp = { ...temp, context: script }
            scriptLibraryVisible = false
          }
        "
        @tag-confirm="
          (tag) => {
            temp = { ...temp, context: (temp.context || '') + `\nG@(\&quot;${tag}\&quot;)` }
            scriptLibraryVisible = false
          }
        "
      ></ScriptLibraryNoPermission>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                scriptLibraryVisible = false
              }
            "
            >{{ $t('pages.node.script-edit.a0451c97') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerScriptConfirm()
              }
            "
            >{{ $t('pages.node.script-edit.703db0bd') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerTagConfirm()
              }
            "
            >{{ $t('pages.node.script-edit.4f288de9') }}</a-button
          >
        </a-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import codeEditor from '@/components/codeEditor'
import { editScript, itemScript } from '@/api/node-other'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'
import { getNodeListAll } from '@/api/node'
import ScriptLibraryNoPermission from '@/pages/system/assets/script-library/no-permission'
export default {
  components: {
    codeEditor,
    ScriptLibraryNoPermission
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
        name: [{ required: true, message: this.$t('pages.node.script-edit.661c0530'), trigger: 'blur' }],
        context: [{ required: true, message: this.$t('pages.node.script-edit.a48e24b3'), trigger: 'blur' }]
      },
      confirmLoading: false,
      scriptLibraryVisible: false
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
          message: this.$t('pages.node.script-edit.250185dc')
        })
        return
      }
      if (!this.temp.nodeId) {
        $notification.warning({
          message: this.$t('pages.node.script-edit.dbed11aa')
        })
        return
      }
      // 检验表单
      this.$refs['editScriptForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message:
                  this.$t('pages.node.script-edit.4149d069') + (i + 1) + this.$t('pages.node.script-edit.94a5dd5e')
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
