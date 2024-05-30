<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      :active-page="activePage"
      table-name="dispatch"
      :columns="columns"
      size="middle"
      :data-source="list"
      bordered
      row-key="id"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @refresh="loadData"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%id%']"
            class="search-input-item"
            :placeholder="$tl('p.distributionID')"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$tl('p.name')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.group"
            allow-clear
            :placeholder="$tl('p.selectGrouping')"
            class="search-input-item"
            @change="loadData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.outGivingProject"
            allow-clear
            :placeholder="$tl('c.distributionType')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $tl('c.independent') }}</a-select-option>
            <a-select-option :value="0">{{ $tl('c.related') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$tl('p.selectStatus')"
            class="search-input-item"
          >
            <a-select-option v-for="(name, key) in statusMap" :key="key">{{ name }}</a-select-option>
          </a-select>
          <a-tooltip :title="$tl('p.quickBackToFirstPage')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleLink">{{ $tl('p.addRelated') }}</a-button>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.addDistribution') }}</a-button>

          <!-- <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" /> -->
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $tl('p.nodeDistributionDescription') }}</div>

            <div>
              <ul>
                <li>{{ $tl('p.addRelatedProjectDescription') }}</li>
                <li>{{ $tl('p.createDistributionProjectDescription') }}</li>
              </ul>
            </div>
          </template>
          <QuestionCircleOutlined />
        </a-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text || '' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'id'">
          <a-tooltip placement="topLeft" :title="text">
            <a-button
              v-if="record.outGivingProject"
              size="small"
              style="padding: 0"
              type="link"
              @click="handleEditDispatchProject(record)"
              >{{ text }}</a-button
            >
            <a-button v-else size="small" style="padding: 0" type="link" @click="handleEditDispatch(record)">{{
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
            <a-tag v-if="text === 2" color="green">{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
            <a-tag v-else-if="text === 1" color="orange">{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
            <a-tag v-else-if="text === 3 || text === 4" color="red">{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
            <a-tag v-else>{{ statusMap[text] || $tl('c.unknown') }}</a-tag>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'clearOld'">
          <a-tooltip>
            <a-switch
              size="small"
              :checked-children="$tl('c.yes')"
              :un-checked-children="$tl('c.no')"
              disabled
              :checked="text"
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
          <span v-if="text">{{ $tl('c.independent') }}</span>
          <span v-else>{{ $tl('c.related') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDispatch(record)">
              {{ $tl('p.distributionFiles') }}
            </a-button>

            <a-button
              v-if="record.outGivingProject"
              size="small"
              type="primary"
              @click="handleEditDispatchProject(record)"
              >{{ $tl('c.edit') }}</a-button
            >
            <a-button v-else size="small" type="primary" @click="handleEditDispatch(record)">
              {{ $tl('c.edit') }}
            </a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $tl('p.more') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button
                      type="primary"
                      danger
                      size="small"
                      :disabled="record.status !== 1"
                      @click="handleCancel(record)"
                    >
                      {{ $tl('p.cancelDistribution') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleDelete(record, '')">
                      {{ record.outGivingProject ? $tl('p.delete') : $tl('p.release') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item v-if="record.outGivingProject">
                    <a-button type="primary" danger size="small" @click="handleDelete(record, 'thorough')">
                      {{ $tl('p.deleteCompletely') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleUnbind(record)">
                      {{ $tl('p.unbind') }}
                    </a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </CustomTable>
    <!-- 新增/编辑关联项目 -->
    <a-modal
      v-model:open="linkDispatchVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="900px"
      :title="temp.type === 'edit' ? $tl('p.editRelatedProject') : $tl('p.addRelatedProject')"
      :mask-closable="false"
      @ok="handleLinkDispatchOk"
      @cancel="clearDispatchList"
    >
      <a-form ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item name="id">
          <template #label>
            <a-tooltip>
              {{ $tl('c.distributionID') }}
              <template #title>{{ $tl('c.distributionIDEqualsProjectID') }}</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$tl('c.cannotModifyAfterCreation')"
          />
          <template v-else>
            <a-input-search
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$tl('c.cannotModifyAfterCreation')"
              @search="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary">
                  {{ $tl('c.randomlyGenerated') }}
                </a-button>
              </template>
            </a-input-search>
          </template>
          <!-- <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" /> -->
        </a-form-item>

        <a-form-item :label="$tl('c.distributionName')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('c.distributionName')" />
            </a-col>
            <a-col :span="4" style="text-align: right">{{ $tl('c.groupingName') }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$tl('c.addNewGrouping')"
                  :select-placeholder="$tl('c.selectGrouping')"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item :label="$tl('c.distributionNode')" required>
          <a-list
            item-layout="horizontal"
            :data-source="dispatchList"
            :locale="{
              emptyText: $tl('p.noData')
            }"
            :row-key="
              (item) => {
                return item.nodeId + item.projectId + item.index
              }
            "
          >
            <template #renderItem="{ item, index }">
              <a-list-item>
                <a-space>
                  <div>{{ $tl('p.node') }}</div>
                  <a-select
                    :placeholder="$tl('p.selectNode')"
                    :not-found-content="$tl('p.noNodeInfo')"
                    style="width: 140px"
                    :value="item.nodeId ? item.nodeId : undefined"
                    :disabled="
                      item.nodeId || (nodeIdMap[item.nodeId] && nodeIdMap[item.nodeId].openStatus !== 1) ? true : false
                    "
                    @change="(nodeId) => handleNodeListChange(nodeId, index)"
                  >
                    <a-select-option
                      v-for="nodeItemList in nodeProjectsList"
                      :key="nodeItemList.id"
                      :disabled="nodeIdMap[nodeItemList.id].openStatus !== 1"
                    >
                      {{ nodeNameMap[nodeItemList.id] }}
                    </a-select-option>
                  </a-select>
                  <span>{{ $tl('p.project') }} </span>
                  <a-select
                    style="width: 300px"
                    :placeholder="item.placeholder"
                    :default-value="item.projectId ? item.projectId : undefined"
                    :not-found-content="$tl('c.noProjectInThisNode')"
                    :disabled="dispatchList[index].disabled"
                    @change="(projectId) => handleProjectChange(projectId, index)"
                  >
                    <a-select-option
                      v-for="project in nodeProjectsList.filter((nitem) => nitem.id == item.nodeId)[0] &&
                      nodeProjectsList.filter((nitem) => nitem.id == item.nodeId)[0].projects"
                      :key="project.projectId"
                      :value="project.projectId"
                      :disabled="
                        project.outGivingProject ||
                        dispatchList.filter((item, nowIndex) => {
                          return (
                            item.nodeId === project.nodeId && item.projectId === project.projectId && nowIndex !== index
                          )
                        }).length > 0
                      "
                    >
                      <a-tooltip
                        :title="`${project.outGivingProject ? $tl('c.independentDistribution') : ''} ${project.name}`"
                      >
                        {{ project.outGivingProject ? $tl('c.independentDistribution') : '' }}
                        {{ project.name }}
                      </a-tooltip>
                    </a-select-option>
                  </a-select>
                  <a-button type="primary" danger size="small" @click="delDispachList(index)"
                    ><DeleteOutlined />
                  </a-button>
                </a-space>
              </a-list-item>
            </template>
          </a-list>
          <a-button type="primary" size="small" @click="addDispachList">{{ $tl('p.add') }}</a-button>
        </a-form-item>
        <a-form-item :label="$tl('c.distributionOperationAfter')" name="afterOpt">
          <a-select v-model:value="temp.afterOpt" :placeholder="$tl('c.selectOperationAfterPublish')">
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="temp.afterOpt === 2 || temp.afterOpt === 3" name="intervalTime">
          <template #label>
            <a-tooltip>
              {{ $tl('c.intervalTime') }}
              <template #title>
                {{ $tl('c.ensureProjectRestart') }}, {{ $tl('c.waitPreviousProjectStart') }},{{
                  $tl('c.configureBasedOnProjectStartTime')
                }}
                <li>{{ $tl('c.suggestedIntervalTime') }}</li>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input-number
            v-model:value="temp.intervalTime"
            :min="0"
            :placeholder="$tl('c.distributionIntervalTimeEffect')"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item name="secondaryDirectory" :label="$tl('c.secondaryDirectory')">
          <a-input v-model:value="temp.secondaryDirectory" :placeholder="$tl('c.publishToRootIfNotFilled')" />
        </a-form-item>
        <a-form-item name="clearOld">
          <template #label>
            <a-tooltip>
              {{ $tl('c.clearPublish') }}
              <template #title>
                {{ $tl('c.clearPublishDescription') }}
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-switch
                v-model:checked="temp.clearOld"
                :checked-children="$tl('c.yes')"
                :un-checked-children="$tl('c.no')"
              />
            </a-col>

            <a-col :span="4" style="text-align: right">
              <a-tooltip v-if="temp.type !== 'edit'">
                <template #title> {{ $tl('c.publishStopBefore') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              {{ $tl('c.publishStopBeforeColon') }}
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch
                  v-model:checked="temp.uploadCloseFirst"
                  :checked-children="$tl('c.yes')"
                  :un-checked-children="$tl('c.no')"
              /></a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="webhook">
          <template #label>
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $tl('c.distributionRequestAddress') }}</li>
                  <li>{{ $tl('c.parametersForDistribution') }}</li>
                  <li>status {{ $tl('c.statusValues') }}:{{ $tl('c.distributionStatuses') }}</li>
                  <li>{{ $tl('c.asynchronousRequest') }}</li>
                </ul>
              </template>
              WebHooks
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" :placeholder="$tl('c.distributionProcessRequest')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 创建/编辑分发项目 -->
    <a-modal
      v-model:open="editDispatchVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      :z-index="900"
      width="60vw"
      :title="temp.type === 'edit' ? $tl('p.editDistributionProject') : $tl('p.createDistributionProject')"
      :mask-closable="false"
      @ok="handleEditDispatchOk"
    >
      <a-spin :tip="$tl('p.loadingData')" :spinning="editDispatchLoading">
        <a-form
          ref="editDispatchForm"
          :rules="rules"
          :model="temp"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-alert
            v-if="!nodeList || !nodeList.length"
            :message="$tl('p.remind')"
            type="warning"
            show-icon
            style="margin-bottom: 10px"
          >
            <template #description>{{ $tl('p.noLogicalNode') }}</template>
          </a-alert>
          <a-form-item name="id">
            <template #label>
              <a-tooltip>
                {{ $tl('c.distributionID') }}
                <template #title>{{ $tl('c.distributionIDEqualsProjectID') }}</template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input
              v-if="temp.type === 'edit'"
              v-model:value="temp.id"
              :max-length="50"
              :disabled="temp.type === 'edit'"
              :placeholder="$tl('c.immutableAfterCreation')"
            />
            <template v-else>
              <a-input-search
                v-model:value="temp.id"
                :max-length="50"
                :placeholder="$tl('c.immutableAfterCreation')"
                @search="
                  () => {
                    temp = { ...temp, id: randomStr(6) }
                  }
                "
              >
                <template #enterButton>
                  <a-button type="primary">
                    {{ $tl('c.randomlyGenerated') }}
                  </a-button>
                </template>
              </a-input-search>
            </template>
            <!-- <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改,分发 ID 等同于项目 ID" /> -->
          </a-form-item>
          <a-form-item :label="$tl('c.distributionName')" name="name">
            <a-row>
              <a-col :span="10">
                <a-input v-model:value="temp.name" :max-length="50" :placeholder="$tl('p.distributionName')" />
              </a-col>
              <a-col :span="4" style="text-align: right">{{ $tl('c.groupingName') }}</a-col>
              <a-col :span="10">
                <a-form-item-rest>
                  <custom-select
                    v-model:value="temp.group"
                    :max-length="50"
                    :data="groupList"
                    :input-placeholder="$tl('c.addNewGrouping')"
                    :select-placeholder="$tl('c.selectGrouping')"
                  >
                  </custom-select>
                </a-form-item-rest>
              </a-col>
            </a-row>
          </a-form-item>

          <a-form-item name="runMode">
            <template #label>
              <a-tooltip>
                {{ $tl('p.runningMode') }}
                <template #title>
                  <ul>
                    <li><b>Dsl</b> {{ $tl('p.scriptTemplate') }}</li>
                    <li>
                      <b>ClassPath</b> java -classpath xxx
                      {{ $tl('c.runProject') }}
                    </li>
                    <li><b>Jar</b> java -jar xxx {{ $tl('c.runProject') }}</li>
                    <li>
                      <b>JarWar</b> java -jar Springboot war
                      {{ $tl('c.runProject') }}
                    </li>
                    <li>
                      <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
                      {{ $tl('c.runProject') }}
                    </li>
                    <li><b>File</b> {{ $tl('p.staticFolder') }},{{ $tl('p.noProjectStatus') }}</li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>

            <a-select v-model:value="temp.runMode" :placeholder="$tl('p.selectRunningMode')">
              <a-select-option v-for="item in runModeArray.filter((item) => item.onlyNode !== true)" :key="item.name">
                <template v-if="item.desc.indexOf($tl('p.notRecommended')) > -1">
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
            <template #label>
              <a-tooltip>
                {{ $tl('p.projectPath') }}
                <template #title>
                  <ul>
                    <li>{{ $tl('p.authorizationPath') }}</li>
                    <li>{{ $tl('p.modifyAuthorizationConfig') }}</li>
                    <li>{{ $tl('p.projectFolder') }}</li>
                    <li>
                      {{ $tl('p.projectFilesStored') }} <br />&nbsp;&nbsp;<b>{{
                        $tl('p.authorizationPathProjectFolder')
                      }}</b>
                    </li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input-group compact>
              <a-select
                v-model:value="temp.whitelistDirectory"
                style="width: 50%"
                :placeholder="$tl('c.selectProjectAuthorizationPath')"
              >
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">
                    {{ access }}
                  </a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input v-model:value="temp.lib" style="width: 50%" :placeholder="$tl('p.jarFolder')"
              /></a-form-item-rest>
            </a-input-group>
            <template #help>
              <div>
                {{ $tl('p.configureAuthorizationDirectory') }}

                <a-button
                  size="small"
                  type="link"
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $tl('p.configureDirectory') }}
                </a-button>
              </div>
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" :label="$tl('p.projectFullDirectory')">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
          <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
            <template #label>
              <a-tooltip>
                DSL {{ $tl('p.content') }}
                <template #title>
                  <p>{{ $tl('p.yamlConfig') }}</p>
                  <p>{{ $tl('p.variablesInScript') }}</p>
                  <p>
                    <b>status</b>
                    {{ $tl('p.scriptOutput') }}:$pid <b>$pid {{ $tl('p.processID') }}</b
                    >{{ $tl('p.incorrectOutputFormat') }}
                  </p>
                  <p>{{ $tl('p.referToConfigExample') }}</p>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <template #help>
              <div>
                scriptId{{ $tl('p.recommendedScriptDistribution') }}
                <a-button
                  type="link"
                  size="small"
                  @click="
                    () => {
                      viewScriptVisible = true
                    }
                  "
                >
                  {{ $tl('p.viewServerScript') }}
                </a-button>
              </div>
            </template>
            <a-form-item-rest>
              <!-- <a-tabs>
                <a-tab-pane key="1" tab="DSL 配置">
                  <code-editor
                    height="40vh"
                    v-model:content="temp.dslContent"
                    :options="{ mode: 'yaml', tabSize: 2 }"
                  ></code-editor>
                </a-tab-pane>
                <a-tab-pane key="2" tab="配置示例">
                  <code-editor
                    height="40vh"
                    v-model:content="PROJECT_DSL_DEFATUL"
                    :options="{
                      mode: 'yaml',
                      tabSize: 2,
                      readOnly: true
                    }"
                  ></code-editor>
                </a-tab-pane>
              </a-tabs> -->
              <code-editor
                v-show="dslEditTabKey === 'content'"
                v-model:content="temp.dslContent"
                height="40vh"
                :show-tool="true"
                :options="{ mode: 'yaml', tabSize: 2 }"
                :placeholder="$tl('p.fillProjectDSLConfig')"
              >
                <template #tool_before>
                  <a-segmented
                    v-model:value="dslEditTabKey"
                    :options="[
                      { label: `DSL ${$tl('c.configuration')}`, value: 'content' },
                      { label: $tl('c.configurationExample'), value: 'demo' }
                    ]"
                  />
                </template>
              </code-editor>
              <code-editor
                v-show="dslEditTabKey === 'demo'"
                v-model:content="PROJECT_DSL_DEFATUL"
                height="40vh"
                :show-tool="true"
                :options="{ mode: 'yaml', tabSize: 2, readOnly: true }"
              >
                <template #tool_before>
                  <a-segmented
                    v-model:value="dslEditTabKey"
                    :options="[
                      { label: `DSL ${$tl('c.configuration')}`, value: 'content' },
                      { label: $tl('c.configurationExample'), value: 'demo' }
                    ]"
                  />
                </template>
              </code-editor>
            </a-form-item-rest>
          </a-form-item>
          <a-form-item v-show="noFileModes.includes(temp.runMode)">
            <template #label>
              <a-tooltip>
                {{ $tl('p.logDirectory') }}
                <template #title>
                  <ul>
                    <li>{{ $tl('p.logDirectorySelection') }}</li>
                    <li>{{ $tl('c.defaultLogDirectory') }}</li>
                    <li>{{ $tl('p.sameConfigAsAuthorizationDirectory') }}</li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-select v-model:value="temp.logPath" :placeholder="$tl('p.selectLogDirectory')">
              <a-select-option key="" value="">{{ $tl('c.defaultLogDirectory') }}</a-select-option>
              <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            v-show="javaModes.includes(temp.runMode) && temp.runMode !== 'Jar'"
            label="Main Class"
            name="mainClass"
          >
            <a-input v-model:value="temp.mainClass" :placeholder="$tl('p.mainClass')" />
          </a-form-item>
          <a-form-item
            v-show="javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp'"
            label="JavaExtDirsCp"
            name="javaExtDirsCp"
          >
            <a-input
              v-model:value="temp.javaExtDirsCp"
              :placeholder="`-Dext.dirs=xxx: -cp xx  ${$tl('p.jvmArgs')}:xx】`"
            />
          </a-form-item>
          <a-form-item :label="$tl('c.distributionOperationAfter')" name="afterOpt">
            <a-select v-model:value="temp.afterOpt" :placeholder="$tl('c.selectOperationAfterPublish')">
              <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="temp.afterOpt === 2 || temp.afterOpt === 3" name="intervalTime">
            <template #label>
              <a-tooltip>
                {{ $tl('c.intervalTime') }}
                <template #title>
                  {{ $tl('c.ensureProjectRestart') }},{{ $tl('c.waitPreviousProjectStart') }},{{
                    $tl('c.configureBasedOnProjectStartTime')
                  }}
                  <li>{{ $tl('c.suggestedIntervalTime') }}</li>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input-number
              v-model:value="temp.intervalTime"
              :min="0"
              :placeholder="$tl('c.distributionIntervalTimeEffect')"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item name="secondaryDirectory" :label="$tl('c.secondaryDirectory')">
            <a-input v-model:value="temp.secondaryDirectory" :placeholder="$tl('c.publishToRootIfNotFilled')" />
          </a-form-item>
          <a-form-item name="clearOld">
            <template #label>
              <a-tooltip>
                {{ $tl('c.clearPublish') }}
                <template #title>
                  {{ $tl('c.clearPublishDescription') }}
                </template>
                <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-row>
              <a-col :span="4">
                <a-switch
                  v-model:checked="temp.clearOld"
                  :checked-children="$tl('c.yes')"
                  :un-checked-children="$tl('c.no')"
                />
              </a-col>
              <a-col :span="4" style="text-align: right">
                <a-tooltip v-if="temp.type !== 'edit'">
                  <template #title> {{ $tl('c.publishStopBefore') }} </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
                {{ $tl('c.publishStopBeforeColon') }}
              </a-col>
              <a-col :span="4">
                <a-form-item-rest>
                  <a-switch
                    v-model:checked="temp.uploadCloseFirst"
                    :checked-children="$tl('c.yes')"
                    :un-checked-children="$tl('c.no')"
                  />
                </a-form-item-rest>
              </a-col>
            </a-row>
          </a-form-item>
          <a-form-item name="webhook">
            <template #label>
              <a-tooltip>
                WebHooks
                <template #title>
                  <ul>
                    <li>{{ $tl('c.distributionRequestAddress') }}</li>
                    <li>{{ $tl('c.parametersForDistribution') }}</li>
                    <li>status {{ $tl('c.statusValues') }}:{{ $tl('c.distributionStatuses') }}</li>
                    <li>{{ $tl('c.asynchronousRequest') }}</li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="!temp.id" />
              </a-tooltip>
            </template>
            <a-input v-model:value="temp.webhook" :placeholder="$tl('c.distributionProcessRequest')" />
          </a-form-item>
          <!-- 节点 -->
          <a-form-item :label="$tl('c.distributionNode')" name="nodeId">
            <a-select
              v-model:value="temp.nodeIdList"
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
              mode="multiple"
              :placeholder="$tl('p.selectDistributionNode')"
            >
              <a-select-option v-for="node in nodeList" :key="node.id">{{ `${node.name}` }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-collapse>
            <a-collapse-panel v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeNameMap[nodeId] || nodeId">
              <a-form-item v-show="javaModes.includes(temp.runMode)" :label="$tl('p.jvmParams')" name="jvm">
                <a-textarea
                  v-model:value="temp[`${nodeId}_jvm`]"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="`jvm${$tl('p.params')},${$tl('p.nonMandatory')}.如：-Xms512m -Xmx512m`"
                />
              </a-form-item>
              <a-form-item v-show="javaModes.includes(temp.runMode)" :label="$tl('p.argsParams')" name="args">
                <a-textarea
                  v-model:value="temp[`${nodeId}_args`]"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="`Main ${$tl('p.functionArgsParams')}. ${$tl('p.argsParamsExample')}.port=8080`"
                />
              </a-form-item>
              <a-form-item v-show="noFileModes.includes(temp.runMode)" name="autoStart">
                <template #label>
                  <a-tooltip>
                    {{ $tl('p.autoStart') }}
                    <template #title>{{ $tl('p.checkProjectStatus') }}</template>
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </a-tooltip>
                </template>
                <template #help>
                  <div>
                    {{ $tl('p.nonServerAutoStart') }}<b>{{ $tl('p.pluginAutoStart') }}</b
                    >{{ $tl('p.turnOnAutoStart') }}
                  </div>
                </template>
                <a-switch
                  v-model:checked="temp[`${nodeId}_autoStart`]"
                  :checked-children="$tl('p.on')"
                  :un-checked-children="$tl('p.off')"
                />
                {{ $tl('p.pluginAutoCheckProjectOnStart') }}
              </a-form-item>
              <a-form-item name="disableScanDir">
                <template #label>
                  <a-tooltip> {{ $tl('p.disableScanDir') }} </a-tooltip>
                </template>
                <template #help>
                  <div>{{ $tl('p.disableScanDirTip') }}</div>
                </template>
                <div>
                  <a-switch
                    v-model:checked="temp[`${nodeId}_disableScanDir`]"
                    :checked-children="$tl('p.dontScanning')"
                    :un-checked-children="$tl('p.scanning')"
                  />
                </div>
              </a-form-item>
              <a-form-item v-if="temp.runMode === 'Dsl'" name="dslEnv" :label="$tl('p.dslEnvironmentVariables')">
                <!-- <a-input
                  v-model:checked="temp[`${nodeId}_dslEnv`]"
                  placeholder="DSL{{$tl('p.environmentVariables')}},{{$tl('p.exampleVariable')}}=values1&keyvalue2"
                /> -->
                <parameter-widget v-model:value="temp[`${nodeId}_dslEnv`]"></parameter-widget>
              </a-form-item>
              <a-form-item v-show="noFileModes.includes(temp.runMode)" name="token">
                <template #label>
                  <a-tooltip>
                    <template #title>
                      <ul>
                        <li>{{ $tl('p.projectRequestOnStartStopRestart') }}</li>
                        <li>{{ $tl('p.parametersForProjectRequest') }}</li>
                        <li>type {{ $tl('p.valuesForProjectRequest') }}</li>
                      </ul>
                    </template>
                    WebHooks
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </a-tooltip>
                </template>
                <a-input v-model:value="temp[`${nodeId}_token`]" :placeholder="$tl('p.projectRequestOnFileChange')" />
              </a-form-item>
            </a-collapse-panel>
          </a-collapse>
        </a-form>
      </a-spin>
    </a-modal>
    <!-- 分发项目 -->
    <start-dispatch
      v-if="dispatchVisible"
      :data="temp"
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
          drawerStatusVisible = false
        }
      "
    />
    <!-- 配置工作空间授权目录 -->
    <a-modal
      v-model:open="configDir"
      destroy-on-close
      :title="`${$tl('p.configureAuthorizationDirectory1')}`"
      :footer="null"
      width="50vw"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </a-modal>
    <!-- 查看服务端脚本 -->
    <a-drawer
      destroy-on-close
      :title="`${$tl('p.viewScript')}`"
      placement="right"
      :open="viewScriptVisible"
      width="70vw"
      :z-index="1109"
      @close="
        () => {
          viewScriptVisible = false
        }
      "
    >
      <scriptPage
        v-if="viewScriptVisible"
        choose="checkbox"
        @cancel="
          () => {
            viewScriptVisible = false
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
          title: this.$tl('c.distributionID'),
          dataIndex: 'id',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$tl('c.distributionName'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$tl('p.projectGrouping'),
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('c.distributionType'),
          dataIndex: 'outGivingProject',
          width: '90px',
          ellipsis: true
        },
        {
          title: this.$tl('p.distributionStatus'),
          dataIndex: 'afterOpt',
          ellipsis: true,
          width: '150px'
        },
        {
          title: this.$tl('c.clearPublish'),
          dataIndex: 'clearOld',
          align: 'center',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$tl('c.intervalTime'),
          dataIndex: 'intervalTime',
          width: 90,
          ellipsis: true
        },

        {
          title: this.$tl('p.status'),
          dataIndex: 'status',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$tl('c.secondaryDirectory'),
          dataIndex: 'secondaryDirectory',
          ellipsis: true,
          width: 110,
          tooltip: true
        },
        {
          title: this.$tl('p.creationTime'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modificationTime'),
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$tl('p.modifiedBy'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },
        {
          title: this.$tl('p.operations'),
          dataIndex: 'operation',

          fixed: 'right',
          width: '210px',
          align: 'center'
        }
      ],

      rules: {
        id: [{ required: true, message: this.$tl('p.enterProjectID'), trigger: 'blur' }],
        name: [{ required: true, message: this.$tl('p.enterProjectName'), trigger: 'blur' }],
        projectId: [{ required: true, message: this.$tl('c.selectProject'), trigger: 'blur' }],
        runMode: [{ required: true, message: this.$tl('p.selectProjectRunningMode'), trigger: 'blur' }],
        whitelistDirectory: [
          { required: true, message: this.$tl('c.selectProjectAuthorizationPath'), trigger: 'blur' }
        ],
        lib: [{ required: true, message: this.$tl('p.enterProjectFolder'), trigger: 'blur' }],
        afterOpt: [{ required: true, message: this.$tl('c.selectOperationAfterPublish'), trigger: 'blur' }]
      },
      // countdownTime: Date.now(),
      // refreshInterval: 5,

      viewDispatchManager: false,
      dslEditTabKey: 'content',
      drawerStatusVisible: false
    }
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
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
    $tl(key, ...args) {
      return this.$t(`pages.dispatch.list.${key}`, ...args)
    },
    randomStr,
    CHANGE_PAGE,

    // 静默
    // silenceLoadData() {
    //   if (this.$attrs.routerUrl !== this.$route.path) {
    //     //   // 重新计算倒计时
    //     //   // this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     return
    //   }
    //   getDishPatchList(this.listQuery, false).then((res) => {
    //     if (res.code === 200) {
    //       this.list = res.data.result
    //       this.listQuery.total = res.data.total
    //       //

    //       // 重新计算倒计时
    //       // this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     }
    //   })
    // },
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
          $notification.error({ message: this.$tl('p.selectAtLeastOneNodeProject') })
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
    // 新增分发项目
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
            // 新增 nodeIdList
            this.temp.nodeIdList.push(ele.nodeId)
            // 新增 jvm token args
            this.temp[`${ele.nodeId}_jvm`] = res.data.jvm || ''
            this.temp[`${ele.nodeId}_token`] = res.data.token || ''
            this.temp[`${ele.nodeId}_args`] = res.data.args || ''
            this.temp[`${ele.nodeId}_autoStart`] = res.data.autoStart
            this.temp[`${ele.nodeId}_disableScanDir`] = res.data.disableScanDir
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
            message: this.$tl('p.selectAtLeastOneNode')
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
      if (record.outGivingProject) {
        $confirm({
          title: this.$tl('c.systemPrompt'),
          zIndex: 1009,
          content: thorough
            ? this.$tl('p.confirmDeletionOfDistributionInfo')
            : this.$tl('p.confirmDeletionOfDistributionInfoSimple'),
          okText: this.$tl('c.confirm'),
          cancelText: this.$tl('c.cancel'),
          onOk: () => {
            return delDisPatchProject({ id: record.id, thorough: thorough }).then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                this.loadData()
              }
            })
          }
        })
        return
      }
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmReleaseOfDistributionInfo'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return releaseDelDisPatch(record.id).then((res) => {
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
    // 解绑
    handleUnbind(record) {
      const html = `
      <b style='font-size: 20px;'>
        ${this.$tl('p.confirmUnbindingCurrentNodeDistribution')}
      </b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
          <li>${this.$tl('p.unbindCheckDataAssociation')}</b></li>
          <li>${this.$tl('p.unbindForUnreachableServer')}</li>
          <li>${this.$tl('p.cautionDueToMistakeOperation')}</li>
      </ul>`

      $confirm({
        title: this.$tl('p.dangerousOperation'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', size: 'small', danger: true },
        cancelButtonProps: { type: 'primary' },
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return unbindOutgiving(record.id).then((res) => {
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
        this.dispatchList[index].placeholder = this.$tl('c.noProjectInThisNode')
        this.dispatchList[index].disabled = true
      } else {
        this.dispatchList[index].placeholder = this.$tl('c.selectProject')
        this.dispatchList[index].disabled = false
      }
      this.dispatchList[index].nodeId = nodeId
    },
    // 选择项目
    handleProjectChange(value, index) {
      this.dispatchList[index].projectId = value
    },
    // 新增分发
    addDispachList() {
      if (this.dispatchList.length >= this.totalProjectNum) {
        $notification.error({
          message: this.$tl('p.noMoreNodeProjects')
        })
        return false
      }
      this.dispatchList.push({
        index: this.dispatchList.length,
        placeholder: this.$tl('p.selectNodeFirst'),
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
      $confirm({
        title: this.$tl('c.systemPrompt'),
        zIndex: 1009,
        content: this.$tl('p.confirmCancellationOfCurrentDistribution'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return cancelOutgiving({ id: record.id }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
              // this.silenceLoadData()
            }
          })
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
