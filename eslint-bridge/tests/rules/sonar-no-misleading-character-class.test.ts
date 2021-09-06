/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2021 SonarSource SA
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
import { RuleTester } from 'eslint';
import { rule } from 'rules/sonar-no-misleading-character-class';
const ruleTester = new RuleTester({ parserOptions: { ecmaVersion: 2018, sourceType: 'module' } });

const surrogatePairWithoutUFlag = "Unexpected surrogate pair in character class. Use 'u' flag.";
const combiningClass = 'Unexpected combined character in character class.';
const emojiModifier = 'Unexpected modified Emoji in character class.';
const regionalIndicatorSymbol = 'Unexpected national flag in character class.';
const zwj = 'Unexpected joined character sequence in character class.';

ruleTester.run('', rule, {
  valid: [
    'var r = /[👍]/u',
    'var r = /[\\uD83D\\uDC4D]/u',
    'var r = /[\\u{1F44D}]/u',
    'var r = /❇️/',
    'var r = /Á/',
    'var r = /[❇]/',
    'var r = /👶🏻/',
    'var r = /[👶]/u',
    'var r = /🇯🇵/',
    'var r = /[JP]/',
    'var r = /👨‍👩‍👦/',

    // Ignore solo lead/tail surrogate.
    'var r = /[\\uD83D]/',
    'var r = /[\\uDC4D]/',
    'var r = /[\\uD83D]/u',
    'var r = /[\\uDC4D]/u',

    // Ignore solo combining char.
    'var r = /[\\u0301]/',
    'var r = /[\\uFE0F]/',
    'var r = /[\\u0301]/u',
    'var r = /[\\uFE0F]/u',

    // Ignore solo emoji modifier.
    'var r = /[\\u{1F3FB}]/u',
    'var r = /[\u{1F3FB}]/u',

    // Coverage
    'var r = /[x\\S]/u',
    'var r = /[xa-z]/u',
  ],
  invalid: [
    // RegExp Literals.
    {
      code: 'var r = /[👍]/',
      errors: [{ column: 12, endColumn: 13, message: surrogatePairWithoutUFlag }],
    },
    {
      code: 'var r = /[\\uD83D\\uDC4D]/',
      errors: [{ column: 17, endColumn: 23, message: surrogatePairWithoutUFlag }],
    },
    {
      code: 'var r = /[Á]/',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[Á]/u',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[\\u0041\\u0301]/',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[\\u0041\\u0301]/u',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[\\u{41}\\u{301}]/u',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[❇️]/',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[❇️]/u',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[\\u2747\\uFE0F]/',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[\\u2747\\uFE0F]/u',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[\\u{2747}\\u{FE0F}]/u',
      errors: [{ message: combiningClass }],
    },
    {
      code: 'var r = /[👶🏻]/',
      errors: [{ message: surrogatePairWithoutUFlag }],
    },
    {
      code: 'var r = /[👶🏻]/u',
      errors: [{ message: emojiModifier }],
    },
    {
      code: 'var r = /[\\uD83D\\uDC76\\uD83C\\uDFFB]/u',
      errors: [{ message: emojiModifier }],
    },
    {
      code: 'var r = /[\\u{1F476}\\u{1F3FB}]/u',
      errors: [{ message: emojiModifier }],
    },
    {
      code: 'var r = /[🇯🇵]/',
      errors: [{ message: surrogatePairWithoutUFlag }],
    },
    {
      code: 'var r = /[🇯🇵]/u',
      errors: [{ message: regionalIndicatorSymbol }],
    },
    {
      code: 'var r = /[\\uD83C\\uDDEF\\uD83C\\uDDF5]/u',
      errors: [{ message: regionalIndicatorSymbol }],
    },
    {
      code: 'var r = /[\\u{1F1EF}\\u{1F1F5}]/u',
      errors: [{ message: regionalIndicatorSymbol }],
    },
    {
      code: 'var r = /[👨‍👩‍👦]/',
      errors: [{ message: surrogatePairWithoutUFlag }, { message: zwj }],
    },
    {
      code: 'var r = /[👨‍👩‍👦]/u',
      errors: [{ message: zwj }],
    },
    {
      code: 'var r = /[\\uD83D\\uDC68\\u200D\\uD83D\\uDC69\\u200D\\uD83D\\uDC66]/u',
      errors: [{ message: zwj }],
    },
    {
      code: 'var r = /[\\u{1F468}\\u{200D}\\u{1F469}\\u{200D}\\u{1F466}]/u',
      errors: [{ message: zwj }],
    },

    // RegExp constructors.
    {
      code: String.raw`var r = new RegExp("[👍]", "")`,
      errors: [{ column: 24, endColumn: 25, message: surrogatePairWithoutUFlag }],
    },
    {
      code: String.raw`var r = new RegExp("[\\uD83D\\uDC4D]", "")`,
      errors: [{ message: surrogatePairWithoutUFlag }],
    },
    {
      code: String.raw`var r = new RegExp("[Á]", "")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[Á]", "u")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u0041\\u0301]", "")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u0041\\u0301]", "u")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u{41}\\u{301}]", "u")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[❇️]", "")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[❇️]", "u")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u2747\\uFE0F]", "")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u2747\\uFE0F]", "u")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u{2747}\\u{FE0F}]", "u")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new RegExp("[👶🏻]", "")`,
      errors: [{ message: surrogatePairWithoutUFlag }],
    },
    {
      code: String.raw`var r = new RegExp("[👶🏻]", "u")`,
      errors: [{ message: emojiModifier }],
    },
    {
      code: String.raw`var r = new RegExp("[\\uD83D\\uDC76\\uD83C\\uDFFB]", "u")`,
      errors: [{ message: emojiModifier }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u{1F476}\\u{1F3FB}]", "u")`,
      errors: [{ message: emojiModifier }],
    },
    {
      code: String.raw`var r = new RegExp("[🇯🇵]", "")`,
      errors: [{ message: surrogatePairWithoutUFlag }],
    },
    {
      code: String.raw`var r = new RegExp("[🇯🇵]", "u")`,
      errors: [{ message: regionalIndicatorSymbol }],
    },
    {
      code: String.raw`var r = new RegExp("[\\uD83C\\uDDEF\\uD83C\\uDDF5]", "u")`,
      errors: [{ message: regionalIndicatorSymbol }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u{1F1EF}\\u{1F1F5}]", "u")`,
      errors: [{ message: regionalIndicatorSymbol }],
    },
    {
      code: String.raw`var r = new RegExp("[👨‍👩‍👦]", "")`,
      errors: [{ message: surrogatePairWithoutUFlag }, { message: zwj }],
    },
    {
      code: String.raw`var r = new RegExp("[👨‍👩‍👦]", "u")`,
      errors: [{ message: zwj }],
    },
    {
      code: String.raw`var r = new RegExp("[\\uD83D\\uDC68\\u200D\\uD83D\\uDC69\\u200D\\uD83D\\uDC66]", "u")`,
      errors: [{ message: zwj }],
    },
    {
      code: String.raw`var r = new RegExp("[\\u{1F468}\\u{200D}\\u{1F469}\\u{200D}\\u{1F466}]", "u")`,
      errors: [{ message: zwj }],
    },
    {
      code: String.raw`var r = new globalThis.RegExp("[❇️]", "")`,
      errors: [{ message: combiningClass }],
    },
    {
      code: String.raw`var r = new globalThis.RegExp("[👶🏻]", "u")`,
      errors: [{ message: emojiModifier }],
    },
    {
      code: String.raw`var r = new globalThis.RegExp("[🇯🇵]", "")`,
      errors: [{ message: surrogatePairWithoutUFlag }],
    },
    {
      code: String.raw`var r = new globalThis.RegExp("[\\u{1F468}\\u{200D}\\u{1F469}\\u{200D}\\u{1F466}]", "u")`,
      errors: [{ message: zwj }],
    },
    {
      code: String.raw`"cc̈d̈d".replaceAll(RegExp("[c̈d̈]"), "X")`,
      errors: [{ message: combiningClass }],
    },
  ],
});
