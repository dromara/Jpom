const TOKEN_KEY = 'JpomToken';

const user = {
  state: {
    token: localStorage.getItem(TOKEN_KEY)
  },
  mutations: {
    setToken(state, token) {
      state.token = token
    }
  },
  actions: {
    login({commit}, token) {
      commit('setToken', token);
      localStorage.setItem(TOKEN_KEY, token);
    }
  },
  getters: {
    getToken(state) {
      return state.token;
    }
  }
}

export default user
