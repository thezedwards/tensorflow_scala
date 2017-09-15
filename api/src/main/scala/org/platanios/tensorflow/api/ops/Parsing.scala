/* Copyright 2017, Emmanouil Antonios Platanios. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.platanios.tensorflow.api.ops

import org.platanios.tensorflow.api.core.Shape
import org.platanios.tensorflow.api.ops.Gradients.{Registry => GradientsRegistry}
import org.platanios.tensorflow.api.types.{DataType, FLOAT32, INT32, STRING, UINT8}

/** Contains functions for constructing ops related to parsing data.
  *
  * @author Emmanouil Antonios Platanios
  */
trait Parsing {
  /** $OpDocParsingEncodeTensor
    *
    * @group ParsingOps
    * @param  tensor Tensor to encode.
    * @param  name   Name for the created op.
    * @return Created op output.
    */
  def encodeTensor(tensor: Output, name: String = "EncodeTensor"): Output = {
    Op.Builder(opType = "SerializeTensor", name = name)
        .addInput(tensor)
        .build().outputs(0)
  }

  /** $OpDocParsingDecodeTensor
    *
    * @group ParsingOps
    * @param  data     [[STRING]] tensor containing a serialized `TensorProto` proto.
    * @param  dataType Data type of the serialized tensor. The provided data type must match the data type of the
    *                  serialized tensor and no implicit conversion will take place.
    * @param  name     Name for the created op.
    * @return Created op output.
    * @throws IllegalArgumentException If `data` is not a [[STRING]] tensor.
    */
  @throws[IllegalArgumentException]
  def decodeTensor(data: Output, dataType: DataType, name: String = "DecodeTensor"): Output = {
    require(data.dataType == STRING, s"Tensor data type was ${data.dataType}, while STRING was expected.")
    Op.Builder(opType = "ParseTensor", name = name)
        .addInput(data)
        .setAttribute("out_type", dataType)
        .build().outputs(0)
  }

  /** $OpDocParsingDecodeRaw
    *
    * @group ParsingOps
    * @param  bytes        [[STRING]] tensor interpreted as raw bytes. All the elements must have the same length.
    * @param  dataType     Output tensor data type.
    * @param  littleEndian Boolean value indicating whether the input `bytes` are stored in little-endian order. Ignored
    *                      for `dataType` values that are stored in a single byte, like [[UINT8]].
    * @param  name         Name for the created op.
    * @return Created op output.
    * @throws IllegalArgumentException If `bytes` is not a [[STRING]] tensor.
    */
  @throws[IllegalArgumentException]
  def decodeRaw(bytes: Output, dataType: DataType, littleEndian: Boolean = true, name: String = "DecodeRaw"): Output = {
    require(bytes.dataType == STRING, s"Tensor data type was ${bytes.dataType}, while STRING was expected.")
    Op.Builder(opType = "DecodeRaw", name = name)
        .addInput(bytes)
        .setAttribute("out_type", dataType)
        .setAttribute("little_endian", littleEndian)
        .build().outputs(0)
  }

  /** $OpDocParsingDecodeCSV
    *
    * @group ParsingOps
    * @param  records            [[STRING]] tensor where each string is a record/row in the csv and all records should
    *                            have the same format.
    * @param  recordDefaults     One tensor per column of the input record, with either a scalar default value for that
    *                            column or empty if the column is required.
    * @param  dataTypes          Output tensor data types.
    * @param  delimiter          Delimiter used to separate fields in a record.
    * @param  useQuoteDelimiters If `false`, the op treats double quotation marks as regular characters inside the
    *                            string fields (ignoring RFC 4180, Section 2, Bullet 5).
    * @param  name               Name for the created op.
    * @return Created op outputs.
    * @throws IllegalArgumentException If `records` is not a [[STRING]] tensor.
    */
  @throws[IllegalArgumentException]
  def decodeCSV(
      records: Output, recordDefaults: Seq[Output], dataTypes: Seq[DataType], delimiter: String = ",",
      useQuoteDelimiters: Boolean = true, name: String = "DecodeCSV"): Seq[Output] = {
    require(records.dataType == STRING, s"Tensor data type was ${records.dataType}, while STRING was expected.")
    Op.Builder(opType = "DecodeCSV", name = name)
        .addInput(records)
        .addInputList(recordDefaults)
        .setAttribute("OUT_TYPE", dataTypes.toArray)
        .setAttribute("field_delim", delimiter)
        .setAttribute("use_quote_delim", useQuoteDelimiters)
        .build().outputs.toSeq
  }

  /** $OpDocParsingStringToNumber
    *
    * @group ParsingOps
    * @param  data     [[STRING]] tensor containing string representations of numbers.
    * @param  dataType Output tensor data type.
    * @param  name     Name for the created op.
    * @return Created op output.
    * @throws IllegalArgumentException If `data` is not a [[STRING]] tensor.
    */
  @throws[IllegalArgumentException]
  def stringToNumber(data: Output, dataType: DataType, name: String = "StringToNumber"): Output = {
    require(data.dataType == STRING, s"Tensor data type was ${data.dataType}, while STRING was expected.")
    Op.Builder(opType = "StringToNumber", name = name)
        .addInput(data)
        .setAttribute("out_type", dataType)
        .build().outputs(0)
  }

  /** $OpDocParsingDecodeJSONExample
    *
    * @group ParsingOps
    * @param  jsonExamples [[STRING]] tensor where each string is a JSON object serialized according to the JSON mapping
    *                      of the `Example` proto.
    * @param  name         Name for the created op.
    * @return Created op output.
    * @throws IllegalArgumentException If `jsonExamples` is not a [[STRING]] tensor.
    */
  @throws[IllegalArgumentException]
  def decodeJSONExample(jsonExamples: Output, name: String = "DecodeJSONExample"): Output = {
    require(
      jsonExamples.dataType == STRING, s"Tensor data type was ${jsonExamples.dataType}, while STRING was expected.")
    Op.Builder(opType = "DecodeJSONExample", name = name)
        .addInput(jsonExamples)
        .build().outputs(0)
  }
}

private[ops] object Parsing extends Parsing {
  /** Creates an op that transforms a vector of `Example` protos (represented as strings) into typed tensors.
    *
    * @param  bytes         [[STRING]] tensor containing containing a batch of binary serialized `Example` protos.
    * @param  sparseKeys    [[STRING]] rank-0 tensors containing the keys expected in the `Example` features associated
    *                       with sparse values.
    * @param  sparseTypes   Data types of the `Example` features associated with sparse values.
    * @param  denseKeys     [[STRING]] rank-0 tensors containing the keys expected in the `Example` features associated
    *                       with dense values.
    * @param  denseShapes   Rank-1 tensors containing the shapes of the values in each feature given in `denseKeys`. The
    *                       number of elements in the feature corresponding to `denseKeys(j)` must always be equal to
    *                       `denseShapes(j).size`. If `denseShapes(j) == (D0, D1, ..., DN)` then the shape of the output
    *                       tensor `denseValues(j)` will be `(|bytes|, D0, D1, ..., DN)`: the dense outputs are just the
    *                       inputs row-stacked by batch. This works for `denseShapes(j) = (-1, D1, ..., DN)`. In this
    *                       case, the shape of the output tensor `denseValues(j)` will be `(|bytes|, M, D1, .., DN)`,
    *                       where `M` is the maximum number of blocks of elements of length `D1 * .... * DN`, across all
    *                       minibatch entries in the input. Any minibatch entry with less than `M` blocks of elements of
    *                       length `D1 * ... * DN` will be padded with the corresponding `denseDefaults` scalar element
    *                       along the second dimension.
    * @param  denseDefaults Tensors (some of which may be empty) containing default dense values. `denseDefaults(j)`
    *                       provides default values when the example's feature map lacks `denseKeys(j)`. If an empty
    *                       tensor is provided for `denseDefaults(j)`, then the feature `denseKeys(j)` is required. The
    *                       input type is inferred from `denseDefaults(j)`, even when it's empty. If `denseDefaults(j)`
    *                       is not empty, and `denseShapes(j)` is fully defined, then the shape of `denseDefaults(j)`
    *                       must match that of `denseShapes(j)`. If `denseShapes(j)` has an undefined major dimension
    *                       (variable strides dense feature), `denseDefaults(j)` must contain a single element: the
    *                       padding element.
    * @param  debugNames    [[STRING]] rank-1 tensor containing the names of the serialized protos. May contain, for
    *                       example, table key (descriptive) names for the corresponding serialized protos. These are
    *                       purely useful for debugging purposes, and the presence of values here has no effect on the
    *                       output. May also be an empty vector if no names are available. If non-empty, this vector
    *                       must have the same length as `bytes`.
    * @param  name          Name for the created op.
    * @return Tuple containing:
    *         1. Sparse tensor indices.
    *         2. Sparse tensor values.
    *         3. Sparse tensor shapes.
    *         4. Dense tensor values.
    * @throws IllegalArgumentException If any of the input arguments has invalid data type or size.
    */
  @throws[IllegalArgumentException]
  private[Parsing] def parseExample(
      bytes: Output, sparseKeys: Seq[Output], sparseTypes: Seq[DataType], denseKeys: Seq[Output],
      denseShapes: Seq[Shape], denseDefaults: Seq[Output], debugNames: Output,
      name: String = "ParseExample"): (Seq[Output], Seq[Output], Seq[Output], Seq[Output]) = {
    require(bytes.dataType == STRING, s"Tensor data type was ${bytes.dataType}, while STRING was expected.")
    require(!sparseKeys.exists(_.dataType == STRING), "The sparse keys must all be STRING tensors.")
    require(!denseKeys.exists(_.dataType == STRING), "The dense keys must all be STRING tensors.")
    require(sparseKeys.length == sparseTypes.length, "The number of sparse keys does not match that of sparse types.")
    require(denseKeys.length == denseShapes.length, "The number of dense keys does not match that of dense shapes.")
    require(denseKeys.length == denseDefaults.length, "The number of dense keys does not match that of dense defaults.")
    require(debugNames.dataType == STRING, s"Tensor data type was ${debugNames.dataType}, while STRING was expected.")
    val numSparse = sparseKeys.length
    val numDense = denseKeys.length
    val outputs = Op.Builder(opType = "ParseExample", name = name)
        .addInput(bytes)
        .addInput(debugNames)
        .addInputList(sparseKeys)
        .addInputList(denseKeys)
        .addInputList(denseDefaults)
        .setAttribute("sparse_types", sparseTypes.toArray)
        .setAttribute("dense_shapes", denseShapes.toArray)
        .build().outputs
    val sparseIndices = outputs.take(numSparse)
    val sparseValues = outputs.slice(numSparse, 2 * numSparse)
    val sparseShapes = outputs.slice(2 * numSparse, 3 * numSparse)
    val denseValues = outputs.takeRight(numDense)
    (sparseIndices, sparseValues, sparseShapes, denseValues)
  }

  /** Creates an op that transforms a scalar `SequenceExample` protos (represented as strings) into typed tensors.
    *
    * @param  bytes                               [[STRING]] tensor containing containing the binary serialized
    *                                             `SequenceExample` proto.
    * @param  contextSparseKeys                   [[STRING]] rank-0 tensors containing the keys expected in the
    *                                             `SequenceExample` features associated with context sparse values.
    * @param  contextSparseTypes                  Data types of the `SequenceExample` features associated with context
    *                                             sparse values.
    * @param  contextDenseKeys                    [[STRING]] rank-0 tensors containing the keys expected in the
    *                                             `SequenceExample` features associated with context dense values.
    * @param  contextDenseShapes                  Rank-1 tensors containing the shapes of the values in each context
    *                                             feature given in `contextDenseKeys`. The  number of elements in the
    *                                             feature corresponding to `contextDenseKeys(j)` must always be equal to
    *                                             `contextDenseShapes(j).size`. The shape of `contextDenseValues(j)`
    *                                             will match `contextDenseShapes(j)`.
    * @param  contextDenseDefaults                Tensors (some of which may be empty) containing default context dense
    *                                             values. `contextDenseDefaults(j)` provides default values when the
    *                                             sequence example's feature map lacks `contextDenseKeys(j)`. If an
    *                                             empty tensor is provided for `contextDenseDefaults(j)`, then the
    *                                             feature `contextDenseKeys(j)` is required. The input type is inferred
    *                                             from `contextDenseDefaults(j)`, even when it's empty. If
    *                                             `contextDenseDefaults(j)` is not empty, its shape must match
    *                                             `contextDenseShapes(j)`.
    * @param  featureListSparseKeys               [[STRING]] rank-0 tensors containing the keys expected in the
    *                                             `FeatureList`s associated with sparse values.
    * @param  featureListSparseTypes              Data types of data in each `FeatureList` given in
    *                                             `featureListSparseKeys`.
    * @param  featureListDenseKeys                [[STRING]] rank-0 tensors containing the keys expected in the
    *                                             `SequenceExample` feature lists associated with lists of dense values.
    * @param  featureListDenseShapes              Rank-1 tensors containing the shapes of the values in each feature
    *                                             list given in `featureListDenseKeys`. The  number of elements in the
    *                                             feature corresponding to `featureListDenseKeys(j)` must always be
    *                                             equal to `featureListDenseShapes(j).size`. The shape of
    *                                             `featureListDenseValues(j)` will match `featureListDenseShapes(j)`.
    * @param  featureListDenseMissingAssumedEmpty [[STRING]] rank-1 tensor containing the `FeatureList` keys which may
    *                                             be missing from the `SequenceExample`. If the associated `FeatureList`
    *                                             is missing, it is treated as empty. By default, any `FeatureList` not
    *                                             listed in this vector must exist in the `SequenceExample`.
    * @param  debugName                           [[STRING]] rank-0 tensor containing the name of the serialized proto.
    *                                             May contain, for example, a table key (descriptive) name for the
    *                                             corresponding serialized proto. This is purely useful for debugging
    *                                             purposes, and the presence of values here has no effect on the output.
    *                                             May also be an empty scalar if no name is available.
    * @param  name                                Name for the created op.
    * @return Tuple containing:
    *         1. Context sparse tensor indices.
    *         2. Context sparse tensor values.
    *         3. Context sparse tensor shapes.
    *         4. Context dense tensor values.
    *         5. Feature list sparse tensor indices.
    *         6. Feature list sparse tensor values.
    *         7. Feature list sparse tensor shapes.
    *         8. Feature list dense tensor values.
    * @throws IllegalArgumentException If any of the input arguments has invalid data type or size.
    */
  private[Parsing] def parseSingleSequenceExample(
      bytes: Output,
      contextSparseKeys: Seq[Output], contextSparseTypes: Seq[DataType],
      contextDenseKeys: Seq[Output], contextDenseShapes: Seq[Shape], contextDenseDefaults: Seq[Output],
      featureListSparseKeys: Seq[Output], featureListSparseTypes: Seq[DataType],
      featureListDenseKeys: Seq[Output], featureListDenseShapes: Seq[Shape],
      featureListDenseMissingAssumedEmpty: Output, debugName: Output, name: String = "ParseSingleSequenceExample"):
  (Seq[Output], Seq[Output], Seq[Output], Seq[Output], Seq[Output], Seq[Output], Seq[Output], Seq[Output]) = {
    require(bytes.dataType == STRING, s"Tensor data type was ${bytes.dataType}, while STRING was expected.")
    require(!contextSparseKeys.exists(_.dataType == STRING), "The context sparse keys must all be STRING tensors.")
    require(!contextDenseKeys.exists(_.dataType == STRING), "The context dense keys must all be STRING tensors.")
    require(!featureListSparseKeys.exists(_.dataType == STRING),
            "The feature list sparse keys must all be STRING tensors.")
    require(!featureListDenseKeys.exists(_.dataType == STRING),
            "The feature list dense keys must all be STRING tensors.")
    require(contextSparseKeys.length == contextSparseTypes.length,
            "The number of context sparse keys does not match that of context sparse types.")
    require(contextDenseKeys.length == featureListDenseShapes.length,
            "The number of context dense keys does not match that of context dense shapes.")
    require(contextDenseKeys.length == contextDenseDefaults.length,
            "The number of context dense keys does not match that of context dense defaults.")
    require(featureListSparseKeys.length == featureListSparseTypes.length,
            "The number of feature list sparse keys does not match that of feature list sparse types.")
    require(featureListDenseKeys.length == featureListDenseShapes.length,
            "The number of feature list dense keys does not match that of feature list dense shapes.")
    require(featureListDenseMissingAssumedEmpty.dataType == STRING,
            s"Tensor data type was ${featureListDenseMissingAssumedEmpty.dataType}, while STRING was expected.")
    require(debugName.dataType == STRING, s"Tensor data type was ${debugName.dataType}, while STRING was expected.")
    val numContextSparse = contextSparseKeys.length
    val numContextDense = contextDenseKeys.length
    val numFeatureListSparse = featureListSparseKeys.length
    val numFeatureListDense = featureListDenseKeys.length
    val outputs = Op.Builder(opType = "ParseSingleSequenceExample", name = name)
        .addInput(bytes)
        .addInput(featureListDenseMissingAssumedEmpty)
        .addInputList(contextSparseKeys)
        .addInputList(contextDenseKeys)
        .addInputList(featureListSparseKeys)
        .addInputList(featureListDenseKeys)
        .addInputList(contextDenseDefaults)
        .addInput(debugName)
        .setAttribute("context_sparse_types", contextSparseTypes.toArray)
        .setAttribute("context_dense_shapes", contextDenseShapes.toArray)
        .setAttribute("feature_list_sparse_types", featureListSparseTypes.toArray)
        .setAttribute("feature_list_dense_shapes", featureListDenseShapes.toArray)
        .build().outputs
    var index = 0
    val contextSparseIndices = outputs.take(index + numContextSparse)
    index += numContextSparse
    val contextSparseValues = outputs.slice(index, index + numContextSparse)
    index += numContextSparse
    val contextSparseShapes = outputs.slice(index, index + numContextSparse)
    index += numContextSparse
    val contextDenseValues = outputs.slice(index, index + numContextDense)
    index += numContextDense
    val featureListSparseIndices = outputs.slice(index, index + numFeatureListSparse)
    index += numFeatureListSparse
    val featureListSparseValues = outputs.slice(index, index + numFeatureListSparse)
    index += numFeatureListSparse
    val featureListSparseShapes = outputs.slice(index, index + numFeatureListSparse)
    index += numFeatureListSparse
    val featureListDenseValues = outputs.slice(index, index + numFeatureListDense)
    (contextSparseIndices, contextSparseValues, contextSparseShapes, contextDenseValues,
        featureListSparseIndices, featureListSparseValues, featureListSparseShapes, featureListDenseValues)
  }

  private[ops] object Gradients {
    GradientsRegistry.registerNonDifferentiable("SerializeTensor")
    GradientsRegistry.registerNonDifferentiable("ParseTensor")
    GradientsRegistry.registerNonDifferentiable("DecodeRaw")
    GradientsRegistry.registerNonDifferentiable("DecodeCSV")
    GradientsRegistry.registerNonDifferentiable("ParseExample")
    GradientsRegistry.registerNonDifferentiable("ParseSingleSequenceExample")
    GradientsRegistry.registerNonDifferentiable("StringToNumber")
    GradientsRegistry.registerNonDifferentiable("DecodeJSONExample")
  }

  /** @define OpDocParsingEncodeTensor
    *   The `encodeTensor` op transforms a tensor into a serialized `TensorProto` proto.
    *
    * @define OpDocParsingDecodeTensor
    *   The `decodeTensor` op transforms a serialized `TensorProto` proto into a tensor.
    *
    * @define OpDocParsingDecodeRaw
    *   The `decodeRaw` op reinterprets the bytes of a string as a vector of numbers.
    *
    * @define OpDocParsingDecodeCSV
    *   The `decodeCSV` op converts CSV records to tensors. Each column maps to one tensor.
    *
    *   The [RFC 4180](https://tools.ietf.org/html/rfc4180) format is expected for the CSV records. Note that we allow
    *   leading and trailing spaces with integer or floating-point fields.
    *
    * @define OpDocParsingStringToNumber
    *   The `stringToNumber` op converts each string in the input tensor to the specified numeric type,
    *
    *   '''NOTE:''' [[INT32]] overflow results in an error while [[FLOAT32]] overflow results in a rounded value.
    *
    * @define OpDocParsingDecodeJSONExample
    *   The `decodeJSONExample` op converts JSON-encoded `Example` records to binary protocol buffer strings.
    *
    *   The op translates a tensor containing `Example` records, encoded using the
    *   [standard JSON mapping](https://developers.google.com/protocol-buffers/docs/proto3#json), into a tensor
    *   containing the same records encoded as binary protocol buffers. The resulting tensor can then be fed to any of
    *   the other `Example`-parsing ops.
    */
  private[ops] trait Documentation
}
