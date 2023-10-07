<template>
  <div class="node-full-content">
    <a-alert :message=$t('node.node_layout.nginx.cert.alert') banner />
    <!-- 数据表格 -->
    <a-table :data-source="list" size="middle" :loading="loading" :columns="columns" :pagination="false" bordered :rowKey="(record, index) => index">
      <template slot="title">
        <a-space>
          <a-button type="primary" @click="loadData">{{ $t('common.refresh') }}</a-button>
        </a-space>
      </template>
      <a-tooltip slot="id" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="name" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <a-tooltip slot="domain" slot-scope="text" placement="topLeft" :title="text">
        <span>{{ text }}</span>
      </a-tooltip>
      <template slot="operation" slot-scope="text, record">
        <a-space>
          <a-button size="small" type="primary" @click="handleDownload(record)">{{$t('common.import')}}</a-button>
          <a-button size="small" type="primary" @click="handleTemplate(record)">{{$t('common.template')}}</a-button>
          <a-button size="small" type="danger" @click="handleDelete(record)">{{$t('common.delete')}}</a-button>
        </a-space>
      </template>
    </a-table>

    <!-- 模板 -->
    <a-modal destroyOnClose v-model="templateVisible" :title=$t('node.node_layout.nginx.cert.configTemp') :footer="null" :maskClosable="true">
      <pre class="config">
      ssl	on;
      listen	443 ssl;
      server_name	{{ temp.domain }};
      ssl_certificate	{{ temp.cert }};
      ssl_certificate_key	{{ temp.key }};
      ssl_session_cache	shared:SSL:1m;
      ssl_session_timeout	5m;
      ssl_ciphers	ECDHE-RSA-AES128-GCM-SHA256:ECDHE:ECDH:AES:HIGH:!NULL:!aNULL:!MD5:!ADH:!RC4;
      ssl_protocols	TLSv1 TLSv1.1 TLSv1.2;
      ssl_prefer_server_ciphers	on;
      </pre>
    </a-modal>
  </div>
</template>
<script>
import { deleteCert, downloadCert, getCertList } from "@/api/node-nginx";
import { parseTime } from "@/utils/const";

export default {
  props: {
    node: {
      type: Object,
    },
  },
  data() {
    return {
      loading: false,
      tableHeight: "70vh",
      whiteList: [],
      list: [],

      temp: {},

      templateVisible: false,
      columns: [
        { title: "ID", dataIndex: "id", ellipsis: true, scopedSlots: { customRender: "id" } },
        { title: this.$t('common.name'), dataIndex: "name", ellipsis: true, scopedSlots: { customRender: "name" } },
        { title: this.$t('common.domain'), dataIndex: "domain", ellipsis: true, scopedSlots: { customRender: "domain" } },
        {
          title: this.$t('common.effectiveTime'),
          dataIndex: "effectiveTime",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        {
          title: this.$t('common.expirationTime'),
          dataIndex: "expirationTime",
          customRender: (text) => {
            if (!text) {
              return "";
            }
            return parseTime(text);
          },
          width: 180,
        },
        { title: this.$t('common.operation'), dataIndex: "operation", align: "center", scopedSlots: { customRender: "operation" }, width: 220 },
      ],
      rules: {
        id: [{ required: true, message: "Please input ID", trigger: "blur" }],
        name: [{ required: true, message: "Please input name", trigger: "blur" }],
        path: [{ required: true, message: "Please select path", trigger: "blur" }],
      },
    };
  },
  mounted() {
    // this.calcTableHeight();
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true;
      const params = {
        nodeId: this.node.id,
      };
      getCertList(params).then((res) => {
        if (res.code === 200) {
          this.list = res.data;
        }
        this.loading = false;
      });
    },

    // 删除
    handleDelete(record) {
      this.$confirm({
        title: this.$t('common.systemPrompt'),
        content: this.$t('node.node_layout.nginx.cert.deleteContent'),
        okText: this.$t('common.confirm'),
        cancelText: this.$t('common.cancel'),
        onOk: () => {
          // 组装参数
          const params = {
            nodeId: this.node.id,
            id: record.id,
          };
          deleteCert(params).then((res) => {
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
    // 下载证书文件
    handleDownload(record) {
      this.$notification.info({
        message: this.$t('node.node_layout.nginx.cert.downloadContent'),
      });
      // 请求参数
      const params = {
        nodeId: this.node.id,
        id: record.id,
      };
      // 请求接口拿到 blob
      window.open(downloadCert(params), "_blank");
    },
    // 显示模板
    handleTemplate(record) {
      this.temp = Object.assign({}, record);
      this.templateVisible = true;
    },
  },
};
</script>
<style scoped>
.config {
  background-color: black;
  color: #fff;
}
</style>
