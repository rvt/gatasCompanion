package nl.rvantwisk.gatas.lib.extensions

// MIT License
//
// Copyright (c) 2022 Fabian Dittmann
// https://github.com/FabD86/cobs-kotlin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
fun ByteArray.cobsEncode(): ByteArray {
  val result = this.toMutableList()
  result.add(0, 0) // Add leading pointer byte
  result.add(0) // Add closing byte

  var pointerByte = 0
  var counter = 1

  while (pointerByte + counter < result.size) {
    while (result[pointerByte + counter] != 0.toByte()) {
      if (counter == 255) {                                   // break if 255 is hit
        result.add(pointerByte + 255, 42.toByte())    // this is the next pointer byte the value doesn't matter
        break
      }
      counter++
    }

    if (result[pointerByte + counter] == 0.toByte() && counter == 255) {
      result.add(pointerByte + 255, 42.toByte())    // this is the next pointer byte the value doesn't matter
    }

    result[pointerByte] = counter.toByte()
    pointerByte += counter
    counter = 1
  }

  return result.toByteArray()
}

fun ByteArray.cobsDecode(): ByteArray {
  val result = this.toMutableList()
  var pointer = (result[0] - 1).toUByte().toInt()
  var jumpValue = result[0].toUByte().toInt()
  result.removeAt(0)
  var delNext = false
  while (pointer < result.size - 1) {
    if (jumpValue == 255 || delNext) {
      delNext = result[pointer] == 255.toByte()

      jumpValue = (result[pointer].toUByte().toInt() - 1)
      result.removeAt(pointer)
    } else {
      delNext = false
      jumpValue = result[pointer].toUByte().toInt()
      result[pointer] = 0
    }

    pointer += jumpValue
  }

  if (result.last() == 0.toByte())
    result.removeLast()

  return result.toByteArray()
}
