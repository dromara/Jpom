<template>
  <terminal :url="this.socketUrl" />
</template>

<script>
import { mapState } from 'pinia'
import { getWebSocketUrl } from '@/api/config'
import terminal from '@/components/terminal'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
// https://blog.csdn.net/qq_41840688/article/details/108636267

export default {
  components: {
    terminal
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    containerId: {
      type: String
    },
    machineDockerId: {
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
        '/socket/docker_cli',
        `userId=${this.getLongTermToken}&id=${this.id}&machineDockerId=${
          this.machineDockerId
        }&nodeId=system&type=docker&containerId=${this.containerId}&workspaceId=${this.getWorkspaceId()}`
      )
    }
  },
  mounted() {},
  beforeUnmount() {},
  methods: {}
}
</script>
