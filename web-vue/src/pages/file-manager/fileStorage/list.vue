<template>
  <div class="full-content">
    <div>
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
        bordered
        rowKey="id"
      >
        <template slot="title">
          <a-space>
            <a-input v-model="listQuery['%name%']" @pressEnter="loadData" placeholder="文件名称" class="search-input-item" />
            <a-input v-model="listQuery['id']" @pressEnter="loadData" placeholder="文件id,精准搜索" class="search-input-item" />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleUpload">上传文件</a-button>
            <a-button type="primary" @click="handleRemoteDownload">远程下载</a-button>
          </a-space>
        </template>
        <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-tooltip slot="id" slot-scope="text, item" placement="topLeft" :title="text">
          <span v-if="item.status === 0 || item.status === 2">-</span>
          <span v-else>{{ text }}</span>
        </a-tooltip>
        <a-popover slot="name" slot-scope="text, item" title="文件信息">
          <template slot="content">
            <p>文件名：{{ text }}</p>
            <p>文件描述：{{ item.description }}</p>
            <p v-if="item.status !== undefined">下载状态：{{ statusMap[item.status] || "未知" }}</p>
            <p v-if="item.progressDesc">状态描述：{{ item.progressDesc }}</p>
          </template>
          <!-- {{ text }} -->
          <a-button type="link" style="padding: 0px" @click="handleEdit(item)" size="small">{{ text }}</a-button>
        </a-popover>

        <a-tooltip slot="renderSize" slot-scope="text" placement="topLeft" :title="renderSize(text)">
          <span>{{ renderSize(text) }}</span>
        </a-tooltip>
        <a-tooltip slot="source" slot-scope="text" placement="topLeft" :title="`${sourceMap[text] || '未知'}`">
          <span>{{ sourceMap[text] || "未知" }}</span>
        </a-tooltip>
        <a-tooltip slot="time" slot-scope="text" placement="topLeft" :title="parseTime(text)">
          <span>{{ parseTime(text, "{m}-{d} {h}:{i}") }}</span>
        </a-tooltip>

        <template slot="exists" slot-scope="text">
          <a-tag v-if="text" color="green">存在</a-tag>
          <a-tag v-else color="red">丢失</a-tag>
        </template>
        <template slot="global" slot-scope="text">
          <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
          <a-tag v-else>工作空间</a-tag>
        </template>
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <!-- <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button> -->
            <a-button size="small" :disabled="!record.exists" type="primary" @click="handleDownloadUrl(record)">下载</a-button>
            <a-button size="small" :disabled="!record.exists" type="primary" @click="handleReleaseFile(record)">发布</a-button>
            <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal destroyOnClose v-model="uploadVisible" :closable="!uploading" :footer="uploading ? null : undefined" :keyboard="false" :title="`上传文件`" @ok="handleUploadOk" :maskClosable="false">
        <a-form-model ref="form" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-model-item label="选择文件" prop="file">
            <a-progress v-if="percentage" :percent="percentage">
              <template #format="percent">
                {{ percent }}%
                <template v-if="percentageInfo.total"> ({{ renderSize(percentageInfo.total) }}) </template>
                <template v-if="percentageInfo.duration"> 用时:{{ formatDuration(percentageInfo.duration) }} </template>
              </template>
            </a-progress>

            <a-upload
              :file-list="fileList"
              :disabled="!!percentage"
              :remove="
                (file) => {
                  this.fileList = [];
                }
              "
              :before-upload="
                (file) => {
                  // 只允许上传单个文件
                  this.fileList = [file];
                  return false;
                }
              "
            >
              <a-icon type="loading" v-if="percentage" />
              <a-button v-else type="primary" icon="upload">选择文件</a-button>
            </a-upload>
          </a-form-model-item>
          <a-form-model-item label="保留天数" prop="keepDay">
            <a-input-number v-model="temp.keepDay" :min="1" style="width: 100%" placeholder="文件保存天数,默认 3650 天" />
          </a-form-model-item>
          <a-form-model-item label="文件共享" prop="global">
            <a-radio-group v-model="temp.global">
              <a-radio :value="true"> 全局 </a-radio>
              <a-radio :value="false"> 当前工作空间 </a-radio>
            </a-radio-group>
          </a-form-model-item>
          <a-form-model-item label="文件描述" prop="description">
            <a-textarea v-model="temp.description" placeholder="请输入文件描述" />
          </a-form-model-item>
        </a-form-model>
      </a-modal>
      <!-- 编辑文件 -->
      <a-modal destroyOnClose v-model="editVisible" :title="`修改文件`" @ok="handleEditOk" :maskClosable="false">
        <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-model-item label="文件名" prop="name">
            <a-input placeholder="文件名" v-model="temp.name" />
          </a-form-model-item>
          <a-form-model-item label="保留天数" prop="keepDay">
            <a-input-number v-model="temp.keepDay" :min="1" style="width: 100%" placeholder="文件保存天数,默认 3650 天" />
          </a-form-model-item>
          <a-form-model-item label="文件共享" prop="global">
            <a-radio-group v-model="temp.global">
              <a-radio :value="true"> 全局 </a-radio>
              <a-radio :value="false"> 当前工作空间 </a-radio>
            </a-radio-group>
          </a-form-model-item>
          <a-form-model-item label="文件描述" prop="description">
            <a-textarea v-model="temp.description" placeholder="请输入文件描述" />
          </a-form-model-item>
        </a-form-model>
      </a-modal>
      <!--远程下载  -->
      <a-modal destroyOnClose v-model="uploadRemoteFileVisible" title="远程下载文件" @ok="handleRemoteUpload" :maskClosable="false">
        <a-form-model :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }" :rules="rules" ref="remoteForm">
          <a-form-model-item label="远程下载URL" prop="url">
            <a-input v-model="temp.url" placeholder="远程下载地址" />
          </a-form-model-item>
          <a-form-model-item label="保留天数" prop="keepDay">
            <a-input-number v-model="temp.keepDay" :min="1" style="width: 100%" placeholder="文件保存天数,默认 3650 天" />
          </a-form-model-item>
          <a-form-model-item label="文件共享" prop="global">
            <a-radio-group v-model="temp.global">
              <a-radio :value="true"> 全局 </a-radio>
              <a-radio :value="false"> 当前工作空间 </a-radio>
            </a-radio-group>
          </a-form-model-item>
          <a-form-model-item label="文件描述" prop="description">
            <a-textarea v-model="temp.description" placeholder="请输入文件描述" />
          </a-form-model-item>
        </a-form-model>
      </a-modal>
      <!-- 断点下载 -->
      <a-modal destroyOnClose v-model="triggerVisible" title="断点/分片下载" width="50%" :footer="null" :maskClosable="false">
        <a-form-model ref="editTriggerForm" :model="temp" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-tabs default-active-key="1">
            <template slot="tabBarExtraContent">
              <a-tooltip title="重置下载 token 信息,重置后之前的下载 token 将失效">
                <a-button type="primary" size="small" @click="resetTrigger">重置</a-button>
              </a-tooltip>
            </template>
            <a-tab-pane key="1" tab="断点/分片下载">
              <a-alert
                v-clipboard:copy="`${temp.triggerDownloadUrl}`"
                v-clipboard:success="
                  () => {
                    tempVue.prototype.$notification.success({ message: '复制成功' });
                  }
                "
                v-clipboard:error="
                  () => {
                    tempVue.prototype.$notification.error({ message: '复制失败' });
                  }
                "
                type="info"
                :message="`下载地址(点击可以复制)`"
              >
                <template slot="description">
                  <a-tag>GET</a-tag> <span>{{ `${temp.triggerDownloadUrl}` }} </span>
                  <a-icon type="copy" />
                </template>
              </a-alert>
            </a-tab-pane>
          </a-tabs>
        </a-form-model>
      </a-modal>
      <a-modal destroyOnClose v-model="releaseFileVisible" title="发布文件" width="50%" :maskClosable="false" @ok="handleCrateTask">
        <a-form-model ref="releaseFileForm" :rules="releaseFileRules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-model-item label="任务名" prop="name">
            <a-input placeholder="请输入任务名" :maxLength="50" v-model="temp.name" />
          </a-form-model-item>

          <a-form-model-item label="发布方式" prop="taskType">
            <a-radio-group v-model="temp.taskType" @change="taskTypeChange">
              <a-radio :value="0"> SSH </a-radio>
              <a-radio :value="1" :disabled="true"> 节点 </a-radio>
            </a-radio-group>
          </a-form-model-item>

          <a-form-model-item prop="taskDataIds" label="发布的SSH" v-if="temp.taskType === 0">
            <a-row>
              <a-col :span="22">
                <a-select show-search option-filter-prop="children" mode="multiple" v-model="temp.taskDataIds" placeholder="请选择SSH">
                  <a-select-option v-for="ssh in sshList" :key="ssh.id">
                    <a-tooltip :title="ssh.name"> {{ ssh.name }}</a-tooltip>
                  </a-select-option>
                </a-select>
              </a-col>
              <a-col :span="1" style="margin-left: 10px">
                <a-icon type="reload" @click="loadSshList" />
              </a-col>
            </a-row>
          </a-form-model-item>

          <a-form-model-item prop="releasePathParent" label="发布目录">
            <a-input-group compact>
              <a-select show-search allowClear style="width: 30%" v-model="temp.releasePathParent" placeholder="请选择发布的一级目录">
                <a-select-option v-for="item in accessList" :key="item">
                  <a-tooltip :title="item">{{ item }}</a-tooltip>
                </a-select-option>
                <a-icon slot="suffixIcon" type="reload" @click="loadAccesList" />
              </a-select>

              <a-input style="width: 70%" v-model="temp.releasePathSecondary" placeholder="请填写发布的二级目录" />
            </a-input-group>
          </a-form-model-item>

          <a-form-model-item label="执行脚本" prop="releaseBeforeCommand">
            <a-tabs tabPosition="right">
              <a-tab-pane key="before" tab="上传前">
                <div style="height: 40vh; overflow-y: scroll">
                  <code-editor v-model="temp.beforeScript" :options="{ mode: temp.taskType === 0 ? 'shell' : '', tabSize: 2, theme: 'abcdef' }"></code-editor>
                </div>
                <div style="margin-top: 10px">文件上传前需要执行的脚本(非阻塞命令)</div>
              </a-tab-pane>
              <a-tab-pane key="after" tab="上传后">
                <div style="height: 40vh; overflow-y: scroll">
                  <code-editor v-model="temp.afterScript" :options="{ mode: temp.taskType === 0 ? 'shell' : '', tabSize: 2, theme: 'abcdef' }"></code-editor>
                </div>
                <div style="margin-top: 10px">文件上传成功后需要执行的脚本(非阻塞命令)</div>
              </a-tab-pane>
            </a-tabs>
          </a-form-model-item>
        </a-form-model>
      </a-modal>
    </div>
  </div>
</template>

<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize, formatDuration } from "@/utils/const";
import { fileStorageList, uploadFile, uploadFileMerge, fileEdit, hasFile, delFile, sourceMap, remoteDownload, statusMap, triggerUrl } from "@/api/file-manager/file-storage";
import { uploadPieces } from "@/utils/upload-pieces";
import Vue from "vue";
import { getSshListAll } from "@/api/ssh";
import { getDispatchWhiteList } from "@/api/dispatch";
import codeEditor from "@/components/codeEditor";
import { addReleaseTask } from "@/api/file-manager/release-task-log";

export default {
  components: {
    codeEditor,
  },
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        { title: "文件MD5", dataIndex: "id", ellipsis: true, width: "100px", scopedSlots: { customRender: "id" } },
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "大小", dataIndex: "size", sorter: true, ellipsis: true, scopedSlots: { customRender: "renderSize" }, width: "100px" },
        { title: "后缀", dataIndex: "extName", ellipsis: true, scopedSlots: { customRender: "tooltip" }, width: "80px" },
        { title: "共享", dataIndex: "workspaceId", ellipsis: true, scopedSlots: { customRender: "global" }, width: "80px" },
        { title: "来源", dataIndex: "source", ellipsis: true, scopedSlots: { customRender: "source" }, width: "80px" },
        {
          title: "过期天数",
          dataIndex: "validUntil",
          sorter: true,
          customRender: (text) => {
            if (!text) {
              return "-";
            }
            return Math.floor((new Date(Number(text)).getTime() - Date.now()) / (60 * 60 * 24 * 1000));
          },
          width: "100px",
        },
        { title: "文件状态", dataIndex: "exists", ellipsis: true, scopedSlots: { customRender: "exists" }, width: "80px" },
        { title: "创建人", dataIndex: "createUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: "120px" },
        { title: "修改人", dataIndex: "modifyUser", ellipsis: true, scopedSlots: { customRender: "modifyUser" }, width: "120px" },
        {
          title: "创建时间",
          dataIndex: "createTimeMillis",
          sorter: true,
          scopedSlots: { customRender: "time" },
          width: "100px",
        },
        {
          title: "修改时间",
          dataIndex: "modifyTimeMillis",
          sorter: true,
          scopedSlots: { customRender: "time" },
          width: "100px",
        },
        { title: "操作", dataIndex: "operation", align: "center", ellipsis: true, scopedSlots: { customRender: "operation" }, fixed: "right", width: "170px" },
      ],
      rules: {
        name: [{ required: true, message: "请输入文件名称", trigger: "blur" }],
        url: [{ required: true, message: "请输入远程地址", trigger: "blur" }],
      },
      releaseFileRules: {
        name: [{ required: true, message: "请输入文件任务名", trigger: "blur" }],
        taskType: [{ required: true, message: "请选择发布方式", trigger: "blur" }],
        releasePath: [{ required: true, message: "请选择发布的一级目录和填写二级目录", trigger: "blur" }],
        taskDataIds: [{ required: true, message: "请选择发布的SSH", trigger: "blur" }],
      },
      temp: {},
      sourceMap,
      statusMap,
      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,
      uploadRemoteFileVisible: false,
      tempVue: null,
      triggerVisible: false,
      releaseFileVisible: false,
      sshList: [],
      accessList: [],
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    CHANGE_PAGE,
    renderSize,
    formatDuration,
    parseTime,
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      fileStorageList(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    handleUpload() {
      this.temp = {
        global: false,
      };
      this.uploadVisible = true;
      this.$refs["form"]?.resetFields();
    },
    // 上传文件
    handleUploadOk() {
      // 检验表单
      this.$refs["form"].validate((valid) => {
        if (!valid) {
          return false;
        }

        // 判断文件
        if (this.fileList.length === 0) {
          this.$notification.error({
            message: "请选择文件",
          });
          return false;
        }
        this.percentage = 0;
        this.percentageInfo = {};
        this.uploading = true;
        uploadPieces({
          file: this.fileList[0],
          uploadBeforeAbrot: (md5) => {
            return new Promise((resolve) => {
              hasFile({
                fileSumMd5: md5,
              }).then((res) => {
                if (res.code === 200) {
                  if (res.data) {
                    //
                    this.$notification.warning({
                      message: `当前文件已经存在啦,文件名：${res.data.name} ,是否共享：${res.data.workspaceId === "GLOBAL" ? "是" : "否"}`,
                    });
                    //
                    this.uploading = false;
                  } else {
                    resolve();
                  }
                }
              });
            });
          },
          process: (process, end, total, duration) => {
            this.percentage = Math.max(this.percentage, process);
            this.percentageInfo = { end, total, duration };
          },
          success: (uploadData) => {
            // 准备合并
            uploadFileMerge(Object.assign({}, { ...uploadData[0] }, this.temp))
              .then((res) => {
                if (res.code === 200) {
                  this.fileList = [];
                  this.loadData();
                  this.uploadVisible = false;
                }
                setTimeout(() => {
                  this.percentage = 0;
                  this.percentageInfo = {};
                }, 2000);
                this.uploading = false;
              })
              .catch(() => {
                this.uploading = false;
              });
          },
          error: (msg) => {
            this.$notification.error({
              message: msg,
            });
            this.uploading = false;
          },
          uploadCallback: (formData) => {
            return new Promise((resolve, reject) => {
              // 上传文件
              uploadFile(formData)
                .then((res) => {
                  if (res.code === 200) {
                    resolve();
                  } else {
                    reject();
                  }
                })
                .catch(() => {
                  reject();
                });
            });
          },
        });

        return true;
      });
    },
    // 编辑
    handleEdit(item) {
      this.temp = { ...item, global: item.workspaceId === "GLOBAL" };
      this.editVisible = true;
      this.$refs["editForm"]?.resetFields();
    },
    // 编辑确认
    handleEditOk() {
      this.$refs["editForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        fileEdit(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.editVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 删除文件
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除当前文件么？" + record.name,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          delFile({
            id: record.id,
          }).then((res) => {
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
    // 远程下载
    handleRemoteDownload() {
      this.uploadRemoteFileVisible = true;
      this.temp = {
        global: false,
      };
      this.$refs["remoteForm"]?.resetFields();
    },
    // 开始远程下载
    handleRemoteUpload() {
      //
      this.$refs["remoteForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        remoteDownload(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.uploadRemoteFileVisible = false;
            this.loadData();
          }
        });
      });
    },
    // 下载地址
    handleDownloadUrl(record) {
      this.temp = Object.assign({}, record);
      this.tempVue = Vue;
      triggerUrl({
        id: record.id,
      }).then((res) => {
        if (res.code === 200) {
          this.fillDownloadUrlResult(res);
          this.triggerVisible = true;
        }
      });
    },
    // 重置触发器
    resetTrigger() {
      triggerUrl({
        id: this.temp.id,
        rest: "rest",
      }).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.fillDownloadUrlResult(res);
        }
      });
    },
    fillDownloadUrlResult(res) {
      this.temp = { ...this.temp, triggerDownloadUrl: `${location.protocol}//${location.host}${res.data.triggerDownloadUrl}` };
    },
    // 发布文件
    handleReleaseFile(record) {
      this.releaseFileVisible = true;
      this.temp = { fileId: record.id, taskType: 0, stepsCurrent: 0 };
      this.taskTypeChange(0);
      this.loadAccesList();
    },
    taskTypeChange(value) {
      this.temp = { ...this.temp, taskDataIds: undefined };
      if (value === 0) {
        this.loadSshList();
      }
    },
    // 创建任务
    handleCrateTask() {
      this.$refs["releaseFileForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        addReleaseTask({ ...this.temp, taskDataIds: this.temp.taskDataIds?.join(",") }).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });

            this.releaseFileVisible = false;
            this.loadData();
          }
        });
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
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = [];
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data;
            resolve();
          }
        });
      });
    },
  },
};
</script>
<style scoped>
/deep/ .ant-progress-text {
  width: auto;
}
</style>
