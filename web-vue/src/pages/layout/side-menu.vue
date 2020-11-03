<template>
  <a-menu theme="dark" mode="inline">
    <a-sub-menu v-for="menu in getMenus" :key="menu.id">
      <span slot="title">
        <a-icon type="apartment" />
        <span>{{menu.title}}</span>
      </span>
      <a-menu-item v-for="subMenu in menu.childs" :key="subMenu.id" @click="handleClick(subMenu.url)">
        <span>{{subMenu.title}}</span>
      </a-menu-item>
    </a-sub-menu>
    
  </a-menu>
</template>
<script>
import { mapGetters } from 'vuex';
import routeMenuList from '../../router/route-menu';
export default {
  data() {
    return {
      routeList: routeMenuList
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
    // 匹配路由和菜单
    checkRouteMenu(id) {
      const tempList = this.routeList.filter(route => {
        return route.id === id;
      })
      return tempList.length > 0;
    },
    handleClick(path) {
      console.log(path)
      this.$router.push(path)
    }
  }
}
</script>