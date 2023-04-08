<template>
  <div>loading</div>
</template>
<script>
import { mapGetters } from "vuex";
export default {
  components: {},
  computed: {
    ...mapGetters(["getToken", "getUserInfo"]),
  },
  data() {
    return {};
  },

  watch: {},
  mounted() {
    if (this.getUserInfo && this.getToken) {
      this.$router.push({ path: "/node/list" });
    } else {
      // 将参数携带进去
      this.$router.push({
        path: "/login",
        query: this.$route.query,
      });
    }
  },
  methods: {},
};
</script>
