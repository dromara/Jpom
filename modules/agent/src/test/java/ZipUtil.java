import io.jpom.util.CompressionFileUtil;

import java.io.File;

public class ZipUtil {

	private static final int BUFFER_SIZE = 1024 * 100;

	public static void main(String[] args) throws Exception {
		CompressionFileUtil.unCompress(new File("C:\\Users\\Colorful\\Desktop\\Desktop.tar.bz2"), new File("C:\\Users\\Colorful\\Desktop\\Desktop.tar.gz11\\"));

	}
}
