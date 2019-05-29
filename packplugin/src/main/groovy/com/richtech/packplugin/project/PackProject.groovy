package com.richtech.packplugin.project

import com.richtech.packplugin.task.PackApkTask
import com.richtech.packplugin.util.LOG
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * -------------------------------------
 * author      : wangyalei
 * time        : 2019-05-28
 * description :
 * history     :
 * -------------------------------------
 */
class PackProject implements Plugin<Project>{
    Project mProject
    PackExt mPackExt

    @Override
    void apply(Project project) {
        mProject = project
        applyPackTask()
    }

    void applyPackTask(){
        mPackExt = mProject.extensions.create("PackExt", PackExt.class)

        mProject.afterEvaluate {
            mProject.android.applicationVariants.all{variant ->
                def variantName = variant.name.capitalize()
                def taskName = mProject.gradle.startParameter.taskNames.join(',')
                LOG.log(taskName)

                PackApkTask packApkTask = mProject.tasks.create("packApk${variantName}", PackApkTask)
                packApkTask.mBuildType = variant.buildType.name
                packApkTask.mInputFile = variant.outputs[0].outputFile
                packApkTask.mPackExt = mPackExt
                packApkTask.dependsOn variant.assemble
            }
        }
    }


}
