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
     * Create <code>NOT</code> expression
     *
     * In case of double negation, the internal one is deleted and the retrieved expression
     * is the originally non negated one: <code>NOT(NOT(expression)) = expression</code>
     *
     * @return negated expression
     */
    Expression not();


    /**
     * Create <code>AND</code> expression
     *
     * @param expressions   expressions to be bound with the <code>AND</code> operator
     * @return expression
     */
    Expression and(@NonNull Expression... expressions);


    /**
     * Create <code>OR</code> expression
     *
     * @param expressions   expressions to be bound with the <code>OR</code> operator
     * @return expression
     */
    Expression or(@NonNull Expression... expressions);


    /**
     * Create <code>XOR</code> expression
     *
     * @param expressions   expressions to be bound with the <code>XOR</code> operator
     * @return expression
     */
    Expression xor(@NonNull Expression... expressions);


    /**
     * Create <code>NAND</code> expression
     *
     * @param expressions   expressions to be bound with the <code>NAND</code> operator
     * @return expression
     */
    Expression nand(@NonNull Expression... expressions);


    /**
     * Create <code>NOR</code> expression
     *
     * @param expressions   expressions to be bound with the <code>NOR</code> operator
     * @return expression
     */
    Expression nor(@NonNull Expression... expressions);


    /**
     * Create <code>XNOR</code> expression
     *
     * @param expressions   expressions to be bound with <code>XNOR</code> operator
     * @return expression
     */
    Expression xnor(@NonNull Expression... expressions);

}
