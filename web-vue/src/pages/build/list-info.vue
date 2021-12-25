/** * 这是新版本的构建列表页面，主要是分离了部分数据到【仓库管理】，以及数据会存储到数据库 */
<template>
  <div class="full-content">
    <div ref="filter" class="filter">
      <a-input allowClear class="search-input-item" v-model="listQuery['%name%']" placeholder="构建名称" />
      <a-select show-search option-filter-prop="children" v-model="listQuery.status" allowClear placeholder="状态" class="filter-item">
        <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
      </a-select>
      <a-select show-search option-filter-prop="children" v-model="listQuery.releaseMethod" allowClear placeholder="发布方式" class="filter-item">
        <a-select-option v-for="(val, key) in releaseMethodMap" :key="key">{{ val }}</a-select-option>
      </a-select>
      <a-input allowClear class="search-input-item" v-model="listQuery['%resultDirFile%']" placeholder="产物目录" />
      <a-tooltip title="按住 Ctr 或者 Alt 键点击按钮快速回到第一页">
        <a-button type="primary" @click="loadData">搜索</a-button>
      </a-tooltip>
      <a-button type="primary" @click="handleAdd">新增</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" bordered rowKey="id" :pagination="pagination" @change="changePage">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="branchName" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="resultDirFile" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="script" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="releaseMethod" slot-scope="text" placement="topleft" :title="text">
        <span>{{ releaseMethodMap[text] }}</span>
      </template>
      <template slot="status" slot-scope="text">
        <span>{{ statusMap[text] || "未知" }}</span>
      </template>
      <a-tooltip slot="buildId" slot-scope="text, record" placement="topLeft" :title="text + ' ( 点击查看日志 ) '">
        <span v-if="record.buildId <= 0"></span>
        <a-tag v-else color="#108ee9" @click="handleBuildLog(record)">#{{ text }}</a-tag>
      </a-tooltip>
      <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" v-if="record.status === 1 || record.status === 4" @click="handleStopBuild(record)">停止 </a-button>
        <a-button type="primary" v-else @click="handleStartBuild(record)">构建</a-button>
        <a-dropdown>
          <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
            更多
            <a-icon type="down" />
          </a>
          <a-menu slot="overlay">
            <a-menu-item>
              <a-button type="primary" @click="handleTrigger(record)">触发器</a-button>
            </a-menu-item>
            <a-menu-item>
              <a-button type="danger" @click="handleDelete(record)">删除</a-button>
            </a-menu-item>
            <a-menu-item>
              <a-tooltip
                title="清除代码(仓库目录)为删除服务器中存储仓库目录里面的所有东西,删除后下次构建将重新拉起仓库里面的文件,一般用于解决服务器中文件和远程仓库中文件有冲突时候使用。执行时间取决于源码目录大小和文件数量如超时请耐心等待，或稍后重试"
              >
                <a-button type="danger" :disabled="!record.sourceDirExist" @click="handleClear(record)">清除代码 </a-button>
              </a-tooltip>
            </a-menu-item>
          </a-menu>
        </a-dropdown>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editBuildVisible" title="编辑构建" @ok="handleEditBuildOk" width="50%" :maskClosable="false">
      <a-form-model ref="editBuildForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="temp.name" placeholder="名称" />
        </a-form-model-item>

        <a-form-model-item label="仓库地址" prop="repositoryId">
          <a-select show-search option-filter-prop="children" v-model="temp.repositoryId" @select="changeRepositpry" @change="changeRepositpry" placeholder="请选择仓库">
            <a-select-option v-for="item in repositoryList" :key="item.id" :value="item.id">{{ item.name }}[{{ item.gitUrl }}]</a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item v-show="tempRepository.repoType === 0" label="分支" prop="branchName">
          <a-row>
            <a-col :span="10">
              <custom-select v-model="temp.branchName" :data="branchList" @onRefreshSelect="loadBranchList" inputPlaceholder="自定义分支通配表达式" selectPlaceholder="请选择构建对应的分支,必选">
                <div slot="inputTips">
                  支持通配符(AntPathMatcher)
                  <ul>
                    <li>? 匹配一个字符</li>
                    <li>* 匹配零个或多个字符</li>
                    <li>** 匹配路径中的零个或多个目录</li>
                  </ul>
                </div>
              </custom-select>
            </a-col>
            <a-col :span="4" style="text-align: right"> 标签(TAG)：</a-col>
            <a-col :span="10">
              <custom-select
                v-model="temp.branchTagName"
                :data="branchTagList"
                @onRefreshSelect="loadBranchList"
                inputPlaceholder="自定义标签通配表达式"
                selectPlaceholder="请选择构建对应标签,可以不选择"
              >
                <div slot="inputTips">
                  支持通配符(AntPathMatcher)
                  <ul>
                    <li>? 匹配一个字符</li>
                    <li>* 匹配零个或多个字符</li>
                    <li>** 匹配路径中的零个或多个目录</li>
                  </ul>
                </div>
              </custom-select>
            </a-col>
          </a-row>
        </a-form-model-item>
        <a-form-model-item label="构建命令" prop="script">
          <a-input v-model="temp.script" type="textarea" :auto-size="{ minRows: 2, maxRows: 6 }" allow-clear placeholder="构建执行的命令(非阻塞命令)，如：mvn clean package、npm run build" />
        </a-form-model-item>
        <a-form-model-item prop="resultDirFile" class="jpom-target-dir">
          <template slot="label">
            产物目录
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                <div>可以理解为项目打包的目录。 如 Jpom 项目执行（构建命令） <b>mvn clean package</b> 构建命令，构建产物相对路径为：<b>modules/server/target/server-2.4.2-release</b></div>
                <div><br /></div>
                <div>
                  支持通配符(AntPathMatcher)【目前只使用匹配到的第一项】
                  <ul>
                    <li>? 匹配一个字符</li>
                    <li>* 匹配零个或多个字符</li>
                    <li>** 匹配路径中的零个或多个目录</li>
                  </ul>
                </div>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.resultDirFile" placeholder="构建产物目录,相对仓库的路径,如 java 项目的 target/xxx.jar vue 项目的 dist" />
        </a-form-model-item>
        <a-form-model-item prop="releaseMethod">
          <template slot="label">
            发布操作
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                <ul>
                  <li>发布操作是指,执行完构建命令后将构建产物目录中的文件用不同的方式发布(上传)到对应的地方</li>
                  <li>节点分发是指,一个项目部署在多个节点中使用节点分发一步完成多个节点中的项目发布操作</li>
                  <li>项目是指,节点中的某一个项目,需要提前在节点中创建项目</li>
                  <li>SSH 是指,通过 SSH 命令的方式对产物进行发布或者执行多条命令来实现发布(需要到 SSH 中提前去添加)</li>
                  <li>本地命令是指,在服务端本地执行多条命令来实现发布</li>
                  <li>SSH、本地命令发布都执行变量替换,系统预留变量有：#{BUILD_ID}、#{BUILD_NAME}、#{BUILD_RESULT_FILE}、#{BUILD_NUMBER_ID}</li>
                  <li>可以引用工作空间的环境变量 变量占位符 #{xxxx} xxxx 为变量名称</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-radio-group v-model="temp.releaseMethod" name="releaseMethod">
            <a-radio v-for="tempValue in releaseMethodArray" :key="tempValue.value" :value="tempValue.value">{{ tempValue.name }}</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <!-- 节点分发 -->
        <a-form-model-item v-if="temp.releaseMethod === 1" label="分发项目" prop="releaseMethodDataId">
          <a-select v-model="tempExtraData.releaseMethodDataId_1" placeholder="请选择分发项目">
            <a-select-option v-for="dispatch in dispatchList" :key="dispatch.id">{{ dispatch.name }} </a-select-option>
          </a-select>
        </a-form-model-item>
        <!-- 项目 -->
        <a-form-model-item v-if="temp.releaseMethod === 2" label="发布项目" prop="releaseMethodDataIdList">
          <a-cascader v-model="temp.releaseMethodDataIdList" :options="cascaderList" placeholder="请选择节点项目" />
        </a-form-model-item>
        <a-form-model-item v-if="temp.releaseMethod === 2" label="发布后操作" prop="afterOpt">
          <a-select v-model="tempExtraData.afterOpt" placeholder="请选择发布后操作">
            <a-select-option v-for="opt in afterOptList" :key="opt.value">{{ opt.title }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <!-- SSH -->
        <a-form-model-item v-if="temp.releaseMethod === 3" label="SSH/目录：" prop="releaseMethodDataId">
          <a-input-group compact>
            <a-select style="width: 30%" v-model="tempExtraData.releaseMethodDataId_3" placeholder="请选择SSH">
              <a-select-option v-for="ssh in sshList" :key="ssh.id">{{ ssh.name }}</a-select-option>
            </a-select>
            <a-input style="width: 70%" v-model="tempExtraData.releasePath" placeholder="发布目录,构建产物上传到对应目录" />
          </a-input-group>
        </a-form-model-item>
        <a-form-model-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" prop="releaseCommand">
          <!-- sshCommand LocalCommand -->
          <template slot="label">
            发布命令
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                发布执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java
                <ul>
                  <li>支持变量替换：#{BUILD_ID}、#{BUILD_NAME}、#{BUILD_RESULT_FILE}、#{BUILD_NUMBER_ID}</li>
                  <li>可以引用工作空间的环境变量 变量占位符 #{xxxx} xxxx 为变量名称</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input
            v-model="tempExtraData.releaseCommand"
            allow-clear
            :auto-size="{ minRows: 2, maxRows: 10 }"
            type="textarea"
            :rows="3"
            placeholder="发布执行的命令(非阻塞命令),一般是启动项目命令 如：ps -aux | grep java,支持变量替换：#{BUILD_ID}、#{BUILD_NAME}、#{BUILD_RESULT_FILE}、#{BUILD_NUMBER_ID}"
          />
        </a-form-model-item>

        <a-form-model-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" prop="clearOld">
          <template slot="label">
            清空发布
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件现删除后再保存新文件 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-switch v-model="tempExtraData.clearOld" checked-children="是" un-checked-children="否" />
            </a-col>
            <div v-if="temp.releaseMethod === 2">
              <a-col :span="4" style="text-align: right">
                <a-tooltip v-if="!temp.id">
                  <template slot="title">
                    差异发布是指对应构建产物和项目文件夹里面的文件是否存在差异,如果存在增量差异那么上传或者覆盖文件。
                    <ul>
                      <li>开启差异发布并且开启清空发布时将自动删除项目目录下面有的文件但是构建产物目录下面没有的文件 【清空发布差异上传前会先执行删除差异文件再执行上传差异文件】</li>
                      <li>开启差异发布但不开启清空发布时相当于只做增量和变动更新</li>
                    </ul>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
                差异发布：
              </a-col>
              <a-col :span="10">
                <a-switch v-model="tempExtraData.diffSync" checked-children="是" un-checked-children="否" />
              </a-col>
            </div>
          </a-row>
        </a-form-model-item>
        <a-form-model-item prop="webhook">
          <template slot="label">
            WebHooks
            <a-tooltip v-show="!temp.id">
              <template slot="title">
                <ul>
                  <li>构建过程请求对应的地址,开始构建,构建完成,开始发布,发布完成,构建异常,发布异常</li>
                  <li>传人参数有：buildId、buildName、type、error、triggerTime</li>
                  <li>type 的值有：startReady、pull、executeCommand、release、done、stop、success</li>
                  <li>异步请求不能保证有序性</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.webhook" placeholder="构建过程请求,非必填，GET请求" />
        </a-form-model-item>
        <a-form-model-item label="自动构建" prop="autoBuildCron">
          <a-auto-complete placeholder="如果需要定时自动构建则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）" option-label-prop="value">
            <template slot="dataSource">
              <a-select-opt-group v-for="group in cronDataSource" :key="group.title">
                <span slot="label">
                  {{ group.title }}
                </span>
                <a-select-option v-for="opt in group.children" :key="opt.title" :value="opt.value"> {{ opt.title }} {{ opt.value }} </a-select-option>
              </a-select-opt-group>
            </template>
            <a-input v-model="temp.autoBuildCron" placeholder="如果需要定时自动构建则填写,cron 表达式.默认未开启秒级别,需要去修改配置文件中:[system.timerMatchSecond]）" />
          </a-auto-complete>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 触发器 -->
    <a-modal v-model="triggerVisible" title="触发器" :footer="null" :maskClosable="false">
      <a-form-model ref="editTriggerForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-model-item label="触发器地址" prop="triggerBuildUrl">
          <a-input v-model="temp.triggerBuildUrl" type="textarea" readOnly :rows="3" style="resize: none" placeholder="触发器地址" />
        </a-form-model-item>
        <a-row>
          <a-col :span="6"></a-col>
          <a-col :span="16">
            <a-button type="primary" class="btn-add" @click="resetTrigger">重置</a-button>
          </a-col>
        </a-row>
        可以添加 delay=x 参数来延迟执行构建
      </a-form-model>
    </a-modal>
    <!-- 构建日志 -->
    <a-modal width="80vw" v-model="buildLogVisible" title="构建日志" :footer="null" :maskClosable="false" @cancel="closeBuildLogModel">
      <build-log v-if="buildLogVisible" :temp="temp" />
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import CustomSelect from "@/components/customSelect";
import BuildLog from "./log";
import { getRepositoryListAll } from "@/api/repository";
import { clearBuid, deleteBuild, editBuild, getBranchList, getBuildList, getTriggerUrl, releaseMethodMap, releaseMethodArray, resetTrigger, startBuild, stopBuild, statusMap } from "@/api/build-info";
import { getDishPatchListAll, afterOptList } from "@/api/dispatch";
import { getProjectListAll, getNodeListAll } from "@/api/node";
import { getSshListAll } from "@/api/ssh";
import { itemGroupBy, parseTime } from "@/utils/time";
import { PAGE_DEFAULT_LIMIT, PAGE_DEFAULT_SIZW_OPTIONS, PAGE_DEFAULT_SHOW_TOTAL, PAGE_DEFAULT_LIST_QUERY, CRON_DATA_SOURCE } from "@/utils/const";

export default {
  components: {
    BuildLog,
    CustomSelect,
  },
  data() {
    return {
      releaseMethodMap: releaseMethodMap,
      releaseMethodArray: releaseMethodArray,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      cronDataSource: CRON_DATA_SOURCE,
      // 动态列表参数
      groupList: [],
      list: [],
      statusMap: statusMap,
      repositoryList: [],
      // 当前仓库信息
      tempRepository: {},
      // 当前构建信息的 extraData 属性
      tempExtraData: {},
      branchList: [],
      branchTagList: [],
      dispatchList: [],
      cascaderList: [],
      sshList: [],
      temp: {},
      // 页面控制变量
      editBuildVisible: false,
      triggerVisible: false,
      buildLogVisible: false,
      afterOptList: afterOptList,
      columns: [
        { title: "名称", dataIndex: "name", width: 100, sorter: true, ellipsis: true, scopedSlots: { customRender: "name" } },
        // { title: "分组", dataIndex: "group", key: "group%", sorter: true, width: 100, ellipsis: true, scopedSlots: { customRender: "group" } },
        {
          title: "分支",
          dataIndex: "branchName",
          width: 100,
          ellipsis: true,
          scopedSlots: { customRender: "branchName" },
        },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        {
          title: "构建 ID",
          dataIndex: "buildId",
          width: 80,
          ellipsis: true,
          scopedSlots: { customRender: "buildId" },
        },
        {
          title: "修改人",
          dataIndex: "modifyUser",
          width: 150,
          ellipsis: true,
          sorter: true,
          scopedSlots: { customRender: "modifyUser" },
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "发布方式",
          dataIndex: "releaseMethod",
          width: 100,
          ellipsis: true,
          scopedSlots: { customRender: "releaseMethod" },
        },
        {
          title: "产物目录",
          dataIndex: "resultDirFile",
          ellipsis: true,
          width: 100,
          scopedSlots: { customRender: "resultDirFile" },
        },
        { title: "构建命令", width: 100, dataIndex: "script", ellipsis: true, scopedSlots: { customRender: "script" } },
        {
          title: "操作",
          dataIndex: "operation",
          width: 240,
          scopedSlots: { customRender: "operation" },
          align: "left",
          fixed: "right",
        },
      ],
      rules: {
        name: [{ required: true, message: "Please input build name", trigger: "blur" }],
        script: [{ required: true, message: "Please input build script", trigger: "blur" }],
        resultDirFile: [{ required: true, message: "Please input build target path", trigger: "blur" }],
        releasePath: [{ required: true, message: "Please input release path", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return {
        total: this.listQuery.total,
        current: this.listQuery.page || 1,
        pageSize: this.listQuery.limit || PAGE_DEFAULT_LIMIT,
        pageSizeOptions: PAGE_DEFAULT_SIZW_OPTIONS,
        showSizeChanger: true,
        showTotal: (total) => {
          return PAGE_DEFAULT_SHOW_TOTAL(total, this.listQuery);
        },
      };
    },
    ...mapGetters(["getGuideFlag"]),
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    // 页面引导
    introGuide() {
      if (this.getGuideFlag) {
        this.$introJs()
          .setOptions({
            hidePrev: true,
            steps: [
              {
                title: "Jpom 导航助手",
                element: document.querySelector(".jpom-target-dir"),
                intro: "可以理解为项目打包的目录。如 Jpom 项目执行 <b>mvn clean package</b> 构建命令，构建产物相对路径为：<b>modules/server/target/server-2.4.2-release</b>",
              },
            ],
          })
          .start();
        return false;
      }
      this.$introJs().exit();
    },

    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      getBuildList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载仓库列表
    loadRepositoryList(fn) {
      getRepositoryListAll().then((res) => {
        if (res.code === 200) {
          this.repositoryList = res.data;
          fn && fn();
        }
      });
    },
    // 加载节点分发列表
    loadDispatchList() {
      this.dispatchList = [];
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data;
        }
      });
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.cascaderList = [];
      getNodeListAll().then((res0) => {
        if (res0.code !== 200) {
          return;
        }
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            let temp = itemGroupBy(res.data, "nodeId", "value", "children");

            this.cascaderList = temp.map((item) => {
              let findArra = res0.data.filter((res0Item) => {
                return res0Item.id === item.value;
              });
              item.label = findArra.length ? findArra[0].name : "未知";
              item.children = item.children.map((item2) => {
                return {
                  label: item2.name,
                  value: item2.projectId,
                };
              });
              return item;
            });
          }
        });
      });
    },
    // 加载 SSH 列表
    loadSshList() {
      this.sshList = [];
      getSshListAll().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data;
        }
      });
    },
    // 筛选
    handleFilter() {
      this.loadData();
      // this.loadRepositoryList();
    },
    // 选择仓库
    changeRepositpry(value) {
      this.repositoryList.forEach((element) => {
        if (element.id === value) {
          this.tempRepository = element;
          this.temp.branchName = "";
          this.temp.branchTagName = "";
          // 刷新分支
          this.loadBranchList();
        }
      });
    },
    // 添加
    handleAdd() {
      this.temp = {};
      this.branchList = [];
      this.loadRepositoryList();
      this.loadDispatchList();
      this.loadNodeProjectList();
      this.loadSshList();
      this.editBuildVisible = true;
      this.tempExtraData = {};
      this.$nextTick(() => {
        setTimeout(() => {
          this.introGuide();
        }, 500);
      });
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign(record);
      this.temp.tempGroup = "";
      // 设置当前临时的 额外构建信息
      this.tempExtraData = JSON.parse(record.extraData) || {};
      if (typeof this.tempExtraData === "string") {
        this.tempExtraData = JSON.parse(this.tempExtraData);
      }
      // 设置发布方式的数据
      if (this.tempExtraData.releaseMethodDataId) {
        if (record.releaseMethod === 1) {
          this.tempExtraData.releaseMethodDataId_1 = this.tempExtraData.releaseMethodDataId;
        }
        if (record.releaseMethod === 2) {
          this.temp = {
            ...this.temp,
            releaseMethodDataIdList: this.tempExtraData.releaseMethodDataId.split(":"),
          };
        }
        if (record.releaseMethod === 3) {
          this.tempExtraData.releaseMethodDataId_3 = this.tempExtraData.releaseMethodDataId;
        }
      }
      this.loadRepositoryList(() => {
        // 从仓库列表里匹配对应的仓库信息
        // console.log(this.repositoryList);
        this.tempRepository = this.repositoryList.filter((element) => this.temp.repositoryId === element.id)[0];
        this.editBuildVisible = true;
        this.loadBranchList();
      });

      this.loadDispatchList();

      this.loadNodeProjectList();
      this.loadSshList();
    },
    // 获取仓库分支
    loadBranchList() {
      if (this.tempRepository.repoType !== 0) {
        return;
      }
      const loading = this.$loading.service({
        lock: true,
        text: "正在加载项目分支",
        spinner: "el-icon-loading",
        background: "rgba(0, 0, 0, 0.7)",
      });
      this.branchList = [];
      const params = {
        repositoryId: this.tempRepository?.id,
      };
      getBranchList(params).then((res) => {
        if (res.code === 200) {
          this.branchList = res.data[0];
          this.branchTagList = res.data[1];
        }
        loading.close();
      });
    },
    // 提交节点数据
    handleEditBuildOk() {
      // 检验表单
      this.$refs["editBuildForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 设置参数
        if (this.temp.releaseMethod === 2) {
          if (this.temp.releaseMethodDataIdList.length < 2) {
            this.$notification.warn({
              message: "请选择节点项目,可能是节点中不存在任何项目,需要去节点中创建项目",
            });
            return false;
          }
          this.tempExtraData.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0];
          this.tempExtraData.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1];
        }

        this.temp = {
          ...this.temp,
          extraData: JSON.stringify(this.tempExtraData),
        };
        // 提交数据
        editBuild(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editBuildForm"].resetFields();
            this.editBuildVisible = false;
            this.handleFilter();
            // this.loadGroupList();
          }
        });
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除构建信息么？删除也将同步删除所有的构建历史记录信息",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteBuild(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 触发器
    handleTrigger(record) {
      this.temp = Object.assign(record);
      getTriggerUrl(record.id).then((res) => {
        if (res.code === 200) {
          this.temp.triggerBuildUrl = `${location.protocol}//${location.host}${res.data.triggerBuildUrl}`;
          this.triggerVisible = true;
        }
      });
    },
    // 重置触发器
    resetTrigger() {
      resetTrigger(this.temp.id).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.triggerVisible = false;
          this.handleTrigger(this.temp);
        }
      });
    },
    // 清除构建
    handleClear(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要清除构建信息么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          clearBuid(record.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
            }
          });
        },
      });
    },
    // 开始构建
    handleStartBuild(record) {
      this.$confirm({
        title: "系统提示",
        content: "确定要开始构建 【名称：" + record.name + "】 【分支：" + record.branchName + "】 吗？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          this.temp = Object.assign(record);
          startBuild(this.temp.id).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.handleFilter();
              // 自动打开构建日志
              this.handleBuildLog({
                id: this.temp.id,
                buildId: res.data,
              });
            }
          });
        },
      });
    },
    // 停止构建
    handleStopBuild(record) {
      this.temp = Object.assign(record);
      stopBuild(this.temp.id).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.handleFilter();
        }
      });
    },
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.id,
        buildId: record.buildId,
      };
      this.buildLogVisible = true;
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.handleFilter();
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery.page = pagination.current;
      this.listQuery.limit = pagination.pageSize;
      if (sorter) {
        this.listQuery.order = sorter.order;
        this.listQuery.order_field = sorter.field;
      }
      this.loadData();
    },
  },
};
</script>
<style scoped>
.filter {
  margin-bottom: 10px;
}

.ant-btn {
  margin-right: 10px;
}

.filter-item {
  width: 150px;
  margin-right: 10px;
}

.btn-add {
  margin-left: 10px;
  margin-right: 0;
}
</style>
