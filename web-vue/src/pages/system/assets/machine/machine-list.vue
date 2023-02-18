<template>
  <div>
    <a-card :bodyStyle="{ padding: '10px' }">
      <template slot="title">
        <a-space>
          <a-input class="search-input-item" @pressEnter="getMachineList" v-model="listQuery['%name%']" placeholder="机器名称" />
          <a-input class="search-input-item" @pressEnter="getMachineList" v-model="listQuery['%jpomUrl%']" placeholder="节点地址" />
          <a-select show-search option-filter-prop="children" v-model="listQuery.groupName" allowClear placeholder="分组" class="search-input-item">
            <a-select-option v-for="item in groupList" :key="item">{{ item }}</a-select-option>
          </a-select>
          <a-select v-model="listQuery['order_field']" allowClear placeholder="请选择排序字段" class="search-input-item">
            <a-select-option value="networkDelay">网络延迟</a-select-option>
            <a-select-option value="osOccupyCpu">cpu</a-select-option>
            <a-select-option value="osOccupyDisk">硬盘</a-select-option>
            <a-select-option value="osOccupyMemory">内存</a-select-option>
            <a-select-option value="osOccupyMemory">更新时间</a-select-option>
            <a-select-option value="osOccupyMemory">创建时间</a-select-option>
          </a-select>
          <a-button :loading="loading" type="primary" @click="getMachineList">搜索</a-button>
        </a-space>
      </template>
      <a-row :gutter="[16, 16]">
        <template v-if="list && list.length">
          <a-col v-for="item in list" :key="item.id" :span="6">
            <template>
              <a-card :headStyle="{ padding: '0 6px' }" :bodyStyle="{ padding: '10px' }">
                <template slot="title">
                  <a-row :gutter="[4, 0]">
                    <a-col :span="17" style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
                      <a-tooltip>
                        <template slot="title">
                          <div>节点名称：{{ item.name }}</div>
                          <div>节点地址：{{ item.jpomUrl }}</div>
                        </template>

                        <a-button type="link" size="small" icon="edit" @click="handleEdit(item)">
                          {{ item.name }}
                        </a-button>
                      </a-tooltip>
                    </a-col>
                    <a-col :span="7" style="text-align: right">
                      <a-tooltip :title="`当前状态：${statusMap[item.status]} ${item.statusMsg ? '状态消息：' + item.statusMsg : ''} `">
                        <a-tag :color="item.status === 1 ? 'green' : 'pink'" style="margin-right: 0px"> {{ statusMap[item.status] }}</a-tag>
                      </a-tooltip>
                      <a-button type="link" icon="fullscreen" size="small"> </a-button>
                      <!-- <a-icon type="fullscreen" /> -->
                    </a-col>
                  </a-row>
                </template>

                <!-- <a-row :gutter="[8, 8]"> -->
                <a-tooltip :title="item.osName">
                  <div class="item-info">
                    <div class="title">系统名称:</div>
                    <div class="content">
                      {{ item.osName }}
                    </div>
                  </div>
                </a-tooltip>
                <a-tooltip :title="item.osVersion">
                  <div class="item-info">
                    <div class="title">系统版本:</div>
                    <div class="content">
                      {{ item.osVersion }}
                    </div>
                  </div>
                </a-tooltip>
                <a-tooltip :title="item.osLoadAverage">
                  <div class="item-info">
                    <div class="title">系统负载:</div>
                    <div class="content">
                      {{ item.osLoadAverage }}
                    </div>
                  </div>
                </a-tooltip>
                <a-tooltip :title="item.jpomVersion">
                  <div class="item-info">
                    <div class="title">插件版本:</div>
                    <div class="content">
                      {{ item.jpomVersion }}
                    </div>
                  </div>
                </a-tooltip>
                <!-- <a-button type="link" :size="size"> 详情 </a-button> -->
              </a-card>
            </template>
          </a-col>
        </template>
        <a-col v-else :span="24">
          <a-empty description="没有任何节点" />
        </a-col>
      </a-row>
      <a-row>
        <a-col>
          <a-pagination
            v-model="listQuery.page"
            v-if="listQuery.total / listQuery.limit > 1"
            :showTotal="
              (total) => {
                return PAGE_DEFAULT_SHOW_TOTAL(total, listQuery);
              }
            "
            :showSizeChanger="true"
            :pageSizeOptions="sizeOptions"
            :pageSize="listQuery.limit"
            :total="listQuery.total"
            @showSizeChange="
              (current, size) => {
                this.listQuery.limit = size;
                this.getMachineList();
              }
            "
            @change="this.getMachineList"
            show-less-items
          />
        </a-col>
      </a-row>
    </a-card>
    <!-- 编辑区 -->
    <a-modal destroyOnClose v-model="editVisible" width="50%" title="编辑机器" @ok="handleEditOk" :maskClosable="false">
      <a-form-model ref="editNodeForm" :rules="rules" :model="temp" :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
        <a-form-model-item label="机器名称" prop="name">
          <a-input :maxLength="50" v-model="temp.name" placeholder="机器名称" />
        </a-form-model-item>
        <a-form-model-item label="机器分组" prop="group">
          <custom-select v-model="temp.groupName" :data="groupList" suffixIcon="" inputPlaceholder="添加分组" selectPlaceholder="选择分组名"> </custom-select>
        </a-form-model-item>

        <a-form-model-item prop="jpomUrl">
          <template slot="label">
            节点地址
            <a-tooltip v-show="!temp.id">
              <template slot="title"
                >节点地址为插件端的 IP:PORT 插件端端口默认为：2123
                <ul>
                  <li>节点地址建议使用内网地址</li>
                  <li>如果插件端正常运行但是连接失败请检查端口是否开放,防火墙规则,云服务器的安全组入站规则</li>
                </ul>
              </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input v-model="temp.jpomUrl" placeholder="节点地址 (127.0.0.1:2123)">
            <a-select placeholder="选择协议类型" slot="addonBefore" v-model="temp.jpomProtocol" default-value="Http://" style="width: 80px">
              <a-select-option value="Http"> Http:// </a-select-option>
              <a-select-option value="Https"> Https:// </a-select-option>
            </a-select>
          </a-input>
        </a-form-model-item>

        <a-form-model-item label="节点账号" prop="loginName">
          <a-input v-model="temp.jpomUsername" placeholder="节点账号,请查看节点启动输出的信息" />
        </a-form-model-item>
        <a-form-model-item :prop="`${temp.id ? 'loginPwd-update' : 'loginPwd'}`">
          <template slot="label">
            节点密码
            <a-tooltip v-show="!temp.id">
              <template slot="title"> 节点账号密码默认由系统生成：可以通过插件端数据目录下 agent_authorize.json 文件查看（如果自定义配置了账号密码将没有此文件） </template>
              <a-icon type="question-circle" theme="filled" />
            </a-tooltip>
          </template>
          <a-input-password v-model="temp.jpomPassword" placeholder="节点密码,请查看节点启动输出的信息" />
        </a-form-model-item>

        <a-collapse>
          <a-collapse-panel key="1" header="其他配置">
            <a-form-model-item label="超时时间(s)" prop="timeOut">
              <a-input-number v-model="temp.jpomTimeout" :min="0" placeholder="秒 (值太小可能会取不到节点状态)" style="width: 100%" />
            </a-form-model-item>

            <a-form-model-item label="代理" prop="jpomHttpProxy">
              <a-input v-model="temp.jpomHttpProxy" placeholder="代理地址 (127.0.0.1:8888)">
                <a-select slot="addonBefore" v-model="temp.jpomHttpProxyType" placeholder="选择代理类型" default-value="HTTP" style="width: 100px">
                  <a-select-option value="HTTP">HTTP</a-select-option>
                  <a-select-option value="SOCKS">SOCKS</a-select-option>
                  <a-select-option value="DIRECT">DIRECT</a-select-option>
                </a-select>
              </a-input>
            </a-form-model-item>
          </a-collapse-panel>
        </a-collapse>
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import { machineListData, machineListGroup, statusMap, machineEdit } from "@/api/system/assets-machine";
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, PAGE_DEFAULT_SHOW_TOTAL, formatDuration, parseTime } from "@/utils/const";
import CustomSelect from "@/components/customSelect";
export default {
  components: {
    CustomSelect,
  },
  data() {
    return {
      statusMap,
      listQuery: Object.assign({ order: "descend", order_field: "networkDelay" }, PAGE_DEFAULT_LIST_QUERY, {}),
      sizeOptions: ["8", "12", "16", "20", "24"],
      list: [],
      groupList: [],
      loading: true,
      editVisible: false,
      temp: {},
      rules: {
        name: [{ required: true, message: "请输入机器的名称", trigger: "blur" }],
      },
    };
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery);
    },
  },
  mounted() {
    this.loadGroupList();
    this.getMachineList();
  },
  methods: {
    parseTime,
    formatDuration,
    PAGE_DEFAULT_SHOW_TOTAL,
    // 获取所有的分组
    loadGroupList() {
      machineListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data;
        }
      });
    },
    getMachineList(pointerEvent) {
      this.loading = true;
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page;
      machineListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result;
          this.listQuery.total = res.data.total;
        }
        this.loading = false;
      });
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter });
      this.getMachineList();
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record);
      delete this.temp.statusMsg;
      this.editVisible = true;
    },
    // 提交节点数据
    handleEditOk() {
      // 检验表单
      this.$refs["editNodeForm"].validate((valid) => {
        if (!valid) {
          return false;
        }
        // 提交数据
        machineEdit(this.temp).then((res) => {
          if (res.code === 200) {
            // 成功
            this.$notification.success({
              message: res.msg,
            });
            this.$refs["editNodeForm"].resetFields();
            this.editVisible = false;
            this.loadGroupList();
            this.getMachineList();
          }
        });
      });
    },
  },
};
</script>

<style scoped>
.item-info {
  padding: 5px 0;
}
.item-info .title {
  display: inline;
  font-weight: bold;
}
.item-info .content {
  display: inline;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
