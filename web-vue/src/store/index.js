import Vuex from "vuex";
import Vue from "vue";

import user from "./modules/user";
import app from "./modules/app";
import guide from "./modules/guide";

Vue.use(Vuex);

const store = new Vuex.Store({
  modules: {
    user,
    app,
    guide,
  },
});

export default store;
