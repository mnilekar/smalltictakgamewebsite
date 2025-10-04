package com.tictac.game.engine;

import java.util.ArrayList;
import java.util.List;

public final class TicTacEngine {
    private TicTacEngine(){}

    public static final char EMPTY='.';
    private static final int[][] LINES = {
            {0,1,2},{3,4,5},{6,7,8},
            {0,3,6},{1,4,7},{2,5,8},
            {0,4,8},{2,4,6}
    };

    public static char winner(String b){
        for (int[] line: LINES){
            char a=b.charAt(line[0]), c=b.charAt(line[1]), d=b.charAt(line[2]);
            if (a!='.' && a==c && c==d) return a;
        }
        return '.';
    }

    public static boolean isFull(String b){
        return b.indexOf(EMPTY) < 0;
    }

    public static String applyMove(String board, int row, int col, char mark){
        int idx=row*3+col;
        if (board.charAt(idx) != EMPTY) throw new IllegalArgumentException("Cell not empty");
        StringBuilder sb = new StringBuilder(board);
        sb.setCharAt(idx, mark);
        return sb.toString();
    }

    public static List<int[]> emptyCells(String b){
        List<int[]> cells = new ArrayList<>();
        for (int i=0;i<9;i++){
            if (b.charAt(i)==EMPTY){
                cells.add(new int[]{i/3,i%3});
            }
        }
        return cells;
    }

    public static int[] bestSystemMove(String b, char systemMark){
        char human = (systemMark=='X') ? 'O' : 'X';

        // 1) Win if possible
        int[] win = findCompletingMove(b, systemMark);
        if (win!=null) return win;

        // 2) Block opponent
        int[] block = findCompletingMove(b, human);
        if (block!=null) return block;

        // 3) Center
        if (b.charAt(4)==EMPTY) return new int[]{1,1};

        // 4) Corners
        int[][] corners = {{0,0},{0,2},{2,0},{2,2}};
        for (int[] c: corners){
            if (b.charAt(c[0]*3+c[1])==EMPTY) return c;
        }
        // 5) Sides
        int[][] sides = {{0,1},{1,0},{1,2},{2,1}};
        for (int[] s: sides){
            if (b.charAt(s[0]*3+s[1])==EMPTY) return s;
        }
        return null;
    }

    private static int[] findCompletingMove(String b, char mark){
        for (int[] line: LINES){
            int countMark=0, countEmpty=0, emptyIdx=-1;
            for (int k=0;k<3;k++){
                int idx=line[k];
                char c=b.charAt(idx);
                if (c==mark) countMark++;
                else if (c==EMPTY){ countEmpty++; emptyIdx=idx; }
            }
            if (countMark==2 && countEmpty==1){
                return new int[]{ emptyIdx/3, emptyIdx%3 };
            }
        }
        return null;
    }
}
