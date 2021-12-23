package io.jpom.service.h2db;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.DefaultSystemLog;
import io.jpom.model.BaseDbModel;
import io.jpom.model.BaseWorkspaceModel;
import io.jpom.model.PageResultDto;
import io.jpom.system.ServerExtConfigBean;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 自动清理日志
 *
 * @author bwcx_jzy
 * @since 2021/12/23
 */
public abstract class BaseLogAutoClearService<T extends BaseWorkspaceModel> extends BaseWorkspaceService<T> {

	@Override
	public void insert(T t) {
		super.insert(t);
		this.executeClear();
	}

	@Override
	public void insert(Collection<T> t) {
		super.insert(t);
		this.executeClear();
	}

	/**
	 * 执行清理
	 */
	private void executeClear() {
		ServerExtConfigBean instance = ServerExtConfigBean.getInstance();
		int h2DbLogStorageCount = instance.getH2DbLogStorageCount();
		if (h2DbLogStorageCount <= 0) {
			return;
		}
		this.executeClearImpl(h2DbLogStorageCount);
	}

	/**
	 * 清理分发实现
	 *
	 * @param h2DbLogStorageCount 保留数量
	 */
	protected void executeClearImpl(int h2DbLogStorageCount) {
		String[] strings = this.clearTimeColumns();
		for (String timeColumn : strings) {
			this.autoClear(timeColumn, h2DbLogStorageCount, time -> {
				Entity entity = Entity.create(super.getTableName());
				entity.set(timeColumn, "< " + time);
				int count = super.del(entity);
				DefaultSystemLog.getLog().info("{} 清理了 {}条数据", super.getTableName(), count);
			});
		}
	}

	/**
	 * 安装时间自动清理数据对字段
	 *
	 * @return 数组
	 */
	protected String[] clearTimeColumns() {
		return new String[]{"createTimeMillis"};
	}

	/**
	 * 自动清理数据接口
	 *
	 * @param timeColumn 时间字段
	 * @param maxCount   最大数量
	 * @param consumer   查询出超过范围的时间回调
	 */
	protected void autoClear(String timeColumn, int maxCount, Consumer<Long> consumer) {
		if (maxCount <= 0) {
			return;
		}
		ThreadUtil.execute(() -> {
			long timeValue = this.getLastTimeValue(timeColumn, maxCount, null);
			if (timeValue <= 0) {
				return;
			}
			consumer.accept(timeValue);
		});
	}

	/**
	 * 查询指定字段降序 指定条数对最后一个值
	 *
	 * @param timeColumn 时间字段
	 * @param maxCount   最大数量
	 * @param whereCon   添加查询条件回调
	 * @return 时间
	 */
	protected long getLastTimeValue(String timeColumn, int maxCount, Consumer<Entity> whereCon) {
		Entity entity = Entity.create(super.getTableName());
		if (whereCon != null) {
			// 条件
			whereCon.accept(entity);
		}
		Page page = new Page(maxCount, 1);
		page.addOrder(new Order(timeColumn, Direction.DESC));
		PageResultDto<T> pageResult = super.listPage(entity, page);
		if (pageResult.isEmpty()) {
			return 0L;
		}
		T entity1 = pageResult.get(0);
		Object fieldValue = ReflectUtil.getFieldValue(entity1, timeColumn);
		return Convert.toLong(fieldValue, 0L);
	}

	/**
	 * 自动清理数据接口
	 *
	 * @param timeClo  时间字段
	 * @param maxCount 最大数量
	 * @param consumer 查询出超过范围的时间回调
	 */
	protected void autoLoopClear(String timeClo, int maxCount, Consumer<Entity> whereCon, Consumer<T> consumer) {
		if (maxCount <= 0) {
			return;
		}
		ThreadUtil.execute(() -> {
			Entity entity = Entity.create(super.getTableName());
			long timeValue = this.getLastTimeValue(timeClo, maxCount, whereCon);
			if (timeValue <= 0) {
				return;
			}
			if (whereCon != null) {
				// 条件
				whereCon.accept(entity);
			}
			entity.set(timeClo, "< " + timeValue);
			while (true) {
				Page page = new Page(1, 50);
				page.addOrder(new Order(timeClo, Direction.DESC));
				PageResultDto<T> pageResult = super.listPage(entity, page);
				if (pageResult.isEmpty()) {
					return;
				}
				pageResult.each(consumer);
				List<String> ids = pageResult.getResult().stream().map(BaseDbModel::getId).collect(Collectors.toList());
				super.delByKey(ids, null);
			}
		});
	}
}
