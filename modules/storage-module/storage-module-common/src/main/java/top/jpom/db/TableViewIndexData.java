package top.jpom.db;

import lombok.Data;

/**
 * @author bwcx_jzy
 * @since 2023/1/6
 */
@Data
public class TableViewIndexData {

    private String indexType;

    private String tableName;

    private String name;

    /**
     * 多个，使用 + 分割
     */
    private String field;
}
