<template>
  <div>
    <CustomModal
      destroy-on-close
      :open="true"
      :title="$t('i18n_c446efd80d')"
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
        :message="$t('i18n_4b027f3979')"
        type="warning"
        show-icon
        style="margin-bottom: 10px"
      >
        <template #description>{{ $t('i18n_50453eeb9e') }}</template>
      </a-alert>
      <a-form ref="editScriptForm" :rules="rules" :model="temp" :label-col="{ span: 3 }" :wrapper-col="{ span: 19 }">
        <a-alert v-if="temp.scriptType === 'server-sync'" :message="$t('i18n_a33a2a4a90')" banner />
        <a-form-item :label="$t('i18n_7e2b40fc86')">
          <a-select
            v-model:value="temp.nodeId"
            :disabled="!!temp.nodeId"
            allow-clear
            :placeholder="$t('i18n_f8a613d247')"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ node.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="temp.nodeId">
          <a-form-item :label="$t('i18n_e747635151')" name="name">
            <a-input v-model:value="temp.name" :placeholder="$t('i18n_d7ec2d3fea')" />
          </a-form-item>
          <a-form-item :label="$t('i18n_4d9c3a0ed0')" name="context">
            <template #help>{{ $t('i18n_a77cc03013') }}</template>
            <a-form-item-rest>
              <code-editor v-model:content="temp.context" height="40vh" :show-tool="true" :options="{ mode: 'shell' }">
                <template #tool_before>
                  <a-button type="link" @click="scriptLibraryVisible = true">{{ $t('i18n_f685377a22') }}</a-button>
                </template>
              </code-editor>
            </a-form-item-rest>
          </a-form-item>

          <a-form-item :label="$t('i18n_2171d1b07d')">
            <a-space style="width: 100%" direction="vertical">
              <a-row v-for="(item, index) in commandParams" :key="item.key">
                <a-col :span="22">
                  <a-space style="width: 100%" direction="vertical">
                    <a-input
                      v-model:value="item.desc"
                      :addon-before="$t('i18n_417fa2c2be', { index: index + 1 })"
                      :placeholder="`${$t('i18n_55721d321c')},${$t('i18n_2b1015e902')},${$t('i18n_72d4ade571')}`"
                    />
                    <a-input
                      v-model:value="item.value"
                      :addon-before="$t('i18n_620489518c', { index: index + 1 })"
                      :placeholder="`${$t('i18n_bfed4943c5')}${$t('i18n_e9f2c62e54')}`"
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
              <a-button type="primary" @click="() => commandParams.push({})">{{ $t('i18n_4c0eead6ff') }}</a-button>
            </a-space>
          </a-form-item>
          <a-form-item :label="$t('i18n_fffd3ce745')" name="global">
            <a-radio-group v-model:value="temp.global">
              <a-radio :value="true"> {{ $t('i18n_2be75b1044') }}</a-radio>
              <a-radio :value="false"> {{ $t('i18n_691b11e443') }}</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="$t('i18n_6b2e348a2b')" name="autoExecCron">
            <a-auto-complete
              v-model:value="temp.autoExecCron"
              :placeholder="$t('i18n_5dff0d31d0')"
              :options="CRON_DATA_SOURCE"
            >
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
            </a-auto-complete>
          </a-form-item>
          <a-form-item :label="$t('i18n_3bdd08adab')" name="description">
            <a-textarea
              v-model:value="temp.description"
              :rows="3"
              style="resize: none"
              :placeholder="$t('i18n_ae653ec180')"
            />
          </a-form-item>
        </template>
      </a-form>
    </CustomModal>
    <!-- pages.node.script-edit.a36f20d3 -->
    <CustomDrawer
      v-if="scriptLibraryVisible"
      destroy-on-close
      :title="$t('i18n_53bdd93fd6')"
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
            >{{ $t('i18n_625fb26b4b') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerScriptConfirm()
              }
            "
            >{{ $t('i18n_f71316d0dd') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptLibraryRef'].handerTagConfirm()
              }
            "
            >{{ $t('i18n_9300692fac') }}</a-button
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
        name: [{ required: true, message: this.$t('i18n_fb7b9876a6'), trigger: 'blur' }],
        context: [{ required: true, message: this.$t('i18n_da1cb76e87'), trigger: 'blur' }]
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
          message: this.$t('i18n_a33a2a4a90')
        })
        return
      }
      if (!this.temp.nodeId) {
        $notification.warning({
          message: this.$t('i18n_d1f0fac71d')
        })
        return
      }
      // 检验表单
      this.$refs['editScriptForm'].validate().then(() => {
        if (this.commandParams && this.commandParams.length > 0) {
          for (let i = 0; i < this.commandParams.length; i++) {
            if (!this.commandParams[i].desc) {
              $notification.error({
                message: this.$t('i18n_8ae2b9915c') + (i + 1) + this.$t('i18n_c583b707ba')
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
