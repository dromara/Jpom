import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.util.Vector;
import java.util.function.Consumer;

/**
 * @author bwcx_jzy
 * @date 2019/8/9
 */
public class TestSftp {
    public static void main(String[] args) throws SftpException {
        Session session = JschUtil.createSession("39.105.190.109", 23, "root", "keepbx&BX123");
        ChannelSftp channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
        Vector<ChannelSftp.LsEntry> vector = channel.ls("/jpom/");
        String pwd = channel.pwd();
        System.out.println(pwd);
//        System.out.println(vector);
        vector.forEach(new Consumer<ChannelSftp.LsEntry>() {
            @Override
            public void accept(ChannelSftp.LsEntry lsEntry) {
                System.out.println(lsEntry.getFilename());

            }
        });
    }
}
