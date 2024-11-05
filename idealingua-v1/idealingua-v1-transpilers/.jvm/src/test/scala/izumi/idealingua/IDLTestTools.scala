package izumi.idealingua

import izumi.fundamentals.platform.build.MacroParameters
import izumi.fundamentals.platform.files.IzFiles
import izumi.idealingua.il.loader.*
import izumi.idealingua.model.loader.LoadedDomain
import izumi.idealingua.translator.*

import java.io.File
import java.nio.file.*
import scala.util.Try

final case class CompilerOutput(targetDir: Path, allFiles: Seq[Path]) {
  def absoluteTargetDir: Path = targetDir.toAbsolutePath

  def phase2: Path = absoluteTargetDir.getParent.resolve("phase2-compiler-output")

  def phase2Relative: Path = absoluteTargetDir.relativize(phase2)

  def relativeOutputs: Seq[String] = allFiles.map(p => absoluteTargetDir.relativize(p.toAbsolutePath).toString)
}

object IDLTestTools {
  def hasDocker: Boolean = IzFiles.haveExecutables("docker")

  def isCI: Boolean = MacroParameters.sbtIsInsideCI().contains(true)

  def loadDefs(): Seq[LoadedDomain.Success] = loadDefs("/defs/any")

  def loadDefs(base: String): Seq[LoadedDomain.Success] = loadDefs(makeLoader(base), makeResolver(base))

  // fixme: workaround for `Error: Could not create the Java Virtual Machine.` (remove if it doesn't reproduce anymore)
  def scalaSysEnv = Map("JAVA_OPTS" -> "")

  def makeLoader(base: String): LocalModelLoaderContext = {
    val src     = new File(getClass.getResource(base).toURI).toPath
    val context = new LocalModelLoaderContext(Seq(src), Seq.empty)
    context
  }

  def makeResolver(base: String): ModelResolver = {
    val last = base.split('/').last
    val rules = Try(TypespaceCompilerBaseFacade.descriptor(IDLLanguage.parse(last)).rules)
      .getOrElse(TypespaceCompilerBaseFacade.descriptors.flatMap(_.rules))
    new ModelResolver(rules)
  }

  def loadDefs(context: LocalModelLoaderContext, resolver: ModelResolver): Seq[LoadedDomain.Success] = {
    val loaded   = context.loader.load()
    val resolved = resolver.resolve(loaded).ifWarnings(w => System.err.println(w)).throwIfFailed()

    val loadable = context.enumerator.enumerate().filter(_._1.name.endsWith(context.domainExt)).keySet
    val good     = resolved.successful.map(_.path).toSet
    val failed   = loadable.diff(good)
    assert(failed.isEmpty, s"domains were not loaded: $failed")

    resolved.successful
  }

}
