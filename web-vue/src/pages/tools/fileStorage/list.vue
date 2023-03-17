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
            <a-input v-model="listQuery['id']" @pressEnter="loadData" placeholder="文件id" class="search-input-item" />
            <a-tooltip title="按住 Ctr 或者 Alt/Option 键点击按钮快速回到第一页">
              <a-button type="primary" :loading="loading" @click="loadData">搜索</a-button>
            </a-tooltip>
            <a-button type="primary" @click="handleUpload">上传文件</a-button>
          </a-space>
        </template>
        <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <a-popover slot="name" slot-scope="text, item" title="文件信息">
          <template slot="content">
            <p>文件名：{{ text }}</p>
            <p>文件描述：{{ item.description }}</p>
          </template>
          {{ text }}
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
          <a-tag v-if="text">存在</a-tag>
          <a-tag v-else>丢失</a-tag>
        </template>
        <template slot="global" slot-scope="text">
          <a-tag v-if="text === 'GLOBAL'">全局</a-tag>
          <a-tag v-else>工作空间</a-tag>
        </template>
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <a-button type="primary" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button type="danger" size="small" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal
        destroyOnClose
        v-model="uploadVisible"
        :closable="!uploading"
        :footer="uploading ? null : undefined"
        :keyboard="false"
        width="50%"
        :title="`上传文件`"
        @ok="handleUploadOk"
        :maskClosable="false"
      >
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
          <a-form-model-item label="保留天数">
            <a-input-number v-model="temp.keepDay" :min="1" style="width: 100%" placeholder="文件保存天数,默认 3650 天" />
          </a-form-model-item>
          <a-form-model-item label="文件共享">
            <a-radio-group v-model="temp.global">
              <a-radio :value="true"> 全局 </a-radio>
              <a-radio :value="false"> 当前工作空间 </a-radio>
            </a-radio-group>
          </a-form-model-item>
          <a-form-model-item label="文件描述">
            <a-textarea v-model="temp.description" placeholder="请输入文件描述" />
          </a-form-model-item>
        </a-form-model>
      </a-modal>
      <!-- 编辑文件 -->
      <a-modal destroyOnClose v-model="editVisible" width="50%" :title="`修改文件`" @ok="handleEditOk" :maskClosable="false">
        <a-form-model ref="editForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
          <a-form-model-item label="文件名">
            <a-input placeholder="文件名" v-model="temp.name" />
          </a-form-model-item>
          <a-form-model-item label="保留天数">
            <a-input-number v-model="temp.keepDay" :min="1" style="width: 100%" placeholder="文件保存天数,默认 3650 天" />
          </a-form-model-item>
          <a-form-model-item label="文件共享">
            <a-radio-group v-model="temp.global">
              <a-radio :value="true"> 全局 </a-radio>
              <a-radio :value="false"> 当前工作空间 </a-radio>
            </a-radio-group>
          </a-form-model-item>
          <a-form-model-item label="文件描述">
            <a-textarea v-model="temp.description" placeholder="请输入文件描述" />
          </a-form-model-item>
        </a-form-model>
      </a-modal>
    </div>
  </div>
</template>

<script>
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime, renderSize, formatDuration } from "@/utils/const";
import { fileStorageList, uploadFile, uploadFileMerge, fileEdit, hasFile, delFile, sourceMap } from "@/api/tools/file-storage";
import { uploadPieces } from "@/utils/upload-pieces";

export default {
  data() {
    return {
      loading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      list: [],
      columns: [
        { title: "文件MD5", dataIndex: "id", ellipsis: true, width: "100px", scopedSlots: { customRender: "tooltip" } },
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "大小", dataIndex: "size", sorter: true, ellipsis: true, scopedSlots: { customRender: "renderSize" }, width: "80px" },
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
        { title: "操作", dataIndex: "operation", ellipsis: true, scopedSlots: { customRender: "operation" }, width: 120 },
      ],
      rules: {
        name: [{ required: true, message: "请输入文件名称", trigger: "blur" }],
      },
      temp: {},
      sourceMap,
      fileList: [],
      percentage: 0,
      percentageInfo: {},
      uploading: false,
      uploadVisible: false,
      editVisible: false,
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
  },
};
</script>
<style scoped>
/deep/ .ant-progress-text {
  width: auto;
}
</style>
