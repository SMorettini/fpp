package fpp.compiler.codegen

import fpp.compiler.analysis._
import fpp.compiler.ast._
import fpp.compiler.util._

/** Write out F Prime XML */
object XmlWriter extends AstStateVisitor with LineUtils {

  type State = XmlWriterState

  override def defArrayAnnotatedNode(s: XmlWriterState, aNode: Ast.Annotated[AstNode[Ast.DefArray]]) = {
    val (_, node, _) = aNode
    val data = node.data
    val fileName = ComputeXmlFiles.getArrayFileName(data)
    val lines = ArrayXmlWriter.defArrayAnnotatedNode(s, aNode)
    writeXmlFile(s, fileName, lines)
  }

  override def defEnumAnnotatedNode(s: XmlWriterState, aNode: Ast.Annotated[AstNode[Ast.DefEnum]]) = {
    val (_, node, _) = aNode
    val data = node.data
    val fileName = ComputeXmlFiles.getEnumFileName(data)
    val lines = EnumXmlWriter.defEnumAnnotatedNode(s, aNode)
    writeXmlFile(s, fileName, lines)
  }

  override def defModuleAnnotatedNode(
    s: XmlWriterState,
    aNode: Ast.Annotated[AstNode[Ast.DefModule]]
  ) = {
    val (_, node, _) = aNode
    val data = node.data
    val a = s.a.copy(scopeNameList = data.name :: s.a.scopeNameList)
    val s1 = s.copy(a = a)
    visitList(s1, data.members, matchModuleMember)
    Right(s)
  }

  override def defPortAnnotatedNode(s: XmlWriterState, aNode: Ast.Annotated[AstNode[Ast.DefPort]]) = {
    val (_, node, _) = aNode
    val data = node.data
    val fileName = ComputeXmlFiles.getPortFileName(data)
    val lines = PortXmlWriter.defPortAnnotatedNode(s, aNode)
    writeXmlFile(s, fileName, lines)
  }

  override def defStructAnnotatedNode(s: XmlWriterState, aNode: Ast.Annotated[AstNode[Ast.DefStruct]]) = {
    val (_, node, _) = aNode
    val loc = Locations.get(node.id)
    val data = node.data
    val fileName = ComputeXmlFiles.getStructFileName(data)
    val lines = StructXmlWriter.defStructAnnotatedNode(s, aNode)
    for {
      _ <- if (data.members.length == 0) Left(CodeGenError.EmptyStruct(loc)) else Right(())
      s <- writeXmlFile(s, fileName, lines)
    } yield s
  }

  override def transUnit(s: XmlWriterState, tu: Ast.TransUnit) = 
    visitList(s, tu.members, matchTuMember)

  private def writeXmlHeader(fileName: String) = lines(
    s"""|<?xml version=${XmlTags.quoted("1.0")} encoding=${XmlTags.quoted("UTF-8")}?>
        |
        |<!-- =====================================================================
        |$fileName
        |Generated by fpp-to-xml
        |====================================================================== -->
    """
  )

  private def writeXmlFile(s: XmlWriterState, fileName: String, lines: List[Line]) = {
    val path = java.nio.file.Paths.get(s.dir, fileName)
    val file = File.Path(path)
    val headerLines = writeXmlHeader(fileName)
    for (writer <- file.openWrite()) yield { 
      (headerLines ++ lines).map(Line.write(writer) _)
      writer.close()
      s
    }
  }

}
