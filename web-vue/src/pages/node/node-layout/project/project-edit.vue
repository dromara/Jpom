<template>
  <div>
    <!-- 编辑区 -->
    <a-spin :tip="$tl('p.loadingData')" :spinning="loading">
      <a-form ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$tl('p.projectId')" name="id">
          <a-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$tl('c.createOnce')"
          />
          <template v-else>
            <a-input-search
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$tl('c.createOnce')"
              @search="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $tl('p.randomGenerate') }} </a-button>
              </template>
            </a-input-search>
          </template>
        </a-form-item>

        <a-form-item :label="$tl('c.projectName')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.projectName')" />
            </a-col>
            <a-col :span="4" style="text-align: right">{{ $tl('p.groupName') }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$tl('p.addNewGroup')"
                  :select-placeholder="$tl('p.selectGroup')"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="runMode">
          <template #label>
            <a-tooltip>
              {{ $tl('p.runMode') }}
              <template #title>
                <ul>
                  <li><b>Dsl</b> {{ $tl('p.customizeProject') }}</li>
                  <li><b>ClassPath</b> java -classpath xxx {{ $tl('c.runProject') }}</li>
                  <li><b>Jar</b> java -jar xxx {{ $tl('c.runProject') }}</li>
                  <li><b>JarWar</b> java -jar Springboot war {{ $tl('c.runProject') }}</li>
                  <li>
                    <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS {{ $tl('c.runProject') }}
                  </li>
                  <li><b>File</b> {{ $tl('p.staticFolder') }},{{ $tl('p.noStatusControl') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select v-model:value="temp.runMode" :placeholder="$tl('p.selectRunMode')" @change="changeRunMode">
            <a-select-option v-for="item in runModeArray" :key="item.name">
              <template v-if="item.desc.indexOf($tl('p.notRecommended')) > -1">
                <s>
                  <b>[{{ item.name }}]</b> {{ item.desc }}
                </s>
              </template>
              <template v-else>
                <b>[{{ item.name }}]</b> {{ item.desc }}
              </template>
            </a-select-option>
          </a-select>
        </a-form-item>
        <template v-if="temp.runMode === 'Link'">
          <a-form-item :label="$tl('p.softLinkProject')" name="linkId">
            <a-select v-model:value="temp.linkId" :placeholder="$tl('p.selectSoftLinkProject')" @change="changeLinkId">
              <a-select-option
                v-for="item in projectList"
                :key="item.projectId"
                :disabled="item.runMode === 'File' || item.runMode === 'Link'"
              >
                <b>[{{ item.runMode }}]</b> {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </template>
        <template v-else>
          <a-form-item name="whitelistDirectory">
            <template #label>
              <a-tooltip>
                {{ $tl('p.projectPath') }}
                <template #title>
                  <ul>
                    <li>{{ $tl('p.authPathDesc') }}</li>
                    <li>{{ $tl('p.modifyAuthConfig') }}</li>
                    <li>{{ $tl('p.folderName') }}</li>
                    <li>
                      {{ $tl('p.storagePath') }} <br />&nbsp;&nbsp;<b>{{ $tl('p.fullPath') }}</b>
                    </li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <template #help>
              <div>
                {{ $tl('p.preConfigAuthDir') }}
                <a-button
                  type="link"
                  size="small"
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $tl('p.quickConfig') }}
                </a-button>
              </div>
            </template>
            <a-input-group compact>
              <a-select
                v-model:value="temp.whitelistDirectory"
                style="width: 50%"
                :placeholder="$tl('c.selectAuthPath')"
              >
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">{{ access }}</a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input v-model:value="temp.lib" style="width: 50%" :placeholder="$tl('p.storageFolder')" />
              </a-form-item-rest>
            </a-input-group>
            <template #extra>
              <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" :label="$tl('p.completePath')">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
        </template>
        <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
          <template #label>
            <a-tooltip>
              DSL {{ $tl('p.content') }}
              <template #title>
                <p>{{ $tl('p.configFormat') }}</p>
                <p>{{ $tl('p.supportedVars') }}</p>
                <p>
                  <b>status</b>
                  {{ $tl('p.outputFormat') }}:$pid <b>$pid {{ $tl('p.processId') }}</b
                  >{{ $tl('p.statusCheck') }}
                </p>
                <p>{{ $tl('p.configReference') }}</p>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help>
            scriptId{{ $tl('p.useNodeScript') }}
            <a-button
              type="link"
              size="small"
              @click="
                () => {
                  drawerVisible = true
                }
              "
            >
              {{ $tl('c.viewNodeScript') }}
            </a-button>
          </template>
          <a-form-item-rest>
            <code-editor
              v-show="dslEditTabKey === 'content'"
              v-model:content="temp.dslContent"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'yaml', tabSize: 2 }"
              :placeholder="$tl('p.fillDSL')"
            >
              <template #tool_before>
                <a-segmented
                  v-model:value="dslEditTabKey"
                  :options="[
                    { label: `DSL ${$tl('c.configuration')}`, value: 'content' },
                    { label: $tl('c.configExample'), value: 'demo' }
                  ]"
                />
              </template>
            </code-editor>
            <code-editor
              v-show="dslEditTabKey === 'demo'"
              v-model:content="PROJECT_DSL_DEFATUL"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'yaml', tabSize: 2, readOnly: true }"
            >
              <template #tool_before>
                <a-segmented
                  v-model:value="dslEditTabKey"
                  :options="[
                    { label: `DSL ${$tl('c.configuration')}`, value: 'content' },
                    { label: $tl('c.configExample'), value: 'demo' }
                  ]"
                />
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item v-show="noFileModes.includes(temp.runMode) && temp.runMode !== 'Link'">
          <template #label>
            <a-tooltip>
              {{ $tl('p.logDir') }}
              <template #title>
                <ul>
                  <li>{{ $tl('p.logDirDesc') }}</li>
                  <li>{{ $tl('c.defaultLogPath') }}</li>
                  <li>{{ $tl('p.selectableList') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select v-model:value="temp.logPath" :placeholder="$tl('c.selectAuthPath')">
            <a-select-option key="" value="">{{ $tl('c.defaultLogPath') }}</a-select-option>
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-show="
            (javaModes.includes(temp.runMode) && temp.runMode !== 'Jar') ||
            (javaModes.includes(linkProjectData.runMode) && linkProjectData.runMode !== 'Jar')
          "
          label="Main Class"
          name="mainClass"
        >
          <a-input v-model:value="temp.mainClass" :placeholder="$tl('p.mainClass')" />
        </a-form-item>
        <a-form-item
          v-show="
            (javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp') ||
            (javaModes.includes(linkProjectData.runMode) && linkProjectData.runMode === 'JavaExtDirsCp')
          "
          label="JavaExtDirsCp"
          name="javaExtDirsCp"
        >
          <a-input
            v-model:value="temp.javaExtDirsCp"
            :placeholder="`-Dext.dirs=xxx: -cp xx  ${$tl('p.fillInXxx')}:xx】`"
          />
        </a-form-item>
        <a-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$tl('p.jvmParameters')"
          name="jvm"
        >
          <a-textarea
            v-model:value="temp.jvm"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            placeholder="jvm{{$tl('p.parameters')}},{{$tl('p.optional')}}.如：-Xms512m -Xmx512m"
          />
        </a-form-item>
        <a-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$tl('p.argsParameters')"
          name="args"
        >
          <a-textarea
            v-model:value="temp.args"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="`Main ${$tl('p.functionArgs')}. ${$tl('p.argsExample')}.port=8080`"
          />
        </a-form-item>
        <a-form-item
          v-if="temp.runMode === 'Dsl' || linkProjectData.runMode === 'Dsl'"
          name="dslEnv"
          :label="$tl('p.dslEnvVariables')"
        >
          <!-- <a-input
            v-model:value="temp.dslEnv"
            placeholder="DSL{{$tl('p.environmentVariables')}},{{$tl('p.envExample')}}=values1&keyvalue2"
          /> -->
          <parameter-widget v-model:value="temp.dslEnv"></parameter-widget>
        </a-form-item>

        <a-form-item v-show="noFileModes.includes(temp.runMode)" name="autoStart">
          <template #label>
            <a-tooltip>
              {{ $tl('p.autoStart') }}
              <template #title>{{ $tl('p.checkProjectStatusOnStartup') }}</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help>
            <div>
              {{ $tl('p.notAutoStartOnBoot') }}<b>{{ $tl('p.pluginAutoStartOnBoot') }}</b
              >{{ $tl('p.enableAutoStartSwitch') }}
            </div>
          </template>
          <div>
            <a-switch
              v-model:checked="temp.autoStart"
              :checked-children="$tl('p.switchOn')"
              :un-checked-children="$tl('p.switchOff')"
            />
            {{ $tl('p.checkAndStartOnPluginStartup') }}
          </div>
        </a-form-item>

        <a-form-item name="disableScanDir">
          <template #label>
            <a-tooltip> {{ $tl('p.disableScanning') }} </a-tooltip>
          </template>
          <template #help>
            <div>{{ $tl('p.disableScanningForLargeProjects') }}</div>
          </template>
          <div>
            <a-switch
              v-model:checked="temp.disableScanDir"
              :checked-children="$tl('p.noScanning')"
              :un-checked-children="$tl('p.enableScanning')"
            />
          </div>
        </a-form-item>

        <a-form-item v-show="noFileModes.includes(temp.runMode)" name="token">
          <template #label>
            <a-tooltip>
              WebHooks
              <template #title>
                <ul>
                  <li>{{ $tl('p.notifyUrl') }}</li>
                  <li>{{ $tl('p.notifyUrlParams') }}</li>
                  <li>type {{ $tl('p.notifyUrlValues') }}</li>
                  <li>DSL {{ $tl('p.projectSpecificTypes') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.token" :placeholder="$tl('p.optionalNotifyUrl')" />
        </a-form-item>

        <a-form-item
          v-if="temp.runCommand"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          :label="$tl('p.runCommand')"
          name="runCommand"
        >
          <a-alert :message="temp.runCommand || $tl('p.none')" type="success" />
        </a-form-item>
      </a-form>
    </a-spin>
    <!-- 配置节点授权目录 -->
    <a-modal
      v-model:open="configDir"
      destroy-on-close
      :title="`${$tl('p.authorizedDirectory')}`"
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
        :node-id="nodeId"
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 管理节点 -->
    <NodeFunc
      v-if="drawerVisible"
      :id="nodeId"
      :name="$tl('c.viewNodeScript')"
      :tabs="['scripct']"
      @close="
        () => {
          drawerVisible = false
        }
      "
    ></NodeFunc>
  </div>
</template>

<script>
import CustomSelect from '@/components/customSelect'
import NodeFunc from '@/pages/node/node-func'
import codeEditor from '@/components/codeEditor'
import { PROJECT_DSL_DEFATUL, randomStr } from '@/utils/const'
import whiteList from '@/pages/node/node-layout/system/white-list.vue'

import {
  editProject,
  getProjectAccessList,
  getProjectData,
  javaModes,
  runModeArray,
  noFileModes,
  getProjectGroupAll
} from '@/api/node-project'
import { getProjectListAll } from '@/api/node'

export default {
  components: {
    CustomSelect,
    whiteList,
    codeEditor,
    NodeFunc
  },
  props: {
    projectId: {
      type: String,
      default: ''
    },
    nodeId: {
      type: String,
      default: ''
    },
    data: { type: Object, default: null }
  },
  emits: ['close'],
  data() {
    return {
      accessList: [],
      groupList: [],
      runModeArray,
      projectList: [],
      javaModes,
      noFileModes,
      PROJECT_DSL_DEFATUL,
      configDir: false,
      temp: {},
      drawerVisible: false,
      rules: {
        id: [{ required: true, message: this.$tl('p.projectIdInput'), trigger: 'blur' }],
        name: [{ required: true, message: this.$tl('p.projectNameInput'), trigger: 'blur' }],
        runMode: [{ required: true, message: this.$tl('p.projectRunModeSelection'), trigger: 'blur' }],
        whitelistDirectory: [{ required: true, message: this.$tl('c.selectAuthPath'), trigger: 'blur' }],
        lib: [{ required: true, message: this.$tl('p.projectFolderInput'), trigger: 'blur' }]
      },
      linkProjectData: {},
      loading: true,
      dslEditTabKey: 'content'
    }
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    }
  },
  watch: {
    nodeId: {
      deep: true,

      handler() {
        this.initData()
      },

      immediate: true
    }
  },
  mounted() {
    // this.initData();
  },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.node.nodeLayout.project.projectEdit.${key}`, ...args)
    },
    randomStr,
    initData() {
      this.loadAccesList()
      this.loadGroupList()
      this.$refs['editProjectForm']?.resetFields()

      if (this.projectId) {
        // 修改
        const params = {
          id: this.projectId,
          nodeId: this.nodeId
        }

        getProjectData(params)
          .then((res) => {
            if (res.code === 200 && res.data) {
              this.temp = {
                ...res.data,
                type: 'edit'
              }
            } else {
              if (this.data) {
                // 复制项目
                this.temp = { ...this.temp, ...this.data, type: 'add' }
              }
            }
            if (this.temp.runMode === 'Link') {
              this.listProjectList()
            }
          })
          .finally(() => {
            this.loading = false
          })
      } else {
        // 新增
        this.temp = {
          type: 'add',
          logPath: ''
        }
        this.loading = false
      }
    },
    // 修改软链项目
    changeLinkId() {
      this.linkProjectData = this.projectList.find((item) => item.projectId === this.temp.linkId) || {}
    },
    // 修改运行模式
    changeRunMode() {
      if (this.temp.runMode === 'Link') {
        this.listProjectList()
      }
    },
    // 加载项目
    listProjectList() {
      if (this.projectList.length) {
        return
      }
      getProjectListAll({
        nodeId: this.nodeId
      }).then((res) => {
        if (res.code === 200) {
          this.projectList = res.data || []
          this.changeLinkId()
        }
      })
    },
    // 加载项目授权列表
    loadAccesList() {
      getProjectAccessList(this.nodeId).then((res) => {
        if (res.code === 200) {
          this.accessList = res.data
        }
      })
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },

    // 提交
    handleOk() {
      return new Promise((resolve, reject) => {
        if (this.temp.outGivingProject) {
          $notification.warning({
            message: this.$tl('p.distributionManagement')
          })
          reject(false)
          return
        }
        // 检验表单
        this.$refs['editProjectForm']
          .validate()
          .then(() => {
            const params = {
              ...this.temp,
              nodeId: this.nodeId
            }
            // 删除旧数据
            delete params.javaCopyItemList
            editProject(params).then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                resolve(true)
                this.$emit('close')
              } else {
                reject(false)
              }
            })
          })
          .catch(() => {
            reject(false)
          })
      })
    }

    // //检查节点是否存在
    // checkLibIndexExist() {
    //   // 检查是否输入完整
    //   if (this.temp.lib && this.temp.lib.length !== 0 && this.temp.whitelistDirectory && this.temp.whitelistDirectory.length !== 0) {
    //     const params = {
    //       nodeId: this.node.id,
    //       id: this.temp.id,
    //       newLib: this.temp.whitelistDirectory + this.temp.lib,
    //     };
    //     nodeJudgeLibExist(params).then((res) => {
    //       // if (res.code === 401) {
    //       //   this.temp = { ...this.temp, libExist: true, libExistMsg: res.msg };
    //       // }
    //       if (res.code !== 200) {
    //         $notification.warning({
    //           message: "提示",
    //           description: res.msg,
    //         });
    //         this.temp = { ...this.temp, libExist: true, libExistMsg: res.msg };
    //       } else {
    //         this.temp = { ...this.temp, libExist: false, libExistMsg: "" };
    //       }
    //     });
    //   }
    // },
    // handleReadFile() {

    // },
  }
}
</script>
