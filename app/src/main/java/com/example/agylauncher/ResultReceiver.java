package com.example.agylauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Receives the finished command output from Termux. */
public class ResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ChatStore.init(context);
        Bundle r = intent.getBundleExtra("result");
        String out = "";
        String err = "";
        String errmsg = null;
        int code = -1;
        if (r != null) {
            out = r.getString("stdout", "");
            err = r.getString("stderr", "");
            errmsg = r.getString("errmsg");
            code = r.getInt("exitCode", -1);
        }
        String text = ChatStore.clean(out);
        if (text.isEmpty()) text = ChatStore.clean(err);
        if (text.isEmpty()) {
            text = (errmsg != null && !errmsg.isEmpty())
                    ? errmsg
                    : "(no output, exit code " + code + ")";
        }
        String kind = intent.getStringExtra("kind");
        if ("status".equals(kind)) {
            ChatStore.setInstall(text, code == 0);
            return;
        }
        if ("models".equals(kind)) {
            ChatStore.setModels(text, code == 0);
            return;
        }
        ChatStore.finish(text, code == 0);
    }
}
