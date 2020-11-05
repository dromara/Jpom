import Vuex from 'vuex';
import Vue from 'vue';

import user from './modules/user';
import app from './modules/app';

Vue.use(Vuex);

const store = new Vuex.Store({
  modules: {
    user,
    app
  }
})

export default store
