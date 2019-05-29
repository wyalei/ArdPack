package com.richtech.packplugin.task

import com.android.utils.FileUtils
import com.richtech.packplugin.ftp.FtpUtil
import com.richtech.packplugin.project.PackExt
import com.richtech.packplugin.util.LOG
import org.apache.commons.net.ftp.FTPClient
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat;

/**
 * -------------------------------------
 * author      : wangyalei
 * time        : 2019-05-29
 * description :
 * history     :
 * -------------------------------------
 */
public class PackApkTask extends DefaultTask{

    public File mInputFile

    String mVersionName
    String mBuildType
    PackExt mPackExt

    PackApkTask() {
        group "pack"
    }

    @TaskAction
    void packing(){
        LOG.log("PackApkTask packing -------------")
        project.android.applicationVariants.all{ variant->
            mVersionName = variant.versionName
        }
        LOG.info("packing mVersionName: " + mVersionName + " type: " + mBuildType)

        def dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        String date = dateFormat.format(new Date())
        date = date.replaceAll(":", "-")
                .replaceAll("\\.", "-")
                .replaceAll(" ", "_")


        def apkName = "ard_" + mVersionName + "_" + date + "_" + mBuildType + ".apk"
        def path = mInputFile.absolutePath
        def apkDir = path.substring(0, path.lastIndexOf("/"))

        def apkPath = apkDir + File.separator + apkName
        File targetApkFile = new File(apkPath)

        LOG.info("packing date: " + date)
        LOG.info("packing path: " + path)
        LOG.info("packing apkPath: " + apkPath)
        FileUtils.copyFile(mInputFile, targetApkFile)
//        FTPClient client = FtpUtil.createFtpClient(mPackExt)
//        FtpUtil.uploadFile2Ftp(client, targetApkFile, mPackExt.ftpDir, false)
    }
}
