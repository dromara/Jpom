<template>
  <div>
    <a-form
      ref="releaseFileForm"
      :rules="releaseFileRules"
      :model="temp"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 20 }"
    >
      <a-form-item :label="$tl('p.taskName')" name="name">
        <a-input v-model:value="temp.name" :placeholder="$tl('p.taskNamePlaceholder')" :max-length="50" />
      </a-form-item>

      <a-form-item :label="$tl('p.publishMode')" name="taskType">
        <a-radio-group v-model:value="temp.taskType" @change="taskTypeChange">
          <a-radio :value="0"> SSH </a-radio>
          <a-radio :value="1"> {{ $tl('p.node') }} </a-radio>
        </a-radio-group>
        <template #help>
          <template v-if="temp.taskType === 0">{{ $tl('p.fileNameAfterPublish') }} </template>
        </template>
      </a-form-item>

      <a-form-item v-if="temp.taskType === 0" name="taskDataIds" :label="$tl('p.ssh')">
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
              :placeholder="$tl('p.sshPlaceholder')"
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
      <a-form-item v-else-if="temp.taskType === 1" name="taskDataIds" :label="$tl('p.publishNode')">
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
              :placeholder="$tl('p.nodePlaceholder')"
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

      <a-form-item name="releasePathParent" :label="$tl('p.publishDir')">
        <template #help>
          <a-tooltip :title="$tl('p.authDirConfig')"
            ><a-button
              size="small"
              type="link"
              @click="
                () => {
                  configDir = true
                }
              "
            >
              <InfoCircleOutlined />{{ $tl('p.authDir') }}
            </a-button>
          </a-tooltip>
        </template>
        <a-input-group compact>
          <a-select
            v-model:value="temp.releasePathParent"
            show-search
            allow-clear
            style="width: 30%"
            :placeholder="$tl('p.firstLevelDir')"
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
              :placeholder="$tl('p.secondLevelDir')"
            />
          </a-form-item-rest>
        </a-input-group>
      </a-form-item>

      <a-form-item name="releaseBeforeCommand">
        <template #label>
          {{ $tl('p.executeScript') }}
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $tl('p.scriptVariable') }}</li>
                <li>{{ $tl('p.workspaceEnvVariable') }}</li>
                <li>{{ $tl('p.renameFile') }}</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </template>
        <template #help>
          <div v-if="scriptTabKey === 'before'">{{ $tl('p.preUploadScript') }}</div>
          <div v-else-if="scriptTabKey === 'after'">{{ $tl('p.postUploadScript') }}</div>
        </template>
        <a-form-item-rest>
          <a-tabs v-model:activeKey="scriptTabKey" tab-position="right" type="card">
            <a-tab-pane key="before" :tab="$tl('c.content')">
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
                      <b>{{ $tl('c.content') }}</b>
                      {{ $tl('p.execute') }}
                    </a-tag>
                    <a-button
                      type="link"
                      @click="
                        () => {
                          chooseScriptVisible = 1
                        }
                      "
                    >
                      脚本模板
                    </a-button>
                  </a-space>
                </template>
              </code-editor>
            </a-tab-pane>
            <a-tab-pane key="after" :tab="$tl('p.afterUpload')">
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
                    <a-tag>{{ $tl('p.afterUploadExecute') }}</a-tag>
                    <a-button
                      type="link"
                      @click="
                        () => {
                          chooseScriptVisible = 2
                        }
                      "
                    >
                      脚本模板
                    </a-button>
                  </a-space>
                </template>
              </code-editor>
            </a-tab-pane>
          </a-tabs>
        </a-form-item-rest>
      </a-form-item>
    </a-form>
    <!-- 配置授权目录 -->
    <a-modal
      v-model:open="configDir"
      destroy-on-close
      :title="`${$tl('p.authDirSetting')}`"
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
    </a-modal>

    <!-- 选择脚本 -->
    <a-drawer
      destroy-on-close
      title="选择脚本"
      placement="right"
      :open="chooseScriptVisible != 0"
      width="70vw"
      :z-index="1009"
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
              ? temp.beforeScript?.replace('$ref.script.')
              : ''
            : temp.afterScript?.indexOf('$ref.script.') !== -1
              ? temp.afterScript?.replace('$ref.script.')
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
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
          >
            确认
          </a-button>
        </a-space>
      </template>
    </a-drawer>
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
        name: [{ required: true, message: this.$tl('p.taskNameInput'), trigger: 'blur' }],
        taskType: [{ required: true, message: this.$tl('p.publishModeSelect'), trigger: 'blur' }],
        releasePath: [
          {
            required: true,
            message: this.$tl('p.publishDirSelect'),
            trigger: 'blur'
          }
        ],
        taskDataIds: [{ required: true, message: this.$tl('p.sshSelect'), trigger: 'blur' }]
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
    $tl(key, ...args) {
      return this.$t(`pages.fileManager.fileStorage.releaseFile.${key}`, ...args)
    },
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
