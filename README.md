# 用途
读取配置文件构造不同的app
## 配置文件
1. build_config/config.json
```
{
  "build": {
    "appName": "我是谁",
    "applicationId": "com.galaxybruce.test",
    "versionName": "1.0.0",
    "versionCode": 1
  }
}
```
config.json主要是配置app必须项。build节点中的参数都会编译到rootProject.ext中
```
rootProject.ext.build_appName
rootProject.ext.build_applicationId
rootProject.ext.build_versionName
rootProject.ext.build_versionCode
```
2. build_config/config_biz.json
该配置文件是一些业务上配置，可以是任何差异化的配置。

## 配置文件按照节点的方式以下划线隔开平铺，并且都放在manifestPlaceholders中。
```
BUILD_CONFIG=URLEncoder.encode(new JSONObject(map).toString(), "UTF-8")
build_appName=我是谁
build_applicationId=com.galaxybruce.test
build_versionName=1.0.0
build_versionCode=1
```
代码中通过从BUILD_CONFIG中读取任何想要的配置。
