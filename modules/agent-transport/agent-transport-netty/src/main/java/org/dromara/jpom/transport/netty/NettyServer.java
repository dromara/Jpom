package org.dromara.jpom.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.dromara.jpom.transport.properties.NettyProperties;
import org.dromara.jpom.transport.netty.service.ChannelServiceManager;
import org.dromara.jpom.transport.netty.service.NettyProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


/**
 * Netty服务端
 *
 * @author Hong
 * @since 2023/08/22
 */
public abstract class NettyServer implements CommandLineRunner, Closeable {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    /** boss 线程组用于处理连接工作 **/
    private final EventLoopGroup boss = new NioEventLoopGroup();

    /** work 线程组用于数据处理 **/
    private final EventLoopGroup work = new NioEventLoopGroup();

    private NettyProperties nettyProperties;

    private List<ChannelInitializerServer> supports;

    @Override
    public void run(String... args) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, work);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.SO_BACKLOG, 1024); //服务端可连接队列数,对应TCP/IP协议listen函数中backlog参数
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true); //设置TCP长连接,一般如果两个小时内没有数据的通信时,TCP会自动发送一个活动探测数据报文
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true); //将小的数据包包装成更大的帧进行传送，提高网络的负载,即TCP延迟传输
        Optional<ChannelInitializerServer> channelSupport = supports.stream().filter(it -> it.support(nettyProperties.getTcp())).findFirst();
        if (channelSupport.isPresent()) {
            bootstrap.childHandler(channelSupport.get());
        } else {
            log.info("Tcp方式错误，仅支持websocket/socket");
            throw new RuntimeException("Tcp方式错误，仅支持websocket/socket");
        }
        ChannelFuture future = bootstrap.bind(nettyProperties.getPort()).sync();
        if (future.isSuccess()) {
            log.info("启动 {} server, port: {}", nettyProperties.getTcp(), nettyProperties.getPort());
        }
        ChannelServiceManager.setChannelService(NettyProducer.INSTANCE);
    }

    @Autowired
    public void setNettyProperties(NettyProperties nettyProperties) {
        this.nettyProperties = nettyProperties;
    }

    @Autowired
    public void setSupports(List<ChannelInitializerServer> supports) {
        this.supports = supports;
    }

    @Override
    public void close() throws IOException {
        try {
            boss.shutdownGracefully().sync();
            work.shutdownGracefully().sync();
            log.info("关闭Netty");
        } catch (Exception e) {
            log.error("关闭Netty失败");
        }
    }
}
