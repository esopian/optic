package com.opticdev.core.sourcegear
import com.opticdev.core.compiler.{Compiler, CompilerOutput}
import com.opticdev.common.PackageRef
import com.opticdev.core.compiler.errors.{ErrorAccumulator, SomePackagesFailedToCompile}
import com.opticdev.core.sourcegear.project.config.ProjectFile
import com.opticdev.opm.{DependencyTree, OpticPackage, PackageManager}
import com.opticdev.parsers.ParserBase
import com.opticdev.sdk.descriptions.Lens
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.util.{Failure, Try}

object SGConstructor {

  def fromProjectFile(projectFile: ProjectFile) : Future[SGConfig] = Future {
    val dependencies = dependenciesForProjectFile(projectFile).get

    val compiled = compileDependencyTree(dependencies).get

    val schemaSet = dependencies.flattenSchemas.map(_.toColdStorage)
    val gears = compiled.flatMap(_.gears).toSet

    SGConfig(dependencies.hash, Set(), gears, schemaSet)
  }

  def dependenciesForProjectFile(projectFile: ProjectFile) : Try[DependencyTree] = {
    val dependencies = projectFile.dependencies.getOrElse(Vector())
    PackageManager.collectPackages(dependencies)
  }

  def compileDependencyTree(dT: DependencyTree): Try[Seq[CompilerOutput]] = Try {

    implicit val dependencyTree = dT

    val compiledPackages = dependencyTree.flatten.map(p=> {
      val compiler = Compiler.setup(p)
      compiler.execute
    }).toSeq

    if (compiledPackages.forall(_.isSuccess)) {
      compiledPackages
    } else {
      val failedPackages = compiledPackages.filter(_.isFailure).map(i=> (i.opticPackage, i.errors))
        .toMap

      failedPackages.values.flatMap(_.values).foreach(_.printAll)

      throw new SomePackagesFailedToCompile(failedPackages)
    }
  }

}
