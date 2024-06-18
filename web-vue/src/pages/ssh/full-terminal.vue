<template>
  <div>
    <a-spin :spinning="spinning">
      <a-card
        size="small"
        :body-style="{
          height: `calc(100vh - 45px)`
        }"
      >
        <template #title>
          <template v-if="sshData">
            <a-space>
              <div>
                {{ sshData.name }}
                <template v-if="sshData.host">({{ sshData.host }})</template>
              </div>

              <a-button size="small" type="primary" :disabled="!sshData.fileDirs" @click="handleFile()">{{
                $t('i18n_2a0c4740f1')
              }}</a-button>
            </a-space>
          </template>
          <template v-else>loading</template>
        </template>
        <template #extra>
          <a href="#"></a>
        </template>
        <terminal1 v-if="sshData" :ssh-id="sshData.id" />
        <template v-else>
          <a-result status="404" :title="$t('i18n_bda44edeb5')" :sub-title="$t('i18n_729eebb5ff')">
            <template #extra>
              <router-link :to="{ path: '/ssh', query: {} }">
                <a-button type="primary">{{ $t('i18n_5a1367058c') }}</a-button>
              </router-link>
            </template>
          </a-result>
        </template>
      </a-card>
    </a-spin>
    <!-- 文件管理 -->
    <CustomDrawer
      v-if="sshData && drawerVisible"
      destroy-on-close
      placement="right"
      width="90vw"
      :open="drawerVisible"
      @close="onClose"
    >
      <template #title>
        {{ sshData.name }}<template v-if="sshData.host"> ({{ sshData.host }}) </template>{{ $t('i18n_8780e6b3d1') }}
      </template>
      <ssh-file v-if="drawerVisible" :ssh-id="sshData.id" />
    </CustomDrawer>
  </div>
</template>
<script>
import terminal1 from './terminal'
import { getItem } from '@/api/ssh'
import SshFile from '@/pages/ssh/ssh-file'

export default {
  components: {
    terminal1,
    SshFile
  },

  data() {
    return {
      sshId: '',
      sshData: null,
      spinning: true,
      drawerVisible: false
    }
  },
  computed: {},
  mounted() {
    this.sshId = this.$route.query.id
    if (this.sshId) {
      this.loadItemData()
    }
  },
  beforeUnmount() {},
  methods: {
    loadItemData() {
      getItem({
        id: this.sshId
      }).then((res) => {
        this.spinning = false
        if (res.code === 200) {
          this.sshData = res.data
        }
        // console.log(this.sshData);
      })
    },
    handleFile() {
      this.drawerVisible = true
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false
    }
  }
}
</script>
