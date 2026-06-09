pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        exclusiveContent {
            forRepository {
                maven { url = uri("https://chaquo.com/maven") }
            }
            filter {
                includeGroup("com.chaquo.python")
                includeGroup("com.chaquo.python.runtime")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven { url = uri("https://chaquo.com/maven") }
            }
            filter {
                includeGroup("com.chaquo.python")
                includeGroup("com.chaquo.python.runtime")
            }
        }
    }
}

rootProject.name = "chat-gusogst"
include(":app")
