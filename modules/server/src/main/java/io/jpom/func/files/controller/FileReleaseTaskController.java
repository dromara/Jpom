package io.jpom.func.files.controller;

import io.jpom.common.BaseServerController;
import io.jpom.permission.ClassFeature;
import io.jpom.permission.Feature;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bwcx_jzy
 * @since 2023/3/18
 */
@RestController
@RequestMapping(value = "/file-storage/release-task")
@Feature(cls = ClassFeature.FILE_STORAGE)
public class FileReleaseTaskController extends BaseServerController {
}
