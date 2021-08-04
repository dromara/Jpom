<template>
  <div>
    <!-- 搜索区 -->
    <div ref="filter" class="filter">
      <a-select v-model="listQuery.group" allowClear placeholder="请选择分组"
        class="filter-item" @change="handleFilter">
        <a-select-option v-for="group in groupList" :key="group">{{ group }}</a-select-option>
      </a-select>
      <a-button type="primary" @click="handleFilter">搜索</a-button>
      <a-button type="primary" @click="handleAdd">新增</a-button>
      <a-button type="primary" @click="loadData">刷新</a-button>
    </div>
    <!-- 表格 -->
    <a-table :loading="loading" :columns="columns" :data-source="list" :style="{'max-height': tableHeight + 'px' }"
      :scroll="{x: 1210, y: tableHeight - 60}" bordered rowKey="id" :pagination="false">
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="gitUrl" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="repoType" slot-scope="text">
          <span v-if="text === 0">GIT</span>
          <span v-else-if="text === 1">SVN</span>
          <span v-else>未知</span>
      </template>
      <template slot="operation" slot-scope="text, record">
        <a-button type="primary" @click="handleEdit(record)">编辑</a-button>
        <a-button type="danger" @click="handleDelete(record)">删除</a-button>
      </template>
    </a-table>
  </div>
</template>
<script>
import {mapGetters} from 'vuex';
import { getRepositoryList } from '../../api/repository';
import {parseTime} from '../../utils/time';

export default {
  components: {
  },
  data() {
    return {
      loading: false,
      listQuery: {},
      tableHeight: '70vh',
      list: [],
      temp: {},
      editBuildVisible: false,
      columns: [
        {title: '名称', dataIndex: 'name', width: 150, ellipsis: true, scopedSlots: {customRender: 'name'}},
        {
          title: '仓库地址',
          dataIndex: 'gitUrl',
          width: 300,
          ellipsis: true,
          scopedSlots: {customRender: 'gitUrl'}
        },
        {
            title: '仓库类型',
            dataIndex: 'repoType',
            width: 80,
            ellipsis: true,
            scopedSlots: {customRender: 'repoType'}
        },
        {
          title: '修改时间', dataIndex: 'modifyTime', customRender: (text) => {
            if (!text) {
              return '';
            }
            return parseTime(text);
          }, width: 180
        },
        {
            title: '操作',
            dataIndex: 'operation',
            width: 240,
            scopedSlots: {customRender: 'operation'},
            align: 'left',
            fixed: 'right'
        }
      ],
      rules: {
          name: [
              {required: true, message: 'Please input build name', trigger: 'blur'}
          ],
          script: [
              {required: true, message: 'Please input build script', trigger: 'blur'}
          ],
          resultDirFile: [
              {required: true, message: 'Please input build target path', trigger: 'blur'}
          ],
          releasePath: [
              {required: true, message: 'Please input release path', trigger: 'blur'}
          ]
      }
    }
  },
  computed: {
    ...mapGetters([
      'getGuideFlag'
    ])
  },
  watch: {
    getGuideFlag() {
      this.introGuide();
    }
  },
  created() {
    this.calcTableHeight();
    this.handleFilter();
  },
  methods: {
      // 页面引导
      introGuide() {
          if (this.getGuideFlag) {
              this.$introJs().setOptions({
                  hidePrev: true,
                  steps: [{
                    title: 'Jpom 导航助手',
                    element: document.querySelector('.jpom-target-dir'),
                    intro: '可以理解为项目打包的目录。如 Jpom 项目执行 <b>mvn clean package</b> 构建命令，构建产物相对路径为：<b>modules/server/target/server-2.4.2-release</b>'
                  }]
              }).start();
              return false;
          }
          this.$introJs().exit();
      },
      // 计算表格高度
      calcTableHeight() {
          this.$nextTick(() => {
              this.tableHeight = window.innerHeight - this.$refs['filter'].clientHeight - 135;
          })
      },
      // 加载数据
      loadData() {
        this.list = [];
        this.loading = true;
        getRepositoryList(this.listQuery).then(res => {
          if (res.code === 200) {
            this.list = res.data;
          }
          this.loading = false;
        })
      },
      // 筛选
      handleFilter() {
        this.loadData();
      },
      // 添加
      handleAdd() {
        this.temp = {};
        this.editBuildVisible = true;
        this.$nextTick(() => {
          setTimeout(() => {
            this.introGuide();
          }, 500);
        })
      },
      // 修改
      handleEdit(record) {
          this.temp = Object.assign(record);
          this.editBuildVisible = true;
      },
      // 添加分组
      handleAddGroup() {
          this.$notification.success({
            message: '添加成功',
            duration: 2
          });
          this.addGroupvisible = false;
      },
      // 提交节点数据
      handleEditBuildOk() {
          // 检验表单
          this.$refs['editBuildForm'].validate((valid) => {
              if (!valid) {
                  return false;
              }
              // 提交数据
              // editBuild(this.temp).then(res => {
              //     if (res.code === 200) {
              //         // 成功
              //         this.$notification.success({
              //             message: res.msg,
              //             duration: 2
              //         });
              //         this.$refs['editBuildForm'].resetFields();
              //         this.editBuildVisible = false;
              //         this.handleFilter();
              //     }
              // })
          })
      },
      // 删除
      handleDelete(record) {
        console.log(record);
        this.$confirm({
          title: '系统提示',
          content: '真的要删除构建信息么？',
          okText: '确认',
          cancelText: '取消',
          onOk: () => {
            // 删除
            // deleteBuild(record.id).then((res) => {
            //   if (res.code === 200) {
            //     this.$notification.success({
            //       message: res.msg,
            //       duration: 2
            //     });
            //     this.loadData();
            //   }
            // })
          }
        });
      }
  }
}
</script>
<style scoped>
.filter {
    margin-bottom: 10px;
}

.ant-btn {
    margin-right: 10px;
}

.filter-item {
    width: 150px;
    margin-right: 10px;
}

.btn-add {
    margin-left: 10px;
    margin-right: 0;
}
</style>
