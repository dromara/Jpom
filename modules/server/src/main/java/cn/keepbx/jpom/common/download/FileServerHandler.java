package cn.keepbx.jpom.common.download;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.SshModel;
import cn.keepbx.jpom.service.node.ssh.SshService;
import cn.keepbx.jpom.system.ServerConfigBean;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.util.StringUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;


/**
  * 下载handler
  * @Author: myzf
  * @Date: 2019/8/11 19:42
  * @param
*/

@Slf4j
public class FileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;



    public boolean downloadSftpFile(ChannelSftp channel,String remoteFilePath,String serverLocalFilePath)
    {
        FileOutputStream fieloutput = null;
        try
        {
            ServerConfigBean instance = ServerConfigBean.getInstance();
            File file = new File(serverLocalFilePath);
            fieloutput = new FileOutputStream(file);
            SftpATTRS attr = channel.stat(remoteFilePath);
            long fileSize = attr.getSize();
            channel.get(remoteFilePath, fieloutput,new ProgressMonitor(fileSize));
            return true;
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (SftpException e)
        {
            e.printStackTrace();
        } finally
        {
            if (null != fieloutput)
            {
                try
                {
                    fieloutput.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }







    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        //检测解码情况
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        Map<String, String> parse = parse(request);//获取请求参数 共下面页面单个下载用

        final String uri = request.uri();
      //  final String path = sanitizeUri(uri);
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
            Session session = null;
            ChannelSftp channel = null;
            try {
                session = JschUtil.openSession(sshModel.getHost(), sshModel.getPort(), sshModel.getUser(), sshModel.getPassword());
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                String normalize = FileUtil.normalize(path + "/" + name);
                downloadSftpFile(channel,normalize,"c:" + "/" + name);
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("下载失败", e);
            sendError(ctx, INTERNAL_SERVER_ERROR);
            return;
        }

        String path1 = "";
        if (path1 == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }


        //读取要下载的文件
        File file = new File("c:/"+name);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(ctx, file);
            } else {
                sendRedirect(ctx, uri + '/');
            }
            return;
        }
        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }
        String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return;
            }
        }
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, fileLength);
        String s = toUtf8String(file.getName(), request.headers());
        setContentTypeHeader(response, file);
        response.headers().add("Content-disposition", "attachment; filename=" + s);// 设定默认文件输出名
        setDateAndCacheHeaders(response, file);
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set("CONNECTION", HttpHeaderValues.KEEP_ALIVE);
        }

        ctx.write(response);
        ChannelFuture sendFileFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
        } else {
            sendFileFuture = ctx.write(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)), ctx.newProgressivePromise());
        }
        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                    if(progress == total){
                        log.info("下载完毕！");
                    }
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });


        ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        if (!HttpUtil.isKeepAlive(request)) {
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private static String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (!uri.startsWith("/")) {
            return null;
        }

        uri = uri.replace('/', File.separatorChar);

        if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.startsWith(".") || uri.endsWith(".")
                || INSECURE_URI.matcher(uri).matches()) {
            return null;
        }
        return SystemPropertyUtil.get("user.dir") + File.separator + uri;
    }

    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

    private static void sendListing(ChannelHandlerContext ctx, File dir) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

        StringBuilder buf = new StringBuilder();
        String dirPath = dir.getPath();

        buf.append("<!DOCTYPE html>\r\n");
        buf.append("<html><head><title>");
        buf.append("Listing of: ");
        buf.append(dirPath);
        buf.append("</title></head><body>\r\n");

        buf.append("<h3>Listing of: ");
        buf.append(dirPath);
        buf.append("</h3>\r\n");

        buf.append("<ul>");
        buf.append("<li><a href=\"../\">..</a></li>\r\n");

        for (File f : dir.listFiles()) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }

            String name = f.getName();
            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
                continue;
            }

            buf.append("<li><a href=\"");
            buf.append(name);
            buf.append("\">");
            buf.append(name);
            buf.append("</a></li>\r\n");
        }

        buf.append("</ul></body></html>\r\n");
        ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
        response.headers().set(LOCATION, newUri);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendCompelet(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.copiedBuffer("文件下载成功!" + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.write(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }



    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
    }


    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));


        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));


        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }


    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }



    public static String toUtf8String(String fileName, HttpHeaders headers) throws Exception {
        final String userAgent = headers.get(USER_AGENT);
        String finalFileName = null;
        if (StringUtils.contains(userAgent, "MSIE")|| StringUtils.contains(userAgent, "Trident")) {// IE浏览器（旧版/新版）
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
            decoder.parameters().entrySet().forEach( entry -> {
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
            log.error("不支持的请求方式");
        }

        return parmMap;
    }


}