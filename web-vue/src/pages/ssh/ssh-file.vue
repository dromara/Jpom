<template>
  <!-- 布局 -->
  <a-layout class="ssh-file-layout">
    <!-- 目录树 -->
    <a-layout-sider theme="light" class="sider" width="25%">
      <a-row class="dir-container">
        <a-button size="small" type="primary" @click="loadData()">刷新</a-button>
      </a-row>
      <a-empty v-if="treeList.length === 0" />
      <a-directory-tree :treeData="treeList" :replaceFields="replaceFields" @select="onSelect"> </a-directory-tree>
    </a-layout-sider>
    <!-- 表格 -->
    <a-layout-content class="file-content">
      <!-- <div ref="filter" class="filter"></div> -->
      <a-table size="middle" :data-source="fileList" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
        <template slot="title">
          <a-space>
            <a-dropdown :disabled="!this.tempNode.nextPath">
              <a-button size="small" type="primary" @click="(e) => e.preventDefault()">上传小文件</a-button>
              <a-menu slot="overlay">
                <a-menu-item @click="handleUpload">
                  <a-space><a-icon type="file-add" />上传文件</a-space>
                </a-menu-item>
                <a-menu-item @click="handleUploadZip">
                  <a-space><a-icon type="file-zip" />上传压缩文件（自动解压）</a-space>
                </a-menu-item>
              </a-menu>
            </a-dropdown>
            <a-dropdown :disabled="!this.tempNode.nextPath">
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
            <a-button size="small" :disabled="!this.tempNode.nextPath" type="primary" @click="loadFileList()">刷新</a-button>
            <a-button size="small" :disabled="!this.tempNode.nextPath" type="danger" @click="handleDeletePath()">删除</a-button>
            <div>
              文件夹：
              <a-switch :disabled="!this.tempNode.nextPath" @change="changeListShowDir" checked-children="显示" un-checked-children="隐藏" v-model="listShowDir" />
            </div>
            <span v-if="this.nowPath">当前目录:{{ this.nowPath }}</span>
            <!-- <span v-if="this.nowPath">{{ this.tempNode.parentDir }}</span> -->
          </a-space>
        </template>
        <a-tooltip slot="name" slot-scope="text, record" placement="topLeft" :title="` 名称：${text} 长名称：${record.longname}`">
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
        <a-tooltip slot="dir" slot-scope="text, record" placement="topLeft" :title="`${record.link ? '链接' : text ? '目录' : '文件'}`">
          <span>{{ record.link ? "链接" : text ? "目录" : "文件" }}</span>
        </a-tooltip>
        <a-tooltip slot="size" slot-scope="text" placement="topLeft" :title="renderSize(text)">
          <span>{{ renderSize(text) }}</span>
        </a-tooltip>
        <a-tooltip slot="tooltip" slot-scope="text" placement="topLeft" :title="text">
          <span>{{ text }}</span>
        </a-tooltip>
        <template slot="operation" slot-scope="text, record">
          <a-space>
            <a-tooltip title="需要到 ssh 信息中配置允许编辑的文件后缀">
              <a-button size="small" type="primary" :disabled="!record.textFileEdit" @click="handleEdit(record)">编辑</a-button>
            </a-tooltip>
            <a-tooltip title="修改文件权限">
              <a-button size="small" type="primary" @click="handleFilePermission(record)">修改文件权限</a-button>
            </a-tooltip>
            <a-button size="small" type="primary" :disabled="record.dir" @click="handleDownload(record)">下载</a-button>
            <a-button size="small" type="danger" @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </a-table>
      <!-- 上传文件 -->
      <a-modal destroyOnClose @cancel="closeUploadFile" v-model="uploadFileVisible" width="300px" title="上传文件" :footer="null" :maskClosable="true">
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
      <a-modal v-model="addFileFolderVisible" width="300px" :title="temp.addFileOrFolderType === 1 ? '新增目录' : '新建文件'" :footer="null" :maskClosable="true">
        <a-space direction="vertical" style="width: 100%">
          <span v-if="this.nowPath">当前目录:{{ this.nowPath }}</span>
          <!-- <a-tag v-if="">目录创建成功后需要手动刷新右边树才能显示出来哟</a-tag> -->
          <a-tooltip :title="this.temp.addFileOrFolderType === 1 ? '目录创建成功后需要手动刷新右边树才能显示出来哟' : ''">
            <a-input v-model="temp.fileFolderName" placeholder="输入文件或者文件夹名" />
          </a-tooltip>
          <a-row type="flex" justify="center">
            <a-button type="primary" :disabled="!temp.fileFolderName || temp.fileFolderName.length === 0" @click="startAddFileFolder">确认</a-button>
          </a-row>
        </a-space>
      </a-modal>

      <a-modal destroyOnClose v-model="editFileVisible" width="80vw" title="编辑文件" cancelText="关闭" :maskClosable="true" @ok="updateFileData">
        <div style="height: 60vh">
          <code-editor showTool v-model="temp.fileContent" :fileSuffix="temp.name"></code-editor>
        </div>
      </a-modal>
      <!-- 从命名文件/文件夹 -->
      <a-modal destroyOnClose v-model="renameFileFolderVisible" width="300px" :title="`重命名`" :footer="null" :maskClosable="true">
        <a-space direction="vertical" style="width: 100%">
          <a-input v-model="temp.fileFolderName" placeholder="输入新名称" />

          <a-row type="flex" justify="center" v-if="temp.fileFolderName">
            <a-button type="primary" :disabled="temp.fileFolderName.length === 0 || temp.fileFolderName === temp.oldFileFolderName" @click="renameFileFolder">确认</a-button>
          </a-row>
        </a-space>
      </a-modal>

      <!-- 修改文件权限 -->
      <a-modal destroyOnClose v-model="editFilePermissionVisible" width="400px" :title="`修改文件权限`" :footer="null" :maskClosable="true">
        <a-row>
          <a-col :span="6"><span class="title">权限</span></a-col>
          <a-col :span="6"><span class="title">所属用户</span></a-col>
          <a-col :span="6"><span class="title">用户组</span></a-col>
          <a-col :span="6"><span class="title">其他</span></a-col>
        </a-row>
        <a-row>
          <a-col :span="6">
            <span>读</span>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.owner.read" @change="renderFilePermissionsTips"/>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.owner.write" @change="renderFilePermissionsTips"/>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.owner.execute" @change="renderFilePermissionsTips"/>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="6">
            <span>写</span>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.group.read" @change="renderFilePermissionsTips"/>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.group.write" @change="renderFilePermissionsTips"/>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.group.execute" @change="renderFilePermissionsTips"/>
          </a-col>
        </a-row>
        <a-row>
          <a-col :span="6">
            <span>执行</span>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.others.read" @change="renderFilePermissionsTips"/>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.others.write" @change="renderFilePermissionsTips"/>
          </a-col>
          <a-col :span="6">
            <a-checkbox v-model="permissions.others.execute" @change="renderFilePermissionsTips"/>
          </a-col>
        </a-row>
        <a-row type="flex" style="margin-top: 20px;">
          <a-button type="primary" @click="updateFilePermissions">确认修改</a-button>
        </a-row>
        <a-row>
          <a-alert style="margin-top: 20px;" :message="permissionTips" type="success" />
        </a-row>
      </a-modal>
    </a-layout-content>
  </a-layout>
</template>
<script>
import { deleteFile, downloadFile, getFileList, getRootFileList, newFileFolder, readFile, renameFileFolder, updateFileData, uploadFile, parsePermissions, calcFilePermissionValue } from "@/api/ssh-file";

import codeEditor from "@/components/codeEditor";
import { ZIP_ACCEPT, renderSize } from "@/utils/const";

export default {
  props: {
    sshId: {
      type: String,
      default: "",
    },
    machineSshId: {
      type: String,
      default: "",
    },
  },
  components: {
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
      listShowDir: false,
      tableHeight: "80vh",
      replaceFields: {
        children: "children",
        title: "name",
        key: "key",
      },
      columns: [
        { title: "文件名称", dataIndex: "name", width: 100, ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "文件类型", dataIndex: "dir", width: 100, ellipsis: true, scopedSlots: { customRender: "dir" } },
        { title: "文件大小", dataIndex: "size", width: 120, ellipsis: true, scopedSlots: { customRender: "size" } },
        { title: "权限", dataIndex: "permissions", width: 120, ellipsis: true, scopedSlots: { customRender: "tooltip" } },
        { title: "修改时间", dataIndex: "modifyTime", width: "170px", ellipsis: true },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: "260px" },
      ],
      editFileVisible: false,
      addFileFolderVisible: false,
      editFilePermissionVisible: false,
      permissions: {
        owner: { read: false, write: false, execute: false, },
        group: { read: false, write: false, execute: false, },
        others: { read: false, write: false, execute: false, },
      },
      permissionTips: '',
    };
  },
  mounted() {
    this.listShowDir = Boolean(localStorage.getItem("ssh-list-show-dir"));
    this.loadData();
  },
  computed: {
    nowPath() {
      if (!this.tempNode.allowPathParent) {
        return "";
      }
      return ((this.tempNode.allowPathParent || "") + "/" + (this.tempNode.nextPath || "")).replace(new RegExp("//+", "gm"), "/");
    },
    baseUrl() {
      if (this.sshId) {
        return "/node/ssh/";
      }
      return "/system/assets/ssh-file/";
    },
    reqDataId() {
      return this.sshId || this.machineSshId;
    },
  },
  methods: {
    renderSize,
    // 加载数据
    loadData() {
      this.loading = true;
      getRootFileList(this.baseUrl, this.reqDataId).then((res) => {
        if (res.code === 200) {
          this.treeList = res.data.map((element) => {
            return {
              key: element.id,
              name: element.allowPathParent,
              allowPathParent: element.allowPathParent,
              nextPath: "/",
              isLeaf: false,
              // 配置的白名单目录可能不存在
              disabled: !!element.error,
            };
          });
        }
        this.loading = false;
      });
    }, // 选中目录
    onSelect(selectedKeys, { node }) {
      return new Promise((resolve) => {
        if (node.dataRef.disabled) {
          resolve();
          return;
        }
        // console.log(node.dataRef, this.tempNode.key);
        if (node.dataRef.key === this.tempNode.key) {
          resolve();
          return;
        }
        this.tempNode = node.dataRef;
        // 请求参数
        const params = {
          id: this.reqDataId,
          allowPathParent: node.dataRef.allowPathParent,
          nextPath: node.dataRef.nextPath,
        };
        this.fileList = [];
        this.loading = true;
        // 加载文件
        getFileList(this.baseUrl, params).then((res) => {
          if (res.code === 200) {
            let children = [];
            // 区分目录和文件
            res.data.forEach((element) => {
              if (element.dir) {
                if (this.listShowDir) {
                  this.fileList.push({
                    // path: node.dataRef.path,
                    ...element,
                  });
                }
                children.push({
                  key: element.id,
                  name: element.name,
                  allowPathParent: node.dataRef.allowPathParent,
                  nextPath: (element.nextPath + "/" + element.name).replace(new RegExp("//+", "gm"), "/"),
                  isLeaf: !element.dir,
                  // 可能有错误
                  disabled: !!element.error,
                });
              } else {
                // 设置文件表格
                this.fileList.push({
                  // path: node.dataRef.path,
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
    changeListShowDir() {
      this.loadFileList();
      localStorage.setItem("ssh-list-show-dir", this.listShowDir);
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
        id: this.reqDataId,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath,
      };
      // this.fileList = [];
      this.loading = true;
      // 加载文件
      getFileList(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          // 区分目录和文件
          this.fileList = res.data
            .filter((element) => {
              if (this.listShowDir) {
                return true;
              }
              return !element.dir;
            })
            .map((element) => {
              // 设置文件表格
              return {
                // path: this.tempNode.path,
                ...element,
              };
            });
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
      // 目录1 文件2 标识
      // addFileOrFolderType: 1,
      //       fileFolderName: "",
      this.temp = {
        fileFolderName: "",
        addFileOrFolderType: 1,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath,
      };
    },
    handleAddFile() {
      this.addFileFolderVisible = true;
      // 目录1 文件2 标识
      // addFileOrFolderType: 1,
      //       fileFolderName: "",
      this.temp = {
        fileFolderName: "",
        addFileOrFolderType: 2,
        allowPathParent: this.tempNode.allowPathParent,
        nextPath: this.tempNode.nextPath,
      };
    },
    // closeAddFileFolder() {
    //   this.addFileFolderVisible = false;
    //   this.fileFolderName = "";
    // },
    // 确认新增文件  目录
    startAddFileFolder() {
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        name: this.temp.fileFolderName,
        unFolder: this.temp.addFileOrFolderType !== 1,
      };
      newFileFolder(this.baseUrl, params).then((res) => {
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
        formData.append("id", this.reqDataId);
        formData.append("allowPathParent", this.tempNode.allowPathParent);
        formData.append("unzip", this.uploadFileZip);
        formData.append("nextPath", this.tempNode.nextPath);
        // 上传文件
        uploadFile(this.baseUrl, formData).then((res) => {
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

    // 编辑
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      const params = {
        id: this.reqDataId,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
        name: record.name,
      };
      readFile(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, fileContent: res.data };
          this.editFileVisible = true;
        }
      });
      //
    },
    updateFileData() {
      const params = {
        id: this.reqDataId,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
        name: this.temp.name,
        content: this.temp.fileContent,
      };

      updateFileData(this.baseUrl, params).then((res) => {
        if (res.code === 200) {
          this.$notification.success({
            message: res.msg,
          });
          this.editFileVisible = false;
        }
      });
    },
    // 修改文件权限
    handleFilePermission(record) {
      this.temp = Object.assign({}, record);
      this.permissions = parsePermissions(this.temp.permissions);
      const permissionsValue = calcFilePermissionValue(this.permissions);
      this.permissionTips = `cd ${this.temp.nextPath} && chmod ${permissionsValue} ${this.temp.name}`
      this.editFilePermissionVisible = true;
    },
    // 更新文件权限提示
    renderFilePermissionsTips() {
      const permissionsValue = calcFilePermissionValue(this.permissions);
      this.permissionTips = `cd ${this.temp.nextPath} && chmod ${permissionsValue} ${this.temp.name}`
    },
    // 确认修改文件权限
    updateFilePermissions() {

    },

    // 下载
    handleDownload(record) {
      // 请求参数
      const params = {
        id: this.reqDataId,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
        name: record.name,
      };
      // 请求接口拿到 blob
      window.open(downloadFile(this.baseUrl, params), "_blank");
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
            id: this.reqDataId,
            allowPathParent: this.tempNode.allowPathParent,
            nextPath: this.tempNode.nextPath,
          };
          // 删除
          deleteFile(this.baseUrl, params).then((res) => {
            if (res.code === 200) {
              this.$notification.success({
                message: res.msg,
              });
              // 刷新树
              this.loadData();
              this.fileList = [];
              //this.loadFileList();
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
            id: this.reqDataId,
            allowPathParent: record.allowPathParent,
            nextPath: record.nextPath,
            name: record.name,
          };
          // 删除
          deleteFile(this.baseUrl, params).then((res) => {
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
        fileFolderName: record.name,
        oldFileFolderName: record.name,
        allowPathParent: record.allowPathParent,
        nextPath: record.nextPath,
      };
    },
    // 确认修改文件 目录名称
    renameFileFolder() {
      const params = {
        id: this.reqDataId,
        name: this.temp.oldFileFolderName,
        newname: this.temp.fileFolderName,
        allowPathParent: this.temp.allowPathParent,
        nextPath: this.temp.nextPath,
      };
      renameFileFolder(this.baseUrl, params).then((res) => {
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
<style scoped>
.ssh-file-layout {
  padding: 0;
  min-height: calc(100vh - 75px);
}

.dir-container {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.sider {
  border: 1px solid #e2e2e2;
  /* height: calc(100vh - 80px); */
  /* overflow-y: auto; */
  overflow-x: auto;
}

.file-content {
  /* height: calc(100vh - 100px); */
  /* overflow-y: auto; */
  margin: 10px 10px 0;
  padding: 10px;
  background-color: #fff;
}

.title {
  font-weight: 600;
  font-size: larger;
}
</style>
