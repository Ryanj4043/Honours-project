package com.example.ryan.honours_project;

import android.app.Activity;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        Activity act = new MainActivity();
        LocationHandler loc =  new LocationHandler(act);
        double[] x = loc.getLoc();
        double[] y = new double[2];
        y[0] = -0.118092;
        y[1] = 51.5074;

        assertEquals(y, x);
    }
}