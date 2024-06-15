<template>
  <div>
    <!-- 编辑区 -->
    <a-spin :tip="$t('pages.node.node-layout.project.project-edit.a5c1d44')" :spinning="loading">
      <a-form ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('pages.node.node-layout.project.project-edit.4eaba425')" name="id">
          <template #help>{{ $t('pages.node.node-layout.project.project-edit.49427983') }}</template>

          <a-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$t('pages.node.node-layout.project.project-edit.92ebd5f0')"
          />
          <template v-else>
            <a-input-search
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$t('pages.node.node-layout.project.project-edit.92ebd5f0')"
              @search="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $t('pages.node.node-layout.project.project-edit.45f317b2') }} </a-button>
              </template>
            </a-input-search>
          </template>
        </a-form-item>

        <a-form-item :label="$t('pages.node.node-layout.project.project-edit.7cb5b39')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input
                v-model:value="temp.name"
                :max-length="50"
                :placeholder="$t('pages.node.node-layout.project.project-edit.7cb5b39')"
              />
            </a-col>
            <a-col :span="4" style="text-align: right">{{
              $t('pages.node.node-layout.project.project-edit.12d0e469')
            }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$t('pages.node.node-layout.project.project-edit.95c41d82')"
                  :select-placeholder="$t('pages.node.node-layout.project.project-edit.761c903a')"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="runMode">
          <template #label>
            <a-tooltip>
              {{ $t('pages.node.node-layout.project.project-edit.eaf004ca') }}
              <template #title>
                <ul>
                  <li><b>Dsl</b> {{ $t('pages.node.node-layout.project.project-edit.64759ad3') }}</li>
                  <li>
                    <b>ClassPath</b> java -classpath xxx {{ $t('pages.node.node-layout.project.project-edit.788bfc') }}
                  </li>
                  <li><b>Jar</b> java -jar xxx {{ $t('pages.node.node-layout.project.project-edit.788bfc') }}</li>
                  <li>
                    <b>JarWar</b> java -jar Springboot war
                    {{ $t('pages.node.node-layout.project.project-edit.788bfc') }}
                  </li>
                  <li>
                    <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
                    {{ $t('pages.node.node-layout.project.project-edit.788bfc') }}
                  </li>
                  <li>
                    <b>File</b> {{ $t('pages.node.node-layout.project.project-edit.d33f4295') }},{{
                      $t('pages.node.node-layout.project.project-edit.35f7833f')
                    }}
                  </li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="temp.runMode"
            :placeholder="$t('pages.node.node-layout.project.project-edit.9f508cc8')"
            @change="changeRunMode"
          >
            <a-select-option v-for="item in runModeArray" :key="item.name">
              <template v-if="item.desc.indexOf($t('pages.node.node-layout.project.project-edit.ad7005ba')) > -1">
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
          <a-form-item :label="$t('pages.node.node-layout.project.project-edit.4b36f25f')" name="linkId">
            <a-select
              v-model:value="temp.linkId"
              :placeholder="$t('pages.node.node-layout.project.project-edit.9ac42fe6')"
              @change="changeLinkId"
            >
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
                {{ $t('pages.node.node-layout.project.project-edit.5b716424') }}
                <template #title>
                  <ul>
                    <li>{{ $t('pages.node.node-layout.project.project-edit.9d024ef5') }}</li>
                    <li>{{ $t('pages.node.node-layout.project.project-edit.80b551d6') }}</li>
                    <li>{{ $t('pages.node.node-layout.project.project-edit.28952768') }}</li>
                    <li>
                      {{ $t('pages.node.node-layout.project.project-edit.528d80cb') }} <br />&nbsp;&nbsp;<b>{{
                        $t('pages.node.node-layout.project.project-edit.5ef24c2f')
                      }}</b>
                    </li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <template #help>
              <div>
                {{ $t('pages.node.node-layout.project.project-edit.20f66fff') }}
                <a-button
                  type="link"
                  size="small"
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $t('pages.node.node-layout.project.project-edit.2ebdf25e') }}
                </a-button>
              </div>
            </template>
            <a-input-group compact>
              <a-select
                v-model:value="temp.whitelistDirectory"
                style="width: 50%"
                :placeholder="$t('pages.node.node-layout.project.project-edit.6fa628c8')"
              >
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">{{ access }}</a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input
                  v-model:value="temp.lib"
                  style="width: 50%"
                  :placeholder="$t('pages.node.node-layout.project.project-edit.d4e69bd6')"
                />
              </a-form-item-rest>
            </a-input-group>
            <template #extra>
              <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" :label="$t('pages.node.node-layout.project.project-edit.2f9f87a5')">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
        </template>
        <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
          <template #label>
            <a-tooltip>
              DSL {{ $t('pages.node.node-layout.project.project-edit.99ff48c8') }}
              <template #title>
                <p>{{ $t('pages.node.node-layout.project.project-edit.e79a829d') }}</p>
                <p>{{ $t('pages.node.node-layout.project.project-edit.56b96c89') }}</p>
                <p>
                  <b>status</b>
                  {{ $t('pages.node.node-layout.project.project-edit.3cbddbbe') }}:$pid
                  <b>$pid {{ $t('pages.node.node-layout.project.project-edit.3cf2b4f7') }}</b
                  >{{ $t('pages.node.node-layout.project.project-edit.b76bdd94') }}
                </p>
                <p>{{ $t('pages.node.node-layout.project.project-edit.f26fd1f1') }}</p>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help>
            <!-- <a-space>
              <template #split>
                <a-divider type="vertical" />
              </template> -->
            <div>
              scriptId{{ $t('pages.node.node-layout.project.project-edit.2695c530') }}
              <a-button
                type="link"
                size="small"
                @click="
                  () => {
                    drawerVisible = true
                  }
                "
              >
                {{ $t('pages.node.node-layout.project.project-edit.41ceb72c') }}
              </a-button>
            </div>
            <div>scriptId也可以引入脚本库中的脚本,需要提前同步至机器节点中</div>
            <!-- </a-space> -->
          </template>
          <a-form-item-rest>
            <code-editor
              v-show="dslEditTabKey === 'content'"
              v-model:content="temp.dslContent"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'yaml', tabSize: 2 }"
              :placeholder="$t('pages.node.node-layout.project.project-edit.89967495')"
            >
              <template #tool_before>
                <a-segmented
                  v-model:value="dslEditTabKey"
                  :options="[
                    { label: `DSL ${$t('pages.node.node-layout.project.project-edit.28f9e270')}`, value: 'content' },
                    { label: $t('pages.node.node-layout.project.project-edit.a3186ee5'), value: 'demo' }
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
                    { label: `DSL ${$t('pages.node.node-layout.project.project-edit.28f9e270')}`, value: 'content' },
                    { label: $t('pages.node.node-layout.project.project-edit.a3186ee5'), value: 'demo' }
                  ]"
                />
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item v-show="noFileModes.includes(temp.runMode) && temp.runMode !== 'Link'">
          <template #label>
            <a-tooltip>
              {{ $t('pages.node.node-layout.project.project-edit.e9f209dd') }}
              <template #title>
                <ul>
                  <li>{{ $t('pages.node.node-layout.project.project-edit.8f348042') }}</li>
                  <li>{{ $t('pages.node.node-layout.project.project-edit.1008ec50') }}</li>
                  <li>{{ $t('pages.node.node-layout.project.project-edit.a3cf55e2') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select
            v-model:value="temp.logPath"
            :placeholder="$t('pages.node.node-layout.project.project-edit.6fa628c8')"
          >
            <a-select-option key="" value="">{{
              $t('pages.node.node-layout.project.project-edit.1008ec50')
            }}</a-select-option>
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
          <a-input
            v-model:value="temp.mainClass"
            :placeholder="$t('pages.node.node-layout.project.project-edit.b324b030')"
          />
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
            :placeholder="`-Dext.dirs=xxx: -cp xx  ${$t('pages.node.node-layout.project.project-edit.4241b582')}:xx】`"
          />
        </a-form-item>
        <a-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$t('pages.node.node-layout.project.project-edit.19af9418')"
          name="jvm"
        >
          <a-textarea
            v-model:value="temp.jvm"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="
              $t('pages.node.node-layout.project.project-edit.17924912', {
                slot1: $t('pages.node.node-layout.project.project-edit.6e6cc3c2'),
                slot2: $t('pages.node.node-layout.project.project-edit.a72cfae2')
              })
            "
          />
        </a-form-item>
        <a-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$t('pages.node.node-layout.project.project-edit.c5feb1a')"
          name="args"
        >
          <a-textarea
            v-model:value="temp.args"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="`Main ${$t('pages.node.node-layout.project.project-edit.ee6f8f43')}. ${$t(
              'pages.node.node-layout.project.project-edit.18629de'
            )}.port=8080`"
          />
        </a-form-item>
        <a-form-item
          v-if="temp.runMode === 'Dsl' || linkProjectData.runMode === 'Dsl'"
          name="dslEnv"
          :label="$t('pages.node.node-layout.project.project-edit.ede504be')"
        >
          <!-- <a-input
            v-model:value="temp.dslEnv"
            placeholder="DSL{{$t('pages.node.node-layout.project.project-edit.c81b2c2e')}},{{$t('pages.node.node-layout.project.project-edit.7e189322')}}=values1&keyvalue2"
          /> -->
          <parameter-widget v-model:value="temp.dslEnv"></parameter-widget>
        </a-form-item>

        <a-form-item v-show="noFileModes.includes(temp.runMode)" name="autoStart">
          <template #label>
            <a-tooltip>
              {{ $t('pages.node.node-layout.project.project-edit.12861e4e') }}
              <template #title>{{ $t('pages.node.node-layout.project.project-edit.be7082a2') }}</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help>
            <div>
              {{ $t('pages.node.node-layout.project.project-edit.b35cbffb')
              }}<b>{{ $t('pages.node.node-layout.project.project-edit.297af6c7') }}</b
              >{{ $t('pages.node.node-layout.project.project-edit.53c6c542') }}
            </div>
          </template>
          <div>
            <a-switch
              v-model:checked="temp.autoStart"
              :checked-children="$t('pages.node.node-layout.project.project-edit.c5f50974')"
              :un-checked-children="$t('pages.node.node-layout.project.project-edit.e9a7e1c4')"
            />
            {{ $t('pages.node.node-layout.project.project-edit.f32199a3') }}
          </div>
        </a-form-item>

        <a-form-item name="disableScanDir">
          <template #label>
            <a-tooltip> {{ $t('pages.node.node-layout.project.project-edit.217ba8f3') }} </a-tooltip>
          </template>
          <template #help>
            <div>{{ $t('pages.node.node-layout.project.project-edit.3342e1c4') }}</div>
          </template>
          <div>
            <a-switch
              v-model:checked="temp.disableScanDir"
              :checked-children="$t('pages.node.node-layout.project.project-edit.f78bec3f')"
              :un-checked-children="$t('pages.node.node-layout.project.project-edit.fc6ab345')"
            />
          </div>
        </a-form-item>

        <a-form-item v-show="noFileModes.includes(temp.runMode)" name="token">
          <template #label>
            <a-tooltip>
              WebHooks
              <template #title>
                <ul>
                  <li>{{ $t('pages.node.node-layout.project.project-edit.5db82550') }}</li>
                  <li>{{ $t('pages.node.node-layout.project.project-edit.316f78d0') }}</li>
                  <li>type {{ $t('pages.node.node-layout.project.project-edit.84063ac5') }}</li>
                  <li>DSL {{ $t('pages.node.node-layout.project.project-edit.d4d5dc62') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="temp.token"
            :placeholder="$t('pages.node.node-layout.project.project-edit.8b1eb070')"
          />
        </a-form-item>

        <a-form-item
          v-if="temp.runCommand"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          :label="$t('pages.node.node-layout.project.project-edit.da878eca')"
          name="runCommand"
        >
          <a-alert
            :message="temp.runCommand || $t('pages.node.node-layout.project.project-edit.9ac5e2d4')"
            type="success"
          />
        </a-form-item>
      </a-form>
    </a-spin>
    <!-- 配置节点授权目录 -->
    <a-modal
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('pages.node.node-layout.project.project-edit.277cb48f')}`"
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
      :name="$t('pages.node.node-layout.project.project-edit.41ceb72c')"
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
        id: [
          { required: true, message: this.$t('pages.node.node-layout.project.project-edit.6916e3ff'), trigger: 'blur' }
        ],

        name: [
          { required: true, message: this.$t('pages.node.node-layout.project.project-edit.bf4a219b'), trigger: 'blur' }
        ],

        runMode: [
          { required: true, message: this.$t('pages.node.node-layout.project.project-edit.4551702c'), trigger: 'blur' }
        ],

        whitelistDirectory: [
          { required: true, message: this.$t('pages.node.node-layout.project.project-edit.6fa628c8'), trigger: 'blur' }
        ],

        lib: [
          { required: true, message: this.$t('pages.node.node-layout.project.project-edit.b5d7203a'), trigger: 'blur' }
        ]
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
            message: this.$t('pages.node.node-layout.project.project-edit.d074a68')
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
