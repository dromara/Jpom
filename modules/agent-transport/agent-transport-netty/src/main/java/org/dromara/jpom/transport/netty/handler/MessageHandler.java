package org.dromara.jpom.transport.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.dromara.jpom.transport.event.MessageEvent;
import org.dromara.jpom.transport.netty.service.ChannelServiceManager;
import org.dromara.jpom.transport.netty.service.NettyProducer;
import org.dromara.jpom.transport.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.util.StringUtils;

/**
 * Netty Message处理
 *
 * @author Hong
 * @since 2023/08/22
 */
public class MessageHandler extends SimpleChannelInboundHandler<Message> {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final ApplicationEventMulticaster eventMulticaster;

    public MessageHandler(ApplicationEventMulticaster eventMulticaster) {
        this.eventMulticaster = eventMulticaster;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Message message) throws Exception {
        //获取客户端发送的消息内容
        log.debug("消息Id: {}", message.messageId());
        log.debug("消息Cmd: {}", message.cmd());
        log.debug("消息Header: {}", message.header());
        if (message instanceof HeartbeatMessage) {
            log.debug("收到心跳");
        } else if (message instanceof RegisterMessage) {
            log.debug("收到注册消息");
            NettyProducer.register((RegisterMessage) message, channelHandlerContext.channel());
        } if (message instanceof NoneMessage) {
            log.info("收到未知消息");
        } else {
            String channelName = "";
            if (ChannelServiceManager.INSTANCE instanceof NettyProducer) {
                channelName = NettyProducer.getName(channelHandlerContext.channel());
                // 客户端如果未注册，接收到客户端的消息时就下线
                if (!StringUtils.hasText(channelName)) {
                    channelHandlerContext.channel().close();
                    return;
                }
            }
            eventMulticaster.multicastEvent(new MessageEvent<>(this, message, ChannelServiceManager.INSTANCE, channelName));
        }
    }
}
