<!--

    Copyright (c) 2019 Of Him Code Technology Studio
    Jpom is licensed under Mulan PSL v2.
    You can use this software according to the terms and conditions of the Mulan PSL v2.
    You may obtain a copy of Mulan PSL v2 at:
    			http://license.coscl.org.cn/MulanPSL2
    THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
    See the Mulan PSL v2 for more details.

-->
<template>
  <terminal2 :url="socketUrl" />
</template>
<script>
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { getWebSocketUrl } from '@/api/config'
import terminal2 from '@/components/terminal'

// https://blog.csdn.net/qq_41840688/article/details/108636267

export default {
  components: {
    terminal2
  },
  props: {
    sshId: {
      type: String,
      default: ''
    },
    machineSshId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {}
  },
  computed: {
    ...mapState(useUserStore, ['getLongTermToken']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    socketUrl() {
      return getWebSocketUrl(
        '/socket/ssh',
        `id=${this.sshId}&machineSshId=${
          this.machineSshId
        }&nodeId=system&type=ssh&workspaceId=${this.getWorkspaceId()}`
      )
    }
  },
  mounted() {},
  beforeUnmount() {},
  methods: {}
}
</script>
