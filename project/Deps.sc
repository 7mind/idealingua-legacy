import $ivy.`io.7mind.izumi.sbt:sbtgen_2.13:0.0.104`
import izumi.sbtgen._
import izumi.sbtgen.model._

object Idealingua {

  object V {
    val izumi = Version.VExpr("Izumi.version")

    val sbtgen = Version.VExpr("V.sbtgen")

    val kind_projector = Version.VExpr("V.kind_projector")
    val scalatest      = Version.VExpr("V.scalatest")

    val cats                 = Version.VExpr("Izumi.Deps.fundamentals_bioJVM.org_typelevel_cats_core_version")
    val cats_effect          = Version.VExpr("Izumi.Deps.fundamentals_bioJVM.org_typelevel_cats_effect_version")
    val circe                = Version.VExpr("Izumi.Deps.fundamentals_json_circeJVM.io_circe_circe_core_version")
    val circe_generic_extras = Version.VExpr("V.circe_generic_extras")
    val circe_derivation     = Version.VExpr("V.circe_derivation")
    val jawn                 = Version.VExpr("Izumi.Deps.fundamentals_json_circeJVM.org_typelevel_jawn_parser_version")
    val zio                  = Version.VExpr("Izumi.Deps.fundamentals_bioJVM.dev_zio_zio_version")
    val zio_interop_cats     = Version.VExpr("Izumi.Deps.fundamentals_bioJVM.dev_zio_zio_interop_cats_version")
    val izumi_reflect        = Version.VExpr("Izumi.Deps.fundamentals_bioJVM.dev_zio_izumi_reflect_version")

    val http4s          = Version.VExpr("V.http4s")
    val http4s_blaze    = Version.VExpr("V.http4s_blaze")
    val scalameta       = Version.VExpr("V.scalameta")
    val fastparse       = Version.VExpr("V.fastparse")
    val scala_xml       = Version.VExpr("V.scala_xml")
    val asynchttpclient = Version.VExpr("V.asynchttpclient")

    val slf4j           = Version.VExpr("V.slf4j")
    val typesafe_config = Version.VExpr("V.typesafe_config")

    val scala_java_time = Version.VExpr("V.scala_java_time")
  }

  object PV {
    val izumi                      = Version.VExpr("PV.izumi")
    val packager                   = Version.VExpr("PV.packager")
    val sbt_mdoc                   = Version.VExpr("PV.sbt_mdoc")
    val sbt_paradox_material_theme = Version.VExpr("PV.sbt_paradox_material_theme")
    val sbt_ghpages                = Version.VExpr("PV.sbt_ghpages")
    val sbt_site                   = Version.VExpr("PV.sbt_site")
    val sbt_unidoc                 = Version.VExpr("PV.sbt_unidoc")
    val sbt_scoverage              = Version.VExpr("PV.sbt_scoverage")
    val sbt_pgp                    = Version.VExpr("PV.sbt_pgp")

    val scala_js_version        = Version.VExpr("PV.scala_js_version")
    val crossproject_version    = Version.VExpr("PV.crossproject_version")
    val scalajs_bundler_version = Version.VExpr("PV.scalajs_bundler_version")
  }

  def entrypoint(args: Seq[String]): Unit = {
    Entrypoint.main(izumi, settings, Seq("-o", ".") ++ args)
  }

  val settings = GlobalSettings(
    groupId        = "io.7mind.izumi",
    sbtVersion     = None,
    scalaJsVersion = Version.VConst("1.17.0"),
  )

  object Deps {
    final val fundamentals_collections = Library("io.7mind.izumi", "fundamentals-collections", V.izumi, LibraryType.Auto)
    final val fundamentals_platform    = Library("io.7mind.izumi", "fundamentals-platform", V.izumi, LibraryType.Auto)
    final val fundamentals_functional  = Library("io.7mind.izumi", "fundamentals-functional", V.izumi, LibraryType.Auto)
    final val fundamentals_bio         = Library("io.7mind.izumi", "fundamentals-bio", V.izumi, LibraryType.Auto)
    final val logstage_core            = Library("io.7mind.izumi", "logstage-core", V.izumi, LibraryType.Auto)
    final val logstage_adapter_slf4j   = Library("io.7mind.izumi", "logstage-adapter-slf4j", V.izumi, LibraryType.Auto)

    final val fundamentals_basics = Seq(
      fundamentals_collections,
      fundamentals_platform,
      fundamentals_functional,
    )
    final val scalatest = Library("org.scalatest", "scalatest", V.scalatest, LibraryType.Auto) in Scope.Test.all

    final val cats_core   = Library("org.typelevel", "cats-core", V.cats, LibraryType.Auto)
    final val cats_effect = Library("org.typelevel", "cats-effect", V.cats_effect, LibraryType.Auto)
    final val cats_all = Seq(
      cats_core,
      cats_effect,
    )

    final val zio_core         = Library("dev.zio", "zio", V.zio, LibraryType.Auto)
    final val zio_interop_cats = Library("dev.zio", "zio-interop-cats", V.zio_interop_cats, LibraryType.Auto)
    final val izumi_reflect    = Library("dev.zio", "izumi-reflect", V.izumi_reflect, LibraryType.Auto)
    final val zio_all = Seq(
      zio_core,
      zio_interop_cats,
      izumi_reflect,
    )

    final val typesafe_config = Library("com.typesafe", "config", V.typesafe_config, LibraryType.Invariant) in Scope.Compile.all

    final val jawn = Library("org.typelevel", "jawn-parser", V.jawn, LibraryType.AutoJvm)

    final val circe_all = Seq(
      Library("io.circe", "circe-parser", V.circe, LibraryType.Auto) in Scope.Compile.all,
      Library("io.circe", "circe-literal", V.circe, LibraryType.Auto) in Scope.Compile.all,
      Library("io.circe", "circe-generic-extras", V.circe_generic_extras, LibraryType.Auto) in Scope.Compile.all.scalaVersion(ScalaVersionScope.AllScala2),
      Library("io.circe", "circe-derivation", V.circe_derivation, LibraryType.Auto) in Scope.Compile.all.scalaVersion(ScalaVersionScope.AllScala2),
      Library("io.circe", "circe-generic", V.circe, LibraryType.Auto) in Scope.Compile.all.scalaVersion(ScalaVersionScope.AllScala3),
    )

    final val scala_sbt = Library("org.scala-sbt", "sbt", Version.VExpr("sbtVersion.value"), LibraryType.Invariant)
    final val scala_reflect = Library("org.scala-lang", "scala-reflect", Version.VExpr("scalaVersion.value"), LibraryType.Invariant) in Scope.Provided.all.scalaVersion(
      ScalaVersionScope.AllScala2
    )
    final val scala_xml = Library("org.scala-lang.modules", "scala-xml", V.scala_xml, LibraryType.Auto) in Scope.Compile.all
    final val scalameta = Library("org.scalameta", "scalameta", V.scalameta, LibraryType.Auto) in Scope.Compile.all

    final val projector = Library("org.typelevel", "kind-projector", V.kind_projector, LibraryType.Invariant)
      .more(LibSetting.Raw("cross CrossVersion.full"))

    final val fastparse = Library("com.lihaoyi", "fastparse", V.fastparse, LibraryType.Auto) in Scope.Compile.all

    final val http4s_client = Seq(
      Library("org.http4s", "http4s-blaze-client", V.http4s_blaze, LibraryType.Auto)
    )

    val http4s_server = Seq(
      Library("org.http4s", "http4s-dsl", V.http4s, LibraryType.Auto),
      Library("org.http4s", "http4s-circe", V.http4s, LibraryType.Auto),
      Library("org.http4s", "http4s-blaze-server", V.http4s_blaze, LibraryType.Auto),
    )

    val http4s_all = http4s_server ++ http4s_client

    val asynchttpclient = Library("org.asynchttpclient", "async-http-client", V.asynchttpclient, LibraryType.Invariant)

    val scala_java_time = Library("io.github.cquiroz", "scala-java-time", V.scala_java_time, LibraryType.Auto)
  }

  import Deps._

  // DON'T REMOVE, these variables are read from CI build (build.sh)
  final val scala212 = ScalaVersion("2.12.20")
  final val scala213 = ScalaVersion("2.13.16")
  final val scala300 = ScalaVersion("3.3.4")

  object Groups {
    final val idealingua = Set(Group("idealingua"))
  }

  object Targets {
    // switch order to use 2.13 in IDEA
//    val targetScala = Seq(scala212, scala213)
    val targetScala2 = Seq(scala213, scala212)
    val targetScala3 = Seq(scala300, scala213, scala212)
    private val jvmPlatform2 = PlatformEnv(
      platform = Platform.Jvm,
      language = targetScala2,
      settings = Seq.empty,
    )
    private val jsPlatform2 = PlatformEnv(
      platform = Platform.Js,
      language = targetScala2,
      settings = Seq(
        "coverageEnabled" := false,
        "scalaJSLinkerConfig" in (SettingScope.Project, Platform.Js) := "{ scalaJSLinkerConfig.value.withModuleKind(ModuleKind.CommonJSModule) }".raw,
      ),
    )
    private val jvmPlatform3 = PlatformEnv(
      platform = Platform.Jvm,
      language = targetScala3,
      settings = Seq.empty,
    )
    private val jsPlatform3 = PlatformEnv(
      platform = Platform.Js,
      language = targetScala3,
      settings = Seq(
        "coverageEnabled" := false,
        "scalaJSLinkerConfig" in (SettingScope.Project, Platform.Js) := "{ scalaJSLinkerConfig.value.withModuleKind(ModuleKind.CommonJSModule) }".raw,
      ),
    )
    final val cross2 = Seq(jvmPlatform2, jsPlatform2)
    final val jvm2   = Seq(jvmPlatform2)

    final val cross3 = Seq(jvmPlatform3, jsPlatform3)
    final val jvm3   = Seq(jvmPlatform3)
  }

  object Projects {

    final val plugins = Plugins(
      Seq(Plugin("IzumiPlugin"))
    )

    implicit class VersionOptionExt(version: Option[Version]) {
      def asString = {

        version match {
          case Some(v: Version.VConst) => v.value
          case Some(v: Version.VExpr)  => v.value
          case _                       => ???
        }
      }
    }

    implicit class VersionExt(version: Version) {
      def asString = {

        version match {
          case v: Version.VConst => v.value
          case v: Version.VExpr  => v.value
        }
      }
    }

    object root {
      final val id = ArtifactId("idealingua-v1")
      final val plugins = Plugins(
        enabled = Seq(Plugin("SbtgenVerificationPlugin"))
      )
      final val settings = Seq(
        "libraryDependencySchemes" in SettingScope.Build += s""""io.circe" %% "circe-core" % VersionScheme.Always""".raw,
        "libraryDependencySchemes" in SettingScope.Build += s""""io.circe" %% "circe-core_sjs1" % VersionScheme.Always""".raw,
      )

      final val sharedAggSettings = Seq(
        "crossScalaVersions" := "Nil".raw
      )

      final val rootSettings = Defaults.SbtMetaRootOptions ++ Defaults.RootOptions ++ Seq(
        "crossScalaVersions" := "Nil".raw,
        "organization" in SettingScope.Build := "io.7mind.izumi",
        "sonatypeProfileName" := "io.7mind",
        "sonatypeSessionName" := """s"[sbt-sonatype] ${name.value} ${version.value} ${java.util.UUID.randomUUID}"""".raw,
        "publishTo" in SettingScope.Build :=
          """
            |(if (!isSnapshot.value) {
            |    sonatypePublishToBundle.value
            |  } else {
            |    Some(Opts.resolver.sonatypeSnapshots)
            |})
            |""".stripMargin.raw,
        "credentials" in SettingScope.Build ++=
          """
            |{
            |val credTarget = Path.userHome / ".sbt" / "secrets" / "credentials.sonatype-nexus.properties"
            |if (credTarget.exists) {
            |  Seq(Credentials(credTarget))
            |} else {
            |  Seq.empty
            |}
            |}""".stripMargin.raw,
        "credentials" in SettingScope.Build ++=
          """
            |{
            |val credTarget = file(".") / ".secrets" / "credentials.sonatype-nexus.properties"
            |if (credTarget.exists) {
            |  Seq(Credentials(credTarget))
            |} else {
            |  Seq.empty
            |}
            |}""".stripMargin.raw,
        "refreshFlakeTask" := """{
          val log = streams.value.log
          val result = "./build.sh nix flake-refresh flake-validate" ! log
          if (result != 0) {
            throw new MessageOnlyException("flake.nix update failed!")
          }
        }""".stripMargin.raw,
        "releaseProcess" := """Seq[ReleaseStep](
                              |  checkSnapshotDependencies,
                              |  inquireVersions,
                              |  runClean,
                              |  runTest,
                              |  setReleaseVersion,
                              |  releaseStepTask(refreshFlakeTask),
                              |  commitReleaseVersion,
                              |  tagRelease,
                              |  //publishArtifacts,
                              |  setNextVersion,
                              |  commitNextVersion,
                              |  pushChanges
                              |)""".stripMargin.raw,
        "homepage" in SettingScope.Build := """Some(url("https://izumi.7mind.io"))""".raw,
        "licenses" in SettingScope.Build := """Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))""".raw,
        "developers" in SettingScope.Build :=
          """List(
          Developer(id = "7mind", name = "Septimal Mind", url = url("https://github.com/7mind"), email = "team@7mind.io"),
        )""".raw,
        "scmInfo" in SettingScope.Build := """Some(ScmInfo(url("https://github.com/7mind/izumi"), "scm:git:https://github.com/7mind/izumi.git"))""".raw,
        "scalacOptions" in SettingScope.Build += s"""s${"\"" * 3}-Xmacro-settings:scalatest-version=$${${V.scalatest.asString}}${"\"" * 3}""".raw,
        "scalacOptions" in SettingScope.Build += s"""${"\"" * 3}-Xmacro-settings:scalajs-version=${Idealingua.settings.scalaJsVersion.asString}${"\"" * 3}""".raw,
        "scalacOptions" in SettingScope.Build += s"""${"\"" * 3}-Xmacro-settings:bundler-version=${Idealingua.settings.bundlerVersion.asString}${"\"" * 3}""".raw,
        "scalacOptions" in SettingScope.Build += s"""${"\"" * 3}-Xmacro-settings:sbt-js-version=${Idealingua.settings.sbtJsDependenciesVersion.asString}${"\"" * 3}""".raw,
        "scalacOptions" in SettingScope.Build += s"""${"\"" * 3}-Xmacro-settings:crossproject-version=${Idealingua.settings.crossProjectVersion.asString}${"\"" * 3}""".raw,
        "scalacOptions" in SettingScope.Build += """s"-Xmacro-settings:is-ci=${insideCI.value}"""".raw,

        // scala-steward workaround
        // add sbtgen version to sbt build to allow scala-steward to find it and update it in .sc files
        // https://github.com/scala-steward-org/scala-steward/issues/696#issuecomment-545800968
        "libraryDependencies" += s""""io.7mind.izumi.sbt" % "sbtgen_2.13" % "${Version.SbtGen.value}" % Provided""".raw,

        // Ignore scala-xml version conflict between scoverage where `coursier` requires scala-xml v2
        // and scoverage requires scala-xml v1 on Scala 2.12,
        // introduced when updating scoverage to 2.0.7 https://github.com/7mind/idealingua-v1/pull/373/
        "libraryDependencySchemes" in SettingScope.Build += """"org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always""".raw,
      )

      final val sharedSettings = Defaults.SbtMetaSharedOptions ++ Defaults.CrossScalaPlusSources ++ Seq(
        "testOptions" in SettingScope.Test += """Tests.Argument("-oDF")""".raw,
        // "testOptions" in (SettingScope.Test, Platform.Jvm) ++= s"""Seq(Tests.Argument("-u"), Tests.Argument(s"$${target.value}/junit-xml-$${scalaVersion.value}"))""".raw,
        "scalacOptions" ++= Seq(
          SettingKey(Some(scala212), None) := Defaults.Scala212Options,
          SettingKey(Some(scala213), None) := Defaults.Scala213Options,
          SettingKey(Some(scala300), None) := Defaults.Scala3Options,
          SettingKey.Default := Const.EmptySeq,
        ),
        "scalacOptions" ++= Seq(
          SettingKey(None, Some(true)) := Seq(
            "-opt:l:inline",
            "-opt-inline-from:izumi.**",
          ),
          SettingKey.Default := Const.EmptySeq,
        ),
        "scalacOptions" -= "-Wconf:any:error",
        "scalacOptions" += "-Wconf:msg=nowarn:silent",
        "scalacOptions" += "-Wconf:msg=pattern var charIn:silent",
      )

    }

    object idealingua {
      final val id       = ArtifactId("idealingua")
      final val basePath = Seq("idealingua-v1")

      final val model                = ArtifactId("idealingua-v1-model")
      final val core                 = ArtifactId("idealingua-v1-core")
      final val runtimeRpcScala      = ArtifactId("idealingua-v1-runtime-rpc-scala")
      final val testDefs             = ArtifactId("idealingua-v1-test-defs")
      final val transpilers          = ArtifactId("idealingua-v1-transpilers")
      final val runtimeRpcHttp4s     = ArtifactId("idealingua-v1-runtime-rpc-http4s")
      final val runtimeRpcTypescript = ArtifactId("idealingua-v1-runtime-rpc-typescript")
      final val runtimeRpcCSharp     = ArtifactId("idealingua-v1-runtime-rpc-csharp")
      final val runtimeRpcGo         = ArtifactId("idealingua-v1-runtime-rpc-go")
      final val compiler             = ArtifactId("idealingua-v1-compiler")
    }

    object docs {
      final val id       = ArtifactId("doc")
      final val basePath = Seq("doc")

      final lazy val microsite = ArtifactId("microsite")
    }

  }

  final val forkTests = Seq(
    "fork" in (SettingScope.Test, Platform.Jvm) := true
  )

  final lazy val idealingua = Aggregate(
    name = Projects.idealingua.id,
    artifacts = Seq(
      Artifact(
        name    = Projects.idealingua.model,
        libs    = Seq(scala_reflect) ++ Deps.fundamentals_basics.map(_ in Scope.Compile.all),
        depends = Seq.empty,
      ),
      Artifact(
        name      = Projects.idealingua.core,
        libs      = Seq(fastparse) ++ Seq(Deps.fundamentals_platform in Scope.Compile.all),
        depends   = Seq(Projects.idealingua.model).map(_ in Scope.Compile.all),
        platforms = Targets.cross2,
      ),
      Artifact(
        name = Projects.idealingua.runtimeRpcScala,
        libs = Seq(
          scala_reflect,
          Deps.fundamentals_bio in Scope.Compile.all,
          Deps.fundamentals_platform in Scope.Compile.all,
          jawn in Scope.Compile.js,
          scala_java_time in Scope.Test.js,
        ) ++
          cats_all.map(_ in Scope.Compile.all) ++
          circe_all ++
          zio_all.map(_ in Scope.Test.all),
        depends = Seq.empty,
      ),
      Artifact(
        name = Projects.idealingua.runtimeRpcHttp4s,
        libs = (http4s_all ++ Seq(asynchttpclient, Deps.logstage_core, Deps.logstage_adapter_slf4j)).map(_ in Scope.Compile.all),
        depends = Seq(Projects.idealingua.runtimeRpcScala).map(_ in Scope.Compile.all) ++
          Seq(Projects.idealingua.testDefs).map(_ in Scope.Test.jvm),
        platforms = Targets.jvm3,
      ),
      Artifact(
        name = Projects.idealingua.transpilers,
        libs = Seq(
          scala_xml,
          scalameta,
          Deps.fundamentals_bio in Scope.Compile.all,
          jawn in Scope.Compile.js,
        ) ++
          circe_all,
        depends = Seq(
          Projects.idealingua.core,
          Projects.idealingua.runtimeRpcScala,
        ).map(_ in Scope.Compile.all) ++
          Seq(Projects.idealingua.testDefs, Projects.idealingua.runtimeRpcTypescript, Projects.idealingua.runtimeRpcGo, Projects.idealingua.runtimeRpcCSharp)
            .map(_ in Scope.Test.jvm),
        settings  = forkTests,
        platforms = Targets.cross2,
      ),
      Artifact(
        name      = Projects.idealingua.testDefs,
        libs      = zio_all,
        depends   = Seq(Projects.idealingua.runtimeRpcScala).map(_ in Scope.Compile.all),
        platforms = Targets.jvm3,
      ),
      Artifact(
        name      = Projects.idealingua.runtimeRpcTypescript,
        libs      = Seq.empty,
        depends   = Seq.empty,
        platforms = Targets.jvm3,
      ),
      Artifact(
        name      = Projects.idealingua.runtimeRpcGo,
        libs      = Seq.empty,
        depends   = Seq.empty,
        platforms = Targets.jvm3,
      ),
      Artifact(
        name      = Projects.idealingua.runtimeRpcCSharp,
        libs      = Seq.empty,
        depends   = Seq.empty,
        platforms = Targets.jvm3,
      ),
      Artifact(
        name = Projects.idealingua.compiler,
        libs = Seq(typesafe_config),
        depends = Seq(
          Projects.idealingua.transpilers,
          Projects.idealingua.runtimeRpcScala,
          Projects.idealingua.runtimeRpcTypescript,
          Projects.idealingua.runtimeRpcGo,
          Projects.idealingua.runtimeRpcCSharp,
          Projects.idealingua.testDefs,
        ).map(_ in Scope.Compile.all),
        platforms = Targets.jvm2,
        settings  = Seq.empty,
        plugins = Plugins(
          Seq(Plugin("JavaAppPackaging"))
        ),
      ),
    ),
    pathPrefix       = Projects.idealingua.basePath,
    groups           = Groups.idealingua,
    defaultPlatforms = Targets.cross3,
  )

  val izumi: Project = Project(
    name = Projects.root.id,
    aggregates = Seq(
      idealingua
    ),
    topLevelSettings  = Projects.root.settings,
    sharedSettings    = Projects.root.sharedSettings,
    sharedAggSettings = Projects.root.sharedAggSettings,
    rootSettings      = Projects.root.rootSettings,
    imports = Seq(
      Import("sbtrelease.ReleaseStateTransformations._"),
      Import("""scala.sys.process._
               |
               |lazy val refreshFlakeTask = taskKey[Unit]("Refresh flake.nix")
               |""".stripMargin),
    ),
    globalLibs = Seq(
      ScopedLibrary(projector, FullDependencyScope(Scope.Compile, Platform.All, ScalaVersionScope.AllScala2), compilerPlugin = true),
      scalatest,
    ),
    rootPlugins   = Projects.root.plugins,
    globalPlugins = Projects.plugins,
    appendPlugins = Defaults.SbtGenPlugins ++ Seq(
      SbtPlugin("com.github.sbt", "sbt-pgp", PV.sbt_pgp),
      SbtPlugin("org.scoverage", "sbt-scoverage", PV.sbt_scoverage),
      SbtPlugin("io.7mind.izumi", "sbt-izumi-deps", PV.izumi),
      SbtPlugin("com.github.sbt", "sbt-native-packager", PV.packager),
    ),
  )
}
