/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */
package org.dromara.jpom;

/**
 * apiDoc 通用文档块
 *
 * @author bwcx_jzy
 * @since 2022/2/28
 */
public interface ApiDoc {

    /**
     * 登录用户返回消息体
     *
     * @author bwcx_jzy
     *
     * @apiDefine loginUser
     * @apiUse defResultJson
     * @apiHeader {String} Authorization 用户token
     * @apiPermission login-user
     * @apiSuccess (800) {none} data 需要登录
     * @apiSuccess (801) {none} data 登录信息过期,但是可以续期
     * @apiSuccess (302) {none} data 当前用户没有操作权限
     * @apiSuccess (999) {none} data 当前 IP 不能访问
     */
    void loginUser();

    /**
     * 默认的通用返回消息体
     *
     * @author bwcx_jzy
     *
     * @apiDefine defResultJson
     * @apiSuccessExample {json} Success-Response:
     * HTTP/1.1 200 OK
     * {
     *   "code": "200",
     *   "msg": "成功",
     *   "data": {},
     * }
     */
    void defResultJson();
}
