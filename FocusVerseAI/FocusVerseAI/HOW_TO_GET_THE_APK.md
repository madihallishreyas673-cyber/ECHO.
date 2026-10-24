# How to get your APK (no coding needed)

GitHub builds the APK for you, free, on its own computers. You only upload this folder.

## Steps (on a laptop or PC)

1. Go to https://github.com and create a free account (or log in).
2. Click the **+** at the top right, then **New repository**.
   - Name: `FocusVerseAI`
   - Choose **Public** (builds are free and unlimited for public repositories)
   - Click **Create repository**.
3. Unzip `FocusVerseAI.zip` on your computer.
4. On the new repository page, click **uploading an existing file**.
5. Open the unzipped `FocusVerseAI` folder and drag **everything inside it** into the browser
   (the folders `app` and `.github`, plus the files `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `.gitignore`).
   - If your computer hides the `.github` folder, turn on "show hidden files" first
     (Mac: press Cmd+Shift+. in Finder; Windows: View > Show > Hidden items).
   - IMPORTANT: the `.github` folder must be uploaded, or nothing will build.
6. Click **Commit changes**.
7. Click the **Actions** tab. A run called **Build APK** starts by itself. Wait about 5 to 8 minutes for the green tick.
   - If it shows a red cross, click it, copy the error text and send it to me. I will fix it.
8. Click the finished run, scroll to **Artifacts**, click **FocusVerseAI-apk**. A zip downloads. Unzip it to get `app-debug.apk`.
9. Send `app-debug.apk` to your phone (WhatsApp to yourself, Google Drive, or USB cable).
10. Tap the APK on your phone. Allow "Install unknown apps" when asked. Install.

## First launch on your phone

- Android may show "Play Protect" warning because the app is not from the Play Store. Choose **Install anyway**.
- In the **Focus** tab, grant **Usage Access** and **Show over other apps** if you want app blocking.
- In the **Tutor** tab, paste your own Anthropic API key (console.anthropic.com) to turn the AI tutor on.

## Permissions the app asks for

| Permission | Why |
|---|---|
| Usage Access (you switch it on in Settings) | To see WHICH app is open during a focus session |
| Show over other apps (you switch it on in Settings) | To show the "Stay focused" screen on top of a blocked app |
| Notifications | Session finished message |
| Internet | AI Tutor only |

## Not in this version yet

Accounts, friends, private chat, photo sharing, push notifications, full KCET syllabus tracker,
mock tests, spaced-repetition calendar, Biology content. These need an online backend (Firebase)
and are the next phases.
