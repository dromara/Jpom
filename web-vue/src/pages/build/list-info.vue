<template>
  <div>
    <!-- 表格 -->
    <!-- <a-card :body-style="{ padding: '10px' }"> -->

    <!-- <template v-if="layoutType === 'card'">
      <template v-if="list && list.length">
        <a-row :gutter="[16, 16]">
          <a-col v-for="item in list" :key="item.id" :span="6"> </a-col>
        </a-row>
      </template>
      <template v-else>
        <a-empty :image="Empty.PRESENTED_IMAGE_SIMPLE" description="没有任何构建" />
      </template>
    </template> -->
    <!-- <template v-else-if="layoutType === 'table'"> -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      table-name="buildList"
      :empty-description="$tl('p.noBuild')"
      :active-page="activePage"
      :loading="loading"
      :layout="layout"
      size="middle"
      :columns="columns"
      :data-source="list"
      bordered
      row-key="id"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      :row-selection="rowSelection"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, {
            pagination,
            sorter
          })
          loadData()
        }
      "
      @refresh="loadData"
      @change-table-layout="
        (layoutType) => {
          tableSelections = []
          listQuery = CHANGE_PAGE(listQuery, {
            pagination: { limit: layoutType === 'card' ? 8 : 10 }
          })
          loadData()
        }
      "
    >
      <template #title>
        <a-space wrap class="search-box">
          <a-input
            v-model:value="listQuery['%name%']"
            allow-clear
            class="search-input-item"
            :placeholder="$tl('p.buildName')"
            @press-enter="loadData"
          />
          <a-select
            v-model:value="listQuery.status"
            show-search
            allow-clear
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
            :placeholder="$tl('c.status')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in statusMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.releaseMethod"
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
            allow-clear
            :placeholder="$tl('c.publishMethod')"
            class="search-input-item"
          >
            <a-select-option v-for="(val, key) in releaseMethodMap" :key="key">{{ val }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="listQuery.group"
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
            allow-clear
            :placeholder="$tl('c.group')"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%resultDirFile%']"
            allow-clear
            class="search-input-item"
            :placeholder="$tl('c.artifactCatalog')"
            @press-enter="loadData"
          />
          <a-tooltip :title="$tl('p.backToFirstPage')">
            <a-button type="primary" :loading="loading" @click="loadData">{{ $tl('p.search') }}</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">{{ $tl('p.new') }}</a-button>
          <template v-if="tableSelections && tableSelections.length">
            <a-dropdown>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1" @click="batchBuild"> {{ $tl('p.batchBuild') }} </a-menu-item>
                  <a-menu-item key="2" @click="batchCancel"> {{ $tl('p.batchCancel') }} </a-menu-item>
                  <a-menu-item key="3" @click="handleBatchDelete"> {{ $tl('p.batchDelete') }} </a-menu-item>
                </a-menu>
              </template>
              <a-button type="primary"> {{ $tl('c.batchOperation') }}<DownOutlined /> </a-button>
            </a-dropdown>
          </template>
          <a-tooltip v-else :title="$tl('p.batchOperationNote')">
            <a-button :disabled="true" type="primary"> {{ $tl('c.batchOperation') }} <DownOutlined /> </a-button>
          </a-tooltip>

          <!-- <a-button v-if="!layout" type="primary" @click="changeLayout">
            <template #icon>
              <LayoutOutlined v-if="layoutType === 'card'" />
              <TableOutlined v-else />
            </template>
            {{ layoutType === 'card' ? '卡片' : '表格' }}
          </a-button>

          <a-statistic-countdown
            v-if="!choose"
            format=" s 秒"
            title="刷新倒计时"
            :value="countdownTime"
            @finish="silenceLoadData"
          />
          -->
        </a-space>
      </template>
      <template #cardBodyCell="{ item }">
        <a-card :head-style="{ padding: '0 6px' }" :body-style="{ padding: '10px' }">
          <template #title>
            <a-row :gutter="[4, 0]">
              <a-col :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                <a-button type="link" style="padding: 0" size="small" @click="handleDetails(item)">
                  <span> {{ item.name }}</span>
                </a-button>
              </a-col>
              <a-col :span="7" style="text-align: right" class="text-overflow-hidden">
                <a-tooltip
                  :title="`${$tl('p.currentStatus')}${statusMap[item.status]} ${item.statusMsg ? $tl('p.statusMessage') + item.statusMsg : ''}`"
                >
                  <a-tag :color="statusColor[item.status]" style="margin-right: 0">
                    {{ statusMap[item.status] || $tl('p.unknownStatus') }}</a-tag
                  >
                </a-tooltip>
              </a-col>
            </a-row>
          </template>

          <a-tooltip>
            <template #title>
              <div v-if="item.branchTagName">
                <div>{{ $tl('c.tagName') }}{{ item.branchTagName }}</div>
                <div>{{ $tl('c.lastCommitId') }}{{ item.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>{{ $tl('c.branchName') }}{{ item.branchName }}</div>
                <div>{{ $tl('c.lastCommitId') }}{{ item.repositoryLastCommitId }}</div>
              </div>
            </template>

            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $tl('p.groupTag') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.branchName }} {{ item.branchTagName }}</a-col
              >
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.resultDirFile">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">{{ $tl('p.product') }}</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.resultDirFile }}
              </a-col>
            </a-row>
          </a-tooltip>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $tl('p.buildId') }}:</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <a-tag v-if="item.buildId <= 0">-</a-tag>
              <a-tag v-else color="#108ee9" @click="handleBuildLog(item)">#{{ item.buildId }}</a-tag>
            </a-col>
          </a-row>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $tl('p.buildMethod') }}</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <template v-if="item.buildMode === 1">
                <CloudOutlined />
                {{ $tl('c.containerBuild') }}
              </template>
              <template v-else>
                <CodeOutlined />
                {{ $tl('c.localBuild') }}
              </template>
            </a-col>
          </a-row>
          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">{{ $tl('p.publishMethod') }}</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              {{ releaseMethodMap[item.releaseMethod] }}
            </a-col>
          </a-row>

          <a-row type="flex" align="middle" justify="center" style="margin-top: 10px">
            <a-button-group>
              <a-button
                v-if="item.status === 1 || item.status === 4 || item.status === 9"
                size="small"
                type="primary"
                danger
                @click="handleStopBuild(item)"
                >{{ $tl('c.stop') }}
              </a-button>
              <a-dropdown v-else>
                <a-button size="small" type="primary" @click="handleConfirmStartBuild(item)">
                  {{ $tl('c.build') }}
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, true)"
                        >{{ $tl('c.directBuild') }}
                      </a-button>
                    </a-menu-item>
                    <a-menu-item key="2">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, false)"
                      >
                        {{ $tl('c.backgroundBuild') }}
                      </a-button>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown>
                <a-button size="small" type="primary" @click="handleEdit(item)">{{ $tl('c.edit') }}</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleEdit(item, 0)">
                      <a href="javascript:;">{{ $tl('c.buildMethod') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 1)">
                      <a href="javascript:;">{{ $tl('c.basicInfo') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 2)">
                      <a href="javascript:;">{{ $tl('c.buildProcess') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 3)">
                      <a href="javascript:;">{{ $tl('c.publishOperation') }}</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 4)">
                      <a href="javascript:;">{{ $tl('c.otherConfig') }}</a>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-button size="small" @click="handleDelete(item)">{{ $tl('c.delete') }}</a-button>
              <a-tooltip placement="leftBottom" :title="$tl('c.clearCode')">
                <a-button size="small" :disabled="!item.sourceDirExist" @click="handleClear(item)"
                  >{{ $tl('undefined') }}
                </a-button>
              </a-tooltip>
            </a-button-group>
          </a-row>
        </a-card>
      </template>
      <template #tableBodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip
            placement="topLeft"
            :title="`${$tl('p.name')}${text} ${$tl('p.viewDetails')}`"
            @click="handleDetails(record)"
          >
            <a-button type="link" style="padding: 0" size="small"> <FullscreenOutlined />{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'branchName'">
          <a-tooltip placement="topLeft">
            <template #title>
              <div v-if="record.branchTagName">
                <div>{{ $tl('c.tagName') }}{{ record.branchTagName }}</div>
                <div>{{ $tl('c.lastCommitId') }}{{ record.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>{{ $tl('c.branchName') }}{{ text }}</div>
                <div>{{ $tl('c.lastCommitId') }}{{ record.repositoryLastCommitId }}</div>
              </div>
            </template>
            <span v-if="record.branchTagName"><TagOutlined />{{ record.branchTagName }}</span>
            <span v-else>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'buildMode'">
          <a-tooltip placement="topLeft" :title="text === 1 ? $tl('c.containerBuild') : $tl('c.localBuild')">
            <CloudOutlined v-if="text === 1" />
            <CodeOutlined v-else />
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <a-tooltip>
            <template #title>
              <ul>
                <li>{{ $tl('p.publishMethod') }}{{ releaseMethodMap[text] }}</li>
                <li>{{ $tl('p.artifactCatalog') }}{{ record.resultDirFile }}</li>
                <li v-if="record.buildMode !== 1">{{ $tl('p.buildCommand') }}{{ record.script }}</li>
              </ul>
            </template>
            <span>{{ releaseMethodMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || $tl('c.unknown')">
            <a-tag
              :color="statusColor[record.status]"
              :title="record.statusMsg || statusMap[text] || $tl('c.unknown')"
              >{{ statusMap[text] || $tl('c.unknown') }}</a-tag
            >
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildId'">
          <a-tooltip placement="topLeft" :title="text + ` ( ${$tl('p.viewLog')} ) `">
            <span v-if="record.buildId <= 0"></span>
            <a-tag v-else color="#108ee9" @click="handleBuildLog(record)">#{{ text }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.tooltip">
          <a-tooltip placement="topLeft" :title="text">
            <span>{{ text || '' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <a-space>
            <a-button
              v-if="record.status === 1 || record.status === 4 || record.status === 9"
              size="small"
              type="primary"
              danger
              @click="handleStopBuild(record)"
              >{{ $tl('c.stop') }}
            </a-button>
            <a-dropdown v-else>
              <a-button size="small" type="primary" @click="handleConfirmStartBuild(record)"
                >{{ $tl('c.build') }}<DownOutlined
              /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, true)"
                      >{{ $tl('c.directBuild') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item key="2">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, false)"
                      >{{ $tl('c.backgroundBuild') }}</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a-button size="small" type="primary" @click="handleEdit(record, 1)">{{ $tl('c.edit') }}</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleEdit(record, 0)">
                    <a href="javascript:;">{{ $tl('c.buildMethod') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 1)">
                    <a href="javascript:;">{{ $tl('c.basicInfo') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 2)">
                    <a href="javascript:;">{{ $tl('c.buildProcess') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 3)">
                    <a href="javascript:;">{{ $tl('c.publishOperation') }}</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 4)">
                    <a href="javascript:;">{{ $tl('c.otherConfig') }}</a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                {{ $tl('p.more') }}
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">{{ $tl('p.copy') }}</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.resultHasFile"
                      type="primary"
                      @click="handleDownloadFile(record)"
                      >{{ $tl('p.downloadArtifact') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">{{
                      $tl('c.delete')
                    }}</a-button>
                  </a-menu-item>

                  <a-menu-item>
                    <a-tooltip placement="leftBottom" :title="$tl('c.clearCode')">
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.sourceDirExist"
                        @click="handleClear(record)"
                        >{{ $tl('p.clearCode') }}
                      </a-button>
                    </a-tooltip>
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'top')"
                      >{{ $tl('p.pin') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >{{ $tl('p.moveUp') }}</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                    >
                      {{ $tl('p.moveDown') }}
                    </a-button>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
      <!-- <template #cardPageTool>
        <a-row type="flex" justify="center">
          <a-divider v-if="listQuery.total / listQuery.limit > 1" dashed />
          <a-col>
            <a-pagination
              v-model:current="listQuery.page"
              v-model:pageSize="listQuery.limit"
              :show-total="
                (total) => {
                  return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery)
                }
              "
              :show-size-changer="true"
              :page-size-options="sizeOptions"
              :total="listQuery.total"
              :hide-on-single-page="true"
              show-less-items
              @show-size-change="
                (current, size) => {
                  listQuery.limit = size
                  loadData()
                }
              "
              @change="loadData"
            />
          </a-col>
        </a-row>
      </template> -->
    </CustomTable>
    <!-- </template> -->
    <!-- </a-card> -->

    <!-- 编辑区 -->
    <build-item
      v-if="editBuildVisible != 0"
      :id="temp.id"
      :visible-type="editBuildVisible"
      :edit-steps="editSteps"
      :data="temp"
      @close="
        () => {
          editBuildVisible = 0
        }
      "
      @build="
        (build, buildId, buildEnvParameter) => {
          editBuildVisible = 0
          loadData()
          loadGroupList()
          if (build) {
            reqStartBuild({ id: buildId, buildEnvParameter: buildEnvParameter || temp.buildEnvParameter }, true)
          }
        }
      "
    ></build-item>
    <!-- 构建日志 -->
    <build-log
      v-if="buildLogVisible > 0"
      :temp="temp"
      :visible="buildLogVisible != 0"
      @close="
        () => {
          buildLogVisible = 0
        }
      "
    />
    <!-- 构建确认 -->
    <a-modal
      v-model:open="buildConfirmVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="40vw"
      :title="$tl('p.buildConfirmDialog')"
      :mask-closable="false"
      @ok="handleStartBuild"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item :label="$tl('p.name')" name="name">
          <a-input v-model:value="temp.name" read-only disabled />
        </a-form-item>
        <a-form-item :label="$tl('p.branch')" name="branchName">
          <custom-select
            v-model:value="temp.branchName"
            :data="branchList"
            :disabled="temp.branchTagName ? true : false"
            :can-reload="true"
            :input-placeholder="$tl('p.customBranchWildcard')"
            :select-placeholder="$tl('p.selectBranch')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $tl('c.xxx') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $tl('c.x') }}</li>
                  <li>* {{ $tl('c.y') }}</li>
                  <li>** {{ $tl('c.z') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item
          v-if="(branchTagList && branchTagList.length) || (temp.branchTagName && temp.branchTagName.length)"
          :label="$tl('p.tag')"
          name="branchTagName"
        >
          <custom-select
            v-model:value="temp.branchTagName"
            :data="branchTagList"
            :can-reload="true"
            :input-placeholder="$tl('p.customTagWildcard')"
            :select-placeholder="$tl('p.selectTag')"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                {{ $tl('c.xxx') }}(AntPathMatcher)
                <ul>
                  <li>? {{ $tl('c.x') }}</li>
                  <li>* {{ $tl('c.y') }}</li>
                  <li>** {{ $tl('c.z') }}</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item name="resultDirFile" :label="$tl('c.artifactCatalog')">
          <a-input v-model:value="temp.resultDirFile" :placeholder="$tl('p.notUpdate')" />
        </a-form-item>
        <a-form-item name="checkRepositoryDiff" :label="$tl('p.diffBuild')" help="">
          <a-space>
            <a-switch
              v-model:checked="temp.checkRepositoryDiff"
              :checked-children="$tl('p.yes')"
              :un-checked-children="$tl('p.no')"
            />
            <span>
              <a-tooltip>
                <template #title> {{ $tl('p.diffBuildDescription') }} </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              {{ $tl('p.onlyThisTime') }}
            </span>
          </a-space>
        </a-form-item>

        <a-form-item
          v-if="temp.releaseMethod === 1 || temp.releaseMethod === 2"
          name="projectSecondaryDirectory"
          :label="$tl('p.subDirectory')"
        >
          <a-input v-model:value="temp.projectSecondaryDirectory" :placeholder="$tl('p.notFillRoot')" />
        </a-form-item>
        <a-form-item :label="$tl('p.envVariable')" name="buildEnvParameter" :help="$tl('p.saveToCurrentBuild')">
          <a-textarea
            v-model:value="temp.buildEnvParameter"
            :placeholder="$tl('p.buildEnvVariable')"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item :label="$tl('p.buildNote')" name="buildRemark" :help="$tl('p.onlyThisBuild')">
          <a-textarea
            v-model:value="temp.buildRemark"
            :max-length="240"
            :placeholder="$tl('p.inputNote')"
            :auto-size="{ minRows: 2, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item
          v-if="dispatchProjectList && dispatchProjectList.length"
          name="selectProject"
          :label="$tl('p.filterProject')"
          :help="$tl('p.filterEffect')"
        >
          <a-select
            v-model:value="temp.dispatchSelectProjectArray"
            mode="multiple"
            :placeholder="$tl('p.selectProject')"
          >
            <a-select-option
              v-for="item in dispatchProjectList"
              :key="item.id"
              :value="`${item.projectId}@${item.nodeId}`"
            >
              {{ item.nodeName }}-{{ item.cacheProjectName || item.projectId }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import BuildLog from './log'
import BuildItem from './item'
import CustomSelect from '@/components/customSelect'
import { Empty } from 'ant-design-vue'
import {
  clearBuid,
  deleteBuild,
  getBuildGroupAll,
  getBuildList,
  releaseMethodMap,
  downloadBuildFileByBuild,
  startBuild,
  statusMap,
  statusColor,
  stopBuild,
  sortItem,
  getBranchList,
  deleteatchBuild
} from '@/api/build/build-info'
import { getDispatchProject } from '@/api/dispatch'

import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime
  // PAGE_DEFAULT_SHOW_TOTAL,
  // getCachePageLimit
} from '@/utils/const'

export default {
  components: {
    BuildLog,
    BuildItem,
    CustomSelect
  },
  props: {
    repositoryId: {
      type: String,
      default: ''
    },
    fullContent: {
      type: Boolean,
      default: true
    },
    choose: {
      // "radio"
      type: String,
      default: ''
    },
    layout: {
      type: String,
      default: ''
    }
  },
  emits: ['cancel', 'confirm'],
  data() {
    return {
      Empty,
      sizeOptions: ['8', '12', '16', '20', '24'],
      releaseMethodMap,
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      // 动态列表参数
      groupList: [],
      list: [],
      statusMap,
      statusColor,
      branchTagList: [],
      branchList: [],
      temp: {},
      // 页面控制变量
      editBuildVisible: 0,
      editSteps: null,
      buildLogVisible: 0,
      buildConfirmVisible: false,
      columns: [
        {
          title: this.$tl('p.name'),
          dataIndex: 'name',
          sorter: true,
          width: 200,
          ellipsis: true
        },
        {
          title: this.$tl('c.group'),
          dataIndex: 'group',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.branchTag'),
          dataIndex: 'branchName',
          ellipsis: true,
          width: 100
        },

        {
          title: this.$tl('p.method'),
          dataIndex: 'buildMode',
          align: 'center',
          width: '80px',
          sorter: true,
          ellipsis: true
        },
        {
          title: this.$tl('c.status'),
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.buildId'),
          dataIndex: 'buildId',
          width: '90px',
          ellipsis: true,
          align: 'center'
        },

        {
          title: this.$tl('c.publishMethod'),
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$tl('p.artifact'),
          dataIndex: 'resultDirFile',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.timingBuild'),
          dataIndex: 'autoBuildCron',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: this.$tl('p.modifier'),
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },

        {
          title: this.$tl('p.createTime'),
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: this.$tl('p.modifyTime'),
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: this.$tl('p.sortValue'),
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: this.$tl('p.operation'),
          dataIndex: 'operation',
          width: '200px',

          align: 'center',
          fixed: 'right'
        }
      ],

      // countdownTime: Date.now(),
      // refreshInterval: 5,
      tableSelections: [],
      dispatchProjectList: [],
      // layoutType: null,
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections,
        type: this.choose || 'checkbox'
      }
    }
  },
  watch: {},
  created() {
    // if (this.layout) {
    // this.layoutType = this.layout
    this.loadData()
    // } else {
    // this.changeLayout()
    // }
    this.loadGroupList()
    //
    // this.countdownTime = Date.now() + this.refreshInterval * 1000
  },
  methods: {
    CHANGE_PAGE,
    $tl(key, ...args) {
      return this.$t(`pages.build.listInfo.${key}`, ...args)
    },
    // PAGE_DEFAULT_SHOW_TOTAL,
    // getCachePageLimit,
    // 分组数据
    loadGroupList() {
      getBuildGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      this.listQuery.repositoryId = this.repositoryId
      this.loading = true
      getBuildList(this.listQuery)
        .then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
            // 重新计算倒计时
            // this.countdownTime = Date.now() + this.refreshInterval * 1000
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // silenceLoadData() {
    //   if (this.$attrs.routerUrl !== this.$route.path) {
    //     // 重新计算倒计时
    //     this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     return
    //   }
    //   this.loading = true
    //   getBuildList(this.listQuery, false)
    //     .then((res) => {
    //       if (res.code === 200) {
    //         this.list = res.data.result
    //         this.listQuery.total = res.data.total
    //         // 重新计算倒计时
    //         this.countdownTime = Date.now() + this.refreshInterval * 1000
    //       }
    //     })
    //     .finally(() => {
    //       this.loading = false
    //     })
    // },

    // 新增
    handleAdd() {
      this.temp = {}
      this.editBuildVisible = 2
      this.editSteps = 0
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record)
      delete temp.id
      delete temp.triggerToken
      temp.name = temp.name + this.$tl('p.duplicate')
      this.temp = temp
      this.editBuildVisible = 2
      this.editSteps = 1
      // this.handleEdit(temp, 1)
    },
    handleEdit(record, steps) {
      this.temp = { id: record.id, buildEnvParameter: record.buildEnvParameter }
      this.editBuildVisible = 2

      this.editSteps = steps
    },
    handleDetails(record) {
      this.editBuildVisible = 1
      this.editSteps = 2
      this.temp = { id: record.id, buildEnvParameter: record.buildEnvParameter }
    },
    loadBranchListById(id) {
      this.branchList = []
      this.branchTagList = []
      const params = {
        repositoryId: id
      }
      getBranchList(params).then((res) => {
        if (res.code === 200) {
          this.branchList = res.data?.branch || []
          this.branchTagList = res.data?.tags || []
        }
      })
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        content: this.$tl('p.deleteConfirm'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          // 删除
          return deleteBuild(record.id).then((res) => {
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
    // 批量删除
    handleBatchDelete() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$tl('c.noData')
        })
        return
      }
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        content: this.$tl('p.batchDeleteConfirm'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          // 删除
          return deleteatchBuild({ ids: this.tableSelections.join(',') }).then((res) => {
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
    // 清除构建
    handleClear(record) {
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        content: this.$tl('p.clearConfirm'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return clearBuid(record.id).then((res) => {
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
    // 开始构建
    handleConfirmStartBuild(record) {
      this.dispatchProjectList = []
      // 判断构建方式
      if (record.releaseMethod === 1) {
        // 节点分发
        getDispatchProject(record.releaseMethodDataId, true).then((res) => {
          if (res.code === 200) {
            this.dispatchProjectList = res.data?.projectList || []

            this.showBuildConfirm(record)
          }
        })
      } else {
        this.showBuildConfirm(record)
      }
      // console.log(record);
    },
    showBuildConfirm(record) {
      this.temp = Object.assign({}, record)
      this.buildConfirmVisible = true
      this.branchList = []
      this.branchTagList = []
      //
      try {
        const extraData = JSON.parse(record.extraData) || {}
        this.temp = {
          ...this.temp,
          checkRepositoryDiff: extraData.checkRepositoryDiff,
          projectSecondaryDirectory: extraData.projectSecondaryDirectory
        }
      } catch (e) {
        //
      }
    },
    handleStartBuild() {
      this.confirmLoading = true
      this.reqStartBuild(
        {
          id: this.temp.id,
          buildRemark: this.temp.buildRemark,
          resultDirFile: this.temp.resultDirFile,
          branchTagName: this.temp.branchTagName,
          branchName: this.temp.branchName,
          checkRepositoryDiff: this.temp.checkRepositoryDiff,
          projectSecondaryDirectory: this.temp.projectSecondaryDirectory,
          buildEnvParameter: this.temp.buildEnvParameter,
          dispatchSelectProject:
            (this.temp.dispatchSelectProjectArray && this.temp.dispatchSelectProjectArray.join(',')) || ''
        },
        true
      )
        .then(() => {
          this.buildConfirmVisible = false
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    reqStartBuild(data, openLog) {
      return new Promise((resolve) => {
        startBuild(data).then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.loadData()
            if (openLog) {
              // 自动打开构建日志
              this.handleBuildLog({
                id: data.id,
                buildId: res.data
              })
            }
            resolve()
          }
        })
      })
    },
    // 停止构建
    handleStopBuild(record) {
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        // TODO 后续抽优化
        content: this.$tl('p.cancelConfirm') + record.name + this.$tl('p.cancelConfirmEnd'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          this.temp = Object.assign({}, record)
          return stopBuild(this.temp.id).then((res) => {
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
    // 查看构建日志
    handleBuildLog(record) {
      this.temp = {
        id: record.id,
        buildId: record.buildId
      }
      this.buildLogVisible = new Date() * Math.random()
    },
    // 关闭日志对话框
    closeBuildLogModel() {
      this.loadData()
    },

    // 排序
    sortItemHander(record, index, method) {
      const msgData = {
        top: this.$tl('p.pinConfirm'),
        up: this.$tl('p.moveUpConfirm'),
        down: this.$tl('p.moveDownConfirm')
      }
      let msg = msgData[method] || this.$tl('p.confirmOperation')
      if (!record.sortValue) {
        msg += this.$tl('p.currentDataIsDefault')
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        content: msg,
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          return sortItem({
            id: record.id,
            method: method,
            compareId: compareId
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
    // 下载构建产物
    handleDownloadFile(record) {
      window.open(downloadBuildFileByBuild(record.id, record.buildId), '_blank')
    },
    // 批量构建
    batchBuild() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$tl('c.noData')
        })
        return
      }
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        content: this.$tl('p.batchBuildConfirm'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          this.tableSelections.forEach((item) => {
            startBuild({
              id: item
            }).then((res) => {
              if (res.code === 200) {
                //
              }
            })
          })
          this.tableSelections = []
          this.loadData()
        }
      })
    },
    // 批量取消构建
    batchCancel() {
      if (!this.tableSelections || this.tableSelections.length <= 0) {
        $notification.warning({
          message: this.$tl('c.noData')
        })
        return
      }
      $confirm({
        title: this.$tl('c.alert'),
        zIndex: 1009,
        content: this.$tl('p.batchCancelConfirm'),
        okText: this.$tl('c.confirm'),
        cancelText: this.$tl('c.cancel'),
        onOk: () => {
          this.tableSelections.forEach((item) => {
            stopBuild(item).then((res) => {
              if (res.code === 200) {
                //
              }
            })
          })
          this.tableSelections = []
          this.loadData()
        }
      })
    },
    // // 切换视图
    // changeLayout() {
    //   if (!this.layoutType) {
    //     const layoutType = localStorage.getItem('tableLayout')
    //     // 默认表格
    //     this.layoutType = layoutType === 'card' ? 'card' : 'table'
    //   } else {
    //     this.layoutType = this.layoutType === 'card' ? 'table' : 'card'
    //     localStorage.setItem('tableLayout', this.layoutType)
    //   }
    //   this.listQuery = {
    //     ...this.listQuery,
    //     limit: this.layoutType === 'card' ? 8 : getCachePageLimit()
    //   }
    //   this.loadData()
    // },
    // 选择确认
    handerConfirm() {
      if (!this.tableSelections.length) {
        $notification.warning({
          message: this.$tl('c.selectBuild')
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: this.$tl('c.selectBuild')
        })
        return
      }
      this.$emit('confirm', selectData)
    }
  }
}
</script>

<style scoped>
.item-info {
  padding: 4px 0;
}
/* :deep(.ant-statistic div) {
  display: inline-block;
  font-weight: normal;
}
:deep(.ant-statistic-content-value, .ant-statistic-content) {
  font-size: 16px;
} */
</style>
