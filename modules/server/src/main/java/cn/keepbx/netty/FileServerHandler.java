package cn.keepbx.netty;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
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
import org.thymeleaf.util.StringUtils;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
 * 下载handler
 *
 * @author myzf
 * @date 2019/8/11 19:42
 */
public class FileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements ChannelProgressiveFutureListener {
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    private Session session = null;
    private ChannelSftp channel = null;

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
            long fileLength = attr.getSize();
            ChannelSftp finalChannel = channel;
            PipedInputStream pipedInputStream = new PipedInputStream();
            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
            ThreadUtil.execute(() -> {
                try {
                    finalChannel.get(normalize, pipedOutputStream, new ProgressMonitor(pipedOutputStream));
                } catch (SftpException e) {
                    DefaultSystemLog.ERROR().error("下载异常", e);
                }
            });
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            HttpUtil.setContentLength(response, fileLength);
            String s = toUtf8String(FileUtil.getName(name), request.headers());
            response.headers().set(CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
            // 设定默认文件输出名
            response.headers().add("Content-disposition", "attachment; filename=" + s);
            setDateAndCacheHeaders(response);
            if (HttpUtil.isKeepAlive(request)) {
                response.headers().set("CONNECTION", HttpHeaderValues.KEEP_ALIVE);
            }
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


    private static void setDateAndCacheHeaders(HttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));


        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));


        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
//        response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }


    public static String toUtf8String(String fileName, HttpHeaders headers) throws Exception {
        final String userAgent = headers.get(USER_AGENT);
        String finalFileName = null;
        if (StringUtils.contains(userAgent, "MSIE") || StringUtils.contains(userAgent, "Trident")) {// IE浏览器（旧版/新版）
            finalFileName = URLEncoder.encode(fileName, "UTF8");
        } else if (StringUtils.contains(userAgent, "Mozilla")) {// google,火狐浏览器
            finalFileName = new String(fileName.getBytes(), "ISO8859-1");
        } else {
            finalFileName = URLEncoder.encode(fileName, "UTF8");// 其他浏览器
        }
        return finalFileName;
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
            decoder.parameters().entrySet().forEach(entry -> {
                // entry.getValue()是一个List, 只取第一个元素
                parmMap.put(entry.getKey(), entry.getValue().get(0));
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