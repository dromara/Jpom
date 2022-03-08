/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom;

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
