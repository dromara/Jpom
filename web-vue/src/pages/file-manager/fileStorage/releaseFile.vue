<template>
  <div>
    <a-form
      ref="releaseFileForm"
      :rules="releaseFileRules"
      :model="temp"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 20 }"
    >
      <a-form-item :label="$t('pages.file-manager.fileStorage.releaseFile.e62a5bf3')" name="name">
        <a-input
          v-model:value="temp.name"
          :placeholder="$t('pages.file-manager.fileStorage.releaseFile.70cf48dc')"
          :max-length="50"
        />
      </a-form-item>

      <a-form-item :label="$t('pages.file-manager.fileStorage.releaseFile.73754f45')" name="taskType">
        <a-radio-group v-model:value="temp.taskType" @change="taskTypeChange">
          <a-radio :value="0"> SSH </a-radio>
          <a-radio :value="1"> {{ $t('pages.file-manager.fileStorage.releaseFile.602a0a5e') }} </a-radio>
        </a-radio-group>
        <template #help>
          <template v-if="temp.taskType === 0"
            >{{ $t('pages.file-manager.fileStorage.releaseFile.469af1fa') }}
          </template>
        </template>
      </a-form-item>

      <a-form-item
        v-if="temp.taskType === 0"
        name="taskDataIds"
        :label="$t('pages.file-manager.fileStorage.releaseFile.6ed1da89')"
      >
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
              :placeholder="$t('pages.file-manager.fileStorage.releaseFile.f1f8e3bc')"
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
      <a-form-item
        v-else-if="temp.taskType === 1"
        name="taskDataIds"
        :label="$t('pages.file-manager.fileStorage.releaseFile.61c0e0ab')"
      >
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
              :placeholder="$t('pages.file-manager.fileStorage.releaseFile.89a4deb8')"
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

      <a-form-item name="releasePathParent" :label="$t('pages.file-manager.fileStorage.releaseFile.b6c9f9c')">
        <template #help>
          <a-tooltip :title="$t('pages.file-manager.fileStorage.releaseFile.61fec7a9')"
            ><a-button
              size="small"
              type="link"
              @click="
                () => {
                  configDir = true
                }
              "
            >
              <InfoCircleOutlined />{{ $t('pages.file-manager.fileStorage.releaseFile.31f003d') }}
            </a-button>
          </a-tooltip>
        </template>
        <a-input-group compact>
          <a-select
            v-model:value="temp.releasePathParent"
            show-search
            allow-clear
            style="width: 30%"
            :placeholder="$t('pages.file-manager.fileStorage.releaseFile.fda0ef21')"
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
              :placeholder="$t('pages.file-manager.fileStorage.releaseFile.a6ba1641')"
            />
          </a-form-item-rest>
        </a-input-group>
      </a-form-item>

      <a-form-item name="releaseBeforeCommand">
        <template #label>
          {{ $t('pages.file-manager.fileStorage.releaseFile.1cf6b640') }}
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $t('pages.file-manager.fileStorage.releaseFile.9bab648c') }}</li>
                <li>{{ $t('pages.file-manager.fileStorage.releaseFile.52373158') }}</li>
                <li>{{ $t('pages.file-manager.fileStorage.releaseFile.6fcc2557') }}</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </template>
        <template #help>
          <div v-if="scriptTabKey === 'before'">{{ $t('pages.file-manager.fileStorage.releaseFile.5fcb4809') }}</div>
          <div v-else-if="scriptTabKey === 'after'">
            {{ $t('pages.file-manager.fileStorage.releaseFile.3fcb1a9a') }}
          </div>
        </template>
        <a-form-item-rest>
          <a-tabs v-model:activeKey="scriptTabKey" tab-position="right" type="card">
            <a-tab-pane key="before" :tab="$t('pages.file-manager.fileStorage.releaseFile.cb2367c')">
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
                      <b>{{ $t('pages.file-manager.fileStorage.releaseFile.cb2367c') }}</b>
                      {{ $t('pages.file-manager.fileStorage.releaseFile.985968bf') }}
                    </a-tag>
                    <a-button
                      type="link"
                      @click="
                        () => {
                          chooseScriptVisible = 1
                        }
                      "
                      >{{ $t('pages.file-manager.fileStorage.releaseFile.d236a971') }}</a-button
                    >
                  </a-space>
                </template>
              </code-editor>
            </a-tab-pane>
            <a-tab-pane key="after" :tab="$t('pages.file-manager.fileStorage.releaseFile.e643b0a1')">
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
                    <a-tag>{{ $t('pages.file-manager.fileStorage.releaseFile.278fde90') }}</a-tag>
                    <a-button
                      type="link"
                      @click="
                        () => {
                          chooseScriptVisible = 2
                        }
                      "
                      >{{ $t('pages.file-manager.fileStorage.releaseFile.d236a971') }}</a-button
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
      :title="`${$t('pages.file-manager.fileStorage.releaseFile.6978ea2b')}`"
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
      :title="$t('pages.file-manager.fileStorage.releaseFile.952117a8')"
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
            >{{ $t('pages.file-manager.fileStorage.releaseFile.a0451c97') }}</a-button
          >
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
            >{{ $t('pages.file-manager.fileStorage.releaseFile.1cbe2507') }}</a-button
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
        name: [
          { required: true, message: this.$t('pages.file-manager.fileStorage.releaseFile.378f28a1'), trigger: 'blur' }
        ],

        taskType: [
          { required: true, message: this.$t('pages.file-manager.fileStorage.releaseFile.c33e3d16'), trigger: 'blur' }
        ],

        releasePath: [
          {
            required: true,
            message: this.$t('pages.file-manager.fileStorage.releaseFile.8092e55'),
            trigger: 'blur'
          }
        ],

        taskDataIds: [
          { required: true, message: this.$t('pages.file-manager.fileStorage.releaseFile.6a05f5c8'), trigger: 'blur' }
        ]
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
