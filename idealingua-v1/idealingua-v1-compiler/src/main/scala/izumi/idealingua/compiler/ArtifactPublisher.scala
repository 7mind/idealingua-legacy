package izumi.idealingua.compiler

import izumi.fundamentals.platform.files.IzFiles
import izumi.idealingua.model.publishing.BuildManifest
import izumi.idealingua.model.publishing.manifests.{GoLangBuildManifest, ProtobufBuildManifest}
import izumi.idealingua.translator.IDLLanguage

import java.nio.file.*
import java.time.ZonedDateTime
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.*
import scala.sys.process.*
import scala.util.Try

@nowarn("msg=lazyLines")
class ArtifactPublisher(targetDir: Path, lang: IDLLanguage, creds: Credentials, manifest: BuildManifest) {
  private val log: CompilerLog = CompilerLog.Default

  def publish(): Either[Throwable, Unit] = ((creds, lang, manifest): @unchecked) match {
    case (c: ScalaCredentials, IDLLanguage.Scala, _)                              => publishScala(targetDir, c)
    case (c: TypescriptCredentials, IDLLanguage.Typescript, _)                    => publishTypescript(targetDir, c)
    case (c: GoCredentials, IDLLanguage.Go, m: GoLangBuildManifest)               => publishGo(targetDir, c, m)
    case (c: CsharpCredentials, IDLLanguage.CSharp, _)                            => publishCsharp(targetDir, c)
    case (c: ProtobufCredentials, IDLLanguage.Protobuf, m: ProtobufBuildManifest) => publishProtobuf(targetDir, c, m)
    case (c, l, _) if c.lang != l =>
      Left(
        new IllegalArgumentException(
          "Language and credentials type didn't match. " +
          s"Got credentials for $l, expect for ${c.lang}"
        )
      )
  }

  private def publishScala(targetDir: Path, creds: ScalaCredentials): Either[Throwable, Unit] = Try {
    log.log("Prepare to package Scala sources")
    Process(
      "sbt clean package",
      targetDir.toFile,
    ).lineStream.foreach(log.log)

    log.log("Writing credentials file to ")
    val buildFile    = targetDir.toAbsolutePath.resolve("build.sbt")
    val sbtCredsFile = targetDir.toAbsolutePath.resolve(".credentials")

    val credsLines = Seq(
      "\n",
      // TODO: Gigahorse apears to be cause of `Too many follow-up requests: 21` exception during publishing
      "ThisBuild / updateOptions := updateOptions.value.withGigahorse(false)",
      "\n",
      s"""credentials += Credentials(Path("${sbtCredsFile.toAbsolutePath.toString}").asFile)""",
      "\n",
      s"""
         |ThisBuild / publishTo := {
         |  if (isSnapshot.value)
         |    Some("snapshots" at "${creds.sbtSnapshotsRepo}")
         |  else
         |    Some("releases"  at "${creds.sbtReleasesRepo}")
         |}
      """.stripMargin,
    )

    Files.write(buildFile, credsLines.asJava, StandardOpenOption.WRITE, StandardOpenOption.APPEND)

    Files.write(
      sbtCredsFile,
      Seq[String](
        s"realm=${creds.sbtRealm}",
        s"host=${creds.sbtHost}",
        s"user=${creds.sbtUser}",
        s"password=${creds.sbtPassword}",
      ).asJava,
    )

    Process(
      "sbt publish",
      targetDir.toFile,
    ).lineStream.foreach(log.log)
  }.toEither

  private def publishTypescript(targetDir: Path, creds: TypescriptCredentials): Either[Throwable, Unit] = Try {
    log.log("Prepare to package Typescript sources")

    val packagesDir = Files.list(targetDir.resolve("packages")).filter(_.toFile.isDirectory).iterator().asScala.toSeq.head
    val credsFile   = Paths.get(System.getProperty("user.home")).resolve("~/.npmrc")
    val repoName    = creds.npmRepo.replaceAll("http://", "").replaceAll("https://", "")
    val scope       = packagesDir.getFileName
    val processDir  = targetDir.toFile

    log.log(s"Writing credentials in ${credsFile.toAbsolutePath.getFileName}")
    val scriptLines = List(
      Seq("echo", s"Setting NPM registry for scope $scope to $repoName using user & _password method..."),
      Seq("npm", "config", "set", s"$scope:registry", s"${creds.npmRepo}"),
      Seq("npm", "config", "set", s"//$repoName:email", s"${creds.npmEmail}"),
      Seq("npm", "config", "set", s"//$repoName:username", s"${creds.npmUser}"),
      Seq("npm", "config", "set", s"//$repoName:_password", (Seq("echo", "-n", s"${creds.npmPassword}") #| Seq("openssl", "base64")).!!),
    )

    scriptLines.foreach(s => Process(s, processDir).lineStream.foreach(log.log))

    log.log("Publishing NPM packages")

    log.log("Yarn installing")
    Process("yarn install", processDir).lineStream.foreach(log.log)

    log.log("Yarn building ES5")
    Process("yarn build", processDir).lineStream.foreach(log.log)

    log.log("Yarn building ESNext")
    Process("yarn build-es", processDir).lineStream.foreach(log.log)

    def publishToNpm(dir: Path, packageFileName: String): Unit = {
      Files.list(dir).filter(_.toFile.isDirectory).iterator().asScala.foreach {
        module =>
          val cmd = s"npm publish --force --registry ${creds.npmRepo} ${module.toAbsolutePath.toString}"
          log.log(s"Publish ${module.getFileName}. Cmd: `$cmd`")
          Files.copy(packagesDir.resolve(s"${module.getFileName}/$packageFileName"), module.resolve("package.json"))
          Process(cmd, processDir).lineStream.foreach(log.log)
      }
    }

    publishToNpm(targetDir.resolve("dist"), "package.json")
    publishToNpm(targetDir.resolve("dist-es"), "package.es.json")
  }.toEither

  private def publishCsharp(targetDir: Path, creds: CsharpCredentials): Either[Throwable, Unit] = Try {
//    val nuspecDir  = targetDir.resolve("nuspec")
//    val nuspecDirAsFile = nuspecDir.toFile

    val targetDirAsFile = targetDir.toFile

    log.log("Publishing C#...")

    log.log("Preparing credentials")
    Process(
      s"dotnet nuget add source ${creds.nugetRepo} --name IzumiPublishSource --username ${creds.nugetUser} --password ${creds.nugetPassword}",
      targetDirAsFile,
    ).#||("true").lineStream.foreach(log.log)

//    Process(
//      s"nuget setapikey ${creds.nugetUser}:${creds.nugetPassword} -Source IzumiPublishSource",
//      nuspecDirAsFile,
//    ).lineStream.foreach(log.log)
//
//    log.log("Publishing")
//    Files.list(nuspecDir).filter(_.getFileName.toString.endsWith(".nuspec")).iterator().asScala.foreach {
//      module =>
//        Try(
//          Process(
//            s"nuget pack ${module.getFileName.toString}",
//            nuspecDirAsFile,
//          ).lineStream.foreach(log.log)
//        )
//    }

    Process(
      s"dotnet build -c Release",
      targetDirAsFile,
    ).lineStream.foreach(log.log)

    IzFiles.walk(targetDirAsFile).filter(_.getFileName.toString.endsWith(".nupkg")).foreach {
      pack =>
        Process(
          s"dotnet nuget push ${pack.getFileName.toString} -s ${creds.nugetRepo}",
          targetDirAsFile,
        ).lineStream.foreach(log.log)
    }
  }.toEither

  private def publishGo(targetDir: Path, creds: GoCredentials, manifest: GoLangBuildManifest): Either[Throwable, Unit] = Try {
    log.log("Prepare to package GoLang sources")

    val env = Seq(
      "GOPATH"      -> s"${targetDir.toAbsolutePath.toString}",
      "GO111MODULE" -> "off",
    )
    IzFiles.recreateDir(targetDir.resolve("src"))

    Files.move(targetDir.resolve("github.com"), targetDir.resolve("src/github.com"), StandardCopyOption.REPLACE_EXISTING)

    Process(
      "go get github.com/gorilla/websocket",
      targetDir.toFile,
      env*
    ).lineStream.foreach(log.log)

    if (manifest.enableTesting) {
      log.log("Testing")
      Process(
        "go test ./...",
        targetDir.resolve("src").toFile,
        env*
      ).lineStream.foreach(log.log)
      log.log("Testing - OK")
    } else {
      log.log("Testing is disabled. Skipping.")
    }

    log.log("Publishing to github repo")

    log.log("Seting up Git")
    val pubKey = targetDir.resolve("go-key.pub")
    Files.write(pubKey, Seq(creds.gitPubKey).asJava)

    Process(Seq("git", "config", "--global", "--replace-all", "user.name", creds.gitUser)).lineStream.foreach(log.log)
    Process(Seq("git", "config", "--global", "--replace-all", "user.email", creds.gitEmail)).lineStream.foreach(println)
    Process(
      Seq(
        "git",
        "config",
        "--global",
        "--replace-all",
        "core.sshCommand",
        s"ssh -i ${pubKey.toAbsolutePath.toString} -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no",
      )
    ).lineStream.foreach(println)

    Process(
      "git config --global --list",
      targetDir.toFile,
    ).lineStream.foreach(log.log)

    Process(
      s"git clone ${creds.gitRepoUrl}",
      targetDir.toFile,
    ).lineStream.foreach(log.log)

    Files
      .list(targetDir.resolve(creds.gitRepoName)).iterator().asScala
      .filter(_.getFileName.toString.charAt(0) != '.')
      .foreach {
        path =>
          IzFiles.erase(path)
      }

    Files.list(targetDir.resolve("src").resolve(manifest.repository.repository)).iterator().asScala.foreach {
      srcDir =>
        Files.move(srcDir, targetDir.resolve(creds.gitRepoName).resolve(srcDir.getFileName.toString))
    }

    Files.write(
      targetDir.resolve(creds.gitRepoName).resolve(".timestamp"),
      Seq(
        ZonedDateTime.now().toString
      ).asJava,
    )
    Files.write(
      targetDir.resolve(creds.gitRepoName).resolve("README.md"),
      Seq(
        s"# ${creds.gitRepoName}",
        s"Auto-generated golang apis, ${manifest.common.version.toString}",
      ).asJava,
    )

    Process(
      "git add .",
      targetDir.resolve(creds.gitRepoName).toFile,
    ).lineStream.foreach(log.log)

    log.log(s"Git commit: 'golang-api-update,version=${manifest.common.version}'")
    Process(
      s"""git commit --no-edit -am 'golang-api-update,version=${manifest.common.version}'""",
      targetDir.resolve(creds.gitRepoName).toFile,
    ).lineStream.foreach(log.log)

    log.log(s"Setting git tag: v${manifest.common.version.toString}")
    Process(
      s"git tag -f v${manifest.common.version.toString}",
      targetDir.resolve(creds.gitRepoName).toFile,
    ).lineStream.foreach(log.log)

    log.log("Git push")
    Process(
      "git push --all -f",
      targetDir.resolve(creds.gitRepoName).toFile,
    ).lineStream.foreach(log.log)
    Process(
      "git push --tags -f",
      targetDir.resolve(creds.gitRepoName).toFile,
    ).lineStream.foreach(log.log)
  }.toEither

  private def publishProtobuf(targetDir: Path, creds: ProtobufCredentials, manifest: ProtobufBuildManifest): Either[Throwable, Unit] = Try {
    log.log("Publishing Protobuf sources to github repo")
    // copy all files to tmp
    val sources    = Files.list(targetDir).iterator().asScala.toList
    val sourcesDir = targetDir.resolve("src_tmp")
    val repoDir    = targetDir.resolve(creds.gitRepoName)

    IzFiles.recreateDir(sourcesDir)
    sources.foreach(src => Files.move(src, sourcesDir.resolve(src.getFileName)))

    log.log("Seting up Git")
    val pubKey = targetDir.resolve("protobuf-key.pub")
    Files.write(pubKey, Seq(creds.gitPubKey).asJava)

    Process(Seq("git", "config", "--global", "--replace-all", "user.name", creds.gitUser)).lineStream.foreach(log.log)
    Process(Seq("git", "config", "--global", "--replace-all", "user.email", creds.gitEmail)).lineStream.foreach(println)
    Process(
      Seq(
        "git",
        "config",
        "--global",
        "--replace-all",
        "core.sshCommand",
        s"ssh -i ${pubKey.toAbsolutePath.toString} -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no",
      )
    ).lineStream.foreach(println)

    Process(
      "git config --global --list",
      targetDir.toFile,
    ).lineStream.foreach(log.log)

    // clone repo
    Process(
      s"git clone ${creds.gitRepoUrl}",
      targetDir.toFile,
    ).lineStream.foreach(log.log)

    // cleanup repo
    Files
      .list(repoDir).iterator().asScala
      .filter(_.getFileName.toString.charAt(0) != '.')
      .foreach(path => IzFiles.erase(path))

    // move everything from tmp to repo
    Files.list(sourcesDir).iterator().asScala.foreach {
      srcDir =>
        Files.move(srcDir, repoDir.resolve(srcDir.getFileName.toString))
    }

    // add def files
    Files.write(
      repoDir.resolve(".timestamp"),
      Seq(
        ZonedDateTime.now().toString
      ).asJava,
    )
    Files.write(
      repoDir.resolve("README.md"),
      Seq(
        s"# ${creds.gitRepoName}",
        s"Auto-generated protobuf sources, ${manifest.common.version.toString}",
      ).asJava,
    )

    // commit repo
    Process(
      "git add .",
      repoDir.toFile,
    ).lineStream.foreach(log.log)

    log.log(s"Git commit: 'protobuf-sources-update,version=${manifest.common.version}'")
    Process(
      s"""git commit --no-edit -am 'protobuf-sources-update,version=${manifest.common.version}'""",
      repoDir.toFile,
    ).lineStream.foreach(log.log)

    log.log(s"Setting git tag: v${manifest.common.version.toString}")
    Process(
      s"git tag -f v${manifest.common.version.toString}",
      repoDir.toFile,
    ).lineStream.foreach(log.log)

    log.log("Git push")
    Process(
      "git push --all -f",
      repoDir.toFile,
    ).lineStream.foreach(log.log)
    Process(
      "git push --tags -f",
      repoDir.toFile,
    ).lineStream.foreach(log.log)
  }.toEither
}
