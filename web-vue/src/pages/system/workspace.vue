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
  <div>
    <a-tabs default-active-key="1">
      <a-tab-pane key="1" :tab="$t('i18n_ffa9fd37b5')"><workspaceList></workspaceList> </a-tab-pane>
      <a-tab-pane key="2" :tab="$t('i18n_74ea72bbd6')">
        <clusterList></clusterList>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>
<script>
import workspaceList from './workspace-list.vue'
import clusterList from './cluster-list.vue'
export default {
  components: {
    workspaceList,
    clusterList
  },
  data() {
    return {}
  },
  computed: {},
  created() {},
  methods: {}
}
</script>
