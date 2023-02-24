package fpp.compiler.codegen

import fpp.compiler.analysis._
import fpp.compiler.ast._
import fpp.compiler.codegen._

/** Writes out C++ for component commands */
case class ComponentCommands (
  s: CppWriterState,
  aNode: Ast.Annotated[AstNode[Ast.DefComponent]]
) extends ComponentCppWriterUtils(s, aNode) {

  private val cmdParamMap = nonParamCmds.map((opcode, cmd) => {(
    opcode,
    writeFormalParamList(
      cmd.aNode._2.data.params,
      s,
      Nil,
      Some("Fw::CmdStringArg"),
      CppWriterUtils.Value
    )
  )}).toMap

  private val opcodeParam = CppDoc.Function.Param(
    CppDoc.Type("FwOpcodeType"),
    "opCode",
    Some("The opcode")
  )

  private val cmdSeqParam = CppDoc.Function.Param(
    CppDoc.Type("U32"),
    "cmdSeq",
    Some("The command sequence number")
  )

  def getConstantMembers: List[CppDoc.Class.Member] = {
    if !(hasCommands || hasParameters) then Nil
    else List(
      linesClassMember(
        List(
          Line.blank :: lines(s"//! Command opcodes"),
          wrapInEnum(
            lines(
              sortedCmds.map((opcode, cmd) =>
                writeEnumConstant(
                  commandConstantName(cmd),
                  opcode,
                  cmd match {
                    case Command.NonParam(aNode, _) =>
                      AnnotationCppWriter.asStringOpt(aNode)
                    case Command.Param(aNode, kind) =>
                      Some(s"Opcode to ${getCommandParamString(kind)} parameter ${aNode._2.data.name}")
                  },
                  ComponentCppWriterUtils.Hex
                )
              ).mkString("\n")
            )
          )
        ).flatten
      )
    )
  }

  def getPublicFunctionMembers: List[CppDoc.Class.Member] = {
    if !(hasCommands || hasParameters) then Nil
    else
      getRegFunction
  }

  def getProtectedFunctionMembers: List[CppDoc.Class.Member] = {
    if !(hasCommands || hasParameters) then Nil
    else List(
      getResponseFunction,
      getFunctions
    ).flatten
  }

  private def getFunctions: List[CppDoc.Class.Member] = {
    List(
      getHandlers,
      getHandlerBases,
      getPreMsgHooks
    ).flatten
  }

  private def getRegFunction: List[CppDoc.Class.Member] = {
    addAccessTagAndComment(
      "public",
      "Command registration",
      List(
        functionClassMember(
          Some(
            s"""|\\brief Register commands with the Command Dispatcher
                |
                |Connect the dispatcher first
                |"""
          ),
          "regCommands",
          Nil,
          CppDoc.Type("void"),
          Nil
        )
      )
    )
  }

  private def getHandlers: List[CppDoc.Class.Member] = {
    addAccessTagAndComment(
      "PROTECTED",
      "Command handlers to implement",
      nonParamCmds.map((opcode, cmd) =>
        functionClassMember(
          Some(
            addSeparatedString(
              s"Handler for command ${cmd.getName}",
              AnnotationCppWriter.asStringOpt(cmd.aNode)
            )
          ),
          commandHandlerName(cmd.getName),
          List(
            List(
              opcodeParam,
              cmdSeqParam,
            ),
            cmdParamMap(opcode)
          ).flatten,
          CppDoc.Type("void"),
          Nil,
          CppDoc.Function.PureVirtual
        )
      ),
      CppDoc.Lines.Hpp
    )
  }

  private def getHandlerBases: List[CppDoc.Class.Member] = {
    addAccessTagAndComment(
      "PROTECTED",
      """|Command handler base-class functions
         |
         |Call these functions directly to bypass the command input port
         |""",
      nonParamCmds.map((_, cmd) =>
        functionClassMember(
          Some(
            addSeparatedString(
              s"Base-class handler function for command ${cmd.getName}",
              AnnotationCppWriter.asStringOpt(cmd.aNode)
            )
          ),
          commandHandlerBaseName(cmd.getName),
          List(
            opcodeParam,
            cmdSeqParam,
            CppDoc.Function.Param(
              CppDoc.Type("Fw::CmdArgBuffer&"),
              "args",
              Some("The command argument buffer")
            )
          ),
          CppDoc.Type("void"),
          Nil
        )
      )
    )
  }

  private def getResponseFunction: List[CppDoc.Class.Member] = {
    addAccessTagAndComment(
      "PROTECTED",
      "Command response",
      List(
        functionClassMember(
          Some(
            "Emit command response"
          ),
          "cmdResponse_out",
          List(
            opcodeParam,
            cmdSeqParam,
            CppDoc.Function.Param(
              CppDoc.Type("Fw::CmdResponse"),
              "response",
              Some("The command response")
            )
          ),
          CppDoc.Type("void"),
          Nil
        )
      )
    )
  }

  private def getPreMsgHooks: List[CppDoc.Class.Member] = {
    addAccessTagAndComment(
      "PROTECTED",
      """|Pre-message hooks for async commands
         |
         |Each of these functions is invoked just before processing the
         |corresponding command. By default they do nothing. You can
         |override them to provide specific pre-command behavior.
         |""",
      asyncCmds.map((_, cmd) =>
        functionClassMember(
          Some(s"Pre-message hook for command ${cmd.getName}"),
          inputPortHookName(cmd.getName),
          List(
            opcodeParam,
            cmdSeqParam
          ),
          CppDoc.Type("void"),
          Nil,
          CppDoc.Function.Virtual
        )
      )
    )
  }

}
