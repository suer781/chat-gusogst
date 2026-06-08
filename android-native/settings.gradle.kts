pluginManagement {
    // 预防性声明：明确仓库顺序，避免不确定的行为
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://chaquo.com/maven") }
    }
}
dependencyResolutionManagement {
    // 预防性声明：明确仓库模式，使用 FAIL_ON_PROJECT_REPOS 强制统一管理
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    
    repositories {
        google()
        mavenCentral()
    }
}

// 预防性声明：明确项目名称和包含模块
rootProject.name = "chat-gusogst"
include(":app")
