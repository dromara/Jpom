<template>
  <div>
    <!-- 编辑区 -->
    <a-spin tip="加载项目数据中..." :spinning="loading">
      <a-form ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="项目 ID" name="id">
          <a-input
            :maxLength="50"
            v-model:value="temp.id"
            v-if="temp.type === 'edit'"
            :disabled="temp.type === 'edit'"
            placeholder="创建之后不能修改"
          />
          <template v-else>
            <a-input-search
              :maxLength="50"
              v-model:value="temp.id"
              placeholder="创建之后不能修改"
              @search="
                () => {
                  this.temp = { ...this.temp, id: randomStr(6) }
                }
              "
            >
              <template v-slot:enterButton>
                <a-button type="primary"> 随机生成 </a-button>
              </template>
            </a-input-search>
          </template>
        </a-form-item>

        <a-form-item label="项目名称" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.name" :maxLength="50" placeholder="项目名称" />
            </a-col>
            <a-col :span="4" style="text-align: right">分组名称：</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  :maxLength="50"
                  v-model:value="temp.group"
                  :data="groupList"
                  inputPlaceholder="新增分组"
                  selectPlaceholder="选择分组"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="runMode">
          <template v-slot:label>
            <a-tooltip>
              运行方式
              <template v-slot:title>
                <ul>
                  <li><b>Dsl</b> 配合脚本模版实现自定义项目管理</li>
                  <li><b>ClassPath</b> java -classpath xxx 运行项目</li>
                  <li><b>Jar</b> java -jar xxx 运行项目</li>
                  <li><b>JarWar</b> java -jar Springboot war 运行项目</li>
                  <li><b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS 运行项目</li>
                  <li><b>File</b> 项目为静态文件夹,没有项目状态以及控制等功能</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select v-model:value="temp.runMode" placeholder="请选择运行方式" @change="changeRunMode">
            <a-select-option v-for="item in runModeArray" :key="item.name">
              <template v-if="item.desc.indexOf('不推荐') > -1">
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
          <a-form-item label="软链的项目" name="linkId">
            <a-select v-model:value="temp.linkId" placeholder="请选择软链的项目" @change="changeLinkId">
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
            <template v-slot:label>
              <a-tooltip>
                项目路径
                <template v-slot:title>
                  <ul>
                    <li>授权路径是指项目文件存放到服务中的文件夹</li>
                    <li>可以到节点管理中的【插件端配置】=>【授权配置】修改</li>
                    <li>项目文件夹是项目实际存放的目录名称</li>
                    <li>项目文件会存放到 <br />&nbsp;&nbsp;<b>项目授权路径+项目文件夹</b></li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <template #help>
              需要提前为机器配置授权目录
              <a-button
                type="link"
                size="small"
                @click="
                  () => {
                    configDir = true
                  }
                "
              >
                <InfoCircleOutlined /> 快速配置
              </a-button>
            </template>
            <a-input-group compact>
              <a-select style="width: 50%" v-model:value="temp.whitelistDirectory" placeholder="请选择项目授权路径">
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">{{ access }}</a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input style="width: 50%" v-model:value="temp.lib" placeholder="项目存储的文件夹" />
              </a-form-item-rest>
            </a-input-group>
            <template #extra>
              <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" label="项目完整目录">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
        </template>
        <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
          <template v-slot:label>
            <a-tooltip>
              DSL 内容
              <template v-slot:title>
                <p>
                  以 yaml/yml 格式配置,scriptId
                  为项目路径下的脚本文件的相对路径或者脚本模版ID，可以到脚本模版编辑弹窗中查看 scriptId
                </p>
                <p>脚本里面支持的变量有：${PROJECT_ID}、${PROJECT_NAME}、${PROJECT_PATH}</p>
                <p>
                  <b>status</b>
                  流程执行完脚本后，输出的内容最后一行必须为：running:$pid
                  <b>$pid 为当前项目实际的进程ID</b>。如果输出最后一行不是预期格式项目状态将是未运行
                </p>
                <p>配置详情请参考配置示例</p>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help>
            scriptId可以使用节点脚本：
            <a-button
              type="link"
              size="small"
              @click="
                () => {
                  drawerVisible = true
                }
              "
            >
              查看节点脚本
            </a-button>
          </template>
          <a-form-item-rest>
            <code-editor
              height="40vh"
              :showTool="true"
              v-model:content="temp.dslContent"
              :options="{ mode: 'yaml', tabSize: 2 }"
              v-show="dslEditTabKey === 'content'"
              placeholder="请填写项目 DSL 配置内容,可以点击上方切换 tab 查看配置示例"
            >
              <template #tool_before>
                <a-segmented
                  v-model:value="dslEditTabKey"
                  :options="[
                    { label: 'DSL 配置', value: 'content' },
                    { label: '配置示例', value: 'demo' }
                  ]"
                />
              </template>
            </code-editor>
            <code-editor
              v-show="dslEditTabKey === 'demo'"
              height="40vh"
              :showTool="true"
              v-model:content="PROJECT_DSL_DEFATUL"
              :options="{ mode: 'yaml', tabSize: 2, readOnly: true }"
            >
              <template #tool_before>
                <a-segmented
                  v-model:value="dslEditTabKey"
                  :options="[
                    { label: 'DSL 配置', value: 'content' },
                    { label: '配置示例', value: 'demo' }
                  ]"
                />
              </template>
            </code-editor>
          </a-form-item-rest>
        </a-form-item>
        <a-form-item v-show="noFileModes.includes(temp.runMode) && temp.runMode !== 'Link'">
          <template v-slot:label>
            <a-tooltip>
              日志目录
              <template v-slot:title>
                <ul>
                  <li>日志目录是指控制台日志存储目录</li>
                  <li>默认是在插件端数据目录/${projectId}/${projectId}.log</li>
                  <li>可选择的列表和项目授权目录是一致的，即相同配置</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-select v-model:value="temp.logPath" placeholder="请选择项目授权路径">
            <a-select-option key="" value="">默认是在插件端数据目录/${projectId}/${projectId}.log</a-select-option>
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="Main Class"
          name="mainClass"
          v-show="
            (javaModes.includes(temp.runMode) && temp.runMode !== 'Jar') ||
            (javaModes.includes(linkProjectData.runMode) && linkProjectData.runMode !== 'Jar')
          "
        >
          <a-input v-model:value="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)" />
        </a-form-item>
        <a-form-item
          label="JavaExtDirsCp"
          name="javaExtDirsCp"
          v-show="
            (javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp') ||
            (javaModes.includes(linkProjectData.runMode) && linkProjectData.runMode === 'JavaExtDirsCp')
          "
        >
          <a-input v-model:value="temp.javaExtDirsCp" placeholder="-Dext.dirs=xxx: -cp xx  填写【xxx:xx】" />
        </a-form-item>
        <a-form-item
          label="JVM 参数"
          name="jvm"
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
        >
          <a-textarea
            v-model:value="temp.jvm"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m"
          />
        </a-form-item>
        <a-form-item
          label="args 参数"
          name="args"
          v-show="javaModes.includes(temp.runMode) || javaModes.includes(linkProjectData.runMode)"
        >
          <a-textarea
            v-model:value="temp.args"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080"
          />
        </a-form-item>
        <a-form-item
          v-if="temp.runMode === 'Dsl' || linkProjectData.runMode === 'Dsl'"
          name="dslEnv"
          label="DSL环境变量"
        >
          <a-input v-model:value="temp.dslEnv" placeholder="DSL环境变量,如：key1=values1&keyvalue2" />
        </a-form-item>

        <a-form-item name="autoStart" v-show="noFileModes.includes(temp.runMode)">
          <template v-slot:label>
            <a-tooltip>
              自启动
              <template v-slot:title>插件端启动的时候检查项目状态，如果项目状态是未运行则尝试执行启动项目</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <template #help><div>非服务器开机自启,如需开机自启建议配置<b>插件端开机自启</b>并开启此开关</div></template>
          <a-switch v-model:checked="temp.autoStart" checked-children="开" un-checked-children="关" />
          插件端启动时自动检查项目如未启动将尝试启动
        </a-form-item>

        <a-form-item name="token" v-show="noFileModes.includes(temp.runMode)">
          <template v-slot:label>
            <a-tooltip>
              WebHooks
              <template v-slot:title>
                <ul>
                  <li>项目启动,停止,重启,文件变动都将请求对应的地址</li>
                  <li>传入参数有：projectId、projectName、type、result</li>
                  <li>type 的值有：stop、beforeStop、start、beforeRestart、fileChange</li>
                  <li>DSL 类型项目特有的 type：reload、restart</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input
            v-model:value="temp.token"
            placeholder="项目启动,停止,重启,文件变动都将请求对应的地址,非必填，GET请求"
          />
        </a-form-item>

        <a-form-item
          v-if="temp.runCommand"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          label="运行命令"
          name="runCommand"
        >
          <a-alert :message="temp.runCommand || '无'" type="success" />
        </a-form-item>
      </a-form>
    </a-spin>
    <!-- 配置节点授权目录 -->
    <a-modal
      destroyOnClose
      v-model:open="configDir"
      :title="`配置授权目录`"
      :footer="null"
      :maskClosable="false"
      @cancel="
        () => {
          this.configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        :nodeId="this.nodeId"
        @cancel="
          () => {
            this.configDir = false
            this.loadAccesList()
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 管理节点 -->
    <NodeFunc
      v-if="drawerVisible"
      name="查看节点脚本"
      :id="nodeId"
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
  props: {
    projectId: {
      type: String,
      default: ''
    },
    nodeId: {
      type: String
    },
    data: { type: Object, default: null }
  },
  components: {
    CustomSelect,
    whiteList,
    codeEditor,
    NodeFunc
  },
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
        id: [{ required: true, message: '请输入项目ID', trigger: 'blur' }],
        name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
        runMode: [{ required: true, message: '请选择项目运行方式', trigger: 'blur' }],
        whitelistDirectory: [{ required: true, message: '请选择项目授权路径', trigger: 'blur' }],
        lib: [{ required: true, message: '请输入项目文件夹', trigger: 'blur' }]
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
            message: '独立的项目分发请到分发管理中去修改'
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
  },
  emits: ['close']
}
</script>
