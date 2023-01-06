package top.jpom.db;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bwcx_jzy
 * @since 2023/1/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TableViewAlterData extends TableViewRowData {

    private String alterType;

    private String tableName;
}
