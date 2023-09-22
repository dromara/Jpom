package org.dromara.jpom.transport.protocol.extend;

import java.io.Serializable;

/**
 * 注册时携带的设备信息
 * @author Hong
 * @since 2023/09/22
**/
public interface RegisterDevice extends Serializable {

    String getName();

    String getVersion();

    String getHost();

    int getPort();

}
