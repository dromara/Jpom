<template>
  <div>
    <a-form
      ref="releaseFileForm"
      :rules="releaseFileRules"
      :model="temp"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 20 }"
    >
      <a-form-item :label="$t('i18n_ce23a42b47')" name="name">
        <a-input v-model:value="temp.name" :placeholder="$t('i18n_5f4c724e61')" :max-length="50" />
      </a-form-item>

      <a-form-item :label="$t('i18n_f98994f7ec')" name="taskType">
        <a-radio-group v-model:value="temp.taskType" @change="taskTypeChange">
          <a-radio :value="0"> SSH </a-radio>
          <a-radio :value="1"> {{ $t('i18n_3bf3c0a8d6') }} </a-radio>
        </a-radio-group>
        <template #help>
          <template v-if="temp.taskType === 0">{{ $t('i18n_28bf369f34') }} </template>
        </template>
      </a-form-item>

      <a-form-item v-if="temp.taskType === 0" name="taskDataIds" :label="$t('i18n_b188393ea7')">
        <a-row>
          <a-col :span="22">
            <a-select
              v-model:value="temp.taskDataIds"
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
              mode="multiple"
              :placeholder="$t('i18n_260a3234f2')"
            >
              <a-select-option v-for="ssh in sshList" :key="ssh.id">
                <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="1" style="margin-left: 10px">
            <ReloadOutlined @click="loadSshList" />
          </a-col>
        </a-row>
      </a-form-item>
      <a-form-item v-else-if="temp.taskType === 1" name="taskDataIds" :label="$t('i18n_473badc394')">
        <a-row>
          <a-col :span="22">
            <a-select
              v-model:value="temp.taskDataIds"
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
              mode="multiple"
              :placeholder="$t('i18n_f8a613d247')"
            >
              <a-select-option v-for="ssh in nodeList" :key="ssh.id">
                <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
              </a-select-option>
            </a-select>
          </a-col>
          <a-col :span="1" style="margin-left: 10px">
            <ReloadOutlined @click="loadNodeList" />
          </a-col>
        </a-row>
      </a-form-item>

      <a-form-item name="releasePathParent" :label="$t('i18n_dbb2df00cf')">
        <template #help>
          <a-tooltip :title="$t('i18n_bfe8fab5cd')"
            ><a-button
              size="small"
              type="link"
              @click="
                () => {
                  configDir = true
                }
              "
            >
              <InfoCircleOutlined />{{ $t('i18n_1e5533c401') }}
            </a-button>
          </a-tooltip>
        </template>
        <a-input-group compact>
          <a-select
            v-model:value="temp.releasePathParent"
            show-search
            allow-clear
            style="width: 30%"
            :placeholder="$t('i18n_edd716f524')"
          >
            <a-select-option v-for="item in accessList" :key="item">
              <a-tooltip :title="item">{{ item }}</a-tooltip>
            </a-select-option>
            <template #suffixIcon>
              <ReloadOutlined @click="loadAccesList" />
            </template>
          </a-select>
          <a-form-item-rest>
            <a-input
              v-model:value="temp.releasePathSecondary"
              style="width: 70%"
              :placeholder="$t('i18n_dc0d06f9c7')"
            />
          </a-form-item-rest>
        </a-input-group>
      </a-form-item>

      <a-form-item name="releaseBeforeCommand">
        <template #label>
          {{ $t('i18n_cfb00269fd') }}
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $t('i18n_799ac8bf40') }}</li>
                <li>{{ $t('i18n_5fbde027e3') }}</li>
                <li>{{ $t('i18n_a9c999e0bd') }}</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </template>
        <template #help>
          <div v-if="scriptTabKey === 'before'">{{ $t('i18n_00de0ae1da') }}</div>
          <div v-else-if="scriptTabKey === 'after'">
            {{ $t('i18n_08ac1eace7') }}
          </div>
        </template>
        <a-form-item-rest>
          <a-tabs v-model:activeKey="scriptTabKey" tab-position="right" type="card">
            <a-tab-pane key="before" :tab="$t('i18n_d0c879f900')">
              <code-editor
                v-model:content="temp.beforeScript"
                height="40vh"
                :show-tool="true"
                :options="{
                  mode: 'shell'
                }"
              >
                <template #tool_before>
                  <a-space>
                    <a-tag>
                      <b>{{ $t('i18n_d0c879f900') }}</b>
                      {{ $t('i18n_1a6aa24e76') }}
                    </a-tag>
                    <a-button
                      type="link"
                      @click="
                        () => {
                          chooseScriptVisible = 1
                        }
                      "
                      >{{ $t('i18n_54f271cd41') }}</a-button
                    >
                  </a-space>
                </template>
              </code-editor>
            </a-tab-pane>
            <a-tab-pane key="after" :tab="$t('i18n_9b1c5264a0')">
              <code-editor
                v-model:content="temp.afterScript"
                height="40vh"
                :show-tool="true"
                :options="{
                  mode: 'shell'
                }"
              >
                <template #tool_before>
                  <a-space>
                    <a-tag>{{ $t('i18n_e7ffc33d05') }}</a-tag>
                    <a-button
                      type="link"
                      @click="
                        () => {
                          chooseScriptVisible = 2
                        }
                      "
                      >{{ $t('i18n_54f271cd41') }}</a-button
                    >
                  </a-space>
                </template>
              </code-editor>
            </a-tab-pane>
          </a-tabs>
        </a-form-item-rest>
      </a-form-item>
    </a-form>
    <!-- 配置授权目录 -->
    <CustomModal
      v-if="configDir"
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('i18n_eee6510292')}`"
      :footer="null"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </CustomModal>
    <!-- 选择脚本 -->
    <CustomDrawer
      v-if="chooseScriptVisible != 0"
      destroy-on-close
      :title="$t('i18n_a056d9c4b3')"
      placement="right"
      :open="chooseScriptVisible != 0"
      width="70vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseScriptVisible = 0
        }
      "
    >
      <scriptPage
        v-if="chooseScriptVisible"
        ref="scriptPage"
        choose="radio"
        :choose-val="
          chooseScriptVisible === 1
            ? temp.beforeScript?.indexOf('$ref.script.') !== -1
              ? temp.beforeScript?.replace('$ref.script.', '')
              : ''
            : temp.afterScript?.indexOf('$ref.script.') !== -1
            ? temp.afterScript?.replace('$ref.script.', '')
            : ''
        "
        mode="choose"
        @confirm="
          (id) => {
            if (chooseScriptVisible === 1) {
              temp = { ...temp, beforeScript: '$ref.script.' + id }
            } else if (chooseScriptVisible === 2) {
              temp = { ...temp, afterScript: '$ref.script.' + id }
            }
            chooseScriptVisible = 0
          }
        "
        @cancel="
          () => {
            chooseScriptVisible = 0
          }
        "
      ></scriptPage>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                chooseScriptVisible = false
              }
            "
            >{{ $t('i18n_625fb26b4b') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
            >{{ $t('i18n_e83a256e4f') }}</a-button
          >
        </a-space>
      </template>
    </CustomDrawer>
  </div>
</template>
<script>
import { getSshListAll } from '@/api/ssh'
import { getDispatchWhiteList } from '@/api/dispatch'
import { getNodeListAll } from '@/api/node'
import codeEditor from '@/components/codeEditor'
import whiteList from '@/pages/dispatch/white-list.vue'
import scriptPage from '@/pages/script/script-list.vue'
export default {
  components: {
    codeEditor,
    whiteList,
    scriptPage
  },
  emits: ['commit'],
  data() {
    return {
      temp: {},
      releaseFileRules: {
        name: [{ required: true, message: this.$t('i18n_89d18c88a3'), trigger: 'blur' }],

        taskType: [{ required: true, message: this.$t('i18n_29b48a76be'), trigger: 'blur' }],

        releasePath: [
          {
            required: true,
            message: this.$t('i18n_be28f10eb6'),
            trigger: 'blur'
          }
        ],

        taskDataIds: [{ required: true, message: this.$t('i18n_3e51d1bc9c'), trigger: 'blur' }]
      },
      sshList: [],
      accessList: [],
      nodeList: [],
      configDir: false,
      scriptTabKey: 'before',
      chooseScriptVisible: 0
    }
  },
  created() {
    this.temp = { taskType: 0 }
    this.taskTypeChange(0)
    this.loadAccesList()
  },
  methods: {
    taskTypeChange() {
      const value = this.temp.taskType
      this.temp = { ...this.temp, taskDataIds: undefined }
      if (value === 0) {
        this.loadSshList()
      } else if (value === 1) {
        this.loadNodeList()
      }
    },
    // 创建任务
    tryCommit() {
      this.$refs['releaseFileForm'].validate().then(() => {
        this.$emit('commit', {
          ...this.temp,
          taskDataIds: this.temp.taskDataIds?.join(',')
        })
      })
    },
    // 加载项目授权列表
    loadAccesList() {
      getDispatchWhiteList().then((res) => {
        if (res.code === 200) {
          this.accessList = res.data.outGivingArray || []
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = []
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data
            resolve()
          }
        })
      })
    },
    // 加载节点
    loadNodeList() {
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
        }
      })
    }
  }
}
</script>
<style scoped>
:deep(.ant-tabs-tabpane) {
  padding-right: 0 !important;
}
</style>
