package com.galaxybruce.buildconfig

import com.alibaba.fastjson.JSONObject
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.galaxybruce.buildconfig.utils.Utils

import java.util.function.BiConsumer

class PluginBuildConfig implements Plugin<Project> {
    private Map map = new HashMap()

    /**
     * 文件存放路径
     */
    private File buildConfigFile
    private File buildBizConfigFile
    private File buildConfigResDir
    private File buildConfigAssetsDir
    private File buildConfigLibsDir

    String BUILD_CONFIG = "build_config"
    String PLATFORM_FLAG = "platformFlag"

    /**
     * 应用基础配置信息
     * 读取至config.json
     */
    JSONObject config
    /**
     * 业务配置信息（可选）
     * config_biz.json
     */
    JSONObject configBiz


    @Override
    void apply(Project project) {
        boolean isRootProject = project == project.rootProject
        if (!isRootProject){
            throw new Exception("buildconfig must apply in root project")
        }
        //租户信息
        String platformNum = Utils.getParameterAnyWhere(project, PLATFORM_FLAG)
        parseBuildConfig(project, platformNum)

        setRootProjectExt(project, config)
        project.subprojects(new Action<Project>() {
            @Override
            void execute(Project subProject) {
                subProject.afterEvaluate {
                    handleConfig(subProject, config, configBiz)
                }
            }
        })
    }

    static void printLog(String msg){
        println("[BuildConfig] $msg")
    }

    void parseBuildConfig(Project project, String appFlag){
        //拼接配置文件路径
        printLog("appFlag: $appFlag")
        if (appFlag == null || appFlag.isEmpty() || "null".equalsIgnoreCase(appFlag)) {
            configDirPath = "$BUILD_CONFIG/config/"
        } else{
            configDirPath = "$BUILD_CONFIG/config_$appFlag/"
        }
        File buildConfigDir = new File(new File(project.rootProject.rootDir, BUILD_CONFIG), configDirPath)
        printLog "当前配置文件路径: ${buildConfigDir.absolutePath}"

        buildConfigFile = new File(buildConfigDir,"config.json")
        buildBizConfigFile = new File(buildConfigDir,"config_biz.json")
        buildConfigResDir = new File(buildConfigDir,"res-android/")
        buildConfigAssetsDir = new File(buildConfigDir,"assets-android/")
        buildConfigLibsDir = new File(buildConfigDir,"libs-android/")

        if (buildConfigFile.exists()) {
            printLog "加载配置文件: ${buildConfigFile.getAbsolutePath()}"
            String fileContents = new File(buildConfigFile.getPath()).getText('UTF-8')
            config = JSONObject.parse(fileContents)
        } else {
            throw new Exception("$buildConfigFile.absolutePath 配置文件不存在")
        }

        if (buildBizConfigFile.exists()){
            printLog "加载biz配置文件: ${buildBizConfigFile.getAbsolutePath()}"
            String fileContents = new File(buildBizConfigFile.getPath()).getText('UTF-8')
            configBiz = JSONObject.parse(fileContents)
        }

        printLog "配置文件解析完成..."
    }

    void buildConfig(JSONObject object){
        if (object != null) {
            Utils.buildConfigTypeSpec(map, "", object)
        }
    }

    static void setRootProjectExt(Project project, JSONObject jsonObject) {
        JSONObject baseObject = jsonObject.getJSONObject("build")
        baseObject.forEach(new BiConsumer<String, Object>() {
            @Override
            void accept(String key, Object value) {
                project.rootProject.ext {
                    setProperty(key, value)
                }
            }
        })
    }

    void handleConfig(Project project, JSONObject jsonObject, JSONObject jsonObjectBiz) {
        println("assembleThisModule: " + (project.ext.has('assembleThisModule') && project.ext.assembleThisModule))
        if(project.name != 'app' &&
                // galaxybruce-pioneer插件中设置的变量，表示该module正在独立运行
                !(project.ext.has('assembleThisModule') && project.ext.assembleThisModule)) {
            return
        }

        buildConfig(jsonObject)
        buildConfig(jsonObjectBiz)

        if (map != null) {
            printLog "配置文件解析结果:" + new JSONObject(map).toString()
        }
        project.android.sourceSets.all { sourceSet ->
            //资源替换
            if (!"main".equals(sourceSet.name)) {
                sourceSet.res.srcDirs += buildConfigResDir
                sourceSet.assets.srcDirs += buildConfigAssetsDir
                sourceSet.jniLibs.srcDirs += buildConfigLibsDir
            }
        }
        project.android.applicationVariants.all { variant ->
            //修改应用包名
            def mergedFlavor = variant.mergedFlavor
            String newApplicationId = map.get("build_applicationId")
            if (!Utils.isEmpty(newApplicationId)){
                mergedFlavor.setApplicationId(newApplicationId)
            }

            String newVersionCode = map.get("build_versionCode")
            String newVersionName = map.get("build_versionName")
            //FIX ANDROID GRADLE PLUGIN 3.0 https://stackoverflow.com/questions/46990900/gradle-versionname-and-versioncode-not-working-after-3-0-update
            variant.outputs.all { output ->
                if (!Utils.isEmpty(newVersionName)){
                    output.setVersionNameOverride(newVersionName)
                }
                if (!Utils.isEmpty(newVersionCode)){
                    output.setVersionCodeOverride(Integer.parseInt(newVersionCode))
                }
            }

            mergedFlavor.manifestPlaceholders.put("BUILD_CONFIG", URLEncoder.encode(new JSONObject(map).toString(), "UTF-8"))

            map.forEach(new BiConsumer<String, Object>() {
                @Override
                void accept(String key, Object object) {
                    String value = object.toString();
//                    variant.resValue("string", key, value)
                    try {
                        mergedFlavor.manifestPlaceholders.put(key, value)
                    }
                    catch (Exception e) {
                        throw e
                    }
                }
            })
        }
    }
}
