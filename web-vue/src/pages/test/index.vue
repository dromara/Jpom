<template>
  <code-editor v-model="code" showTool @checkJson="checkJson" :options="{ mode: 'application/json' }"> </code-editor>
</template>

<script>
import codeEditor from "@/components/codeEditor";
export default {
  components: {
    codeEditor,
  },
  data() {
    return {
      code: "",
    };
  },
  methods: {
    checkJson() {
      console.log(11);
    },
  },
};
</script>

<style></style>
