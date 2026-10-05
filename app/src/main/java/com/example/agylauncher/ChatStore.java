package com.example.agylauncher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Chat history (saved on disk) and shared state between the Activity and the result receiver. */
final class ChatStore {

    interface Listener {
        void onChanged();
    }

    static final class Msg {
        final boolean user;
        final String text;

        Msg(boolean user, String text) {
            this.user = user;
            this.text = text;
        }
    }

    static final class Info {
        final String id;
        final String title;
        final String folder;
        final long ts;

        Info(String id, String title, String folder, long ts) {
            this.id = id;
            this.title = title;
            this.folder = folder;
            this.ts = ts;
        }
    }

    private static final class Chat {
        String id;
        String title = "";
        String folder = "";
        long ts;
        final List<Msg> msgs = new ArrayList<>();
    }

    private static final long CHAT_TIMEOUT_MS = 16L * 60L * 1000L;
    private static final long MODELS_TIMEOUT_MS = 90L * 1000L;

    private static final List<Chat> CHATS = new ArrayList<>(); // newest first
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static Listener listener;
    private static File file;
    private static boolean inited;

    private static String currentId;   // null = a new, still empty chat
    private static String lastAgyId;   // chat that "agy --continue" refers to
    private static boolean busy;
    private static boolean markContinue;
    private static boolean forceContinue;
    private static long busySince;
    private static String busyChatId;

    private static String modelsRaw = "";
    private static String modelsError = "";
    private static boolean modelsLoading;
    private static long modelsSince;

    private static final long INSTALL_TIMEOUT_MS = 60L * 1000L;
    private static String installRaw = "";
    private static String installError = "";
    private static boolean installChecking;
    private static long installSince;

    private ChatStore() {}

    // ------------------------------------------------------------ setup / storage

    static synchronized void init(Context c) {
        if (inited) return;
        inited = true;
        file = new File(c.getApplicationContext().getFilesDir(), "chats.json");
        try {
            if (!file.exists()) return;
            FileInputStream in = new FileInputStream(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            JSONObject o = new JSONObject(out.toString("UTF-8"));
            String cur = o.optString("current", "");
            String last = o.optString("last", "");
            modelsRaw = o.optString("modelsRaw", "");
            JSONArray arr = o.optJSONArray("chats");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject co = arr.getJSONObject(i);
                    Chat ch = new Chat();
                    ch.id = co.getString("id");
                    ch.title = co.optString("title", "");
                    ch.folder = co.optString("folder", "");
                    ch.ts = co.optLong("ts", 0L);
                    JSONArray ma = co.optJSONArray("m");
                    if (ma != null) {
                        for (int j = 0; j < ma.length(); j++) {
                            JSONObject mo = ma.getJSONObject(j);
                            ch.msgs.add(new Msg(mo.getBoolean("u"), mo.getString("t")));
                        }
                    }
                    CHATS.add(ch);
                }
            }
            currentId = (!cur.isEmpty() && find(cur) != null) ? cur : null;
            lastAgyId = last.isEmpty() ? null : last;
        } catch (Exception ignored) {
            // corrupt file: start empty
        }
    }

    private static void save() {
        if (file == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("current", currentId == null ? "" : currentId);
            o.put("last", lastAgyId == null ? "" : lastAgyId);
            o.put("modelsRaw", modelsRaw);
            JSONArray arr = new JSONArray();
            int max = Math.min(CHATS.size(), 60);
            for (int i = 0; i < max; i++) {
                Chat c = CHATS.get(i);
                JSONObject co = new JSONObject();
                co.put("id", c.id);
                co.put("title", c.title);
                co.put("folder", c.folder);
                co.put("ts", c.ts);
                JSONArray ma = new JSONArray();
                int from = Math.max(0, c.msgs.size() - 300);
                for (int j = from; j < c.msgs.size(); j++) {
                    Msg m = c.msgs.get(j);
                    JSONObject mo = new JSONObject();
                    mo.put("u", m.user);
                    mo.put("t", m.text);
                    ma.put(mo);
                }
                co.put("m", ma);
                arr.put(co);
            }
            o.put("chats", arr);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(o.toString().getBytes("UTF-8"));
            fos.close();
        } catch (Exception ignored) {
            // best effort
        }
    }

    // ------------------------------------------------------------ listener

    static synchronized void setListener(Listener l) {
        listener = l;
    }

    static synchronized void clearListener(Listener l) {
        if (listener == l) listener = null;
    }

    private static void fire() {
        final Listener l = listener;
        if (l != null) {
            H.post(new Runnable() {
                @Override
                public void run() {
                    l.onChanged();
                }
            });
        }
    }

    // ------------------------------------------------------------ lookups

    private static Chat find(String id) {
        if (id == null) return null;
        for (Chat c : CHATS) {
            if (id.equals(c.id)) return c;
        }
        return null;
    }

    private static Chat newChatObj(String folder) {
        Chat c = new Chat();
        c.id = UUID.randomUUID().toString();
        c.folder = folder == null ? "" : folder;
        c.ts = System.currentTimeMillis();
        CHATS.add(0, c);
        while (CHATS.size() > 60) CHATS.remove(CHATS.size() - 1);
        return c;
    }

    static synchronized String currentId() {
        return currentId;
    }

    static synchronized List<Msg> snapshot() {
        Chat c = find(currentId);
        return c == null ? new ArrayList<Msg>() : new ArrayList<>(c.msgs);
    }

    static synchronized List<Info> chats() {
        List<Info> out = new ArrayList<>();
        for (Chat c : CHATS) {
            String t = c.title.isEmpty() ? "New chat" : c.title;
            out.add(new Info(c.id, t, c.folder, c.ts));
        }
        return out;
    }

    // ------------------------------------------------------------ chat actions

    static synchronized void openChat(String id) {
        if (find(id) == null) return;
        currentId = id;
        save();
        fire();
    }

    static synchronized void newChat() {
        currentId = null;
        save();
        fire();
    }

    static synchronized void deleteChat(String id) {
        Chat c = find(id);
        if (c == null) return;
        CHATS.remove(c);
        if (id.equals(currentId)) currentId = null;
        if (id.equals(lastAgyId)) lastAgyId = null;
        save();
        fire();
    }

    static synchronized void clearAll() {
        CHATS.clear();
        currentId = null;
        lastAgyId = null;
        save();
        fire();
    }

    static synchronized void addUser(String text, String folder) {
        Chat c = find(currentId);
        if (c == null) {
            c = newChatObj(folder);
            currentId = c.id;
        }
        c.msgs.add(new Msg(true, text));
        if (c.title.isEmpty()) {
            String t = text.replace("\n", " ").trim();
            c.title = t.length() > 44 ? t.substring(0, 44) + "..." : t;
        }
        c.ts = System.currentTimeMillis();
        CHATS.remove(c);
        CHATS.add(0, c);
        save();
        fire();
    }

    static synchronized void addAgent(String text) {
        Chat c = find(currentId);
        if (c == null) {
            c = newChatObj("");
            currentId = c.id;
        }
        c.msgs.add(new Msg(false, text));
        c.ts = System.currentTimeMillis();
        save();
        fire();
    }

    static synchronized String lastUserText() {
        Chat c = find(currentId);
        if (c == null) return null;
        for (int i = c.msgs.size() - 1; i >= 0; i--) {
            if (c.msgs.get(i).user) return c.msgs.get(i).text;
        }
        return null;
    }

    static synchronized void popLastAgent() {
        Chat c = find(currentId);
        if (c == null || c.msgs.isEmpty()) return;
        if (!c.msgs.get(c.msgs.size() - 1).user) {
            c.msgs.remove(c.msgs.size() - 1);
            save();
            fire();
        }
    }

    static synchronized String exportCurrent() {
        Chat c = find(currentId);
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Msg m : c.msgs) {
            sb.append(m.user ? "You: " : "NH: ").append(m.text).append("\n\n");
        }
        return sb.toString().trim();
    }

    /** Earlier messages of the current chat, used when agy cannot continue its own session. */
    static synchronized String contextForNewSession(int maxChars) {
        Chat c = find(currentId);
        if (c == null || c.msgs.size() < 2) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = c.msgs.size() - 2; i >= 0; i--) {
            Msg m = c.msgs.get(i);
            String line = (m.user ? "User: " : "Assistant: ") + m.text + "\n";
            if (sb.length() + line.length() > maxChars) break;
            sb.insert(0, line);
        }
        if (sb.length() == 0) return "";
        return "This is a continuation of an earlier conversation. Earlier messages:\n" + sb;
    }

    // ------------------------------------------------------------ busy state

    private static void expireIfNeeded() {
        if (busy && System.currentTimeMillis() - busySince > CHAT_TIMEOUT_MS) {
            finishLocked("Timed out waiting for NH.", false);
        }
    }

    static synchronized boolean isBusy() {
        expireIfNeeded();
        return busy;
    }

    static synchronized boolean isBusyHere() {
        expireIfNeeded();
        if (!busy) return false;
        return busyChatId == null ? currentId == null : busyChatId.equals(currentId);
    }

    static synchronized long busySince() {
        return busySince;
    }

    static synchronized void setBusy(boolean b, boolean markContinueOnSuccess) {
        if (b && !busy) {
            busySince = System.currentTimeMillis();
            busyChatId = currentId;
        }
        busy = b;
        markContinue = markContinueOnSuccess;
        fire();
    }

    static synchronized boolean shouldContinue() {
        return forceContinue || (currentId != null && currentId.equals(lastAgyId));
    }

    static synchronized void setContinue(boolean c) {
        forceContinue = c;
    }

    static synchronized void finish(String text, boolean ok) {
        finishLocked(text, ok);
    }

    private static void finishLocked(String text, boolean ok) {
        Chat c = find(busyChatId);
        if (c == null) {
            c = newChatObj("");
            c.title = "NH";
            if (currentId == null) currentId = c.id;
        }
        c.msgs.add(new Msg(false, text));
        c.ts = System.currentTimeMillis();
        if (ok && markContinue) lastAgyId = c.id;
        forceContinue = false;
        busy = false;
        busyChatId = null;
        save();
        fire();
    }

    // ------------------------------------------------------------ models

    static synchronized String modelsRaw() {
        return modelsRaw;
    }

    static synchronized String modelsError() {
        return modelsError;
    }

    static synchronized boolean modelsLoading() {
        if (modelsLoading && System.currentTimeMillis() - modelsSince > MODELS_TIMEOUT_MS) {
            modelsLoading = false;
        }
        return modelsLoading;
    }

    static synchronized void setModelsLoading(boolean b) {
        modelsLoading = b;
        if (b) modelsSince = System.currentTimeMillis();
    }

    static synchronized void setModels(String text, boolean ok) {
        modelsLoading = false;
        if (ok && text != null && !text.trim().isEmpty()) {
            modelsRaw = text;
            modelsError = "";
        } else {
            modelsError = text == null ? "" : text;
        }
        save();
        fire();
    }

    // ------------------------------------------------------------ install check

    static synchronized String installRaw() {
        return installRaw;
    }

    static synchronized String installError() {
        return installError;
    }

    static synchronized boolean installChecking() {
        if (installChecking && System.currentTimeMillis() - installSince > INSTALL_TIMEOUT_MS) {
            installChecking = false;
        }
        return installChecking;
    }

    static synchronized void setInstallChecking(boolean b) {
        installChecking = b;
        if (b) installSince = System.currentTimeMillis();
    }

    static synchronized void setInstall(String text, boolean ok) {
        installChecking = false;
        if (ok && text != null && text.contains("check=done")) {
            installRaw = text;
            installError = "";
        } else {
            installRaw = "";
            installError = text == null ? "" : text;
        }
        fire();
    }

    // ------------------------------------------------------------ text helper

    /** Removes terminal escape codes and carriage returns. */
    static String clean(String s) {
        if (s == null) return "";
        s = s.replaceAll("\u001B\\][^\u0007\u001B]*(\u0007|\u001B\\\\)", "");
        s = s.replaceAll("\u001B\\[[0-9;?]*[ -/]*[@-~]", "");
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        return s.trim();
    }
}
