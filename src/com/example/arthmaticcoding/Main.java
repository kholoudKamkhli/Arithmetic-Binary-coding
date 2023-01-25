package com.example.arthmaticcoding;

import java.io.*;
import java.util.*;

class symbol_range
{
    public double low_range;
    public double high_range;
    public symbol_range(double l, double h){
        low_range = l;
        high_range = h;
    }
}

public class Main {
    public static Map<Character, Double> probability(String text){
        Map<Character, Double> map = new HashMap<>();
        //prepare table for count
        for (int i=0 ; i<text.length() ; i++){
            if (map.containsKey(text.charAt(i))) {
                Double num = map.get(text.charAt(i)) + 1.0;
                map.remove(text.charAt(i));
                map.put(text.charAt(i), num);
            }else {
                map.put(text.charAt(i), 1.0);
            }
        }
        //prepare table for probability
        Map <Character, Double> prob = new HashMap<>();
        for (Character c : map.keySet()){
            Double num = (map.get(c) / text.length());
            prob.put(c, num);
        }
        return prob;
    }


    public static String encode(String text){
        Map<Character, Double> prob = probability(text);
        //get smallest prob
        double smallest = prob.get(text.charAt(0));
        for (Character c : prob.keySet()){
            if (smallest > prob.get(c))
                smallest = prob.get(c);
        }
        //make k
        int k;
        for (int i=1;;i++){
            if (smallest > (1/Math.pow(2, i))) {
                k = i;
                break;
            }
        }

        //make ranges
        Map <Character, symbol_range> ranges = new HashMap<>();
        double rangeCnt = 0;
        for (Character c : prob.keySet()){
            ranges.put(c, new symbol_range(rangeCnt,  prob.get(c) + rangeCnt));
            rangeCnt += prob.get(c);
        }

        //start generate code
        String code = "";
        //Lower = Lower + range * original Low range
        //Upper = Lower + range * original High range
        double lower = 0;
        double upper = 1;
        for (int i=0 ; i<text.length();i++){
            double l = lower + ((upper - lower) * ranges.get(text.charAt(i)).low_range);
            double u = lower + ((upper - lower) * ranges.get(text.charAt(i)).high_range);
            while (true){
                //E1
                if (u < 0.5){
                    l *= 2;
                    u *= 2;
                    code += '0';
                }
                //E2
                else if (l > 0.5){
                    l -= 0.5;
                    l *= 2;
                    u -= 0.5;
                    u *= 2;
                    code += '1';
                }
                else
                    break;
            }
            lower = l;
            upper = u;
        }
        //add k code
        code += '1';
        for (int i = 1; i<k ; i++)
            code += '0';

        return code;
    }
    public static String decode(String code, Map<Character, Double> prob){
        //get smallest prob
        double smallest = 1.0;
        for (Character c : prob.keySet()){
            if (smallest > prob.get(c))
                smallest = prob.get(c);
        }
        //make k
        int k;
        for (int i=1;;i++){
            if ( smallest > (1/Math.pow(2, i)) ) {
                k = i;
                break;
            }
        }

        //make ranges
        Map <Character, symbol_range> ranges = new HashMap<>();
        double rangeCnt = 0;
        for (Character c : prob.keySet()){
            ranges.put(c, new symbol_range(rangeCnt,  prob.get(c) + rangeCnt));
            rangeCnt += prob.get(c);
        }

        //start generate text
        String text = "";
        //Lower = Lower + range * original Low range
        //Upper = Lower + range * original High range
        double lower = 0;
        double upper = 1;

        //use k bits
        int start = 0;
        while(start < code.length() - k){
            String subCode = code.substring(start, start+k);
            double sum = 0;
            for (int i=0 ; i<subCode.length() ;i++) {
                if (Integer.parseInt(String.valueOf((subCode.charAt(i)))) == 1)
                    sum += Math.pow(2, k - i - 1);//01
            }
            double codeDecimal = (sum/Math.pow(2, k));

            double codeProb = (codeDecimal-lower)/(upper-lower);

            symbol_range range = null;
            for (Character p : ranges.keySet()){
                if (ranges.get(p).low_range < codeProb && codeProb < ranges.get(p).high_range) {
                    text += p;
                    range = ranges.get(p);
                    break;
                }
            }

            double l = lower + ((upper - lower) * range.low_range);
            double u = lower + ((upper - lower) * range.high_range);
            while (true){
                //E1
                if (u < 0.5){
                    l *= 2;
                    u *= 2;
                    start++;
                }
                //E2
                else if (l > 0.5){
                    l -= 0.5;
                    l *= 2;
                    u -= 0.5;
                    u *= 2;
                    start++;
                }
                else
                    break;
            }
            lower = l;
            upper = u;
        }
        for (Character p : ranges.keySet()){
            if (ranges.get(p).low_range < 0.5 && 0.5 < ranges.get(p).high_range) {
                text += p;
                break;
            }
        }
        return text;
    }


    public static void main(String[] args) throws IOException{
        FileReader frEncode = new FileReader("InputText.txt"); //read from file
        int toEncode; //to loop in file symbols
        String textEncode = ""; //create our string
        while ((toEncode = frEncode.read()) != -1)
            textEncode += (char)toEncode;
        frEncode.close();

        String Code = encode(textEncode);

        FileWriter fwEncode = new FileWriter("BinaryCode.txt"); //write in the file
        fwEncode.write(Code);
        fwEncode.flush();
        fwEncode.close();

        //Calculations of the size of the original text & the size of the compressed one in bits
        System.out.println("Decompress: ");
        System.out.println("the size of the original text ("+textEncode+") = "+textEncode.length()*8 +" bits");
        System.out.println("the size of the compressed code ("+Code+") = "+Code.length() +" bits");
        System.out.println();
        //read from file to decode
        String code = "";

        //read code
        frEncode = new FileReader("BinaryCode2.txt"); //read from file
        while ((toEncode = frEncode.read()) != -1)
            code += (char)toEncode;
        frEncode.close();

        //create huffman table and code
        Map<Character, Double> prob = new HashMap();

        //read table
        BufferedReader frDecode = new BufferedReader(new FileReader("ProbTable.txt")); //the same file of the codes
        //to read lines
        String Line;
        while ((Line = frDecode.readLine()) != null)
        {
            prob.put(Line.charAt(0), Double.parseDouble(Line.substring(2,Line.length())));
        }
        frDecode.close();

        String str = decode(code, prob);
        //write decompression text to file
        fwEncode = new FileWriter("TextAfterDecompress.txt");
        fwEncode.write(str);
        fwEncode.flush();
        fwEncode.close();

        //Calculations of the size of the original text & the size of the compressed one in bits
        System.out.println("Decompress: ");
        System.out.println("the size of the compressed code ("+code+") = "+code.length() +" bits");
        System.out.println("the size of the original text ("+str+") = "+str.length()*8 +" bits");
    }
}
