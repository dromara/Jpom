package cn.keepbx.jpom.controller.node.tomcat;

import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.plugin.ClassFeature;
import cn.keepbx.plugin.Feature;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author bwcx_jzy
 * @date 2019/8/16
 */
@Controller
@RequestMapping(value = TomcatManageController.TOMCAT_URL)
@Feature(cls = ClassFeature.TOMCAT)
public class TomcatProjectController extends BaseServerController {
}
