<template>
  <div class="full-content">
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" tab="管理">
        <!-- 数据表格 -->
        <a-table
          :data-source="list"
          :columns="columns"
          size="middle"
          :pagination="pagination"
          @change="changePage"
          bordered
          rowKey="id"
          :row-selection="rowSelection"
        >
          <template #title>
            <a-space>
              <a-input
                class="search-input-item"
                @pressEnter="loadData"
                v-model="listQuery['%name%']"
                placeholder="ssh名称"
              />
              <a-input
                class="search-input-item"
                @pressEnter="loadData"
                v-model="listQuery['%host%']"
                placeholder="host"
              />
              <a-select
                show-search
                option-filter-prop="children"
                v-model="listQuery.groupName"
                allowClear
                placeholder="分组"
                class="search-input-item"
              >
                <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
              </a-select>

              <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
                <a-button type="primary" :loading="loading" @click="loadData">搜索 </a-button>
              </a-tooltip>

              <a-button type="primary" @click="handleAdd">新增</a-button>
              <a-button :disabled="!this.tableSelections.length" @click="syncToWorkspaceShow()" type="primary">
                批量分配</a-button
              >
              <a-button icon="download" type="primary" @click="handlerExportData()">导出</a-button>
              <a-dropdown>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button type="primary" @click="handlerImportTemplate()">下载导入模板</a-button>
                    </a-menu-item>
                  </a-menu>
                </template>

                <a-upload
                  name="file"
                  accept=".csv"
                  action=""
                  :showUploadList="false"
                  :multiple="false"
                  :before-upload="beforeUpload"
                >
                  <a-button type="primary" icon="upload"> 导入 <down-outlined /> </a-button>
                </a-upload>
              </a-dropdown>
              <a-tooltip>
                <template #title>
                  <div>
                    <ul>
                      <li>节点状态是异步获取有一定时间延迟</li>
                      <li>节点状态会自动识别服务器中是否存在 java 环境,如果没有 Java 环境不能快速安装节点</li>
                      <li>关联节点如果服务器存在 java 环境,但是插件端未运行则会显示快速安装按钮</li>
                    </ul>
                  </div>
                </template>
                <question-circle-filled />
              </a-tooltip>
            </a-space>
          </template>
          <a-tooltip #name slot-scope="text, item" :title="text">
            <a-button style="padding: 0" type="link" size="small" @click="handleEdit(item)"> {{ text }}</a-button>
          </a-tooltip>
          <a-tooltip #tooltip slot-scope="text" :title="text"> {{ text }}</a-tooltip>
          <a-tooltip #host slot-scope="text, record" :title="`${text}:${record.port}`">
            {{ text }}:{{ record.port }}</a-tooltip
          >

          <a-popover title="系统信息" #osName slot-scope="text, record">
            <template #content>
              <p>系统名：{{ record.osName }}</p>
              <p>系统版本：{{ record.osVersion }}</p>
              <p>CPU型号：{{ record.osCpuIdentifierName }}</p>
              <p>主机名：{{ record.hostName }}</p>
              <p>开机时间：{{ formatDuration(record.osSystemUptime) }}</p>
            </template>
            {{ text || '未知' }}
          </a-popover>
          <template #nodeId slot-scope="text, record">
            <div v-if="record.javaVersion">
              <a-tooltip
                v-if="record.jpomAgentPid > 0"
                placement="topLeft"
                :title="` ssh 中已经运行了插件端进程ID：${record.jpomAgentPid},java :  ${record.javaVersion}`"
              >
                <a-tag> {{ record.jpomAgentPid }}</a-tag>
              </a-tooltip>
              <a-button v-else size="small" type="primary" @click="install(record)">安装节点</a-button>
            </div>

            <a-tag color="orange" v-else>no java</a-tag>
          </template>
          <template #status slot-scope="text, record">
            <a-tooltip :title="record.statusMsg">
              <a-tag :color="record.status === 1 ? 'green' : 'red'">{{
                record.status === 1 ? '正常' : '无法连接'
              }}</a-tag>
            </a-tooltip>
          </template>
          <a-tooltip #renderSize slot-scope="text" placement="topLeft" :title="renderSize(text)">
            <span>{{ renderSize(text) }}</span>
          </a-tooltip>
          <a-tooltip
            #osOccupyMemory
            slot-scope="text, record"
            placement="topLeft"
            :title="`内存使用率：${formatPercent(record.osOccupyMemory)},总内存：${renderSize(record.osMoneyTotal)}`"
          >
            <span>{{ formatPercent(record.osOccupyMemory) }}/{{ renderSize(record.osMoneyTotal) }}</span>
          </a-tooltip>

          <a-popover title="硬盘信息" #osMaxOccupyDisk slot-scope="text, record">
            <template #content>
              <p>硬盘总量：{{ renderSize(record.osMoneyTotal) }}</p>
              <p>硬盘最大的使用率：{{ formatPercent(record.osMaxOccupyDisk) }}</p>
              <p>使用率最大的分区：{{ record.osMaxOccupyDiskName }}</p>
            </template>
            <span>{{ formatPercent(record.osMaxOccupyDisk) }} / {{ renderSize(record.osMoneyTotal) }}</span>
          </a-popover>

          <a-tooltip
            #osOccupyCpu
            slot-scope="text, record"
            placement="topLeft"
            :title="`CPU使用率：${formatPercent2Number(record.osOccupyCpu)}%,CPU数：${record.osCpuCores}`"
          >
            <span>{{ (formatPercent2Number(record.osOccupyCpu) || '-') + '%' }} / {{ record.osCpuCores }}</span>
          </a-tooltip>

          <template #operation slot-scope="text, record">
            <a-space>
              <a-dropdown>
                <a-button size="small" type="primary" @click="handleTerminal(record, false)">
                  终端 <down-outlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button size="small" type="primary" icon="fullscreen" @click="handleTerminal(record, true)"
                        >全屏终端</a-button
                      >
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-button size="small" type="primary" @click="syncToWorkspaceShow(record)">分配</a-button>
              <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
              <a-button size="small" type="primary" @click="handleViewWorkspaceSsh(record)">关联</a-button>

              <a-dropdown>
                <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                  更多
                  <down-outlined />
                </a>
                <template #overlay>
                  <a-menu>
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
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </a-table>
        <!-- 编辑区 -->
        <a-modal
          destroyOnClose
          v-model="editSshVisible"
          width="600px"
          title="编辑 SSH"
          @ok="handleEditSshOk"
          :maskClosable="false"
        >
          <a-form ref="editSshForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
            <a-form-item label="SSH 名称" prop="name">
              <a-input v-model="temp.name" :maxLength="50" placeholder="SSH 名称" />
            </a-form-item>
            <a-form-item label="分组名称" prop="group">
              <custom-select
                v-model="temp.groupName"
                :data="groupList"
                suffixIcon=""
                inputPlaceholder="添加分组"
                selectPlaceholder="选择分组名"
              >
              </custom-select>
            </a-form-item>
            <a-form-item label="Host" prop="host">
              <a-input-group compact prop="host">
                <a-input style="width: 70%" v-model="temp.host" placeholder="主机 Host" />
                <a-input-number style="width: 30%" v-model="temp.port" :min="1" placeholder="端口号" />
              </a-input-group>
            </a-form-item>
            <a-form-item label="认证方式" prop="connectType">
              <a-radio-group v-model="temp.connectType" :options="options" />
            </a-form-item>
            <a-form-item prop="user">
              <template #label>
                用户名
                <a-tooltip v-if="!temp.id">
                  <template #title> 账号支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                  <question-circle-filled />
                </a-tooltip>
              </template>
              <a-input v-model="temp.user" placeholder="用户">
                <a-tooltip
                  v-if="temp.id"
                  #suffix
                  title=" 密码字段和密钥字段在编辑的时候不会返回，如果需要重置或者清空就请点我"
                >
                  <a-button size="small" type="danger" @click="handerRestHideField(temp)">清除</a-button>
                </a-tooltip>
              </a-input>
            </a-form-item>
            <!-- 新增时需要填写 -->
            <!--				<a-form-item v-if="temp.type === 'add'" label="Password" prop="password">-->
            <!--					<a-input-password v-model="temp.password" placeholder="密码"/>-->
            <!--				</a-form-item>-->
            <!-- 修改时可以不填写 -->
            <a-form-item
              :prop="`${temp.type === 'add' && temp.connectType === 'PASS' ? 'password' : 'password-update'}`"
            >
              <template #label>
                密码
                <a-tooltip v-if="!temp.id">
                  <template #title> 密码支持引用工作空间变量：<b>$ref.wEnv.xxxx</b> xxxx 为变量名称</template>
                  <question-circle-filled />
                </a-tooltip>
              </template>
              <a-input-password
                v-model="temp.password"
                :placeholder="`${temp.type === 'add' ? '密码' : '密码若没修改可以不用填写'}`"
              />
            </a-form-item>
            <a-form-item v-if="temp.connectType === 'PUBKEY'" prop="privateKey">
              <template #label>
                私钥内容
                <a-tooltip v-if="temp.type !== 'edit'" placement="topLeft">
                  <template #title
                    >不填将使用默认的 $HOME/.ssh 目录中的配置,使用优先级是：id_dsa>id_rsa>identity
                  </template>
                  <question-circle-filled />
                </a-tooltip>
              </template>

              <a-textarea
                v-model="temp.privateKey"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                placeholder="私钥内容,不填将使用默认的 $HOME/.ssh 目录中的配置。支持配置文件目录:file:/xxxx/xx"
              />
            </a-form-item>
            <a-form-item label="编码格式" prop="charset">
              <a-input v-model="temp.charset" placeholder="编码格式" />
            </a-form-item>
            <a-form-item label="超时时间(s)" prop="timeout">
              <a-input-number v-model="temp.timeout" :min="1" placeholder="单位秒,最小值 1 秒" style="width: 100%" />
            </a-form-item>
            <a-form-item label="文件后缀" prop="suffix" help="此配置仅对服务端管理生效,工作空间的 ssh 配置需要单独配置">
              <a-input
                v-model="temp.allowEditSuffix"
                type="textarea"
                :rows="5"
                style="resize: none"
                placeholder="请输入允许编辑文件的后缀及文件编码，不设置编码则默认取系统编码，多个使用换行。示例：设置编码：txt@utf-8， 不设置编码：txt"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 安装节点 -->
        <a-modal
          destroyOnClose
          v-model="nodeVisible"
          width="80%"
          title="安装插件端"
          :footer="null"
          @cancel="
            () => {
              this.nodeVisible = false
              this.loadData()
            }
          "
          :maskClosable="false"
        >
          <fastInstall v-if="nodeVisible"></fastInstall>
        </a-modal>
        <!-- 文件管理 -->
        <a-drawer
          destroyOnClose
          :title="`${this.temp.name} 文件管理`"
          placement="right"
          width="90vw"
          :visible="drawerVisible"
          @close="
            () => {
              this.drawerVisible = false
            }
          "
        >
          <ssh-file v-if="drawerVisible" :machineSshId="temp.id" />
        </a-drawer>
        <!-- Terminal -->
        <a-modal
          destroyOnClose
          :dialogStyle="{
            maxWidth: '100vw',
            top: this.terminalFullscreen ? 0 : false,
            paddingBottom: 0
          }"
          :width="terminalFullscreen ? '100vw' : '80vw'"
          :bodyStyle="{
            padding: '0px 10px',
            paddingTop: '10px',
            marginRight: '10px',
            height: `${this.terminalFullscreen ? 'calc(100vh - 56px)' : '70vh'}`
          }"
          v-model="terminalVisible"
          :title="temp.name"
          :footer="null"
          :maskClosable="false"
        >
          <terminal v-if="terminalVisible" :machineSshId="temp.id" />
        </a-modal>
        <!-- 操作日志 -->
        <a-modal
          destroyOnClose
          v-model="viewOperationLog"
          title="操作日志"
          width="80vw"
          :footer="null"
          :maskClosable="false"
        >
          <OperationLog v-if="viewOperationLog" :machineSshId="temp.id"></OperationLog>
        </a-modal>
        <!-- 查看 ssh 关联工作空间的信息 -->
        <a-modal
          destroyOnClose
          v-model="viewWorkspaceSsh"
          width="50%"
          title="关联工作空间ssh"
          :footer="null"
          :maskClosable="false"
        >
          <a-list bordered :data-source="workspaceSshList">
            <a-list-item #renderItem slot-scope="item" style="display: block">
              <a-row>
                <a-col :span="10">SSH名称：{{ item.name }}</a-col>
                <a-col :span="10">所属工作空间： {{ item.workspace && item.workspace.name }}</a-col>
                <a-col :span="4">
                  <a-button v-if="item.workspace" size="small" type="primary" @click="configWorkspaceSsh(item)"
                    >配置
                  </a-button>
                  <a-button v-else size="small" type="danger" @click="handleDeleteWorkspaceItem(item)">删除 </a-button>
                </a-col>
              </a-row>
            </a-list-item>
          </a-list>
        </a-modal>
        <a-modal
          destroyOnClose
          v-model="configWorkspaceSshVisible"
          width="50%"
          title="配置ssh"
          @ok="handleConfigWorkspaceSshOk"
          :maskClosable="false"
        >
          <a-form
            ref="editConfigWorkspaceSshForm"
            :rules="rules"
            :model="temp"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 18 }"
          >
            <a-form-item label="" :label-col="{ span: 0 }" :wrapper-col="{ span: 24 }">
              <a-alert message="当前配置仅对选择的工作空间生效,其他工作空间需要另行配置" banner />
            </a-form-item>
            <a-form-item label="SSH 名称">
              <a-input v-model="temp.name" :disabled="true" :maxLength="50" placeholder="SSH 名称" />
            </a-form-item>
            <a-form-item label="工作空间名称">
              <a-input v-model="temp.workspaceName" :disabled="true" :maxLength="50" placeholder="工作空间名称" />
            </a-form-item>

            <a-form-item prop="fileDirs">
              <template #label>
                文件目录
                <a-tooltip>
                  <template #title> 绑定指定目录可以在线管理，同时构建 ssh 发布目录也需要在此配置 </template>
                  <question-circle-filled />
                </a-tooltip>
              </template>
              <a-textarea
                v-model="temp.fileDirs"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                placeholder="授权可以直接访问的目录，多个回车换行即可"
              />
            </a-form-item>

            <a-form-item label="文件后缀" prop="suffix">
              <a-input
                v-model="temp.allowEditSuffix"
                type="textarea"
                :rows="5"
                style="resize: none"
                placeholder="请输入允许编辑文件的后缀及文件编码，不设置编码则默认取系统编码，多个使用换行。示例：设置编码：txt@utf-8， 不设置编码：txt"
              />
            </a-form-item>
            <a-form-item prop="notAllowedCommand">
              <template #label>
                禁止命令
                <a-tooltip>
                  <template #title>
                    限制禁止在在线终端执行的命令
                    <ul>
                      <li>超级管理员没有任何限制</li>
                      <li>其他用户可以配置权限解除限制</li>
                    </ul>
                  </template>
                  <question-circle-filled />
                </a-tooltip>
              </template>
              <a-textarea
                v-model="temp.notAllowedCommand"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                placeholder="禁止命令是不允许在终端执行的名，多个逗号隔开。(超级管理员没有任何限制)"
              />
            </a-form-item>
          </a-form>
        </a-modal>
        <!-- 分配到其他工作空间 -->
        <a-modal
          destroyOnClose
          v-model="syncToWorkspaceVisible"
          title="分配到其他工作空间"
          @ok="handleSyncToWorkspace"
          :maskClosable="false"
        >
          <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
            <a-form-item> </a-form-item>
            <a-form-item label="选择工作空间" prop="workspaceId">
              <a-select
                show-search
                option-filter-prop="children"
                v-model="temp.workspaceId"
                placeholder="请选择工作空间"
              >
                <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-form>
        </a-modal>
      </a-tab-pane>
      <a-tab-pane key="2" tab="命令日志"> <OperationLog type="machinessh"></OperationLog></a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import {
  machineSshListData,
  machineSshListGroup,
  machineSshEdit,
  machineSshDelete,
  machineListGroupWorkspaceSsh,
  machineSshSaveWorkspaceConfig,
  machineSshDistribute,
  restHideField,
  importTemplate,
  exportData,
  importData
} from '@/api/system/assets-ssh'
import {
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  CHANGE_PAGE,
  renderSize,
  formatPercent,
  formatDuration,
  formatPercent2Number
} from '@/utils/const'
import fastInstall from '@/pages/node/fast-install.vue'
import CustomSelect from '@/components/customSelect'
import SshFile from '@/pages/ssh/ssh-file'
import Terminal from '@/pages/ssh/terminal'
import OperationLog from '@/pages/system/assets/ssh/operation-log'
import { deleteForeSsh } from '@/api/ssh'
import { getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: { fastInstall, CustomSelect, Terminal, SshFile, OperationLog },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    }
  },
  data() {
    return {
      loading: true,
      groupList: [],
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,
      temp: {},
      options: [
        { label: '密码', value: 'PASS' },
        { label: '证书', value: 'PUBKEY' }
      ],
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          width: 120,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: 'name' }
        },

        {
          title: 'Host',
          dataIndex: 'host',
          width: 120,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: 'host' }
        },
        // { title: "Port", dataIndex: "port", sorter: true, width: 80, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        {
          title: '用户名',
          dataIndex: 'user',
          sorter: true,
          width: '80px',
          ellipsis: true,
          scopedSlots: { customRender: 'tooltip' }
        },
        {
          title: '系统名',
          dataIndex: 'osName',
          width: 120,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: 'osName' }
        },
        {
          title: 'CPU',
          dataIndex: 'osOccupyCpu',
          sorter: true,
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'osOccupyCpu' }
        },
        {
          title: '内存',
          dataIndex: 'osOccupyMemory',
          sorter: true,
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'osOccupyMemory' }
        },
        {
          title: '硬盘',
          dataIndex: 'osMaxOccupyDisk',
          sorter: true,
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'osMaxOccupyDisk' }
        },
        // { title: "编码格式", dataIndex: "charset", sorter: true, width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        {
          title: '连接状态',
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px',
          scopedSlots: { customRender: 'status' }
        },
        {
          title: '节点状态',
          dataIndex: 'nodeId',
          scopedSlots: { customRender: 'nodeId' },
          width: '80px',
          ellipsis: true
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          scopedSlots: { customRender: 'operation' },
          width: '300px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],
      // 表单校验规则
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口号', trigger: 'blur' }],
        connectType: [
          {
            required: true,
            message: '请选择连接方式',
            trigger: 'blur'
          }
        ],
        user: [{ required: true, message: '请输入账号名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
      },
      nodeVisible: false,

      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,
      drawerVisible: false,
      workspaceSshList: [],
      viewWorkspaceSsh: false,
      configWorkspaceSshVisible: false,
      syncToWorkspaceVisible: false,
      workspaceList: [],
      tableSelections: []
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    formatDuration,
    renderSize,
    formatPercent,
    formatPercent2Number,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineSshListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
        }
        this.loading = false
      })
    },
    // 获取所有的分组
    loadGroupList() {
      machineSshListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 新增 SSH
    handleAdd() {
      this.temp = {
        charset: 'UTF-8',
        port: 22,
        timeout: 5,
        connectType: 'PASS'
      }
      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record, {
        allowEditSuffix: record.allowEditSuffix ? JSON.parse(record.allowEditSuffix).join('\r\n') : ''
      })

      this.temp = {
        ...this.temp,

        timeout: record.timeout || 5
      }
      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
      this.loadGroupList()
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs['editSshForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        // 提交数据
        machineSshEdit(this.temp).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.editSshVisible = false
            this.loadData()
            this.loadGroupList()
          }
        })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 安装节点
    install() {
      this.nodeVisible = true
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
      this.terminalFullscreen = terminalFullscreen
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: '系统提示',
        content: '真的要删除机器 SSH 么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          machineSshDelete({
            id: record.id
          }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },

    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record)
      this.viewOperationLog = true
    },
    // 查看工作空间的 ssh
    handleViewWorkspaceSsh(item) {
      machineListGroupWorkspaceSsh({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            machineSshId: item.id
          }
          this.viewWorkspaceSsh = true
          this.workspaceSshList = res.data
        }
      })
    },
    // 配置 ssh
    configWorkspaceSsh(item) {
      this.temp = {
        ...this.temp,
        id: item.id,
        name: item.name,
        fileDirs: item.fileDirs ? JSON.parse(item.fileDirs).join('\r\n') : '',
        allowEditSuffix: item.allowEditSuffix ? JSON.parse(item.allowEditSuffix).join('\r\n') : '',
        workspaceName: item.workspace?.name
      }
      this.configWorkspaceSshVisible = true
    },
    // 提交 SSH 配置 数据
    handleConfigWorkspaceSshOk() {
      // 检验表单
      this.$refs['editConfigWorkspaceSshForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        // 提交数据
        machineSshSaveWorkspaceConfig(this.temp).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.configWorkspaceSshVisible = false
            machineListGroupWorkspaceSsh({
              id: this.temp.machineSshId
            }).then((res) => {
              if (res.code === 200) {
                this.workspaceSshList = res.data
              }
            })
          }
        })
      })
    },
    // 删除工作空间的数据
    handleDeleteWorkspaceItem(record) {
      $confirm({
        title: '系统提示',
        content: '真的要删除对应工作空间的 SSH 么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          deleteForeSsh(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              machineListGroupWorkspaceSsh({
                id: this.temp.machineSshId
              }).then((res) => {
                if (res.code === 200) {
                  this.workspaceSshList = res.data
                }
              })
            }
          })
        }
      })
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)

      this.drawerVisible = true
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 同步到其他工作情况
    syncToWorkspaceShow(item) {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      if (item) {
        this.temp = {
          ids: item.id
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: '请选择工作空间'
        })
        return false
      }
      if (!this.temp.ids) {
        this.temp = { ...this.temp, ids: this.tableSelections.join(',') }
        this.tableSelections = []
      }
      // 同步
      machineSshDistribute(this.temp).then((res) => {
        if (res.code == 200) {
          $notification.success({
            message: res.msg
          })

          this.syncToWorkspaceVisible = false
          return false
        }
      })
    },
    // 清除隐藏字段
    handerRestHideField(record) {
      $confirm({
        title: '系统提示',
        content: '真的要清除 SSH 隐藏字段信息么？（密码，私钥）',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 恢复
          restHideField(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), '_blank')
    },
    beforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      importData(formData).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
    }
  }
}
</script>
