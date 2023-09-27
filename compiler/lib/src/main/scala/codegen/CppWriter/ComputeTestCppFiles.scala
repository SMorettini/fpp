package fpp.compiler.codegen

import fpp.compiler.analysis._
import fpp.compiler.ast._

case class ComputeTestCppFiles(autoTestSetupMode: CppWriter.AutoTestSetupMode)
  extends ComputeCppFiles
{

  override def defComponentAnnotatedNode(
    s: State,
    aNode: Ast.Annotated[AstNode[Ast.DefComponent]]
  ) = {
    val node = aNode._2
    val data = node.data
    val name = s.getName(Symbol.Component(aNode))
    val loc = Locations.get(node.id)
    for {
      s <- addMappings(s, ComputeCppFiles.FileNames.getComponentGTestBase(name), Some(loc))
      s <- visitList (s, data.members, matchComponentMember)
      s <- addMappings(s, ComputeCppFiles.FileNames.getComponentTesterBase(name), Some(loc))
      s <- autoTestSetupMode match {
        // If auto test setup is on, then the test helpers are part of the autocode
        case CppWriter.AutoTestSetupMode.On =>
          addMappings(s, ComputeCppFiles.FileNames.getComponentTesterBase(name), Some(loc))
        // Otherwise they are part of the implementation
        case CppWriter.AutoTestSetupMode.Off => Right(s)
      }
      s <- visitList (s, data.members, matchComponentMember)
    }
    yield s
  }

  override def defModuleAnnotatedNode(
    s: State,
    aNode: Ast.Annotated[AstNode[Ast.DefModule]]
  ) = visitList(s, aNode._2.data.members, matchModuleMember)

}
