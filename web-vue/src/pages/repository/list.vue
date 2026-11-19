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
    confirm(data) {
      this.$emit('confirm', data)
    },
    handerConfirm() {
      this.$refs.repository.handerConfirm()
    }
  }
}
</script>
