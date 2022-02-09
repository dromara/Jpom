package io.jpom.service.h2db;

import io.jpom.ApplicationStartTest;
import io.jpom.service.dblog.BackupInfoService;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;

/**
 * @author bwcx_jzy
 * @since 2022/2/9
 */
public class H2BackupTest extends ApplicationStartTest {

	@Resource
	protected BackupInfoService backupInfoService;

	@Test
	public void testBackup() {
		backupInfoService.checkAutoBackup();
	}
}
