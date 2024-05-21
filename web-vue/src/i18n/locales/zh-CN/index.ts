///
/// Copyright (c) 2019 Of Him Code Technology Studio
/// Jpom is licensed under Mulan PSL v2.
/// You can use this software according to the terms and conditions of the Mulan PSL v2.
/// You may obtain a copy of Mulan PSL v2 at:
/// 			http://license.coscl.org.cn/MulanPSL2
/// THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
/// See the Mulan PSL v2 for more details.
///

import page404 from './pages/404/404'
import build from './pages/build'
import certificate from './pages/certificate'
import dispatch from './pages/dispatch'
import docker from './pages/docker'
import fileManager from './pages/file-manager'
import monitor from './pages/monitor'
import layout from './pages/layout'
import login from './pages/login'
import repository from './pages/repository'
import script from './pages/script'
import node from './pages/node'
import ssh from './pages/ssh'
import tools from './pages/tools'
import user from './pages/user'
export default {
  pages: {
    404: page404,
    build,
    certificate,
    dispatch,
    docker,
    fileManager,
    monitor,
    layout,
    login,
    repository,
    script,
    node,
    ssh,
    tools,
    user
  }
}
