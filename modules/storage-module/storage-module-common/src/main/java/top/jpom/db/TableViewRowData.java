package top.jpom.db;

import lombok.Data;

/**
 * @author bwcx_jzy
 * @since 2023/1/6
 */
@Data
public class TableViewRowData {

    private String name;
    private String type;
    private Integer len;
    private String defaultValue;
    private Boolean notNull;
    private String comment;
}
