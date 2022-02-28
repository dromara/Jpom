package io.jpom;

/**
 * @author bwcx_jzy
 * @since 2022/2/28
 */
public class ApiDoc {

    /**
     * @author bwcx_jzy
     * @apiDefine loginUser
     * @apiUse defResultJson
     * @apiHeader {String} Authorization 用户token
     * @apiPermission login-user
     * @apiSuccess (800) {none} data 需要登录
     * @apiSuccess (801) {none} data 登录信息过期,但是可以续期
     * @apiSuccess (302) {none} data 当前用户没有操作权限
     * @apiSuccess (999) {none} data 当前 IP 不能访问
     */
    private void loginUser() {
    }

    /**
     * @author bwcx_jzy
     * @apiDefine defResultJson
     * @apiSuccessExample {json} Success-Response:
     * HTTP/1.1 200 OK
     * {
     * "code": "200",
     * "msg": "成功",
     * "data": {},
     * }
     */
    private void defResultJson() {
    }
}
