/*******************************************************************************
 * Copyright (c) 2018 Microsoft Research. All rights reserved.
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/
package tlc2.value.impl;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import tlc2.util.FP64;
import tlc2.value.IValue;
import tlc2.value.RandomEnumerableValues;
import tlc2.value.ValueEnumeration;
import tlc2.value.ValueVec;
import tlc2.value.impl.EnumerableValue;
import tlc2.value.impl.IntervalValue;
import tlc2.value.impl.SetEnumValue;
import tlc2.value.impl.SubsetValue;

@State(Scope.Benchmark)
public class EnumerateSubsetBenchmark {

	static {
		RandomEnumerableValues.setSeed(15041980L);
		RandomEnumerableValues.reset();

		FP64.Init();
	}
	
	@Param({"0", "1", "2", "3", "4", "8", "10", "12", "14", "16", "18", "19"})
	public int numOfElements;

	@Benchmark
	public EnumerableValue elementsAlwaysNormalized() {
		final IntervalValue inner = new IntervalValue(1, numOfElements);
		final SubsetValue subset = new SubsetValue(inner);

		final ValueVec vals = new ValueVec(subset.size());
		final ValueEnumeration Enum = subset.elementsNormalized();
		IValue elem;
		while ((elem = Enum.nextElement()) != null) {
			vals.addElement(elem);
		}
        return (EnumerableValue) new SetEnumValue(vals, true).normalize();
	}

	@Benchmark
	public EnumerableValue kElementsNotNormalized() {
		final IntervalValue inner = new IntervalValue(1, numOfElements);
		final SubsetValue subset = new SubsetValue(inner);

		final ValueVec vec = new ValueVec(subset.size());
		for (int i = 0; i <= inner.size(); i++) {
			final ValueEnumeration Enum = subset.kElements(i);
			IValue elem;
			while ((elem = Enum.nextElement()) != null) {
				vec.addElement(elem);
			}
		}
        return (EnumerableValue) new SetEnumValue(vec, false);
	}
	
	@Benchmark
	public EnumerableValue kElementsNormalized() {
		final IntervalValue inner = new IntervalValue(1, numOfElements);
		final SubsetValue subset = new SubsetValue(inner);

		final ValueVec vec = new ValueVec(subset.size());
		for (int i = 0; i <= inner.size(); i++) {
			final ValueEnumeration Enum = subset.kElements(i);
			IValue elem;
			while ((elem = Enum.nextElement()) != null) {
				vec.addElement(elem);
			}
		}
        return (EnumerableValue) new SetEnumValue(vec, false).normalize();
	}

	@Benchmark
	public EnumerableValue elementsNotNormalized() {
		final IntervalValue inner = new IntervalValue(1, numOfElements);
		final SubsetValue subset = new SubsetValue(inner);
		
		final ValueVec vals = new ValueVec(subset.size());
		final ValueEnumeration Enum = subset.elementsLexicographic();
		IValue elem;
		while ((elem = Enum.nextElement()) != null) {
			vals.addElement(elem);
		}
		return (EnumerableValue) new SetEnumValue(vals, false);
	}

	@Benchmark
	public EnumerableValue elementsNormalized() {
		final IntervalValue inner = new IntervalValue(1, numOfElements);
		final SubsetValue subset = new SubsetValue(inner);
		
		final ValueVec vals = new ValueVec(subset.size());
		final ValueEnumeration Enum = subset.elementsLexicographic();
		IValue elem;
		while ((elem = Enum.nextElement()) != null) {
			vals.addElement(elem);
		}
		return (EnumerableValue) new SetEnumValue(vals, false).normalize();
	}
}