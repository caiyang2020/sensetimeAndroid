package com.sensetime.autotest.tools;

import android.content.Intent;
import android.content.Context;

public  class  IntentTools {

    static Context mContext ;

    static Intent intent = new Intent("com.caisang");

    public static void intentTo(String type,String message){
        intent.putExtra(type,message);

    }
}
