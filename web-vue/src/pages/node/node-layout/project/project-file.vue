<template>
  <div>
    <!-- 布局 -->
    <a-layout class="file-layout node-full-content">
      <!-- 目录树 -->
      <a-layout-sider theme="light" class="sider" width="25%">
        <div class="dir-container">
          <a-space>
            <a-button size="small" type="primary" @click="loadData">刷新目录</a-button>
            <a-button size="small" type="primary" v-if="showConsole" @click="goConsole" v-show="noFileModes.includes(runMode)">控制台</a-button>
            <a-button size="small" type="primary" @click="backupList">备份列表</a-button>
          </a-space>
        </div>

        <a-directory-tree :replace-fields="treeReplaceFields" @select="nodeClick" :loadData="onTreeData" :treeData="treeList"></a-directory-tree>
      </a-layout-sider>
      <!-- 表格 -->
      <a-layout-content class="file-content">
        <a-table :data-source="fileList" size="middle" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
          <template slot="title">
            <!-- <a-tag color="#2db7f5">项目目录: {{ absPath }}</a-tag>-->
            <a-space>
              <a-dropdown :disabled="!Object.keys(this.tempNode).length">
                <a-button size="small" type="primary" @click="(e) => e.preventDefault()"><a-icon type="upload" />上传</a-button>
                <a-menu slot="overlay">
                  <a-menu-item @click="handleUpload">
                    <a-space><a-icon type="file" />上传文件</a-space>
                  </a-menu-item>
                  <a-menu-item @click="handleZipUpload">
                    <a-space><a-icon type="file-zip" />上传压缩包并自动解压</a-space>
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
              <a-dropdown :disabled="!Object.keys(this.tempNode).length">
                <a-button size="small" type="primary" @click="(e) => e.preventDefault()">新建</a-button>
                <a-menu slot="overlay">
                  <a-menu-item @click="handleAddFile(1)">
                    <a-space>
                      <a-icon type="folder-add" />
                      <a-space>新建目录</a-space>
                    </a-space>
                  </a-menu-item>
                  <a-menu-item @click="handleAddFile(2)">
                    <a-space>
                      <a-icon type="file-add" />
                      <a-space>新建空白文件</a-space>
                    </a-space>
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
              <a-tooltip title="通过 URL 下载远程文件到项目文件夹,需要到节点系统配置->白名单配置中配置允许的 HOST 白名单">
                <a-button size="small" type="primary" @click="openRemoteUpload"><a-icon type="cloud-download" /></a-button>
              </a-tooltip>
              <a-tooltip title="刷新文件表格">
                <a-button size="small" type="primary" @click="loadFileList"><a-icon type="reload" /></a-button>
              </a-tooltip>
              <a-tooltip title="清空当前目录文件">
                <a-button size="small" type="danger" @click="clearFile"><a-icon type="delete" /></a-button>
              </a-tooltip>

              <a-tag color="#2db7f5" v-if="uploadPath">当前目录: {{ uploadPath || "" }}</a-tag>
              <div>文件名栏支持右键菜单</div>
            </a-space>
          </template>
          <a-tooltip slot="filename" slot-scope="text, record" placement="topLeft" :title="text">
            <a-dropdown :trigger="['contextmenu']">
              <div>{{ text }}</div>
              <a-menu slot="overlay">
                <a-menu-item key="1">
                  <a-button icon="bars" @click="goReadFile(record)" :disabled="!record.textFileEdit" type="link"> 阅读文件 </a-button>
                </a-menu-item>
                <a-menu-item key="2">
                  <a-button icon="highlight" @click="handleRenameFile(record)" type="link"> 重命名 </a-button>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
          </a-tooltip>
          <a-tooltip slot="isDirectory" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text ? "目录" : "文件" }}</span>
          </a-tooltip>
          <a-tooltip slot="fileSize" slot-scope="text" placement="topLeft" :title="text">
            <span>{{ text }}</span>
          </a-tooltip>
          <template slot="operation" slot-scope="text, record">
            <a-space>
              <template v-if="record.isDirectory">
                <a-tooltip title="目录不能编辑">
                  <a-button size="small" type="primary" :disabled="true">编辑</a-button>
                </a-tooltip>
                <a-tooltip title="不能下载目录">
                  <a-button size="small" type="primary" :disabled="true">下载</a-button>
                </a-tooltip>
              </template>
              <template v-else>
                <a-tooltip title="需要到 节点管理中的系统管理的白名单配置中配置允许编辑的文件后缀">
                  <a-button size="small" type="primary" :disabled="!record.textFileEdit" @click="handleEditFile(record)">编辑</a-button>
                </a-tooltip>
                <a-button size="small" type="primary" @click="handleDownload(record)">下载</a-button>
              </template>
              <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </a-table>
        <!-- 批量上传文件 -->
        <a-modal v-model="uploadFileVisible" width="300px" title="上传项目文件" :footer="null" :maskClosable="false">
          <a-upload
            :file-list="uploadFileList"
            :remove="
              () => {
                this.uploadFileList = [];
              }
            "
            :before-upload="beforeUpload"
            multiple
          >
            <a-button><a-icon type="upload" />选择文件</a-button>
          </a-upload>
          <br />
          <a-progress v-if="percentage" :percent="percentage" status="success"></a-progress>
          <br />
          <a-space>
            <a-button type="primary" :disabled="fileUploadDisabled" @click="startUpload">开始上传</a-button>
            <a-tag color="green" :visible="successSize !== 0" :closable="true" class="successTag"> 上传成功: {{ successSize }} 个文件! </a-tag>
          </a-space>
        </a-modal>
        <!-- 上传压缩文件 -->
        <a-modal v-model="uploadZipFileVisible" width="300px" title="上传压缩文件" :footer="null" :maskClosable="false">
          <a-upload
            :file-list="uploadFileList"
            :remove="
              () => {
                this.uploadFileList = [];
              }
            "
            :before-upload="beforeZipUpload"
            :accept="ZIP_ACCEPT"
          >
            <a-button><a-icon type="upload" />选择压缩文件</a-button>
          </a-upload>
          <br />
          <a-switch v-model="checkBox" checked-children="清空覆盖" un-checked-children="不清空" style="margin-bottom: 10px" />
          <br />
          <a-progress v-if="percentage" :percent="percentage" status="success"></a-progress>
          <br />
          <a-space>
            <a-button type="primary" :disabled="fileUploadDisabled" @click="startZipUpload">开始上传</a-button>
            <a-tag color="green" :visible="successSize !== 0" :closable="true" class="successTag"> 上传成功: {{ successSize }} 个文件! </a-tag>
          </a-space>
        </a-modal>

        <a-modal v-model="editFileVisible" width="80vw" :title="`编辑文件 ${filename}`" :maskClosable="true">
          <div style="height: 60vh">
            <code-editor showTool v-model="fileContent" :fileSuffix="filename"></code-editor>
          </div>

          <template slot="footer">
            <!-- @ok="updateFileData" @cancel="handleCloseModal" -->
            <a-button @click="handleCloseModal"> 关闭 </a-button>
            <a-button type="primary" @click="updateFileData"> 保存 </a-button>
            <a-button
              type="primary"
              @click="
                () => {
                  updateFileData();
                  handleCloseModal();
                }
              "
            >
              保存并关闭
            </a-button>
          </template>
        </a-modal>
        <!--远程下载  -->
        <a-modal v-model="uploadRemoteFileVisible" title="远程下载文件" @ok="handleRemoteUpload" @cancel="openRemoteUpload" :maskClosable="false">
          <a-form-model :model="remoteDownloadData" :label-col="{ span: 5 }" :wrapper-col="{ span: 24 }" :rules="rules" ref="ruleForm">
            <a-form-model-item label="远程下载URL" prop="url">
              <a-input v-model="remoteDownloadData.url" placeholder="远程下载地址" />
            </a-form-model-item>
            <a-form-model-item label="是否为压缩包">
              <a-switch v-model="remoteDownloadData.unzip" checked-children="是" un-checked-children="否" v-decorator="['unzip', { valuePropName: 'checked' }]" />
            </a-form-model-item>
          </a-form-model>
        </a-modal>
        <!-- 创建文件/文件夹 -->
        <a-modal v-model="addFileFolderVisible" width="300px" :title="addFileOrFolderType === 1 ? '新增目录' : '新建文件'" :footer="null" :maskClosable="true">
          <a-space direction="vertical" style="width: 100%">
            <span v-if="uploadPath">当前目录:{{ uploadPath }}</span>
            <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->
            <a-tooltip :title="this.addFileOrFolderType === 1 ? '目录创建成功后需要手动刷新右边树才能显示出来哟' : ''">
              <a-input v-model="fileFolderName" placeholder="输入文件或者文件夹名" />
            </a-tooltip>
            <a-row type="flex" justify="center">
              <a-button type="primary" :disabled="fileFolderName.length === 0" @click="startAddFileFolder">确认</a-button>
            </a-row>
          </a-space>
        </a-modal>
        <!-- 从命名文件/文件夹 -->
        <a-modal v-model="renameFileFolderVisible" width="300px" :title="`重命名`" :footer="null" :maskClosable="true">
          <a-space direction="vertical" style="width: 100%">
            <a-input v-model="fileFolderName" placeholder="输入新名称" />

            <a-row type="flex" justify="center">
              <a-button type="primary" :disabled="fileFolderName.length === 0" @click="renameFileFolder">确认</a-button>
            </a-row>
          </a-space>
        </a-modal>
      </a-layout-content>
    </a-layout>
    <!-- 查看备份列表 -->
    <a-modal
      v-model="backupListVisible"
      width="80vw"
      height="80vh"
      title="备份列表"
      :footer="null"
      :maskClosable="true"
      @cancel="
        () => {
          loadData();
        }
      "
    >
      <projectFileBackup v-if="backupListVisible" :nodeId="this.nodeId" :projectId="this.projectId"></projectFileBackup>
    </a-modal>
  </div>
</template>
<script>
import {deleteProjectFile, downloadProjectFile, getFileList, newFileFolder, noFileModes, readFile, remoteDownload, renameFileFolder, updateFile, uploadProjectFile} from "@/api/node-project";
import {ZIP_ACCEPT} from "@/utils/const";
import codeEditor from "@/components/codeEditor";
import projectFileBackup from "./project-file-backup.vue";

export default {
  components: {
    codeEditor,
    projectFileBackup,
  },
  props: {
    nodeId: {
      type: String,
    },
    projectId: {
      type: String,
    },
    runMode: {
      type: String,
    },
    absPath: {
      type: String,
    },
    showConsole: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      ZIP_ACCEPT: ZIP_ACCEPT,
      noFileModes: noFileModes,
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      filename: "",
      uploadFileVisible: false,
      uploadZipFileVisible: false,
      uploadRemoteFileVisible: false,
      editFileVisible: false,
      backupListVisible: false,
      successSize: 0,
      fileContent: "",
      treeReplaceFields: {
        title: "filename",
        isLeaf: "isDirectory",
      },
      cmOptions: {
        mode: "application/json",
      },
      // 是否是上传状态
      uploading: false,
      percentage: 0,
      checkBox: false,
      // tableHeight: "80vh",
      defaultProps: {
        children: "children",
        label: "filename",
      },
      remoteDownloadData: {
        id: "",
        url: "",
        unzip: false,
      },
      columns: [
        { title: "文件名称", dataIndex: "filename", ellipsis: true, scopedSlots: { customRender: "filename" } },
        { title: "文件类型", dataIndex: "isDirectory", width: 100, ellipsis: true, scopedSlots: { customRender: "isDirectory" } },
        { title: "文件大小", dataIndex: "fileSize", width: 120, ellipsis: true, scopedSlots: { customRender: "fileSize" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 180, ellipsis: true },
        { title: "操作", dataIndex: "operation", width: 180, align: "center", scopedSlots: { customRender: "operation" } },
      ],
      rules: {
        url: [{ required: true, message: "远程下载Url不为空", trigger: "change" }],
      },
      addFileFolderVisible: false,
      // 目录1 文件2 标识
      addFileOrFolderType: 1,
      fileFolderName: "",
      oldFileFolderName: "",
      renameFileFolderVisible: false,
    };
  },
  computed: {
    fileUploadDisabled() {
      return this.uploadFileList.length === 0 || this.uploading;
    },
    uploadPath() {
      if (!Object.keys(this.tempNode).length) {
        return "";
      }
      if (this.tempNode.level === 1) {
        return "";
      } else {
        return (this.tempNode.levelName || "") + "/" + this.tempNode.filename;
      }
    },
  },
  mounted() {
    this.loadData();
  },
  methods: {
    handleEditFile(record) {
      this.editFileVisible = true;
      this.loadFileData(record.filename);
      this.filename = record.filename;
    },

    // 关闭编辑器弹窗
    handleCloseModal() {
      this.fileContent = "";
      this.editFileVisible = false;
    },
    onTreeData(treeNode) {
      return new Promise((resolve) => {
        if (treeNode.dataRef.children || !treeNode.dataRef.isDirectory) {
          resolve();
          return;
        }
        this.loadNode(treeNode.dataRef, resolve);
      });
    },
    // 加载数据
    loadData() {
      const key = "root-" + new Date().getTime();
      this.treeList = [
        {
          filename: "目录：" + (this.absPath || ""),
          level: 1,
          isDirectory: true,
          key: key,
          isLeaf: false,
        },
      ];
      // 设置默认展开第一个
      setTimeout(() => {
        const node = this.treeList[0];
        this.tempNode = node;
        this.expandKeys = [key];
        this.loadFileList();
      }, 1000);
    },
    // 加载子节点
    loadNode(data, resolve) {
      this.tempNode = data;
      // 如果是目录
      if (data.isDirectory) {
        setTimeout(() => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            path: this.uploadPath,
          };
          // if (node.level === 1) {
          //   params.path = ''
          // } else {
          //   params.path = data.levelName || '' + '/' + data.filename
          // }
          // 加载文件
          getFileList(params).then((res) => {
            if (res.code === 200) {
              const treeData = res.data
                .filter((ele) => {
                  return ele.isDirectory;
                })
                .map((ele) => {
                  ele.isLeaf = !ele.isDirectory;
                  ele.key = ele.filename + "-" + new Date().getTime();
                  return ele;
                });
              data.children = treeData;

              this.treeList = [...this.treeList];
              resolve();
            } else {
              resolve();
            }
          });
        }, 500);
      } else {
        resolve();
      }
    },

    // 读取文件数据
    loadFileData(filename) {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename,
      };

      readFile(params).then((res) => {
        if (res.code === 200) {
          setTimeout(() => {
            this.fileContent = res.data;
          }, 300);
        }
      });
    },

    updateFileData() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        filePath: this.uploadPath,
        filename: this.filename,
        fileText: this.fileContent,
      };

      updateFile(params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },

    // 点击树节点
    nodeClick(selectedKeys, { node }) {
      if (node.dataRef.isDirectory) {
        this.tempNode = node.dataRef;
        this.loadFileList();
      }
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.error({
          message: "请选择一个节点",
        });
        return;
      }
      //初始化成功数
      this.successSize = 0;
      this.uploadFileList = [];
      this.uploading = false;
      this.percentage = 0;
      this.uploadFileVisible = true;
      this.timer && clearInterval(this.timer);
    },
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file);
      const newFileList = this.uploadFileList.slice();
      newFileList.splice(index, 1);
      this.uploadFileList = newFileList;
    },
    beforeUpload(file) {
      this.successSize = 0;
      this.uploadFileList = [...this.uploadFileList, file];
      return false;
    },
    // 开始上传文件
    startUpload() {
      this.$notification.info({
        message: "正在上传文件，请稍后...",
      });
      // 设置上传状态
      this.uploading = true;
      this.timer = setInterval(() => {
        this.percentage = this.percentage > 99 ? 99 : this.percentage + 1;
      }, 1000);

      // 遍历上传文件
      this.uploadFileList.forEach((file, index) => {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("nodeId", this.nodeId);
        formData.append("id", this.projectId);
        // 计算属性 uploadPath
        formData.append("levelName", this.uploadPath);

        // 上传文件
        uploadProjectFile(formData).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.successSize++;
          }
          // 判断是否全部上传完成
          if (index === this.uploadFileList.length - 1) {
            this.percentage = 100;
            setTimeout(() => {
              this.percentage = 0;
              this.uploading = false;
              clearInterval(this.timer);
              this.loadFileList();
              this.uploadFileList = [];
            }, 1000);
          }
        });
      });
    },
    // 上传压缩文件
    handleZipUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.error({
          message: "请选择一个节点",
        });
        return;
      }
      this.checkBox = false;
      this.successSize = 0;
      this.uploadFileList = [];
      this.uploading = false;
      this.percentage = 0;
      this.uploadZipFileVisible = true;
      this.timer && clearInterval(this.timer);
    },

    beforeZipUpload(file) {
      this.uploadFileList = [file];
      return false;
    },
    // 开始上传压缩文件
    startZipUpload() {
      this.$notification.info({
        message: "正在上传文件，请稍后...",
      });
      // 设置上传状态
      this.uploading = true;
      this.timer = setInterval(() => {
        this.percentage = this.percentage > 99 ? 99 : this.percentage + 1;
      }, 1000);

      // 上传文件
      const file = this.uploadFileList[0];
      const formData = new FormData();
      formData.append("file", file);
      formData.append("nodeId", this.nodeId);
      formData.append("id", this.projectId);
      // 计算属性 uploadPath
      formData.append("levelName", this.uploadPath);
      formData.append("type", "unzip");
      formData.append("clearType", this.checkBox ? "clear" : "noClear");
      // 上传文件
      uploadProjectFile(formData).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.successSize++;
          this.percentage = 100;
          setTimeout(() => {
            this.percentage = 0;
            this.uploading = false;
            clearInterval(this.timer);
            this.checkBox = false;
            this.uploadFileList = [];
            this.loadFileList();
          }, 1000);
        }
        this.percentage = 0;
      });
    },
    //打开远程上传
    openRemoteUpload() {
      // this.$refs.ruleForm.resetFields();
      this.uploadRemoteFileVisible = true;
    },
    //关闭远程上传
    closeRemoteUpload() {
      //   this.$refs.ruleForm.resetFields();
      this.uploadRemoteFileVisible = false;
    },
    //处理上传文件
    handleRemoteUpload() {
      this.$refs.ruleForm.validate((valid) => {
        if (valid) {
          const params = {
            id: this.projectId,
            nodeId: this.nodeId,
            url: this.remoteDownloadData.url,
            levelName: this.uploadPath,
            unzip: this.remoteDownloadData.unzip,
          };
          remoteDownload(params).then((res) => {
            if (res.code == 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.remoteDownloadData = {};
              this.uploadRemoteFileVisible = false;
              this.loadFileList();
            }
          });
        } else {
          return false;
        }
      });
    },
    // 加载文件列表
    loadFileList() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.warn({
          message: "请选择一个节点",
        });
        return false;
      }
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        path: this.uploadPath,
      };
      this.fileList = [];
      this.loading = true;
      // 加载文件
      getFileList(params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          res.data.forEach((element) => {
            // if (!element.isDirectory) {
            // 设置文件表格
            this.fileList.push({
              ...element,
            });
            // }
          });
        }
        this.loading = false;
      });
    },
    // 清空文件
    clearFile() {
      const msg = this.uploadPath ? "真的要清空 【" + this.uploadPath + "】目录和文件么？" : "真的要清空项目目录和文件么？";
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            type: "clear",
            levelName: this.uploadPath,
          };
          // 删除
          deleteProjectFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadFileList();
            }
          });
        },
      });
    },
    // 下载
    handleDownload(record) {
      this.$notification.info({
        message: "正在下载，请稍等...",
      });
      // 请求参数
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: record.levelName,
        filename: record.filename,
      };
      // 请求接口拿到 blob
      downloadProjectFile(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", record.filename);
        document.body.appendChild(link);
        link.click();
      });
    },
    // 删除
    handleDelete(record) {
      const msg = record.isDirectory ? "真的要删除【" + record.filename + "】文件夹么？" : "真的要删除【" + record.filename + "】文件么？";
      this.$confirm({
        title: "系统提示",
        content: msg,
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            nodeId: this.nodeId,
            id: this.projectId,
            levelName: record.levelName,
            filename: record.filename,
          };
          // 删除
          deleteProjectFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              this.loadData();
              // this.loadFileList();
            }
          });
        },
      });
    },
    goConsole() {
      this.$emit("goConsole");
    },
    goReadFile(record) {
      // const filePath = this.uploadPath + record.filename;
      this.$emit("goReadFile", this.uploadPath, record.filename);
    },
    handleAddFile(type) {
      this.addFileFolderVisible = true;
      this.addFileOrFolderType = type;
      this.fileFolderName = "";
    },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: this.uploadPath,
        filename: this.fileFolderName,
        unFolder: this.addFileOrFolderType === 1 ? false : true,
      };
      newFileFolder(params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.addFileFolderVisible = false;
          this.loadData();
          // this.loadFileList();
        }
      });
    },
    handleRenameFile(record) {
      this.renameFileFolderVisible = true;
      this.fileFolderName = record.filename;
      this.oldFileFolderName = record.filename;
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        nodeId: this.nodeId,
        id: this.projectId,
        levelName: this.uploadPath,
        newname: this.fileFolderName,
        filename: this.oldFileFolderName,
      };
      renameFileFolder(params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.renameFileFolderVisible = false;
          this.loadFileList();
        }
      });
    },
    // 查看备份列表
    backupList() {
      this.backupListVisible = true;
    },
  },
};
</script>

<style scoped>
.file-layout {
  padding: 0;
}
.sider {
  border: 1px solid #e2e2e2;
  height: calc(100vh - 80px);
  overflow-y: auto;
}
.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.file-content {
  height: calc(100vh - 100px);
  overflow-y: auto;
  margin: 10px 10px 0;
  padding: 10px;
  background-color: #fff;
}
.successTag {
  height: 32px;
  line-height: 30px;
}
</style>
