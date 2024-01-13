<template>
  <div>
    <!-- 数据表格 -->
    <a-table
      size="middle"
      :data-source="list"
      :columns="columns"
      @change="changePage"
      :pagination="pagination"
      bordered
      rowKey="id"
      :row-selection="rowSelection"
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            v-model:value="listQuery['%name%']"
            @pressEnter="loadData"
            placeholder="名称"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%host%']"
            @pressEnter="loadData"
            placeholder="host"
            class="search-input-item"
          />
          <a-input
            v-model:value="listQuery['%swarmId%']"
            @pressEnter="loadData"
            placeholder="集群ID"
            class="search-input-item"
          />
          <a-select
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
            v-model:value="listQuery.groupName"
            allowClear
            placeholder="分组"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" @click="loadData" :loading="loading">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <a-button :disabled="!this.tableSelections.length" @click="syncToWorkspaceShow()" type="primary">
            批量分配</a-button
          >
          <a-tooltip title="自动检测服务端所在服务器中是否存在 docker，如果存在将自动新增到列表中">
            <a-button type="dashed" @click="handleTryLocalDocker"> <QuestionCircleOutlined />自动探测 </a-button>
          </a-tooltip>
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip :title="text">
            <a-button style="padding: 0" type="link" size="small" @click="handleEdit(record)"> {{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'swarmId'">
          <template v-if="text">
            <a-tooltip v-if="record.swarmControlAvailable" title="管理节点">
              <ClusterOutlined />
            </a-tooltip>
            <a-tooltip v-else title="工作节点">
              <BlockOutlined />
            </a-tooltip>
            <a-popover title="集群信息">
              <template v-slot:content>
                <p>集群ID：{{ record.swarmId }}</p>
                <p>当前节点ID：{{ record.swarmNodeId }}</p>
                <p>当前节点地址：{{ record.swarmNodeAddr }}</p>
                <p>集群创建时间：{{ parseTime(record.swarmCreatedAt) }}</p>
                <p>集群修改时间：{{ parseTime(record.swarmUpdatedAt) }}</p>
              </template>
              {{ text }}
            </a-popover>
          </template>
        </template>

        <template v-else-if="column.dataIndex === 'tlsVerify'">
          <a-tooltip
            placement="topLeft"
            :title="record.tlsVerify ? '开启 TLS 认证,证书信息：' + record.certInfo : '关闭 TLS 认证'"
          >
            <template v-if="record.tlsVerify">
              <template v-if="record.certExist">
                <a-switch
                  size="small"
                  v-model:checked="record.tlsVerify"
                  :disabled="true"
                  checked-children="开"
                  un-checked-children="关"
                />
              </template>
              <a-tag v-else color="red"> 证书丢失 </a-tag>
            </template>
            <template v-else>
              <a-switch
                size="small"
                v-model:checked="record.tlsVerify"
                :disabled="true"
                checked-children="开"
                un-checked-children="关"
              />
            </template>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.failureMsg">
            <a-tag :color="statusMap[record.status].color">{{ statusMap[record.status].desc || '未知' }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" :disabled="record.status !== 1" @click="handleConsole(record)"
              >控制台</a-button
            >
            <template v-if="!record.swarmId && record.status === 1">
              <a-popover title="集群操作">
                <template v-slot:content>
                  <p>
                    <a-button size="small" type="primary" @click="initSwarm(record)">创建集群</a-button>
                  </p>
                  <p>
                    <a-button size="small" type="primary" @click="joinSwarm(record)">加入集群</a-button>
                  </p>
                </template>
                <a-button size="small" type="primary"><EditOutlined />集群</a-button>
              </a-popover>
            </template>
            <template v-else>
              <a-button
                size="small"
                @click="handleSwarmConsole(record)"
                :disabled="parseInt(record.status) !== 1"
                type="primary"
                ><SelectOutlined />集群</a-button
              >
            </template>
            <a-button size="small" @click="syncToWorkspaceShow(record)" type="primary">分配</a-button>
            <a-button size="small" @click="viewWorkspaceDataHander(record)" type="primary">关联</a-button>
            <a-dropdown>
              <a @click="(e) => e.preventDefault()"> 更多 <DownOutlined /> </a>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="handleEdit(record)">编辑</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.swarmId || record.status !== 1"
                      type="primary"
                      danger
                      @click="handleLeaveForce(record)"
                      >退出集群</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      v-model:open="editVisible"
      width="50%"
      title="编辑  Docker"
      @ok="handleEditOk"
      :confirmLoading="confirmLoading"
      :maskClosable="false"
    >
      <a-form ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-space direction="vertical" style="width: 100%">
          <a-alert banner>
            <template v-slot:message>
              <template v-if="temp.enableSsh">
                <ul>
                  <li>SSH 方式连接 docker 是通过终端实现，每次操作 docker 相关 api 需要登录一次终端</li>
                  <li>docker 版本需要大于 18.09 才能使用 SSH 方式连接</li>
                  <li>如果使用 SSH 方式但是 SSH 无法选择，是表示系统没有监测到 docker 服务</li>
                  <li>
                    如果您 SSH 机器中存在 docker 但是系统没有监测到，您需要到【配置管理】->【系统配置目录】中修改
                    <b>ssh/monitor-script.sh</b> 文件来兼容您的机器
                  </li>
                </ul>
              </template>
              <template v-else>
                <ul>
                  <li>
                    系统使用 docker http 接口实现和 docker 通讯和管理，但是默认<b style="color: red"
                      >没有开启任何认证</b
                    >
                  </li>
                  <li>
                    这样使得
                    <b style="color: red">docker 极不安全</b>
                  </li>
                  <li>如果端口暴露到公网很<b style="color: red"> 容易出现挖矿情况 </b></li>
                  <li>
                    所以这里 我们<b style="color: red">强烈建议您使用 TLS 证书</b>（证书生成方式可以参考文档）来连接
                    docker 提升安全性
                  </li>
                  <li>如果端口<b style="color: red">保证在内网中使用可以忽略 TLS 证书</b></li>
                  <li>注意：<b style="color: red">证书的允许的 IP 需要和 docker host 一致</b></li>
                </ul>
              </template>
            </template>
          </a-alert>
          <div></div>
        </a-space>
        <a-form-item label="容器名称" name="name">
          <a-input v-model:value="temp.name" placeholder="容器名称" />
        </a-form-item>
        <a-form-item label="分组" name="groupName">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            inputPlaceholder="新增分组"
            selectPlaceholder="选择分组名"
          >
          </custom-select>
        </a-form-item>
        <a-form-item label="开启SSH访问" name="enableSsh">
          <a-switch v-model:checked="temp.enableSsh" checked-children="开" un-checked-children="关" />
        </a-form-item>
        <a-form-item v-if="temp.enableSsh" label="SSH连接信息" name="enableSsh">
          <a-select v-model:value="temp.machineSshId" allowClear placeholder="SSH连接信息" class="search-input-item">
            <a-select-option :disabled="!item.dockerInfo" v-for="item in sshList" :key="item.id" :value="item.id">
              <a-tooltip :title="`${item.name}(${item.host})`">
                <template #title>
                  {{ item.name }}({{ item.host }})[{{
                    (item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) ||
                    '未获取到 Docker 或者禁用监控'
                  }}]
                </template>
                {{ item.name }}({{ item.host }}) [{{
                  (item.dockerInfo && JSON.parse(item.dockerInfo) && JSON.parse(item.dockerInfo).version) ||
                  '未获取到 Docker 或者禁用监控'
                }}]</a-tooltip
              >
            </a-select-option>
          </a-select>
          <template #help>需要 SSH 监控中能获取到 docker 信息</template>
        </a-form-item>
        <template v-if="!temp.enableSsh">
          <a-form-item label="host" name="host">
            <a-input v-model:value="temp.host" placeholder="容器地址 tcp://127.0.0.1:2375" />
          </a-form-item>

          <a-form-item label="TLS 认证" name="tlsVerify">
            <a-switch v-model:checked="temp.tlsVerify" checked-children="开" un-checked-children="关" />
          </a-form-item>
          <a-form-item
            v-if="temp.tlsVerify"
            label="证书信息"
            name="certInfo"
            help="可以通过证书管理中提前上传或者点击后面选择证书去选择/导入证书"
          >
            <a-input-search
              v-model:value="temp.certInfo"
              placeholder="请输入证书信息或者选择证书信息,证书信息填写规则：序列号:证书类型"
              enter-button="选择证书"
              @search="
                () => {
                  this.certificateVisible = true
                }
              "
            />
          </a-form-item>
        </template>

        <a-collapse>
          <a-collapse-panel key="1" header="其他配置">
            <a-form-item label="超时时间" name="heartbeatTimeout">
              <a-input-number style="width: 100%" v-model:value="temp.heartbeatTimeout" placeholder="超时时间 单位秒" />
            </a-form-item>
            <a-form-item label="仓库地址" name="registryUrl">
              <a-input v-model:value="temp.registryUrl" placeholder="仓库地址" />
            </a-form-item>
            <a-form-item label="仓库账号" name="registryUsername">
              <a-input v-model:value="temp.registryUsername" placeholder="仓库账号" />
            </a-form-item>
            <a-form-item label="仓库密码" name="registryPassword">
              <a-input-password v-model:value="temp.registryPassword" placeholder="仓库密码" />
            </a-form-item>
            <a-form-item label="账号邮箱" name="registryEmail">
              <a-input v-model:value="temp.registryEmail" placeholder="账号邮箱" />
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>
    <!-- 创建集群 -->
    <a-modal
      destroyOnClose
      v-model:open="initSwarmVisible"
      title="创建 Docker 集群"
      @ok="handleSwarm"
      :confirmLoading="confirmLoading"
      :maskClosable="false"
    >
      <a-form ref="initForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-alert message="温馨提示" type="warning">
          <template v-slot:description>
            创建集群会将尝试获取 docker
            中集群信息，如果存在集群信息将自动同步集群信息到系统，反之不存在集群信息将自动创建 swarm 集群
          </template>
        </a-alert>
      </a-form>
    </a-modal>
    <!-- 加入集群 -->
    <a-modal
      destroyOnClose
      v-model:open="joinSwarmVisible"
      title="加入 Docker 集群"
      @ok="handleSwarmJoin"
      :confirmLoading="confirmLoading"
      :maskClosable="false"
    >
      <a-form ref="joinForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="选择集群" name="managerId">
          <a-select
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
            @change="
              (v) => {
                tempList = swarmList.filter((item) => {
                  return item.id === v
                })
                if (tempList.length) {
                  temp = { ...temp, remoteAddr: tempList[0].swarmNodeAddr }
                } else {
                  temp = { ...temp, remoteAddr: '' }
                }
              }
            "
            v-model:value="temp.managerId"
            allowClear
            placeholder="加入到哪个集群"
          >
            <a-select-option v-for="item in swarmList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item v-if="temp.remoteAddr" label="集群IP" name="remoteAddr"
          ><a-input v-model:value="temp.remoteAddr" placeholder="关联容器标签" />
        </a-form-item>

        <a-form-item label="角色" name="role">
          <a-radio-group name="role" v-model:value="temp.role">
            <a-radio value="worker"> 工作节点</a-radio>
            <a-radio value="manager"> 管理节点 </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 控制台 -->

    <console
      v-if="consoleVisible"
      :visible="consoleVisible"
      :machineDockerId="temp.id"
      urlPrefix="/system/assets/docker"
      @close="onClose"
    ></console>
    <!-- </a-drawer> -->
    <!-- 集群控制台 -->

    <swarm-console
      v-if="swarmConsoleVisible"
      :id="temp.id"
      :visible="swarmConsoleVisible"
      :initMenu="temp.menuKey"
      urlPrefix="/system/assets"
      @close="onSwarmClose"
    ></swarm-console>

    <!-- 分配到其他工作空间 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="syncToWorkspaceVisible"
      title="分配到其他工作空间"
      @ok="handleSyncToWorkspace"
      :maskClosable="false"
    >
      <a-form :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item> </a-form-item>
        <a-form-item label="分配类型" name="type">
          <a-radio-group v-model:value="temp.type">
            <a-radio value="docker"> docker </a-radio>
            <a-radio value="swarm" :disabled="temp.swarmId === true ? false : true"> 集群 </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="选择工作空间" name="workspaceId">
          <a-select
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
            v-model:value="temp.workspaceId"
            placeholder="请选择工作空间"
          >
            <a-select-option v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 查看 docker 关联工作空间的信息 -->
    <a-modal
      destroyOnClose
      v-model:open="viewWorkspaceDocker"
      width="50%"
      title="关联工作空间 docker"
      :footer="null"
      :maskClosable="false"
    >
      <a-tabs>
        <a-tab-pane key="1" tab="docker">
          <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.dockerList">
            <template v-slot:renderItem="{ item }">
              <a-list-item style="display: block">
                <a-row>
                  <a-col :span="10">Docker 名称：{{ item.name }}</a-col>
                  <a-col :span="10">所属工作空间： {{ item.workspace && item.workspace.name }}</a-col>
                  <a-col :span="4"> </a-col>
                </a-row>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>
        <a-tab-pane key="2" tab="集群">
          <a-list bordered :data-source="workspaceDockerData && workspaceDockerData.swarmList">
            <template v-slot:renderItem="{ item }">
              <a-list-item style="display: block">
                <a-row>
                  <a-col :span="10">集群名称：{{ item.name }}</a-col>
                  <a-col :span="10">所属工作空间： {{ item.workspace && item.workspace.name }}</a-col>
                  <a-col :span="4"> </a-col>
                </a-row>
              </a-list-item>
            </template>
          </a-list>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <!-- 选择证书文件 -->
    <a-drawer
      destroyOnClose
      :title="`选择证书文件`"
      placement="right"
      :open="certificateVisible"
      width="85vw"
      :zIndex="1009"
      @close="
        () => {
          this.certificateVisible = false
        }
      "
      :footer-style="{ textAlign: 'right' }"
    >
      <certificate
        v-if="certificateVisible"
        ref="certificate"
        @confirm="
          (certInfo) => {
            this.temp = { ...this.temp, certInfo: certInfo }
            this.certificateVisible = false
          }
        "
        @cancel="
          () => {
            this.certificateVisible = false
          }
        "
      ></certificate>
      <template #footer>
        <a-space>
          <a-button
            @click="
              () => {
                this.chooseVisible = 0
              }
            "
          >
            取消
          </a-button>
          <a-button
            type="primary"
            @click="
              () => {
                this.$refs['certificate'].handerConfirm()
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
import {
  dockerList,
  editDocker,
  tryLocalDocker,
  deleteDcoker,
  initDockerSwarm,
  joinDockerSwarm,
  dockerSwarmListAll,
  dcokerSwarmLeaveForce,
  machineDockerDistribute,
  dockerListWorkspace,
  dockerListGroup,
  statusMap
} from '@/api/system/assets-docker'
import { machineSshListData } from '@/api/system/assets-ssh'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'
import { getWorkSpaceListAll } from '@/api/workspace'
import Console from '@/pages/docker/console'
import SwarmConsole from '@/pages/docker/swarm/console.vue'

import certificate from '@/pages/certificate/list.vue'
import CustomSelect from '@/components/customSelect'

export default {
  components: {
    Console,
    SwarmConsole,
    certificate,
    CustomSelect
  },
  props: {},
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      list: [],
      groupList: [],
      temp: {},
      editVisible: false,
      templateVisible: false,
      consoleVisible: false,
      swarmConsoleVisible: false,
      initSwarmVisible: false,
      joinSwarmVisible: false,
      swarmList: [],
      sshList: [],
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          ellipsis: true,

          width: 150
        },
        {
          title: 'host',
          dataIndex: 'host',
          ellipsis: true,
          tooltip: true,

          width: 150
        },
        {
          title: 'docker版本',
          dataIndex: 'dockerVersion',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },

        {
          title: '状态',
          dataIndex: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: 'TLS',
          dataIndex: 'tlsVerify',
          width: '80px',
          align: 'center',
          ellipsis: true
        },
        {
          title: '分组名',
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: '集群',
          dataIndex: 'swarmId',
          ellipsis: true
        },
        // { title: "apiVersion", dataIndex: "apiVersion", width: 100, ellipsis: true, },
        {
          title: '最后修改人',
          dataIndex: 'modifyUser',
          width: 120,
          ellipsis: true
        },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',

          fixed: 'right',
          align: 'center',
          width: '320px'
        }
      ],
      rules: {
        // id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: '请填写容器名称', trigger: 'blur' }],
        // host: [{ required: true, message: "请填写容器地址", trigger: "blur" }],

        managerId: [
          {
            required: true,
            message: '请选择要加入到哪个集群',
            trigger: 'blur'
          }
        ],
        role: [{ required: true, message: '请选择节点角色', trigger: 'blur' }],
        remoteAddr: [
          { required: true, message: '请填写集群IP', trigger: 'blur' },
          {
            pattern:
              /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/,
            message: '填写正确的IP地址'
          }
        ]
      },
      syncToWorkspaceVisible: false,
      workspaceList: [],
      viewWorkspaceDocker: false,
      workspaceDockerData: {
        dockerList: [],
        swarmList: []
      },
      tableSelections: [],
      certificateVisible: false,
      confirmLoading: false
    }
  },
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
  mounted() {
    this.sshListData()
    this.loadData()
    this.loadGroupList()
  },
  methods: {
    //
    parseTime,
    // 获取所有的分组
    loadGroupList() {
      dockerListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    sshListData() {
      machineSshListData().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data.result
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page

      dockerList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
          const dockerId = this.$route.query.dockerId
          const type = this.$route.query.type
          this.list.map((item) => {
            if (dockerId === item.id && type === 'docker') {
              this.handleConsole(item)
            } else if (dockerId === item.id && type === 'swarm') {
              this.handleSwarmConsole(item)
            }
          })
        }
        this.loading = false
      })
    },
    // 新增
    handleAdd() {
      this.temp = {}
      this.editVisible = true

      this.$refs['editForm']?.resetFields()
    },
    // 控制台
    handleConsole(record) {
      this.temp = { ...record }
      this.consoleVisible = true

      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'docker' }
        })
      }
    },
    // 集群控制台
    handleSwarmConsole(record) {
      this.temp = { ...record }
      this.swarmConsoleVisible = true
      let dockerId = this.$route.query.dockerId
      if (dockerId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, dockerId: record.id, type: 'swarm' }
        })
      }
    },
    // 关闭抽屉层
    onClose() {
      this.consoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    onSwarmClose() {
      this.swarmConsoleVisible = false
      const query = Object.assign({}, this.$route.query)
      delete query.dockerId
      delete query.type
      this.$router.replace({
        query: query
      })
    },
    // 修改
    handleEdit(record) {
      this.temp = { ...record }
      this.editVisible = true

      // this.temp = { ...this.temp };

      this.$refs['editForm']?.resetFields()
    },

    // 提交  数据
    handleEditOk() {
      // 检验表单

      this.$refs['editForm'].validate().then(() => {
        const temp = Object.assign({}, this.temp)
        if (temp.enableSsh) {
          if (!temp.machineSshId) {
            this.$message.warning('请选择SSH连接信息')
            return false
          }
        } else {
          if (!temp.host) {
            this.$message.warning('请输入host')
            return false
          }
        }
        this.confirmLoading = true
        editDocker(temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.editVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除
    handleDelete(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除该 Docker 么？删除只会检查本地系统的数据关联,不会删除 docker 容器中数据',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 组装参数
            const params = {
              id: record.id
            }
            deleteDcoker(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 强制解绑
    handleLeaveForce(record) {
      const html =
        "<h1 style='color:red;'>真的要强制退出集群吗？</h1> " +
        "<h3 style='color:red;'>如果当前集群还存在可能出现数据不一致问题奥</h3> " +
        "<ul style='color:red;'>" +
        '<li>请提前备份数据再操作奥</li>' +
        "<li style='font-weight: bold;'>请不要优先退出管理节点</li>" +
        '<li>操作不能撤回奥</li>' +
        ' </ul>'
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 组装参数
            const params = {
              id: record.id
            }
            dcokerSwarmLeaveForce(params)
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.loadData()
                }
                resolve()
              })
              .catch(reject)
          })
        }
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },

    // 创建集群
    initSwarm(record) {
      this.temp = {
        id: record.id
      }
      this.initSwarmVisible = true
      this.$refs['initForm']?.resetFields()
    },
    handleSwarm() {
      this.$refs['initForm'].validate().then(() => {
        this.confirmLoading = true
        initDockerSwarm(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.initSwarmVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 加入集群
    joinSwarm(record) {
      dockerSwarmListAll().then((res) => {
        this.swarmList = res.data
        this.temp = {
          id: record.id
        }
        this.joinSwarmVisible = true
        this.$refs['joinForm']?.resetFields()
      })
    },
    // 处理加入集群
    handleSwarmJoin() {
      this.$refs['joinForm'].validate().then(() => {
        this.confirmLoading = true

        joinDockerSwarm(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.joinSwarmVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    //
    handleTryLocalDocker() {
      tryLocalDocker().then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
          this.loadData()
        }
      })
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
          ids: item.id,
          swarmId: item.swarmId ? true : false
        }
      } else {
        this.temp = {
          swarmId: true
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.type) {
        $notification.warn({
          message: '请选择分配类型'
        })
        return false
      }
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
      this.confirmLoading = true
      machineDockerDistribute(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })

            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 查看工作空间的 docker 信息
    viewWorkspaceDataHander(item) {
      this.workspaceDockerData = {}
      dockerListWorkspace({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.viewWorkspaceDocker = true
          this.workspaceDockerData = res.data
        }
      })
    }
  }
}
</script>
