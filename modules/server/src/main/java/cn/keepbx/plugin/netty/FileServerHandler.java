package cn.keepbx.plugin.netty;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.jpom.service.user.UserService;
import cn.keepbx.util.UserChannelRel;
import com.alibaba.fastjson.JSON;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 下载handler
 *
 * @author myzf
 * @date 2019/8/11 19:42
 */
public class FileServerHandler extends SimpleChannelInboundHandler<Object> implements ChannelProgressiveFutureListener {

    private Session session = null;
    private ChannelSftp channel = null;
    private long fileSize;

    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;

            //检测解码情况
            if (!request.decoderResult().isSuccess()) {
                sendError(ctx, BAD_REQUEST);
                return;
            }
            Map<String, String> parse = parse(request);
            //获取请求参数 共下面页面单个下载用
            String id = parse.get("id");
            String path = parse.get("path");
            String name = parse.get("name");
            SshService sshService = SpringUtil.getBean(SshService.class);
            SshModel sshModel = sshService.getItem(id);
            if (sshModel == null) {
                sendError(ctx, NOT_FOUND);
                return;
            }
            List<String> fileDirs = sshModel.getFileDirs();
            //
            if (StrUtil.isEmpty(path) || !fileDirs.contains(path)) {
                sendError(ctx, NOT_FOUND);
                return;
            }
            if (StrUtil.isEmpty(name)) {
                sendError(ctx, NOT_FOUND);
                return;
            }
            try {
                session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                String normalize = FileUtil.normalize(path + "/" + name);
                SftpATTRS attr = channel.stat(normalize);
                fileSize = attr.getSize();
                ChannelSftp finalChannel = channel;
                PipedInputStream pipedInputStream = new PipedInputStream();
                PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
                ThreadUtil.execute(() -> {
                    try {
                        finalChannel.get(normalize, pipedOutputStream);
                    } catch (SftpException e) {
                        DefaultSystemLog.ERROR().error("下载异常", e);
                    }
                    IoUtil.close(pipedOutputStream);
                });
                HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                HttpUtil.setContentLength(response, fileSize);

                response.headers().set(CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                // 设定默认文件输出名
                String fileName = URLUtil.encode(FileUtil.getName(name));
                response.headers().add("Content-disposition", "attachment; filename=" + fileName);

                ctx.write(response);
                ChannelFuture sendFileFuture = ctx.write(new HttpChunkedInput(new ChunkedStream(pipedInputStream)), ctx.newProgressivePromise());
                sendFileFuture.addListener(this);

                ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

                if (!HttpUtil.isKeepAlive(request)) {
                    lastContentFuture.addListener(ChannelFutureListener.CLOSE);
                }
            } catch (Exception e) {
                DefaultSystemLog.ERROR().error("下载失败", e);
                sendError(ctx, INTERNAL_SERVER_ERROR);
            }
        } else if (msg instanceof WebSocketFrame) {
            TextWebSocketFrame tmsg = (TextWebSocketFrame) msg;
            // 获取客户端传输过来的消息
            String content = tmsg.text();
            if (StrUtil.hasEmpty(content)) {
                ctx.channel().close();

            }
            // 1. 获取客户端发来的消息
            AuthMsg authMsg = JSON.parseObject(content, AuthMsg.class);
            if (UserChannelRel.get(authMsg.getUserId()) == null) {
                if (1 == authMsg.getType()) {//鉴权类型的
                    UserService userService = SpringUtil.getBean(UserService.class);
                    UserModel userModel = userService.checkUser(authMsg.getUserId());
                    if (userModel == null) {
                        //鉴权失败
                        ctx.channel().writeAndFlush(new TextWebSocketFrame("鉴权失败!"));
                        ctx.channel().close();
                    }
                }

            }


        }


    }


    /**
     * 当客户端连接服务端之后（打开连接）
     * 获取客户端的channle，并且放到ChannelGroup中去进行管理
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

        String channelId = ctx.channel().id().asShortText();
        System.out.println("客户端被移除，channelId为：" + channelId);
        channels.remove(ctx.channel());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
        ctx.channel().close();
        channels.remove(ctx.channel());
    }


    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 解析netty
     * 请求参数
     *
     * @return 包含所有请求参数的键值对, 如果没有参数, 则返回空Map
     * @throws
     * @throws IOException
     */
    public Map<String, String> parse(FullHttpRequest request) throws IOException {
        HttpMethod method = request.method();
        Map<String, String> parmMap = new HashMap<>();
        if (HttpMethod.GET == method) {
            // 是GET请求
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            decoder.parameters().forEach((key, value) -> {
                // entry.getValue()是一个List, 只取第一个元素
                parmMap.put(key, value.get(0));
            });
        } else if (HttpMethod.POST == method) {
            // 是POST请求
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
            decoder.offer(request);
            List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
            for (InterfaceHttpData parm : parmList) {
                Attribute data = (Attribute) parm;
                parmMap.put(data.getName(), data.getValue());
            }

        } else {
        }
        return parmMap;
    }


    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
        if (total < 0) {
            String s = String.format("%.2f", (progress / (double) fileSize) * 100) + "%";
            // websocket连接在线
            for (Channel channel : channels) {
                channel.writeAndFlush(
                        new TextWebSocketFrame(
                                s));
            }
            System.err.println(s);

        }
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) throws Exception {
        for (Channel channel : channels) {
            channel.writeAndFlush(
                    new TextWebSocketFrame(
                            "complete"));
        }
        this.close();
    }

    private void close() {
        JschUtil.close(channel);
        JschUtil.close(session);
    }
}