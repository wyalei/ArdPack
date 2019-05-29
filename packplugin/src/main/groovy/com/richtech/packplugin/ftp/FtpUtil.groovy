package com.richtech.packplugin.ftp

import com.richtech.packplugin.project.PackExt
import com.richtech.packplugin.util.LOG
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpVersion
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.params.CoreProtocolPNames
import org.apache.http.util.EntityUtils

/**
 * -------------------------------------
 * author      : wangyalei
 * time        : 2019-05-29
 * description :
 * history     :
 * -------------------------------------
 */
public class FtpUtil {
    static final String TAG = "FtpUtil"

    /**
     * 创建ftp客户端
     *
     * @param isToQA 针对测试同学上传的是/jenkins/software/目录
     * @return
     */
    static createFtpClient(PackExt packExt) {
        FTPClient ftpClient = new FTPClient()
        LOG.info("FtpUtil", packExt.toString())

        if (packExt != null && packExt.ftpAddr && packExt.ftpUser
                && packExt.ftpPwd) {
            LOG.info("FtpUtil", "in custom ftp address")
            // 可以为 apk 上传自定义 ftp 地址，比如市场营销部的一些需求。
            ftpClient.connect(packExt.ftpAddr)
            ftpClient.login(packExt.ftpUser, packExt.ftpPwd)
        } else {
            ftpClient.connect("192.168.6.250")
            // 目录：jenkins/software/，批量包需要admin账户上传
            ftpClient.login("yf_ftp", "lPaYInVgduKrguvS")
        }

        ftpClient.enterLocalPassiveMode()
        ftpClient.setAutodetectUTF8(true)
        /*
            这里，设置传输模式要放在登录之后。
            因为登录前传输模式不起作用，登录后没有设置的话，默认为ASCII模式，中文模式下会有下载后解压失败等问题。
         */
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE)
        return ftpClient
    }

    /**
     * 从FTP服务器下载文件
     *
     * @param ftp  FTP客户端
     * @param remotePath FTP服务器上的相对路径
     * @param fileName 要下载的文件名
     * @param localPath 下载后保存到本地的路径
     * @return
     */
    public static boolean downFile(FTPClient ftp,
                                   String remotePath,
                                   String fileName,
                                   String localPath){

        boolean success = false;
        try {
            ftp.changeWorkingDirectory(remotePath); //转移到FTP服务器目录
            FTPFile[] fs = ftp.listFiles();

            for (FTPFile ff : fs) {
                if (ff.getName().contains("publish_sec_SIGNED.apk")) {  // 母包
                    File localFile = new File(localPath + "/" + ff.getName());
                    OutputStream is = new FileOutputStream(localFile);
                    ftp.retrieveFile(ff.getName(), is);
                    is.close();
                    break;
                }
            }
            ftp.logout();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return success;
    }

    /**
     * 下载母包
     *
     * @param ftp
     * @param dstDir
     * @param ftpDir
     */
    public static File downloadBaseFileFromFtp(FTPClient ftp, String localPath, String ftpDir){
        File localFile = null;

        if (changeWorkingDirectory(ftp, ftpDir, false)) {
            FTPFile[] fs = ftp.listFiles();
            for (FTPFile ff : fs) {
                if (ff.getName().contains("publish_sec_SIGNED.apk")) {  // 母包

                    String fileName = new String(ff.getName().getBytes("iso-8859-1"), "utf-8"); //涉及到中文文件
                    localFile = new File(localPath + "/" + fileName);
                    if (!localFile.exists()) {
                        localFile.getParentFile().mkdirs()
                    }

                    OutputStream fos = new FileOutputStream(localFile)
                    boolean isSuccess = ftp.retrieveFile(fileName, fos)
                    LOG.info isSuccess ? "母包下载成功：\n " + localFile.absolutePath : "母包下载失败！！！"

                    fos.flush()
                    fos.close()

                    break
                }
            }
            ftp.logout();

        } else {
            LOG.info  "切入ftp路径 $ftpDir 失败"
        }
        return localFile;
    }




    /**
     * 文件转成 byte[] <一句话功能简述> <功能详细描述>
     *
     * @param inStream
     * @return
     * @throws IOException
     * @see [类、类#方法、类#成员]
     */
    public static byte[] input2byte(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc = 0;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        byte[] in2b = swapStream.toByteArray();

        swapStream.close();

        return in2b;
    }

    /**
     * 从ftp上下载文件
     *
     * @param ftp
     * @param dstDir
     * @param ftpDir
     */
    public static void downloadDirFilesFromFtp(FTPClient ftp, String dstDir, String ftpDir) {
        if (changeWorkingDirectory(ftp, ftpDir, false)) {
            ftp.listFiles().each {
                File localFile = new File(dstDir + "/" + it.name)
                if (!localFile.exists()) {
                    localFile.getParentFile().mkdirs()
                }
                OutputStream is = new FileOutputStream(localFile)
                ftp.retrieveFile(it.name, is)
                is.close()
            }
        } else {
            LOG.info  "切入ftp路径 $ftpDir 失败"
        }
    }

    /**
     *
     * @param ftp
     * @param ftpDir
     * @param mkDir
     * @return
     */
    static boolean changeWorkingDirectory(FTPClient ftp, String ftpDir, boolean mkDir) {
        String[] ff = ftpDir.split("/")
        boolean changeDirectorySuccess = true
        ff.each {
            if (it != "") {
                boolean success = ftp.changeWorkingDirectory(it)
                if (!success && mkDir) {
                    success = makeDir(ftp, it)
                }
                changeDirectorySuccess = changeDirectorySuccess && success
            }
        }
        return changeDirectorySuccess
    }

    /**
     *  上传文件到FTP
     *
     * @param ftp
     * @param file
     * @param ftpDir
     * @param delLocal
     * @return
     */
    public static boolean uploadFile2Ftp(FTPClient ftp, File file, String ftpDir, boolean delLocal) {
        LOG.info(TAG, "ftpDir:" + ftpDir)
        if(ftpDir == "" || ftpDir == null){
            LOG.info(TAG, "ftpDir is empty, so return")
            return
        }
        changeWorkingDirectory(ftp, ftpDir, true)
//        String fileName = file.name;
        String fileName = new String(file.name.getBytes("UTF-8"), "ISO-8859-1")
        if (file.isFile()) {
            LOG.info  "上传文件：$file.name"
            file.withInputStream { ostream ->
                ftp.storeFile(fileName, ostream)
                if (delLocal) {
                    file.delete()
                }
            }
        } else {
            if (!makeDir(ftp, fileName)) {
                throw new RuntimeException("切入FTP目录失败：$ftpDir");
            } else {
                LOG.info  "切入FTP目录成功：$fileName"
            }
            file.listFiles().each {
                uploadFile2Ftp(ftp, it, "", delLocal)
            }
        }
        LOG.info(TAG, "ftp upload done-----------")
    }

    /**
     * 创建目录
     *
     * @param ftp
     * @param dir
     * @return
     */
    public static boolean  makeDir(FTPClient ftp, String dir) {
        if (dir == "") {
            return true
        }
//        String d = dir;
        if (ftp.changeWorkingDirectory(dir)) {
            return true
        }
        String d = new String(dir.toString().getBytes("GBK"), "iso-8859-1");
        if (ftp.changeWorkingDirectory(d)) {
            return true
        }
        d = new String(dir.getBytes("UTF-8"), "ISO-8859-1")
        if (ftp.changeWorkingDirectory(d)) {
            return true
        }
        if (!ftp.makeDirectory(d)) {
            LOG.info  "[失败]ftp创建目录：" + dir.toString()
            return false;
        }
        return ftp.changeWorkingDirectory(d)
    }

    /**
     * 上传ftp
     */
    public def uploadFtp() {
        new FTPClient().with {
            connect "192.168.6.250"
            enterLocalPassiveMode()
            setAutodetectUTF8(true)
            login "admin", "adminTESTzzw"
            setFileType(FTPClient.BINARY_FILE_TYPE)
            changeWorkingDirectory "/software/jenkins/autopack/"
            File file = new File(extension.batchApkDir.absolutePath)
            file.eachFile {
                LOG.info  '上传FTP:' + it.getName()
//                String fileName = it.getName()
                String fileName = new String(it.getName().getBytes("UTF-8"), "ISO-8859-1")
                it.withInputStream { ostream ->
                    storeFile(fileName, ostream)
                }
                it.delete()

                String name = it.getName().replace('iReader_android_V' + apkVersion + '_', "").replace("_sec_SIGNED.apk", "")
                String channel = name.substring(name.lastIndexOf("_") + 1, name.length())
                marketNameMap.remove(channel)
            }
        }
    }



    /**
     * upLoadByAsyncHttpClient:由HttpClient4上传
     *
     * @return void
     * @throws IOException
     * @throws org.apache.http.client.ClientProtocolException
     * @throws
     * @since CodingExample　Ver 1.1
     */
    public static void upLoadByHttpClient4(String path, String name, String version) throws ClientProtocolException, IOException {

//        StringBuilder sbUrl = new StringBuilder(Constant.Url.UPLOAD_PHP);
//        sbUrl.append("?g=").append("Apk");
//        sbUrl.append("&m=").append("Upload");
//        sbUrl.append("&a=").append("index");
//        sbUrl.append("&name=").append(name); // 客户端名称
//        sbUrl.append("&version=").append(version); // 客户端版本
//        String url = sbUrl.toString();
//
//        LOG.info("同步内测平台 url", url);
//        LOG.info("同步内测平台 path", path);
//
//        URL server = new URL(url);
//
//        DefaultHttpClient httpclient = new DefaultHttpClient();
//        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
//        // HTTP Auth
//        httpclient.getCredentialsProvider().setCredentials(
//                new AuthScope(server.getHost(), server.getPort()), new UsernamePasswordCredentials("zhangyue", "uploadapk"));
//
//
//        HttpPost httppost = new HttpPost(url);
//        File file = new File(path);
//        MultipartEntity entity = new MultipartEntity();
//        FileBody fileBody = new FileBody(file);
//        entity.addPart("uploadfile", fileBody);
//        httppost.setEntity(entity);
//
//
//        HttpResponse response = httpclient.execute(httppost);
//        HttpEntity resEntity = response.getEntity();
//
//        if (resEntity != null) {
//            LOG.info("同步内测平台 response", EntityUtils.toString(resEntity));
//        }
//        if (resEntity != null) {
//            resEntity.consumeContent();
//        }
//
//        httpclient.getConnectionManager().shutdown();
    }
}
