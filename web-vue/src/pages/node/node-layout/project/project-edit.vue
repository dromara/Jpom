<template>
  <!-- 编辑区 -->

  <a-form-model ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
    <a-form-model-item label="项目 ID" prop="id">
      <a-input :maxLength="50" v-model="temp.id" v-if="temp.type === 'edit'" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" />
      <template v-else>
        <a-input-search
          :maxLength="50"
          v-model="temp.id"
          placeholder="创建之后不能修改"
          @search="
            () => {
              this.temp = { ...this.temp, id: randomStr(6) };
            }
          "
        >
          <template slot="enterButton">
            <a-button type="primary"> 随机生成 </a-button>
          </template>
        </a-input-search>
      </template>
    </a-form-model-item>

    <a-form-model-item label="项目名称" prop="name">
      <a-row>
        <a-col :span="10">
          <a-input v-model="temp.name" :maxLength="50" placeholder="项目名称" />
        </a-col>
        <a-col :span="4" style="text-align: right">分组名称：</a-col>
        <a-col :span="10">
          <custom-select suffixIcon="" :maxLength="50" v-model="temp.group" :data="groupList" inputPlaceholder="添加分组" selectPlaceholder="选择分组"> </custom-select>
        </a-col>
      </a-row>
    </a-form-model-item>
    <a-form-model-item prop="runMode">
      <template slot="label">
        <a-tooltip>
          运行方式
          <template slot="title">
            <ul>
              <li><b>Dsl</b> 配合脚本模版实现自定义项目管理</li>
              <li><b>ClassPath</b> java -classpath xxx 运行项目</li>
              <li><b>Jar</b> java -jar xxx 运行项目</li>
              <li><b>JarWar</b> java -jar Springboot war 运行项目</li>
              <li><b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS 运行项目</li>
              <li><b>File</b> 项目为静态文件夹,没有项目状态以及控制等功能</li>
            </ul>
          </template>
          <a-icon v-show="temp.type !== 'edit'" type="question-circle" theme="filled" />
        </a-tooltip>
      </template>
      <a-select v-model="temp.runMode" placeholder="请选择运行方式">
        <a-select-option v-for="runMode in runModeList" :key="runMode">{{ runMode }}</a-select-option>
      </a-select>
    </a-form-model-item>
    <a-form-model-item prop="whitelistDirectory" class="jpom-node-project-whitelist">
      <template slot="label">
        <a-tooltip>
          项目路径
          <template slot="title">
            <ul>
              <li>授权路径是指项目文件存放到服务中的文件夹</li>
              <li>可以到节点管理中的【插件端配置】=>【授权配置】修改</li>
              <li>项目文件夹是项目实际存放的目录名称</li>
              <li>项目文件会存放到 <br />&nbsp;&nbsp;<b>项目授权路径+项目文件夹</b></li>
            </ul>
          </template>
          <a-icon v-show="temp.type !== 'edit'" type="question-circle" theme="filled" />
        </a-tooltip>
      </template>
      <a-input-group compact>
        <a-select style="width: 50%" v-model="temp.whitelistDirectory" placeholder="请选择项目授权路径">
          <a-select-option v-for="access in accessList" :key="access">
            <a-tooltip :title="access">{{ access }}</a-tooltip>
          </a-select-option>
        </a-select>
        <a-input style="width: 50%" v-model="temp.lib" placeholder="项目存储的文件夹" />
      </a-input-group>
      <template #extra>
        <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
      </template>
    </a-form-model-item>
    <!-- <a-form-model-item prop="lib">
          <template slot="label">
            项目文件夹
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul></ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
        </a-form-model-item> -->
    <a-form-model-item v-show="filePath !== ''" label="项目完整目录">
      <a-alert :message="filePath" type="success" />
    </a-form-model-item>
    <a-form-model-item v-show="temp.runMode === 'Dsl'" prop="dslContent">
      <template slot="label">
        <a-tooltip>
          DSL 内容
          <template slot="title">
            <p>以 yaml/yml 格式配置,scriptId 为项目路径下的脚本文件的相对路径或者脚本模版ID，可以到脚本模版编辑弹窗中查看 scriptId</p>
            <p>脚本里面支持的变量有：${PROJECT_ID}、${PROJECT_NAME}、${PROJECT_PATH}</p>
            <p><b>status</b> 流程执行完脚本后，输出的内容最后一行必须为：running:$pid <b>$pid 为当前项目实际的进程ID</b>。如果输出最后一行不是预期格式项目状态将是未运行</p>
            <p>配置详情请参考配置示例</p>
          </template>
          <a-icon v-show="temp.type !== 'edit'" type="question-circle" theme="filled" />
        </a-tooltip>
      </template>
      <a-tabs>
        <a-tab-pane key="1" tab="DSL 配置">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="temp.dslContent" :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef' }"></code-editor>
          </div>
        </a-tab-pane>
        <a-tab-pane key="2" tab="配置示例">
          <div style="height: 40vh; overflow-y: scroll">
            <code-editor v-model="PROJECT_DSL_DEFATUL" :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef', readOnly: true }"></code-editor>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-form-model-item>
    <a-form-model-item v-show="noFileModes.includes(temp.runMode)">
      <template slot="label">
        <a-tooltip>
          日志目录
          <template slot="title">
            <ul>
              <li>日志目录是指控制台日志存储目录</li>
              <li>默认是在项目文件夹父级</li>
              <li>可选择的列表和项目授权目录是一致的，即相同配置</li>
            </ul>
          </template>
          <a-icon v-show="temp.type !== 'edit'" type="question-circle" theme="filled" />
        </a-tooltip>
      </template>
      <a-select v-model="temp.logPath" placeholder="请选择项目授权路径">
        <a-select-option key="" value="">默认是在项目文件夹父级</a-select-option>
        <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
      </a-select>
    </a-form-model-item>

    <a-form-model-item label="Main Class" prop="mainClass" v-show="javaModes.includes(temp.runMode) && temp.runMode !== 'Jar'">
      <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)" />
    </a-form-model-item>
    <a-form-model-item label="JavaExtDirsCp" prop="javaExtDirsCp" v-show="javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp'">
      <a-input v-model="temp.javaExtDirsCp" placeholder="-Dext.dirs=xxx: -cp xx  填写【xxx:xx】" />
    </a-form-model-item>
    <a-form-model-item label="JVM 参数" prop="jvm" v-show="javaModes.includes(temp.runMode)">
      <a-textarea v-model="temp.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
    </a-form-model-item>
    <a-form-model-item label="args 参数" prop="args" v-show="javaModes.includes(temp.runMode)">
      <a-textarea v-model="temp.args" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
    </a-form-model-item>

    <a-form-model-item prop="autoStart" v-show="noFileModes.includes(temp.runMode)">
      <template slot="label">
        <a-tooltip>
          自启动
          <template slot="title">插件端启动的时候检查项目状态，如果项目状态是未运行则尝试执行启动项目</template>
          <a-icon v-show="temp.type !== 'edit'" type="question-circle" theme="filled" />
        </a-tooltip>
      </template>
      <a-switch v-model="temp.autoStart" checked-children="开" un-checked-children="关" />
    </a-form-model-item>
    <a-form-model-item v-if="temp.runMode === 'Dsl'" prop="dslEnv" label="DSL环境变量">
      <a-input v-model="temp.dslEnv" placeholder="DSL环境变量,如：key1=values1&keyvalue2" />
    </a-form-model-item>
    <a-form-model-item prop="token" v-show="noFileModes.includes(temp.runMode)" class="jpom-node-project-token">
      <template slot="label">
        <a-tooltip>
          WebHooks
          <template slot="title">
            <ul>
              <li>项目启动,停止,重启,文件变动都将请求对应的地址</li>
              <li>传入参数有：projectId、projectName、type、result</li>
              <li>type 的值有：stop、beforeStop、start、beforeRestart、fileChange</li>
            </ul>
          </template>
          <a-icon v-show="temp.type !== 'edit'" type="question-circle" theme="filled" />
        </a-tooltip>
      </template>
      <a-input v-model="temp.token" placeholder="项目启动,停止,重启,文件变动都将请求对应的地址,非必填，GET请求" />
    </a-form-model-item>
    <a-form-model-item v-if="temp.log" v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)" label="日志路径" prop="log">
      <a-alert :message="temp.log" type="success" />
    </a-form-model-item>
    <a-form-model-item v-if="temp.runCommand" v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)" label="运行命令" prop="runCommand">
      <a-alert :message="temp.runCommand || '无'" type="success" />
    </a-form-model-item>
  </a-form-model>
</template>
<script>
import CustomSelect from "@/components/customSelect";

import codeEditor from "@/components/codeEditor";
import { PROJECT_DSL_DEFATUL, randomStr } from "@/utils/const";

import {
  editProject,
  getProjectAccessList,
  getProjectData,
  javaModes,
  // nodeJudgeLibExist,
  noFileModes,
  runModeList,
  getProjectGroupAll,
} from "@/api/node-project";

export default {
  props: {
    projectId: {
      type: String,
      default: "",
    },
    nodeId: {
      type: String,
      default: "",
    },
    data: { type: Object, default: null },
  },
  components: {
    CustomSelect,
    // Replica,
    codeEditor,
  },
  data() {
    return {
      accessList: [],
      groupList: [],
      runModeList: runModeList,
      javaModes: javaModes,
      noFileModes: noFileModes,
      PROJECT_DSL_DEFATUL,

      temp: {},

      rules: {
        id: [{ required: true, message: "请输入项目ID", trigger: "blur" }],
        name: [{ required: true, message: "请输入项目名称", trigger: "blur" }],
        runMode: [{ required: true, message: "请选择项目运行方式", trigger: "blur" }],
        whitelistDirectory: [{ required: true, message: "请选择项目授权路径", trigger: "blur" }],
        lib: [{ required: true, message: "请输入项目文件夹", trigger: "blur" }],
      },
    };
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
  },
  watch: {},
  mounted() {
    this.loadAccesList();
    this.loadGroupList();
    this.$refs["editProjectForm"]?.resetFields();

    if (this.projectId) {
      // 修改
      const params = {
        id: this.projectId,
        nodeId: this.nodeId,
      };

      getProjectData(params).then((res) => {
        if (res.code === 200 && res.data) {
          this.temp = {
            ...res.data,
            type: "edit",
          };
        }
        if (this.data) {
          // 复制项目
          this.temp = { ...this.temp, ...this.data, type: "add" };
        }
      });
    } else {
      // 添加
      this.temp = {
        type: "add",
        logPath: "",
      };

      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      });
    }
  },
  methods: {
    randomStr,

    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "project",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-project-whitelist"),
              intro: "这里是选择节点设置的授权目录，授权的设置在侧边栏菜单<b>插件端配置</b>里面。",
            },
          ],
        },
      });
    },
    // 加载项目授权列表
    loadAccesList() {
      getProjectAccessList(this.nodeId).then((res) => {
        if (res.code === 200) {
          this.accessList = res.data;
        }
      });
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },

    // 提交
    handleOk() {
      if (this.temp.outGivingProject) {
        this.$notification.warning({
          message: "独立的项目分发请到分发管理中去修改",
        });
        return;
      }
      // 检验表单
      this.$refs["editProjectForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const params = {
          ...this.temp,
          nodeId: this.nodeId,
        };
        // 删除旧数据
        delete params.javaCopyItemList;
        editProject(params).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.$emit("close");
          }
        });
      });
    },

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
    //         this.$notification.warning({
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
};
</script>
<style scoped>
.replica-area {
  width: 80%;
}

/* .replica-btn-del {
  position: absolute;
  right: 120px;
  top: 74px;
} */

/* .lib-exist {
  color: #faad14;
} */
</style>
