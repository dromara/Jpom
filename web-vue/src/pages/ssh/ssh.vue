<template>
  <div class="full-content">
    <!-- <div ref="filter" class="filter">

    </div> -->
    <!-- 数据表格 -->
    <a-table :data-source="list" :columns="columns" size="middle" :pagination="pagination" @change="changePage" bordered rowKey="id" :row-selection="rowSelection">
      <template slot="title">
        <a-space>
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%name%']" placeholder="节点名称" />
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%host%']" placeholder="节点地址" />
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%user%']" placeholder="用户名" />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()"> 更多 <a-icon type="down" /> </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" :disabled="!tableSelections || !tableSelections.length" @click="syncToWorkspaceShow">工作空间同步</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          <a-tooltip>
            <template slot="title">
              <div>
                <ul>
                  <li>关联节点数据是异步获取有一定时间延迟</li>
                  <li>关联节点会自动识别服务器中是否存在 java 环境,如果没有 Java 环境不能快速安装节点</li>
                  <li>关联节点如果服务器存在 java 环境,但是插件端未运行则会显示快速安装按钮</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="tooltip" slot-scope="text" :title="text"> {{ text }}</a-tooltip>
      <template slot="nodeId" slot-scope="text, record">
        <!-- <a-button v-if="!record.nodeModel" type="primary" @click="install(record)" :disabled="record.installed">安装节点</a-button> -->
        <div v-if="sshAgentInfo[record.id]">
          <div v-if="sshAgentInfo[record.id].javaVersion">
            <a-tooltip v-if="sshAgentInfo[record.id].nodeId" placement="topLeft" :title="`节点名称：${sshAgentInfo[record.id].nodeName}`">
              <a-button size="small" style="width: 90px; padding: 0 10px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis" type="" @click="toNode(sshAgentInfo[record.id].nodeId)">
                {{ sshAgentInfo[record.id].nodeName }}
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="sshAgentInfo[record.id].error" placement="topLeft" :title="`${sshAgentInfo[record.id].error}`">
              {{ sshAgentInfo[record.id].error }}
            </a-tooltip>
            <a-tooltip
              v-if="sshAgentInfo[record.id].ok"
              placement="topLeft"
              :title="`${sshAgentInfo[record.id].pid > 0 ? 'ssh 中已经运行了插件端进程ID：' + sshAgentInfo[record.id].pid : '点击快速安装插件端,java :' + sshAgentInfo[record.id].javaVersion}`"
            >
              <a-button size="small" type="primary" @click="install(record)" :disabled="sshAgentInfo[record.id].pid > 0">{{ sshAgentInfo[record.id].pid > 0 ? "运行中" : "安装节点" }}</a-button>
            </a-tooltip>
          </div>
          <div v-else>
            <a-tooltip v-if="sshAgentInfo[record.id].error" :title="sshAgentInfo[record.id].error">
              <a-tag>连接异常</a-tag>
            </a-tooltip>
            <a-tag v-else>没有Java环境</a-tag>
          </div>
        </div>
        <div v-else>-</div>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-dropdown>
            <a-button size="small" type="primary" @click="handleTerminal(record, false)">终端<a-icon type="down" /></a-button>
            <a-menu slot="overlay">
              <a-menu-item key="1">
                <a-button size="small" type="primary" icon="fullscreen" @click="handleTerminal(record, true)">全屏终端</a-button>
              </a-menu-item>
              <a-menu-item key="2">
                <router-link target="_blank" :to="{ path: '/full-terminal', query: { id: record.id, wid: getWorkspaceId } }">
                  <a-button size="small" type="primary" icon="fullscreen"> 新标签终端</a-button>
                </router-link>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

          <a-tooltip placement="topLeft" title="如果按钮不可用,请去 ssh 编辑中添加允许管理的授权文件夹">
            <a-button size="small" type="primary" :disabled="!record.fileDirs" @click="handleFile(record)">文件</a-button>
          </a-tooltip>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button size="small" type="primary" @click="handleViewLog(record)">终端日志</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal v-model="editSshVisible" width="600px" title="编辑 SSH" @ok="handleEditSshOk" :maskClosable="false">
      <a-form-model ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="SSH 名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="SSH 名称" />
        </a-form-model-item>
        <a-form-model-item label="Host" prop="host">
          <a-input-group compact prop="host">
            <a-input style="width: 70%" v-model="temp.host" placeholder="主机 Host" />
            <a-input-number style="width: 30%" v-model="temp.port" :min="1" placeholder="端口号" />
          </a-input-group>
        </a-form-model-item>
        <a-form-model-item label="认证方式" prop="connectType">
          <a-radio-group v-model="temp.connectType" :options="options" />
        </a-form-model-item>
        <a-form-model-item label="用户名" prop="user">
          <a-input v-model="temp.user" placeholder="用户" />
        </a-form-model-item>
        <!-- 新增时需要填写 -->
        <!--				<a-form-model-item v-if="temp.type === 'add'" label="Password" prop="password">-->
        <!--					<a-input-password v-model="temp.password" placeholder="密码"/>-->
        <!--				</a-form-model-item>-->
        <!-- 修改时可以不填写 -->
        <a-form-model-item label="Password" :prop="`${temp.type === 'add' && temp.connectType === 'PASS' ? 'password' : 'password-update'}`">
          <a-input-password v-model="temp.password" :placeholder="`${temp.type === 'add' ? '密码' : '密码若没修改可以不用填写'}`" />
        </a-form-model-item>
        <a-form-model-item v-if="temp.connectType === 'PUBKEY'" prop="privateKey">
          <template slot="label">
            私钥内容
            <a-tooltip v-if="temp.type !== 'edit'" placement="topLeft">
              <template slot="title">不填将使用默认的 $HOME/.ssh 目录中的配置,使用优先级是：id_dsa>id_rsa>identity </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>

          <a-textarea v-model="temp.privateKey" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="私钥内容,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:/xxxx/xx" />
        </a-form-model-item>

        <a-form-model-item prop="fileDirs">
          <template slot="label">
            文件目录
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title"> 绑定指定目录可以在线管理，同时构建 ssh 发布目录也需要在此配置 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-textarea v-model="temp.fileDirs" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="授权可以直接访问的目录，多个回车换行即可" />
        </a-form-model-item>

        <a-form-model-item label="文件后缀" prop="suffix">
          <a-input
            v-model="temp.allowEditSuffix"
            type="textarea"
            :rows="5"
            style="resize: none"
            placeholder="请输入允许编辑文件的后缀及文件编码，不设置编码则默认取系统编码，多个使用换行。示例：设置编码：txt@utf-8， 不设置编码：txt"
          />
        </a-form-model-item>
        <a-collapse>
          <a-collapse-panel key="1" header="其他配置">
            <a-form-model-item label="编码格式" prop="charset">
              <a-input v-model="temp.charset" placeholder="编码格式" />
            </a-form-model-item>
            <a-form-model-item label="超时时间(s)" prop="timeout">
              <a-input-number v-model="temp.timeout" :min="1" placeholder="单位秒,最小值 1 秒" style="width: 100%" />
            </a-form-model-item>
            <a-form-model-item prop="notAllowedCommand">
              <template slot="label">
                禁止命令
                <a-tooltip v-show="temp.type !== 'edit'">
                  <template slot="title">
                    限制禁止在在线终端执行的命令
                    <ul>
                      <li>超级管理员没有任何限制</li>
                      <li>其他用户可以配置权限解除限制</li>
                    </ul>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-textarea v-model="temp.notAllowedCommand" :auto-size="{ minRows: 3, maxRows: 5 }" placeholder="禁止命令是不允许在终端执行的名，多个逗号隔开。(超级管理员没有任何限制)" />
            </a-form-model-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
    <!-- 安装节点 -->
    <a-modal v-model="nodeVisible" width="600px" title="安装节点" @ok="handleEditNodeOk" :maskClosable="false">
      <a-collapse :defaultActiveKey="['1', '2']">
        <a-collapse-panel key="1" header="插件端安装包管理">
          <a-form-model :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
            <a-form-model-item label="安装文件" style="margin-bottom: 0px">
              <a-row>
                <a-col :span="8">
                  <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload" accept=".zip">
                    <a-button>
                      <a-icon type="upload" />
                      选择文件
                    </a-button>
                  </a-upload>
                </a-col>
                <a-col :span="6">
                  <a-button type="primary" :disabled="!fileList || !fileList.length" @click="handleUploadAgent">上传</a-button>
                </a-col>
                <a-col :span="8">
                  <div v-if="agentData">
                    <a-tooltip :title="`打包时间：${agentData.timeStamp} 文件路径：${agentData.path}`">
                      当前版本：<a-tag>{{ agentData.version }}</a-tag>
                    </a-tooltip>
                  </div>
                  <div v-else><a-tag color="orange">需要上传</a-tag></div>
                </a-col>
              </a-row>
            </a-form-model-item>
          </a-form-model>
        </a-collapse-panel>
        <a-collapse-panel key="2" header="快速安装">
          <a-form-model ref="nodeForm" :rules="rules" :model="tempNode" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
            <!-- <a-form-model-item label="节点 ID" prop="id">
            <a-input v-model="tempNode.id" placeholder="节点 ID" />
          </a-form-model-item> -->
            <a-form-model-item label="节点名称" prop="name">
              <a-input v-model="tempNode.name" placeholder="节点名称" />
            </a-form-model-item>
            <a-form-model-item label="节点协议" prop="protocol">
              <a-select
                :getPopupContainer="
                  (triggerNode) => {
                    return triggerNode.parentNode || document.body;
                  }
                "
                v-model="tempNode.protocol"
                defaultValue="http"
                placeholder="节点协议"
              >
                <a-select-option key="http">HTTP</a-select-option>
                <a-select-option key="https">HTTPS</a-select-option>
              </a-select>
            </a-form-model-item>
            <a-form-model-item label="节点地址" prop="url">
              <a-input v-model="tempNode.url" placeholder="节点地址 (127.0.0.1:2123)" />
            </a-form-model-item>
            <a-form-model-item label="安装路径" prop="path">
              <a-input v-model="tempNode.path" placeholder="安装路径" />
            </a-form-model-item>
            <a-form-model-item label="脚本权限" prop="chmod">
              <a-input v-model="tempNode.chmod" placeholder="非 root 状态下，需要给脚本文件添加权限的命令(非阻塞的命令)">
                <a-tooltip slot="suffix">
                  <template slot="title">
                    <div>非 root 状态下，需要给脚本文件添加权限的命令</div>
                    <div><br /></div>
                    <div>
                      例如
                      <ul>
                        <li>chmod 755</li>
                        <li>chmod u=rwx</li>
                        <li>chmod u+x</li>
                      </ul>
                    </div>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </a-input>
            </a-form-model-item>
            <a-form-model-item label="等待次数" prop="waitCount">
              <a-input v-model="tempNode.waitCount" placeholder="上传插件端后,等待插件端启动成功次数，1次5秒。默认5次" />
            </a-form-model-item>

            <!-- <template slot="footer">
              <a-button key="back" @click="nodeVisible = false">Cancel</a-button>
              <a-button key="submit" type="primary" :loadingg="formLoading" @click="handleEditNodeOk">Ok</a-button>
            </template> -->
          </a-form-model>
        </a-collapse-panel>
      </a-collapse>
    </a-modal>
    <!-- 文件管理 -->
    <a-drawer :title="drawerTitle" placement="right" width="90vw" :visible="drawerVisible" @close="onClose">
      <ssh-file v-if="drawerVisible" :ssh="temp" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal
      :dialogStyle="{
        maxWidth: '100vw',
        top: this.terminalFullscreen ? 0 : false,
        paddingBottom: 0,
      }"
      :width="this.terminalFullscreen ? '100vw' : '80vw'"
      :bodyStyle="{
        padding: '0px 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `${this.terminalFullscreen ? 'calc(100vh - 56px)' : '70vh'}`,
      }"
      v-model="terminalVisible"
      :title="temp.name"
      :footer="null"
      :maskClosable="false"
      :destroyOnClose="true"
    >
      <terminal v-if="terminalVisible" :sshId="temp.id" />
    </a-modal>
    <!-- 操作日志 -->
    <a-modal v-model="viewOperationLog" title="操作日志" width="80vw" :footer="null" :maskClosable="false">
      <!-- <div ref="filter" class="filter"></div> -->
      <!-- 数据表格 -->
      <a-table
        :data-source="viewOperationLogList"
        :loading="viewOperationLoading"
        :columns="viewOperationLogColumns"
        :pagination="viewOperationLogPagination"
        @change="changeListLog"
        bordered
        size="middle"
        :rowKey="(record, index) => index"
      >
        <template slot="title">
          <a-space>
            <a-input class="search-input-item" @pressEnter="handleListLog" v-model="viewOperationLogListQuery['modifyUser']" placeholder="操作人" />
            <a-input class="search-input-item" @pressEnter="handleListLog" v-model="viewOperationLogListQuery['name']" placeholder="ssh name" />
            <a-input class="search-input-item" @pressEnter="handleListLog" v-model="viewOperationLogListQuery['ip']" placeholder="ip" />
            <a-input class="search-input-item" @pressEnter="handleListLog" v-model="viewOperationLogListQuery['%commands%']" placeholder="执行命令" />
            <a-range-picker class="filter-item search-input-item" :show-time="{ format: 'HH:mm:ss' }" format="YYYY-MM-DD HH:mm:ss" @change="onchangeListLogTime" />
            <a-button type="primary" @click="handleListLog">搜索</a-button>
          </a-space>
        </template>
        <a-tooltip
          slot="commands"
          slot-scope="text"
          placement="topLeft"
          :title="text"
          v-clipboard:copy="text"
          v-clipboard:success="
            () => {
              tempVue.prototype.$notification.success({ message: '复制成功' });
            }
          "
          v-clipboard:error="
            () => {
              tempVue.prototype.$notification.error({ message: '复制失败' });
            }
          "
        >
          <a-input disabled :value="text"><a-icon slot="suffix" type="copy" /></a-input>
        </a-tooltip>
        <a-tooltip slot="modifyUser" slot-scope="text, item" placement="topLeft" :title="item.modifyUser || item.userId">
          <span>{{ item.modifyUser || item.userId }}</span>
        </a-tooltip>

        <a-tooltip slot="userAgent" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="refuse" slot-scope="text">
          <span>{{ text ? "成功" : "拒绝" }}</span>
        </template>
      </a-table>
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal v-model="syncToWorkspaceVisible" title="同步到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用 IP+PORT+连接方式 确定是同一个服务器</li>
            <li>当目标工作空间不存在对应的 SSH 时候将自动创建一个新的 SSH</li>
            <li>当目标工作空间已经存在 SSH 时候将自动同步 SSH 账号、密码、私钥等信息</li>
          </ul>
        </template>
      </a-alert>
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            show-search
            option-filter-prop="children"
            v-model="temp.workspaceId"
            placeholder="请选择工作空间"
          >
            <a-select-option :disabled="getWorkspaceId === item.id" v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </div>
</template>
<script>
import {deleteSsh, editSsh, getAgent, getSshCheckAgent, getSshList, getSshOperationLogList, installAgentNode, syncToWorkspace, uploadAgent} from "@/api/ssh";
import SshFile from "@/pages/ssh/ssh-file";
import Terminal from "@/pages/ssh/terminal";
import {parseTime} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY} from "@/utils/const";
import {getWorkSpaceListAll} from "@/api/workspace";
import Vue from "vue";
import {mapGetters} from "vuex";

export default {
  components: {
    SshFile,
    Terminal,
  },
  data() {
    return {
      loading: false,
      list: [],
      temp: {},
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,
      nodeVisible: false,
      syncToWorkspaceVisible: false,
      tableSelections: [],
      workspaceList: [],
      tempNode: {},
      fileList: [],
      sshAgentInfo: {},
      agentData: {},
      formLoading: false,
      drawerTitle: "",
      drawerVisible: false,
      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,
      viewOperationLoading: false,
      viewOperationLogList: [],
      viewOperationLogColumns: [
        { title: "操作者", dataIndex: "modifyUser", width: 100, scopedSlots: { customRender: "modifyUser" } },
        { title: "IP", dataIndex: "ip" /*width: 130*/ },
        {
          title: "sshName",
          dataIndex: "sshName",
          width: 200,
          ellipsis: true,
          scopedSlots: { customRender: "sshName" },
        },
        {
          title: "执行命令",
          dataIndex: "commands",
          width: 200,
          ellipsis: true,
          scopedSlots: { customRender: "commands" },
        },
        {
          title: "userAgent",
          dataIndex: "userAgent",
          /*width: 240,*/ ellipsis: true,
          scopedSlots: { customRender: "userAgent" },
        },
        {
          title: "是否成功",
          dataIndex: "refuse",
          width: 100,
          ellipsis: true,
          scopedSlots: { customRender: "refuse" },
        },

        {
          title: "操作时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          } /*width: 180*/,
        },
      ],

      viewOperationLogListQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      columns: [
        { title: "名称", dataIndex: "name", sorter: true, ellipsis: true, scopedSlots: { customRender: "tooltip" } },

        { title: "Host", dataIndex: "host", sorter: true, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "Port", dataIndex: "port", sorter: true, width: 80, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "用户名", dataIndex: "user", sorter: true, width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        {
          title: "关联节点",
          dataIndex: "nodeId",
          scopedSlots: { customRender: "nodeId" },
          width: 120,
          ellipsis: true,
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        {
          title: "操作",
          dataIndex: "operation",
          scopedSlots: { customRender: "operation" },
          width: 200,
          align: "center",
          // ellipsis: true,
        },
      ],
      options: [
        { label: "密码", value: "PASS" },
        { label: "证书", value: "PUBKEY" },
      ],
      // 表单校验规则
      rules: {
        id: [{ required: true, message: "Please input id", trigger: "blur" }],
        name: [{ required: true, message: "Please input name", trigger: "blur" }],
        host: [{ required: true, message: "Please input host", trigger: "blur" }],
        port: [{ required: true, message: "Please input port", trigger: "blur" }],
        protocol: [{ required: true, message: "Please input protocol", trigger: "blur" }],
        connectType: [
          {
            required: true,
            message: "Please select connet type",
            trigger: "blur",
          },
        ],
        user: [{ required: true, message: "Please input user", trigger: "blur" }],
        password: [{ required: true, message: "Please input password", trigger: "blur" }],
        // privateKey: [{ required: true, message: "Please input key", trigger: "blur" }],
        url: [{ required: true, message: "Please input url", trigger: "blur" }],
        path: [{ required: true, message: "Please input path", trigger: "blur" }],
      },
      tempVue: null,
    };
  },
  computed: {
    ...mapGetters(["getWorkspaceId"]),
    viewOperationLogPagination() {
      return COMPUTED_PAGINATION(this.viewOperationLogListQuery);
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys;
        },
        selectedRowKeys: this.tableSelections,
      };
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      getSshList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
          //
          this.sshAgentInfo = {};
          let ids = this.list
            .map((item) => {
              return item.id;
            })
            .join(",");
          if (ids.length > 0) {
            getSshCheckAgent({
              ids: ids,
            }).then((res) => {
              this.sshAgentInfo = res.data;
            });
          }
        }
        this.loading = false;
      });
    },
    // 新增 SSH
    handleAdd() {
      this.temp = {
        type: "add",
        charset: "UTF-8",
        port: 22,
        timeout: 5,
        connectType: "PASS",
      };
      this.editSshVisible = true;
      // @author jzy 08-04
      this.$refs["editSshForm"] && this.$refs["editSshForm"].resetFields();
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      // this.temp.;
      this.temp.allowEditSuffix = record.allowEditSuffix ? JSON.parse(record.allowEditSuffix).join("\r\n") : "";
      // this.temp.type = "edit";
      this.temp = {
        ...this.temp,
        fileDirs: record.fileDirs ? JSON.parse(record.fileDirs).join("\r\n") : "",
        type: "edit",
        allowEditSuffix: record.allowEditSuffix ? JSON.parse(record.allowEditSuffix).join("\r\n") : "",
        timeout: record.timeout || 5,
      };
      this.editSshVisible = true;
      // @author jzy 08-04
      this.$refs["editSshForm"] && this.$refs["editSshForm"].resetFields();
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs["editSshForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editSsh(this.temp).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            //this.$refs['editSshForm'].resetFields();
            this.fileList = [];
            this.editSshVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record);
      this.terminalVisible = true;
      this.terminalFullscreen = terminalFullscreen;
    },
    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record);
      this.viewOperationLog = true;
      this.viewOperationLogList = [];

      this.viewOperationLogListQuery = {
        sshId: this.temp.id,
        total: 0,
        page: 1,
      };
      // this.viewOperationLogListQuery.total = 0;
      //this.viewOperationLogListQuery.page = 1;
      this.tempVue = Vue;
      this.handleListLog();
    },
    handleListLog() {
      this.viewOperationLoading = true;
      getSshOperationLogList(this.viewOperationLogListQuery).then((res) => {
        if (res.code === 200) {
          this.viewOperationLogList = res.data.result;
          this.viewOperationLogListQuery.total = res.data.total;
        }
        this.viewOperationLoading = false;
      });
    },
    changeListLog(pagination, filters, sorter) {
      this.viewOperationLogListQuery = CHANGE_PAGE(this.viewOperationLogListQuery, { pagination, sorter });

      this.handleListLog();
    },
    // 选择时间
    onchangeListLogTime(value, dateString) {
      if (dateString[0]) {
        this.viewOperationLogListQuery.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`;
      } else {
        this.viewOperationLogListQuery.createTimeMillis = "";
      }
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `${this.temp.name} (${this.temp.host}) 文件管理`;
      this.drawerVisible = true;
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除 SSH 么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteSsh(record.id).then((res) => {
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
    // 前往节点
    toNode(nodeId) {
      this.$router.push({
        path: "/node/list",
        query: {
          ...this.$route.query,
          tipNodeId: nodeId,
        },
      });
    },
    // 安装节点
    install(record) {
      this.getAgentFn().then(() => {
        this.temp = Object.assign({}, record);
        this.tempNode = {
          url: `${this.temp.host}:2123`,
          protocol: "http",
        };
        //this.agentData = res.data;
        this.nodeVisible = true;
        this.formLoading = false;
        this.$refs["nodeForm"] && this.$refs["nodeForm"].resetFields();
      });
    },
    getAgentFn() {
      return new Promise((resolve) => {
        getAgent().then((res) => {
          this.agentData = res.data;
          resolve();
        });
      });
    },
    // 处理文件移除
    handleRemove(file) {
      const index = this.fileList.indexOf(file);
      const newFileList = this.fileList.slice();
      newFileList.splice(index, 1);
      this.fileList = newFileList;
    },
    // 准备上传文件
    beforeUpload(file) {
      // 只允许上传单个文件
      this.fileList = [file];
      return false;
    },
    // 上传文件
    handleUploadAgent() {
      // 检测文件是否选择了
      if (this.fileList.length === 0) {
        this.$notification.error({
          message: "请选择 zip 文件",
        });
        return false;
      }
      const formData = new FormData();
      formData.append("file", this.fileList[0]);
      uploadAgent(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.fileList = [];
          this.getAgentFn();
        }
      });
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs["nodeForm"].validate((valid) => {
        if (!valid) {
          return false;
        }

        this.formLoading = true;
        const tempData = {
          nodeData: JSON.stringify({ ...this.tempNode }),
          id: this.temp.id,
          path: this.tempNode.path,
          waitCount: this.tempNode.waitCount || "",
          chmod: this.tempNode.chmod || "",
        };
        // formData.append("file", this.fileList[0]);
        // formData.append("id", this.temp.id);
        // // formData.append(");
        // formData.append("path", this.tempNode.path);
        // formData.append("waitCount", this.tempNode.waitCount || "");
        // formData.append("chmod", this.tempNode.chmod || "");
        // 提交数据
        installAgentNode(tempData).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: "操作成功",
            });
            //this.$refs['nodeForm'].resetFields();
            this.nodeVisible = false;
            this.loadData();
          }
          this.formLoading = false;
        });
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false;
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data;
        }
      });
    },
    // 同步到其他工作情况
    syncToWorkspaceShow() {
      this.syncToWorkspaceVisible = true;
      this.loadWorkSpaceListAll();
      this.temp = {
        workspaceId: undefined,
      };
    },
    //
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        this.$notification.warn({
          message: "请选择工作空间",
        });
        return false;
      }
      // 同步
      syncToWorkspace({
        ids: this.tableSelections.join(","),
        workspaceId: this.temp.workspaceId,
      }).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.tableSelections = [];
          this.syncToWorkspaceVisible = false;
          return false;
        }
      });
    },
  },
};
</script>
<style scoped></style>
