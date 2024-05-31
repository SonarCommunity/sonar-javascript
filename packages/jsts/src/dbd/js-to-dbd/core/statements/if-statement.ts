import { TSESTree } from '@typescript-eslint/utils';
import { handleExpression } from '../expressions';
import type { Block } from '../block';
import { createBranchingInstruction } from '../instructions/branching-instruction';
import { createConditionalBranchingInstruction } from '../instructions/conditional-branching-instruction';
import { createScopeDeclarationInstruction, isTerminated } from '../utils';
import { handleStatement } from './index';
import type { StatementHandler } from '../statement-handler';
import { AST_NODE_TYPES } from '@typescript-eslint/typescript-estree';

export const handleIfStatement: StatementHandler<TSESTree.IfStatement> = (node, context) => {
  const { consequent, alternate, test } = node;
  const { blockManager, scopeManager, createScopedBlock } = context;
  const { getCurrentBlock, pushBlock } = blockManager;

  // the "finally" block belongs to the same scope as the current block
  const finallyBlock = createScopedBlock(node.loc);

  const processNode = (innerNode: TSESTree.Statement | null): Block => {
    const environmentRecord = scopeManager.createDeclarativeEnvironmentRecord(context.functionInfo);

    scopeManager.pushEnvironmentRecord(environmentRecord);

    let block;
    if (innerNode === null) {
      innerNode = {
        type: AST_NODE_TYPES.BlockStatement,
        parent: node.parent,
        loc: node.loc,
        range: node.range,
        body: [],
      };
    }
    const loc = innerNode.loc;

    block = createScopedBlock(loc);

    block.instructions.push(createScopeDeclarationInstruction(environmentRecord, innerNode.loc));

    pushBlock(block);

    handleStatement(innerNode, context);

    scopeManager.popEnvironmentRecord();

    if (!isTerminated(getCurrentBlock())) {
      // branch the CURRENT BLOCK to the finally one
      getCurrentBlock().instructions.push(createBranchingInstruction(finallyBlock, loc));
    }

    return block;
  };

  const { value: testValue } = handleExpression(
    test,
    scopeManager.getCurrentEnvironmentRecord(),
    context,
  );

  const currentBlock = getCurrentBlock();

  // process the consequent block
  const consequentBlock = processNode(consequent);

  // process the alternate block
  const alternateBlock = processNode(alternate);

  // add the conditional branching instruction
  currentBlock.instructions.push(
    createConditionalBranchingInstruction(testValue, consequentBlock, alternateBlock, node.loc),
  );

  pushBlock(finallyBlock);
};
