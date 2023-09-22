package org.dromara.jpom.transport.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 客户端连接状态事件
 * <br>
 * Created by Hong 2023/9/22
**/
@Getter
public class ClientStatusEvent extends ApplicationEvent {

    private final Status status;

    public ClientStatusEvent(Object source, Status status) {
        super(source);
        this.status = status;
    }

    public enum Status {

        CONNECT_SUCCESS,
        DISCONNECT_SUCCESS;

    }
}
