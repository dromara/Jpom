<template>
  <div>
    <a-table
      :columns="columns"
      size="middle"
      :data-source="list"
      bordered
      rowKey="id"
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
          this.loadData()
        }
      "
      :scroll="{
        x: 'max-content'
      }"
    >
      <template v-slot:title>
        <a-space>
          <a-input
            class="search-input-item"
            @pressEnter="loadData"
            v-model:value="listQuery['%id%']"
            placeholder="分发id"
          />
          <a-input
            class="search-input-item"
            @pressEnter="loadData"
            v-model:value="listQuery['%name%']"
            placeholder="名称"
          />
          <a-select
            v-model:value="listQuery.group"
            allowClear
            placeholder="请选择分组"
            class="search-input-item"
            @change="loadData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.outGivingProject"
            allowClear
            placeholder="分发类型"
            class="search-input-item"
          >
            <a-select-option :value="1">独立</a-select-option>
            <a-select-option :value="0">关联</a-select-option>
          </a-select>
          <a-select v-model:value="listQuery.status" allowClear placeholder="请选择状态" class="search-input-item">
            <a-select-option v-for="(name, key) in statusMap" :key="key">{{ name }}</a-select-option>
          </a-select>
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleLink">添加关联</a-button>
          <a-button type="primary" @click="handleAdd">创建分发</a-button>
          <a-tooltip>
            <template v-slot:title>
              <div>
                节点分发是指,一个项目运行需要在多个节点(服务器)中运行,使用节点分发来统一管理这个项目(可以实现分布式项目管理功能)
              </div>

              <div>
                <ul>
                  <li>添加关联项目是指,将已经在节点中创建好的项目关联为节点分发项目来实现统一管理</li>
                  <li>
                    创建分发项目是指,全新创建一个属于节点分发到项目,创建成功后项目信息将自动同步到对应的节点中,修改节点分发信息也自动同步到对应的节点中
                  </li>
                </ul>
              </div>
            </template>
            <QuestionCircleOutlined />
          </a-tooltip>
          <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" />
        </a-space>
      </template>
      <template #bodyCell="{ column, text, record, index }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text || '' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'id'">
          <a-tooltip placement="topLeft" :title="text">
            <a-button
              size="small"
              style="padding: 0"
              type="link"
              v-if="record.outGivingProject"
              @click="handleEditDispatchProject(record)"
              >{{ text }}</a-button
            >
            <a-button size="small" style="padding: 0" type="link" v-else @click="handleEditDispatch(record)">{{
              text
            }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="text">
            <a-button size="small" style="padding: 0" type="link" @click="handleViewStatus(record)"
              ><FullscreenOutlined />{{ text }}</a-button
            >
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip placement="topLeft" :title="`${record.statusMsg || ''}`">
            <a-tag v-if="text === 2" color="green">{{ statusMap[text] || '未知' }}</a-tag>
            <a-tag v-else-if="text === 1" color="orange">{{ statusMap[text] || '未知' }}</a-tag>
            <a-tag v-else-if="text === 3 || text === 4" color="red">{{ statusMap[text] || '未知' }}</a-tag>
            <a-tag v-else>{{ statusMap[text] || '未知' }}</a-tag>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'clearOld'">
          <a-tooltip>
            <a-switch size="small" checked-children="是" un-checked-children="否" disabled :checked="text"
          /></a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'afterOpt'">
          <a-tooltip
            :title="
              afterOptList.filter((item) => {
                return item.value === text
              }).length &&
              afterOptList.filter((item) => {
                return item.value === text
              })[0].title
            "
          >
            <span>{{
              afterOptList.filter((item) => {
                return item.value === text
              }).length &&
              afterOptList.filter((item) => {
                return item.value === text
              })[0].title
            }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'outGivingProject'">
          <span v-if="text">独立</span>
          <span v-else>关联</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDispatch(record)">分发文件</a-button>

            <a-button
              size="small"
              type="primary"
              v-if="record.outGivingProject"
              @click="handleEditDispatchProject(record)"
              >编辑</a-button
            >
            <a-button size="small" type="primary" v-else @click="handleEditDispatch(record)">编辑</a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                更多
                <DownOutlined />
              </a>
              <template v-slot:overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.status !== 1"
                      @click="handleCancel(record)"
                      >取消分发</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleDelete(record)">{{
                      record.outGivingProject ? '删除' : '释放'
                    }}</a-button>
                  </a-menu-item>
                  <a-menu-item v-if="record.outGivingProject">
                    <a-button type="primary" danger size="small" @click="handleDelete(record, 'thorough')">
                      彻底删除</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleUnbind(record)">解绑</a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </a-table>
    <!-- 添加/编辑关联项目 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="linkDispatchVisible"
      width="900px"
      :title="temp.type === 'edit' ? '编辑关联项目' : '添加关联项目'"
      @ok="handleLinkDispatchOk"
      :maskClosable="false"
      @cancel="clearDispatchList"
    >
      <a-form ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item name="id">
          <template v-slot:label>
            <a-tooltip>
              分发 ID
              <template v-slot:title>分发 ID 等同于项目 ID</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
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
          <!-- <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" /> -->
        </a-form-item>

        <a-form-item label="分发名称" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.name" :maxLength="50" placeholder="分发名称" />
            </a-col>
            <a-col :span="4" style="text-align: right">分组名称：</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  :maxLength="50"
                  v-model:value="temp.group"
                  :data="groupList"
                  inputPlaceholder="添加分组"
                  selectPlaceholder="选择分组"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item label="分发节点" required>
          <a-list
            item-layout="horizontal"
            :data-source="dispatchList"
            :locale="{
              emptyText: '暂无数据,请先添加节点项目数据'
            }"
            :rowKey="
              (item) => {
                return item.nodeId + item.projectId + item.index
              }
            "
          >
            <template #renderItem="{ item, index }">
              <a-list-item>
                <a-space>
                  <div>节点:</div>
                  <a-select
                    placeholder="请选择节点"
                    notFoundContent="暂无节点信息"
                    style="width: 140px"
                    :value="item.nodeId ? item.nodeId : undefined"
                    @change="(nodeId) => handleNodeListChange(nodeId, index)"
                    :disabled="
                      item.nodeId || (nodeIdMap[item.nodeId] && nodeIdMap[item.nodeId].openStatus !== 1) ? true : false
                    "
                  >
                    <a-select-option
                      v-for="nodeList in nodeProjectsList"
                      :key="nodeList.id"
                      :disabled="nodeIdMap[nodeList.id].openStatus !== 1"
                    >
                      {{ nodeNameMap[nodeList.id] }}
                    </a-select-option>
                  </a-select>
                  <span>项目: </span>
                  <a-select
                    style="width: 300px"
                    :placeholder="item.placeholder"
                    :defaultValue="item.projectId ? item.projectId : undefined"
                    notFoundContent="此节点暂无项目"
                    @change="(projectId) => handleProjectChange(projectId, index)"
                    :disabled="dispatchList[index].disabled"
                  >
                    <a-select-option
                      :value="project.projectId"
                      v-for="project in nodeProjectsList.filter((nitem) => nitem.id == item.nodeId)[0] &&
                      nodeProjectsList.filter((nitem) => nitem.id == item.nodeId)[0].projects"
                      :disabled="
                        project.outGivingProject ||
                        dispatchList.filter((item, nowIndex) => {
                          return (
                            item.nodeId === project.nodeId && item.projectId === project.projectId && nowIndex !== index
                          )
                        }).length > 0
                      "
                      :key="project.projectId"
                    >
                      <a-tooltip :title="`${project.outGivingProject ? '【独立分发】' : ''} ${project.name}`">
                        {{ project.outGivingProject ? '【独立分发】' : '' }}
                        {{ project.name }}
                      </a-tooltip>
                    </a-select-option>
                  </a-select>
                  <a-button type="primary" danger @click="delDispachList(index)" size="small"
                    ><DeleteOutlined />
                  </a-button>
                </a-space>
              </a-list-item>
            </template>
          </a-list>
          <a-button type="primary" @click="addDispachList" size="small">添加</a-button>
        </a-form-item>
        <a-form-item label="分发后操作" name="afterOpt">
          <a-select v-model:value="temp.afterOpt" placeholder="请选择发布后操作">
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item name="intervalTime" v-if="temp.afterOpt === 2 || temp.afterOpt === 3">
          <template v-slot:label>
            <a-tooltip>
              间隔时间
              <template v-slot:title>
                在执行多节点分发时候使用 顺序重启、完整顺序重启
                时候需要保证项目能正常重启,并等待上一个项目启动完成才能关闭下一个项目,请根据自身项目启动时间来配置
                <li>一般建议 10 秒以上</li>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input-number
            :min="0"
            v-model:value="temp.intervalTime"
            placeholder="分发间隔时间 （顺序重启、完整顺序重启）方式才生效"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item name="secondaryDirectory" label="二级目录">
          <a-input v-model:value="temp.secondaryDirectory" placeholder="不填写则发布至项目的根目录" />
        </a-form-item>
        <a-form-item name="clearOld">
          <template v-slot:label>
            <a-tooltip>
              清空发布
              <template v-slot:title>
                清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-switch v-model:checked="temp.clearOld" checked-children="是" un-checked-children="否" />
            </a-col>

            <a-col :span="4" style="text-align: right">
              <a-tooltip v-if="temp.type !== 'edit'">
                <template v-slot:title>
                  发布前停止是指在发布文件到项目文件时先将项目关闭，再进行文件替换。避免 windows
                  环境下出现文件被占用的情况
                </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              发布前停止：
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch v-model:checked="temp.uploadCloseFirst" checked-children="是" un-checked-children="否"
              /></a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="webhook">
          <template v-slot:label>
            <a-tooltip>
              <template v-slot:title>
                <ul>
                  <li>分发过程请求对应的地址,开始分发,分发完成,分发失败,取消分发</li>
                  <li>传入参数有：outGivingId、outGivingName、status、statusMsg、executeTime</li>
                  <li>status 的值有：1:分发中、2：分发结束、3：已取消、4：分发失败</li>
                  <li>异步请求不能保证有序性</li>
                </ul>
              </template>
              WebHooks
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" placeholder="分发过程请求,非必填，GET请求" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 创建/编辑分发项目 -->
    <a-modal
      destroyOnClose
      :confirmLoading="confirmLoading"
      v-model:open="editDispatchVisible"
      width="60vw"
      :title="temp.type === 'edit' ? '编辑分发项目' : '创建分发项目'"
      @ok="handleEditDispatchOk"
      :maskClosable="false"
    >
      <a-spin tip="加载数据中" :spinning="editDispatchLoading">
        <a-form
          ref="editDispatchForm"
          :rules="rules"
          :model="temp"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-alert
            v-if="!nodeList || !nodeList.length"
            message="提醒"
            type="warning"
            show-icon
            style="margin-bottom: 10px"
          >
            <template #description>当前工作空间还没有逻辑节点不能创建节点分发奥</template>
          </a-alert>
          <a-form-item name="id">
            <template v-slot:label>
              <a-tooltip>
                分发 ID
                <template v-slot:title>分发 ID 等同于项目 ID</template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input
              :maxLength="50"
              v-model:value="temp.id"
              v-if="temp.type === 'edit'"
              :disabled="temp.type === 'edit'"
              placeholder="创建之后不能修改,分发 ID 等同于项目 ID"
            />
            <template v-else>
              <a-input-search
                :maxLength="50"
                v-model:value="temp.id"
                placeholder="创建之后不能修改,分发 ID 等同于项目 ID"
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
            <!-- <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改,分发 ID 等同于项目 ID" /> -->
          </a-form-item>
          <a-form-item label="分发名称" name="name">
            <a-row>
              <a-col :span="10">
                <a-input v-model:value="temp.name" :maxLength="50" placeholder="分发名称（项目名称）" />
              </a-col>
              <a-col :span="4" style="text-align: right">分组名称：</a-col>
              <a-col :span="10">
                <a-form-item-rest>
                  <custom-select
                    :maxLength="50"
                    v-model:value="temp.group"
                    :data="groupList"
                    inputPlaceholder="添加分组"
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

            <a-select v-model:value="temp.runMode" placeholder="请选择运行方式">
              <a-select-option v-for="item in runModeArray.filter((item) => item.onlyNode !== true)" :key="item.name">
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

          <a-form-item name="whitelistDirectory" class="jpom-project-whitelist">
            <template v-slot:label>
              <a-tooltip>
                项目路径
                <template v-slot:title>
                  <ul>
                    <li>授权路径是指项目文件存放到服务中的文件夹</li>
                    <li>可以到【节点分发】=>【分发授权配置】修改</li>
                    <li>项目文件夹是项目实际存放的目录名称</li>
                    <li>项目文件会存放到 <br />&nbsp;&nbsp;<b>项目授权路径+项目文件夹</b></li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input-group compact>
              <a-select style="width: 50%" v-model:value="temp.whitelistDirectory" placeholder="请选择项目授权路径">
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">
                    {{ access }}
                  </a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input style="width: 50%" v-model:value="temp.lib" placeholder="项目存储的文件夹，jar 包存放的文件夹"
              /></a-form-item-rest>
            </a-input-group>
            <template v-slot:help>
              需要提前为工作空间配置授权目录

              <a-button
                size="small"
                type="link"
                @click="
                  () => {
                    this.configDir = true
                  }
                "
              >
                <InfoCircleOutlined /> 配置目录
              </a-button>
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" label="项目完整目录">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
          <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
            <template v-slot:label>
              <a-tooltip>
                DSL 内容
                <template v-slot:title>
                  <p>
                    以 yaml/yml 格式配置,scriptId
                    为项目路径下的脚本文件的相对路径或者服务端脚本模版ID，可以到服务端脚本模版编辑弹窗中查看 scriptId
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
              scriptId建议使用服务端脚本分发到脚本：
              <a-button
                type="link"
                size="small"
                @click="
                  () => {
                    viewScriptVisible = true
                  }
                "
              >
                查看服务端脚本
              </a-button>
            </template>
            <a-form-item-rest>
              <a-tabs>
                <a-tab-pane key="1" tab="DSL 配置">
                  <div style="height: 40vh; overflow-y: scroll">
                    <code-editor
                      v-model:value="temp.dslContent"
                      :options="{ mode: 'yaml', tabSize: 2, theme: 'abcdef' }"
                    ></code-editor>
                  </div>
                </a-tab-pane>
                <a-tab-pane key="2" tab="配置示例">
                  <div style="height: 40vh; overflow-y: scroll">
                    <code-editor
                      v-model:value="PROJECT_DSL_DEFATUL"
                      :options="{
                        mode: 'yaml',
                        tabSize: 2,
                        theme: 'abcdef',
                        readOnly: true
                      }"
                    ></code-editor>
                  </div>
                </a-tab-pane> </a-tabs
            ></a-form-item-rest>
          </a-form-item>
          <a-form-item v-show="noFileModes.includes(temp.runMode)">
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
            <a-select v-model:value="temp.logPath" placeholder="请选择日志目录">
              <a-select-option key="" value="">默认是在插件端数据目录/${projectId}/${projectId}.log</a-select-option>
              <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            label="Main Class"
            name="mainClass"
            v-show="javaModes.includes(temp.runMode) && temp.runMode !== 'Jar'"
          >
            <a-input v-model:value="temp.mainClass" placeholder="程序运行的 main 类(jar 模式运行可以不填)" />
          </a-form-item>
          <a-form-item
            label="JavaExtDirsCp"
            name="javaExtDirsCp"
            v-show="javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp'"
          >
            <a-input v-model:value="temp.javaExtDirsCp" placeholder="-Dext.dirs=xxx: -cp xx  填写【xxx:xx】" />
          </a-form-item>
          <a-form-item label="分发后操作" name="afterOpt">
            <a-select v-model:value="temp.afterOpt" placeholder="请选择发布后操作">
              <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="intervalTime" v-if="temp.afterOpt === 2 || temp.afterOpt === 3">
            <template v-slot:label>
              <a-tooltip>
                间隔时间
                <template v-slot:title>
                  在执行多节点分发时候使用 顺序重启、完整顺序重启
                  时候需要保证项目能正常重启,并等待上一个项目启动完成才能关闭下一个项目,请根据自身项目启动时间来配置
                  <li>一般建议 10 秒以上</li>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input-number
              :min="0"
              v-model:value="temp.intervalTime"
              placeholder="分发间隔时间 （顺序重启、完整顺序重启）方式才生效"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item name="secondaryDirectory" label="二级目录">
            <a-input v-model:value="temp.secondaryDirectory" placeholder="不填写则发布至项目的根目录" />
          </a-form-item>
          <a-form-item name="clearOld">
            <template v-slot:label>
              <a-tooltip>
                清空发布
                <template v-slot:title>
                  清空发布是指在上传新文件前,会将项目文件夹目录里面的所有文件先删除后再保存新文件
                </template>
                <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-row>
              <a-col :span="4">
                <a-switch v-model:checked="temp.clearOld" checked-children="是" un-checked-children="否" />
              </a-col>
              <a-col :span="4" style="text-align: right">
                <a-tooltip v-if="temp.type !== 'edit'">
                  <template v-slot:title>
                    发布前停止是指在发布文件到项目文件时先将项目关闭，再进行文件替换。避免 windows
                    环境下出现文件被占用的情况
                  </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
                发布前停止：
              </a-col>
              <a-col :span="4">
                <a-form-item-rest>
                  <a-switch v-model:checked="temp.uploadCloseFirst" checked-children="是" un-checked-children="否" />
                </a-form-item-rest>
              </a-col>
            </a-row>
          </a-form-item>
          <a-form-item name="webhook">
            <template v-slot:label>
              <a-tooltip>
                WebHooks
                <template v-slot:title>
                  <ul>
                    <li>分发过程请求对应的地址,开始分发,分发完成,分发失败,取消分发</li>
                    <li>传入参数有：outGivingId、outGivingName、status、statusMsg、executeTime</li>
                    <li>status 的值有：1:分发中、2：分发结束、3：已取消、4：分发失败</li>
                    <li>异步请求不能保证有序性</li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="!temp.id" />
              </a-tooltip>
            </template>
            <a-input v-model:value="temp.webhook" placeholder="分发过程请求,非必填，GET请求" />
          </a-form-item>
          <!-- 节点 -->
          <a-form-item label="分发节点" name="nodeId">
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
              v-model:value="temp.nodeIdList"
              mode="multiple"
              placeholder="请选择分发节点"
            >
              <a-select-option v-for="node in nodeList" :key="node.id">{{ `${node.name}` }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-collapse v-show="noFileModes.includes(temp.runMode)">
            <a-collapse-panel v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeNameMap[nodeId] || nodeId">
              <a-form-item label="JVM 参数" name="jvm" v-show="javaModes.includes(temp.runMode)">
                <a-textarea
                  v-model:value="temp[`${nodeId}_jvm`]"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  placeholder="jvm参数,非必填.如：-Xms512m -Xmx512m"
                />
              </a-form-item>
              <a-form-item label="args 参数" name="args" v-show="javaModes.includes(temp.runMode)">
                <a-textarea
                  v-model:value="temp[`${nodeId}_args`]"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  placeholder="Main 函数 args 参数，非必填. 如：--server.port=8080"
                />
              </a-form-item>
              <a-form-item name="autoStart" v-show="noFileModes.includes(temp.runMode)">
                <template v-slot:label>
                  <a-tooltip>
                    自启动
                    <template v-slot:title
                      >插件端启动的时候检查项目状态，如果项目状态是未运行则尝试执行启动项目</template
                    >
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </a-tooltip>
                </template>
                <template #help
                  ><div>非服务器开机自启,如需开机自启建议配置<b>插件端开机自启</b>并开启此开关</div></template
                >
                <a-switch
                  v-model:checked="temp[`${nodeId}_autoStart`]"
                  checked-children="开"
                  un-checked-children="关"
                />
                插件端启动时自动检查项目如未启动将尝试启动
              </a-form-item>
              <a-form-item name="dslEnv" label="DSL环境变量" v-if="temp.runMode === 'Dsl'">
                <a-input
                  v-model:checked="temp[`${nodeId}_dslEnv`]"
                  placeholder="DSL环境变量,如：key1=values1&keyvalue2"
                />
              </a-form-item>
              <a-form-item name="token" v-show="noFileModes.includes(temp.runMode)">
                <template v-slot:label>
                  <a-tooltip>
                    <template v-slot:title>
                      <ul>
                        <li>项目启动,停止,重启都将请求对应的地址</li>
                        <li>传入参数有：projectId、projectName、type、result</li>
                        <li>type 的值有：stop、beforeStop、start、beforeRestart</li>
                      </ul>
                    </template>
                    WebHooks
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </a-tooltip>
                </template>
                <a-input
                  v-model:value="temp[`${nodeId}_token`]"
                  placeholder="项目启动,停止,重启,文件变动都将请求对应的地址,非必填，GET请求"
                />
              </a-form-item>
            </a-collapse-panel>
          </a-collapse>
        </a-form>
      </a-spin>
    </a-modal>
    <!-- 分发项目 -->
    <start-dispatch
      :data="temp"
      v-if="dispatchVisible"
      @cancel="
        () => {
          dispatchVisible = false
          loadData()
        }
      "
    />

    <!-- 分发状态 -->
    <Status
      v-if="drawerStatusVisible"
      :id="temp.id"
      :name="temp.name"
      @close="
        () => {
          this.drawerStatusVisible = false
        }
      "
    />
    <!-- 配置工作空间授权目录 -->
    <a-modal
      destroyOnClose
      v-model:open="configDir"
      :title="`配置授权目录`"
      :footer="null"
      width="50vw"
      :maskClosable="false"
      @cancel="
        () => {
          this.configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        @cancel="
          () => {
            this.configDir = false
            this.loadAccesList()
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 查看服务端脚本 -->
    <a-drawer
      destroyOnClose
      :title="`查看脚本`"
      placement="right"
      :open="viewScriptVisible"
      width="70vw"
      :zIndex="1009"
      @close="
        () => {
          this.viewScriptVisible = false
        }
      "
    >
      <scriptPage
        v-if="viewScriptVisible"
        choose="checkbox"
        @cancel="
          () => {
            this.viewScriptVisible = false
          }
        "
      ></scriptPage>
    </a-drawer>
  </div>
</template>

<script>
import Status from './status'
import codeEditor from '@/components/codeEditor'
import {
  afterOptList,
  delDisPatchProject,
  editDispatch,
  editDispatchProject,
  getDishPatchList,
  getDispatchWhiteList,
  releaseDelDisPatch,
  statusMap,
  unbindOutgiving,
  dispatchStatusMap,
  cancelOutgiving
} from '@/api/dispatch'
import { getNodeListAll, getProjectListAll } from '@/api/node'
import { getProjectData, javaModes, noFileModes, runModeArray, getProjectGroupAll } from '@/api/node-project'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  PROJECT_DSL_DEFATUL,
  randomStr,
  itemGroupBy,
  parseTime
} from '@/utils/const'
import scriptPage from '@/pages/script/script-list'
import CustomSelect from '@/components/customSelect'
import whiteList from '@/pages/dispatch/white-list'
import StartDispatch from './start'
export default {
  components: {
    codeEditor,
    CustomSelect,
    whiteList,
    Status,
    StartDispatch,
    scriptPage
  },
  data() {
    return {
      loading: false,
      confirmLoading: false,
      editDispatchLoading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      javaModes,
      noFileModes,
      runModeArray,
      dispatchStatusMap,
      PROJECT_DSL_DEFATUL,
      list: [],
      accessList: [],
      nodeList: [],
      projectList: [],
      groupList: [],
      afterOptList,
      targetKeys: [],
      // reqId: "",
      temp: {},
      configDir: false,
      viewScriptVisible: false,
      linkDispatchVisible: false,
      editDispatchVisible: false,
      dispatchVisible: false,

      nodeProjectsList: [],
      nodeNameMap: {},
      nodeIdMap: {},
      dispatchList: [],
      totalProjectNum: 0,
      columns: [
        {
          title: '分发 ID',
          dataIndex: 'id',
          ellipsis: true,
          width: 110
        },
        {
          title: '分发名称',
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: '项目分组',
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: '分发类型',
          dataIndex: 'outGivingProject',
          width: '90px',
          ellipsis: true
        },
        {
          title: '分发后',
          dataIndex: 'afterOpt',
          ellipsis: true,
          width: '150px'
        },
        {
          title: '清空发布',
          dataIndex: 'clearOld',
          align: 'center',
          ellipsis: true,
          width: '100px'
        },
        {
          title: '间隔时间',
          dataIndex: 'intervalTime',
          width: 90,
          ellipsis: true
        },

        {
          title: '状态',
          dataIndex: 'status',
          ellipsis: true,
          width: 110
        },
        {
          title: '二级目录',
          dataIndex: 'secondaryDirectory',
          ellipsis: true,
          width: 110,
          tooltip: true
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
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: '操作',
          dataIndex: 'operation',

          fixed: 'right',
          width: '200px',
          align: 'center'
        }
      ],

      rules: {
        id: [{ required: true, message: '请输入项目ID', trigger: 'blur' }],
        name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
        projectId: [{ required: true, message: '请选择项目', trigger: 'blur' }],
        runMode: [{ required: true, message: '请选择项目运行方式', trigger: 'blur' }],
        whitelistDirectory: [{ required: true, message: '请选择项目授权路径', trigger: 'blur' }],
        lib: [{ required: true, message: '请输入项目文件夹', trigger: 'blur' }],
        afterOpt: [{ required: true, message: '请选择发布后操作', trigger: 'blur' }]
      },
      countdownTime: Date.now(),
      refreshInterval: 5,

      viewDispatchManager: false,

      drawerStatusVisible: false
    }
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    },
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  watch: {},
  created() {
    this.loadData()
    this.loadNodeList()
    this.loadGroupList()
  },
  methods: {
    randomStr,
    CHANGE_PAGE,

    // 静默
    silenceLoadData() {
      if (this.$attrs.routerUrl !== this.$route.path) {
        // 重新计算倒计时
        this.countdownTime = Date.now() + this.refreshInterval * 1000
        return
      }
      getDishPatchList(this.listQuery, false).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //

          // 重新计算倒计时
          this.countdownTime = Date.now() + this.refreshInterval * 1000
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      return new Promise((resolve) => {
        this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
        this.loading = true

        // = false;
        getDishPatchList(this.listQuery).then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
            // 重新计算倒计时
            this.countdownTime = Date.now() + this.refreshInterval * 1000
          }
          this.loading = false
          resolve()
        })
      })
    },
    // 加载项目授权列表
    loadAccesList() {
      getDispatchWhiteList().then((res) => {
        if (res.code === 200) {
          this.accessList = res.data.outGivingArray || []
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

    // 关联分发
    handleLink() {
      this.$refs['linkDispatchForm'] && this.$refs['linkDispatchForm'].resetFields()
      this.temp = {
        type: 'add',
        id: '',
        name: '',
        projectId: ''
      }
      this.loadNodeList(() => {
        this.loadProjectListAll()
      })
      this.linkDispatchVisible = true
    },
    // 编辑分发
    handleEditDispatch(record) {
      this.$nextTick(() => {
        this.$refs['linkDispatchForm'] && this.$refs['linkDispatchForm'].resetFields()
      })
      this.loadNodeList(() => {
        this.loadProjectListAll(() => {
          //分发节点重新渲染
          this.temp = {}
          this.dispatchList = []
          JSON.parse(record.outGivingNodeProjectList).forEach((ele) => {
            this.dispatchList.push({
              nodeId: ele.nodeId,
              projectId: ele.projectId,
              index: this.dispatchList.length
              // project: this.nodeProjectsList.filter((item) => item.id === ele.nodeId)[0].projects,
            })
            // console.log(this.dispatchList);
          })
          this.temp = { ...this.temp }

          this.temp = {
            ...this.temp,
            type: 'edit',
            projectId: record.projectId,
            name: record.name,
            afterOpt: record.afterOpt,
            id: record.id,
            intervalTime: record.intervalTime,
            clearOld: record.clearOld,
            secondaryDirectory: record.secondaryDirectory || '',
            uploadCloseFirst: record.uploadCloseFirst,
            group: record.group,
            webhook: record.webhook
          }
          // console.log(this.temp);
          this.linkDispatchVisible = true
        })
      })
    },
    // 选择项目
    selectProject(value) {
      this.targetKeys = []
      this.nodeList.forEach((node) => {
        node.disabled = true
      })
      this.nodeProjectMap[value].forEach((nodeId) => {
        this.nodeList.forEach((node) => {
          if (node.key === nodeId) {
            node.disabled = false
          }
        })
      })
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys
    },
    // 提交关联项目
    handleLinkDispatchOk() {
      // 检验表单
      this.$refs['linkDispatchForm'].validate().then(() => {
        // 校验分发节点数据
        if (this.dispatchList.length < 1) {
          $notificationr({ message: '至少选择1个节点项目' })
          return false
        }
        this.dispatchList.forEach((item, index) => {
          this.temp['node_' + item.nodeId + '_' + index] = item.projectId
        })
        this.confirmLoading = true
        // 提交
        editDispatch(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.targetKeys = []
              this.$refs['linkDispatchForm'].resetFields()
              this.linkDispatchVisible = false
              this.clearDispatchList()
              this.loadData()
            } else {
              this.targetKeys = []
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 添加分发项目
    handleAdd() {
      this.loadNodeList(() => {
        this.temp = {
          type: 'add',
          id: '',
          name: '',
          afterOpt: undefined,
          runMode: undefined,
          mainClass: '',
          javaExtDirsCp: '',
          whitelistDirectory: undefined,
          lib: '',
          nodeIdList: [],
          intervalTime: undefined,
          clearOld: false
        }

        this.loadAccesList()
        this.loadGroupList()

        this.editDispatchVisible = true

        this.$refs['editDispatchForm']?.resetFields()
      })
    },
    // 编辑分发项目
    handleEditDispatchProject(record) {
      this.$nextTick(() => {
        this.$refs['editDispatchForm'] && this.$refs['editDispatchForm'].resetFields()
      })
      this.editDispatchLoading = true
      this.editDispatchVisible = true
      this.loadNodeList(() => {
        //
        this.temp = {}
        JSON.parse(record.outGivingNodeProjectList).forEach(async (ele) => {
          const params = {
            id: ele.projectId,
            nodeId: ele.nodeId
          }
          const res = await getProjectData(params)
          if (res.code === 200) {
            // 如果 temp.id 不存在
            if (!this.temp.id) {
              this.temp = {
                id: res.data.id,
                name: res.data.name,
                type: 'edit',
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
                secondaryDirectory: record.secondaryDirectory,
                uploadCloseFirst: record.uploadCloseFirst,
                group: record.group,
                webhook: record.webhook
              }
            }
            // 添加 nodeIdList
            this.temp.nodeIdList.push(ele.nodeId)
            // 添加 jvm token args
            this.temp[`${ele.nodeId}_jvm`] = res.data.jvm || ''
            this.temp[`${ele.nodeId}_token`] = res.data.token || ''
            this.temp[`${ele.nodeId}_args`] = res.data.args || ''
            this.temp[`${ele.nodeId}_autoStart`] = res.data.autoStart
            this.temp[`${ele.nodeId}_dslEnv`] = res.data.dslEnv || ''

            this.temp = { ...this.temp }
          }
        })

        // 加载其他数据
        this.loadAccesList()
        this.loadGroupList()

        this.editDispatchLoading = false
      })
    },

    // 提交创建分发项目
    handleEditDispatchOk() {
      // 检验表单
      this.$refs['editDispatchForm'].validate().then(() => {
        const tempData = Object.assign({}, this.temp)
        // 检查
        if (tempData.nodeIdList.length < 1) {
          $notification.warn({
            message: '请至少选择 1 个节点'
          })
          return false
        }
        // 设置 reqId
        // this.temp.reqId = this.reqId;

        tempData.nodeIds = tempData.nodeIdList.join(',')
        delete tempData.nodeIdList
        this.confirmLoading = true
        // 提交
        editDispatchProject(tempData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editDispatchForm'].resetFields()
              this.editDispatchVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },

    // 处理分发
    handleDispatch(record) {
      this.temp = Object.assign({ type: 'upload' }, record)
      this.dispatchVisible = true
    },

    // 删除
    handleDelete(record, thorough) {
      const that = this
      if (record.outGivingProject) {
        this.$confirm({
          title: '系统提示',
          zIndex: 1009,
          content: thorough
            ? '真的要彻底删除分发信息么？删除后节点下面的项目也都将彻底删除，彻底项目会自动删除项目相关文件奥(包含项目日志，日志备份，项目文件)'
            : '真的要删除分发信息么？删除后节点下面的项目也都将删除',
          okText: '确认',
          cancelText: '取消',
          async onOk() {
            return await new Promise((resolve, reject) => {
              // 删除
              delDisPatchProject({ id: record.id, thorough: thorough })
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
        return
      }
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要释放分发信息么？释放之后节点下面的项目信息还会保留，如需删除项目还需要到节点管理中操作',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            releaseDelDisPatch(record.id)
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
    // 解绑
    handleUnbind(record) {
      const html =
        "<b style='font-size: 20px;'>真的要解绑当前节点分发么？</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        '<li>解绑会检查数据关联性,不会真实请求节点删除项目信息</b></li>' +
        '<li>一般用于服务器无法连接且已经确定不再使用</li>' +
        '<li>如果误操作会产生冗余数据！！！</li>' +
        ' </ul>'
      const that = this
      this.$confirm({
        title: '危险操作！！！',
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', size: 'small', danger: true },
        cancelButtonProps: { type: 'primary' },
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            unbindOutgiving(record.id)
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

    loadProjectListAll(fn) {
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          this.totalProjectNum = res.data ? res.data.length : 0
          this.nodeProjectsList = itemGroupBy(res.data, 'nodeId', 'id', 'projects')
          // console.log(this.nodeProjectsList);
          fn && fn()
        }
      })
    },
    // 加载节点以及项目
    loadNodeList(fn) {
      this.nodeList = []
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
          this.nodeList.map((item) => {
            this.nodeNameMap[item.id] = item.name
            this.nodeIdMap[item.id] = item
          })
          fn && fn()
        }
      })
    },
    // 选择节点
    handleNodeListChange(nodeId, index) {
      const nodeData = this.nodeProjectsList.filter((item) => item.id === nodeId)[0]
      if (nodeData.projects.length === 0) {
        this.dispatchList[index].placeholder = '此节点暂无项目'
        this.dispatchList[index].disabled = true
      } else {
        this.dispatchList[index].placeholder = '请选择项目'
        this.dispatchList[index].disabled = false
      }
      this.dispatchList[index].nodeId = nodeId
    },
    // 选择项目
    handleProjectChange(value, index) {
      this.dispatchList[index].projectId = value
    },
    // 添加分发
    addDispachList() {
      if (this.dispatchList.length >= this.totalProjectNum) {
        $notification.error({
          message: '已无更多节点项目，请先创建项目'
        })
        return false
      }
      this.dispatchList.push({
        index: this.dispatchList.length,
        placeholder: '请先选择节点',
        disabled: true
      })
    },
    // 删除分发
    delDispachList(value) {
      this.dispatchList.splice(value, 1)
    },
    // 清理缓存
    clearDispatchList() {
      this.dispatchList = []
    },

    // 取消
    handleCancel(record) {
      const that = this
      this.$confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的取消当前分发吗？',
        okText: '确认',
        cancelText: '取消',
        async onOk() {
          return await new Promise((resolve, reject) => {
            // 删除
            cancelOutgiving({ id: record.id }).then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                that.loadData()
                that.silenceLoadData()
              }
              resolve()
            })
          }).catch(reject)
        }
      })
    },

    // 查看项目状态
    handleViewStatus(item) {
      this.drawerStatusVisible = true
      this.temp = { ...item }
    }
  }
}
</script>

<style scoped>
:deep(.ant-statistic div) {
  display: inline-block;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
}
</style>
