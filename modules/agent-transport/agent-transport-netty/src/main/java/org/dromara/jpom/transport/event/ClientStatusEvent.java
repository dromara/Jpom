package org.dromara.jpom.transport.event;

import org.springframework.context.ApplicationEvent;

public class ClientStatusEvent extends ApplicationEvent {

    private final Status status;

    public ClientStatusEvent(Object source, Status status) {
        super(source);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {

        CONNECT_SUCCESS,
        DISCONNECT_SUCCESS;

    }
}
