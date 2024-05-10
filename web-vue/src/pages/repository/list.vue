<template>
  <repository
    ref="repository"
    :workspace-id="getWorkspaceId()"
    :global="true"
    :choose="choose"
    :choose-val="chooseVal"
    @confirm="confirm"
  >
  </repository>
</template>

<script>
import repository from './repository-list.vue'
import { mapState } from 'pinia'
import { useAppStore } from '@/stores/app'
export default {
  components: { repository },
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
  emits: ['confirm'],
  computed: { ...mapState(useAppStore, ['getWorkspaceId']) },
  methods: {
    $tl(key, ...args) {
      return this.$t(`pages.repository.list.${key}`, ...args)
    },
    confirm(data) {
      this.$emit('confirm', data)
    },
    handerConfirm() {
      this.$refs.repository.handerConfirm()
    }
  }
}
</script>
