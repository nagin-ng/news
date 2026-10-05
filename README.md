# NH Chat v7

Claude-style chat app for Google Antigravity CLI (agy) running in Termux.
Type a message, the app runs `agy -p "<message>"` in Termux in the background and
shows the reply. Not affiliated with Google, Anthropic or Termux; bundles no binaries.

## Screen
- Top left: all chats (side panel). Top right: copy whole chat, new chat, all commands.
- Input card: + quick actions, model picker pill, mic, send.
- Under each reply: copy, share, read aloud, like, dislike, retry.

## Build
Open in Android Studio (JDK 17), let Gradle sync, Build > Build APK(s).

## First-time setup (three-dot menu > Setup)
1. Install Termux from F-Droid and open it once.
2. Setup 2: copy the command, paste it in Termux, fully restart Termux.
3. Setup 1: grant permission. Allow Termux "Display over other apps".
4. Setup 3: storage permission. Setup 4: install agy. Log in once via
   "Open interactive agy in Termux".

## Notes
- Chats are saved on the phone. agy keeps its own history; an older chat is sent to agy
  with the earlier messages as context.
- The percent on the progress card is an estimate; agy does not report progress.
- Auto-approve lets agy edit or delete files in the project folder without asking.

## What is new in v3
- New adaptive app icon (gradient tile with a sparkle), themed icon support on Android 13+,
  and a matching splash colour on Android 12+.
- Animated welcome screen on first launch (replay it from the three-dot menu > App).
- Input card fixed: the send button can no longer be pushed off screen by a long model name.
- The chat scrolls to the latest message when you tap the box, type, or the keyboard opens.

## What is new in v4
- The welcome screen now appears every time the app is started (cold start). Rotating the
  phone or switching to Termux and back does not show it again.
- Welcome layout reworked: sizes adapt to the screen height, the button is always visible at
  the bottom, and the logo glow no longer gets cut into a square.

## What is new in v5
- Welcome screen: top-left "Install & Guide" button. Red = Antigravity is not installed,
  green = complete install found, grey = checking. Tap it to open the install guide.
- Install guide (also in the three-dot menu > Setup): a summary with progress, a
  "Run complete install" button (runs every step in one Termux session), "Copy all commands",
  "Recheck", and 10 step cards. Each step shows Done / To do / Manual, its commands with a Copy
  button, and a "Run in Termux" button.
- The status is read by asking Termux (silently) whether glibc, curl, zip, storage access and
  agy exist. It needs the Termux permission (steps 2 and 3) to work; until then it shows red.

## What is new in v6
- Screen text is renamed: Termux is shown as "server" and agy as "NH". Only text you read
  changed. Commands, file paths, package names and settings keys are untouched, because they must
  stay real to work. Commands in the install guide (for example `agy --version`) are therefore
  still the real commands.
- Layout fixes: the Continue button was cut at both ends by its own breathing animation, the
  "Copy commands" button text was clipped, the guide title was truncated, and the welcome cards
  did not fit on tall screens. The welcome screen now measures itself and shrinks the logo, then
  hides the card sub-lines, only when something would not fit.

## What is new in v7
- Welcome screen: Continue opens the chat only when the install check is green. If NH is not
  installed, the page blurs (Android 12+) / dims, and the Install & Guide button is highlighted
  with a pulsing ring and a hint. Tap the highlighted button to open the guide.
- Chat page: the animated icon (empty chat and "working" card) is now an anti-gravity pixel arch
  whose blocks float and drift upward.
- Top bar: the document icon is replaced by Settings. "Copy whole chat" moved into Settings and
  the three-dot menu.
- Settings: Theme (System / Light / Dark), accent colour, tools, developer details, about.
- Developer details (name, Telegram link, experience, role, credit) are stored only in the native
  library `libnhcore.so` (src/main/cpp/nhcore.cpp), scrambled and checksummed. The profile photo
  is verified against a hash stored in the same library; a changed photo or changed value is
  refused. Release builds also turn on R8 obfuscation.

## Build notes
- The native library needs the NDK and CMake. In Android Studio: Settings > Languages &
  Frameworks > Android SDK > SDK Tools > tick "NDK (Side by side)" and "CMake" > Apply.
  Gradle usually offers to download them on the first sync.
- Honest limit: this makes casual editing hard and any change detectable, but nothing shipped on
  a phone can be made impossible to reverse engineer.
