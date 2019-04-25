/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.interfaces;

import androidx.annotation.NonNull;

/**
 * Expressions are used to create the "where" clause for queries
 */
public interface Expression {

    /**
     * Create "NOT" expression
     *
     * In case of double negation, the internal one is deleted and the retrieved expression
     * is the originally non negated one: NOT(NOT(expression)) = expression
     *
     * @return negated expression
     */
    Expression not();


    /**
     * Create "AND" expression
     *
     * @param expressions   expressions to be bound with "AND" operator
     * @return expression
     */
    Expression and(@NonNull Expression... expressions);


    /**
     * Create "OR" expression
     *
     * @param expressions   expressions to be bound with "OR" operator
     * @return expression
     */
    Expression or(@NonNull Expression... expressions);


    /**
     * Create "XOR" expression
     *
     * @param expressions   expressions to be bound with "XOR" operator
     * @return expression
     */
    Expression xor(@NonNull Expression... expressions);


    /**
     * Create "NAND" expression
     *
     * @param expressions   expressions to be bound with "NAND" operator
     * @return expression
     */
    Expression nand(@NonNull Expression... expressions);


    /**
     * Create "NOR" expression
     *
     * @param expressions   expressions to be bound with "NOR" operator
     * @return expression
     */
    Expression nor(@NonNull Expression... expressions);


    /**
     * Create "XNOR" expression
     *
     * @param expressions   expressions to be bound with "XNOR" operator
     * @return expression
     */
    Expression xnor(@NonNull Expression... expressions);

}
