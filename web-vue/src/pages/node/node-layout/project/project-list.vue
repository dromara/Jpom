<template>
  <div class="node-full-content">
    <!-- 数据表格 -->
    <a-table
      :data-source="list"
      size="middle"
      :columns="columns"
      :pagination="pagination"
      @change="
        (pagination, filters, sorter) => {
          this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });

          this.loadData();
        }
      "
      :row-selection="rowSelection"
      bordered
      rowKey="id"
    >
      <template slot="title">
        <a-space>
          <a-select v-model="listQuery.group" allowClear placeholder="请选择分组" class="search-input-item" @change="loadData">
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
              <a-menu-item key="1"> <a-button type="primary" @click="handlerImportTemplate()">下载导入模板</a-button> </a-menu-item>
            </a-menu>

            <a-upload name="file" accept=".csv" action="" :showUploadList="false" :multiple="false" :before-upload="beforeUpload">
              <a-button type="primary" icon="upload"> 导入 <a-icon type="down" /> </a-button>
            </a-upload>
          </a-dropdown>
          <a-tooltip>
            <template slot="title">
              <div>状态数据是异步获取有一定时间延迟</div>
            </template>
            <a-icon type="question-circle" theme="filled" />
          </a-tooltip>
        </a-space>
      </template>

      <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title="`名称：${text}`" @click="handleEdit(record)">
        <a-button type="link" style="padding: 0" size="small"><a-icon v-if="record.outGivingProject" type="apartment" />{{ text }} </a-button>
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

      <a-tooltip slot="port" slot-scope="text, record" placement="topLeft" :title="`进程号：${(record.pids || [record.pid || '-']).join(',')} / 端口号：${record.port || '-'}`">
        <span>{{ record.port || "-" }}/{{ (record.pids || [record.pid || "-"]).join(",") }}</span>
      </a-tooltip>

      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleFile(record)">文件</a-button>
          <a-tooltip :title="`${noFileModes.includes(record.runMode) ? '到控制台去管理项目状态' : 'File 类型项目不能使用控制台功能'}`">
            <a-button size="small" type="primary" @click="handleConsole(record)" :disabled="!noFileModes.includes(record.runMode)">控制台</a-button>
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
              <a-menu-item v-if="noFileModes.includes(record.runMode)">
                <a-button size="small" type="primary" @click="handleLogBack(record)">项目日志 </a-button>
              </a-menu-item>
              <template v-if="record.outGivingProject">
                <a-menu-item>
                  <a-tooltip title="节点分发项目需要到节点分发中去删除">
                    <a-button size="small" type="danger" :disabled="true">逻辑删除</a-button>
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
                  <a-button size="small" type="danger" @click="handleDelete(record)">逻辑删除</a-button>
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
      @ok="
        () => {
          this.$refs.edit.handleOk();
        }
      "
      :maskClosable="false"
    >
      <project-edit
        ref="edit"
        @close="
          () => {
            editProjectVisible = false;
            loadData();
          }
        "
        :data="temp"
        :nodeId="temp.nodeId"
        :projectId="temp.id"
      />
    </a-modal>
    <!-- 项目文件组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerFileVisible" @close="onFileClose">
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
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerConsoleVisible" @close="onConsoleClose">
      <console v-if="drawerConsoleVisible" :nodeId="node.id" :id="temp.id" :projectId="temp.projectId" :replica="replicaTemp" @goFile="goFile" />
    </a-drawer>
    <!-- 项目跟踪文件组件 -->
    <a-drawer destroyOnClose :title="drawerTitle" placement="right" width="85vw" :visible="drawerReadFileVisible" @close="onReadFileClose">
      <file-read v-if="drawerReadFileVisible" :nodeId="node.id" :readFilePath="temp.readFilePath" :id="temp.id" :projectId="temp.projectId" @goFile="goFile" />
    </a-drawer>

    <!-- 批量操作状态 -->
    <a-modal destroyOnClose v-model="batchVisible" :title="batchTitle" :footer="null" @cancel="batchClose">
      <a-list bordered :data-source="selectedRows">
        <a-list-item slot="renderItem" slot-scope="item">
          <a-list-item-meta :description="item.email">
            <a slot="title"> {{ item.name }}</a>
          </a-list-item-meta>
          <div>{{ item.cause === undefined ? "未开始" : item.cause }}</div>
        </a-list-item>
      </a-list>
    </a-modal>
    <!-- 日志备份 -->
    <a-modal destroyOnClose v-model="lobbackVisible" title="日志备份列表" width="850px" :footer="null" :maskClosable="false">
      <ProjectLog v-if="lobbackVisible" :nodeId="node.id" :projectId="temp.projectId"></ProjectLog>
    </a-modal>
  </div>
</template>
<script>
import File from "./project-file";
import Console from "./project-console";
import FileRead from "./project-file-read";

import ProjectEdit from "./project-edit";
import ProjectLog from "./project-log.vue";

import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, PROJECT_DSL_DEFATUL, parseTime } from "@/utils/const";

import {
  deleteProject,
  getProjectList,
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
  importData,
} from "@/api/node-project";

export default {
  props: {
    node: {
      type: Object,
    },
  },
  components: {
    File,
    Console,

    ProjectEdit,

    FileRead,
    ProjectLog,
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),

      groupList: [],
      runModeList,
      javaModes,
      noFileModes,
      PROJECT_DSL_DEFATUL,
      list: [],
      temp: {},
      replicaTemp: null,
      editProjectVisible: false,
      drawerTitle: "",
      drawerFileVisible: false,
      drawerConsoleVisible: false,

      drawerReplicaVisible: false,
      drawerReadFileVisible: false,
      // addGroupvisible: false,
      // libExist: false,
      selectedRows: [],
      selectedRowKeys: [],
      checkRecord: "",
      batchVisible: false,
      batchTitle: "",

      lobbackVisible: false,
    };
  },
  computed: {
    columns() {
      const columns = [
        { title: "项目名称", dataIndex: "name", width: 150, sorter: true, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "项目分组", dataIndex: "group", sorter: true, width: "100px", ellipsis: true, scopedSlots: { customRender: "group" } },
        {
          title: "项目路径",
          dataIndex: "path",
          ellipsis: true,
          scopedSlots: { customRender: "path" },
          width: 150,
        },
        { title: "运行方式", dataIndex: "runMode", sorter: true, width: "90px", ellipsis: true, align: "center", scopedSlots: { customRender: "runMode" } },

        // {
        //   title: "最后操作人",
        //   dataIndex: "modifyUser",
        //   width: 100,
        //   ellipsis: true,
        //   sorter: true,
        //   scopedSlots: { customRender: "modifyUser" },
        // },
        { title: "运行状态", dataIndex: "status", width: 80, ellipsis: true, align: "center", scopedSlots: { customRender: "status" } },
        { title: "端口/PID", dataIndex: "port", width: 100, ellipsis: true, scopedSlots: { customRender: "port" } },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          ellipsis: true,
          customRender: (text) => parseTime(text),
          width: "170px",
        },
        { title: "修改时间", sorter: true, dataIndex: "modifyTimeMillis", width: "170px", ellipsis: true, customRender: (text) => parseTime(text) },
      ];

      columns.push({ title: "操作", dataIndex: "operation", scopedSlots: { customRender: "operation" }, fixed: "right", align: "center", width: "180px" });
      return columns;
    },
    filePath() {
      return (this.temp.whitelistDirectory || "") + (this.temp.lib || "");
    },
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
    rowSelection() {
      return {
        onChange: this.onSelectChange,
        columnWidth: "40px",
        getCheckboxProps: this.getCheckboxProps,
        selectedRowKeys: this.selectedRowKeys,
        // hideDefaultSelections: true,
      };
    },
  },
  watch: {},
  mounted() {
    this.loadData();
  },
  methods: {
    parseTime,

    CHANGE_PAGE,

    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      this.listQuery.nodeId = this.node.id;
      getProjectList(this.listQuery).then((res1) => {
        if (res1.code === 200) {
          let resultList = res1.data.result;
          this.listQuery.total = res1.data.total;

          // TODO: 由于Ant Design Vue的bug，当表格中首行为disabled时，表格的全选按钮也无法选择。
          // 目前的解决方案是：把需要disabled的元素放到最后。
          // 如果运行模式是文件，则无需批量启动/重启/关闭
          let tempList = resultList.filter((item) => item.runMode !== "File");
          let fileList = resultList.filter((item) => item.runMode === "File");
          this.list = tempList.concat(fileList);

          // // 项目ID 字段更新
          // this.list = this.list.map((element) => {
          //   //element.dataId = element.id;
          //   //element.id = element.projectId;
          //   return element;
          // });

          let ids = tempList.map((item) => {
            return item.projectId;
          });
          // 如果 ids 有数据就继续请求
          if (ids.length > 0) {
            const tempParams = {
              nodeId: this.node.id,
              ids: JSON.stringify(ids),
            };
            getRuningProjectInfo(tempParams, "noTip").then((res2) => {
              if (res2.code === 200) {
                this.list = this.list.map((element) => {
                  if (res2.data[element.projectId]) {
                    element.port = res2.data[element.projectId].port;
                    element.pid = res2.data[element.projectId].pid;
                    element.pids = res2.data[element.projectId].pids;
                    element.status = element.pid > 0;
                    element.error = res2.data[element.projectId].error;
                  }
                  return element;
                });
                // this.list.forEach((element) => {});
              } else {
                // error
                this.list = this.list.map((element) => {
                  element.port = 0;
                  element.pid = 0;
                  element.status = false;
                  element.error = res2.msg;
                  return element;
                });
              }
            });
            //
          }
          this.loadGroupList();
        }
        this.loading = false;
      });
    },
    // 添加
    handleAdd() {
      this.temp = {
        id: "",
        nodeId: this.node.id,
      };

      this.editProjectVisible = true;
    },
    // 复制
    copyItem(record) {
      const temp = Object.assign({}, record);
      delete temp.id;
      delete temp.createTimeMillis;
      delete temp.outGivingProject;
      this.temp = { ...temp, name: temp.name + "副本", id: temp.projectId + "_copy", lib: temp.lib + "_copy" };

      this.editProjectVisible = true;
    },
    // 编辑
    handleEdit(record) {
      this.temp = {
        id: record.projectId,
        nodeId: this.node.id,
      };

      this.editProjectVisible = true;
    },

    // 文件管理
    handleFile(record) {
      this.checkRecord = record;
      this.temp = Object.assign({}, record);
      this.drawerTitle = `文件管理(${this.temp.name})`;
      this.drawerFileVisible = true;
    },
    // 关闭文件管理对话框
    onFileClose() {
      this.drawerFileVisible = false;
    },
    // 控制台
    handleConsole(record) {
      this.checkRecord = record;
      this.temp = Object.assign({}, record);
      this.drawerTitle = `控制台(${this.temp.name})`;
      this.drawerConsoleVisible = true;
      this.replicaTemp = null;
    },

    // 关闭控制台
    onConsoleClose() {
      this.drawerConsoleVisible = false;
      this.loadData();
    },

    // 删除
    handleDelete(record, thorough) {
      this.$confirm({
        title: "系统提示",
        content: thorough
          ? "真的要彻底删除项目么？彻底项目会自动删除项目相关文件奥(包含项目日志，日志备份，项目文件)"
          : "真的要删除项目么？删除项目不会删除项目相关文件奥,建议先清理项目相关文件再删除项目",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: record.projectId,
            thorough: thorough,
          };
          deleteProject(params).then((res) => {
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
    //         this.$notification.warning({
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
      this.drawerReadFileVisible = false;
    },
    // 跟踪文件
    goReadFile(path, filename) {
      this.onFileClose();
      this.drawerReadFileVisible = true;
      this.temp.readFilePath = (path + "/" + filename).replace(new RegExp("//", "gm"), "/");
      this.drawerTitle = `跟踪文件(${filename})`;
    },
    //前往控制台
    goConsole() {
      //关闭文件
      this.onFileClose();
      this.handleConsole(this.checkRecord);
    },
    //前往文件
    goFile() {
      // 关闭控制台
      this.onConsoleClose();
      this.onReadFileClose();
      this.handleFile(this.checkRecord);
    },
    // 获取复选框属性 判断是否可以勾选
    getCheckboxProps(record) {
      return {
        props: {
          disabled: record.runMode === "File",
          name: record.name,
        },
      };
    },
    //选中项目
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRows = selectedRows;
      this.selectedRowKeys = selectedRowKeys;
      //console.log(selectedRowKeys, selectedRows);
    },
    // onSelectAll() {

    // },

    batchClose() {
      this.batchVisible = false;
      this.selectedRowKeys = [];
      this.loadData();
    },
    //批量开始
    batchStart() {
      if (this.selectedRows.length <= 0) {
        this.$notification.warning({
          message: "请选中要启动的项目",
        });
        return;
      }
      this.batchVisible = true;
      this.batchTitle = "批量启动";
      this.batchStartInfo(1);
    },
    //批量启动详情
    batchStartInfo(count) {
      if (count > this.selectedRows.length) {
        return;
      }
      let value = this.selectedRows[count - 1];
      value.cause = "启动中";
      count++;
      if (value.runMode !== "File") {
        const params = {
          nodeId: this.node.id,
          id: value.projectId,
        };
        startProject(params)
          .then((data) => {
            value.cause = data.msg;
            this.selectedRows = [...this.selectedRows];
            this.batchStartInfo(count);
          })
          .catch(() => {
            value.cause = "启动失败";
            this.selectedRows = [...this.selectedRows];

            this.batchStartInfo(count);
          });
      } else {
        value.cause = "跳过";
        this.selectedRows = [...this.selectedRows];

        this.batchStartInfo(count);
      }
    },
    //批量重启
    batchRestart() {
      if (this.selectedRows.length <= 0) {
        this.$notification.warning({
          message: "请选中要重启的项目",
        });
        return;
      }
      this.batchVisible = true;
      this.batchTitle = "批量重新启动";
      this.batchRestartInfo(1);
    },
    //批量重启详情
    batchRestartInfo(count) {
      if (count > this.selectedRows.length) {
        return;
      }
      let value = this.selectedRows[count - 1];
      value.cause = "重新启动中";
      count++;
      if (value.runMode !== "File") {
        const params = {
          nodeId: this.node.id,
          id: value.projectId,
        };
        restartProject(params)
          .then((data) => {
            value.cause = data.msg;
            this.selectedRows = [...this.selectedRows];
            this.batchRestartInfo(count);
          })
          .catch(() => {
            value.cause = "重新启动失败";
            this.selectedRows = [...this.selectedRows];
            this.batchRestartInfo(count);
          });
      } else {
        value.cause = "跳过";
        this.selectedRows = [...this.selectedRows];
        this.batchRestartInfo(count);
      }
    },
    //批量关闭
    batchStop() {
      if (this.selectedRows.length <= 0) {
        this.$notification.warning({
          message: "请选中要关闭的项目",
        });
        return;
      }
      this.batchVisible = true;
      this.batchTitle = "批量关闭启动";
      this.batchStopInfo(1);
    },
    //批量关闭详情
    batchStopInfo(count) {
      if (count > this.selectedRowKeys.length) {
        return;
      }
      let value = this.selectedRows[count - 1];
      value.cause = "关闭中";
      count++;
      if (value.runMode !== "File") {
        const params = {
          nodeId: this.node.id,
          id: value.projectId,
        };
        stopProject(params)
          .then((data) => {
            value.cause = data.msg;
            this.selectedRows = [...this.selectedRows];

            this.batchStopInfo(count);
          })
          .catch(() => {
            value.cause = "关闭失败";
            this.selectedRows = [...this.selectedRows];
            this.batchStopInfo(count);
          });
      } else {
        value.cause = "跳过";
        this.selectedRows = [...this.selectedRows];
        this.batchStopInfo(count);
      }
    },

    // 删除
    handleDeleteCopy(project, record, thorough) {
      this.$confirm({
        title: "系统提示",
        content: thorough ? "真的要彻底删除项目副本么?彻底删除项目会自动删除副本相关文件奥(包含项目日志，日志备份)" : "真的要删除项目副本么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          const params = {
            nodeId: this.node.id,
            id: project.projectId,

            thorough: thorough,
          };
          deleteProject(params).then((res) => {
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
    // 释放分发
    handleReleaseOutgiving(project) {
      const html =
        "<b style='font-size: 20px;'>确定要释放当前项目的分发功能吗？</b>" +
        "<ul style='font-size: 20px;color:red;font-weight: bold;'>" +
        "<li>请慎重操作，否则会产生冗余数据。</b></li>" +
        "<li>一般用于误操作后将本删除转为普通项目再删除项目</li>" +
        "<li>如果关联的分发还存在再重新编辑对应分发后当前项目会再次切换为分发项目！！！</li>" +
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
          const params = {
            nodeId: this.node.id,
            id: project.projectId,
          };
          releaseOutgiving(params).then((res) => {
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
    // 下载导入模板
    handlerImportTemplate() {
      window.open(
        importTemplate({
          nodeId: this.node.id,
        }),
        "_blank"
      );
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), "_blank");
    },
    beforeUpload(file) {
      const formData = new FormData();
      formData.append("file", file);
      formData.append("nodeId", this.node.id);
      importData(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.loadData();
        }
      });
    },
    // 日志备份列表
    handleLogBack(record) {
      this.temp = Object.assign({}, record);
      this.lobbackVisible = true;
    },
  },
};
</script>
