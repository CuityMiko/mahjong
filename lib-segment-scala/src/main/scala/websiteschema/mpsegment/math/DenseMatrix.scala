package websiteschema.mpsegment.math

class DenseMatrix(val row: Int, val col: Int, data: Array[Double]) extends Matrix {

  def +(n: Double) = new DenseMatrix(row, col, data.map(_ + n))

  def +(m: Matrix) = {
    assert(row == m.row && col == m.col)
    new DenseMatrix(row, col, Matrix.arithmetic(flatten, m.flatten, (a, b) => a + b))
  }

  def -(n: Double) = new DenseMatrix(row, col, data.map(_ - n))

  def -(m: Matrix) = {
    assert(row == m.row && col == m.col)
    new DenseMatrix(row, col, Matrix.arithmetic(flatten, m.flatten, (a, b) => a - b))
  }

  def x(n: Double) = new DenseMatrix(row, col, data.map(_ * n))

  def x(m: Matrix) = {
    assert(col == m.row)
    val result = Matrix(row, m.col)
    for (i <- 0 until row) {
      for (j <- 0 until m.col) {
        result(i, j) = Matrix.arithmetic(row(i).flatten, m.col(j).flatten, (a, b) => a * b).sum
      }
    }
    result
  }

  def *(m: Matrix) = {
    assert(row == 1 && m.row == 1)
    Matrix.arithmetic(row(0).flatten, m.row(0).flatten, (a, b) => a * b).sum
  }

  def /(n: Double) = new DenseMatrix(row, col, data.map(_ / n))

  def T = {
    val inverse = new Array[Double](data.length)
    for (i <- 0 until row) {
      for (j <- 0 until col) {
        inverse(row * j + i) = apply(i, j)
      }
    }
    new DenseMatrix(col, row, inverse)
  }

  def row(i: Int): Matrix = Matrix(for (index <- i * col until (i + 1) * col) yield data(index))

  def col(i: Int): Matrix = Matrix(for (index <- 0 until data.length; if (index % col == i)) yield data(index))

  def apply(i: Int, j: Int): Double = data(col * i + j)

  def update(i: Int, j: Int, value: Double) {
    data(col * i + j) = value
  }

  def flatten = data

  def isVector = row == 1

  def clear {
    (0 until data.length).foreach(data(_) = 0D)
  }

  override def toString: String = (for (i <- 0 until row) yield {
    row(i).flatten mkString ", "
  }) mkString "\n"

  override def equals(that: Any): Boolean = {
    that match {
      case other: DenseMatrix => other != null && row == other.row && col == other.col && Matrix.doubleArrayEquals(data, other.flatten)
      case _ => false
    }
  }
}
