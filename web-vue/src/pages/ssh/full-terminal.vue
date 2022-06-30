<template>
  <div>
    <a-spin :spinning="spinning">
      <a-card
        size="small"
        :bodyStyle="{
          height: `calc(100vh - 45px)`,
        }"
      >
        <template #title>
          <template v-if="sshData">
            <a-space>
              <div>{{ sshData.name }} ({{ sshData.host }})</div>

              <a-button size="small" type="primary" :disabled="!sshData.fileDirs" @click="handleFile()">文件</a-button>
            </a-space>
          </template>
          <template v-else>loading</template>
        </template>
        <a slot="extra" href="#"></a>
        <terminal v-if="sshData" :sshId="sshData.id" />
        <template v-else>
          <a-result status="404" title="404" sub-title="没有对应的SSH">
            <template #extra>
              <router-link :to="{ path: '/ssh', query: {} }">
                <a-button type="primary">返回首页</a-button>
              </router-link>
            </template>
          </a-result>
        </template>
      </a-card>
    </a-spin>
    <!-- 文件管理 -->
    <a-drawer v-if="sshData" :title="`${sshData.name} (${sshData.host}) 文件管理`" placement="right" width="90vw" :visible="drawerVisible" @close="onClose">
      <ssh-file v-if="drawerVisible" :ssh="sshData" />
    </a-drawer>
  </div>
</template>
<script>
import terminal from "./terminal";
import {getItem} from "@/api/ssh";
import SshFile from "@/pages/ssh/ssh-file";

export default {
  components: {
    terminal,
    SshFile,
  },

  data() {
    return {
      sshId: "",
      sshData: null,
      spinning: true,
      drawerVisible: false,
    };
  },
  computed: {},
  mounted() {
    this.sshId = this.$route.query.id;
    if (this.sshId) {
      this.loadItemData();
    }
  },
  beforeDestroy() {},
  methods: {
    loadItemData() {
      getItem({
        id: this.sshId,
      }).then((res) => {
        this.spinning = false;
        if (res.code === 200) {
          this.sshData = res.data;
        }
        // console.log(this.sshData);
      });
    },
    handleFile() {
      this.drawerVisible = true;
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false;
    },
  },
};
</script>
