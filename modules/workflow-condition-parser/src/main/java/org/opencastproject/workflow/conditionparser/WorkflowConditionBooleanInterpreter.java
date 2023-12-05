/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.workflow.conditionparser;

import org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionBaseVisitor;

import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Interprets the conditional operation parse tree and produces a boolean value
 */
public final class WorkflowConditionBooleanInterpreter extends WorkflowConditionBaseVisitor<Boolean> {

  public Boolean visitBooleanTerm(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.BooleanTermContext ctx) {
    final boolean base = visit(ctx.booleanValue());
    if (ctx.booleanTerm() != null) {
      return base && visit(ctx.booleanTerm());
    }
    return base;
  }

  public Boolean visitRelationOperand(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.RelationOperandContext ctx) {
    return null;
  }

  public Boolean visitBooleanValue(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.BooleanValueContext ctx) {
    final boolean invert = ctx.NOT() != null && ctx.NOT().size() % 2 != 0;
    final boolean result;
    if (ctx.BOOL() != null) {
      result = Boolean.parseBoolean(ctx.BOOL().getText());
    } else if (ctx.booleanExpression() != null) {
      result = visit(ctx.booleanExpression());
    } else {
      result = visitRelation(ctx.relation());
    }
    if (invert) {
      return !result;
    }
    return result;
  }

  public Boolean visitAtom(org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.AtomContext ctx) {
    return null;
  }

  public Boolean visitBooleanExpression(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.BooleanExpressionContext ctx) {
    final boolean base = visit(ctx.booleanTerm());
    if (ctx.booleanExpression() != null) {
      return base || visit(ctx.booleanExpression());
    }
    return base;
  }

  public Boolean visitRelation(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.RelationContext ctx) {
    Atom left = visitRelationOperandProduceAtom(ctx.relationOperand(0));
    Atom right = visitRelationOperandProduceAtom(ctx.relationOperand(1));
    switch (ComparisonOperator.parseComparisonOperator(ctx.COMPARISONOPERATOR().getText())) {
      case LE:
        return left.compareTo(right) <= 0;
      case LT:
        return left.compareTo(right) < 0;
      case EQ:
        return left.compareTo(right) == 0;
      case NE:
        return left.compareTo(right) != 0;
      case GT:
        return left.compareTo(right) > 0;
      default:
        return left.compareTo(right) >= 0;
    }
  }

  private Atom visitRelationOperandProduceAtom(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.RelationOperandContext ctx) {
    Atom base = visitAtomProduceAtom(ctx.atom(0));
    for (int i = 1; i < ctx.atom().size(); i++) {
      TerminalNode operator = ctx.NUMERICALOPERATOR(i - 1);
      base = base.reduce(visitAtomProduceAtom(ctx.atom(i)), NumericalOperator.parseNumericalOperator(operator.getText()));
    }
    return base;
  }

  private Atom visitAtomProduceAtom(
          org.opencastproject.workflow.conditionparser.antlr.WorkflowConditionParser.AtomContext ctx) {
    if (ctx.NUMBER() != null) {
      return Atom.parseNumber(ctx.NUMBER().getText());
    }
    if (ctx.STRING() != null) {
      final String replaced = ctx.STRING().getText().replace("''", "'");
      return Atom.parseString(replaced.substring(1, replaced.length() - 1));
    }
    if (ctx.BOOL() != null) {
      final String replaced = ctx.BOOL().getText();
      return Atom.fromString(replaced);
    }
    return visitRelationOperandProduceAtom(ctx.relationOperand());
  }
}
