// vite.config.ts
import path from "node:path";
import { defineConfig, loadEnv } from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/vite/dist/node/index.js";
import vue from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import vueJsx from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/@vitejs/plugin-vue-jsx/dist/index.mjs";
import { createHtmlPlugin } from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/vite-plugin-html/dist/index.mjs";
import { visualizer } from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/rollup-plugin-visualizer/dist/plugin/index.js";
import AutoImport from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/unplugin-vue-components/dist/vite.js";
import { AntDesignVueResolver } from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/unplugin-vue-components/dist/resolvers.js";
import postcss from "file:///Users/zhaozhongyang/Desktop/Jpom/web-vue/node_modules/postcss/lib/postcss.mjs";
var __vite_injected_original_dirname = "/Users/zhaozhongyang/Desktop/Jpom/web-vue";
var vite_config_default = defineConfig(({ mode }) => {
  const env = loadEnv(mode, __vite_injected_original_dirname, "JPOM");
  const { JPOM_PROXY_HOST: HOST = "", JPOM_BASE_URL = "", JPOM_PORT = "" } = env;
  console.log(env, `\u5F53\u524D\u4E3A${mode}\u73AF\u5883`);
  return {
    base: JPOM_BASE_URL,
    // 公共基础路径，如当值为jpom时网站访问路径为：https://jpom.top/jpom
    envPrefix: "JPOM_",
    // 可在项目中通过import.meta.env.JPOM_xxx获取环境变量
    resolve: {
      alias: {
        "@/": `${path.resolve(__vite_injected_original_dirname, "src")}/`
      },
      // 忽略后缀名的配置选项
      extensions: [".mjs", ".js", ".ts", ".jsx", ".tsx", ".json", ".vue"]
    },
    build: {
      sourcemap: mode !== "production",
      // 非生产环境都生成sourcemap
      outDir: "../modules/server/src/main/resources/dist",
      rollupOptions: {
        output: {
          // 用于从入口点创建的块的打包输出格式[name]表示文件名,[hash]表示该文件内容hash值
          entryFileNames: "assets/js/[name].[hash].js",
          // 用于命名代码拆分时创建的共享块的输出命名
          chunkFileNames: "assets/js/[name].[hash].js",
          // 用于输出静态资源的命名，[ext]表示文件扩展名
          assetFileNames: "assets/[ext]/[name].[hash].[ext]"
        }
      },
      //打包前清空文件，默认true
      emptyOutDir: true,
      modulePreload: { polyfill: true },
      polyfillModulePreload: true,
      manifest: false
    },
    server: {
      port: Number(JPOM_PORT),
      host: "0.0.0.0",
      proxy: {
        // http
        "/api": {
          target: HOST.includes("http") ? HOST : `http://${HOST}`,
          changeOrigin: true,
          ws: true,
          rewrite: (path2) => path2.replace(/^\/api/, ""),
          timeout: 10 * 60 * 1e3
        }
      }
    },
    plugins: [
      vue(),
      vueJsx(),
      AutoImport({
        //安装两行后你会发现在组件中不用再导入ref，reactive等
        imports: ["vue", "vue-router", "pinia"],
        dts: "src/d.ts/auto-import.d.ts",
        eslintrc: {
          enabled: true,
          filepath: ".eslintrc-auto-import.json",
          globalsPropValue: true
        },
        //ant-design-vue
        resolvers: [AntDesignVueResolver()]
      }),
      AutoImport({
        dirs: ["src/d.ts/global"],
        dts: "src/d.ts/auto-global-import.d.ts",
        eslintrc: {
          enabled: true,
          filepath: ".eslintrc-global-import.json",
          globalsPropValue: true
        }
      }),
      Components({
        dts: "src/d.ts/components.d.ts",
        //ant-design-vue
        resolvers: [AntDesignVueResolver({ importStyle: false, resolveIcons: true })]
      }),
      createHtmlPlugin({
        minify: true,
        viteNext: true,
        inject: {
          data: {
            title: env.JPOM_APP_TITLE,
            base_url: env.JPOM_BASE_URL,
            build: (/* @__PURE__ */ new Date()).getTime(),
            env: process.env.NODE_ENV,
            buildVersion: process.env.npm_package_version
          }
        }
      }),
      visualizer({
        emitFile: false,
        // file: 'states.html',
        open: true
      }),
      {
        name: "vite-plugin-skip-empty-css",
        async transform(code, id) {
          if (/(\.css|\.scss|\.less)$/.test(id)) {
            if (code === "")
              return "";
            const { root } = await postcss([
              {
                postcssPlugin: "check-empty-or-comments-only"
              }
            ]).process(code, { from: id });
            if (root.nodes.length === 0 || root.nodes.every((node) => {
              return node.type === "comment";
            })) {
              return "";
            }
            return code;
          }
          return code;
        }
      }
    ]
  };
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvVXNlcnMvemhhb3pob25neWFuZy9EZXNrdG9wL0pwb20vd2ViLXZ1ZVwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL1VzZXJzL3poYW96aG9uZ3lhbmcvRGVza3RvcC9KcG9tL3dlYi12dWUvdml0ZS5jb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL1VzZXJzL3poYW96aG9uZ3lhbmcvRGVza3RvcC9KcG9tL3dlYi12dWUvdml0ZS5jb25maWcudHNcIjsvLy9cbi8vLyBDb3B5cmlnaHQgKGMpIDIwMTkgT2YgSGltIENvZGUgVGVjaG5vbG9neSBTdHVkaW9cbi8vLyBKcG9tIGlzIGxpY2Vuc2VkIHVuZGVyIE11bGFuIFBTTCB2Mi5cbi8vLyBZb3UgY2FuIHVzZSB0aGlzIHNvZnR3YXJlIGFjY29yZGluZyB0byB0aGUgdGVybXMgYW5kIGNvbmRpdGlvbnMgb2YgdGhlIE11bGFuIFBTTCB2Mi5cbi8vLyBZb3UgbWF5IG9idGFpbiBhIGNvcHkgb2YgTXVsYW4gUFNMIHYyIGF0OlxuLy8vIFx0XHRcdGh0dHA6Ly9saWNlbnNlLmNvc2NsLm9yZy5jbi9NdWxhblBTTDJcbi8vLyBUSElTIFNPRlRXQVJFIElTIFBST1ZJREVEIE9OIEFOIFwiQVMgSVNcIiBCQVNJUywgV0lUSE9VVCBXQVJSQU5USUVTIE9GIEFOWSBLSU5ELCBFSVRIRVIgRVhQUkVTUyBPUiBJTVBMSUVELCBJTkNMVURJTkcgQlVUIE5PVCBMSU1JVEVEIFRPIE5PTi1JTkZSSU5HRU1FTlQsIE1FUkNIQU5UQUJJTElUWSBPUiBGSVQgRk9SIEEgUEFSVElDVUxBUiBQVVJQT1NFLlxuLy8vIFNlZSB0aGUgTXVsYW4gUFNMIHYyIGZvciBtb3JlIGRldGFpbHMuXG4vLy9cblxuaW1wb3J0IHBhdGggZnJvbSAnbm9kZTpwYXRoJ1xuaW1wb3J0IHsgQ29uZmlnRW52LCBkZWZpbmVDb25maWcsIGxvYWRFbnYgfSBmcm9tICd2aXRlJ1xuaW1wb3J0IHZ1ZSBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUnXG5pbXBvcnQgdnVlSnN4IGZyb20gJ0B2aXRlanMvcGx1Z2luLXZ1ZS1qc3gnXG5pbXBvcnQgeyBjcmVhdGVIdG1sUGx1Z2luIH0gZnJvbSAndml0ZS1wbHVnaW4taHRtbCdcbmltcG9ydCB7IHZpc3VhbGl6ZXIgfSBmcm9tICdyb2xsdXAtcGx1Z2luLXZpc3VhbGl6ZXInXG4vL1x1ODFFQVx1NTJBOFx1NUJGQ1x1NTE2NXZ1ZVx1NEUyRGhvb2sgcmVhY3RpdmUgcmVmXHU3QjQ5XG5pbXBvcnQgQXV0b0ltcG9ydCBmcm9tICd1bnBsdWdpbi1hdXRvLWltcG9ydC92aXRlJ1xuLy9cdTgxRUFcdTUyQThcdTVCRkNcdTUxNjV1aS1cdTdFQzRcdTRFRjYgXHU2QkQ0XHU1OTgyXHU4QkY0YW50LWRlc2lnbi12dWUgIGVsZW1lbnQtcGx1c1x1N0I0OVxuaW1wb3J0IENvbXBvbmVudHMgZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvdml0ZSdcbi8vYW50LWRlc2lnbi12dWVcbmltcG9ydCB7IEFudERlc2lnblZ1ZVJlc29sdmVyIH0gZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvcmVzb2x2ZXJzJ1xuaW1wb3J0IHBvc3Rjc3MgZnJvbSAncG9zdGNzcydcblxuLy8gaHR0cHM6Ly92aXRlanMuZGV2L2NvbmZpZy9cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZygoeyBtb2RlIH06IENvbmZpZ0VudikgPT4ge1xuICAvLyBcdTUyQTBcdThGN0RcdTczQUZcdTU4ODNcdTkxNERcdTdGNkVcbiAgY29uc3QgZW52OiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0gbG9hZEVudihtb2RlLCBfX2Rpcm5hbWUsICdKUE9NJylcbiAgY29uc3QgeyBKUE9NX1BST1hZX0hPU1Q6IEhPU1QgPSAnJywgSlBPTV9CQVNFX1VSTCA9ICcnLCBKUE9NX1BPUlQgPSAnJyB9OiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0gZW52XG4gIGNvbnNvbGUubG9nKGVudiwgYFx1NUY1M1x1NTI0RFx1NEUzQSR7bW9kZX1cdTczQUZcdTU4ODNgKVxuXG4gIHJldHVybiB7XG4gICAgYmFzZTogSlBPTV9CQVNFX1VSTCwgLy8gXHU1MTZDXHU1MTcxXHU1N0ZBXHU3ODQwXHU4REVGXHU1Rjg0XHVGRjBDXHU1OTgyXHU1RjUzXHU1MDNDXHU0RTNBanBvbVx1NjVGNlx1N0Y1MVx1N0FEOVx1OEJCRlx1OTVFRVx1OERFRlx1NUY4NFx1NEUzQVx1RkYxQWh0dHBzOi8vanBvbS50b3AvanBvbVxuICAgIGVudlByZWZpeDogJ0pQT01fJywgLy8gXHU1M0VGXHU1NzI4XHU5ODc5XHU3NkVFXHU0RTJEXHU5MDFBXHU4RkM3aW1wb3J0Lm1ldGEuZW52LkpQT01feHh4XHU4M0I3XHU1M0Q2XHU3M0FGXHU1ODgzXHU1M0Q4XHU5MUNGXG5cbiAgICByZXNvbHZlOiB7XG4gICAgICBhbGlhczoge1xuICAgICAgICAnQC8nOiBgJHtwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnc3JjJyl9L2BcbiAgICAgIH0sXG5cbiAgICAgIC8vIFx1NUZGRFx1NzU2NVx1NTQwRVx1N0YwMFx1NTQwRFx1NzY4NFx1OTE0RFx1N0Y2RVx1OTAwOVx1OTg3OVxuICAgICAgZXh0ZW5zaW9uczogWycubWpzJywgJy5qcycsICcudHMnLCAnLmpzeCcsICcudHN4JywgJy5qc29uJywgJy52dWUnXVxuICAgIH0sXG4gICAgYnVpbGQ6IHtcbiAgICAgIHNvdXJjZW1hcDogbW9kZSAhPT0gJ3Byb2R1Y3Rpb24nLCAvLyBcdTk3NUVcdTc1MUZcdTRFQTdcdTczQUZcdTU4ODNcdTkwRkRcdTc1MUZcdTYyMTBzb3VyY2VtYXBcbiAgICAgIG91dERpcjogJy4uL21vZHVsZXMvc2VydmVyL3NyYy9tYWluL3Jlc291cmNlcy9kaXN0JyxcbiAgICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgICAgb3V0cHV0OiB7XG4gICAgICAgICAgLy8gXHU3NTI4XHU0RThFXHU0RUNFXHU1MTY1XHU1M0UzXHU3MEI5XHU1MjFCXHU1RUZBXHU3Njg0XHU1NzU3XHU3Njg0XHU2MjUzXHU1MzA1XHU4RjkzXHU1MUZBXHU2ODNDXHU1RjBGW25hbWVdXHU4ODY4XHU3OTNBXHU2NTg3XHU0RUY2XHU1NDBELFtoYXNoXVx1ODg2OFx1NzkzQVx1OEJFNVx1NjU4N1x1NEVGNlx1NTE4NVx1NUJCOWhhc2hcdTUwM0NcbiAgICAgICAgICBlbnRyeUZpbGVOYW1lczogJ2Fzc2V0cy9qcy9bbmFtZV0uW2hhc2hdLmpzJywgLy8gXHU3NTI4XHU0RThFXHU1NDdEXHU1NDBEXHU0RUUzXHU3ODAxXHU2MkM2XHU1MjA2XHU2NUY2XHU1MjFCXHU1RUZBXHU3Njg0XHU1MTcxXHU0RUFCXHU1NzU3XHU3Njg0XHU4RjkzXHU1MUZBXHU1NDdEXHU1NDBEXG4gICAgICAgICAgY2h1bmtGaWxlTmFtZXM6ICdhc3NldHMvanMvW25hbWVdLltoYXNoXS5qcycsIC8vIFx1NzUyOFx1NEU4RVx1OEY5M1x1NTFGQVx1OTc1OVx1NjAwMVx1OEQ0NFx1NkU5MFx1NzY4NFx1NTQ3RFx1NTQwRFx1RkYwQ1tleHRdXHU4ODY4XHU3OTNBXHU2NTg3XHU0RUY2XHU2MjY5XHU1QzU1XHU1NDBEXG4gICAgICAgICAgYXNzZXRGaWxlTmFtZXM6ICdhc3NldHMvW2V4dF0vW25hbWVdLltoYXNoXS5bZXh0XSdcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICAgIC8vXHU2MjUzXHU1MzA1XHU1MjREXHU2RTA1XHU3QTdBXHU2NTg3XHU0RUY2XHVGRjBDXHU5RUQ4XHU4QkE0dHJ1ZVxuICAgICAgZW1wdHlPdXREaXI6IHRydWUsXG4gICAgICBtb2R1bGVQcmVsb2FkOiB7IHBvbHlmaWxsOiB0cnVlIH0sXG4gICAgICBwb2x5ZmlsbE1vZHVsZVByZWxvYWQ6IHRydWUsXG4gICAgICBtYW5pZmVzdDogZmFsc2VcbiAgICB9LFxuICAgIHNlcnZlcjoge1xuICAgICAgcG9ydDogTnVtYmVyKEpQT01fUE9SVCksXG4gICAgICBob3N0OiAnMC4wLjAuMCcsXG4gICAgICBwcm94eToge1xuICAgICAgICAvLyBodHRwXG4gICAgICAgICcvYXBpJzoge1xuICAgICAgICAgIHRhcmdldDogSE9TVC5pbmNsdWRlcygnaHR0cCcpID8gSE9TVCA6IGBodHRwOi8vJHtIT1NUfWAsXG4gICAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLFxuICAgICAgICAgIHdzOiB0cnVlLFxuICAgICAgICAgIHJld3JpdGU6IChwYXRoKSA9PiBwYXRoLnJlcGxhY2UoL15cXC9hcGkvLCAnJyksXG4gICAgICAgICAgdGltZW91dDogMTAgKiA2MCAqIDEwMDBcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH0sXG4gICAgcGx1Z2luczogW1xuICAgICAgdnVlKCksXG4gICAgICB2dWVKc3goKSxcbiAgICAgIEF1dG9JbXBvcnQoe1xuICAgICAgICAvL1x1NUI4OVx1ODhDNVx1NEUyNFx1ODg0Q1x1NTQwRVx1NEY2MFx1NEYxQVx1NTNEMVx1NzNCMFx1NTcyOFx1N0VDNFx1NEVGNlx1NEUyRFx1NEUwRFx1NzUyOFx1NTE4RFx1NUJGQ1x1NTE2NXJlZlx1RkYwQ3JlYWN0aXZlXHU3QjQ5XG4gICAgICAgIGltcG9ydHM6IFsndnVlJywgJ3Z1ZS1yb3V0ZXInLCAncGluaWEnXSxcbiAgICAgICAgZHRzOiAnc3JjL2QudHMvYXV0by1pbXBvcnQuZC50cycsXG4gICAgICAgIGVzbGludHJjOiB7XG4gICAgICAgICAgZW5hYmxlZDogdHJ1ZSxcbiAgICAgICAgICBmaWxlcGF0aDogJy5lc2xpbnRyYy1hdXRvLWltcG9ydC5qc29uJyxcbiAgICAgICAgICBnbG9iYWxzUHJvcFZhbHVlOiB0cnVlXG4gICAgICAgIH0sXG5cbiAgICAgICAgLy9hbnQtZGVzaWduLXZ1ZVxuICAgICAgICByZXNvbHZlcnM6IFtBbnREZXNpZ25WdWVSZXNvbHZlcigpXVxuICAgICAgfSksXG4gICAgICBBdXRvSW1wb3J0KHtcbiAgICAgICAgZGlyczogWydzcmMvZC50cy9nbG9iYWwnXSxcbiAgICAgICAgZHRzOiAnc3JjL2QudHMvYXV0by1nbG9iYWwtaW1wb3J0LmQudHMnLFxuICAgICAgICBlc2xpbnRyYzoge1xuICAgICAgICAgIGVuYWJsZWQ6IHRydWUsXG4gICAgICAgICAgZmlsZXBhdGg6ICcuZXNsaW50cmMtZ2xvYmFsLWltcG9ydC5qc29uJyxcbiAgICAgICAgICBnbG9iYWxzUHJvcFZhbHVlOiB0cnVlXG4gICAgICAgIH1cbiAgICAgIH0pLFxuICAgICAgQ29tcG9uZW50cyh7XG4gICAgICAgIGR0czogJ3NyYy9kLnRzL2NvbXBvbmVudHMuZC50cycsXG4gICAgICAgIC8vYW50LWRlc2lnbi12dWVcbiAgICAgICAgcmVzb2x2ZXJzOiBbQW50RGVzaWduVnVlUmVzb2x2ZXIoeyBpbXBvcnRTdHlsZTogZmFsc2UsIHJlc29sdmVJY29uczogdHJ1ZSB9KV1cbiAgICAgIH0pLFxuICAgICAgY3JlYXRlSHRtbFBsdWdpbih7XG4gICAgICAgIG1pbmlmeTogdHJ1ZSxcbiAgICAgICAgdml0ZU5leHQ6IHRydWUsXG4gICAgICAgIGluamVjdDoge1xuICAgICAgICAgIGRhdGE6IHtcbiAgICAgICAgICAgIHRpdGxlOiBlbnYuSlBPTV9BUFBfVElUTEUsXG4gICAgICAgICAgICBiYXNlX3VybDogZW52LkpQT01fQkFTRV9VUkwsXG4gICAgICAgICAgICBidWlsZDogbmV3IERhdGUoKS5nZXRUaW1lKCksXG4gICAgICAgICAgICBlbnY6IHByb2Nlc3MuZW52Lk5PREVfRU5WLFxuICAgICAgICAgICAgYnVpbGRWZXJzaW9uOiBwcm9jZXNzLmVudi5ucG1fcGFja2FnZV92ZXJzaW9uXG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KSxcbiAgICAgIHZpc3VhbGl6ZXIoe1xuICAgICAgICBlbWl0RmlsZTogZmFsc2UsXG4gICAgICAgIC8vIGZpbGU6ICdzdGF0ZXMuaHRtbCcsXG4gICAgICAgIG9wZW46IHRydWVcbiAgICAgIH0pLFxuICAgICAge1xuICAgICAgICBuYW1lOiAndml0ZS1wbHVnaW4tc2tpcC1lbXB0eS1jc3MnLFxuICAgICAgICBhc3luYyB0cmFuc2Zvcm0oY29kZSwgaWQpIHtcbiAgICAgICAgICBpZiAoLyhcXC5jc3N8XFwuc2Nzc3xcXC5sZXNzKSQvLnRlc3QoaWQpKSB7XG4gICAgICAgICAgICBpZiAoY29kZSA9PT0gJycpIHJldHVybiAnJ1xuICAgICAgICAgICAgY29uc3QgeyByb290IH0gPSBhd2FpdCBwb3N0Y3NzKFtcbiAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgIHBvc3Rjc3NQbHVnaW46ICdjaGVjay1lbXB0eS1vci1jb21tZW50cy1vbmx5J1xuICAgICAgICAgICAgICB9XG4gICAgICAgICAgICBdKS5wcm9jZXNzKGNvZGUsIHsgZnJvbTogaWQgfSlcbiAgICAgICAgICAgIGlmIChcbiAgICAgICAgICAgICAgcm9vdC5ub2Rlcy5sZW5ndGggPT09IDAgfHxcbiAgICAgICAgICAgICAgcm9vdC5ub2Rlcy5ldmVyeSgobm9kZSkgPT4ge1xuICAgICAgICAgICAgICAgIHJldHVybiBub2RlLnR5cGUgPT09ICdjb21tZW50J1xuICAgICAgICAgICAgICB9KVxuICAgICAgICAgICAgKSB7XG4gICAgICAgICAgICAgIHJldHVybiAnJ1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuIGNvZGVcbiAgICAgICAgICB9XG4gICAgICAgICAgcmV0dXJuIGNvZGVcbiAgICAgICAgfVxuICAgICAgfVxuICAgIF1cbiAgfVxufSlcbiJdLAogICJtYXBwaW5ncyI6ICI7QUFVQSxPQUFPLFVBQVU7QUFDakIsU0FBb0IsY0FBYyxlQUFlO0FBQ2pELE9BQU8sU0FBUztBQUNoQixPQUFPLFlBQVk7QUFDbkIsU0FBUyx3QkFBd0I7QUFDakMsU0FBUyxrQkFBa0I7QUFFM0IsT0FBTyxnQkFBZ0I7QUFFdkIsT0FBTyxnQkFBZ0I7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
