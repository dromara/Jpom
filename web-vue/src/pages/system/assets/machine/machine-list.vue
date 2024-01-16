<template>
  <div>
    <a-card :bodyStyle="{ padding: '10px' }">
      <template v-slot:title>
        <a-space>
          <a-input
            class="search-input-item"
            @pressEnter="getMachineList"
            v-model:value="listQuery['%name%']"
            placeholder="机器名称"
          />
          <a-input
            class="search-input-item"
            @pressEnter="getMachineList"
            v-model:value="listQuery['%jpomUrl%']"
            placeholder="节点地址"
          />
          <a-input
            class="search-input-item"
            @pressEnter="getMachineList"
            v-model:value="listQuery['%jpomVersion%']"
            placeholder="插件版本"
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
          <a-select
            v-model:value="listQuery['order_field']"
            allowClear
            placeholder="请选择排序字段"
            class="search-input-item"
          >
            <a-select-option value="networkDelay">网络延迟</a-select-option>
            <a-select-option value="osOccupyCpu">cpu</a-select-option>
            <a-select-option value="osOccupyDisk">硬盘</a-select-option>
            <a-select-option value="osOccupyMemory">内存</a-select-option>
            <a-select-option value="modifyTimeMillis">更新时间</a-select-option>
            <a-select-option value="createTimeMillis">创建时间</a-select-option>
          </a-select>
          <a-button :loading="loading" type="primary" @click="getMachineList">搜索</a-button>
          <a-button type="primary" @click="addMachine">新增</a-button>

          <a-dropdown v-if="this.layoutType === 'table'">
            <template v-slot:overlay>
              <a-menu>
                <a-menu-item key="1" @click="syncToWorkspaceShow()"> 分配节点 </a-menu-item>
                <a-menu-item key="2" @click="syncNodeWhiteConfig"> 同步授权 </a-menu-item>
                <a-menu-item key="3" @click="syncNodeConfig"> 同步系统配置 </a-menu-item>
              </a-menu>
            </template>
            <a-button type="primary"> 批量操作 <DownOutlined /> </a-button>
          </a-dropdown>
          <a-tooltip v-else title="表格视图才能使用同步配置功能">
            <a-button :disabled="true" type="primary"> 批量操作<DownOutlined /></a-button>
          </a-tooltip>
          <a-button type="primary" @click="changeLayout">
            <template #icon>
              <LayoutOutlined v-if="layoutType === 'card'" />
              <TableOutlined v-else />
            </template>

            {{ this.layoutType === 'card' ? '卡片' : '表格' }}
          </a-button>
          <a-tooltip>
            <template v-slot:title>
              <ul>
                <li>节点账号密码为插件端的账号密码,并非用户账号(管理员)密码</li>
                <li>
                  节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json
                  文件查看（如果自定义配置了账号密码将没有此文件）
                </li>
                <li>节点地址为插件端的 IP:PORT 插件端端口默认为：2123</li>
              </ul>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
        </a-space>
      </template>
      <!-- 卡片视图 -->
      <template v-if="this.layoutType === 'card'">
        <a-row :gutter="[16, 16]">
          <template v-if="list && list.length">
            <a-col v-for="item in list" :span="6">
              <a-card :headStyle="{ padding: '0 6px' }" :bodyStyle="{ padding: '10px' }">
                <template v-slot:title>
                  <a-row :gutter="[4, 0]">
                    <a-col :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                      <a-tooltip>
                        <template v-slot:title>
                          <div>节点名称：{{ item.name }}</div>
                          <div>节点地址：{{ item.jpomUrl }}</div>
                        </template>
                        <span @click="showMachineInfo(item)" style="cursor: pointer">
                          {{ item.name }}
                        </span>
                      </a-tooltip>
                    </a-col>
                    <a-col :span="7" style="text-align: right" class="text-overflow-hidden">
                      <a-tooltip
                        :title="`当前状态：${statusMap[item.status]} ${
                          item.statusMsg ? '状态消息：' + item.statusMsg : ''
                        } `"
                      >
                        <a-tag :color="item.status === 1 ? 'green' : 'pink'" style="margin-right: 0">
                          {{ statusMap[item.status] }}</a-tag
                        >
                      </a-tooltip>
                    </a-col>
                  </a-row>
                </template>

                <a-tooltip :title="item.osName">
                  <a-row class="item-info">
                    <a-col :span="6" class="title text-overflow-hidden">系统名称:</a-col>
                    <a-col :span="18" class="content text-overflow-hidden">
                      <a-button
                        :disabled="!item.osName"
                        style="padding: 0; height: auto"
                        type="link"
                        size="small"
                        @click="showMachineInfo(item)"
                      >
                        {{ item.osName || '-' }}
                      </a-button>
                    </a-col>
                  </a-row>
                </a-tooltip>
                <a-tooltip :title="item.osVersion">
                  <a-row class="item-info">
                    <a-col :span="6" class="title text-overflow-hidden">系统版本:</a-col>
                    <a-col :span="18" class="content text-overflow-hidden">
                      {{ item.osVersion || '-' }}
                    </a-col>
                  </a-row>
                </a-tooltip>
                <a-tooltip :title="item.osLoadAverage">
                  <a-row class="item-info">
                    <a-col :span="6" class="title text-overflow-hidden">系统负载:</a-col>
                    <a-col :span="18" class="content text-overflow-hidden">
                      {{ item.osLoadAverage || '-' }}
                    </a-col>
                  </a-row>
                </a-tooltip>
                <a-tooltip :title="item.jpomVersion">
                  <a-row class="item-info">
                    <a-col :span="6" class="title text-overflow-hidden">插件版本:</a-col>
                    <a-col :span="18" class="content text-overflow-hidden">
                      <a-button
                        :disabled="!item.jpomVersion"
                        style="padding: 0; height: auto"
                        type="link"
                        size="small"
                        @click="showMachineUpgrade(item)"
                      >
                        {{ item.jpomVersion || '-' }}
                      </a-button>
                    </a-col>
                  </a-row>
                </a-tooltip>
                <a-row type="flex" align="middle" justify="center" style="margin-top: 10px">
                  <a-button-group>
                    <a-button @click="handleEdit(item)" type="primary" size="small"> 编辑 </a-button>
                    <a-button @click="showMachineInfo(item)" type="primary" size="small">详情</a-button>
                    <a-button @click="syncToWorkspaceShow(item)" type="primary" size="small">分配</a-button>
                    <a-button @click="viewMachineNode(item)" type="primary" size="small">节点</a-button>
                    <a-button @click="deleteMachineInfo(item)" size="small">删除</a-button>
                  </a-button-group>
                </a-row>
              </a-card>
            </a-col>
          </template>
          <!-- </template> -->
          <a-col v-else :span="24">
            <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="没有任何节点" />
          </a-col>
        </a-row>
        <a-row type="flex" justify="center">
          <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
          <a-col>
            <a-pagination
              v-model:value="listQuery.page"
              :showTotal="
                (total) => {
                  return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery)
                }
              "
              :showSizeChanger="true"
              :pageSizeOptions="sizeOptions"
              :pageSize="listQuery.limit"
              :total="listQuery.total"
              :hideOnSinglePage="true"
              @showSizeChange="
                (current, size) => {
                  this.listQuery.limit = size
                  this.getMachineList()
                }
              "
              @change="this.getMachineList"
              show-less-items
            />
          </a-col>
        </a-row>
      </template>
      <!-- 表格视图 -->
      <template v-else-if="this.layoutType === 'table'">
        <a-table
          :columns="columns"
          :data-source="list"
          bordered
          size="middle"
          rowKey="id"
          :pagination="pagination"
          @change="changePage"
          :row-selection="rowSelection"
          :scroll="{
            x: 'max-content'
          }"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'name'">
              <a-tooltip :title="text">
                <a-button style="padding: 0" type="link" size="small" @click="showMachineInfo(record)">
                  {{ text }}
                </a-button>
              </a-tooltip>
            </template>
            <template v-else-if="column.tooltip">
              <a-tooltip :title="text">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tooltip
                :title="`当前状态：${statusMap[record.status]} ${
                  record.statusMsg ? '状态消息：' + record.statusMsg : ''
                } `"
              >
                <a-tag :color="record.status === 1 ? 'green' : 'pink'" style="margin-right: 0">
                  {{ statusMap[record.status] }}
                </a-tag>
              </a-tooltip>
            </template>
            <template v-else-if="column.duration">
              <a-tooltip placement="topLeft" :title="formatDuration(text)">
                <span>{{ formatDuration(text, '', 2) }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.duration2">
              <a-tooltip placement="topLeft" :title="formatDuration((text || 0) * 1000)">
                <span>{{ formatDuration((text || 0) * 1000, '', 2) }}</span>
              </a-tooltip>
            </template>
            <template v-else-if="column.percent2Number">
              <a-tooltip placement="topLeft" :title="`${(text && formatPercent2Number(text) + '%') || '-'}`">
                <span>{{ (text && formatPercent2Number(text) + '%') || '-' }}</span>
              </a-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operation'">
              <a-space>
                <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
                <a-button @click="syncToWorkspaceShow(record)" type="primary" size="small">分配</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </template>
    </a-card>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editVisible"
      width="50%"
      title="编辑机器"
      @ok="handleEditOk"
      :maskClosable="false"
    >
      <a-form ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-item label="机器名称" name="name">
          <a-input :maxLength="50" v-model:value="temp.name" placeholder="机器名称" />
        </a-form-item>
        <a-form-item label="机器分组" name="groupName">
          <custom-select
            v-model:value="temp.groupName"
            :data="groupList"
            inputPlaceholder="新增分组"
            selectPlaceholder="选择分组名"
          >
          </custom-select>
        </a-form-item>

        <a-form-item name="jpomUrl">
          <template v-slot:label>
            <a-tooltip>
              节点地址
              <template v-slot:title
                >节点地址为插件端的 IP:PORT 插件端端口默认为：2123
                <ul>
                  <li>节点地址建议使用内网地址</li>
                  <li>如果插件端正常运行但是连接失败请检查端口是否开放,防火墙规则,云服务器的安全组入站规则</li>
                </ul>
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <template #help>节点地址格式为：IP:PORT (示例：192.168.1.100:2123)</template>
          <a-input v-model:value="temp.jpomUrl" placeholder="节点地址 (192.168.1.100:2123)">
            <template v-slot:addonBefore>
              <a-select
                placeholder="协议类型"
                v-model:value="temp.jpomProtocol"
                default-value="Http://"
                style="width: 160px"
              >
                <a-select-option value="Http"> Http:// </a-select-option>
                <a-select-option value="Https"> Https:// </a-select-option>
              </a-select>
            </template>
          </a-input>
        </a-form-item>

        <a-form-item label="节点账号" name="loginName">
          <a-input v-model:value="temp.jpomUsername" placeholder="节点账号,请查看节点启动输出的信息" />
          <template #help>默认账号为：jpomAgent</template>
        </a-form-item>
        <a-form-item :name="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
          <template v-slot:label>
            <a-tooltip>
              节点密码
              <template v-slot:title>
                节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json
                文件查看（如果自定义配置了账号密码将没有此文件）
              </template>
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input-password v-model:value="temp.jpomPassword" placeholder="节点密码,请查看节点启动输出的信息" />
        </a-form-item>

        <a-collapse>
          <a-collapse-panel key="1" header="其他配置">
            <a-form-item label="模板节点" name="templateNode" help="">
              <a-switch
                checked-children="是"
                un-checked-children="否"
                default-checked
                v-model:checked="temp.templateNode"
              />
              以此机器节点配置为模板,用于快捷同步其他机器节点的配置
            </a-form-item>

            <a-form-item label="超时时间(s)" name="timeOut">
              <a-input-number
                v-model:value="temp.jpomTimeout"
                :min="0"
                placeholder="秒 (值太小可能会取不到节点状态)"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item label="代理" name="jpomHttpProxy">
              <a-input v-model:value="temp.jpomHttpProxy" placeholder="代理地址 (127.0.0.1:8888)">
                <template v-slot:addonBefore>
                  <a-select
                    v-model:value="temp.jpomHttpProxyType"
                    placeholder="选择代理类型"
                    default-value="HTTP"
                    style="width: 100px"
                  >
                    <a-select-option value="HTTP">HTTP</a-select-option>
                    <a-select-option value="SOCKS">SOCKS</a-select-option>
                    <a-select-option value="DIRECT">DIRECT</a-select-option>
                  </a-select>
                </template>
              </a-input>
            </a-form-item>

            <a-form-item label="编码方式" name="transportEncryption">
              <a-select
                show-search
                default-value="0"
                v-model:value="temp.transportEncryption"
                placeholder="请选择编码方式"
              >
                <a-select-option :value="0">不编码</a-select-option>
                <a-select-option :value="1">BASE64</a-select-option>
                <a-select-option :value="2">AES</a-select-option>
              </a-select>
            </a-form-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form>
    </a-modal>
    <!-- 机器信息组件 -->

    <machine-info
      v-if="drawerVisible"
      :machineId="temp.id"
      :name="temp.name"
      @close="
        () => {
          this.drawerVisible = false
        }
      "
    />
    <!-- 机器在线升级相关信息 -->
    <machine-info
      v-if="drawerUpgradeVisible"
      :machineId="temp.id"
      :name="temp.name"
      tab="upgrade"
      @close="
        () => {
          this.drawerUpgradeVisible = false
        }
      "
    />

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

    <!-- 查看机器关联节点 -->
    <a-modal
      destroyOnClose
      v-model:open="viewLinkNode"
      width="50%"
      title="关联节点"
      :footer="null"
      :maskClosable="false"
    >
      <a-list bordered :data-source="nodeList">
        <template v-slot:renderItem="{ item }">
          <a-list-item style="display: block">
            <a-row>
              <a-col :span="10">节点名称：{{ item.name }}</a-col>
              <a-col :span="10">所属工作空间： {{ item.workspace && item.workspace.name }}</a-col>
              <a-col :span="4">
                <a-button type="link" @click="toNode(item.id, item.name, item.workspace && item.workspace.id)">
                  <LoginOutlined /> </a-button
              ></a-col>
            </a-row>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
    <!-- 分发节点授权 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="whiteConfigVisible"
      width="50%"
      title="同步节点授权"
      @ok="onSubmitWhitelist"
      :maskClosable="false"
    >
      <a-alert
        :message="`一键分发同步多个节点的授权配置,不用挨个配置。配置后会覆盖之前的配置,一般用于节点环境一致的情况`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editWhiteForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-item label="模板节点">
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
            @change="(id) => loadWhitelistData(id)"
            placeholder="请选择模板节点"
            v-model:value="temp.templateNodeId"
          >
            <a-select-option v-for="item in templateNodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="项目路径" name="project">
          <a-textarea
            v-model:value="temp.project"
            :rows="5"
            style="resize: none"
            placeholder="请输入项目存放路径授权，回车支持输入多个路径，系统会自动过滤 ../ 路径、不允许输入根路径"
          />
        </a-form-item>

        <a-form-item label="文件后缀" name="allowEditSuffix">
          <a-textarea
            v-model:value="temp.allowEditSuffix"
            :rows="5"
            style="resize: none"
            placeholder="请输入允许编辑文件的后缀及文件编码，不设置编码则默认取系统编码，示例：设置编码：txt@utf-8， 不设置编码：txt"
          />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 分发机器配置 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="nodeConfigVisible"
      width="50%"
      title="同步节点配置"
      :maskClosable="false"
    >
      <template #footer>
        <a-space>
          <a-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(false)">保存</a-button>
          <a-button type="primary" :disabled="!temp.content" @click="onNodeSubmit(true)">保存并重启</a-button>
        </a-space>
      </template>
      <a-alert
        :message="`一键分发同步多个节点的系统配置,不用挨个配置。配置后会覆盖之前的配置,一般用于节点环境一致的情况`"
        style="margin-top: 10px; margin-bottom: 20px"
        banner
      />
      <a-form ref="editNodeConfigForm" :model="temp">
        <a-form-item label="模版节点">
          <a-select
            show-search
            @change="(id) => loadNodeConfig(id)"
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
            placeholder="请选择模版节点"
            v-model:value="temp.templateNodeId"
          >
            <a-select-option v-for="item in templateNodeList" :key="item.id" :value="item.id">
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :wrapper-col="{ span: 24 }">
          <code-editor
            height="40vh"
            v-model:content="temp.content"
            :options="{ mode: 'yaml', tabSize: 2 }"
          ></code-editor>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import {
  machineListData,
  machineListGroup,
  statusMap,
  machineEdit,
  machineDelete,
  machineDistribute,
  machineListNode,
  machineListTemplateNode,
  saveWhitelist,
  saveNodeConfig
} from '@/api/system/assets-machine'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  PAGE_DEFAULT_SHOW_TOTAL,
  formatDuration,
  parseTime,
  formatPercent2Number,
  getCachePageLimit
} from '@/utils/const'
import CustomSelect from '@/components/customSelect'
import { useAppStore } from '@/stores/app'
import { mapState } from 'pinia'
import machineInfo from './machine-func'
import { getWorkSpaceListAll } from '@/api/workspace'
// import Upgrade from "@/pages/node/node-layout/system/upgrade.vue";
import { Empty } from 'ant-design-vue'
import { getWhiteList } from '@/api/node-system'
import { getConfigData } from '@/api/system'
import codeEditor from '@/components/codeEditor'

export default {
  components: {
    CustomSelect,
    machineInfo,

    codeEditor
  },
  data() {
    return {
      Empty,
      statusMap,
      listQuery: Object.assign({ order: 'descend', order_field: 'networkDelay' }, PAGE_DEFAULT_LIST_QUERY, {}),
      sizeOptions: ['8', '12', '16', '20', '24'],
      list: [],
      groupList: [],
      loading: true,
      editVisible: false,
      syncToWorkspaceVisible: false,
      temp: {},
      rules: {
        name: [{ required: true, message: '请输入机器的名称', trigger: 'blur' }]
      },
      drawerVisible: false,
      drawerUpgradeVisible: false,
      workspaceList: [],
      viewLinkNode: false,
      nodeList: [],
      layoutType: null,
      columns: [
        {
          title: '名称',
          dataIndex: 'name',
          width: 150,
          ellipsis: true
        },
        {
          title: '系统名',
          dataIndex: 'osName',
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '主机名',
          dataIndex: 'hostName',
          width: 150,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '节点地址',
          dataIndex: 'jpomUrl',
          width: 150,
          sorter: true,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '分组名',
          dataIndex: 'groupName',
          ellipsis: true,
          width: '100px',
          tooltip: true
        },
        {
          title: '状态',
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: '开机时间',
          sorter: true,
          dataIndex: 'osSystemUptime',
          width: 150,
          ellipsis: true,
          duration2: true
        },
        {
          title: 'CPU占用',
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyCpu',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: '实际内存占用',
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyMemory',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: '硬盘占用',
          sorter: true,
          align: 'center',
          dataIndex: 'osOccupyDisk',
          width: '100px',
          ellipsis: true,
          percent2Number: true
        },
        {
          title: '插件版本号',
          dataIndex: 'jpomVersion',
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '模板节点',
          dataIndex: 'templateNode',
          width: '90px',
          align: 'center',
          ellipsis: true,
          customRender: ({ text }) => {
            return text ? '是' : '否'
          }
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
          customRender: ({ text }) => parseTime(text),
          sorter: true,
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',
          width: '120px',
          fixed: 'right',

          align: 'center'
        }
      ],
      tableSelections: [],
      whiteConfigVisible: false,
      nodeConfigVisible: false,
      templateNodeList: [],
      confirmLoading: false
    }
  },
  computed: {
    ...mapState(useAppStore, ['getCollapsed']),
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
    this.loadGroupList()
    this.changeLayout()
  },
  methods: {
    parseTime,
    formatDuration,
    formatPercent2Number,
    PAGE_DEFAULT_SHOW_TOTAL,
    // 获取所有的分组
    loadGroupList() {
      machineListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    getMachineList(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
        }
        this.loading = false
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.getMachineList()
    },
    addMachine() {
      this.temp = {}
      this.editVisible = true
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record)
      delete this.temp.statusMsg
      this.editVisible = true
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs['editNodeForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        machineEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editNodeForm'].resetFields()
              this.editVisible = false
              this.loadGroupList()
              this.getMachineList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    showMachineInfo(item) {
      this.temp = { ...item }
      this.drawerVisible = true
    },
    // 删除机器
    deleteMachineInfo(item) {
      const that = this
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除机器么？删除会检查数据关联性',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            machineDelete({
              id: item.id
            })
              .then((res) => {
                if (res.code === 200) {
                  $notification.success({
                    message: res.msg
                  })
                  that.getMachineList()
                }
                resolve()
              })
              .catch(reject)
          })
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
      this.confirmLoading = true
      machineDistribute(this.temp)
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
    // 显示节点版本信息
    showMachineUpgrade(item) {
      this.temp = { ...item }
      this.drawerUpgradeVisible = true
    },
    // 查看机器关联的节点
    viewMachineNode(item) {
      machineListNode({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.viewLinkNode = true
          this.nodeList = res.data
        }
      })
    },
    toNode(nodeId, name, wid) {
      const newpage = this.$router.resolve({
        path: '/node/list',
        query: {
          ...this.$route.query,
          nodeId: nodeId,
          pId: 'manage',
          id: 'manageList',
          wid: wid,
          searchNodeName: name
        }
      })
      window.open(newpage.href, '_blank')
    },
    // 切换视图
    changeLayout() {
      if (!this.layoutType) {
        const layoutType = localStorage.getItem('tableLayout')
        // 默认表格
        this.layoutType = layoutType === 'card' ? 'card' : 'table'
      } else {
        this.layoutType = this.layoutType === 'card' ? 'table' : 'card'
        localStorage.setItem('tableLayout', this.layoutType)
      }
      this.listQuery = {
        ...this.listQuery,
        limit: this.layoutType === 'card' ? 8 : getCachePageLimit()
      }
      this.getMachineList()
    },
    syncNodeWhiteConfig() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warn({
          message: '请选择要同步授权的机器节点'
        })
        return
      }
      machineListTemplateNode().then((res) => {
        //
        if (res.code === 200) {
          if (res.data && res.data.length) {
            this.whiteConfigVisible = true
            this.templateNodeList = res.data
            this.temp = {
              ...this.temp,
              templateNodeId: this.templateNodeList[0].id
            }
            this.loadWhitelistData(this.temp.templateNodeId)
          } else {
            $notification.warn({
              message: '还没有配置模板节点'
            })
          }
        }
      })
    },

    // 加载节点授权分发配置
    loadWhitelistData(id) {
      getWhiteList({
        machineId: id
      }).then((res) => {
        if (res.code === 200 && res.data) {
          this.temp = Object.assign({}, this.temp, res.data)
          // { ...thie.temp,res.data };
        }
      })
    },
    onSubmitWhitelist() {
      this.confirmLoading = true
      saveWhitelist({
        ...this.temp,
        ids: this.tableSelections.join(',')
      })
        .then((res) => {
          if (res.code === 200) {
            // 成功
            $notification.success({
              message: res.msg
            })
            this.tableSelections = []
            this.whiteConfigVisible = false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    syncNodeConfig() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warn({
          message: '请选择要同步系统配置的机器节点'
        })
        return
      }
      machineListTemplateNode().then((res) => {
        //
        if (res.code === 200) {
          if (res.data && res.data.length) {
            this.nodeConfigVisible = true
            this.templateNodeList = res.data
            this.temp = {
              ...this.temp,
              templateNodeId: this.templateNodeList[0].id
            }
            this.loadNodeConfig(this.temp.templateNodeId)
          } else {
            $notification.warn({
              message: '还没有配置模板节点'
            })
          }
        }
      })
    },

    // 修改模版节点
    loadNodeConfig(id) {
      getConfigData({ machineId: id }).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, content: res.data.content }
        }
      })
    },
    // submit
    onNodeSubmit(restart) {
      const that = this
      $confirm({
        title: '系统提示',
        content: restart
          ? '真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！ 保存成功后请及时关注重启状态！！'
          : '真的要保存当前配置吗？如果配置有误,可能无法启动服务需要手动还原奥！！！',
        okText: '确认',
        zIndex: 1009,
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            that.confirmLoading = true
            saveNodeConfig({
              ...that.temp,
              restart: restart,
              ids: that.tableSelections.join(',')
            })
              .then((res) => {
                if (res.code === 200) {
                  // 成功
                  $notification.success({
                    message: res.msg
                  })
                  that.nodeConfigVisible = false
                  that.tableSelections = []
                }
                resolve()
              })
              .catch(reject)
              .finally(() => {
                that.confirmLoading = false
              })
          })
        }
      })
    }
  }
}
</script>

<style scoped>
.item-info {
  padding: 4px 0;
}
.item-info .title {
}
.item-info .content {
}
</style>
