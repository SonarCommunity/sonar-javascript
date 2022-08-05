/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
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
// https://sonarsource.github.io/rspec/#/rspec/S6265/javascript

import { Rule } from 'eslint';
import {
  findPropagatedSetting,
  getProperty,
  getValueOfExpression,
  hasFullyQualifiedName,
  S3BucketTemplate,
  toEncodedMessage,
} from '../utils';
import { NewExpression } from 'estree';

const messages = {
  accessLevel: (param: string) => `Make sure granting ${param} access is safe here.`,
  unrestricted: 'Make sure allowing unrestricted access to objects from this bucket is safe here.',
  secondary: 'Propagated setting.',
};

const props = [{
  name: 'accessControl',
  values: ['PUBLIC_READ', 'PUBLIC_READ_WRITE', 'AUTHENTICATED_READ'],
}]
/* const propNames = {
  ACCESS_CONTROL: 'accessControl',
  PUBLIC_READ_ACCESS: 'publicReadAccess',
};
const accessControlSensitiveValues = ['PUBLIC_READ']; */

export const rule: Rule.RuleModule = S3BucketTemplate(
  (bucketConstructor, context) => {
    for (const prop of props) {
      for (const value of prop.values) {
        checkParam(context, bucketConstructor, prop.name, ['BucketAccessControl', value]);
      }
    }
  },
  {
    meta: {
      schema: [
        {
          // internal parameter for rules having secondary locations
          enum: ['sonar-runtime'],
        },
      ],
    },
  },
);

function checkParam(context: Rule.RuleContext, bucketConstructor: NewExpression, propName: string, paramQualifiers: string[]) {
  const property = getProperty(context, bucketConstructor, propName);
    if (property == null) {
      return;
    }
    // s3.BucketAccessControl.PUBLIC_READ
    const propertyLiteralValue = getValueOfExpression(context, property.value, 'MemberExpression');
    if (
      propertyLiteralValue !== undefined &&
      hasFullyQualifiedName(
        context,
        propertyLiteralValue,
        'aws-cdk-lib/aws-s3', ...paramQualifiers,
      )
    ) {
      const secondary = findPropagatedSetting(property, propertyLiteralValue);
      context.report({
        message: toEncodedMessage(
          messages.accessLevel(paramQualifiers[paramQualifiers.length-1]),
          secondary.locations,
          secondary.messages,
        ),
        node: property,
      });
    }
}
