package cd.test;

// NOTE: This code was adapted from the code found at 
//    http://www.cs.princeton.edu/introcs/96optimization/Diff.java.html

/*************************************************************************
 *  Compilation:  javac Diff
 *  Execution:    java Diff filename1 filename2
 *  Dependencies: In.java
 *  
 *  Reads in two files and compute their diff.
 *  A bare bones version.
 * 
 *  % java Diff input1.txt input2.txt
 * 
 *
 *  Limitations
 *  -----------
 *   - Could hash the lines to avoid potentially more expensive 
 *     string comparisons.
 *
 *************************************************************************/

public class Diff {
	
	public static String computeDiff(String in0, String in1) {
		StringBuffer out = new StringBuffer();
		
        // break into lines of each file
        String[] x = in0.split("\\n");
        String[] y = in1.split("\\n");

        // number of lines of each file
        int M = x.length;
        int N = y.length;

        // opt[i][j] = length of LCS of x[i..M] and y[j..N]
        int[][] opt = new int[M+1][N+1];

        // compute length of LCS and all subproblems via dynamic programming
        for (int i = M-1; i >= 0; i--) {
            for (int j = N-1; j >= 0; j--) {
                if (x[i].equals(y[j]))
                    opt[i][j] = opt[i+1][j+1] + 1;
                else 
                    opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
            }
        }

        // recover LCS itself and print out non-matching lines to standard output
        boolean lineNumber = false;
        int i = 0, j = 0;
        while(i < M && j < N) {
            if (x[i].equals(y[j])) {
                i++;
                j++;
                lineNumber = false;
            }
            else {
            	if (!lineNumber) {
            		out.append(String.format(
            				"At line %3d / %3d:\n", i+1, j+1));
            		lineNumber = true;
            	}
            	assert lineNumber;
            	if (opt[i+1][j] >= opt[i][j+1]) 
            		out.append("< " + x[i++] + "\n");
            	else                                 
            		out.append("> " + y[j++] + "\n");
            }
        }

        // dump out one remainder of one string if the other is exhausted
    	if (!lineNumber) {
    		out.append(String.format("Line %3d / %3d:\n", i+1, j+1));
    	}
        while(i < M || j < N) {
            if      (i == M) 
            	out.append("> " + y[j++] + "\n");
            else if (j == N) 
            	out.append("< " + x[i++] + "\n");
        }
        
        return out.toString();
    }

}
