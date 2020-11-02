<template>
  <a-menu theme="dark" mode="inline">
    <a-sub-menu v-for="menu in getMenus" :key="menu.id">
      <span slot="title">
        <a-icon type="apartment" />
        <span>{{menu.title}}</span>
      </span>
      <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id">
        <span>{{subMenu.title}}</span>
      </a-menu-item>
    </a-sub-menu>
  </a-menu>
</template>
<script>
import { mapGetters } from 'vuex';
export default {
  data() {
    return {

    }
  },
  computed: {
    ...mapGetters([
      'getMenus'
    ])
  },
  created() {
  },
  methods: {
  }
}
</script>