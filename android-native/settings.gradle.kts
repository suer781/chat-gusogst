pluginManagement {
    // 预防性声明：明确仓库顺序，避免不确定的行为
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
}

dependencyResolutionManagement {
    // 预防性声明：使用 PREFER_SETTINGS 模式，允许项目仓库作为后备
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    
    repositories {
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        google()
        mavenCentral()
    }
}

// 预防性声明：明确项目名称和包含模块
rootProject.name = "chat-gusogst"
include(":app")
