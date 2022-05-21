@file:Suppress("LeakingThis") // Following official Gradle guidance

package com.varabyte.kobweb.gradle.application.extensions

import com.varabyte.kobweb.common.navigation.RoutePrefix
import com.varabyte.kobweb.gradle.application.GENERATED_ROOT
import com.varabyte.kobweb.gradle.application.kmp.jsTarget
import com.varabyte.kobweb.gradle.application.kmp.jvmTarget
import com.varabyte.kobweb.project.conf.KobwebConf
import kotlinx.html.HEAD
import kotlinx.html.link
import kotlinx.html.meta
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.get
import java.io.File
import javax.inject.Inject

/**
 * A gradle block used for initializing values that configure a Kobweb project.
 *
 * This class also exposes a handful of methods useful for querying the project.
 */
abstract class KobwebBlock @Inject constructor(conf: KobwebConf) {
    /**
     * A sub-block for defining properties related to the "index.html" document generated by Kobweb
     */
    abstract class IndexDocument @Inject constructor(val routePrefix: RoutePrefix) {
        /**
         * A list of head element builders to add to the generated index.html file.
         *
         * The reason this is exposed as a list instead of a property is so that different plugins can add their own
         * values (usually scripts or stylesheets) independently of one another.
         */
        abstract val head: ListProperty<HEAD.() -> Unit>

        /**
         * The default description to set in the meta tag.
         */
        abstract val description: Property<String>

        init {
            description.convention("Powered by Kobweb")

            head.set(listOf {
                meta {
                    name = "description"
                    content = description.get()
                }
                link {
                    rel = "icon"
                    href = routePrefix.prepend("/favicon.ico")
                }

                // Viewport content chosen for a good mobile experience.
                // See also: https://developer.mozilla.org/en-US/docs/Web/HTML/Viewport_meta_tag#viewport_basics
                meta("viewport", "width=device-width, initial-scale=1")
            })
        }
    }

    /**
     * The string path to the root where generated code will be written to, relative to the project root.
     */
    abstract val genDir: Property<String>

    /**
     * The root package of all pages.
     *
     * Any composable function not under this root will be ignored, even if annotated by @Page.
     *
     * An initial '.' means this should be prefixed by the project group, e.g. ".pages" -> "com.example.pages"
     */
    abstract val pagesPackage: Property<String>

    /**
     * The root package of all apis.
     *
     * Any function not under this root will be ignored, even if annotated by @Api.
     *
     * An initial '.' means this should be prefixed by the project group, e.g. ".api" -> "com.example.api"
     */
    abstract val apiPackage: Property<String>

    /**
     * The path of public resources inside the project's resources folder, e.g. "public" ->
     * "src/jsMain/resources/public"
     */
    abstract val publicPath: Property<String>

    abstract val appGlobals: MapProperty<String, String>

    init {
        genDir.convention(GENERATED_ROOT)
        pagesPackage.convention(".pages")
        apiPackage.convention(".api")
        publicPath.convention("public")
        appGlobals.convention(mapOf())

        (this as ExtensionAware).extensions.create("index", IndexDocument::class.java, RoutePrefix(conf.site.routePrefix))
    }

    fun getGenJsSrcRoot(project: Project): File {
        val jsSrcSuffix = project.jsTarget.srcSuffix
        return project.layout.buildDirectory.dir("${genDir.get()}$jsSrcSuffix").get().asFile
    }

    fun getGenJsResRoot(project: Project): File {
        val jsResourceSuffix = project.jsTarget.resourceSuffix
        return project.layout.buildDirectory.dir("${genDir.get()}$jsResourceSuffix").get().asFile
    }

    fun getGenJvmSrcRoot(project: Project): File {
        val jvmSrcSuffix = (project.jvmTarget ?: error("No JVM target defined")).srcSuffix
        return project.layout.buildDirectory.dir("${genDir.get()}$jvmSrcSuffix").get().asFile
    }
}

val KobwebBlock.index: KobwebBlock.IndexDocument
    get() = ((this as ExtensionAware).extensions["index"] as KobwebBlock.IndexDocument)