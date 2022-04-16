package io.jpom.controller;


import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author bwcx_jzy
 * @since 2022/4/16
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class MyErrorController extends BaseMyErrorController {

    public MyErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }
}
