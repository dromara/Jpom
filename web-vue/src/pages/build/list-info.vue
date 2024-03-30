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
      empty-description="没有任何构建"
      :active-page="activePage"
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
            placeholder="构建名称"
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
            placeholder="状态"
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
            placeholder="发布方式"
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
            placeholder="分组"
            class="search-input-item"
          >
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-input
            v-model:value="listQuery['%resultDirFile%']"
            allow-clear
            class="search-input-item"
            placeholder="产物目录"
            @press-enter="loadData"
          />
          <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
            <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
          </a-tooltip>
          <a-button type="primary" @click="handleAdd">新增</a-button>
          <template v-if="tableSelections && tableSelections.length">
            <a-dropdown>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1" @click="batchBuild"> 批量构建 </a-menu-item>
                  <a-menu-item key="2" @click="batchCancel"> 批量取消 </a-menu-item>
                  <a-menu-item key="3" @click="handleBatchDelete"> 批量删除 </a-menu-item>
                </a-menu>
              </template>
              <a-button type="primary"> 批量操作<DownOutlined /> </a-button>
            </a-dropdown>
          </template>
          <a-tooltip v-else title="表格视图才能使用批量操作功能">
            <a-button :disabled="true" type="primary"> 批量操作 <DownOutlined /> </a-button>
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
                  :title="`当前状态：${statusMap[item.status]} ${item.statusMsg ? '状态消息：' + item.statusMsg : ''} `"
                >
                  <a-tag :color="statusColor[item.status]" style="margin-right: 0">
                    {{ statusMap[item.status] || '未知状态' }}</a-tag
                  >
                </a-tooltip>
              </a-col>
            </a-row>
          </template>

          <a-tooltip>
            <template #title>
              <div v-if="item.branchTagName">
                <div>标签名称：{{ item.branchTagName }}</div>
                <div>上次构建基于 commitId：{{ item.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>分支名称：{{ item.branchName }}</div>
                <div>上次构建基于 commitId：{{ item.repositoryLastCommitId }}</div>
              </div>
            </template>

            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">分组/标签:</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.branchName }} {{ item.branchTagName }}</a-col
              >
            </a-row>
          </a-tooltip>
          <a-tooltip :title="item.resultDirFile">
            <a-row class="item-info">
              <a-col :span="6" class="title text-overflow-hidden">产物:</a-col>
              <a-col :span="18" class="content text-overflow-hidden">
                {{ item.resultDirFile }}
              </a-col>
            </a-row>
          </a-tooltip>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">构建ID:</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <a-tag v-if="item.buildId <= 0">-</a-tag>
              <a-tag v-else color="#108ee9" @click="handleBuildLog(item)">#{{ item.buildId }}</a-tag>
            </a-col>
          </a-row>

          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">构建方式:</a-col>
            <a-col :span="18" class="content text-overflow-hidden">
              <template v-if="item.buildMode === 1">
                <CloudOutlined />
                容器构建
              </template>
              <template v-else>
                <CodeOutlined />
                本地构建
              </template>
            </a-col>
          </a-row>
          <a-row class="item-info">
            <a-col :span="6" class="title text-overflow-hidden">发布方式:</a-col>
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
                >停止
              </a-button>
              <a-dropdown v-else>
                <a-button size="small" type="primary" @click="handleConfirmStartBuild(item)">
                  构建
                  <DownOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="1">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, true)"
                        >直接构建
                      </a-button>
                    </a-menu-item>
                    <a-menu-item key="2">
                      <a-button
                        size="small"
                        type="primary"
                        @click="reqStartBuild({ id: item.id, buildEnvParameter: item.buildEnvParameter }, false)"
                      >
                        后台构建
                      </a-button>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-dropdown>
                <a-button size="small" type="primary" @click="handleEdit(item)">编辑</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleEdit(item, 0)">
                      <a href="javascript:;">构建方式</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 1)">
                      <a href="javascript:;">基础信息</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 2)">
                      <a href="javascript:;">构建流程</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 3)">
                      <a href="javascript:;">发布操作</a>
                    </a-menu-item>
                    <a-menu-item @click="handleEdit(item, 4)">
                      <a href="javascript:;">其他配置</a>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
              <a-button size="small" @click="handleDelete(item)">删除</a-button>
              <a-tooltip
                placement="leftBottom"
                title="清除代码(仓库目录)为删除服务器中存储仓库目录里面的所有东西,删除后下次构建将重新拉起仓库里面的文件,一般用于解决服务器中文件和远程仓库中文件有冲突时候使用。执行时间取决于源码目录大小和文件数量如超时请耐心等待，或稍后重试"
              >
                <a-button size="small" :disabled="!item.sourceDirExist" @click="handleClear(item)">清除代码 </a-button>
              </a-tooltip>
            </a-button-group>
          </a-row>
        </a-card>
      </template>
      <template #tableBodyCell="{ column, text, record, index }">
        <template v-if="column.dataIndex === 'name'">
          <a-tooltip placement="topLeft" :title="`名称：${text} 点击查看详情`" @click="handleDetails(record)">
            <a-button type="link" style="padding: 0" size="small"> <FullscreenOutlined />{{ text }}</a-button>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'branchName'">
          <a-tooltip placement="topLeft">
            <template #title>
              <div v-if="record.branchTagName">
                <div>标签名称：{{ record.branchTagName }}</div>
                <div>上次构建基于 commitId：{{ record.repositoryLastCommitId }}</div>
              </div>
              <div v-else>
                <div>分支名称：{{ text }}</div>
                <div>上次构建基于 commitId：{{ record.repositoryLastCommitId }}</div>
              </div>
            </template>
            <span v-if="record.branchTagName"><TagOutlined />{{ record.branchTagName }}</span>
            <span v-else>{{ text }}</span>
          </a-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'buildMode'">
          <a-tooltip placement="topLeft" :title="text === 1 ? '容器构建' : '本地构建'">
            <CloudOutlined v-if="text === 1" />
            <CodeOutlined v-else />
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'releaseMethod'">
          <a-tooltip>
            <template #title>
              <ul>
                <li>发布方式：{{ releaseMethodMap[text] }}</li>
                <li>产物目录：{{ record.resultDirFile }}</li>
                <li v-if="record.buildMode !== 1">构建命令：{{ record.script }}</li>
              </ul>
            </template>
            <span>{{ releaseMethodMap[text] }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tooltip :title="record.statusMsg || statusMap[text] || '未知'">
            <a-tag :color="statusColor[record.status]" :title="record.statusMsg || statusMap[text] || '未知'">{{
              statusMap[text] || '未知'
            }}</a-tag>
          </a-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'buildId'">
          <a-tooltip placement="topLeft" :title="text + ' ( 点击查看日志 ) '">
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
              >停止
            </a-button>
            <a-dropdown v-else>
              <a-button size="small" type="primary" @click="handleConfirmStartBuild(record)"
                >构建<DownOutlined
              /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="1">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, true)"
                      >直接构建</a-button
                    >
                  </a-menu-item>
                  <a-menu-item key="2">
                    <a-button
                      size="small"
                      type="primary"
                      @click="reqStartBuild({ id: record.id, buildEnvParameter: record.buildEnvParameter }, false)"
                      >后台构建</a-button
                    >
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a-button size="small" type="primary" @click="handleEdit(record, 1)">编辑</a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleEdit(record, 0)">
                    <a href="javascript:;">构建方式</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 1)">
                    <a href="javascript:;">基础信息</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 2)">
                    <a href="javascript:;">构建流程</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 3)">
                    <a href="javascript:;">发布操作</a>
                  </a-menu-item>
                  <a-menu-item @click="handleEdit(record, 4)">
                    <a href="javascript:;">其他配置</a>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-dropdown>
              <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
                更多
                <DownOutlined />
              </a>
              <template #overlay>
                <a-menu>
                  <a-menu-item>
                    <a-button size="small" type="primary" @click="copyItem(record)">复制</a-button>
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      :disabled="!record.resultHasFile"
                      type="primary"
                      @click="handleDownloadFile(record)"
                      >下载产物</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button size="small" type="primary" danger @click="handleDelete(record)">删除</a-button>
                  </a-menu-item>

                  <a-menu-item>
                    <a-tooltip
                      placement="leftBottom"
                      title="清除代码(仓库目录)为删除服务器中存储仓库目录里面的所有东西,删除后下次构建将重新拉起仓库里面的文件,一般用于解决服务器中文件和远程仓库中文件有冲突时候使用。执行时间取决于源码目录大小和文件数量如超时请耐心等待，或稍后重试"
                    >
                      <a-button
                        size="small"
                        type="primary"
                        danger
                        :disabled="!record.sourceDirExist"
                        @click="handleClear(record)"
                        >清除代码
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
                      >置顶</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) <= 1"
                      @click="sortItemHander(record, index, 'up')"
                      >上移</a-button
                    >
                  </a-menu-item>
                  <a-menu-item>
                    <a-button
                      size="small"
                      type="primary"
                      :disabled="(listQuery.page - 1) * listQuery.limit + (index + 1) === listQuery.total"
                      @click="sortItemHander(record, index, 'down')"
                    >
                      下移
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
      title="构建确认弹窗"
      :mask-closable="false"
      @ok="handleStartBuild"
    >
      <a-form :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="名称" name="name">
          <a-input v-model:value="temp.name" read-only disabled />
        </a-form-item>
        <a-form-item label="分支" name="branchName">
          <custom-select
            v-model:value="temp.branchName"
            :data="branchList"
            :disabled="temp.branchTagName ? true : false"
            :can-reload="true"
            input-placeholder="自定义分支通配表达式"
            select-placeholder="请选择构建对应的分支"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                支持通配符(AntPathMatcher)
                <ul>
                  <li>? 匹配一个字符</li>
                  <li>* 匹配零个或多个字符</li>
                  <li>** 匹配路径中的零个或多个目录</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item
          v-if="(branchTagList && branchTagList.length) || (temp.branchTagName && temp.branchTagName.length)"
          label="标签(TAG)"
          name="branchTagName"
        >
          <custom-select
            v-model:value="temp.branchTagName"
            :data="branchTagList"
            :can-reload="true"
            input-placeholder="自定义标签通配表达式"
            select-placeholder="选择构建的标签,不选为最新提交"
            @on-refresh-select="loadBranchListById(temp.repositoryId)"
          >
            <template #inputTips>
              <div>
                支持通配符(AntPathMatcher)
                <ul>
                  <li>? 匹配一个字符</li>
                  <li>* 匹配零个或多个字符</li>
                  <li>** 匹配路径中的零个或多个目录</li>
                </ul>
              </div>
            </template>
          </custom-select>
        </a-form-item>
        <a-form-item name="resultDirFile" label="产物目录">
          <a-input v-model:value="temp.resultDirFile" placeholder="不填写则不更新" />
        </a-form-item>
        <a-form-item name="checkRepositoryDiff" label="差异构建" help="">
          <a-space>
            <a-switch v-model:checked="temp.checkRepositoryDiff" checked-children="是" un-checked-children="否" />
            <span>
              <a-tooltip>
                <template #title> 差异构建是指构建时候是否判断仓库代码有变动，如果没有变动则不执行构建 </template>
                <QuestionCircleOutlined />
              </a-tooltip>
              该选项仅本次构建生效
            </span>
          </a-space>
        </a-form-item>

        <a-form-item
          v-if="temp.releaseMethod === 1 || temp.releaseMethod === 2"
          name="projectSecondaryDirectory"
          label="二级目录"
        >
          <a-input v-model:value="temp.projectSecondaryDirectory" placeholder="不填写则发布至项目的根目录" />
        </a-form-item>
        <a-form-item label="环境变量" name="buildEnvParameter" help="配置后将保存到当前构建">
          <a-textarea
            v-model:value="temp.buildEnvParameter"
            placeholder="请输入构建环境变量：xx=abc 多个变量回车换行即可"
            :auto-size="{ minRows: 3, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item label="构建备注" name="buildRemark" help="填写备注仅本次构建生效">
          <a-textarea
            v-model:value="temp.buildRemark"
            :max-length="240"
            placeholder="请输入构建备注,长度小于 240"
            :auto-size="{ minRows: 2, maxRows: 5 }"
          />
        </a-form-item>
        <a-form-item
          v-if="dispatchProjectList && dispatchProjectList.length"
          name="selectProject"
          label="筛选项目"
          help="筛选之后本次发布操作只发布筛选项,并且只对本次操作生效"
        >
          <a-select v-model:value="temp.dispatchSelectProjectArray" mode="multiple" placeholder="请选择指定发布的项目">
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
} from '@/api/build-info'
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
          title: '名称',
          dataIndex: 'name',
          sorter: true,
          width: 200,
          ellipsis: true
        },
        {
          title: '分组',
          dataIndex: 'group',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '分支/标签',
          dataIndex: 'branchName',
          ellipsis: true,
          width: 100
        },

        {
          title: '方式',
          dataIndex: 'buildMode',
          align: 'center',
          width: '80px',
          sorter: true,
          ellipsis: true
        },
        {
          title: '状态',
          dataIndex: 'status',
          align: 'center',
          width: '100px',
          ellipsis: true
        },
        {
          title: '构建 ID',
          dataIndex: 'buildId',
          width: '90px',
          ellipsis: true,
          align: 'center'
        },

        {
          title: '发布方式',
          dataIndex: 'releaseMethod',
          width: '100px',
          ellipsis: true
        },
        {
          title: '产物',
          dataIndex: 'resultDirFile',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '定时构建',
          dataIndex: 'autoBuildCron',
          width: 100,
          ellipsis: true,
          tooltip: true
        },
        {
          title: '修改人',
          dataIndex: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },

        {
          title: '创建时间',
          dataIndex: 'createTimeMillis',
          sorter: true,
          ellipsis: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: '修改时间',
          dataIndex: 'modifyTimeMillis',
          sorter: true,
          customRender: ({ text }) => parseTime(text),
          width: '160px'
        },
        {
          title: '排序值',
          dataIndex: 'sortValue',
          sorter: true,
          width: '80px'
        },
        {
          title: '操作',
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
      temp.name = temp.name + '副本'
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
        title: '系统提示',
        zIndex: 1009,
        content: '真的要删除构建信息么？删除也将同步删除所有的构建历史记录信息',
        okText: '确认',
        cancelText: '取消',
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
          message: '没有选择任何数据'
        })
        return
      }
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '真的要批量删除这些构建信息么？删除也将同步删除所有的构建历史记录信息，如果中途删除失败将终止删除操作',
        okText: '确认',
        cancelText: '取消',
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
        title: '系统提示',
        zIndex: 1009,
        content: '真的要清除构建信息么？',
        okText: '确认',
        cancelText: '取消',
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
        title: '系统提示',
        zIndex: 1009,
        content: '确定要取消构建 【名称：' + record.name + '】 吗？注意：取消/停止构建不一定能正常关闭所有关联进程',
        okText: '确认',
        cancelText: '取消',
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
        top: '确定要将此数据置顶吗？',
        up: '确定要将此数上移吗？',
        down: '确定要将此数据下移吗？下移操作可能因为列表后续数据没有排序值操作无效！'
      }
      let msg = msgData[method] || '确定要操作吗？'
      if (!record.sortValue) {
        msg += ' 当前数据为默认状态,操后上移或者下移可能不会达到预期排序,还需要对相关数据都操作后才能达到预期排序'
      }
      // console.log(this.list, index, this.list[method === "top" ? index : method === "up" ? index - 1 : index + 1]);
      const compareId = this.list[method === 'top' ? index : method === 'up' ? index - 1 : index + 1].id
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: msg,
        okText: '确认',
        cancelText: '取消',
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
          message: '没有选择任何数据'
        })
        return
      }
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content:
          '确定要取批量构建吗？注意：同时运行多个构建将占用较大的资源,请慎重使用批量构建,如果批量构建的数量超多构建任务队列等待数，构建任务将自动取消',
        okText: '确认',
        cancelText: '取消',
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
          message: '没有选择任何数据'
        })
        return
      }
      $confirm({
        title: '系统提示',
        zIndex: 1009,
        content: '确定要取批量消选中的构建吗？注意：取消/停止构建不一定能正常关闭所有关联进程',
        okText: '确认',
        cancelText: '取消',
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
          message: '请选择要使用的构建'
        })
        return
      }
      const selectData = this.list.filter((item) => {
        return this.tableSelections.indexOf(item.id) > -1
      })
      if (!selectData.length) {
        $notification.warning({
          message: '请选择要使用的构建'
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
