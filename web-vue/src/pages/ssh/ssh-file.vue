<template>
  <!-- 布局 -->
  <a-layout class="ssh-file-layout">
    <!-- 目录树 -->
    <a-layout-sider theme="light" class="sider" width="25%">
      <a-empty v-if="treeList.length === 0" />
      <a-directory-tree :treeData="treeList" :replaceFields="replaceFields" @select="onSelect"></a-directory-tree>
    </a-layout-sider>
    <!-- 表格 -->
    <a-layout-content class="file-content">
      <!-- <div ref="filter" class="filter"></div> -->
      <a-table size="middle" :data-source="fileList" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
        <template slot="title">
          <a-space>
            <a-dropdown :disabled="!this.tempNode.parentDir">
              <a-button size="small" type="primary" @click="(e) => e.preventDefault()">上传</a-button>
              <a-menu slot="overlay">
                <a-menu-item @click="handleUpload">
                  <a-space><a-icon type="file-add" />上传文件</a-space>
                </a-menu-item>
                <a-menu-item @click="handleUploadZip">
                  <a-space><a-icon type="file-zip" />上传压缩文件（自动解压）</a-space>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
            <a-dropdown :disabled="!this.tempNode.parentDir">
              <a-button size="small" type="primary" @click="(e) => e.preventDefault()">新建</a-button>
              <a-menu slot="overlay">
                <a-menu-item @click="handleAddFolder">
                  <a-space>
                    <a-icon type="folder-add" />
                    <a-space>新建目录</a-space>
                  </a-space>
                </a-menu-item>
                <a-menu-item @click="handleAddFile">
                  <a-space>
                    <a-icon type="file-add" />
                    <a-space>新建空白文件</a-space>
                  </a-space>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
            <a-button size="small" :disabled="!this.tempNode.parentDir" type="primary" @click="loadFileList()">刷新</a-button>
            <a-button size="small" :disabled="!this.tempNode.parentDir" type="danger" @click="handleDeletePath()">删除</a-button>
            <span v-if="this.nowPath">当前目录:{{ this.nowPath }}</span>
            <!-- <span v-if="this.nowPath">{{ this.tempNode.parentDir }}</span> -->
          </a-space>
        </template>
        <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title="text">
          <a-dropdown :trigger="['contextmenu']">
            <div>{{ text }}</div>
            <a-menu slot="overlay">
              <a-menu-item key="2">
                <a-button icon="highlight" @click="handleRenameFile(record)" type="link"> 重命名 </a-button>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

          <!-- <span>{{ text }}</span> -->
        </a-tooltip>
        <a-tooltip slot="dir" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text ? "目录" : "文件" }}</span>
        </a-tooltip>
        <a-tooltip slot="size" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <a-tooltip title="需要到 ssh 信息中配置允许编辑的文件后缀">
              <a-button size="small" type="primary" :disabled="!record.textFileEdit" @click="handleEdit(record)">编辑</a-button>
            </a-tooltip>
            <!-- <a-button type="primary" :disabled="!record.textFileEdit" @click="handlePreview(record)">跟踪</a-button> -->
            <a-button size="small" type="primary" @click="handleDownload(record)">下载</a-button>
            <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal @cancel="closeUploadFile" v-model="uploadFileVisible" width="300px" title="上传文件" :footer="null" :maskClosable="true">
        <a-upload :file-list="uploadFileList" :remove="handleRemove" :before-upload="beforeUpload" :accept="`${uploadFileZip ? ZIP_ACCEPT : ''}`" :multiple="!uploadFileZip">
          <a-button>
            <a-icon type="upload" />
            选择文件
            {{ uploadFileZip ? "压缩包" : "" }}
          </a-button>
        </a-upload>
        <br />
        <a-button type="primary" :disabled="uploadFileList.length === 0" @click="startUpload">开始上传</a-button>
      </a-modal>
      <!--  新增文件 目录    -->
      <a-modal v-model="addFileFolderVisible" width="300px" :title="addFileOrFolderType === 1 ? '新增目录' : '新建文件'" :footer="null" :maskClosable="true">
        <a-space direction="vertical" style="width: 100%">
          <span v-if="this.nowPath">当前目录:{{ this.nowPath }}</span>
          <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->
          <a-tooltip :title="this.addFileOrFolderType === 1 ? '目录创建成功后需要手动刷新右边树才能显示出来哟' : ''">
            <a-input v-model="fileFolderName" placeholder="输入文件或者文件夹名" />
          </a-tooltip>
          <a-row type="flex" justify="center">
            <a-button type="primary" :disabled="fileFolderName.length === 0" @click="startAddFileFolder">确认</a-button>
          </a-row>
        </a-space>
      </a-modal>
      <!-- Terminal -->
      <a-modal v-model="terminalVisible" width="50%" title="Terminal" :footer="null" :maskClosable="false">
        <terminal v-if="terminalVisible" :sshId="ssh.id" :nodeId="ssh.nodeModel.id" :tail="temp.path + temp.parentDir" />
      </a-modal>
      <a-modal v-model="editFileVisible" width="80vw" title="编辑文件" cancelText="关闭" :maskClosable="true" @ok="updateFileData">
        <div style="height: 60vh">
          <code-editor showTool v-model="temp.fileContent" :fileSuffix="temp.name"></code-editor>
        </div>
      </a-modal>
      <!-- 从命名文件/文件夹 -->
      <a-modal v-model="renameFileFolderVisible" width="300px" :title="`重命名`" :footer="null" :maskClosable="true">
        <a-space direction="vertical" style="width: 100%">
          <a-input v-model="temp.fileFolderName" placeholder="输入新名称" />

          <a-row type="flex" justify="center" v-if="temp.fileFolderName">
            <a-button type="primary" :disabled="temp.fileFolderName.length === 0" @click="renameFileFolder">确认</a-button>
          </a-row>
        </a-space>
      </a-modal>
    </a-layout-content>
  </a-layout>
</template>
<script>
import {deleteFile, downloadFile, getFileList, getRootFileList, newFileFolder, readFile, renameFileFolder, updateFileData, uploadFile} from "@/api/ssh";
import Terminal from "./terminal";
import codeEditor from "@/components/codeEditor";
import {ZIP_ACCEPT} from "@/utils/const";

export default {
  props: {
    ssh: {
      type: Object,
    },
  },
  components: {
    Terminal,
    codeEditor,
  },
  data() {
    return {
      loading: false,
      treeList: [],
      fileList: [],
      uploadFileList: [],
      tempNode: {},
      temp: {},
      uploadFileVisible: false,
      uploadFileZip: false,
      ZIP_ACCEPT: ZIP_ACCEPT,
      renameFileFolderVisible: false,
      terminalVisible: false,
      tableHeight: "80vh",
      replaceFields: {
        children: "children",
        title: "title",
        key: "key",
      },
      columns: [
        { title: "文件名称", dataIndex: "title", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "文件类型", dataIndex: "dir", width: 100, ellipsis: true, scopedSlots: { customRender: "dir" } },
        { title: "文件大小", dataIndex: "size", width: 120, ellipsis: true, scopedSlots: { customRender: "size" } },
        { title: "修改时间", dataIndex: "modifyTime", width: 180, ellipsis: true },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
      editFileVisible: false,
      addFileFolderVisible: false,
      // 目录1 文件2 标识
      addFileOrFolderType: 1,
      fileFolderName: "",
    };
  },
  mounted() {
    this.loadData();
  },
  computed: {
    nowPath() {
      if (!this.tempNode.parentDir || !this.tempNode.path) {
        return "";
      }
      return ((this.tempNode.path || "") + (this.tempNode.parentDir || "")).replace(new RegExp("//", "gm"), "/");
    },
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      getRootFileList(this.ssh.id).then((res) => {
        if (res.code === 200) {
          let tempList = [];
          res.data.forEach((element) => {
            tempList.push({
              key: element.path,
              title: element.path,
              path: element.path,
              parentDir: "/",
              isLeaf: false,
              disabled: element.error ? true : false,
            });
          });
          this.treeList = tempList;
        }
        this.loading = false;
      });
    },
    // 上传文件
    handleUpload() {
      if (Object.keys(this.tempNode).length === 0) {
        this.$notification.error({
          message: "请选择一个节点",
        });
        return;
      }
      this.uploadFileVisible = true;
      this.uploadFileZip = false;
    },
    handleUploadZip() {
      this.handleUpload();
      this.uploadFileZip = true;
    },
    handleAddFolder() {
      this.addFileFolderVisible = true;
      this.addFileOrFolderType = 1;
      this.fileFolderName = "";
    },
    handleAddFile() {
      this.addFileFolderVisible = true;
      this.addFileOrFolderType = 2;
      this.fileFolderName = "";
    },
    // closeAddFileFolder() {
    //   this.addFileFolderVisible = false;
    //   this.fileFolderName = "";
    // },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        id: this.ssh.id,
        path: this.nowPath,
        name: this.fileFolderName,
        unFolder: this.addFileOrFolderType === 1 ? false : true,
      };
      newFileFolder(params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.addFileFolderVisible = false;
          this.loadFileList();
          // this.closeAddFileFolder();
        }
      });
    },
    handleRemove(file) {
      const index = this.uploadFileList.indexOf(file);
      const newFileList = this.uploadFileList.slice();
      newFileList.splice(index, 1);
      this.uploadFileList = newFileList;
    },
    beforeUpload(file) {
      this.uploadFileList = [...this.uploadFileList, file];
      return false;
    },
    closeUploadFile() {
      this.uploadFileList = [];
    },
    // 开始上传文件
    startUpload() {
      this.uploadFileList.forEach((file) => {
        const formData = new FormData();
        formData.append("file", file);
        formData.append("id", this.ssh.id);
        formData.append("name", this.tempNode.parentDir);
        formData.append("unzip", this.uploadFileZip);
        formData.append("path", this.tempNode.path);
        // 上传文件
        uploadFile(formData).then((res) => {
          if (res.code === 200) {
            this.$notification.success({
              message: res.msg,
            });
            this.loadFileList();
            this.closeUploadFile();
            this.uploadFileVisible = false;
          }
        });
      });
    },
    // 选中目录
    onSelect(selectedKeys, { node }) {
      return new Promise((resolve) => {
        this.tempNode = node.dataRef;
        if (node.dataRef.disabled) {
          resolve();
          return;
        }
        // 请求参数
        const params = {
          id: this.ssh.id,
          path: node.dataRef.path,
          children: node.dataRef.parentDir,
        };
        this.fileList = [];
        this.loading = true;
        // 加载文件
        getFileList(params).then((res) => {
          if (res.code === 200) {
            let children = [];
            // 区分目录和文件
            res.data.forEach((element) => {
              if (element.dir) {
                children.push({
                  key: element.id,
                  title: element.title,
                  name: element.name,
                  path: node.dataRef.path,
                  parentDir: element.parentDir,
                  isLeaf: element.dir ? false : true,
                  disabled: element.error ? true : false,
                });
              } else {
                // 设置文件表格
                this.fileList.push({
                  path: node.dataRef.path,
                  ...element,
                });
              }
            });
            // 设置目录树
            node.dataRef.children = children;
            this.treeList = [...this.treeList];
          }
          this.loading = false;
        });
        resolve();
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
        id: this.ssh.id,
        path: this.tempNode.path,
        children: this.tempNode.parentDir,
      };
      this.fileList = [];
      this.loading = true;
      // 加载文件
      getFileList(params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          this.fileList = res.data
            .filter((element) => {
              return !element.dir;
            })
            .map((element) => {
              // 设置文件表格
              return {
                path: this.tempNode.path,
                ...element,
              };
            });
        }
        this.loading = false;
      });
    },
    // 编辑
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      const params = {
        id: this.ssh.id,
        path: record.path,
        children: record.parentDir,
      };
      readFile(params).then((res) => {
        if (res.code == 200) {
          this.temp.fileContent = res.data;
          this.editFileVisible = true;
        }
      });
      //
    },
    updateFileData() {
      const params = {
        id: this.ssh.id,
        path: this.temp.path,
        children: this.temp.parentDir,
        content: this.temp.fileContent,
      };
      updateFileData(params).then((res) => {
        this.$notification.success({
          message: res.msg,
        });
        if (res.code == 200) {
          this.editFileVisible = false;
        }
      });
    },
    // 查看
    handlePreview(record) {
      this.temp = Object.assign({}, record);
      this.terminalVisible = true;
    },
    // 下载
    handleDownload(record) {
      // 请求参数
      const params = {
        id: this.ssh.id,
        path: record.path,
        name: record.parentDir,
      };
      // 请求接口拿到 blob
      downloadFile(params).then((blob) => {
        const url = window.URL.createObjectURL(blob);
        let link = document.createElement("a");
        link.style.display = "none";
        link.href = url;
        link.setAttribute("download", record.name);
        document.body.appendChild(link);
        link.click();
      });
    },
    // 删除文件夹
    handleDeletePath() {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除当前文件夹么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            id: this.ssh.id,
            path: this.tempNode.path,
            name: this.tempNode.parentDir,
          };
          // 删除
          deleteFile(params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // 刷新树
              this.loadData();
            }
          });
        },
      });
    },
    // 删除
    handleDelete(record) {
      this.$confirm({
        title: "系统提示",
        content: "真的要删除文件么？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 请求参数
          const params = {
            id: this.ssh.id,
            path: record.path,
            name: record.parentDir,
          };
          // 删除
          deleteFile(params).then((res) => {
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
    handleRenameFile(record) {
      this.renameFileFolderVisible = true;
      this.temp = {
        fileFolderName: record.title,
        oldFileFolderName: record.title,
        path: record.path,
      };
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        id: this.ssh.id,
        path: this.temp.path,
        name: this.temp.oldFileFolderName,
        newname: this.temp.fileFolderName,
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
  },
};
</script>
<style scoped lang="stylus">
.ssh-file-layout {
  padding: 0;
  min-height calc(100vh - 75px);
}

.sider {
  border: 1px solid #e2e2e2;
  /* height: calc(100vh - 80px); */
  /* overflow-y: auto; */
}

.file-content {
  /* height: calc(100vh - 100px); */
  /* overflow-y: auto; */
  margin: 10px 10px 0;
  padding: 10px;
  background-color: #fff;
}
</style>
