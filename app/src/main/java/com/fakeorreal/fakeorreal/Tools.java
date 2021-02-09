package com.fakeorreal.fakeorreal;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

class Tools  {
    static String RESTRICTED_FEATURE = "You need to sign in to use this feature";

    static void exceptionToast(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 200);
        toast.show();
    }

    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    public static Map<String, Integer> get_emojiMap(){
        Map<String, Integer> emojiMap = new HashMap<>();
        emojiMap.put("emoji0", R.drawable.emoji_0);
        emojiMap.put("emoji1", R.drawable.emoji_1);
        emojiMap.put("emoji2", R.drawable.emoji_2);
        emojiMap.put("emoji3", R.drawable.emoji_3);
        emojiMap.put("emoji4", R.drawable.emoji_4);
        emojiMap.put("emoji5", R.drawable.emoji_5);
        emojiMap.put("emoji6", R.drawable.emoji_6);
        emojiMap.put("emoji7", R.drawable.emoji_7);
        emojiMap.put("emoji8", R.drawable.emoji_8);
        emojiMap.put("emoji9", R.drawable.emoji_9);
        emojiMap.put("emoji10", R.drawable.emoji_10);
        emojiMap.put("emoji11", R.drawable.emoji_11);
        emojiMap.put("emoji12", R.drawable.emoji_12);
        emojiMap.put("emoji13", R.drawable.emoji_13);
        emojiMap.put("emoji14", R.drawable.emoji_14);
        emojiMap.put("emoji15", R.drawable.emoji_15);
        emojiMap.put("emoji16", R.drawable.emoji_16);
        emojiMap.put("emoji17", R.drawable.emoji_17);
        emojiMap.put("emoji18", R.drawable.emoji_18);
        emojiMap.put("emoji19", R.drawable.emoji_19);
        emojiMap.put("emoji20", R.drawable.emoji_20);
        emojiMap.put("emoji21", R.drawable.emoji_21);
        emojiMap.put("emoji22", R.drawable.emoji_22);
        emojiMap.put("emoji23", R.drawable.emoji_23);
        emojiMap.put("emoji24", R.drawable.emoji_24);
        emojiMap.put("emoji25", R.drawable.emoji_25);
        emojiMap.put("emoji26", R.drawable.emoji_26);
        emojiMap.put("emoji27", R.drawable.emoji_27);
        emojiMap.put("emoji28", R.drawable.emoji_28);
        emojiMap.put("emoji29", R.drawable.emoji_29);
        emojiMap.put("emoji30", R.drawable.emoji_30);
        emojiMap.put("emoji31", R.drawable.emoji_31);
        emojiMap.put("emoji32", R.drawable.emoji_32);
        return emojiMap;
    }

    public static void restrictButton(final Button restrict) {
        restrict.setAlpha(0.5f);
        restrict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exceptionToast(restrict.getContext(), RESTRICTED_FEATURE);
            }
        });
    }

    public static int get_imojiID(String s){
        return Tools.get_emojiMap().get(s);
    }
}