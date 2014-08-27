package me.linmingren;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 如果运行程序发现这样的错误信息：
 * java.util.zip.ZipException: invalid LOC header (bad signature),
 * java.util.zip.ZipException: invalid distance too far back 
 * 则很可能是maven仓库中的jar文件有损坏，运行本程序即可找出对应的jar，把他们删掉重新下载即可。
 * @author linmingren
 *
 */
public class JarVerifier {
	private static MessageDigest messageDigest = null;
	private int invalidFileCounts = 0;

	static {
		try {
			messageDigest = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	private static final int BUFFER_SIZE = 4096;

	private String digestData(byte[] data) {
		messageDigest.update(data);
		data = messageDigest.digest();
		int len = data.length;
		StringBuilder buf = new StringBuilder(len * 2);
		for (int j = 0; j < len; j++) {
			buf.append(HEX_DIGITS[(data[j] >> 4) & 0x0f]).append(
					HEX_DIGITS[data[j] & 0x0f]);
		}
		return buf.toString();
	}

	private String getFileString(File file, String charset) {
		InputStreamReader reader = null;
		StringBuilder out = null;
		try {
			reader = new InputStreamReader(new FileInputStream(file), charset);
			out = new StringBuilder();
			char[] buffer = new char[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = reader.read(buffer)) != -1) {
				out.append(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return out.toString();
	}

	private byte[] getFileData(File file) {
		FileInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new FileInputStream(file);
			out = new ByteArrayOutputStream(BUFFER_SIZE);
			int byteCount = 0;
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				byteCount += bytesRead;
			}
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return out.toByteArray();
	}

	/**
	 * 凡是以.sha1的文件结尾的文件,先将文件名去掉.sha1找到文件sourcefile,获取sourcefile文件的真实的sha1和从这个.
	 * sha1文件获取sha1, 进行比照,如果不匹配,输出实际的sha1和期望的sha1
	 * 
	 * @param file
	 */
	private final void verify(File file) {
		String filename = file.getName();
		if (filename.endsWith(".sha1")) {
			String sourcename = filename
					.substring(0, filename.lastIndexOf('.'));
			File sourcefile = new File(file.getParent(), sourcename);
			byte[] sourcedata = getFileData(sourcefile);
			String sha1Real = digestData(sourcedata);
			String content = getFileString(file, "UTF-8");
			String sha1Check = content.split(" ")[0].trim();
			if (!sha1Real.equalsIgnoreCase(sha1Check)) {
				System.out.println("文件 (" + sourcefile.getAbsolutePath() + ") 已经损坏");
				invalidFileCounts ++;
			}
		}
	}

	public void verifyAllFiles(File dir) {
		File[] fs = dir.listFiles();
		for (int i = 0; i < fs.length; i++) {
			if (fs[i].isDirectory()) {
				verifyAllFiles(fs[i]);
			}
			verify(fs[i]);
		}
	}

	
	public int getInvalidFileCounts() {
		return invalidFileCounts;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("请指定maven仓库路径 - 比如C:\\Users\\用户名\\.m2\\repository");
			return;
		}
		
		System.out.println("检查开始， 仓库路径： \"" + args[0] + "\"");
		long start = System.currentTimeMillis();
		JarVerifier verifier = new JarVerifier();
		verifier.verifyAllFiles(new File(args[0]));
		long stop = System.currentTimeMillis();
		System.out.println("检查结束， 共发现 (" + verifier.getInvalidFileCounts() + ")个损坏文件");
		System.out.println("运行时间： " + (stop - start) / 1000 + "秒");
	}

}
