/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
// https://sonarsource.github.io/rspec/#/rspec/S5868/javascript

import { Rule } from 'eslint';
import { Character, CharacterClassElement } from 'regexpp/ast';
import { createRegExpRule } from './helpers/regex';

export const rule: Rule.RuleModule = createRegExpRule(context => {
  function characters(nodes: CharacterClassElement[]): Character[][] {
    let current: Character[] = [];
    const sequences: Character[][] = [current];
    for (const node of nodes) {
      if (node.type === 'Character') {
        current.push(node);
      } else if (node.type === 'CharacterClassRange') {
        // for following regexp [xa-z] we produce [[xa],[z]]
        // we would report for example if instead of 'xa' there would be unicode combined class
        current.push(node.min);
        current = [node.max];
        sequences.push(current);
      } else if (node.type === 'CharacterSet' && current.length > 0) {
        // CharacterSet is for example [\d], ., or \p{ASCII}
        // see https://github.com/mysticatea/regexpp/blob/master/src/ast.ts#L222
        current = [];
        sequences.push(current);
      }
    }
    return sequences;
  }

  // The order matters as surrogate pair check may trigger at the same time as zero-width-joiner.
  const characterChecks = [
    checkCombinedCharacter,
    checkZeroWidthJoinerCharacter,
    checkModifiedEmojiCharacter,
    checkRegionalIndicatorCharacter,
    checkSurrogatePairTailCharacter,
  ];

  function checkSequence(sequence: Character[]) {
    // Stop on the first illegal character in the sequence
    for (let index = 0; index < sequence.length; index++) {
      if (checkCharacter(sequence[index], index, sequence)) {
        return;
      }
    }
  }

  function checkCharacter(character: Character, index: number, characters: Character[]) {
    // Stop on the first failed check as there may be overlaps between checks
    // for instance a zero-width-sequence containing a modified emoji.
    for (const check of characterChecks) {
      if (check(character, index, characters)) {
        return true;
      }
    }
    return false;
  }

  function checkCombinedCharacter(character: Character, index: number, characters: Character[]) {
    let reported = false;
    if (
      index !== 0 &&
      isCombiningCharacter(character.value) &&
      !isCombiningCharacter(characters[index - 1].value)
    ) {
      const combinedChar = characters[index - 1].raw + characters[index].raw;
      const message = `Move this Unicode combined character '${combinedChar}' outside of [...]`;
      context.reportRegExpNode({ regexpNode: characters[index], node: context.node, message });
      reported = true;
    }
    return reported;
  }

  function checkSurrogatePairTailCharacter(
    character: Character,
    index: number,
    characters: Character[],
  ) {
    let reported = false;
    if (index !== 0 && isSurrogatePair(characters[index - 1].value, character.value)) {
      const surrogatePair = characters[index - 1].raw + characters[index].raw;
      const message = `Move this Unicode surrogate pair '${surrogatePair}' outside of [...] or use 'u' flag`;
      context.reportRegExpNode({ regexpNode: characters[index], node: context.node, message });
      reported = true;
    }
    return reported;
  }

  function checkModifiedEmojiCharacter(
    character: Character,
    index: number,
    characters: Character[],
  ) {
    let reported = false;
    if (
      index !== 0 &&
      isEmojiModifier(character.value) &&
      !isEmojiModifier(characters[index - 1].value)
    ) {
      const modifiedEmoji = characters[index - 1].raw + characters[index].raw;
      const message = `Move this Unicode modified Emoji '${modifiedEmoji}' outside of [...]`;
      context.reportRegExpNode({ regexpNode: characters[index], node: context.node, message });
      reported = true;
    }
    return reported;
  }

  function checkRegionalIndicatorCharacter(
    character: Character,
    index: number,
    characters: Character[],
  ) {
    let reported = false;
    if (
      index !== 0 &&
      isRegionalIndicator(character.value) &&
      isRegionalIndicator(characters[index - 1].value)
    ) {
      const regionalIndicator = characters[index - 1].raw + characters[index].raw;
      const message = `Move this Unicode regional indicator '${regionalIndicator}' outside of [...]`;
      context.reportRegExpNode({ regexpNode: characters[index], node: context.node, message });
      reported = true;
    }
    return reported;
  }

  function checkZeroWidthJoinerCharacter(
    character: Character,
    index: number,
    characters: Character[],
  ) {
    let reported = false;
    if (
      index !== 0 &&
      index !== characters.length - 1 &&
      isZeroWidthJoiner(character.value) &&
      !isZeroWidthJoiner(characters[index - 1].value) &&
      !isZeroWidthJoiner(characters[index + 1].value)
    ) {
      // It's practically difficult to determine the full joined character sequence
      // as it may join more than 2 elements that consist of characters or modified Emojis
      // see: https://unicode.org/emoji/charts/emoji-zwj-sequences.html
      const message = 'Move this Unicode joined character sequence outside of [...]';
      context.reportRegExpNode({ regexpNode: characters[index - 1], node: context.node, message });
      reported = true;
    }
    return reported;
  }

  return {
    onCharacterClassEnter(ccNode) {
      for (const chars of characters(ccNode.elements)) {
        checkSequence(chars);
      }
    },
  };
});

function isCombiningCharacter(codePoint: number) {
  return /^[\p{Mc}\p{Me}\p{Mn}]$/u.test(String.fromCodePoint(codePoint));
}

function isSurrogatePair(lead: number, tail: number) {
  return lead >= 0xd800 && lead < 0xdc00 && tail >= 0xdc00 && tail < 0xe000;
}

function isEmojiModifier(code: number) {
  return code >= 0x1f3fb && code <= 0x1f3ff;
}

function isRegionalIndicator(code: number) {
  return code >= 0x1f1e6 && code <= 0x1f1ff;
}

function isZeroWidthJoiner(code: number) {
  return code === 0x200d;
}
