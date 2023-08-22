package org.dromara.jpom.transport.protocol.extend;

import java.io.Serializable;

public interface RegisterDevice extends Serializable {

    String getName();

    String getVersion();

    String getHost();

    int getPort();

}
