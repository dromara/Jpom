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
            :placeholder="$t('pages.dispatch.list.c266c17')"
            @press-enter="loadData"
          />
          <a-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('pages.dispatch.list.bb769c1d')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.group"
            allow-clear
            :placeholder="$t('pages.dispatch.list.dc532d0a')"
            class="search-input-item"
            @change="loadData"
          >
            <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.outGivingProject"
            allow-clear
            :placeholder="$t('pages.dispatch.list.585c23e6')"
            class="search-input-item"
          >
            <a-select-option :value="1">{{ $t('pages.dispatch.list.f4630780') }}</a-select-option>
            <a-select-option :value="0">{{ $t('pages.dispatch.list.92207645') }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.status"
            allow-clear
            :placeholder="$t('pages.dispatch.list.56195645')"
            class="search-input-item"
          >
            <a-select-option v-for="(name, key) in statusMap" :key="key">{{ name }}</a-select-option>
          </a-select>
          <a-tooltip :title="$t('pages.dispatch.list.cb5a8131')">
            <a-button type="primary" :loading="loading" @click="loadData">{{
              $t('pages.dispatch.list.53c2763c')
            }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleLink">{{ $t('pages.dispatch.list.d2f8649f') }}</a-button>
          <a-button type="primary" @click="handleAdd">{{ $t('pages.dispatch.list.fdb4b6ff') }}</a-button>

          <!-- <a-statistic-countdown format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" /> -->
        </a-space>
      </template>
      <template #tableHelp>
        <a-tooltip>
          <template #title>
            <div>{{ $t('pages.dispatch.list.99b26470') }}</div>

            <div>
              <ul>
                <li>{{ $t('pages.dispatch.list.2399f601') }}</li>
                <li>{{ $t('pages.dispatch.list.2b3dba3c') }}</li>
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
            <a-tag v-if="text === 2" color="green">{{ statusMap[text] || $t('pages.dispatch.list.5f51a112') }}</a-tag>
            <a-tag v-else-if="text === 1" color="orange">{{
              statusMap[text] || $t('pages.dispatch.list.5f51a112')
            }}</a-tag>
            <a-tag v-else-if="text === 3 || text === 4" color="red">{{
              statusMap[text] || $t('pages.dispatch.list.5f51a112')
            }}</a-tag>
            <a-tag v-else>{{ statusMap[text] || $t('pages.dispatch.list.5f51a112') }}</a-tag>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'clearOld'">
          <a-tooltip>
            <a-switch
              size="small"
              :checked-children="$t('pages.dispatch.list.d2fbce36')"
              :un-checked-children="$t('pages.dispatch.list.1c77d6fb')"
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
          <span v-if="text">{{ $t('pages.dispatch.list.f4630780') }}</span>
          <span v-else>{{ $t('pages.dispatch.list.92207645') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button size="small" type="primary" @click="handleDispatch(record)">
              {{ $t('pages.dispatch.list.bd2ea0af') }}
            </a-button>

            <a-button
              v-if="record.outGivingProject"
              size="small"
              type="primary"
              @click="handleEditDispatchProject(record)"
              >{{ $t('pages.dispatch.list.e1224c34') }}</a-button
            >
            <a-button v-else size="small" type="primary" @click="handleEditDispatch(record)">
              {{ $t('pages.dispatch.list.e1224c34') }}
            </a-button>

            <a-dropdown>
              <a @click="(e) => e.preventDefault()">
                {{ $t('pages.dispatch.list.6e071067') }}
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
                      {{ $t('pages.dispatch.list.9afbd509') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleDelete(record, '')">
                      {{
                        record.outGivingProject
                          ? $t('pages.dispatch.list.dd20d11c')
                          : $t('pages.dispatch.list.f97d7b7c')
                      }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item v-if="record.outGivingProject">
                    <a-button type="primary" danger size="small" @click="handleDelete(record, 'thorough')">
                      {{ $t('pages.dispatch.list.dbe32f01') }}
                    </a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button type="primary" danger size="small" @click="handleUnbind(record)">
                      {{ $t('pages.dispatch.list.4c957529') }}
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
      :title="temp.type === 'edit' ? $t('pages.dispatch.list.b006329f') : $t('pages.dispatch.list.9ef2c865')"
      :mask-closable="false"
      @ok="handleLinkDispatchOk"
      @cancel="clearDispatchList"
    >
      <a-form ref="linkDispatchForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item name="id">
          <template #label>
            <a-tooltip>
              {{ $t('pages.dispatch.list.c3c9e67b') }}
              <template #title>{{ $t('pages.dispatch.list.abda741f') }}</template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$t('pages.dispatch.list.9878fcc3')"
          />
          <template v-else>
            <a-input-search
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$t('pages.dispatch.list.9878fcc3')"
              @search="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <a-button type="primary">
                  {{ $t('pages.dispatch.list.24a518c3') }}
                </a-button>
              </template>
            </a-input-search>
          </template>
          <!-- <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" /> -->
        </a-form-item>

        <a-form-item :label="$t('pages.dispatch.list.8aa10ac9')" name="name">
          <a-row>
            <a-col :span="10">
              <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.dispatch.list.8aa10ac9')" />
            </a-col>
            <a-col :span="4" style="text-align: right">{{ $t('pages.dispatch.list.7c047057') }}</a-col>
            <a-col :span="10">
              <a-form-item-rest>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$t('pages.dispatch.list.ded4b4cd')"
                  :select-placeholder="$t('pages.dispatch.list.13bca766')"
                >
                </custom-select>
              </a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>

        <a-form-item :label="$t('pages.dispatch.list.51fd9c8a')" required>
          <a-list
            item-layout="horizontal"
            :data-source="dispatchList"
            :locale="{
              emptyText: $t('pages.dispatch.list.53e901cf')
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
                  <div>{{ $t('pages.dispatch.list.602a0a5e') }}</div>
                  <a-select
                    :placeholder="$t('pages.dispatch.list.580e6c10')"
                    :not-found-content="$t('pages.dispatch.list.e20812b')"
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
                  <span>{{ $t('pages.dispatch.list.4889a88f') }} </span>
                  <a-select
                    style="width: 300px"
                    :placeholder="item.placeholder"
                    :default-value="item.projectId ? item.projectId : undefined"
                    :not-found-content="$t('pages.dispatch.list.e6af4e4c')"
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
                        :title="`${project.outGivingProject ? $t('pages.dispatch.list.44fbbff') : ''} ${project.name}`"
                      >
                        {{ project.outGivingProject ? $t('pages.dispatch.list.44fbbff') : '' }}
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
          <a-button type="primary" size="small" @click="addDispachList">{{
            $t('pages.dispatch.list.7d46652a')
          }}</a-button>
        </a-form-item>
        <a-form-item :label="$t('pages.dispatch.list.50b92ecb')" name="afterOpt">
          <a-select v-model:value="temp.afterOpt" :placeholder="$t('pages.dispatch.list.9a15b54b')">
            <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="temp.afterOpt === 2 || temp.afterOpt === 3" name="intervalTime">
          <template #label>
            <a-tooltip>
              {{ $t('pages.dispatch.list.26d543f6') }}
              <template #title>
                {{ $t('pages.dispatch.list.2c7f38ee') }}, {{ $t('pages.dispatch.list.e68260b4') }},{{
                  $t('pages.dispatch.list.5d1f2d8e')
                }}
                <li>{{ $t('pages.dispatch.list.bd011884') }}</li>
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-input-number
            v-model:value="temp.intervalTime"
            :min="0"
            :placeholder="$t('pages.dispatch.list.c7ed6684')"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item name="secondaryDirectory" :label="$t('pages.dispatch.list.2b72d6ca')">
          <a-input v-model:value="temp.secondaryDirectory" :placeholder="$t('pages.dispatch.list.3a664c1f')" />
        </a-form-item>
        <a-form-item name="clearOld">
          <template #label>
            <a-tooltip>
              {{ $t('pages.dispatch.list.157bc8ea') }}
              <template #title>
                {{ $t('pages.dispatch.list.32718c0') }}
              </template>
              <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
            </a-tooltip>
          </template>
          <a-row>
            <a-col :span="4">
              <a-switch
                v-model:checked="temp.clearOld"
                :checked-children="$t('pages.dispatch.list.d2fbce36')"
                :un-checked-children="$t('pages.dispatch.list.1c77d6fb')"
              />
            </a-col>

            <a-col :span="4" style="text-align: right">
              <a-tooltip v-if="temp.type !== 'edit'">
                <template #title> {{ $t('pages.dispatch.list.9f9df513') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              {{ $t('pages.dispatch.list.267c8d13') }}
            </a-col>
            <a-col :span="4">
              <a-form-item-rest>
                <a-switch
                  v-model:checked="temp.uploadCloseFirst"
                  :checked-children="$t('pages.dispatch.list.d2fbce36')"
                  :un-checked-children="$t('pages.dispatch.list.1c77d6fb')"
              /></a-form-item-rest>
            </a-col>
          </a-row>
        </a-form-item>
        <a-form-item name="webhook">
          <template #label>
            <a-tooltip>
              <template #title>
                <ul>
                  <li>{{ $t('pages.dispatch.list.1af01061') }}</li>
                  <li>{{ $t('pages.dispatch.list.d20429d4') }}</li>
                  <li>status {{ $t('pages.dispatch.list.5323a6a3') }}:{{ $t('pages.dispatch.list.be2dccc9') }}</li>
                  <li>{{ $t('pages.dispatch.list.663c4985') }}</li>
                </ul>
              </template>
              WebHooks
              <QuestionCircleOutlined v-show="!temp.id" />
            </a-tooltip>
          </template>
          <a-input v-model:value="temp.webhook" :placeholder="$t('pages.dispatch.list.ad9cad23')" />
        </a-form-item>
      </a-form>
    </a-modal>
    <!-- 创建/编辑分发项目 -->
    <CustomModal
      v-if="editDispatchVisible"
      v-model:open="editDispatchVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="temp.type === 'edit' ? $t('pages.dispatch.list.7d755f9b') : $t('pages.dispatch.list.898758b9')"
      :mask-closable="false"
      @ok="handleEditDispatchOk"
    >
      <a-spin :tip="$t('pages.dispatch.list.a5c1d44')" :spinning="editDispatchLoading">
        <a-form
          ref="editDispatchForm"
          :rules="rules"
          :model="temp"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 18 }"
        >
          <a-alert
            v-if="!nodeList || !nodeList.length"
            :message="$t('pages.dispatch.list.7e2364d6')"
            type="warning"
            show-icon
            style="margin-bottom: 10px"
          >
            <template #description>{{ $t('pages.dispatch.list.5386da00') }}</template>
          </a-alert>
          <a-form-item name="id">
            <template #label>
              <a-tooltip>
                {{ $t('pages.dispatch.list.c3c9e67b') }}
                <template #title>{{ $t('pages.dispatch.list.abda741f') }}</template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input
              v-if="temp.type === 'edit'"
              v-model:value="temp.id"
              :max-length="50"
              :disabled="temp.type === 'edit'"
              :placeholder="$t('pages.dispatch.list.786953be')"
            />
            <template v-else>
              <a-input-search
                v-model:value="temp.id"
                :max-length="50"
                :placeholder="$t('pages.dispatch.list.786953be')"
                @search="
                  () => {
                    temp = { ...temp, id: randomStr(6) }
                  }
                "
              >
                <template #enterButton>
                  <a-button type="primary">
                    {{ $t('pages.dispatch.list.24a518c3') }}
                  </a-button>
                </template>
              </a-input-search>
            </template>
            <!-- <a-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改,分发 ID 等同于项目 ID" /> -->
          </a-form-item>
          <a-form-item :label="$t('pages.dispatch.list.8aa10ac9')" name="name">
            <a-row>
              <a-col :span="10">
                <a-input v-model:value="temp.name" :max-length="50" :placeholder="$t('pages.dispatch.list.4e848bb3')" />
              </a-col>
              <a-col :span="4" style="text-align: right">{{ $t('pages.dispatch.list.7c047057') }}</a-col>
              <a-col :span="10">
                <a-form-item-rest>
                  <custom-select
                    v-model:value="temp.group"
                    :max-length="50"
                    :data="groupList"
                    :input-placeholder="$t('pages.dispatch.list.ded4b4cd')"
                    :select-placeholder="$t('pages.dispatch.list.13bca766')"
                  >
                  </custom-select>
                </a-form-item-rest>
              </a-col>
            </a-row>
          </a-form-item>

          <a-form-item name="runMode">
            <template #label>
              <a-tooltip>
                {{ $t('pages.dispatch.list.b2108479') }}
                <template #title>
                  <ul>
                    <li><b>Dsl</b> {{ $t('pages.dispatch.list.c086fc82') }}</li>
                    <li>
                      <b>ClassPath</b> java -classpath xxx
                      {{ $t('pages.dispatch.list.788bfc') }}
                    </li>
                    <li><b>Jar</b> java -jar xxx {{ $t('pages.dispatch.list.788bfc') }}</li>
                    <li>
                      <b>JarWar</b> java -jar Springboot war
                      {{ $t('pages.dispatch.list.788bfc') }}
                    </li>
                    <li>
                      <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
                      {{ $t('pages.dispatch.list.788bfc') }}
                    </li>
                    <li>
                      <b>File</b> {{ $t('pages.dispatch.list.d33f4295') }},{{ $t('pages.dispatch.list.b6295c0e') }}
                    </li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>

            <a-select v-model:value="temp.runMode" :placeholder="$t('pages.dispatch.list.79573f90')">
              <a-select-option v-for="item in runModeArray.filter((item) => item.onlyNode !== true)" :key="item.name">
                <template v-if="item.desc.indexOf($t('pages.dispatch.list.ad7005ba')) > -1">
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
                {{ $t('pages.dispatch.list.5b716424') }}
                <template #title>
                  <ul>
                    <li>{{ $t('pages.dispatch.list.87db3fab') }}</li>
                    <li>{{ $t('pages.dispatch.list.2a4034e5') }}</li>
                    <li>{{ $t('pages.dispatch.list.aa675b68') }}</li>
                    <li>
                      {{ $t('pages.dispatch.list.961e28d') }} <br />&nbsp;&nbsp;<b>{{
                        $t('pages.dispatch.list.7bdaa7bf')
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
                :placeholder="$t('pages.dispatch.list.fea56c3')"
              >
                <a-select-option v-for="access in accessList" :key="access">
                  <a-tooltip :title="access">
                    {{ access }}
                  </a-tooltip>
                </a-select-option>
              </a-select>
              <a-form-item-rest>
                <a-input v-model:value="temp.lib" style="width: 50%" :placeholder="$t('pages.dispatch.list.30061739')"
              /></a-form-item-rest>
            </a-input-group>
            <template #help>
              <div>
                {{ $t('pages.dispatch.list.a5df5146') }}

                <a-button
                  size="small"
                  type="link"
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $t('pages.dispatch.list.ed2197b9') }}
                </a-button>
              </div>
            </template>
          </a-form-item>

          <a-form-item v-show="filePath !== ''" :label="$t('pages.dispatch.list.6541b24c')">
            <a-alert :message="filePath" type="success" />
          </a-form-item>
          <a-form-item v-show="temp.runMode === 'Dsl'" name="dslContent">
            <template #label>
              <a-tooltip>
                DSL {{ $t('pages.dispatch.list.99ff48c8') }}
                <template #title>
                  <p>{{ $t('pages.dispatch.list.af2dc0ed') }}</p>
                  <p>{{ $t('pages.dispatch.list.cac3b49f') }}</p>
                  <p>
                    <b>status</b>
                    {{ $t('pages.dispatch.list.24159903') }}:$pid <b>$pid {{ $t('pages.dispatch.list.79c943f') }}</b
                    >{{ $t('pages.dispatch.list.44857232') }}
                  </p>
                  <p>{{ $t('pages.dispatch.list.2d9d9e69') }}</p>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <template #help>
              <div>
                scriptId{{ $t('pages.dispatch.list.14102976') }}
                <a-button
                  type="link"
                  size="small"
                  @click="
                    () => {
                      viewScriptVisible = true
                    }
                  "
                >
                  {{ $t('pages.dispatch.list.e4c6938e') }}
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
                :placeholder="$t('pages.dispatch.list.b3b45f16')"
              >
                <template #tool_before>
                  <a-segmented
                    v-model:value="dslEditTabKey"
                    :options="[
                      { label: `DSL ${$t('pages.dispatch.list.28f9e270')}`, value: 'content' },
                      { label: $t('pages.dispatch.list.36177d5c'), value: 'demo' }
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
                      { label: `DSL ${$t('pages.dispatch.list.28f9e270')}`, value: 'content' },
                      { label: $t('pages.dispatch.list.36177d5c'), value: 'demo' }
                    ]"
                  />
                </template>
              </code-editor>
            </a-form-item-rest>
          </a-form-item>
          <a-form-item v-show="noFileModes.includes(temp.runMode)">
            <template #label>
              <a-tooltip>
                {{ $t('pages.dispatch.list.8d83036e') }}
                <template #title>
                  <ul>
                    <li>{{ $t('pages.dispatch.list.15aa24c8') }}</li>
                    <li>{{ $t('pages.dispatch.list.965acf34') }}</li>
                    <li>{{ $t('pages.dispatch.list.c6fe24a4') }}</li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-select v-model:value="temp.logPath" :placeholder="$t('pages.dispatch.list.549e1e09')">
              <a-select-option key="" value="">{{ $t('pages.dispatch.list.965acf34') }}</a-select-option>
              <a-select-option v-for="access in accessList" :key="access">{{ access }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item
            v-show="javaModes.includes(temp.runMode) && temp.runMode !== 'Jar'"
            label="Main Class"
            name="mainClass"
          >
            <a-input v-model:value="temp.mainClass" :placeholder="$t('pages.dispatch.list.b324b030')" />
          </a-form-item>
          <a-form-item
            v-show="javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp'"
            label="JavaExtDirsCp"
            name="javaExtDirsCp"
          >
            <a-input
              v-model:value="temp.javaExtDirsCp"
              :placeholder="`-Dext.dirs=xxx: -cp xx  ${$t('pages.dispatch.list.fe24b714')}:xx】`"
            />
          </a-form-item>
          <a-form-item :label="$t('pages.dispatch.list.50b92ecb')" name="afterOpt">
            <a-select v-model:value="temp.afterOpt" :placeholder="$t('pages.dispatch.list.9a15b54b')">
              <a-select-option v-for="item in afterOptList" :key="item.value">{{ item.title }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="temp.afterOpt === 2 || temp.afterOpt === 3" name="intervalTime">
            <template #label>
              <a-tooltip>
                {{ $t('pages.dispatch.list.26d543f6') }}
                <template #title>
                  {{ $t('pages.dispatch.list.2c7f38ee') }},{{ $t('pages.dispatch.list.e68260b4') }},{{
                    $t('pages.dispatch.list.5d1f2d8e')
                  }}
                  <li>{{ $t('pages.dispatch.list.bd011884') }}</li>
                </template>
                <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-input-number
              v-model:value="temp.intervalTime"
              :min="0"
              :placeholder="$t('pages.dispatch.list.c7ed6684')"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item name="secondaryDirectory" :label="$t('pages.dispatch.list.2b72d6ca')">
            <a-input v-model:value="temp.secondaryDirectory" :placeholder="$t('pages.dispatch.list.3a664c1f')" />
          </a-form-item>
          <a-form-item name="clearOld">
            <template #label>
              <a-tooltip>
                {{ $t('pages.dispatch.list.157bc8ea') }}
                <template #title>
                  {{ $t('pages.dispatch.list.32718c0') }}
                </template>
                <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
              </a-tooltip>
            </template>
            <a-row>
              <a-col :span="4">
                <a-switch
                  v-model:checked="temp.clearOld"
                  :checked-children="$t('pages.dispatch.list.d2fbce36')"
                  :un-checked-children="$t('pages.dispatch.list.1c77d6fb')"
                />
              </a-col>
              <a-col :span="4" style="text-align: right">
                <a-tooltip v-if="temp.type !== 'edit'">
                  <template #title> {{ $t('pages.dispatch.list.9f9df513') }} </template>
                  <QuestionCircleOutlined />
                </a-tooltip>
                {{ $t('pages.dispatch.list.267c8d13') }}
              </a-col>
              <a-col :span="4">
                <a-form-item-rest>
                  <a-switch
                    v-model:checked="temp.uploadCloseFirst"
                    :checked-children="$t('pages.dispatch.list.d2fbce36')"
                    :un-checked-children="$t('pages.dispatch.list.1c77d6fb')"
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
                    <li>{{ $t('pages.dispatch.list.1af01061') }}</li>
                    <li>{{ $t('pages.dispatch.list.d20429d4') }}</li>
                    <li>status {{ $t('pages.dispatch.list.5323a6a3') }}:{{ $t('pages.dispatch.list.be2dccc9') }}</li>
                    <li>{{ $t('pages.dispatch.list.663c4985') }}</li>
                  </ul>
                </template>
                <QuestionCircleOutlined v-show="!temp.id" />
              </a-tooltip>
            </template>
            <a-input v-model:value="temp.webhook" :placeholder="$t('pages.dispatch.list.ad9cad23')" />
          </a-form-item>
          <!-- 节点 -->
          <a-form-item :label="$t('pages.dispatch.list.51fd9c8a')" name="nodeId">
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
              :placeholder="$t('pages.dispatch.list.27b4e405')"
            >
              <a-select-option v-for="node in nodeList" :key="node.id">{{ `${node.name}` }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-collapse>
            <a-collapse-panel v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeNameMap[nodeId] || nodeId">
              <a-form-item
                v-show="javaModes.includes(temp.runMode)"
                :label="$t('pages.dispatch.list.b482c412')"
                name="jvm"
              >
                <a-textarea
                  v-model:value="temp[`${nodeId}_jvm`]"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="
                    $t('pages.dispatch.list.17924912', {
                      slot1: $t('pages.dispatch.list.68fca368'),
                      slot2: $t('pages.dispatch.list.f3096daa')
                    })
                  "
                />
              </a-form-item>
              <a-form-item
                v-show="javaModes.includes(temp.runMode)"
                :label="$t('pages.dispatch.list.550bae25')"
                name="args"
              >
                <a-textarea
                  v-model:value="temp[`${nodeId}_args`]"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="`Main ${$t('pages.dispatch.list.f2214ebb')}. ${$t(
                    'pages.dispatch.list.94c10a27'
                  )}.port=8080`"
                />
              </a-form-item>
              <a-form-item v-show="noFileModes.includes(temp.runMode)" name="autoStart">
                <template #label>
                  <a-tooltip>
                    {{ $t('pages.dispatch.list.12861e4e') }}
                    <template #title>{{ $t('pages.dispatch.list.6023ee05') }}</template>
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </a-tooltip>
                </template>
                <template #help>
                  <div>
                    {{ $t('pages.dispatch.list.2190c483') }}<b>{{ $t('pages.dispatch.list.7172ec4f') }}</b
                    >{{ $t('pages.dispatch.list.bd19335d') }}
                  </div>
                </template>
                <a-switch
                  v-model:checked="temp[`${nodeId}_autoStart`]"
                  :checked-children="$t('pages.dispatch.list.30c72f5d')"
                  :un-checked-children="$t('pages.dispatch.list.abe04b8e')"
                />
                {{ $t('pages.dispatch.list.78de10a4') }}
              </a-form-item>
              <a-form-item name="disableScanDir">
                <template #label>
                  <a-tooltip> {{ $t('pages.dispatch.list.e1eda90c') }} </a-tooltip>
                </template>
                <template #help>
                  <div>{{ $t('pages.dispatch.list.d8aa9e4c') }}</div>
                </template>
                <div>
                  <a-switch
                    v-model:checked="temp[`${nodeId}_disableScanDir`]"
                    :checked-children="$t('pages.dispatch.list.c85f6b97')"
                    :un-checked-children="$t('pages.dispatch.list.968ca34a')"
                  />
                </div>
              </a-form-item>
              <a-form-item v-if="temp.runMode === 'Dsl'" name="dslEnv" :label="$t('pages.dispatch.list.72ddf462')">
                <!-- <a-input
                  v-model:checked="temp[`${nodeId}_dslEnv`]"
                  placeholder="DSL{{$t('pages.dispatch.list.c81b2c2e')}},{{$t('pages.dispatch.list.24e5c26a')}}=values1&keyvalue2"
                /> -->
                <parameter-widget v-model:value="temp[`${nodeId}_dslEnv`]"></parameter-widget>
              </a-form-item>
              <a-form-item v-show="noFileModes.includes(temp.runMode)" name="token">
                <template #label>
                  <a-tooltip>
                    <template #title>
                      <ul>
                        <li>{{ $t('pages.dispatch.list.3e549c2f') }}</li>
                        <li>{{ $t('pages.dispatch.list.4bebe9d') }}</li>
                        <li>type {{ $t('pages.dispatch.list.bbb000cc') }}</li>
                      </ul>
                    </template>
                    WebHooks
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </a-tooltip>
                </template>
                <a-input v-model:value="temp[`${nodeId}_token`]" :placeholder="$t('pages.dispatch.list.187760d7')" />
              </a-form-item>
            </a-collapse-panel>
          </a-collapse>
        </a-form>
      </a-spin>
    </CustomModal>
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
      :title="`${$t('pages.dispatch.list.1cc6d443')}`"
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
    <CustomDrawer
      v-if="viewScriptVisible"
      destroy-on-close
      :title="`${$t('pages.dispatch.list.9bc942bf')}`"
      placement="right"
      :open="viewScriptVisible"
      width="70vw"
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
    </CustomDrawer>
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
  randomStr,
  itemGroupBy,
  parseTime
} from '@/utils/const'
import { PROJECT_DSL_DEFATUL } from '@/utils/const-i18n'
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
          title: this.$t('pages.dispatch.list.c3c9e67b'),
          dataIndex: 'id',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$t('pages.dispatch.list.8aa10ac9'),
          dataIndex: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('pages.dispatch.list.5eefb9a9'),
          dataIndex: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$t('pages.dispatch.list.585c23e6'),
          dataIndex: 'outGivingProject',
          width: '90px',
          ellipsis: true
        },
        {
          title: this.$t('pages.dispatch.list.2f6529d5'),
          dataIndex: 'afterOpt',
          ellipsis: true,
          width: '150px'
        },
        {
          title: this.$t('pages.dispatch.list.157bc8ea'),
          dataIndex: 'clearOld',
          align: 'center',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('pages.dispatch.list.26d543f6'),
          dataIndex: 'intervalTime',
          width: 90,
          ellipsis: true
        },

        {
          title: this.$t('pages.dispatch.list.9c32c887'),
          dataIndex: 'status',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$t('pages.dispatch.list.2b72d6ca'),
          dataIndex: 'secondaryDirectory',
          ellipsis: true,
          width: 110,
          tooltip: true
        },
        {
          title: this.$t('pages.dispatch.list.f06e8846'),
          dataIndex: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.dispatch.list.61164914'),
          dataIndex: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '170px'
        },
        {
          title: this.$t('pages.dispatch.list.8406fc21'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },
        {
          title: this.$t('pages.dispatch.list.40ebd874'),
          dataIndex: 'operation',

          fixed: 'right',
          width: '210px',
          align: 'center'
        }
      ],

      rules: {
        id: [{ required: true, message: this.$t('pages.dispatch.list.6d76ea6b'), trigger: 'blur' }],
        name: [{ required: true, message: this.$t('pages.dispatch.list.c9d5fa0c'), trigger: 'blur' }],
        projectId: [{ required: true, message: this.$t('pages.dispatch.list.6836d5bb'), trigger: 'blur' }],
        runMode: [{ required: true, message: this.$t('pages.dispatch.list.d83981be'), trigger: 'blur' }],
        whitelistDirectory: [{ required: true, message: this.$t('pages.dispatch.list.fea56c3'), trigger: 'blur' }],
        lib: [{ required: true, message: this.$t('pages.dispatch.list.fc983188'), trigger: 'blur' }],
        afterOpt: [{ required: true, message: this.$t('pages.dispatch.list.9a15b54b'), trigger: 'blur' }]
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
          $notification.error({ message: this.$t('pages.dispatch.list.9548a044') })
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
            message: this.$t('pages.dispatch.list.6ad32a91')
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
          title: this.$t('pages.dispatch.list.a8fe4c17'),
          zIndex: 1009,
          content: thorough ? this.$t('pages.dispatch.list.638fc0de') : this.$t('pages.dispatch.list.f85d7ea4'),
          okText: this.$t('pages.dispatch.list.7da4a591'),
          cancelText: this.$t('pages.dispatch.list.43105e21'),
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
        title: this.$t('pages.dispatch.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.dispatch.list.ab7b03bc'),
        okText: this.$t('pages.dispatch.list.7da4a591'),
        cancelText: this.$t('pages.dispatch.list.43105e21'),
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
        ${this.$t('pages.dispatch.list.c5bfcffb')}
      </b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
          <li>${this.$t('pages.dispatch.list.3764ec84')}</b></li>
          <li>${this.$t('pages.dispatch.list.cbd05868')}</li>
          <li>${this.$t('pages.dispatch.list.b65e38f0')}</li>
      </ul>`

      $confirm({
        title: this.$t('pages.dispatch.list.cd503941'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', size: 'small', danger: true },
        cancelButtonProps: { type: 'primary' },
        okText: this.$t('pages.dispatch.list.7da4a591'),
        cancelText: this.$t('pages.dispatch.list.43105e21'),
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
        this.dispatchList[index].placeholder = this.$t('pages.dispatch.list.e6af4e4c')
        this.dispatchList[index].disabled = true
      } else {
        this.dispatchList[index].placeholder = this.$t('pages.dispatch.list.6836d5bb')
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
          message: this.$t('pages.dispatch.list.4715a27')
        })
        return false
      }
      this.dispatchList.push({
        index: this.dispatchList.length,
        placeholder: this.$t('pages.dispatch.list.3a5ce43c'),
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
        title: this.$t('pages.dispatch.list.a8fe4c17'),
        zIndex: 1009,
        content: this.$t('pages.dispatch.list.a5cbc9d2'),
        okText: this.$t('pages.dispatch.list.7da4a591'),
        cancelText: this.$t('pages.dispatch.list.43105e21'),
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
