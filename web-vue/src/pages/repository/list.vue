<template>
  <repository
    :workspaceId="this.getWorkspaceId()"
    :global="true"
    :choose="choose"
    :chooseVal="chooseVal"
    @confirm="confirm"
    ref="repository"
  >
  </repository>
</template>

<script>
import repository from './repository-list.vue'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
export default {
  props: {
    choose: {
      type: Boolean,
      default: false
    },
    chooseVal: {
      type: String,
      default: ''
    }
  },
  components: { repository },
  computed: { ...mapState(useAppStore, ['getWorkspaceId']) },
  methods: {
    confirm(data) {
      this.$emit('confirm', data)
    },
    handerConfirm() {
      this.$refs.repository.handerConfirm()
    }
  },
  emits: ['confirm']
}
</script>
