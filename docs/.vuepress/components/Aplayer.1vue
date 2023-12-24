<template></template>

<script>
export default {
  mounted() {
    let aplayer = document.getElementsByClassName("aplayer-body")[0];
    if (aplayer) {
      aplayer.style.transform = "translateX(-66px)";
      let miniswitcher = document.getElementsByClassName(
        "aplayer-icon-miniswitcher"
      )[0];
      miniswitcher.addEventListener("click", () => {
        let arrow = document.getElementsByClassName("aplayer-arrow")[0];
        if (arrow) {
          aplayer.style.transform = "translateX(0)";
        } else {
          aplayer.style.transform = "translateX(-66px)";
        }
      });
    }
  },
};
</script>

<style>
</style>