<template>
  <div class="full-content">
    <a-row>
      <a-col span="6" style="">
        <a-row>
          <a-space style="display: inline">
            <a-input v-model="addName" placeholder="创建文件 /xxx/xxx/xxx" style="width: 100%">
              <template slot="addonAfter"><a-button type="primary" size="small" :disabled="!addName" @click="addItemHander">确认</a-button></template>
            </a-input>
          </a-space>
        </a-row>
        <a-directory-tree :treeData="treeData" :replaceFields="replaceFields" @select="select"> </a-directory-tree>
      </a-col>
      <a-col span="18" style="padding-left: 5px">
        <a-space direction="vertical" style="display: flex">
          <div class="config-editor">
            <code-editor :showTool="temp.name ? true : false" v-model="temp.content" :fileSuffix="temp.name">
              <template slot="tool_before">
                <div v-show="temp.name">
                  名称： <a-tag color="red">{{ temp.name }}</a-tag>
                </div>
              </template>
            </code-editor>
          </div>
          <a-row type="flex" justify="center">
            <a-space>
              <a-button type="danger" :disabled="!temp || !temp.name" @click="saveData">保存</a-button>
              <a-button type="primary" v-if="temp.hasDefault" :disabled="!temp || !temp.name" @click="readeDefault">读取默认</a-button>
            </a-space>
          </a-row>
        </a-space>
      </a-col>
    </a-row>
  </div>
</template>
<script>
import codeEditor from "@/components/codeEditor";
import { addItem, listExtConf, getItem, saveItem, getDefaultItem } from "@/api/ext-config";

export default {
  components: {
    codeEditor,
  },
  data() {
    return {
      loading: false,
      treeData: [],
      editVisible: false,
      temp: {},
      columns: [
        { title: "名称", dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: "路径", dataIndex: "path", ellipsis: true, scopedSlots: { customRender: "path" } },
        { title: "操作", dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 180 },
      ],
      replaceFields: {
        children: "children",
        title: "name",
        key: "id",
      },
      addName: "",
    };
  },
  computed: {},
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      listExtConf().then((res) => {
        if (res.code === 200) {
          this.treeData = res.data?.children;
        }
        this.loading = false;
      });
    },
    // 选择
    select(selectedKeys, { node }) {
      if (this.temp?.name === node.dataRef?.name) {
        return;
      }
      if (!node.dataRef.isLeaf) {
        return;
      }
      this.temp = {};
      getItem({ name: node.dataRef?.id }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            name: node.dataRef?.id,
            content: res.data,
            hasDefault: node.dataRef?.hasDefault,
          };
        }
      });
    },
    readeDefault() {
      getDefaultItem({ name: this.temp.name }).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, content: res.data };
          this.$message.success({ content: "已经读取默认配置文件到编辑器中" });
        }
      });
    },
    addItemHander() {
      this.$confirm({
        title: "系统提示",
        zIndex: 1009,
        content: "确认创建该【" + this.addName + "】配置文件吗？配置文件一旦创建不能通过管理页面删除的奥？",
        okText: "确认",
        cancelText: "取消",
        onOk: () => {
          // 删除
          addItem({ name: this.addName }).then((res) => {
            if (res.code === 200) {
              // 成功
              this.$notification.success({
                message: res.msg,
              });
              this.addName = "";
              this.loadData();
            }
          });
        },
      });
    },
    saveData() {
      saveItem(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          this.$notification.success({
            message: res.msg,
          });
        }
      });
    },
  },
};
</script>
<style scoped>
.config-editor {
  height: calc(100vh - 170px);
  width: 100%;
  overflow-y: scroll;
  /* border: 1px solid #d9d9d9; */
}
</style>
