package net.sf.opendf.math.cmatrix;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.FieldPosition;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.StreamTokenizer;

import net.sf.opendf.math.Complex;
import net.sf.opendf.math.util.Maths;


/**
   Jama = Java Matrix class.
<P>
   The Java Matrix Class provides the fundamental operations of numerical
   linear algebra.  Various constructors create Matrices from two dimensional
   arrays of double precision floating point numbers.  Various "gets" and
   "sets" provide access to submatrices and matrix elements.  Several methods 
   implement basic matrix arithmetic, including matrix addition and
   multiplication, matrix norms, and element-by-element array operations.
   Methods for reading and printing matrices are also included.  All the
   operations in this version of the Matrix Class involve real matrices.
   Complex matrices may be handled in a future version.
<P>
   Five fundamental matrix decompositions, which consist of pairs or triples
   of matrices, permutation vectors, and the like, produce results in five
   decomposition classes.  These decompositions are accessed by the Matrix
   class to compute solutions of simultaneous linear equations, determinants,
   inverses and other matrix functions.  The five decompositions are:
<P><UL>
   <LI>Cholesky Decomposition of symmetric, positive definite matrices.
   <LI>LU Decomposition of rectangular matrices.
   <LI>QR Decomposition of rectangular matrices.
   <LI>Singular Value Decomposition of rectangular matrices.
   <LI>Eigenvalue Decomposition of both symmetric and nonsymmetric square matrices.
</UL>
<DL>
<DT><B>Example of use:</B></DT>
<P>
<DD>Solve a linear system A x = b and compute the residual norm, ||b - A x||.
<P><PRE>
      double[][] vals = {{1.,2.,3},{4.,5.,6.},{7.,8.,10.}};
      Matrix A = new Matrix(vals);
      Matrix b = Matrix.random(3,1);
      Matrix x = A.solve(b);
      Matrix r = A.times(x).minus(b);
      double rnorm = r.normInf();
</PRE></DD>
</DL>

@author The MathWorks, Inc. and the National Institute of Standards and Technology.
@version 5 August 1998
*/

public class CMatrix implements Cloneable, java.io.Serializable {

/* ------------------------
   Class variables
 * ------------------------ */

   /** Array for internal storage of elements.
   @serial internal array storage.
   */
   private Complex [][] A;

   /** Row and column dimensions.
   @serial row dimension.
   @serial column dimension.
   */
   private int m, n;

/* ------------------------
   Constructors
 * ------------------------ */

   /** Construct an m-by-n matrix of zeros. 
   @param m    Number of rows.
   @param n    Number of colums.
   */

   public CMatrix (int m, int n) {
	   this (m, n, 0);
   }
   
   public CMatrix(int m, int n, double s) {
	   this (m, n, new Complex(s, 0));
   }

   /** Construct an m-by-n constant matrix.
   @param m    Number of rows.
   @param n    Number of colums.
   @param s    Fill the matrix with this scalar value.
   */

   public CMatrix (int m, int n, Complex s) {
      this.m = m;
      this.n = n;
      A = new Complex[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = s;
         }
      }
   }

   /** Construct a matrix from a 2-D array.
   @param A    Two-dimensional array of doubles.
   @exception  IllegalArgumentException All rows must have the same length
   @see        #constructWithCopy
   */

   public CMatrix (Complex [][] A) {
      m = A.length;
      n = A[0].length;
      for (int i = 0; i < m; i++) {
         if (A[i].length != n) {
            throw new IllegalArgumentException("All rows must have the same length.");
         }
      }
      this.A = A;
   }

   /** Construct a matrix quickly without checking arguments.
   @param A    Two-dimensional array of doubles.
   @param m    Number of rows.
   @param n    Number of colums.
   */

   public CMatrix (Complex [][] A, int m, int n) {
      this.A = A;
      this.m = m;
      this.n = n;
   }
   
   public CMatrix (List<List<Object>> A) {
	   this (createArray(A));
   }



/* ------------------------
   Public Methods
 * ------------------------ */

   /** Make a deep copy of a matrix
   */

   public CMatrix copy () {
      CMatrix X = new CMatrix(m,n);
      Complex [][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j];
         }
      }
      return X;
   }

   /** Clone the Matrix object.
   */

   public Object clone () {
      return this.copy();
   }

   /** Access the internal two-dimensional array.
   @return     Pointer to the two-dimensional array of matrix elements.
   */

   public Complex [][] getArray () {
      return A;
   }
   
   public List<List<Complex>> getList() {
	   List<List<Complex>> res = new ArrayList<List<Complex>>();
	   for (Complex [] r : A) {
		   List<Complex> row = new ArrayList<Complex>();
		   res.add(row);
		   for (Complex v : r) {
			   row.add(v);
		   }
	   }
	   return res;
   }

   /** Copy the internal two-dimensional array.
   @return     Two-dimensional array copy of matrix elements.
   */

   public Complex[][] getArrayCopy () {
      Complex[][] C = new Complex[m][n];
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j];
         }
      }
      return C;
   }


   /** Get row dimension.
   @return     m, the number of rows.
   */

   public int getRowDimension () {
      return m;
   }

   /** Get column dimension.
   @return     n, the number of columns.
   */

   public int getColumnDimension () {
      return n;
   }

   /** Get a single element.
   @param i    Row index.
   @param j    Column index.
   @return     A(i,j)
   @exception  ArrayIndexOutOfBoundsException
   */

   public Complex get (int i, int j) {
      return A[i][j];
   }

   /** Get a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param j0   Initial column index
   @param j1   Final column index
   @return     A(i0:i1,j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public CMatrix getMatrix (int i0, int i1, int j0, int j1) {
      CMatrix X = new CMatrix(i1-i0+1,j1-j0+1);
      Complex[][] B = X.getArray();
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
               B[i-i0][j-j0] = A[i][j];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Get a submatrix.
   @param r    Array of row indices.
   @param c    Array of column indices.
   @return     A(r(:),c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public CMatrix getMatrix (int[] r, int[] c) {
      CMatrix X = new CMatrix(r.length,c.length);
      Complex[][] B = X.getArray();
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = 0; j < c.length; j++) {
               B[i][j] = A[r[i]][c[j]];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Get a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param c    Array of column indices.
   @return     A(i0:i1,c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public CMatrix getMatrix (int i0, int i1, int[] c) {
      CMatrix X = new CMatrix(i1-i0+1,c.length);
      Complex[][] B = X.getArray();
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = 0; j < c.length; j++) {
               B[i-i0][j] = A[i][c[j]];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Get a submatrix.
   @param r    Array of row indices.
   @param i0   Initial column index
   @param i1   Final column index
   @return     A(r(:),j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public CMatrix getMatrix (int[] r, int j0, int j1) {
      CMatrix X = new CMatrix(r.length,j1-j0+1);
      Complex[][] B = X.getArray();
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = j0; j <= j1; j++) {
               B[i][j-j0] = A[r[i]][j];
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
      return X;
   }

   /** Set a single element.
   @param i    Row index.
   @param j    Column index.
   @param s    A(i,j).
   @exception  ArrayIndexOutOfBoundsException
   */

   public void set (int i, int j, Complex s) {
      A[i][j] = s;
   }

   /** Set a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param j0   Initial column index
   @param j1   Final column index
   @param X    A(i0:i1,j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int i0, int i1, int j0, int j1, CMatrix X) {
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = j0; j <= j1; j++) {
               A[i][j] = X.get(i-i0,j-j0);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Set a submatrix.
   @param r    Array of row indices.
   @param c    Array of column indices.
   @param X    A(r(:),c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int[] r, int[] c, CMatrix X) {
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = 0; j < c.length; j++) {
               A[r[i]][c[j]] = X.get(i,j);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Set a submatrix.
   @param r    Array of row indices.
   @param j0   Initial column index
   @param j1   Final column index
   @param X    A(r(:),j0:j1)
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int[] r, int j0, int j1, CMatrix X) {
      try {
         for (int i = 0; i < r.length; i++) {
            for (int j = j0; j <= j1; j++) {
               A[r[i]][j] = X.get(i,j-j0);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Set a submatrix.
   @param i0   Initial row index
   @param i1   Final row index
   @param c    Array of column indices.
   @param X    A(i0:i1,c(:))
   @exception  ArrayIndexOutOfBoundsException Submatrix indices
   */

   public void setMatrix (int i0, int i1, int[] c, CMatrix X) {
      try {
         for (int i = i0; i <= i1; i++) {
            for (int j = 0; j < c.length; j++) {
               A[i][c[j]] = X.get(i-i0,j);
            }
         }
      } catch(ArrayIndexOutOfBoundsException e) {
         throw new ArrayIndexOutOfBoundsException("Submatrix indices");
      }
   }

   /** Matrix transpose.
   @return    A'
   */

   public CMatrix transpose () {
      CMatrix X = new CMatrix(n,m);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[j][i] = A[i][j];
         }
      }
      return X;
   }

   /** One norm
   @return    maximum column sum.
   */

   public double norm1 () {
      double f = 0;
      for (int j = 0; j < n; j++) {
         double s = 0;
         for (int i = 0; i < m; i++) {
            s += A[i][j].abs();
         }
         f = Math.max(f,s);
      }
      return f;
   }

   /** Infinity norm
   @return    maximum row sum.
   */

   public double normInf () {
      double f = 0;
      for (int i = 0; i < m; i++) {
         double s = 0;
         for (int j = 0; j < n; j++) {
            s += A[i][j].abs();
         }
         f = Math.max(f,s);
      }
      return f;
   }


   /**  Unary minus
   @return    -A
   */

   public CMatrix uminus () {
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j].negate();
         }
      }
      return X;
   }

   /** C = A + B
   @param B    another matrix
   @return     A + B
   */

   public CMatrix plus (CMatrix B) {
      checkMatrixDimensions(B);
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j].add(B.A[i][j]);
         }
      }
      return X;
   }

   /** A = A + B
   @param B    another matrix
   @return     A + B
   */

   public CMatrix plusEquals (CMatrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j].add(B.A[i][j]);
         }
      }
      return this;
   }

   /** C = A - B
   @param B    another matrix
   @return     A - B
   */

   public CMatrix minus (CMatrix B) {
      checkMatrixDimensions(B);
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j].subtract(B.A[i][j]);
         }
      }
      return X;
   }

   /** A = A - B
   @param B    another matrix
   @return     A - B
   */

   public CMatrix minusEquals (CMatrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j].subtract(B.A[i][j]);
         }
      }
      return this;
   }

   /** Element-by-element multiplication, C = A.*B
   @param B    another matrix
   @return     A.*B
   */

   public CMatrix arrayTimes (CMatrix B) {
      checkMatrixDimensions(B);
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j].multiply(B.A[i][j]);
         }
      }
      return X;
   }

   /** Element-by-element multiplication in place, A = A.*B
   @param B    another matrix
   @return     A.*B
   */

   public CMatrix arrayTimesEquals (CMatrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j].multiply(B.A[i][j]);
         }
      }
      return this;
   }

   /** Element-by-element right division, C = A./B
   @param B    another matrix
   @return     A./B
   */

   public CMatrix arrayRightDivide (CMatrix B) {
      checkMatrixDimensions(B);
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = A[i][j].divide(B.A[i][j]);
         }
      }
      return X;
   }

   /** Element-by-element right division in place, A = A./B
   @param B    another matrix
   @return     A./B
   */

   public CMatrix arrayRightDivideEquals (CMatrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = A[i][j].divide(B.A[i][j]);
         }
      }
      return this;
   }

   /** Element-by-element left division, C = A.\B
   @param B    another matrix
   @return     A.\B
   */

   public CMatrix arrayLeftDivide (CMatrix B) {
      checkMatrixDimensions(B);
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = B.A[i][j].divide(A[i][j]);
         }
      }
      return X;
   }

   /** Element-by-element left division in place, A = A.\B
   @param B    another matrix
   @return     A.\B
   */

   public CMatrix arrayLeftDivideEquals (CMatrix B) {
      checkMatrixDimensions(B);
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = B.A[i][j].divide(A[i][j]);
         }
      }
      return this;
   }

   /** Multiply a matrix by a scalar, C = s*A
   @param s    scalar
   @return     s*A
   */

   public CMatrix times (double s) {
	   return times(new Complex(s, 0));
   }
   
   public CMatrix times (Complex s) {
      CMatrix X = new CMatrix(m,n);
      Complex[][] C = X.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            C[i][j] = s.multiply(A[i][j]);
         }
      }
      return X;
   }

   public CMatrix timesEquals (double s) {
	   return timesEquals(new Complex(s, 0));
   }
   
   /** Multiply a matrix by a scalar in place, A = s*A
   @param s    scalar
   @return     replace A by s*A
   */

   public CMatrix timesEquals (Complex s) {
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            A[i][j] = s.multiply(A[i][j]);
         }
      }
      return this;
   }

   /** Linear algebraic matrix multiplication, A * B
   @param B    another matrix
   @return     Matrix product, A * B
   @exception  IllegalArgumentException Matrix inner dimensions must agree.
   */

   public CMatrix times (CMatrix B) {
      if (B.m != n) {
         throw new IllegalArgumentException("Matrix inner dimensions must agree.");
      }
      CMatrix X = new CMatrix(m,B.n);
      Complex[][] C = X.getArray();
      Complex[] Bcolj = new Complex[n];
      for (int j = 0; j < B.n; j++) {
         for (int k = 0; k < n; k++) {
            Bcolj[k] = B.A[k][j];
         }
         for (int i = 0; i < m; i++) {
            Complex [] Arowi = A[i];
            Complex s = Complex.ZERO;
            for (int k = 0; k < n; k++) {
               s = s.add(Arowi[k].multiply(Bcolj[k]));
            }
            C[i][j] = s;
         }
      }
      return X;
   }

//   /** LU Decomposition
//   @return     LUDecomposition
//   @see LUDecomposition
//   */
//
//   public LUDecomposition lu () {
//      return new LUDecomposition(this);
//   }
//
//   /** QR Decomposition
//   @return     QRDecomposition
//   @see QRDecomposition
//   */
//
//   public QRDecomposition qr () {
//      return new QRDecomposition(this);
//   }
//
//   /** Cholesky Decomposition
//   @return     CholeskyDecomposition
//   @see CholeskyDecomposition
//   */
//
//   public CholeskyDecomposition chol () {
//      return new CholeskyDecomposition(this);
//   }
//
//   /** Singular Value Decomposition
//   @return     SingularValueDecomposition
//   @see SingularValueDecomposition
//   */
//
//   public SingularValueDecomposition svd () {
//      return new SingularValueDecomposition(this);
//   }
//
//   /** Eigenvalue Decomposition
//   @return     EigenvalueDecomposition
//   @see EigenvalueDecomposition
//   */
//
//   public EigenvalueDecomposition eig () {
//      return new EigenvalueDecomposition(this);
//   }
//
//   /** Solve A*X = B
//   @param B    right hand side
//   @return     solution if A is square, least squares solution otherwise
//   */
//
//   public CMatrix solve (CMatrix B) {
//      return (m == n ? (new LUDecomposition(this)).solve(B) :
//                       (new QRDecomposition(this)).solve(B));
//   }
//
//   /** Solve X*A = B, which is also A'*X' = B'
//   @param B    right hand side
//   @return     solution if A is square, least squares solution otherwise.
//   */
//
//   public CMatrix solveTranspose (CMatrix B) {
//      return transpose().solve(B.transpose());
//   }
//
//   /** Matrix inverse or pseudoinverse
//   @return     inverse(A) if A is square, pseudoinverse otherwise.
//   */
//
//   public CMatrix inverse () {
//      return solve(identity(m,m));
//   }
//
//   /** Matrix determinant
//   @return     determinant
//   */
//
//   public double det () {
//      return new LUDecomposition(this).det();
//   }
//
//   /** Matrix rank
//   @return     effective numerical rank, obtained from SVD.
//   */
//
//   public int rank () {
//      return new SingularValueDecomposition(this).rank();
//   }
//
//   /** Matrix condition (2 norm)
//   @return     ratio of largest to smallest singular value.
//   */
//
//   public double cond () {
//      return new SingularValueDecomposition(this).cond();
//   }

   /** Matrix trace.
   @return     sum of the diagonal elements.
   */

   public Complex trace () {
      Complex t = Complex.ZERO;
      for (int i = 0; i < Math.min(m,n); i++) {
         t = t.add(A[i][i]);
      }
      return t;
   }

   /** Generate matrix with random elements
   @param m    Number of rows.
   @param n    Number of colums.
   @return     An m-by-n matrix with uniformly distributed random elements.
   */

   public static CMatrix random (int m, int n) {
      CMatrix A = new CMatrix(m,n);
      Complex[][] X = A.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            X[i][j] = new Complex(Math.random(), Math.random());
         }
      }
      return A;
   }

   /** Generate identity matrix
   @param m    Number of rows.
   @param n    Number of colums.
   @return     An m-by-n matrix with ones on the diagonal and zeros elsewhere.
   */

   public static CMatrix identity (int m, int n) {
      CMatrix A = new CMatrix(m,n);
      Complex[][] X = A.getArray();
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            X[i][j] = (i == j ? Complex.ONE : Complex.ZERO);
         }
      }
      return A;
   }


   /** Print the matrix to stdout.   Line the elements up in columns
     * with a Fortran-like 'Fw.d' style format.
   @param w    Column width.
   @param d    Number of digits after the decimal.
   */

   public void print (int w, int d) {
      print(new PrintWriter(System.out,true),w,d); }

   /** Print the matrix to the output stream.   Line the elements up in
     * columns with a Fortran-like 'Fw.d' style format.
   @param output Output stream.
   @param w      Column width.
   @param d      Number of digits after the decimal.
   */

   public void print (PrintWriter output, int w, int d) {
      DecimalFormat format = new DecimalFormat();
      format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
      format.setMinimumIntegerDigits(1);
      format.setMaximumFractionDigits(d);
      format.setMinimumFractionDigits(d);
      format.setGroupingUsed(false);
      print(output,format,w+2);
   }

   /** Print the matrix to stdout.  Line the elements up in columns.
     * Use the format object, and right justify within columns of width
     * characters.
     * Note that is the matrix is to be read back in, you probably will want
     * to use a NumberFormat that is set to US Locale.
   @param format A  Formatting object for individual elements.
   @param width     Field width for each column.
   @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

   public void print (NumberFormat format, int width) {
      print(new PrintWriter(System.out,true),format,width); }

   // DecimalFormat is a little disappointing coming from Fortran or C's printf.
   // Since it doesn't pad on the left, the elements will come out different
   // widths.  Consequently, we'll pass the desired column width in as an
   // argument and do the extra padding ourselves.

   /** Print the matrix to the output stream.  Line the elements up in columns.
     * Use the format object, and right justify within columns of width
     * characters.
     * Note that is the matrix is to be read back in, you probably will want
     * to use a NumberFormat that is set to US Locale.
   @param output the output stream.
   @param format A formatting object to format the matrix elements 
   @param width  Column width.
   @see java.text.DecimalFormat#setDecimalFormatSymbols
   */

   public void print (PrintWriter output, NumberFormat format, int width) {
      output.println();  // start on new line.
      for (int i = 0; i < m; i++) {
         for (int j = 0; j < n; j++) {
            String s = format.format(A[i][j]); // format the number
            int padding = Math.max(1,width-s.length()); // At _least_ 1 space
            for (int k = 0; k < padding; k++)
               output.print(' ');
            output.print(s);
         }
         output.println();
      }
      output.println();   // end with blank line.
   }

/* ------------------------
   Private Methods
 * ------------------------ */

   /** Check if size(A) == size(B) **/

   private void checkMatrixDimensions (CMatrix B) {
      if (B.m != m || B.n != n) {
         throw new IllegalArgumentException("Matrix dimensions must agree.");
      }
   }
   
   private static Complex [][] createArray(List<List<Object>> A) {
	   Complex [][] a = new Complex[A.size()][];
	   for (int i = 0; i < a.length; i++) {
		   List<Object> R = A.get(i);
		   Complex [] r = a[i] = new Complex[R.size()];
		   for (int j = 0; j < r.length; j++) {
			   r[j] = makeComplex(R.get(j));
		   }
	   }
	   return a;
   }
   
   private static Complex  makeComplex(Object v) {
	   if (v instanceof Complex) {
		   return (Complex)v;
	   } else if (v instanceof Number) {
		   return new Complex(((Number)v).doubleValue(), 0);
	   } else {
		   throw new RuntimeException("Must be either a Number or a Complex. (" + v + ")");
	   }
   }

}
