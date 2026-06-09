pluginManagement {
    repositories {
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
        google()
        mavenCentral()
    }
}

// 预防性声明：明确项目名称和包含模块
rootProject.name = "chat-gusogst"
include(":app")
