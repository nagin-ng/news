package com.example.agylauncher;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Very small markdown renderer: headings, bold, inline code, code blocks, bullets, tables, links. */
final class Markdown {

    private static final int EX = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE;
    private static final Pattern INLINE =
            Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)\\]\\(([^)]+)\\)");
    private static final Pattern HEAD = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BULLET = Pattern.compile("^(\\s*)[-*]\\s+(.*)$");

    private Markdown() {}

    static CharSequence render(String text, int codeBg, int codeFg, int accent) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean inCode = false;
        String[] lines = text.split("\n", -1);
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            int start = sb.length();
            if (inCode) {
                sb.append(line);
                sb.setSpan(new TypefaceSpan("monospace"), start, sb.length(), EX);
                sb.setSpan(new BackgroundColorSpan(codeBg), start, sb.length(), EX);
                sb.append("\n");
                continue;
            }
            Matcher h = HEAD.matcher(line);
            if (h.matches()) {
                int lvl = h.group(1).length();
                inline(sb, h.group(2), codeBg, codeFg, accent);
                int end = sb.length();
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, end, EX);
                sb.setSpan(new RelativeSizeSpan(lvl == 1 ? 1.3f : lvl == 2 ? 1.2f : 1.1f), start, end, EX);
                sb.append("\n");
                continue;
            }
            String t = line;
            if (t.trim().startsWith("|")) {
                if (isSeparator(t)) continue;
                t = t.trim();
                if (t.startsWith("|")) t = t.substring(1);
                if (t.endsWith("|")) t = t.substring(0, t.length() - 1);
                String[] cells = t.split("\\|");
                StringBuilder row = new StringBuilder();
                for (int i = 0; i < cells.length; i++) {
                    if (i > 0) row.append("   |   ");
                    row.append(cells[i].trim());
                }
                t = row.toString();
            } else {
                Matcher b = BULLET.matcher(line);
                if (b.matches()) t = b.group(1) + "\u2022 " + b.group(2);
            }
            inline(sb, t, codeBg, codeFg, accent);
            sb.append("\n");
        }
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb;
    }

    private static boolean isSeparator(String t) {
        return t.contains("-") && t.matches("^[\\s|:\\-]+$");
    }

    private static void inline(SpannableStringBuilder sb, String s, int codeBg, int codeFg, int accent) {
        Matcher m = INLINE.matcher(s);
        int last = 0;
        while (m.find()) {
            sb.append(s, last, m.start());
            int st = sb.length();
            if (m.group(1) != null) {
                sb.append(m.group(1));
                sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), EX);
            } else if (m.group(2) != null) {
                sb.append(m.group(2));
                sb.setSpan(new TypefaceSpan("monospace"), st, sb.length(), EX);
                sb.setSpan(new BackgroundColorSpan(codeBg), st, sb.length(), EX);
                sb.setSpan(new ForegroundColorSpan(codeFg), st, sb.length(), EX);
            } else {
                sb.append(m.group(3));
                sb.setSpan(new ForegroundColorSpan(accent), st, sb.length(), EX);
            }
            last = m.end();
        }
        sb.append(s, last, s.length());
    }
}
