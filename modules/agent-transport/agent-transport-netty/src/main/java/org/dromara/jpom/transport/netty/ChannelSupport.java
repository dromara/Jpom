package org.dromara.jpom.transport.netty;

/**
 * 是否支持该通道
 *
 * @author Hong
 * @since 2023/08/22
 */
public interface ChannelSupport {

    boolean support(String name);
}
