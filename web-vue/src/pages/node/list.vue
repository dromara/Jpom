<template>
  <div class="full-content">
    <template v-if="this.getUserInfo && this.getUserInfo.systemUser && !this.loading && this.listQuery.total <= 0">
      <a-result title="当前工作空间还没有节点">
        <template slot="subTitle"> 需要您在需要被管理的服务器中安装 agent ，并将 agent 信息添加到系统中 </template>
        <template #extra>
          <a-button type="primary" @click="fastInstallNodeShow">快速安装 </a-button>
          <router-link to="/system/assets/machine-list"> <a-button key="console" type="primary">手动添加</a-button></router-link>
        </template>
        <div class="desc">
          <p style="font-size: 16px">
            <strong>解决办法</strong>
          </p>
          <ol>
            <li>【推荐】使用快速安装方式导入机器并自动添加逻辑节点</li>
            <li>请到【系统管理】-> 【资产管理】-> 【机器管理】添加节点，或者将已添加的机器授权关联、分配到此工作空间</li>
          </ol>
        </div>
      </a-result>
    </template>
    <template v-else>
      <a-card :bodyStyle="{ padding: '10px' }">
        <template slot="title">
          <a-space>
            <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="节点名称" />

            <a-select show-search option-filter-prop="children" v-model="listQuery.group" allowClear placeholder="分组" class="search-input-item">
              <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
            </a-select>
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button :loading="loading" type="primary" @click="loadData">搜索</a-button>
            </a-tooltip>
            <a-button
              type="primary"
              @click="
                () => {
                  this.fastInstallNode = true;
                }
              "
              >快速安装
            </a-button>
            <a-dropdown v-if="this.layoutType === 'table'">
              <a-button type="primary" :disabled="!tableSelections || !tableSelections.length" @click="syncToWorkspaceShow">工作空间同步</a-button>
            </a-dropdown>
            <a-tooltip v-else title="表格视图才能使用工作空间同步功能">
              <a-button :disabled="true" type="primary"> 工作空间同步 </a-button>
            </a-tooltip>

            <a-button type="primary" @click="changeLayout" :icon="this.layoutType === 'card' ? 'layout' : 'table'"> {{ this.layoutType === "card" ? "卡片" : "表格" }} </a-button>
            <a-tooltip placement="bottom">
              <template slot="title">
                <div>
                  <ul>
                    <li>监控数据目前采用原生命令获取,和真实情况有一定差异可以当做参考依据</li>
                    <li>监控频率可以到服务端配置文件中修改</li>
                    <li>悬停到仪表盘上显示具体含义</li>
                    <li>点击仪表盘查看监控历史数据</li>
                    <li>点击延迟可以查看对应节点网络延迟历史数据</li>
                    <li>为了避免部分节点不能及时响应造成监控阻塞,节点统计超时时间不受节点超时配置影响将采用默认超时时间(10秒)</li>
                  </ul>
                </div>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
            <div class="header-statistic">
              <a-statistic-countdown format="s 秒" title="刷新倒计时 " :value="deadline" @finish="onFinish" />
            </div>
          </a-space>
        </template>
        <a-table
          v-if="this.layoutType === 'table'"
          :columns="columns"
          :data-source="list"
          bordered
          size="middle"
          rowKey="id"
          :pagination="pagination"
          @change="
            (pagination, filters, sorter) => {
              this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
              this.loadData();
            }
          "
          :row-selection="rowSelection"
        >
          <a-tooltip slot="url" slot-scope="text, record" placement="topLeft" :title="text">
            <template v-if="record.machineNodeData">
              <span>{{ record.machineNodeData.jpomProtocol }}://{{ record.machineNodeData.jpomUrl }}</span>
            </template>
            <span v-else> - </span>
          </a-tooltip>
          <template slot="name" slot-scope="text, record">
            <template v-if="record.openStatus !== 1">
              <a-tooltip :title="`${text}`">
                <span>{{ text }}</span>
              </a-tooltip>
            </template>
            <template v-else>
              <a-tooltip :title="`${text} 点击进入节点管理`" @click="handleNode(record)">
                <a-button type="link" style="padding: 0px" size="small">
                  <a-icon type="fullscreen" /><span>{{ text }}</span>
                </a-button>
              </a-tooltip>
            </template>
          </template>
          <a-tooltip
            slot="status"
            slot-scope="text, item"
            placement="topLeft"
            :title="`${statusMap[item.machineNodeData && item.machineNodeData.status] || '未知'} ${item.machineNodeData && item.machineNodeData.statusMsg}`"
          >
            <template v-if="item.openStatus === 1">
              <a-tag :color="item.machineNodeData && item.machineNodeData.status === 1 ? 'green' : 'pink'" style="margin-right: 0px">
                {{ statusMap[item.machineNodeData && item.machineNodeData.status] || "未知" }}
              </a-tag>
            </template>
            <a-tag v-else>未启用</a-tag>
          </a-tooltip>
          <a-tooltip slot="osName" slot-scope="text, item" placement="topLeft" :title="text">
            <span>{{ item.machineNodeData && item.machineNodeData.osName }}</span>
          </a-tooltip>
          <a-tooltip slot="javaVersion" slot-scope="text, item" placement="topLeft" :title="text">
            <span>{{ item.machineNodeData && item.machineNodeData.javaVersion }}</span>
          </a-tooltip>
          <a-tooltip
            slot="jvmInfo"
            slot-scope="text, item"
            placement="topLeft"
            :title="`剩余内存：${renderSize(item.machineNodeData && item.machineNodeData.jvmFreeMemory)} 总内存：${renderSize(item.machineNodeData && item.machineNodeData.jvmTotalMemory)}`"
          >
            <span>{{ renderSize(item.machineNodeData && item.machineNodeData.jvmFreeMemory) }} / {{ renderSize(item.machineNodeData && item.machineNodeData.jvmTotalMemory) }}</span>
          </a-tooltip>
          <!-- <a-tooltip slot="freeMemory" slot-scope="text" placement="topLeft" :title="renderSize(text)">
        <span>{{ renderSize(text) }}</span>
      </a-tooltip> -->

          <a-tooltip slot="runTime" slot-scope="text, item" placement="topLeft" :title="formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime)">
            <span>{{ formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, "", 2) }}</span>
          </a-tooltip>
          <template slot="projectCount" slot-scope="text, item">
            <div v-if="item.machineNodeData && item.machineNodeData.status === 1" @click="syncNode(item)">
              <a-tooltip placement="topLeft" title="节点中的所有项目数量,点击重新同步节点项目信息">
                <a-tag>{{ item.machineNodeData.jpomProjectCount }} </a-tag>
                <a-icon type="sync" />
              </a-tooltip>
            </div>
            <span v-else>-</span>
          </template>
          <template slot="scriptCount" slot-scope="text, item">
            <div v-if="item.machineNodeData && item.machineNodeData.status === 1" @click="syncNodeScript(item)">
              <a-tooltip placement="topLeft" title="节点中的所有脚本模版数量,点击重新同步脚本模版信息">
                <a-tag>{{ item.machineNodeData.jpomScriptCount }} </a-tag>
                <a-icon type="sync" />
              </a-tooltip>
            </div>
            <span v-else>-</span>
          </template>

          <template slot="operation" slot-scope="text, record, index">
            <a-space>
              <a-tooltip title="如果按钮不可用则表示当前节点已经关闭啦,需要去编辑中启用">
                <a-button size="small" class="jpom-node-manage-btn" type="primary" @click="handleNode(record)" :disabled="record.openStatus !== 1"><a-icon type="apartment" />管理</a-button>
              </a-tooltip>
              <a-tooltip title="需要到编辑中去为一个节点绑定一个 ssh信息才能启用该功能">
                <a-button size="small" type="primary" @click="handleTerminal(record)" :disabled="!record.sshId"><a-icon type="code" />终端</a-button>
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
                    <a-tooltip placement="leftBottom" title="删除会检查数据关联性,并且节点不存在项目或者脚本">
                      <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-item>
                    <a-tooltip placement="leftBottom" title="解绑会检查数据关联性,同时将自动删除节点项目和脚本缓存信息,一般用于服务器无法连接且已经确定不再使用">
                      <a-button size="small" type="danger" @click="handleUnbind(record)">解绑</a-button>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item>
                    <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1" @click="sortItemHander(record, index, 'top')">置顶</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1" @click="sortItemHander(record, index, 'up')">上移</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total" @click="sortItemHander(record, index, 'down')">
                      下移
                    </a-button>
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
            </a-space>
          </template>
        </a-table>
        <template v-else-if="this.layoutType === 'card'">
          <a-row>
            <a-row :gutter="[16, 16]">
              <template v-if="list && list.length">
                <a-col v-for="item in list" :key="item.id" :span="6">
                  <template>
                    <a-card :headStyle="{ padding: '0 6px' }" :bodyStyle="{ padding: '10px' }">
                      <template slot="title">
                        <a-row :gutter="[4, 0]">
                          <a-col :span="17" class="jpom-node-manage-btn" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                            <a-tooltip>
                              <template slot="title">
                                点击进入节点管理
                                <div>节点名称：{{ item.name }}</div>
                                <div>节点地址：{{ item.url }}</div>
                              </template>

                              <a-button type="link" style="padding: 0px" size="small" @click="handleNode(item)">
                                <span> {{ item.name }}</span>
                              </a-button>
                            </a-tooltip>
                          </a-col>
                          <a-col :span="7" style="text-align: right">
                            <a-tooltip>
                              <template slot="title">
                                <div>当前状态：{{ statusMap[item.machineNodeData && item.machineNodeData.status] }}</div>
                                <div>状态描述：{{ (item.machineNodeData && item.machineNodeData.statusMsg) || "" }}</div>
                              </template>
                              <a-tag :color="item.machineNodeData && item.machineNodeData.status === 1 ? 'green' : 'pink'" style="margin-right: 0px">
                                {{ statusMap[item.machineNodeData && item.machineNodeData.status] }}
                              </a-tag>
                            </a-tooltip>
                          </a-col>
                        </a-row>
                      </template>

                      <a-row :gutter="[8, 8]">
                        <a-col :span="8" style="text-align: center">
                          <a-tooltip @click="handleHistory(item, 'nodeTop')" :title="`CPU 占用率：${item.occupyCpu}%`">
                            <a-progress
                              type="circle"
                              :width="80"
                              :stroke-color="{
                                '0%': '#87d068',
                                '30%': '#87d068',
                                '100%': '#108ee9',
                              }"
                              size="small"
                              status="active"
                              :percent="item.occupyCpu"
                            />
                          </a-tooltip>
                        </a-col>
                        <a-col :span="8" style="text-align: center">
                          <a-tooltip @click="handleHistory(item, 'nodeTop')" :title="`硬盘占用率：${item.occupyDisk}%`">
                            <a-progress
                              type="circle"
                              :width="80"
                              :stroke-color="{
                                '0%': '#87d068',
                                '30%': '#87d068',
                                '100%': '#108ee9',
                              }"
                              size="small"
                              status="active"
                              :percent="item.occupyDisk"
                            />
                          </a-tooltip>
                        </a-col>
                        <a-col :span="8" style="text-align: center">
                          <a-tooltip @click="handleHistory(item, 'nodeTop')" :title="`内存占用率：${item.occupyMemory}%`">
                            <a-progress
                              :width="80"
                              type="circle"
                              :stroke-color="{
                                '0%': '#87d068',
                                '30%': '#87d068',
                                '100%': '#108ee9',
                              }"
                              size="small"
                              status="active"
                              :percent="item.occupyMemory"
                            />
                          </a-tooltip>
                        </a-col>
                      </a-row>

                      <a-row :gutter="[8, 8]" style="text-align: center">
                        <a-col :span="8">
                          <a-tooltip
                            @click="handleHistory(item, 'networkDelay')"
                            :title="`${'延迟' + (formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-') + ' 点击查看历史趋势'}`"
                          >
                            <a-statistic
                              title="延迟"
                              :value="item.machineNodeData && item.machineNodeData.networkDelay"
                              valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                              :formatter="
                                (v) => {
                                  return formatDuration(item.machineNodeData && item.machineNodeData.networkDelay, '', 2) || '-';
                                }
                              "
                            />
                          </a-tooltip>
                        </a-col>
                        <a-col :span="8">
                          <a-tooltip :title="formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, '', 1) || '-'">
                            <a-statistic
                              title="运行时间"
                              valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                              :formatter="
                                (v) => {
                                  return formatDuration(item.machineNodeData && item.machineNodeData.jpomUptime, '', 2) || '-';
                                }
                              "
                            />
                          </a-tooltip>
                        </a-col>
                        <a-col :span="8">
                          <a-tooltip :title="`${parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis)}`">
                            <a-statistic
                              title="更新时间"
                              valueStyle="font-size: 14px;overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
                              :formatter="
                                (v) => {
                                  return parseTime(item.machineNodeData && item.machineNodeData.modifyTimeMillis, '{h}:{i}:{s}');
                                }
                              "
                            />
                          </a-tooltip>
                        </a-col>
                      </a-row>
                    </a-card>
                  </template>
                </a-col>
              </template>
              <a-col v-else :span="24">
                <a-empty description="没有任何节点" />
              </a-col>
            </a-row>
          </a-row>
          <a-row type="flex" justify="center">
            <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
            <a-col>
              <a-pagination
                v-model="listQuery.page"
                :showTotal="
                  (total) => {
                    return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery);
                  }
                "
                :showSizeChanger="true"
                :pageSizeOptions="sizeOptions"
                :pageSize="listQuery.limit"
                :total="listQuery.total"
                :hideOnSinglePage="true"
                @showSizeChange="
                  (current, size) => {
                    this.listQuery.limit = size;
                    this.loadData();
                  }
                "
                @change="this.loadData"
                show-less-items
              />
            </a-col>
          </a-row>
        </template>
      </a-card>
    </template>

    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editNodeVisible" width="50%" title="编辑节点" @ok="handleEditNodeOk" :maskClosable="false">
      <a-form-model ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item label="节点名称" prop="name">
          <a-input :maxLength="50" v-model="temp.name" placeholder="节点名称" />
        </a-form-model-item>
        <a-form-model-item label="分组名称" prop="group">
          <custom-select v-model="temp.group" :data="groupList" suffixIcon="" inputPlaceholder="添加分组" selectPlaceholder="选择分组名"> </custom-select>
        </a-form-model-item>

        <a-form-model-item label="节点状态" prop="openStatus">
          <a-switch
            :checked="temp.openStatus == 1"
            @change="
              (checked) => {
                temp.openStatus = checked ? 1 : 0;
              }
            "
            checked-children="启用"
            un-checked-children="停用"
            default-checked
          />
        </a-form-model-item>
        <a-form-model-item label="绑定 SSH " prop="sshId">
          <a-select show-search option-filter-prop="children" v-model="temp.sshId" placeholder="请选择SSH">
            <a-select-option value="">不绑定</a-select-option>
            <a-select-option v-for="ssh in sshList" :key="ssh.id" :disabled="ssh.disabled">{{ ssh.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 管理节点 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" :width="`${this.getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`" :visible="drawerVisible" @close="onClose">
      <!-- 节点管理组件 -->
      <node-layout v-if="drawerVisible" :node="temp" />
    </a-drawer>
    <!-- Terminal -->
    <a-modal
      v-model="terminalVisible"
      :bodyStyle="{
        padding: '0px 10px',
        paddingTop: '10px',
        marginRight: '10px',
        height: `70vh`,
      }"
      width="80%"
      title="Terminal"
      :footer="null"
      :maskClosable="false"
    >
      <terminal v-if="terminalVisible" :sshId="temp.sshId" :nodeId="temp.id" />
    </a-modal>

    <!-- 快速安装插件端 -->
    <a-modal
      destroyOnClose
      v-model="fastInstallNode"
      width="80%"
      title="快速安装插件端"
      :footer="null"
      :maskClosable="false"
      @cancel="
        () => {
          this.fastInstallNode = false;
          this.loadData();
        }
      "
    >
      <fastInstall v-if="fastInstallNode"></fastInstall>
    </a-modal>
    <!-- 同步到其他工作空间 -->
    <a-modal destroyOnClose v-model="syncToWorkspaceVisible" title="同步到其他工作空间" @ok="handleSyncToWorkspace" :maskClosable="false">
      <a-alert message="温馨提示" type="warning">
        <template slot="description">
          <ul>
            <li>同步机制采用节点地址确定是同一个服务器（节点）</li>
            <li>当目标工作空间不存在对应的节点时候将自动创建一个新的节点（逻辑节点）</li>
            <li>当目标工作空间已经存在节点时候将自动同步节点授权信息、代理配置信息</li>
          </ul>
        </template>
      </a-alert>
      <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
        <a-form-model-item> </a-form-model-item>
        <a-form-model-item label="选择工作空间" prop="workspaceId">
          <a-select show-search option-filter-prop="children" v-model="temp.workspaceId" placeholder="请选择工作空间">
            <a-select-option :disabled="getWorkspaceId === item.id" v-for="item in workspaceList" :key="item.id">{{ item.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 历史监控 -->
    <a-modal destroyOnClose v-model="monitorVisible" width="75%" :title="`${this.temp.name}历史监控图表`" :footer="null" :maskClosable="false">
      <node-top v-if="monitorVisible" :type="this.temp.type" :nodeId="this.temp.id"></node-top>
    </a-modal>
  </div>
</template>
<script>
import { mapGetters } from "vuex";
import { deleteNode, editNode, getNodeGroupAll, getNodeList, syncProject, syncToWorkspace, unbind, sortItem } from "@/api/node";
import { getSshListAll } from "@/api/ssh";
import { syncScript } from "@/api/node-other";
import NodeLayout from "./node-layout";
import Terminal from "@/pages/ssh/terminal";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, formatDuration, renderSize, formatPercent2Number, parseTime, PAGE_DEFAULT_SHOW_TOTAL, getCachePageLimit } from "@/utils/const";
import { getWorkSpaceListAll } from "@/api/workspace";
import CustomSelect from "@/components/customSelect";
import fastInstall from "./fast-install.vue";
import { statusMap } from "@/api/system/assets-machine";
import NodeTop from "@/pages/node/node-layout/node-top";

export default {
  components: {
    NodeLayout,
    Terminal,
    CustomSelect,
    fastInstall,
    NodeTop,
  },
  data() {
    return {
      loading: true,

      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      sshList: [],
      list: [],
      sizeOptions: ["8", "12", "16", "20", "24"],
      groupList: [],
      refreshInterval: 5,
      deadline: 0,
      temp: {},
      monitorVisible: false,
      layoutType: null,
      editNodeVisible: false,
      drawerVisible: false,
      terminalVisible: false,

      fastInstallNode: false,
      syncToWorkspaceVisible: false,
      drawerTitle: "",
      columns: [
        { title: "节点名称", dataIndex: "name", width: 100, sorter: true, key: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "状态", dataIndex: "status", width: 100, ellipsis: true, scopedSlots: { customRender: "status" } },
        { title: "节点地址", dataIndex: "url", key: "url", width: "190px", ellipsis: true, scopedSlots: { customRender: "url" } },
        { title: "系统名", dataIndex: "osName", key: "osName", width: "100px", ellipsis: true, scopedSlots: { customRender: "osName" } },
        { title: "JDK 版本", dataIndex: "javaVersion", width: 100, key: "javaVersion", ellipsis: true, scopedSlots: { customRender: "javaVersion" } },
        { title: "JVM 信息", dataIndex: "jvmInfo", width: 100, ellipsis: true, scopedSlots: { customRender: "jvmInfo" } },
        // { title: "JVM 剩余内存", dataIndex: "machineNodeData.jvmFreeMemory", ellipsis: true, scopedSlots: { customRender: "freeMemory" } },

        { title: "项目数", dataIndex: "count", key: "count", width: "90px", scopedSlots: { customRender: "projectCount" } },
        { title: "脚本数", dataIndex: "scriptCount", key: "scriptCount", width: "90px", scopedSlots: { customRender: "scriptCount" } },

        { title: "插件运行", dataIndex: "runTime", width: "100px", key: "runTime", ellipsis: true, scopedSlots: { customRender: "runTime" } },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        { title: "操作", dataIndex: "operation", key: "operation", fixed: "right", width: "210px", scopedSlots: { customRender: "operation" }, align: "center" },
      ],

      rules: {
        name: [{ required: true, message: "请输入节点名称", trigger: "blur" }],
      },
      workspaceList: [],
      tableSelections: [],
    };
  },
  computed: {
    ...mapGetters(["getCollapsed", "getWorkspaceId", "getUserInfo"]),
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
  watch: {},
  created() {
    const searchNodeName = this.$route.query.searchNodeName;
    if (searchNodeName) {
      this.listQuery = { ...this.listQuery, "%name%": searchNodeName };
    }

    this.changeLayout();
    this.loadGroupList();
  },

  methods: {
    formatDuration,
    renderSize,
    PAGE_DEFAULT_SHOW_TOTAL,
    parseTime,
    CHANGE_PAGE,
    introGuideList() {
      this.$store.dispatch("tryOpenGuide", {
        key: "node-list-manage",
        beforeKey: "index",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".jpom-node-manage-btn"),
              intro: "点击【节点管理】按钮可以进入节点管理,节点管理里面可以挖掘更多功能",
            },
          ],
        },
      });
    },
    // 获取所有的分组
    loadGroupList() {
      getNodeGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    // 加载 SSH 列表
    loadSshList() {
      getSshListAll().then((res) => {
        if (res.code === 200) {
          this.sshList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      return new Promise((resolve) => {
        this.list = [];
        this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
        this.loading = true;
        getNodeList(this.listQuery).then((res) => {
          if (res.code === 200) {
            this.list =
              res.data.result &&
              res.data.result.map((item) => {
                // console.log(item);
                item.occupyCpu = formatPercent2Number(item.machineNodeData?.osOccupyCpu);

                item.occupyDisk = formatPercent2Number(item.machineNodeData?.osOccupyDisk);
                item.occupyMemory = formatPercent2Number(item.machineNodeData?.osOccupyMemory);
                return item;
              });
            this.listQuery.total = res.data.total;
            let nodeId = this.$route.query.nodeId;
            this.list.map((item) => {
              if (nodeId === item.id) {
                this.handleNode(item);
              }
            });
            if (res.data.total > 0) {
              this.$nextTick(() => {
                this.introGuideList();
              });
            }
            resolve();
            this.refreshInterval = 30;
            this.deadline = Date.now() + this.refreshInterval * 1000;
          }
          this.loading = false;
        });
      });
    },

    // 进入终端
    handleTerminal(record) {
      this.temp = Object.assign({}, record);
      this.terminalVisible = true;
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      this.loadSshList();
      // this.temp.tempGroup = "";
      this.editNodeVisible = true;
    },
    // 提交节点数据
    handleEditNodeOk() {
      // 检验表单
      this.$refs["editNodeForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        editNode(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editNodeForm"].resetFields();
            this.editNodeVisible = false;
            this.loadData();
            this.loadGroupList();
          }
        });
      });
    },
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除节点么？删除会检查数据关联性,并且节点不存在项目或者脚本",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          deleteNode(record.id).then((res) => {
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
    // 解绑
    handleUnbind(record) {
      const html =
        "<b style='font-size: 20px;'>真的要解绑节点么？</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        "<li>解绑会检查数据关联性,同时将自动删除节点项目和脚本缓存信息</b></li>" +
        "<li>一般用于服务器无法连接且已经确定不再使用</li>" +
        "<li>如果误操作会产生冗余数据！！！</li>" +
        " </ul>";

      const h = this.$createElement;
      this.$confirm({
        title: "危险操作！！！",
        content: h("div", null, [h("p", { domProps: { innerHTML: html } }, null)]),
        okButtonProps: { props: { type: "danger", size: "small" } },
        cancelButtonProps: { props: { type: "primary" } },
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          unbind(record.id).then((res) => {
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
    // 管理节点
    handleNode(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `${record.name}`;
      this.drawerVisible = true;
      let nodeId = this.$route.query.nodeId;
      if (nodeId !== record.id) {
        this.$router.push({
          query: { ...this.$route.query, nodeId: record.id },
        });
      }
    },
    syncNode(node) {
      syncProject(node.id).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
          return false;
        }
      });
    },
    syncNodeScript(node) {
      syncScript({
        nodeId: node.id,
      }).then((res) => {
        if (res.code == 200) {
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false;
      let query = Object.assign({}, this.$route.query);
      delete query.nodeId, delete query.id, delete query.pId;
      this.$router.replace({
        query: query,
      });
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
        toWorkspaceId: this.temp.workspaceId,
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
    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: "确定要将此数据置顶吗？",
        up: "确定要将此数上移吗？",
        down: "确定要将此数据下移吗？下移操作可能因为列表后续数据没有排序值操作无效！",
      };
      let msg = msgData[method] || "确定要操作吗？";
      if (!record.sortValue) {
        msg += " 当前数据为默认状态,操后上移或者下移可能不会达到预期排序,还需要对相关数据都操作后才能达到预期排序";
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1].id;
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 解锁
          sortItem({
            id: record.id,
            method: method,
            compareId: compareId,
          }).then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });

              this.loadData();
              return false;
            }
          });
        },
      });
    },
    // 切换视图
    changeLayout() {
      if (!this.layoutType) {
        const layoutType = localStorage.getItem("tableLayout");
        // 默认表格
        this.layoutType = layoutType === "card" ? "card" : "table";
      } else {
        this.layoutType = this.layoutType === "card" ? "table" : "card";
        localStorage.setItem("tableLayout", this.layoutType);
      }
      this.listQuery = { ...this.listQuery, limit: this.layoutType === "card" ? 8 : getCachePageLimit() };
      this.loadData();
    },
    onFinish() {
      if (this.drawerVisible) {
        // 打开节点 不刷新
        return;
      }
      if (this.$attrs.routerUrl !== this.$route.path) {
        // 重新计算倒计时
        this.deadline = Date.now() + this.refreshInterval * 1000;
        return;
      }
      this.loadData();
    },
    // 历史图表
    handleHistory(record, type) {
      this.monitorVisible = true;
      this.temp = record;
      this.temp = { ...this.temp, type };
    },
    fastInstallNodeShow() {
      this.fastInstallNode = true;
    },
  },
};
</script>
<style scoped>
/* .filter {
  margin-bottom: 10px;
} */
/*
.filter-item {
  width: 150px;
  margin-right: 10px;
} */
/*
.btn-add {
  margin-left: 10px;
  margin-right: 0;
} */

.header-statistic /deep/ .ant-statistic div {
  display: inline-block;
}

.header-statistic /deep/ .ant-statistic-content-value,
.header-statistic /deep/ .ant-statistic-content {
  font-size: 16px;
}
</style>
