package io.jpom.controller.user;

import io.jpom.common.BaseServerController;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bwcx_jzy
 * @since 2022/8/3
 */
@RestController
@RequestMapping(value = "/user-permission-group")
@Feature(cls = ClassFeature.USER_LOG)
public class UserPermissionGroupController extends BaseServerController {
}
