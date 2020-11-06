<template>
  <div>
    <a-button>新增</a-button>
    <a-table :data-source="list" :columns="columns" bordered>

    </a-table>
  </div>
</template>
<script>
import { getRoleList } from '../../api/role';
export default {
  data() {
    return {
      list: [],
      columns: [
        {title: '角色名称', dataIndex: 'name'},
        {title: '授权人数', dataIndex: 'bindCount'},
        {title: '修改时间', dataIndex: 'updateTime'},
        {title: '操作', dataIndex: 'operation'}
      ]
    }
  },
  created() {
    this.loadData();
  },
  methods: {
    // 加载数据
    loadData() {
      getRoleList().then(res => {
        if (res.code === 200) {
          this.list = res.data;
        }
      })
    }
  }
}
</script>