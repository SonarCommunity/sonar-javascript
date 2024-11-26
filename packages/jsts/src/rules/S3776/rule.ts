/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
// https://sonarsource.github.io/rspec/#/rspec/S3776

import type { TSESTree } from '@typescript-eslint/utils';
import {
  generateMeta,
  getFirstToken,
  getFirstTokenAfter,
  getJsxShortCircuitNodes,
  getMainFunctionTokenLocation,
  isArrowFunctionExpression,
  isIfStatement,
  isLogicalExpression,
  IssueLocation,
  report,
  RuleContext,
  toSecondaryLocation,
} from '../helpers/index.js';
import type { Rule } from 'eslint';
import estree from 'estree';
import { meta, schema } from './meta.js';
import { FromSchema } from 'json-schema-to-ts';

const DEFAULT_THRESHOLD = 15;

type LoopStatement =
  | TSESTree.ForStatement
  | TSESTree.ForInStatement
  | TSESTree.ForOfStatement
  | TSESTree.DoWhileStatement
  | TSESTree.WhileStatement;

type OptionalLocation = TSESTree.SourceLocation | null | undefined;

const message =
  'Refactor this function to reduce its Cognitive Complexity from {{complexityAmount}} to the {{threshold}} allowed.';

export const rule: Rule.RuleModule = {
  meta: generateMeta(
    meta as Rule.RuleMetaData,
    {
      messages: {
        refactorFunction: message,
        fileComplexity: '{{complexityAmount}}',
      },
      schema,
    },
    true,
  ),
  create(context) {
    /** Complexity threshold */
    const threshold = (context.options as FromSchema<typeof schema>)[0] ?? DEFAULT_THRESHOLD;

    /** Indicator if the file complexity should be reported */
    const isFileComplexity = context.options.includes('metric');

    /** Complexity of the file */
    let fileComplexity = 0;

    /** Complexity of the current function if it is *not* considered nested to the first level function */
    let complexityIfNotNested: ComplexityPoint[] = [];

    /** Complexity of the current function if it is considered nested to the first level function */
    let complexityIfNested: ComplexityPoint[] = [];

    /** Current nesting level (number of enclosing control flow statements and functions) */
    let nesting = 0;

    /** Indicator if the current top level function has a structural (generated by control flow statements) complexity */
    let topLevelHasStructuralComplexity = false;

    /** Indicator if the current top level function is React functional component */
    const reactFunctionalComponent = {
      nameStartsWithCapital: false,
      returnsJsx: false,

      isConfirmed() {
        return this.nameStartsWithCapital && this.returnsJsx;
      },

      init(node: TSESTree.FunctionLike) {
        this.nameStartsWithCapital = nameStartsWithCapital(node);
        this.returnsJsx = false;
      },
    };

    /** Own (not including nested functions) complexity of the current top function */
    let topLevelOwnComplexity: ComplexityPoint[] = [];

    /** Nodes that should increase nesting level  */
    const nestingNodes: Set<TSESTree.Node> = new Set();

    /** Set of already considered (with already computed complexity) logical expressions */
    const consideredLogicalExpressions: Set<TSESTree.Node> = new Set();

    /** Stack of enclosing functions */
    const enclosingFunctions: TSESTree.FunctionLike[] = [];

    /** Stack of complexity points for each function without accumulated nested complexity */
    const functionOwnComplexity: ComplexityPoint[][] = [];

    const functionOwnControlFlowNesting: number[] = [];

    let secondLevelFunctions: Array<{
      node: TSESTree.FunctionLike;
      parent: TSESTree.Node | undefined;
      complexityIfThisSecondaryIsTopLevel: ComplexityPoint[];
      complexityIfNested: ComplexityPoint[];
      loc: OptionalLocation;
    }> = [];

    return {
      ':function': (node: estree.Node) => {
        onEnterFunction(node as TSESTree.FunctionLike);
      },
      ':function:exit'(node: estree.Node) {
        onLeaveFunction(node as TSESTree.FunctionLike);
      },
      '*'(node: estree.Node) {
        if (nestingNodes.has(node as TSESTree.Node)) {
          nesting++;
          if (functionOwnControlFlowNesting.length > 0) {
            functionOwnControlFlowNesting[functionOwnControlFlowNesting.length - 1]++;
          }
        }
      },
      '*:exit'(node: estree.Node) {
        if (nestingNodes.has(node as TSESTree.Node)) {
          nesting--;
          nestingNodes.delete(node as TSESTree.Node);
          if (functionOwnControlFlowNesting.length > 0) {
            functionOwnControlFlowNesting[functionOwnControlFlowNesting.length - 1]--;
          }
        }
      },
      Program() {
        fileComplexity = 0;
      },
      'Program:exit'(node: estree.Node) {
        if (isFileComplexity) {
          // value from the message will be saved in SonarQube as file complexity metric
          context.report({
            node,
            messageId: 'fileComplexity',
            data: { complexityAmount: fileComplexity as any },
          });
        }
      },
      IfStatement(node: estree.Node) {
        visitIfStatement(node as TSESTree.IfStatement);
      },
      ForStatement(node: estree.Node) {
        visitLoop(node as TSESTree.ForStatement);
      },
      ForInStatement(node: estree.Node) {
        visitLoop(node as TSESTree.ForInStatement);
      },
      ForOfStatement(node: estree.Node) {
        visitLoop(node as TSESTree.ForOfStatement);
      },
      DoWhileStatement(node: estree.Node) {
        visitLoop(node as TSESTree.DoWhileStatement);
      },
      WhileStatement(node: estree.Node) {
        visitLoop(node as TSESTree.WhileStatement);
      },
      SwitchStatement(node: estree.Node) {
        visitSwitchStatement(node as TSESTree.SwitchStatement);
      },
      ContinueStatement(node: estree.Node) {
        visitContinueOrBreakStatement(node as TSESTree.ContinueStatement);
      },
      BreakStatement(node: estree.Node) {
        visitContinueOrBreakStatement(node as TSESTree.BreakStatement);
      },
      CatchClause(node: estree.Node) {
        visitCatchClause(node as TSESTree.CatchClause);
      },
      LogicalExpression(node: estree.Node) {
        visitLogicalExpression(node as TSESTree.LogicalExpression);
      },
      ConditionalExpression(node: estree.Node) {
        visitConditionalExpression(node as TSESTree.ConditionalExpression);
      },
      ReturnStatement(node: estree.Node) {
        visitReturnStatement(node as TSESTree.ReturnStatement);
      },
    };

    function onEnterFunction(node: TSESTree.FunctionLike) {
      if (enclosingFunctions.length === 0) {
        // top level function
        topLevelHasStructuralComplexity = false;
        reactFunctionalComponent.init(node);
        topLevelOwnComplexity = [];
        secondLevelFunctions = [];
      } else if (enclosingFunctions.length === 1) {
        // second level function
        complexityIfNotNested = [];
        complexityIfNested = [];
      } else {
        nesting++;
        nestingNodes.add(node);
      }

      enclosingFunctions.push(node);
      functionOwnComplexity.push([]);
      functionOwnControlFlowNesting.push(0);
    }

    function onLeaveFunction(node: TSESTree.FunctionLike) {
      const functionComplexity = functionOwnComplexity.pop();
      functionOwnControlFlowNesting.pop();

      enclosingFunctions.pop();
      if (enclosingFunctions.length === 0) {
        // top level function
        if (topLevelHasStructuralComplexity && !reactFunctionalComponent.isConfirmed()) {
          let totalComplexity = topLevelOwnComplexity;
          secondLevelFunctions.forEach(secondLevelFunction => {
            totalComplexity = totalComplexity.concat(secondLevelFunction.complexityIfNested);
          });

          fileComplexity += totalComplexity.reduce((acc, cur) => acc + cur.complexity, 0);
        } else {
          fileComplexity += topLevelOwnComplexity.reduce((acc, cur) => acc + cur.complexity, 0);

          secondLevelFunctions.forEach(secondLevelFunction => {
            fileComplexity += secondLevelFunction.complexityIfThisSecondaryIsTopLevel.reduce(
              (acc, cur) => acc + cur.complexity,
              0,
            );
          });
        }
      } else if (enclosingFunctions.length === 1) {
        // second level function
        secondLevelFunctions.push({
          node,
          parent: node.parent,
          complexityIfNested,
          complexityIfThisSecondaryIsTopLevel: complexityIfNotNested,
          loc: getMainFunctionTokenLocation(node, node.parent, context as unknown as RuleContext),
        });
      }

      if (isFileComplexity) {
        return;
      }

      checkFunction(
        functionComplexity,
        getMainFunctionTokenLocation(node, node.parent, context as unknown as RuleContext),
      );
    }

    function visitIfStatement(ifStatement: TSESTree.IfStatement) {
      const { parent } = ifStatement;
      const { loc: ifLoc } = getFirstToken(ifStatement, context as unknown as RuleContext);
      // if the current `if` statement is `else if`, do not count it in structural complexity
      if (isIfStatement(parent) && parent.alternate === ifStatement) {
        addComplexity(ifLoc);
      } else {
        addStructuralComplexity(ifLoc);
      }

      // always increase nesting level inside `then` statement
      nestingNodes.add(ifStatement.consequent);

      // if `else` branch is not `else if` then
      // - increase nesting level inside `else` statement
      // - add +1 complexity
      if (ifStatement.alternate && !isIfStatement(ifStatement.alternate)) {
        nestingNodes.add(ifStatement.alternate);
        const elseTokenLoc = getFirstTokenAfter(
          ifStatement.consequent,
          context as unknown as RuleContext,
        )!.loc;
        addComplexity(elseTokenLoc);
      }
    }

    function visitLoop(loop: LoopStatement) {
      addStructuralComplexity(getFirstToken(loop, context as unknown as RuleContext).loc);
      nestingNodes.add(loop.body);
    }

    function visitSwitchStatement(switchStatement: TSESTree.SwitchStatement) {
      addStructuralComplexity(
        getFirstToken(switchStatement, context as unknown as RuleContext).loc,
      );
      for (const switchCase of switchStatement.cases) {
        nestingNodes.add(switchCase);
      }
    }

    function visitContinueOrBreakStatement(
      statement: TSESTree.ContinueStatement | TSESTree.BreakStatement,
    ) {
      if (statement.label) {
        addComplexity(getFirstToken(statement, context as unknown as RuleContext).loc);
      }
    }

    function visitCatchClause(catchClause: TSESTree.CatchClause) {
      addStructuralComplexity(getFirstToken(catchClause, context as unknown as RuleContext).loc);
      nestingNodes.add(catchClause.body);
    }

    function visitConditionalExpression(conditionalExpression: TSESTree.ConditionalExpression) {
      const questionTokenLoc = getFirstTokenAfter(
        conditionalExpression.test,
        context as unknown as RuleContext,
      )!.loc;
      addStructuralComplexity(questionTokenLoc);
      nestingNodes.add(conditionalExpression.consequent);
      nestingNodes.add(conditionalExpression.alternate);
    }

    function visitReturnStatement({ argument }: TSESTree.ReturnStatement) {
      // top level function
      if (
        enclosingFunctions.length === 1 &&
        argument &&
        ['JSXElement', 'JSXFragment'].includes(argument.type as any)
      ) {
        reactFunctionalComponent.returnsJsx = true;
      }
    }

    function nameStartsWithCapital(node: TSESTree.FunctionLike) {
      const checkFirstLetter = (name: string) => {
        const firstLetter = name[0];
        return firstLetter === firstLetter.toUpperCase();
      };

      if (!isArrowFunctionExpression(node) && node.id) {
        return checkFirstLetter(node.id.name);
      }

      const { parent } = node;
      if (parent && parent.type === 'VariableDeclarator' && parent.id.type === 'Identifier') {
        return checkFirstLetter(parent.id.name);
      }

      return false;
    }

    function visitLogicalExpression(logicalExpression: TSESTree.LogicalExpression) {
      const jsxShortCircuitNodes = getJsxShortCircuitNodes(logicalExpression);
      if (jsxShortCircuitNodes != null) {
        jsxShortCircuitNodes.forEach(node => consideredLogicalExpressions.add(node));
        return;
      }

      if (!consideredLogicalExpressions.has(logicalExpression)) {
        const flattenedLogicalExpressions = flattenLogicalExpression(logicalExpression);

        let previous: TSESTree.LogicalExpression | undefined;
        for (const current of flattenedLogicalExpressions) {
          if (
            current.operator !== '||' &&
            current.operator !== '??' &&
            (!previous || previous.operator !== current.operator)
          ) {
            const operatorTokenLoc = getFirstTokenAfter(
              current.left,
              context as unknown as RuleContext,
            )!.loc;
            addComplexity(operatorTokenLoc);
          }
          previous = current;
        }
      }
    }

    function flattenLogicalExpression(node: TSESTree.Node): TSESTree.LogicalExpression[] {
      if (isLogicalExpression(node)) {
        consideredLogicalExpressions.add(node);
        return [
          ...flattenLogicalExpression(node.left),
          node,
          ...flattenLogicalExpression(node.right),
        ];
      }
      return [];
    }

    function addStructuralComplexity(location: TSESTree.SourceLocation) {
      const added = nesting + 1;
      const complexityPoint = { complexity: added, location };
      if (enclosingFunctions.length === 0) {
        // top level scope
        fileComplexity += added;
      } else if (enclosingFunctions.length === 1) {
        // top level function
        topLevelHasStructuralComplexity = true;
        topLevelOwnComplexity.push(complexityPoint);
      } else {
        // second+ level function
        complexityIfNested.push({ complexity: added + 1, location });
        complexityIfNotNested.push(complexityPoint);
      }

      if (functionOwnComplexity.length > 0) {
        const addedWithoutFunctionNesting =
          functionOwnControlFlowNesting[functionOwnControlFlowNesting.length - 1] + 1;
        functionOwnComplexity[functionOwnComplexity.length - 1].push({
          complexity: addedWithoutFunctionNesting,
          location,
        });
      }
    }

    function addComplexity(location: TSESTree.SourceLocation) {
      const complexityPoint = { complexity: 1, location };
      if (functionOwnComplexity.length > 0) {
        functionOwnComplexity[functionOwnComplexity.length - 1].push(complexityPoint);
      }

      if (enclosingFunctions.length === 0) {
        // top level scope
        fileComplexity += 1;
      } else if (enclosingFunctions.length === 1) {
        // top level function
        topLevelOwnComplexity.push(complexityPoint);
      } else {
        // second+ level function
        complexityIfNested.push(complexityPoint);
        complexityIfNotNested.push(complexityPoint);
      }
    }

    function checkFunction(complexity: ComplexityPoint[] = [], loc: TSESTree.SourceLocation) {
      const complexityAmount = complexity.reduce((acc, cur) => acc + cur.complexity, 0);
      if (complexityAmount > threshold) {
        const secondaryLocations: IssueLocation[] = complexity.map(complexityPoint => {
          const { complexity, location } = complexityPoint;
          const message =
            complexity === 1 ? '+1' : `+${complexity} (incl. ${complexity - 1} for nesting)`;
          return toSecondaryLocation({ loc: location }, message);
        });

        report(
          context,
          {
            messageId: 'refactorFunction',
            message,
            data: {
              complexityAmount: complexityAmount as any,
              threshold: threshold as any, //currently typings do not accept number
            },
            loc,
          },
          secondaryLocations,
          complexityAmount - threshold,
        );
      }
    }
  },
};

type ComplexityPoint = {
  complexity: number;
  location: TSESTree.SourceLocation;
};
