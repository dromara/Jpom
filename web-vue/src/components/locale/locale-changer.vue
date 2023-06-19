<template>
  <a-dropdown>
    <a class="ant-dropdown-link" @click="(e) => e.preventDefault()">
      {{ this.language }}
      <a-icon type="down" />
    </a>
    <a-menu slot="overlay">
      <a-menu-item @click="handleLocaleChange('zh')"> 中文 </a-menu-item>
      <a-menu-item @click="handleLocaleChange('en')"> English </a-menu-item>
    </a-menu>
  </a-dropdown>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  computed: {
    ...mapGetters(["getLocale"]),

    language() {
      switch (this.getLocale) {
        case "en":
          return "English";
        case "zh":
        default:
          return "中文";
      }
    },
  },
  inject: ["reload"],
  methods: {
    handleLocaleChange(locale) {
      this.$store.commit("setLocale", locale);
      this.$root.$i18n.locale = locale;
    },
  },
};
</script>
