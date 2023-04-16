<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      :expandIconColumnIndex="-1"
      :expandIconAsCell="false"
      :expandedRowKeys="expandedRowKeys"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
          this.loadData()
        }
      "
      :row-selection="rowSelection"
      bordered
      rowKey="id"
    >
      <template slot="title">
        <a-space>
          <a-select
            v-model="listQuery.group"
            allowClear
            placeholder="请选择分组"
            class="search-input-item"
            @change="loadData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-input class="search-input-item" v-model="listQuery['%projectId%']" placeholder="项目ID" />
          <a-input class="search-input-item" v-model="listQuery['%name%']" placeholder="项目名称" />
          <a-select v-model="listQuery.runMode" allowClear placeholder="项目类型" class="search-input-item">
            <a-select-option v-for="item in runModeList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>

          <a-dropdown>
            <a-button type="primary"> 批量操作 <a-icon type="down" /> </a-button>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="primary" @click="batchStart">批量启动</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="primary" @click="batchRestart">批量重启</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="danger" @click="batchStop">批量关闭</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

          <a-button icon="download" type="primary" @click="handlerExportData()">导出</a-button>
          <a-dropdown>
            <a-menu slot="overlay">
              <a-menu-item key="1">
                <a-button type="primary" @click="handlerImportTemplate()">下载导入模板</a-button>
              </a-menu-item>
            </a-menu>

            <a-upload
              name="file"
              accept=".csv"
              action=""
              :showUploadList="false"
              :multiple="false"
              :before-upload="beforeUpload"
            >
              <a-button type="primary" icon="upload"> 导入 <a-icon type="down" /> </a-button>
            </a-upload>
          </a-dropdown>
          <a-tooltip>
            <template slot="title">
              <div>状态数据是异步获取有一定时间延迟</div>
            </template>
            <question-circle-filled />
          </a-tooltip>
        </a-space>
      </template>
      <template slot="copyIcon" slot-scope="javaCopyItemList, record">
        <template v-if="javaCopyItemList">
          <div
            v-if="!expandedRowKeys.includes(record.id)"
            class="ant-table-row-expand-icon ant-table-row-collapsed"
            @click="handleExpand(record, true)"
          ></div>
          <div
            v-else
            class="ant-table-row-expand-icon ant-table-row-expanded"
            @click="handleExpand(record, false)"
          ></div>
        </template>
        <template v-else>
          <a-tooltip title="当项目存在副本集时此列将可以用于查看副本集功能，其他情况此列没有实际作用">
            <a-icon type="minus-circle" />
          </a-tooltip>
        </template>
      </template>
      <a-tooltip
        slot="name"
        slot-scope="text, record"
        placement="topLeft"
        :title="`名称：${text}`"
        @click="handleEdit(record)"
      >
        <a-button type="link" style="padding: 0px" size="small"
          ><a-icon v-if="record.outGivingProject" type="apartment" />{{ text }}
        </a-button>
      </a-tooltip>

      <a-tooltip slot="path" slot-scope="text, item" placement="topLeft" :title="item.whitelistDirectory + item.lib">
        <span>{{ item.whitelistDirectory + item.lib }}</span>
      </a-tooltip>
      <!-- <a-tooltip slot="modifyUser" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip> -->
      <template slot="status" slot-scope="text, record">
        <template v-if="record.error">
          <a-tooltip :title="record.error">
            <a-icon type="warning" />
          </a-tooltip>
        </template>
        <template v-else>
          <a-tooltip v-if="noFileModes.includes(record.runMode)" title="状态操作请到控制台中控制">
            <a-switch :checked="text" disabled checked-children="开" un-checked-children="关" />
          </a-tooltip>
          <span v-else>-</span>
        </template>
      </template>

      <a-tooltip
        slot="port"
        slot-scope="text, record"
        placement="topLeft"
        :title="`进程号：${(record.pids || [record.pid || '-']).join(',')} / 端口号：${record.port || '-'}`"
      >
        <span>{{ record.port || '-' }}/{{ (record.pids || [record.pid || '-']).join(',') }}</span>
      </a-tooltip>

      <template slot="expandedRowRender" slot-scope="record">
        <a-table :columns="copyColumns" :data-source="record.javaCopyItemList" rowKey="id" :pagination="false">
          <template slot="id" slot-scope="text">
            {{ text }}
            <a-icon type="reload" @click="getRuningProjectCopyInfo(record)" />
          </template>
          <template slot="name" slot-scope="text, record">
            {{ text || record.id }}
          </template>
          <a-switch
            slot="status"
            slot-scope="text"
            :checked="text"
            disabled
            checked-children="开"
            un-checked-children="关"
          />
          <template slot="operation" slot-scope="text, copyRecord">
            <a-space>
              <a-button size="small" type="primary" @click="handleConsoleCopy(record, copyRecord)">控制台</a-button>
              <a-button size="small" type="danger" @click="handleDeleteCopy(record, copyRecord, 'thorough')"
                >删除</a-button
              >
            </a-space>
          </template>
        </a-table>
      </template>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
          <a-tooltip
            :title="`${
              noFileModes.includes(record.runMode) ? '到控制台去管理项目状态' : 'File 类型项目不能使用控制台功能'
            }`"
          >
            <a-button
              size="small"
              type="primary"
              @click="handleConsole(record)"
              :disabled="!noFileModes.includes(record.runMode)"
              >控制台</a-button
            >
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
                <a-button size="small" type="primary" @click="copyItem(record)">复制</a-button>
              </a-menu-item>
              <!-- <a-menu-item>
                <a-button size="small" type="primary" @click="handleReplica(record)" v-if="javaModes.includes(record.runMode)" :disabled="!record.javaCopyItemList">副本集 </a-button>
              </a-menu-item> -->
              <template v-if="record.outGivingProject">
                <a-menu-item>
                  <a-tooltip title="节点分发项目需要到节点分发中去删除">
                    <a-button size="small" type="danger" :disabled="true">删除</a-button>
                  </a-tooltip>
                </a-menu-item>
                <a-menu-item>
                  <a-tooltip title="节点分发项目需要到节点分发中去删除">
                    <a-button size="small" type="danger" :disabled="true">彻底删除</a-button>
                  </a-tooltip>
                </a-menu-item>
                <a-menu-item>
                  <a-button size="small" type="danger" @click="handleReleaseOutgiving(record)">释放分发</a-button>
                </a-menu-item>
              </template>
              <template v-else>
                <a-menu-item>
                  <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
                </a-menu-item>
                <a-menu-item>
                  <a-button size="small" type="danger" @click="handleDelete(record, 'thorough')">彻底删除</a-button>
                </a-menu-item>
              </template>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
    </a-table>
    <!-- 编辑区 -->
    <a-modal
      destroyOnClose
      v-model="editProjectVisible"
      width="60vw"
      title="编辑项目"
      @ok="handleEditProjectOk"
      :maskClosable="false"
    >
      <a-form ref="editProjectForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="项目 ID" prop="id">
          <a-input
            :maxLength="50"
            v-model="temp.id"
            v-if="temp.type === 'edit'"
            :disabled="temp.type === 'edit'"
            placeholder="创建之后不能修改"
          />
          <template v-else>
            <a-input-search
              :maxLength="50"
              v-model="temp.id"
              placeholder="创建之后不能修改"
              @search="
                () => {
                  this.temp = { ...this.temp, id: randomStr(6) }
                }
              "
            >
              <template slot="enterButton">
                <a-button type="primary"> 随机生成 </a-button>
              </template>
            </a-input-search>
          </template>
        </a-form-item>

        <a-form-item label="项目名称" prop="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model="temp.name" :maxLength="50" placeholder="项目名称" />
            </a-col>
            <a-col :span="4" style="text-align: right">分组名称：</a-col>
            <a-col :span="10">
              <custom-select
                suffixIcon=""
                :maxLength="50"
                v-model="temp.group"
                :data="groupList"
                inputPlaceholder="添加分组"
                selectPlaceholder="选择分组"
              >
              </custom-select>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item prop="runMode">
          <template slot="label">
            运行方式
            <a-tooltip v-show="temp.type !== 'edit'">
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
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-select v-model="temp.runMode" placeholder="请选择运行方式">
            <a-select-option v-for="runMode in runModeList" :key="runMode">{{ runMode }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item prop="whitelistDirectory" class="jpom-node-project-whitelist">
          <template slot="label">
            项目路径
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>白名单路径是指项目文件存放到服务中的文件夹</li>
                  <li>可以到节点管理中的【系统管理】=>【白名单配置】修改</li>
                  <li>项目文件夹是项目实际存放的目录名称</li>
                  <li>项目文件会存放到 <br />&nbsp;&nbsp;<b>项目白名单路径+项目文件夹</b></li>
                </ul>
              </template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-input-group compact>
            <a-select style="width: 50%" v-model="temp.whitelistDirectory" placeholder="请选择项目白名单路径">
              <a-select-option v-for="access in accessList" :key="access">
                <a-tooltip :title="access">{{ access }}</a-tooltip>
              </a-select-option>
            </a-select>
            <a-input style="width: 50%" v-model="temp.lib" placeholder="项目存储的文件夹" />
          </a-input-group>
          <template #extra>
            <!-- <span class="lib-exist" v-show="temp.libExist">{{ temp.libExistMsg }}</span> -->
          </template>
        </a-form-item>
        <!-- <a-form-item prop="lib">
          <template slot="label">
            项目文件夹
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul></ul>
              </template>
              <question-circle-filled />
            </a-tooltip>
          </template>
        </a-form-item> -->
        <a-form-item v-show="filePath !== ''" label="项目完整目录">
          <a-alert :message="filePath" type="success" />
        </a-form-item>
        <a-form-item v-show="temp.runMode === 'Dsl'" prop="dslContent">
          <template slot="label">
            DSL 内容
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <p>
                  以 yaml/yml 格式配置,scriptId
                  为项目路径下的脚本文件的相对路径或者脚本模版ID，可以到脚本模版编辑弹窗中查看 scriptId
                </p>
                <p>脚本里面支持的变量有：${PROJECT_ID}、${PROJECT_NAME}、${PROJECT_PATH}</p>
                <p>
                  <b>status</b> 流程执行完脚本后，输出的内容最后一行必须为：running:$pid
                  <b>$pid 为当前项目实际的进程ID</b>。如果输出最后一行不是预期格式项目状态将是未运行
                </p>
                <p>配置详情请参考配置示例</p>
              </template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-tabs>
            <a-tab-pane key="1" tab="DSL 配置">
              <div style="height: 40vh; overflow-y: scroll">
                <code-editor
                  v-model="temp.dslContent"
                  :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef' }"
                ></code-editor>
              </div>
            </a-tab-pane>
            <a-tab-pane key="2" tab="配置示例">
              <div style="height: 40vh; overflow-y: scroll">
                <code-editor
                  v-model="PROJECT_DSL_DEFATUL"
                  :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef', readOnly: true }"
                ></code-editor>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-form-item>
        <a-form-item v-show="noFileModes.includes(temp.runMode)">
          <template slot="label">
            日志目录
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>日志目录是指控制台日志存储目录</li>
                  <li>默认是在项目文件夹父级</li>
                  <li>可选择的列表和项目白名单目录是一致的，即相同配置</li>
                </ul>
              </template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-select v-model="temp.logPath" placeholder="请选择项目白名单路径">
            <a-select-option key="" value="">默认是在项目文件夹父级</a-select-option>
            <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="Main Class"
          prop="mainClass"
          v-show="javaModes.includes(temp.runMode) && temp.runMode !== 'Jar'"
        >
          <a-input v-model="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)" />
        </a-form-item>
        <a-form-item
          label="JavaExtDirsCp"
          prop="javaExtDirsCp"
          v-show="javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp'"
        >
          <a-input v-model="temp.javaExtDirsCp" placeholder="-Dext.dirs=xxx: -cp xx  填写【xxx:xx】" />
        </a-form-item>
        <a-form-item label="JVM 参数" prop="jvm" v-show="javaModes.includes(temp.runMode)">
          <a-textarea
            v-model="temp.jvm"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m"
          />
        </a-form-item>
        <a-form-item label="args 参数" prop="args" v-show="javaModes.includes(temp.runMode)">
          <a-textarea
            v-model="temp.args"
            :auto-size="{ minRows: 3, maxRows: 3 }"
            placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080"
          />
        </a-form-item>
        <div v-if="javaModes.includes(temp.runMode)">
          <!-- 副本信息 -->
          <!-- <a-row> </a-row> -->
          <a-form-item>
            <template slot="label">
              副本
              <a-tooltip v-show="temp.type !== 'edit'">
                <template slot="title"> 副本是指同一个项目在一个节点（服务器）中运行多份 </template>
                <question-circle-filled />
              </a-tooltip>
            </template>
            <a-collapse v-if="temp.javaCopyItemList && temp.javaCopyItemList.length">
              <a-collapse-panel v-for="replica in temp.javaCopyItemList" :key="replica.id">
                <template #header>
                  <a-row>
                    <a-col :span="4"> 副本 {{ replica.id }} </a-col>
                    <a-col :span="10">
                      <a-tooltip placement="topLeft" title="已经添加成功的副本需要在副本管理页面去删除">
                        <a-button
                          size="small"
                          :disabled="!replica.deleteAble"
                          type="danger"
                          @click="handleDeleteReplica(replica)"
                          >删除</a-button
                        >
                      </a-tooltip>
                    </a-col>
                  </a-row>
                </template>
                <a-form-item :label="`名称`" prop="replicaName">
                  <a-input v-model="replica.name" class="replica-area" placeholder="副本名称" />
                </a-form-item>
                <a-form-item :label="`JVM 参数`" prop="jvm">
                  <a-textarea
                    v-model="replica.jvm"
                    :auto-size="{ minRows: 3, maxRows: 3 }"
                    class="replica-area"
                    placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m"
                  />
                </a-form-item>
                <a-form-item :label="`args 参数`" prop="args">
                  <a-textarea
                    v-model="replica.args"
                    :auto-size="{ minRows: 3, maxRows: 3 }"
                    class="replica-area"
                    placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080"
                  />
                </a-form-item>
                <!-- <a-form-item> -->

                <!-- </a-form-item> -->
              </a-collapse-panel>
            </a-collapse>
            <!-- 添加副本 -->
            <a-form-item>
              <a-button size="small" type="primary" @click="handleAddReplica">添加副本</a-button>
            </a-form-item>
          </a-form-item>
        </div>
        <a-form-item prop="autoStart" v-show="noFileModes.includes(temp.runMode)">
          <template slot="label">
            自启动
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">插件端启动的时候检查项目状态，如果项目状态是未运行则尝试执行启动项目</template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-switch v-model="temp.autoStart" checked-children="开" un-checked-children="关" />
        </a-form-item>
        <a-form-item prop="token" v-show="noFileModes.includes(temp.runMode)" class="jpom-node-project-token">
          <template slot="label">
            WebHooks
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>项目启动,停止,重启都将请求对应的地址</li>
                  <li>传入参数有：projectId、projectName、type、copyId、result</li>
                  <li>type 的值有：stop、beforeStop、start、beforeRestart、fileChange</li>
                </ul>
              </template>
              <question-circle-filled />
            </a-tooltip>
          </template>
          <a-input v-model="temp.token" placeholder="项目启动,停止,重启都将请求对应的地址,非必填，GET请求" />
        </a-form-item>
        <a-form-item
          v-if="temp.log"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          label="日志路径"
          prop="log"
        >
          <a-alert :message="temp.log" type="success" />
        </a-form-item>
        <a-form-item
          v-if="temp.runCommand"
          v-show="temp.type === 'edit' && javaModes.includes(temp.runMode)"
          label="运行命令"
          prop="runCommand"
        >
          <a-alert :message="temp.runCommand || '无'" type="success" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 项目文件组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerFileVisible"
      @close="onFileClose"
    >
      <file
        v-if="drawerFileVisible"
        :nodeId="node.id"
        :projectId="temp.projectId"
        :runMode="temp.runMode"
        :absPath="(temp.whitelistDirectory || '') + (temp.lib || '')"
        @goConsole="goConsole"
        @goReadFile="goReadFile"
      />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerConsoleVisible"
      @close="onConsoleClose"
    >
      <console
        v-if="drawerConsoleVisible"
        :nodeId="node.id"
        :id="temp.id"
        :projectId="temp.projectId"
        :replica="replicaTemp"
        :copyId="replicaTemp ? replicaTemp.id : ''"
        @goFile="goFile"
      />
    </a-drawer>
    <!-- 项目跟踪文件组件 -->
    <a-drawer
      destroyOnClose
      :title="drawerTitle"
      placement="right"
      width="85vw"
      :visible="drawerReadFileVisible"
      @close="onReadFileClose"
    >
      <file-read
        v-if="drawerReadFileVisible"
        :nodeId="node.id"
        :readFilePath="temp.readFilePath"
        :id="temp.id"
        :projectId="temp.projectId"
        @goFile="goFile"
      />
    </a-drawer>

    <!-- 项目副本集组件 -->
    <!-- <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerReplicaVisible" @close="onReplicaClose">
      <replica v-if="drawerReplicaVisible" :node="node" :project="temp" />
    </a-drawer> -->
    <!-- 批量操作状态 -->
    <a-modal destroyOnClose v-model="batchVisible" :title="batchTitle" :footer="null" @cancel="batchClose">
      <a-list bordered :data-source="selectedRows">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-list-item-meta :description="item.email">
            <a slot="title"> {{ item.name }}</a>
          </a-list-item-meta>
          <div>{{ item.cause === undefined ? '未开始' : item.cause }}</div>
        </a-list-item>
      </a-list>
    </a-modal>
  </div>
</template>
<script>
import File from './project-file'
import Console from './project-console'
import FileRead from './project-file-read'
import CustomSelect from '@/components/customSelect'
// import Replica from "./project-replica";
import codeEditor from '@/components/codeEditor'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  PROJECT_DSL_DEFATUL,
  randomStr,
  parseTime
} from '@/utils/const'

import {
  deleteProject,
  editProject,
  getProjectAccessList,
  getProjectData,
  getProjectList,
  getRuningProjectCopyInfo,
  getRuningProjectInfo,
  javaModes,
  // nodeJudgeLibExist,
  noFileModes,
  restartProject,
  runModeList,
  startProject,
  stopProject,
  getProjectGroupAll,
  releaseOutgiving,
  importTemplate,
  exportData,
  importData
} from '@/api/node-project'

export default {
  props: {
    node: {
      type: Object
    }
  },
  components: {
    File,
    Console,
    CustomSelect,
    // Replica,
    codeEditor,
    FileRead
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      accessList: [],
      groupList: [],
      runModeList: runModeList,
      javaModes: javaModes,
      noFileModes: noFileModes,
      PROJECT_DSL_DEFATUL,
      list: [],
      temp: {},
      replicaTemp: null,
      editProjectVisible: false,
      drawerTitle: '',
      drawerFileVisible: false,
      drawerConsoleVisible: false,

      drawerReplicaVisible: false,
      drawerReadFileVisible: false,
      // addGroupvisible: false,
      // libExist: false,
      selectedRows: [],
      selectedRowKeys: [],
      checkRecord: '',
      batchVisible: false,
      batchTitle: '',
      columns: [
        {
          title: '',
          dataIndex: 'javaCopyItemList',
          align: 'center',
          width: '40px',
          scopedSlots: { customRender: 'copyIcon' }
        },
        {
          title: '项目名称',
          dataIndex: 'name',
          width: 150,
          sorter: true,
          ellipsis: true,
          scopedSlots: { customRender: 'name' }
        },
        {
          title: '项目分组',
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          scopedSlots: { customRender: 'group' }
        },
        {
          title: '项目路径',
          dataIndex: 'path',
          ellipsis: true,
          scopedSlots: { customRender: 'path' },
          width: 150
        },
        {
          title: '运行方式',
          dataIndex: 'runMode',
          sorter: true,
          width: '90px',
          ellipsis: true,
          align: 'center',
          scopedSlots: { customRender: 'runMode' }
        },

        // {
        //   title: "最后操作人",
        //   dataIndex: "modifyUser",
        //   width: 100,
        //   ellipsis: true,
        //   sorter: true,
        //   scopedSlots: { customRender: "modifyUser" },
        // },
        {
          title: '运行状态',
          dataIndex: 'status',
          width: 80,
          ellipsis: true,
          align: 'center',
          scopedSlots: { customRender: 'status' }
        },
        { title: '端口/PID', dataIndex: 'port', width: 100, ellipsis: true, scopedSlots: { customRender: 'port' } },
        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: '170px'
        },
        {
          title: '修改时间',
          sorter: true,
          dataIndex: 'modifyTimeMillis',
          width: '170px',
          ellipsis: true,
          customRender: (text) => parseTime(text)
        },
        {
          title: '操作',
          dataIndex: 'operation',
          scopedSlots: { customRender: 'operation' },
          fixed: 'right',
          align: 'center',
          width: '180px'
        }
      ],
      copyColumns: [
        { title: '编号', dataIndex: 'id', width: '80px', ellipsis: true, scopedSlots: { customRender: 'id' } },
        { title: '名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: { customRender: 'name' } },
        { title: '状态', dataIndex: 'status', width: 100, ellipsis: true, scopedSlots: { customRender: 'status' } },
        { title: '进程 ID', dataIndex: 'pid', width: 100, ellipsis: true, scopedSlots: { customRender: 'pid' } },
        { title: '端口号', dataIndex: 'port', width: 100, ellipsis: true, scopedSlots: { customRender: 'port' } },
        {
          title: '最后修改时间',
          dataIndex: 'modifyTime',
          width: '180px',
          ellipsis: true,
          scopedSlots: { customRender: 'modifyTime' }
        },
        { title: '操作', dataIndex: 'operation', scopedSlots: { customRender: 'operation' }, width: '120px' }
      ],
      rules: {
        id: [{ required: true, message: '请输入项目ID', trigger: 'blur' }],
        name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
        runMode: [{ required: true, message: '请选择项目运行方式', trigger: 'blur' }],
        whitelistDirectory: [{ required: true, message: '请选择项目白名单路径', trigger: 'blur' }],
        lib: [{ required: true, message: '请输入项目文件夹', trigger: 'blur' }]
      },
      expandedRowKeys: []
    }
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: this.onSelectChange,
        columnWidth: '40px',
        getCheckboxProps: this.getCheckboxProps,
        selectedRowKeys: this.selectedRowKeys
        // hideDefaultSelections: true,
      }
    }
  },
  watch: {},
  mounted() {
    this.loadData()
  },
  methods: {
    parseTime,
    randomStr,
    CHANGE_PAGE,
    // 页面引导
    introGuide() {
      this.$store.dispatch('tryOpenGuide', {
        key: 'project',
        options: {
          hidePrev: true,
          steps: [
            {
              title: '导航助手',
              element: document.querySelector('.jpom-node-project-whitelist'),
              intro: '这里是选择节点设置的白名单目录，白名单的设置在侧边栏菜单<b>系统管理</b>里面。'
            }
          ]
        }
      })
    },
    // 加载项目白名单列表
    loadAccesList() {
      getProjectAccessList(this.node.id).then((res) => {
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
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.nodeId = this.node.id
      getProjectList(this.listQuery).then((res1) => {
        if (res1.code === 200) {
          let resultList = res1.data.result
          this.listQuery.total = res1.data.total

          // TODO: 由于Ant Design Vue的bug，当表格中首行为disabled时，表格的全选按钮也无法选择。
          // 目前的解决方案是：把需要disabled的元素放到最后。
          // 如果运行模式是文件，则无需批量启动/重启/关闭
          let tempList = resultList.filter((item) => item.runMode !== 'File')
          let fileList = resultList.filter((item) => item.runMode === 'File')
          this.list = tempList.concat(fileList).map((item) => {
            //  javaCopyItemList
            let javaCopyItemList = null
            if (item.javaCopyItemList) {
              try {
                javaCopyItemList = JSON.parse(item.javaCopyItemList)
                javaCopyItemList = javaCopyItemList.length ? javaCopyItemList : null
              } catch (error) {
                //
              }
            }
            return { ...item, javaCopyItemList: javaCopyItemList }
          })
          // // 项目ID 字段更新
          // this.list = this.list.map((element) => {
          //   //element.dataId = element.id;
          //   //element.id = element.projectId;
          //   return element;
          // });

          let ids = tempList.map((item) => {
            return item.projectId
          })
          // 如果 ids 有数据就继续请求
          if (ids.length > 0) {
            const tempParams = {
              nodeId: this.node.id,
              ids: JSON.stringify(ids)
            }
            getRuningProjectInfo(tempParams, 'noTip').then((res2) => {
              if (res2.code === 200) {
                this.list = this.list.map((element) => {
                  if (res2.data[element.projectId]) {
                    element.port = res2.data[element.projectId].port
                    element.pid = res2.data[element.projectId].pid
                    element.pids = res2.data[element.projectId].pids
                    element.status = element.pid > 0
                    element.error = res2.data[element.projectId].error
                  }
                  return element
                })
                // this.list.forEach((element) => {});
              } else {
                // error
                this.list = this.list.map((element) => {
                  element.port = 0
                  element.pid = 0
                  element.status = false
                  element.error = res2.msg
                  return element
                })
              }
            })
            //
            this.expandedRowKeys = []
          }
          this.loadGroupList()
        }
        this.loading = false
      })
    },
    // 添加
    handleAdd() {
      this.temp = {
        type: 'add',
        logPath: '',
        javaCopyItemList: []
      }
      this.loadAccesList()

      this.editProjectVisible = true
      nextTick(() => {
        setTimeout(() => {
          this.introGuide()
        }, 500)
      })
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record)
      delete temp.id
      delete temp.createTimeMillis
      delete temp.outGivingProject
      this.temp = {
        ...temp,
        name: temp.name + '副本',
        id: temp.projectId + '_copy',
        javaCopyItemList: [],
        lib: temp.lib + '_copy'
      }

      this.loadAccesList()
      this.editProjectVisible = true
    },
    // 编辑
    handleEdit(record) {
      const params = {
        id: record.projectId,
        nodeId: this.node.id
      }
      this.loadAccesList()

      getProjectData(params).then((res) => {
        if (res.code === 200) {
          this.temp = {
            ...res.data,
            type: 'edit'
          }
          if (!this.temp.javaCopyItemList) {
            this.temp = {
              ...this.temp,
              javaCopyItemList: []
            }
          }
          this.editProjectVisible = true
        }
      })
    },
    // 添加副本
    handleAddReplica() {
      let repliccaId = randomStr()
      this.temp.javaCopyItemList.push({
        id: repliccaId,
        jvm: '',
        args: '',
        name: '',
        deleteAble: true
      })
    },
    // 移除副本
    handleDeleteReplica(reeplica) {
      const index = this.temp.javaCopyItemList.findIndex((element) => element.id === reeplica.id)
      const newList = this.temp.javaCopyItemList.slice()
      newList.splice(index, 1)
      this.temp.javaCopyItemList = newList
    },
    // 提交
    handleEditProjectOk() {
      if (this.temp.outGivingProject) {
        $notification.warning({
          message: '独立的项目分发请到分发管理中去修改'
        })
        return
      }
      // 检验表单
      this.$refs['editProjectForm'].validate((valid) => {
        if (!valid) {
          return false
        }
        const params = {
          ...this.temp,
          nodeId: this.node.id
        }
        // 额外参数
        const replicaParams = {}

        let javaCopyIds = this.temp.javaCopyItemList
          .map((element) => {
            //javaCopyIds += `${element.id},`;
            replicaParams[`jvm_${element.id}`] = element.jvm
            replicaParams[`args_${element.id}`] = element.args
            replicaParams[`name_${element.id}`] = element.name
            return element.id
          })
          .join(',')
        replicaParams['javaCopyIds'] = javaCopyIds
        editProject(params, replicaParams).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.$refs['editProjectForm'].resetFields()
            this.editProjectVisible = false
            this.loadData()
          }
        })
      })
    },
    // 文件管理
    handleFile(record) {
      this.checkRecord = record
      this.temp = Object.assign({}, record)
      this.drawerTitle = `文件管理(${this.temp.name})`
      this.drawerFileVisible = true
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false
    },
    // 控制台
    handleConsole(record) {
      this.checkRecord = record
      this.temp = Object.assign({}, record)
      this.drawerTitle = `控制台(${this.temp.name})`
      this.drawerConsoleVisible = true
      this.replicaTemp = null
    },
    // 副本控制台
    handleConsoleCopy(record, copyItem) {
      this.checkRecord = record
      this.temp = Object.assign({}, record)
      this.drawerTitle = `控制台(${this.temp.name})-${copyItem.id}`
      this.drawerConsoleVisible = true
      this.replicaTemp = copyItem
      console.log(record, copyItem)
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false
      this.loadData()
    },

    // // 副本集
    // handleReplica(record) {
    //   this.temp = Object.assign({}, record);
    //   this.drawerTitle = `副本集(${this.temp.name})`;
    //   this.drawerReplicaVisible = true;
    // },
    // 关闭副本集
    // onReplicaClose() {
    //   this.drawerReplicaVisible = false;
    // },
    // 删除
    handleDelete(record, thorough) {
      $confirm({
        title: '系统提示',
        content: thorough
          ? '真的要彻底删除项目么？彻底项目会自动删除项目相关文件奥(包含项目日志，日志备份，项目文件)'
          : '真的要删除项目么？删除项目不会删除项目相关文件奥,建议先清理项目相关文件再删除项目',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: record.projectId,
            thorough: thorough
          }
          deleteProject(params).then((res) => {
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
    onReadFileClose() {
      this.drawerReadFileVisible = false
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose()
      this.drawerReadFileVisible = true
      this.temp.readFilePath = (path + '/' + filename).replace(new RegExp('//', 'gm'), '/')
      this.drawerTitle = `跟踪文件(${filename})`
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose()
      this.handleConsole(this.checkRecord)
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose()
      this.onReadFileClose()
      this.handleFile(this.checkRecord)
    },
    // 获取复选框属性 判断是否可以勾选
    getCheckboxProps(record) {
      return {
        props: {
          disabled: record.runMode === 'File',
          name: record.name
        }
      }
    },
    //选中项目
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRows = selectedRows
      this.selectedRowKeys = selectedRowKeys
      //console.log(selectedRowKeys, selectedRows);
    },
    // onSelectAll() {

    // },

    batchClose() {
      this.batchVisible = false
      this.selectedRowKeys = []
      this.loadData()
    },
    //批量开始
    batchStart() {
      if (this.selectedRows.length <= 0) {
        $notification.warning({
          message: '请选中要启动的项目'
        })
        return
      }
      this.batchVisible = true
      this.batchTitle = '批量启动'
      this.batchStartInfo(1)
    },
    //批量启动详情
    batchStartInfo(count) {
      if (count > this.selectedRows.length) {
        return
      }
      let value = this.selectedRows[count - 1]
      value.cause = '启动中'
      count++
      if (value.runMode !== 'File') {
        const params = {
          nodeId: this.node.id,
          id: value.projectId
        }
        startProject(params)
          .then((data) => {
            value.cause = data.msg
            this.selectedRows = [...this.selectedRows]
            this.batchStartInfo(count)
          })
          .catch(() => {
            value.cause = '启动失败'
            this.selectedRows = [...this.selectedRows]

            this.batchStartInfo(count)
          })
      } else {
        value.cause = '跳过'
        this.selectedRows = [...this.selectedRows]

        this.batchStartInfo(count)
      }
    },
    //批量重启
    batchRestart() {
      if (this.selectedRows.length <= 0) {
        $notification.warning({
          message: '请选中要重启的项目'
        })
        return
      }
      this.batchVisible = true
      this.batchTitle = '批量重新启动'
      this.batchRestartInfo(1)
    },
    //批量重启详情
    batchRestartInfo(count) {
      if (count > this.selectedRows.length) {
        return
      }
      let value = this.selectedRows[count - 1]
      value.cause = '重新启动中'
      count++
      if (value.runMode !== 'File') {
        const params = {
          nodeId: this.node.id,
          id: value.projectId
        }
        restartProject(params)
          .then((data) => {
            value.cause = data.msg
            this.selectedRows = [...this.selectedRows]
            this.batchRestartInfo(count)
          })
          .catch(() => {
            value.cause = '重新启动失败'
            this.selectedRows = [...this.selectedRows]
            this.batchRestartInfo(count)
          })
      } else {
        value.cause = '跳过'
        this.selectedRows = [...this.selectedRows]
        this.batchRestartInfo(count)
      }
    },
    //批量关闭
    batchStop() {
      if (this.selectedRows.length <= 0) {
        $notification.warning({
          message: '请选中要关闭的项目'
        })
        return
      }
      this.batchVisible = true
      this.batchTitle = '批量关闭启动'
      this.batchStopInfo(1)
    },
    //批量关闭详情
    batchStopInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return
      }
      let value = this.selectedRows[count - 1]
      value.cause = '关闭中'
      count++
      if (value.runMode !== 'File') {
        const params = {
          nodeId: this.node.id,
          id: value.projectId
        }
        stopProject(params)
          .then((data) => {
            value.cause = data.msg
            this.selectedRows = [...this.selectedRows]

            this.batchStopInfo(count)
          })
          .catch(() => {
            value.cause = '关闭失败'
            this.selectedRows = [...this.selectedRows]
            this.batchStopInfo(count)
          })
      } else {
        value.cause = '跳过'
        this.selectedRows = [...this.selectedRows]
        this.batchStopInfo(count)
      }
    },

    // 折叠事件
    handleExpand(item, status) {
      //javaCopyItemList
      if (status) {
        this.expandedRowKeys.push(item.id)
        this.getRuningProjectCopyInfo(item)
      } else {
        this.expandedRowKeys = this.expandedRowKeys.filter((item2) => item2 !== item.id)
      }
    },
    // 获取副本信息
    getRuningProjectCopyInfo(project) {
      const ids = project.javaCopyItemList.map((item) => item.id)
      const tempParams = {
        nodeId: this.node.id,
        id: project.projectId,
        copyIds: JSON.stringify(ids)
      }

      getRuningProjectCopyInfo(tempParams).then((res) => {
        if (res.code === 200) {
          this.list = this.list.map((item) => {
            let javaCopyItemList = item.javaCopyItemList
            if (javaCopyItemList && item.projectId === project.projectId) {
              javaCopyItemList = javaCopyItemList.map((copyItem) => {
                if (res.data[copyItem.id]) {
                  // element.port = res.data[element.id].port;
                  // element.pid = res.data[element.id].pid;
                  // element.status = true;

                  return {
                    ...copyItem,
                    status: res.data[copyItem.id].pid > 0,
                    pid: res.data[copyItem.id].pid,
                    pids: res.data[copyItem.id].pids,
                    port: res.data[copyItem.id].port,
                    error: res.data[copyItem.id].error
                  }
                }
                return copyItem
              })
            }
            return { ...item, javaCopyItemList: javaCopyItemList }
          })

          this.list = this.list.map((element) => {
            return element
          })
        }
      })
    },
    // 删除
    handleDeleteCopy(project, record, thorough) {
      $confirm({
        title: '系统提示',
        content: thorough
          ? '真的要彻底删除项目副本么?彻底删除项目会自动删除副本相关文件奥(包含项目日志，日志备份)'
          : '真的要删除项目副本么？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: project.projectId,
            copyId: record.id,
            thorough: thorough
          }
          deleteProject(params).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })

              this.list = this.list.map((item) => {
                let javaCopyItemList = item.javaCopyItemList
                if (javaCopyItemList) {
                  javaCopyItemList = javaCopyItemList.filter((item2) => {
                    return item2.id !== record.id
                  })
                }
                return { ...item, javaCopyItemList: javaCopyItemList }
              })
              // this.loadData();
            }
          })
        }
      })
    },
    // 释放分发
    handleReleaseOutgiving(project) {
      const html =
        "<b style='font-size: 20px;'>确定要释放当前项目的分发功能吗？</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        '<li>请慎重操作，否则会产生冗余数据。</b></li>' +
        '<li>一般用于误操作后将本删除转为普通项目再删除项目</li>' +
        '<li>如果关联的分发还存在再重新编辑对应分发后当前项目会再次切换为分发项目！！！</li>' +
        ' </ul>'

      const h = this.$createElement
      $confirm({
        title: '危险操作！！！',
        content: h('div', null, [h('p', { domProps: { innerHTML: html } }, null)]),
        okButtonProps: { props: { type: 'danger', size: 'small' } },
        cancelButtonProps: { props: { type: 'primary' } },
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          const params = {
            nodeId: this.node.id,
            id: project.projectId
          }
          releaseOutgiving(params).then((res) => {
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
      window.open(
        importTemplate({
          nodeId: this.node.id
        }),
        '_blank'
      )
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), '_blank')
    },
    beforeUpload(file) {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('nodeId', this.node.id)
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
