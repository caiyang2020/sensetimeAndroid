package com.sensetime.autotest;

import org.junit.Test;

import static org.junit.Assert.*;

import com.sensetime.autotest.tools.Tools;

import java.io.IOException;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws IOException {
       String fatherPath = "/Testdata/JL-V362/Gaze/20211217/00010/1217/cut_shuju";
       String s = fatherPath.split("/")[fatherPath.split("/").length-1];
       System.out.println(s);
       StringBuilder sb = new StringBuilder();
       for (int i=0; (i<fatherPath.split("/").length-1);i++){
           if (i==0){
               sb.append(fatherPath.split("/")[i]);
           }else {
               sb.append("/" + fatherPath.split("/")[i]);
           }
       }
        System.out.println(sb);
    }
}