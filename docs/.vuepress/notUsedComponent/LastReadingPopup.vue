<template>
  <transition name="sw-update-popup">
    <div v-if="show" class="sw-update-popup">
      {{ message }}

      <br />

      <button @click="goto">
        {{ sureButtonText }}
      </button>
      <button @click="dontgoto">
        {{ cancelButtonText }}
      </button>
    </div>
  </transition>
</template>

<script>
export default {
  name: "LastReadingPopup",

  data() {
    return {
      lastReading: null,
      show: false,
    };
  },

  computed: {
    popupConfig() {
      const popupConfig = {
        "/": {
          // message: "Go back to the last reading.",
          // buttonText: "Go to",
          message: "检测到您上一次阅读的位置，是否移至该位置？",
          sureButtonText: "确定",
          cancelButtonText: "取消",
        },
        "/zh/": {
          message: "检测到您上一次阅读的位置，是否移至该位置？",
          sureButtonText: "前往",
          cancelButtonText: "取消",
        },
      };
      const lang = this.$lang.split("-")[0];
      return (
        popupConfig[`/${lang}/`] || popupConfig[this.$localePath] || popupConfig
      );
    },
    // 提示消息
    message() {
      const c = this.popupConfig;
      return (c && c.message) || c["/"].message;
    },
    // 确认按钮
    sureButtonText() {
      const c = this.popupConfig;
      return (c && c.sureButtonText) || c["/"].sureButtonText;
    },
    // 取消按钮
    cancelButtonText() {
      const c = this.popupConfig;
      return (c && c.cancelButtonText) || c["/"].cancelButtonText;
    },
  },

  // 如果不想使用该文件的效果，注释掉即可 mouted 函数的所有内容即可
  mounted() {
    if (!!window.ActiveXObject || "ActiveXObject" in window) {
      setTimeout(() => {
        window.addEventListener("load", this.init()); // for IE
      }, 1000);
    } else {
      setTimeout(() => {
        window.addEventListener("load", this.init);
      }, 1000);
    }
  },

  methods: {
    init() {
      this.lastReading = JSON.parse(localStorage.getItem("lastReading"));

      if (this.lastReading) {
        if (this.$route.path === this.lastReading.path) {
          this.goto();
        } else {
          this.show = true;
          10000 && setTimeout(this.clean, 10000);
        }
      }
    },

    goto() {
      if (this.$route.path !== this.lastReading.path) {
        this.$router.replace(this.lastReading.path).then(() => {
          document.documentElement.scrollTop = this.lastReading.scrollTop;
          this.clean();
        });
      } else {
        this.$nextTick(() => {
          document.documentElement.scrollTop = this.lastReading.scrollTop;
          // this.clean();
        });
      }
    },

    dontgoto() {
      this.clean();
    },

    clean() {
      this.show = false;
      localStorage.removeItem("lastReading");
    },
  },
};
</script>

<style scoped>
.sw-update-popup {
  position: fixed;
  right: 1em;
  bottom: 1em;
  padding: 1em;
  border: 1px solid #3eaf7c;
  border-radius: 3px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.5);
  text-align: center;
  z-index: 12;
}

.sw-update-popup > button {
  margin-top: 0.5em;
  padding: 0.25em 2em;
}

.sw-update-popup-enter-active,
.sw-update-popup-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.sw-update-popup-enter,
.sw-update-popup-leave-to {
  opacity: 0;
  transform: translate(0, 50%) scale(0.5);
}
</style>
