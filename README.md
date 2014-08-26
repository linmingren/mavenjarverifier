mavenjarverifier
================
此工具用于找出maven仓库中损坏的文件，jar或者其他文件都可以。
如果运行程序发现这样的错误信息：
 * java.util.zip.ZipException: invalid LOC header (bad signature),
 * java.util.zip.ZipException: invalid distance too far back 
 * 则很可能是maven仓库中的jar文件有损坏，运行本程序即可找出对应的jar，把他们删掉重新下载即可。
