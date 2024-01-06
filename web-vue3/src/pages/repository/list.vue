<template>
  <repository
    :workspaceId="this.getWorkspaceId"
    :global="true"
    :choose="choose"
    :chooseVal="chooseVal"
    @confirm="confirm"
  >
  </repository>
</template>

<script>
import repository from './repository-list.vue'
import { mapGetters } from 'vuex'
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
  computed: { ...mapGetters(['getWorkspaceId']) },
  methods: {
    confirm(data) {
      $emit(this, 'confirm', data)
    }
  },
  emits: ['confirm']
}
</script>
