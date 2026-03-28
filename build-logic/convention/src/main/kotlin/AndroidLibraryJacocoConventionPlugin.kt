import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.shourov.apps.pacedream.configureJacoco
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryJacocoConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("jacoco")
            val androidExtension = extensions.getByType<LibraryExtension>()

            androidExtension.buildTypes.configureEach {
                val isRelease = name == "release"
                enableAndroidTestCoverage = !isRelease
                enableUnitTestCoverage = !isRelease
            }

            configureJacoco(extensions.getByType<LibraryAndroidComponentsExtension>())
        }
    }
}
