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
import cn.keepbx.jpom.service.node.ssh.SshService;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;
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
public class FileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements ChannelProgressiveFutureListener {

    private Session session = null;
    private ChannelSftp channel = null;
    private long fileSize;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //检测解码情况
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }
        //获取请求参数 共下面页面单个下载用
        Map<String, String> parse = parse(request);
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
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
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
        System.out.println(String.format("%.2f", (progress / (double) fileSize) * 100));
        if (total < 0) {
            // total unknown
            //                            System.err.println(future.channel() + " Transfer progress: " + progress);
        } else {
            //                            System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
            if (progress == total) {

            }
        }
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) throws Exception {
        this.close();
    }

    private void close() {
        JschUtil.close(channel);
        JschUtil.close(session);
    }
}