package com.galaxybruce.buildconfig.utils

import com.alibaba.fastjson.JSONObject

class Utils {

    /**
     * 第一个参数可能是settings和project
     * @param project
     * @param key
     * @return
     */
    static String getLocalValue(Object project, String key) {
        Properties localProperties = new Properties()
        def localPropertiesFile = new File(project.rootDir, 'local.properties')
        if (localPropertiesFile.exists()) {
            localPropertiesFile.withReader('UTF-8') { reader ->
                localProperties.load(reader)
            }
        }

        if(localProperties != null) {
            return localProperties.getProperty(key)
        }
        return null
    }

    static String getParameterAnyWhere(Object project, String key) {
        def value = getLocalValue(project, key)
        if(!value) {
            value = System.getenv(key)
        }
        if(!value) {
            value = System.properties[key]
        }
        if(!value) {
            value = project.getProperties().get(key)
        }
        return value
    }

    static boolean isEmpty(String str) {
        return str == null || str.empty
    }

    static void buildConfigTypeSpec(Map map, String parent, JSONObject jsonObject){
        for (String itemKey : jsonObject.keySet()){
            if(jsonObject.get(itemKey) instanceof JSONObject){
                String newParent = isEmpty(parent) ? itemKey : parent + "_" + itemKey
                buildConfigTypeSpec(map, newParent, jsonObject.get(itemKey))
            }
            else{
                buildConfigFieldSpec(map, parent, itemKey, jsonObject.get(itemKey))
            }
        }
    }

    static void buildConfigFieldSpec(Map map, String parent, String key, Object value){
        String placeHolder = (isEmpty(parent) ? key : parent + "_"+ key)
        map.put(placeHolder, value)
    }

}