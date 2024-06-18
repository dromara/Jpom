<template>
  <div>
    <!-- 编辑区 -->
    <a-spin :tip="$t('i18n_2770db3a99')" :spinning="loading">
      <a-form ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="$t('i18n_4fdd2213b5')" name="id">
          <template #help>{{ $t('i18n_e2b0f27424') }}</template>

          <a-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$t('i18n_7ce511154f')"
          />
          <template v-else>
            <a-input-search
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$t('i18n_7ce511154f')"
              @search="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary"> {{ $t('i18n_6709f4548f') }} </a-button>
              </template>
            </a-input-search>
          </template>
        </a-form-item>

        <a-form-item :label="$t('i18n_738a41f965')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_738a41f965')" />
            </a-col>
            <a-col :span="4" style="text-align: right">{{ $t('i18n_1b973fc4d1') }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$t('i18n_bd0362bed3')"
                  :select-placeholder="$t('i18n_3e8c9c54ee')"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="runMode">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_17d444b642') }}
              <template #title>
                <ul>
                  <li><b>Dsl</b> {{ $t('i18n_2356fe4af2') }}</li>
                  <li><b>ClassPath</b> java -classpath xxx {{ $t('i18n_fa4aa1b93b') }}</li>
                  <li><b>Jar</b> java -jar xxx {{ $t('i18n_fa4aa1b93b') }}</li>
                  <li>
                    <b>JarWar</b> java -jar Springboot war
                    {{ $t('i18n_fa4aa1b93b') }}
                  </li>
                  <li>
                    <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
                    {{ $t('i18n_fa4aa1b93b') }}
                  </li>
                  <li><b>File</b> {{ $t('i18n_5d6f47d670') }},{{ $t('i18n_61955b0e4b') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select v-model:value="temp.runMode" :placeholder="$t('i18n_26a3378645')" @change="changeRunMode">
            <a-select-option v-for="item in runModeArray" :key="item.name">
              <template v-if="item.desc.indexOf($t('i18n_888df7a89e')) > -1">
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
          <a-form-item :label="$t('i18n_be166de983')" name="linkId">
            <a-select v-model:value="temp.linkId" :placeholder="$t('i18n_1ba141c9ac')" @change="changeLinkId">
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
                {{ $t('i18n_aabdc3b7c0') }}
                <template #title>
                  <ul>
                    <li>{{ $t('i18n_f89cc4807e') }}</li>
                    <li>{{ $t('i18n_94763baf5f') }}</li>
                    <li>{{ $t('i18n_fe828cefd9') }}</li>
                    <li>
                      {{ $t('i18n_556499017a') }} <br />&nbsp;&nbsp;<b>{{ $t('i18n_67141abed6') }}</b>
                    </li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <template #help>
              <div>
                {{ $t('i18n_fde1b6fb37') }}
                <a-button
                  type="link"
                  size="small"
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $t('i18n_23b444d24c') }}
                </a-button>
              </div>
            </template>
            <a-input-group compact>
              <a-select v-model:value="temp.whitelistDirectory" style="width: 50%" :placeholder="$t('i18n_1d38b2b2bc')">
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">{{ access }}</a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input v-model:value="temp.lib" style="width: 50%" :placeholder="$t('i18n_1dc518bddb')" />
              </a-form-item-rest>
            </a-input-group>
            <template #extra>
              <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" :label="$t('i18n_8283f063d7')">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
        </template>
        <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
          <template #label>
            <a-tooltip>
              DSL {{ $t('i18n_2d711b09bd') }}
              <template #title>
                <p>{{ $t('i18n_73d8160821') }}</p>
                <p>{{ $t('i18n_3517aa30c2') }}</p>
                <p>
                  <b>status</b>
                  {{ $t('i18n_ca69dad8fc') }}:$pid <b>$pid {{ $t('i18n_07a8af8c03') }}</b
                  >{{ $t('i18n_d2f484ff7e') }}
                </p>
                <p>{{ $t('i18n_9f52492fbc') }}</p>
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
              scriptId{{ $t('i18n_21da885538') }}
              <a-button
                type="link"
                size="small"
                @click="
                  () => {
                    drawerVisible = true
                  }
                "
              >
                {{ $t('i18n_35134b6f94') }}
              </a-button>
            </div>
            <div>{{ $t('i18n_6a359e2ab3') }}</div>
            <!-- </a-space> -->
          </template>
          <a-form-item-rest>
            <code-editor
              v-show="dslEditTabKey === 'content'"
              v-model:content="temp.dslContent"
              height="40vh"
              :show-tool="true"
              :options="{ mode: 'yaml', tabSize: 2 }"
              :placeholder="$t('i18n_1c8190b0eb')"
            >
              <template #tool_before>
                <a-segmented
                  v-model:value="dslEditTabKey"
                  :options="[
                    { label: `DSL ${$t('i18n_224e2ccda8')}`, value: 'content' },
                    { label: $t('i18n_da79c2ec32'), value: 'demo' }
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
                    { label: `DSL ${$t('i18n_224e2ccda8')}`, value: 'content' },
                    { label: $t('i18n_da79c2ec32'), value: 'demo' }
                  ]"
                />
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item v-show="noFileModes.includes(temp.runMode) && temp.runMode !== 'Link'">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_2ce44aba57') }}
              <template #title>
                <ul>
                  <li>{{ $t('i18n_12934d1828') }}</li>
                  <li>{{ $t('i18n_138776a1dc') }}</li>
                  <li>{{ $t('i18n_95c5c939e4') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select v-model:value="temp.logPath" :placeholder="$t('i18n_1d38b2b2bc')">
            <a-select-option key="" value="">{{ $t('i18n_138776a1dc') }}</a-select-option>
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
          <a-input v-model:value="temp.mainClass" :placeholder="$t('i18n_ef800ed466')" />
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
            :placeholder="`-Dext.dirs=xxx: -cp xx  ${$t('i18n_c53021f06d')}:xx】`"
          />
        </a-form-item>
        <a-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$t('i18n_497bc3532b')"
          name="jvm"
        >
          <a-textarea
            v-model:value="temp.jvm"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="$t('i18n_eef3653e9a', { slot1: $t('i18n_3d0a2df9ec'), slot2: $t('i18n_eb5bab1c31') })"
          />
        </a-form-item>
        <a-form-item
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
          :label="$t('i18n_e5098786d3')"
          name="args"
        >
          <a-textarea
            v-model:value="temp.args"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            :placeholder="`Main ${$t('i18n_6a9231c3ba')}. ${$t('i18n_848e4e21da')}.port=8080`"
          />
        </a-form-item>
        <a-form-item
          v-if="temp.runMode === 'Dsl' || linkProjectData.runMode === 'Dsl'"
          name="dslEnv"
          :label="$t('i18n_fba5f4f19a')"
        >
          <!-- <a-input
            v-model:value="temp.dslEnv"
            placeholder="DSL{{$t('i18n_3867e350eb')}},{{$t('i18n_9324290bfe')}}=values1&keyvalue2"
          /> -->
          <parameter-widget v-model:value="temp.dslEnv"></parameter-widget>
        </a-form-item>

        <a-form-item v-show="noFileModes.includes(temp.runMode)" name="autoStart">
          <template #label>
            <a-tooltip>
              {{ $t('i18n_8388c637f6') }}
              <template #title>{{ $t('i18n_d4e03f60a9') }}</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help>
            <div>
              {{ $t('i18n_71584de972') }}<b>{{ $t('i18n_1e4a59829d') }}</b
              >{{ $t('i18n_0360fffb40') }}
            </div>
          </template>
          <div>
            <a-switch
              v-model:checked="temp.autoStart"
              :checked-children="$t('i18n_8493205602')"
              :un-checked-children="$t('i18n_d58a55bcee')"
            />
            {{ $t('i18n_1022c545d1') }}
          </div>
        </a-form-item>

        <a-form-item name="disableScanDir">
          <template #label>
            <a-tooltip> {{ $t('i18n_df59a2804d') }} </a-tooltip>
          </template>
          <template #help>
            <div>{{ $t('i18n_b7c139ed75') }}</div>
          </template>
          <div>
            <a-switch
              v-model:checked="temp.disableScanDir"
              :checked-children="$t('i18n_ced3d28cd1')"
              :un-checked-children="$t('i18n_56525d62ac')"
            />
          </div>
        </a-form-item>

        <a-form-item v-show="noFileModes.includes(temp.runMode)" name="token">
          <template #label>
            <a-tooltip>
              WebHooks
              <template #title>
                <ul>
                  <li>{{ $t('i18n_a24d80c8fa') }}</li>
                  <li>{{ $t('i18n_b91961bf0b') }}</li>
                  <li>type {{ $t('i18n_5a63277941') }}</li>
                  <li>DSL {{ $t('i18n_f8f456eb9a') }}</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.token" :placeholder="$t('i18n_6c776e9d91')" />
        </a-form-item>

        <a-form-item
          v-if="temp.runCommand"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          :label="$t('i18n_ce559ba296')"
          name="runCommand"
        >
          <a-alert :message="temp.runCommand || $t('i18n_d81bb206a8')" type="success" />
        </a-form-item>
      </a-form>
    </a-spin>
    <!-- 配置节点授权目录 -->
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
        :node-id="nodeId"
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </CustomModal>
    <!-- 管理节点 -->
    <NodeFunc
      v-if="drawerVisible"
      :id="nodeId"
      :name="$t('i18n_35134b6f94')"
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
import { randomStr } from '@/utils/const'
import { PROJECT_DSL_DEFATUL } from '@/utils/const-i18n'
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
        id: [{ required: true, message: this.$t('i18n_646a518953'), trigger: 'blur' }],

        name: [{ required: true, message: this.$t('i18n_4371e2b426'), trigger: 'blur' }],

        runMode: [{ required: true, message: this.$t('i18n_4310e9ed7d'), trigger: 'blur' }],

        whitelistDirectory: [{ required: true, message: this.$t('i18n_1d38b2b2bc'), trigger: 'blur' }],

        lib: [{ required: true, message: this.$t('i18n_d9657e2b5f'), trigger: 'blur' }]
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
            message: this.$t('i18n_869b506d66')
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
