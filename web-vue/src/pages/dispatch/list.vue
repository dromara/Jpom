<template>
  <div class="full-content">
    <!-- 表格 :scroll="{x: 740, y: tableHeight - 60}" scroll 跟 expandedRowRender 不兼容，没法同时使用不然会多出一行数据-->
    <a-table :columns="columns" :data-source="list" bordered rowKey="id" @expand="expand" :pagination="pagination" @change="changePage">
      <template slot="title">
        <a-space>
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%id%']" placeholder="id" />
          <a-input class="search-input-item" @pressEnter="loadData" v-model="listQuery['%name%']" placeholder="名称" />
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.outGivingProject"
            allowClear
            placeholder="分发类型"
            class="search-input-item"
          >
            <a-select-option :value="1">独立</a-select-option>
            <a-select-option :value="0">关联</a-select-option>
          </a-select>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="listQuery.status"
            allowClear
            placeholder="请选择状态"
            class="search-input-item"
          >
            <a-select-option v-for="(name, key) in statusMap" :key="key">{{ name }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleLink">添加关联项目</a-button>
          <a-button type="primary" @click="handleAdd">创建分发项目</a-button>
          <a-tooltip>
            <template slot="title">
              <div>节点分发是指,一个项目运行需要在多个节点(服务器)中运行,使用节点分发来统一管理这个项目(可以实现分布式项目管理功能)</div>

              <div>
                <ul>
                  <li>添加关联项目是指,将已经在节点中创建好的项目关联为节点分发项目来实现统一管理</li>
                  <li>创建分发项目是指,全新创建一个属于节点分发到项目,创建成功后项目信息将自动同步到对应的节点中,修改节点分发信息也自动同步到对应的节点中</li>
                </ul>
              </div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="status" slot-scope="text" placement="topLeft" :title="statusMap[text]">
        <span>{{ statusMap[text] }}</span>
      </a-tooltip>

      <a-tooltip slot="clearOld" slot-scope="text"> <a-switch size="small" checked-children="是" un-checked-children="否" disabled :checked="text" /></a-tooltip>

      <a-tooltip
        slot-scope="text"
        slot="afterOpt"
        :title="
          afterOptList.filter((item) => {
            return item.value === text;
          }).length &&
          afterOptList.filter((item) => {
            return item.value === text;
          })[0].title
        "
      >
        <span>{{
          afterOptList.filter((item) => {
            return item.value === text;
          }).length &&
          afterOptList.filter((item) => {
            return item.value === text;
          })[0].title
        }}</span>
      </a-tooltip>

      <template slot="outGivingProject" slot-scope="text">
        <span v-if="text">独立</span>
        <span v-else>关联</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleDispatch(record)">分发文件</a-button>
          <a-button size="small" type="primary" v-if="list_expanded[record.id]" @click="handleReload(record)">刷新</a-button>
          <template v-else>
            <a-button size="small" type="primary" v-if="record.outGivingProject" @click="handleEditDispatchProject(record)">编辑</a-button>
            <a-button size="small" type="primary" v-else @click="handleEditDispatch(record)">编辑</a-button>
          </template>
          <a-dropdown>
            <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
              更多
              <a-icon type="down" />
            </a>
            <a-menu slot="overlay">
              <a-menu-item>
                <a-button type="danger" v-if="!list_expanded[record.id]" @click="handleDelete(record)">{{ record.outGivingProject ? "删除" : "释放" }}</a-button>
              </a-menu-item>
              <a-menu-item>
                <a-button type="danger" @click="handleUnbind(record)">解绑</a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
        </a-space>
      </template>
      <!-- 嵌套表格 -->
      <a-table
        slot="expandedRowRender"
        slot-scope="text"
        :loading="childLoading"
        :columns="childColumns"
        size="middle"
        :bordered="false"
        :data-source="text.children"
        :pagination="false"
        :rowKey="(record, index) => record.nodeId + record.projectId + index"
      >
        <a-tooltip slot="nodeId" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ nodeNameMap[text] || text }}</span>
        </a-tooltip>
        <a-tooltip slot="projectName" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="outGivingResult" slot-scope="text, item" placement="topLeft" :title="text">
          <span>{{ text }} {{ item.errorMsg || "" }}</span>
        </a-tooltip>
        <template slot="projectStatus" slot-scope="text, item">
          <a-tooltip v-if="item.errorMsg" :title="item.errorMsg">
            <span>{{ item.errorMsg }}</span>
          </a-tooltip>
          <a-switch v-else :checked="text" size="small" checked-children="运行中" un-checked-children="未运行" />
        </template>

        <template slot="child-operation" slot-scope="text, record">
          <a-space>
            <a-button size="small" :disabled="!record.projectName" type="primary" @click="handleFile(record)">文件</a-button>
            <a-button size="small" :disabled="!record.projectName" type="primary" @click="handleConsole(record)">控制台</a-button>
          </a-space>
        </template>
      </a-table>
    </a-table>
    <!-- 添加/编辑关联项目 -->
    <a-modal v-model="linkDispatchVisible" width="600px" :title="temp.type === 'edit' ? '编辑关联项目' : '添加关联项目'" @ok="handleLinkDispatchOk" :maskClosable="false" @cancel="clearDispatchList">
      <a-form-model ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item prop="id">
          <template slot="label">
            分发 ID
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">分发 ID 等同于项目 ID</template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" />
        </a-form-model-item>
        <a-form-model-item label="分发名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="分发名称" />
        </a-form-model-item>

        <a-form-model-item label="分发节点" required>
          <a-list item-layout="horizontal" :data-source="dispatchList">
            <a-list-item slot="renderItem" slot-scope="item, index" v-if="item.status">
              <span>节点: </span>
              <a-select
                placeholder="请选择节点"
                notFoundContent="暂无节点信息"
                style="width: 140px"
                :defaultValue="item.index === '' ? undefined : item.index"
                @change="(value) => handleNodeListChange(value, index)"
                :disabled="item.index === '' ? false : !nodeNameList[item.index].nodeData || !nodeNameList[item.index].nodeData.openStatus"
              >
                <a-select-option :value="index" v-for="(nodeList, index) in nodeNameList" :key="nodeList.id" :disabled="!nodeList.nodeData || nodeList.nodeData.openStatus !== 1">
                  {{ nodeList.nodeData && nodeList.nodeData.name }}
                </a-select-option>
              </a-select>
              <span>项目: </span>
              <a-select
                style="width: 150px"
                :placeholder="dispatchList[index].placeholder"
                :defaultValue="item.projectId === '' ? undefined : item.projectId"
                notFoundContent="此节点暂无项目"
                @change="(value) => handleProjectChange(value, index)"
                :disabled="dispatchList[index].disabled"
              >
                <a-select-option
                  :value="project.projectId"
                  v-for="project in item.project"
                  :disabled="
                    dispatchList.filter((item, nowIndex) => {
                      return item.nodeId === project.nodeId && item.projectId === project.projectId && nowIndex !== index;
                    }).length > 0
                  "
                  :key="project.projectId"
                >
                  {{ project.name }}
                </a-select-option>
              </a-select>
              <a-button type="danger" @click="delDispachList(index)" icon="delete"></a-button>
            </a-list-item>
          </a-list>
          <a-button type="primary" @click="addDispachList">添加</a-button>
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.afterOpt"
            placeholder="请选择发布后操作"
          >
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item prop="intervalTime">
          <template slot="label">
            间隔时间
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                在执行多节点分发时候使用 顺序重启、完整顺序重启 时候需要保证项目能正常重启,并等待上一个项目启动完成才能关闭下一个项目,请根据自身项目启动时间来配置
                <li>一般建议 10 秒以上</li>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-number :min="0" v-model="temp.intervalTime" placeholder="分发间隔时间 （顺序重启、完整顺序重启）方式才生效" style="width: 100%" />
        </a-form-model-item>
        <a-form-model-item prop="clearOld">
          <template slot="label">
            清空发布
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title"> 清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-switch v-model="temp.clearOld" checked-children="是" un-checked-children="否" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 创建/编辑分发项目 -->
    <a-modal v-model="editDispatchVisible" width="60vw" :title="temp.type === 'edit' ? '编辑分发项目' : '创建分发项目'" @ok="handleEditDispatchOk" :maskClosable="false">
      <a-form-model ref="editDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item prop="id">
          <template slot="label">
            分发 ID
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">分发 ID 等同于项目 ID</template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" />
        </a-form-model-item>
        <a-form-model-item label="项目名称" prop="name">
          <a-input v-model="temp.name" :maxLength="50" placeholder="项目名称" />
        </a-form-model-item>

        <a-form-model-item prop="runMode">
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
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.runMode"
            placeholder="请选择运行方式"
          >
            <a-select-option v-for="runMode in runModeList" :key="runMode">{{ runMode }}</a-select-option>
          </a-select>
        </a-form-model-item>

        <a-form-model-item prop="whitelistDirectory" class="jpom-project-whitelist">
          <template slot="label">
            项目路径
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>白名单路径是指项目文件存放到服务中的文件夹</li>
                  <li>可以到【节点分发】=>【分发白名单配置】修改</li>
                  <li>项目文件夹是项目实际存放的目录名称</li>
                  <li>项目文件会存放到 <br />&nbsp;&nbsp;<b>项目白名单路径+项目文件夹</b></li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-group compact>
            <a-select
              :getPopupContainer="
                (triggerNode) => {
                  return triggerNode.parentNode || document.body;
                }
              "
              style="width: 50%"
              v-model="temp.whitelistDirectory"
              placeholder="请选择项目白名单路径"
            >
              <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
            </a-select>
            <a-input style="width: 50%" v-model="temp.lib" placeholder="项目存储的文件夹，jar 包存放的文件夹" />
          </a-input-group>
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
            DSL 内容
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <p>以 yaml/yml 格式配置,scriptId 为项目路径下的脚本文件的相对路径或者服务端脚本模版ID，可以到服务端脚本模版编辑弹窗中查看 scriptId</p>
                <p>脚本里面支持的变量有：${PROJECT_ID}、${PROJECT_NAME}、${PROJECT_PATH}</p>
                <p><b>status</b> 流程执行完脚本后，输出的内容最后一行必须为：running:$pid <b>$pid 为当前项目实际的进程ID</b>。如果输出最后一行不是预期格式项目状态将是未运行</p>
                <p>配置详情请参考配置示例</p>
              </template>
              <a-icon type="question-circle" theme="filled" />
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
            日志目录
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                <ul>
                  <li>日志目录是指控制台日志存储目录</li>
                  <li>默认是在项目文件夹父级</li>
                  <li>可选择的列表和项目白名单目录是一致的，即相同配置</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.logPath"
            placeholder="请选择日志目录"
          >
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
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.afterOpt"
            placeholder="请选择发布后操作"
          >
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item prop="intervalTime">
          <template slot="label">
            间隔时间
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title">
                在执行多节点分发时候使用 顺序重启、完整顺序重启 时候需要保证项目能正常重启,并等待上一个项目启动完成才能关闭下一个项目,请根据自身项目启动时间来配置
                <li>一般建议 10 秒以上</li>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-number :min="0" v-model="temp.intervalTime" placeholder="分发间隔时间 （顺序重启、完整顺序重启）方式才生效" style="width: 100%" />
        </a-form-model-item>
        <a-form-model-item prop="clearOld">
          <template slot="label">
            清空发布
            <a-tooltip v-show="temp.type !== 'edit'">
              <template slot="title"> 清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-switch v-model="temp.clearOld" checked-children="是" un-checked-children="否" />
        </a-form-model-item>
        <!-- 节点 -->
        <a-form-model-item label="分发节点" prop="nodeId">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.nodeIdList"
            mode="multiple"
            placeholder="请选择分发节点"
          >
            <a-select-option v-for="node in nodeList" :key="node.id">{{ `${node.name}` }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-collapse v-show="noFileModes.includes(temp.runMode)">
          <a-collapse-panel v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeNameMap[nodeId] || nodeId">
            <a-form-model-item label="JVM 参数" prop="jvm" v-show="javaModes.includes(temp.runMode)">
              <a-textarea v-model="temp[`${nodeId}_jvm`]" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
            </a-form-model-item>
            <a-form-model-item label="args 参数" prop="args" v-show="javaModes.includes(temp.runMode)">
              <a-textarea v-model="temp[`${nodeId}_args`]" :auto-size="{ minRows: 3, maxRows: 3 }" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
            </a-form-model-item>
            <a-form-model-item prop="autoStart" v-show="noFileModes.includes(temp.runMode)">
              <template slot="label">
                自启动
                <a-tooltip v-show="temp.type !== 'edit'">
                  <template slot="title">插件端启动的时候检查项目状态，如果项目状态是未运行则尝试执行启动项目</template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-switch v-model="temp[`${nodeId}_autoStart`]" checked-children="开" un-checked-children="关" />
            </a-form-model-item>
            <a-form-model-item prop="token" v-show="noFileModes.includes(temp.runMode)">
              <template slot="label">
                WebHooks
                <a-tooltip v-show="temp.type !== 'edit'">
                  <template slot="title">
                    <ul>
                      <li>项目启动,停止,重启都将请求对应的地址</li>
                      <li>传人参数有：projectId、projectName、type、copyId、result</li>
                      <li>type 的值有：stop、beforeStop、start、beforeRestart</li>
                    </ul>
                  </template>
                  <a-icon type="question-circle" theme="filled" />
                </a-tooltip>
              </template>
              <a-input v-model="temp[`${nodeId}_token`]" placeholder="项目启动,停止,重启都将请求对应的地址,非必填，GET请求" />
            </a-form-model-item>

            <div v-if="javaModes.includes(temp.runMode)">
              <a-form-model-item>
                <template slot="label">
                  副本
                  <a-tooltip v-show="temp.type !== 'edit'">
                    <template slot="title">
                      <ul>
                        <li>副本是指同一个项目在一个节点（服务器）中运行多份</li>
                      </ul>
                    </template>
                    <a-icon type="question-circle" theme="filled" />
                  </a-tooltip>
                </template>
                <!-- 副本信息 -->
                <a-collapse v-if="temp[`${nodeId}_javaCopyItemList`] && temp[`${nodeId}_javaCopyItemList`].length">
                  <a-collapse-panel v-for="replica in temp[`${nodeId}_javaCopyItemList`]" :key="replica.id">
                    <template #header>
                      <a-row>
                        <a-col :span="18"> 副本 {{ replica.name }} {{ replica.id }} </a-col>
                        <a-col :span="4">
                          <a-tooltip placement="topLeft" title="已经添加成功的副本需要在副本管理页面去删除">
                            <a-button size="small" :disabled="!replica.deleteAble" type="danger" @click.stop="handleDeleteReplica(nodeId, replica)">删除</a-button>
                          </a-tooltip>
                        </a-col>
                      </a-row>
                    </template>
                    <a-form-model-item :label="`名称`" prop="replicaName">
                      <a-input v-model="replica.name" class="replica-area" placeholder="副本名称" />
                    </a-form-model-item>
                    <a-form-model-item :label="`JVM 参数`" prop="jvm">
                      <a-textarea v-model="replica.jvm" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m" />
                    </a-form-model-item>
                    <a-form-model-item :label="`args 参数`" prop="args">
                      <a-textarea v-model="replica.args" :auto-size="{ minRows: 3, maxRows: 3 }" class="replica-area" placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080" />
                    </a-form-model-item>
                    <!-- <a-tooltip placement="topLeft" title="已经添加成功的副本需要在副本管理页面去删除" class="replica-btn-del">
                      <a-button :disabled="!replica.deleteAble" type="danger" @click="handleDeleteReplica(nodeId, replica)">删除</a-button>
                    </a-tooltip> -->
                  </a-collapse-panel>
                </a-collapse>

                <!-- 添加副本 -->
                <a-form-model-item>
                  <a-button type="primary" @click="handleAddReplica(nodeId)">添加副本</a-button>
                </a-form-model-item>
              </a-form-model-item>
            </div>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
    <!-- 分发项目 -->
    <a-modal v-model="dispatchVisible" width="600px" :title="'分发项目----' + temp.name" @ok="handleDispatchOk" :maskClosable="false">
      <a-form-model ref="dispatchForm" :rules="rules" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="方式" prop="type">
          <a-radio-group v-model="temp.type" name="type">
            <a-radio :value="'upload'">上传文件</a-radio>
            <a-radio :value="'download'">远程下载</a-radio>
          </a-radio-group>
        </a-form-model-item>
        <a-form-model-item label="选择分发文件" prop="clearOld" v-if="temp.type == 'upload'">
          <!-- accept=".zip,.tar,.gz,.bz2" -->
          <a-upload :file-list="fileList" :remove="handleRemove" :before-upload="beforeUpload">
            <a-button type="primary"><a-icon type="upload" />选择文件上传</a-button>
          </a-upload>
        </a-form-model-item>
        <a-form-model-item label="远程下载URL" prop="url" v-if="temp.type == 'download'">
          <a-input v-model="temp.url" placeholder="远程下载地址" />
        </a-form-model-item>
        <!-- <a-form-model-item label="是否为压缩包" v-if="temp.type == 'download'">
          <a-switch v-model="temp.unzip" checked-children="是" un-checked-children="否" v-decorator="['unzip', { valuePropName: 'checked' }]" />
        </a-form-model-item> -->
        <a-form-model-item prop="clearOld">
          <template slot="label">
            清空发布
            <a-tooltip>
              <template slot="title"> 清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件 </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-switch v-model="temp.clearOld" checked-children="是" un-checked-children="否" />
        </a-form-model-item>
        <a-form-model-item prop="unzip">
          <template slot="label">
            是否解压
            <a-tooltip>
              <template slot="title"> 如果上传的压缩文件是否自动解压 支持的压缩包类型有 tar.bz2, tar.gz, tar, bz2, zip, gz</template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-switch v-model="temp.autoUnzip" checked-children="是" un-checked-children="否" />
        </a-form-model-item>
        <a-form-model-item label="分发后操作" prop="afterOpt">
          <a-select
            :getPopupContainer="
              (triggerNode) => {
                return triggerNode.parentNode || document.body;
              }
            "
            v-model="temp.afterOpt"
            placeholder="请选择发布后操作"
          >
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!-- 项目文件组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
      <file v-if="drawerFileVisible" :id="temp.id" :nodeId="temp.nodeId" :projectId="temp.projectId" @goConsole="goConsole" />
    </a-drawer>
    <!-- 项目控制台组件 -->
    <a-drawer :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :id="temp.id" :nodeId="temp.nodeId" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>
  </div>
</template>
<script>
import File from "@/pages/node/node-layout/project/project-file";
import Console from "@/pages/node/node-layout/project/project-console";
import codeEditor from "@/components/codeEditor";
import {
  afterOptList,
  delDisPatchProject,
  editDispatch,
  editDispatchProject,
  getDishPatchList,
  getDispatchProject,
  getDispatchWhiteList,
  releaseDelDisPatch,
  remoteDownload,
  statusMap,
  unbindOutgiving,
  uploadDispatchFile,
} from "@/api/dispatch";
import {getNodeListAll, getProjectListAll} from "@/api/node";
import {getProjectData, javaModes, noFileModes, runModeList} from "@/api/node-project";
import {itemGroupBy, parseTime} from "@/utils/time";
import {CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, PROJECT_DSL_DEFATUL} from "@/utils/const";

export default {
  components: {
    File,
    Console,
    codeEditor,
  },
  data() {
    return {
      loading: false,
      childLoading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap: statusMap,
      javaModes: javaModes,
      noFileModes: noFileModes,
      PROJECT_DSL_DEFATUL,
      list: [],
      accessList: [],
      nodeList: [],
      projectList: [],
      afterOptList,
      targetKeys: [],
      // reqId: "",
      temp: {},
      fileList: [],
      runModeList: runModeList,
      list_expanded: {},
      linkDispatchVisible: false,
      editDispatchVisible: false,
      dispatchVisible: false,
      drawerTitle: "",
      drawerFileVisible: false,
      drawerConsoleVisible: false,
      nodeNameList: [],
      nodeNameMap: {},
      dispatchList: [],
      totalProjectNum: 0,
      columns: [
        { title: "分发 ID", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: "分发名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "类型", dataIndex: "outGivingProject", width: 90, ellipsis: true, scopedSlots: { customRender: "outGivingProject" } },
        { title: "分发后", dataIndex: "afterOpt", ellipsis: true, width: 100, scopedSlots: { customRender: "afterOpt" } },
        { title: "清空发布", dataIndex: "clearOld", align: "center", ellipsis: true, width: 100, scopedSlots: { customRender: "clearOld" } },
        { title: "间隔时间", dataIndex: "intervalTime", width: 90, ellipsis: true, scopedSlots: { customRender: "intervalTime" } },

        { title: "状态", dataIndex: "status", ellipsis: true, width: 110, scopedSlots: { customRender: "status" } },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          ellipsis: true,
          sorter: true,
          customRender: (text) => {
            return parseTime(text);
          },
          width: 170,
        },
        { title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, width: 210, align: "center" },
      ],
      childColumns: [
        { title: "节点名称", dataIndex: "nodeId", ellipsis: true, scopedSlots: { customRender: "nodeId" } },
        { title: "项目名称", dataIndex: "projectName", ellipsis: true, scopedSlots: { customRender: "projectName" } },
        { title: "项目状态", dataIndex: "projectStatus", width: 120, ellipsis: true, scopedSlots: { customRender: "projectStatus" } },
        { title: "分发状态", dataIndex: "outGivingStatus", width: 120 },
        { title: "分发结果", dataIndex: "outGivingResult", ellipsis: true, scopedSlots: { customRender: "outGivingResult" } },
        { title: "最后分发时间", dataIndex: "lastTime", width: 180, ellipsis: true, scopedSlots: { customRender: "lastTime" } },
        { title: "操作", dataIndex: "child-operation", scopedSlots: { customRender: "child-operation" }, width: 120, align: "center" },
      ],
      rules: {
        id: [{ required: true, message: "请输入项目ID", trigger: "blur" }],
        name: [{ required: true, message: "请输入项目名称", trigger: "blur" }],
        projectId: [{ required: true, message: "请选择项目", trigger: "blur" }],
        runMode: [{ required: true, message: "请选择项目运行方式", trigger: "blur" }],
        whitelistDirectory: [{ required: true, message: "请选择项目白名单路径", trigger: "blur" }],
        lib: [{ required: true, message: "请输入项目文件夹", trigger: "blur" }],
        afterOpt: [{ required: true, message: "请选择发布后操作", trigger: "blur" }],
        url: [{ required: true, message: "请输入远程地址", trigger: "blur" }],
      },
    };
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  watch: {},
  created() {
    this.loadData();
    this.loadNodeList();
  },
  methods: {
    // 页面引导
    introGuide() {
      this.$store.dispatch("tryOpenGuide", {
        key: "dispatch",
        options: {
          hidePrev: true,
          steps: [
            {
              title: "导航助手",
              element: document.querySelector(".jpom-project-whitelist"),
              intro: "项目白名单需要在侧边栏菜单<b>分发白名单</b>组件里面去设置",
            },
          ],
        },
      });
    },

    // 加载数据
    loadData(pointerEvent) {
      this.list = [];
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.loading = true;
      this.childLoading = false;
      getDishPatchList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 加载项目白名单列表
    loadAccesList() {
      getDispatchWhiteList().then((res) => {
        if (res.code === 200) {
          this.accessList = res.data.outGivingArray || [];
        }
      });
    },
    // 展开行
    expand(expanded, record) {
      this.list_expanded[record.id] = expanded;
      this.list_expanded = { ...this.list_expanded };
      if (expanded) {
        this.handleReload(record);
      }
    },
    // 关联分发
    handleLink() {
      this.$refs["linkDispatchForm"] && this.$refs["linkDispatchForm"].resetFields();
      this.temp = {
        type: "add",
        id: "",
        name: "",
        projectId: "",
      };
      this.loadNodeList(() => {
        this.loadProjectListAll();
      });
      this.linkDispatchVisible = true;
    },
    // 编辑分发
    handleEditDispatch(record) {
      this.$nextTick(() => {
        this.$refs["linkDispatchForm"] && this.$refs["linkDispatchForm"].resetFields();
      });
      this.loadNodeList(() => {
        this.loadProjectListAll(() => {
          //分发节点重新渲染
          this.temp = {};
          JSON.parse(record.outGivingNodeProjectList).forEach((ele, eleIndex) => {
            let index = "";
            let projects = [];
            this.nodeNameList.forEach((item, idx) => {
              if (item.id === ele.nodeId) {
                index = idx;
                projects = item.projects;
                item.openStatus = false;
              }
            });
            this.temp[`node_${ele.nodeId}_${eleIndex}`] = ele.projectId;
            this.dispatchList.push({
              nodeId: ele.nodeId,
              projectId: ele.projectId,
              index: index,
              project: projects,
              status: true,
            });
            // console.log(ele, eleIndex);
          });
          this.temp = { ...this.temp };

          this.temp = {
            ...this.temp,
            type: "edit",
            projectId: record.projectId,
            name: record.name,
            afterOpt: record.afterOpt,
            id: record.id,
            intervalTime: record.intervalTime,
            clearOld: record.clearOld,
          };
          // console.log(this.temp);
          this.linkDispatchVisible = true;
        });
      });
    },
    // 选择项目
    selectProject(value) {
      this.targetKeys = [];
      this.nodeList.forEach((node) => {
        node.disabled = true;
      });
      this.nodeProjectMap[value].forEach((nodeId) => {
        this.nodeList.forEach((node) => {
          if (node.key === nodeId) {
            node.disabled = false;
          }
        });
      });
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1;
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys;
    },
    // 提交关联项目
    handleLinkDispatchOk() {
      // 检验表单
      this.$refs["linkDispatchForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 校验分发节点数据
        if (this.dispatchList.length === 0) {
          this.$notification.error({ message: "请添加分发节点!" });
          return false;
        } else if (this.dispatchList.length < 2) {
          this.$notification.error({ message: "至少选择2个节点项目" });
          return false;
        }

        // 提交
        editDispatch(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.targetKeys = [];
            this.$refs["linkDispatchForm"].resetFields();
            this.linkDispatchVisible = false;
            this.clearDispatchList();
            this.loadData();
          } else {
            this.targetKeys = [];
          }
        });
      });
    },
    // 添加分发项目
    handleAdd() {
      this.loadNodeList(() => {
        this.temp = {
          type: "add",
          id: "",
          name: "",
          afterOpt: undefined,
          runMode: undefined,
          mainClass: "",
          javaExtDirsCp: "",
          whitelistDirectory: undefined,
          lib: "",
          nodeIdList: [],
          intervalTime: undefined,
          clearOld: false,
        };
        // 添加 javaCopyItemList
        this.nodeList.forEach((node) => {
          this.temp[`${node.id}_javaCopyItemList`] = [];
        });
        this.loadAccesList();

        this.editDispatchVisible = true;
        this.$nextTick(() => {
          this.$refs["editDispatchForm"].resetFields();
          setTimeout(() => {
            this.introGuide();
          }, 500);
        });
      });
    },
    // 编辑分发项目
    handleEditDispatchProject(record) {
      this.$nextTick(() => {
        this.$refs["editDispatchForm"] && this.$refs["editDispatchForm"].resetFields();
      });

      this.loadNodeList(() => {
        //
        this.temp = {};
        JSON.parse(record.outGivingNodeProjectList).forEach(async (ele) => {
          const params = {
            id: ele.projectId,
            nodeId: ele.nodeId,
          };
          const res = await getProjectData(params);
          if (res.code === 200) {
            // 如果 temp.id 不存在
            if (!this.temp.id) {
              this.temp = {
                id: res.data.id,
                name: res.data.name,
                type: "edit",
                afterOpt: record.afterOpt,
                runMode: res.data.runMode,
                mainClass: res.data.mainClass,
                javaExtDirsCp: res.data.javaExtDirsCp,
                whitelistDirectory: res.data.whitelistDirectory,
                lib: res.data.lib,
                logPath: res.data.logPath,
                dslContent: res.data.dslContent,
                nodeIdList: [],
                intervalTime: record.intervalTime,
                clearOld: record.clearOld,
              };
            }
            // 添加 nodeIdList
            this.temp.nodeIdList.push(ele.nodeId);
            // 添加 jvm token args
            this.temp[`${ele.nodeId}_jvm`] = res.data.jvm || "";
            this.temp[`${ele.nodeId}_token`] = res.data.token || "";
            this.temp[`${ele.nodeId}_args`] = res.data.args || "";
            this.temp[`${ele.nodeId}_autoStart`] = res.data.autoStart;

            // 添加 javaCopyItemList
            this.temp[`${ele.nodeId}_javaCopyItemList`] = res.data.javaCopyItemList || [];
            this.temp = { ...this.temp };
          }
        });

        // 加载其他数据
        this.loadAccesList();

        this.editDispatchVisible = true;
      });
    },
    // 添加副本
    handleAddReplica(nodeId) {
      const $chars = "ABCDEFGHJKMNPQRSTWXYZ0123456789";
      /****默认去掉了容易混淆的字符oOLl,9gq,Vv,Uu,I1****/
      const maxPos = $chars.length;
      let repliccaId = "";
      for (let i = 0; i < 2; i++) {
        repliccaId += $chars.charAt(Math.floor(Math.random() * maxPos));
      }
      this.temp[`${nodeId}_javaCopyItemList`].push({
        id: repliccaId,
        jvm: "",
        args: "",
        name: "",
        deleteAble: true,
      });
      this.temp = { ...this.temp };
    },
    // 移除副本
    handleDeleteReplica(nodeId, reeplica) {
      const index = this.temp[`${nodeId}_javaCopyItemList`].findIndex((element) => element.id === reeplica.id);
      const newList = this.temp[`${nodeId}_javaCopyItemList`].slice();
      newList.splice(index, 1);
      this.temp[`${nodeId}_javaCopyItemList`] = newList;
    },
    // 提交创建分发项目
    handleEditDispatchOk() {
      // 检验表单
      this.$refs["editDispatchForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        const tempData = Object.assign({}, this.temp);
        // 检查
        if (tempData.nodeIdList.length < 2) {
          this.$notification.warn({
            message: "请至少选择 2 个节点",
          });
          return false;
        }
        // 设置 reqId
        // this.temp.reqId = this.reqId;
        this.nodeList.forEach((item) => {
          //console.log(item);
          //delete this.temp[`add_${item.id}`];
          delete tempData[`${item.id}}_javaCopyIds`];
        });
        // 设置节点
        tempData.nodeIdList.forEach((key) => {
          // this.temp[`add_${key}`] = key;
          // 设置副本
          tempData[`${key}_javaCopyIds`] = "";
          const copyIds = [];
          tempData[`${key}_javaCopyItemList`]?.forEach((element) => {
            //this.temp[`${key}_javaCopyIds`] += `${element.id},`;
            copyIds.push(element.id);
            tempData[`${key}_jvm_${element.id}`] = element.jvm;
            tempData[`${key}_args_${element.id}`] = element.args;
            tempData[`${key}_name_${element.id}`] = element.name;
          });
          // 移除多余的后缀 ,
          tempData[`${key}_javaCopyIds`] = copyIds.join(",");
          //  this.temp[`${key}_javaCopyIds`].substring(0, this.temp[`${key}_javaCopyIds`].length - 1);
        });
        tempData.nodeIds = tempData.nodeIdList.join(",");
        delete tempData.nodeIdList;
        // 提交
        editDispatchProject(tempData).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editDispatchForm"].resetFields();
            this.editDispatchVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 刷新
    handleReload(record) {
      this.childLoading = true;
      getDispatchProject(record.id).then((res) => {
        if (res.code === 200) {
          record.children = res.data;
        }
        this.childLoading = false;
      });
    },
    // 处理分发
    handleDispatch(record) {
      this.temp = Object.assign({ type: "upload" }, record);
      this.dispatchVisible = true;
      this.$refs["dispatchForm"] && this.$refs["dispatchForm"].resetFields();
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
    // 提交分发文件
    handleDispatchOk() {
      // 检验表单
      this.$refs["dispatchForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // const key = this.temp.type;
        if (this.temp.type == "upload") {
          // 判断文件
          if (this.fileList.length === 0) {
            this.$notification.error({
              message: "请选择文件",
            });
            return false;
          }
          // 上传文件
          const formData = new FormData();
          //this.$message.loading({ content: "正在上传文件...", key, duration: 0 });
          formData.append("file", this.fileList[0]);
          formData.append("id", this.temp.id);
          formData.append("afterOpt", this.temp.afterOpt);
          formData.append("clearOld", this.temp.clearOld);
          formData.append("autoUnzip", this.temp.autoUnzip);
          uploadDispatchFile(formData).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // this.$message.success({ content: "上传成功,开始分发!", key });
              this.fileList = [];
              this.loadData();
              this.dispatchVisible = false;
              this.$refs["dispatchForm"].resetFields();
            }
          });
          return true;
        }
        if (this.temp.type == "download") {
          if (!this.temp.url) {
            this.$notification.error({
              message: "请填写远程URL",
            });
            return false;
          }
          // 远程下载
          // this.$message.loading({ content: "正在下载文件...", key, duration: 0 });
          remoteDownload(this.temp).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // this.$message.success({ content: "下载成功,开始分发!", key });

              this.loadData();
              this.dispatchVisible = false;
              this.$refs["dispatchForm"] && this.$refs["dispatchForm"].resetFields();
            } else {
              // this.$message.warn({ content: "下载失败", key });
            }
          });
          return true;
        }
      });
    },
    // 删除
    handleDelete(record) {
      if (record.outGivingProject) {
        this.$confirm({
          title: "系统提示",
          content: "真的要删除分发信息么？删除后节点下面的项目也都将删除",
          okText: "确认",
          cancelText: "取消",
          onOk: () => {
            // 删除
            delDisPatchProject(record.id).then((res) => {
              if (res.code === 200) {
                this.$notification.success({
                  message: res.msg,
                });
                this.loadData();
              }
            });
          },
        });
        return;
      }
      this.$confirm({
        title: "系统提示",
        content: "真的要释放分发信息么？释放之后节点下面的项目信息还会保留，如需删除还需要到节点管理中操作",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          releaseDelDisPatch(record.id).then((res) => {
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
      this.$confirm({
        title: "系统提示",
        content: "真的要解绑节点么？解绑会检查数据关联性,不会真实请求节点解绑,一般用于服务器无法连接且已经确定不再使用。如果误操作可能冗余数据",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          unbindOutgiving(record.id).then((res) => {
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
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `文件管理(${this.temp.projectId})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 控制台
    handleConsole(record) {
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.projectId})`;
      this.drawerConsoleVisible = true;
    },
    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose();
      this.handleConsole(this.temp);
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose();
      this.handleFile(this.temp);
    },
    loadProjectListAll(fn) {
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          this.totalProjectNum = res.data ? res.data.length : 0;
          this.nodeNameList = itemGroupBy(res.data, "nodeId", "id", "projects");
          this.nodeNameList = this.nodeNameList.map((item) => {
            item.nodeData = this.nodeList.filter((node) => node.id === item.id)[0];
            return item;
          });
          // .filter((item) => {
          //   return item.nodeData;
          // });
          fn && fn();
          // console.log(this.nodeNameList);
        }
      });
    },
    // 加载节点以及项目
    loadNodeList(fn) {
      this.nodeList = [];
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data;
          this.nodeList.map((item) => {
            this.nodeNameMap[item.id] = item.name;
          });
          fn && fn();
        }
      });
    },
    // 选择节点
    handleNodeListChange(value, index) {
      if (this.nodeNameList[value].projects.length === 0) {
        this.dispatchList[index].placeholder = "此节点暂无项目";
        this.dispatchList[index].disabled = true;
      } else {
        this.dispatchList[index].placeholder = "请选择项目";
        this.dispatchList[index].disabled = false;
      }
      //this.projectNameList = this.nodeNameList[value].projects;
      this.nodeNameList[value].openStatus = false;
      this.dispatchList[index].project = this.nodeNameList[value].projects;
      this.dispatchList[index].nodeId = this.nodeNameList[value].id;
      this.dispatchList[index].index = value;
    },
    // 选择项目
    handleProjectChange(value, index) {
      this.dispatchList[index].projectId = value;
      this.temp["node_" + this.dispatchList[index].nodeId + "_" + index] = value;
    },
    // 添加分发
    addDispachList() {
      if (this.dispatchList.length >= this.totalProjectNum) {
        this.$notification.error({
          message: "已无更多节点项目，请先创建项目",
        });
        return false;
      }
      this.dispatchList.push({ nodeId: "", projectId: "", index: "", project: [], status: true, placeholder: "请先选择节点", disabled: true });
    },
    // 删除分发
    delDispachList(value) {
      if (this.dispatchList[value].index !== "") {
        this.nodeNameList[this.dispatchList[value].index].openStatus = true;
      }
      delete this.temp[`node_${this.dispatchList[value].nodeId}_${this.dispatchList[value].index}`];
      this.dispatchList[value].status = false;
      this.dispatchList.splice(value, 1);
    },
    // 清理缓存
    clearDispatchList() {
      this.dispatchList = [];
      for (let node in this.nodeNameList) {
        this.nodeNameList[node].openStatus = true;
      }
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.loadData();
    },
  },
};
</script>
<style scoped>
.replica-area {
  width: 80%;
}
/* .replica-btn-del {
  position: absolute;
  right: 0;
  top: 74px;
} */
</style>
