package cn.keepbx.util;

import cn.jiangzeyin.common.DefaultSystemLog;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * 邮件工具
 *
 * @author Arno
 */
public class EmailUtil {

    /**
     * 默认方式。 获取站点换存中的邮箱账户和授权码
     *
     * @param nickStr 昵称
     * @param to      接收者
     * @param subject 主题
     * @param msg     信息
     */
    public static void sendHtmlToEmail(String nickStr, String to, String subject, String msg) {
        String from = "arnohand@163.com";
        String password = "";
        String host = "smtp.163.com";
        // 1.创建一个程序与邮件服务器会话对象 Session
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "SMTP");
        props.setProperty("mail.smtp.timeout", "1000");
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.port", "465");
        //Linux下需要设置此项，Windows默认localhost为127.0.0.1
        props.setProperty("mail.smtp.socketFactory.port", "465");
        props.setProperty("mail.smtp.localhost", "127.0.0.1");
        //鉴权验证
        props.setProperty("mail.smtp.auth", "true");
        // 设置邮件服务器
        props.setProperty("mail.smtp.host", host);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                //发件人邮件用户名、密码
                return new PasswordAuthentication(from, password);
            }
        });
        send(session, from, nickStr, subject, msg, to);
    }

    /**
     * 发送邮件 使用SSL加密
     *
     * @param from    发件人邮箱
     * @param to      收件人邮箱
     * @param subject 主题
     * @param msg     消息体
     * @param nickStr 昵称
     */
    private static void send(Session session, String from, String nickStr, String subject, String msg, String to) {
        try {
            // 2.创建一个Message，它相当于是邮件内容
            Message message = new MimeMessage(session);
            String nick = "";
            try {
                nick = MimeUtility.encodeText(nickStr);
            } catch (UnsupportedEncodingException e) {
                DefaultSystemLog.ERROR().error("邮件发送失败。获取昵称失败", e);
            }
            // Set From: 发件人
            message.setFrom(new InternetAddress(nick + " <" + from + ">"));
            // Set To: 收件人
            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(to));
            // Set Subject: 标题
            message.setSubject(subject);
            // 设置消息体
            message.setText(msg);
            // 发送消息
            Transport.send(message);
            System.out.println("Sent message successfully....from " + from + " to " + to);
        } catch (MessagingException e) {
            DefaultSystemLog.ERROR().error("邮件发送失败", e);
        } catch (Exception e) {
            DefaultSystemLog.ERROR().error("邮件发送中出现异常", e);
        }
    }
}
