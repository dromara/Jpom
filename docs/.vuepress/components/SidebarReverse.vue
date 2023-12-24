<template></template>

<script>
// 侧边栏倒序功能
export default {
  mounted() {
    this.reverse();
    this.WatchClick();
  },
  methods: {
    reverse() {
      var interval = setInterval(() => {
        let sidebar = document.getElementsByClassName("sidebar-group-items")[0];
        if (sidebar) {
          let lis = sidebar.childNodes;
          for (let i = lis.length - 1; i >= 0; i--) {
            sidebar.append(lis[i]);
          }
          clearInterval(interval);
        }
      }, 100);
    },
    WatchClick() {
      var interval = setInterval(() => {
        let sidebar = document.getElementsByClassName("sidebar-group");
        if (sidebar) {
          for (let i = 0; i < sidebar.length; i++) {
            sidebar[i].addEventListener("click", () => {
              this.reverse();
            });
          }
          clearInterval(interval);
        }
      }, 100);
    },
  },
};
</script>

<style>
</style>