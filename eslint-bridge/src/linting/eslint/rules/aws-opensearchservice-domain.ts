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
// https://sonarsource.github.io/rspec/#/rspec/S6308/javascript

import { Rule } from 'eslint';
import { AwsCdkTemplate } from './helpers/aws/cdk';
import {
  Identifier,
  Literal,
  MemberExpression,
  NewExpression,
  Node,
  ObjectExpression,
  Property,
} from 'estree';
import {
  getProperty,
  getUniqueWriteUsage,
  getValueOfExpression,
  isBooleanLiteral,
  isDotNotation,
  isIdentifier,
  isLiteral,
  isStringLiteral,
  isUndefined,
} from './helpers';

const DOMAIN_PROPS_POSITION = 2;
const ENABLED_PROPERTY = 'enabled';
const OPEN_SEARCH = 'OpenSearch';
const ELASTIC_SEARCH = 'Elasticsearch';

interface DomainCheckerOptions {
  encryptionProperty: string;
  version: {
    valueType: 'ElasticsearchVersion' | 'EngineVersion' | 'string';
    property: string;
    defaultValue: typeof OPEN_SEARCH | typeof ELASTIC_SEARCH;
  };
}

export const rule: Rule.RuleModule = AwsCdkTemplate(
  {
    'aws-cdk-lib.aws-opensearchservice.Domain': domainChecker({
      encryptionProperty: 'encryptionAtRest',
      version: {
        valueType: 'EngineVersion',
        property: 'version',
        defaultValue: OPEN_SEARCH,
      },
    }),
    'aws-cdk-lib.aws-opensearchservice.CfnDomain': domainChecker({
      encryptionProperty: 'encryptionAtRestOptions',
      version: {
        valueType: 'string',
        property: 'engineVersion',
        defaultValue: OPEN_SEARCH,
      },
    }),
    'aws-cdk-lib.aws-elasticsearch.Domain': domainChecker({
      encryptionProperty: 'encryptionAtRest',
      version: {
        valueType: 'ElasticsearchVersion',
        property: 'version',
        defaultValue: ELASTIC_SEARCH,
      },
    }),
    'aws-cdk-lib.aws-elasticsearch.CfnDomain': domainChecker({
      encryptionProperty: 'encryptionAtRestOptions',
      version: {
        valueType: 'string',
        property: 'elasticsearchVersion',
        defaultValue: ELASTIC_SEARCH,
      },
    }),
  },
  {
    meta: {
      messages: {
        encryptionDisabled: 'Make sure that using unencrypted {{search}} domains is safe here.',
        encryptionOmitted:
          'Omitting {{encryptionPropertyName}} causes encryption of data at rest to be ' +
          'disabled for this {{search}} domain. Make sure it is safe here.',
      },
    },
  },
);

function domainChecker(options: DomainCheckerOptions) {
  return (expr: NewExpression, ctx: Rule.RuleContext) => {
    const argument = queryArgument(expr, DOMAIN_PROPS_POSITION).filter('ObjectExpression');
    const encryptionProperty = queryEncryptionProperty(argument, options.encryptionProperty);
    const isEnabled = encryptionProperty.map(node => queryValue(node, 'boolean'));
    const version = getVersion(argument, options);

    if (isEnabled.isMissing) {
      ctx.report({
        messageId: 'encryptionOmitted',
        node: isEnabled.node,
        data: {
          encryptionPropertyName: options.encryptionProperty,
          search: getSearchEngine(version) ?? options.version.defaultValue,
        },
      });
    } else if (isEnabled.isFalse) {
      ctx.report({
        messageId: 'encryptionDisabled',
        node: encryptionProperty.node,
        data: {
          search: getSearchEngine(version) ?? options.version.defaultValue,
        },
      });
    }

    function queryEncryptionProperty(argument: Result, propertyName: string) {
      const encryptionProperty = argument
        .map((object: ObjectExpression) => queryProperty(object, propertyName))
        .map((property: Property) => property.value);
      return encryptionProperty
        .filter('ObjectExpression')
        .map((object: ObjectExpression) => queryProperty(object, ENABLED_PROPERTY))
        .map((property: Property) => property.value);
    }

    function getVersion(argument: Result, options: DomainCheckerOptions) {
      const version = argument
        .map((object: ObjectExpression) => queryProperty(object, options.version.property))
        .map((property: Property) => property.value);
      const versionValue = getVersionValue(version, options);
      return versionValue?.toLowerCase();
    }

    function getVersionValue(version: Result, options: DomainCheckerOptions) {
      if (options.version.valueType === 'string') {
        const versionValue = version.map(node => queryValue(node, 'string')).asString();
        return `${options.version.property}_${versionValue}`;
      } else if (isMemberIdentifier(version.node)) {
        const versionValue = version.node.property.name;
        if (isIdentifier(version.node.object)) {
          return `${version.node.object.name}_${versionValue}`;
        } else {
          return `${version.node.object.property.name}_${versionValue}`;
        }
      } else {
        return null;
      }
    }

    function getSearchEngine(version: string | undefined) {
      if (version?.includes('opensearch')) {
        return OPEN_SEARCH;
      } else if (version?.includes('elasticsearch')) {
        return ELASTIC_SEARCH;
      } else {
        return null;
      }
    }

    // From here up to the end of the file the code should be shared somehow.

    function queryArgument(node: NewExpression, position: number) {
      const argument = node.arguments[position];
      if (argument == null) {
        return missing(node);
      } else if (isUndefined(argument)) {
        return missing(argument);
      }

      const expression = getValueOfExpression(ctx, argument, 'ObjectExpression');
      return expression == null ? unknown(node) : found(expression);
    }

    function queryProperty(node: ObjectExpression, name: string): Result {
      const property = getProperty(node, name, ctx);
      if (property === undefined) {
        return unknown(node);
      } else if (property === null) {
        return missing(node);
      } else {
        return found(property);
      }
    }

    function queryValue(node: Node, type: 'string' | 'boolean') {
      if (isLiteral(node)) {
        return queryValueFromLiteral(node, type);
      } else if (isIdentifier(node)) {
        return queryValueFromIdentifier(node, type);
      } else {
        return unknown(node);
      }
    }

    function queryValueFromLiteral(node: Literal, type: 'string' | 'boolean') {
      if (typeof node.value !== type) {
        return unknown(node);
      }
      return found(node);
    }

    function queryValueFromIdentifier(node: Identifier, type: 'string' | 'boolean'): Result {
      if (isUndefined(node)) {
        return missing(node);
      }

      const usage = getUniqueWriteUsage(ctx, node.name);
      if (!usage) {
        return unknown(node);
      }

      return queryValue(usage, type).withNodeIfNotFound(node);
    }
  };
}

class Result {
  constructor(readonly node: Node, readonly status: 'missing' | 'unknown' | 'found') {}

  get isFound() {
    return this.status === 'found';
  }

  get isMissing() {
    return this.status === 'missing';
  }

  get isFalse() {
    return isBooleanLiteral(this.node) && !this.node.value;
  }

  asString() {
    return isStringLiteral(this.node) ? this.node.value : null;
  }

  map<N extends Node>(_closure: (node: N) => Result | Node): Result {
    return this;
  }

  filter(_type: string): Result {
    return this;
  }

  withNodeIfNotFound(node: Node): Result {
    return !this.isFound ? new Result(node, this.status) : this;
  }
}

class FoundResult extends Result {
  constructor(value: Node) {
    super(value, 'found');
  }

  map<N extends Node>(closure: (node: N) => Result | Node): Result {
    const resultOrNode = closure(this.node as N);
    return resultOrNode instanceof Result ? resultOrNode : found(resultOrNode);
  }

  filter(type: string): Result {
    return this.node.type === type ? this : unknown(this.node);
  }
}

function unknown(node: Node): Result {
  return new Result(node, 'unknown');
}

function missing(node: Node): Result {
  return new Result(node, 'missing');
}

function found(node: Node) {
  return new FoundResult(node);
}

type MemberIdentifier = MemberExpression & {
  object: Identifier | (MemberExpression & { property: Identifier });
  property: Identifier;
};

function isMemberIdentifier(node: Node): node is MemberIdentifier {
  return isDotNotation(node) && (isIdentifier(node.object) || isDotNotation(node.object));
}
