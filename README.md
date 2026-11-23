# JellyHome: a Jellyfin home screen for Fire TV

A full-screen home screen that shows only:

- **Your apps**: the ones you pick, in the order you choose
- **Your Jellyfin library**: Continue Watching, Next Up, Recently Added Movies, Shows With New Episodes, plus random rows of unwatched movies and shows
- **Trailers**: when you rest on a title, its trailer loads silently in the background and starts playing behind the title after a few seconds. **Press ▲ Up** to grow it to 90% of the screen height.

> Won't work on the **Fire TV Stick 4K Select (2025)**. It runs Vega OS, which can't install Android apps. Every other Fire TV Stick, Cube or Fire TV Smart TV is fine.

## Remote controls

| While browsing | |
|---|---|
| ◀ ▶ ▲ ▼ | Move between titles and rows |
| ▲ Up on a title whose trailer is playing | Open the big trailer view (90% height, with sound) |
| OK | Play / Resume, Watch the trailer big, Mark as watched, Open the Jellyfin app |
| ▶❚❚ | Watch this title's trailer big right away |
| Back | Return to the start of the row, then to the top |

| In the big trailer view | |
|---|---|
| ◀ ▶ | Previous / next title in the row (its trailer starts right away) |
| ▶❚❚ | Pause / continue |
| OK | Options for this title |
| ▼ Down or Back | Back to browsing (the trailer keeps playing behind the title) |

---

## 1. Build the APK on GitHub (no Android Studio needed)

GitHub builds the app for free on its servers. You only need a free GitHub account and a web browser.

1. Sign in at **github.com**, click **+** (top right), then **New repository**. Name it `JellyHome`, choose **Public**, and click **Create repository**.
   *Public is needed so the Fire TV can download the APK without signing in. Nothing personal is in these files. Your server address and password are entered on the TV and stay there.*
2. On the new repository's page, click **uploading an existing file**. Unzip `JellyHome.zip` on your computer, open the `JellyHome` folder, select **everything inside it**, and drag it onto the page. Click **Commit changes**.
   - **Mac users:** Finder hides the `.github` folder. Press **⌘ Shift .** in Finder to show it before selecting everything.
   - If `.github` didn't upload: click **Add file → Create new file**, name it `.github/workflows/build.yml`, paste in the contents of that file from the zip, and commit.
3. Open the **Actions** tab. A "Build JellyHome APK" run starts by itself and takes about 3–5 minutes. A green check means it worked.
4. Your APK is now always at:
   ```
   https://github.com/YOUR-USERNAME/JellyHome/releases/latest/download/JellyHome.apk
   ```
   Every time you upload a change, it rebuilds and replaces this file.

If the run shows a red ✗, click it, open the failed step, and send me the error text.

## 2. Install it on the Fire TV

1. On the Fire TV: **Settings → My Fire TV → Developer options**. Turn on **Apps from unknown sources** (or **Install unknown apps → Downloader**).
   If you don't see Developer options, go to **Settings → My Fire TV → About** and press OK on your device name 7 times.
2. Install **Downloader** (by AFTVnews) from the Fire TV Appstore.
3. In Downloader, enter the link from step 1.4 above and choose **Install**.
   *Tip: a link shortener (e.g. tinyurl.com) saves typing on the remote.*
4. Updating later works the same way: download the link again and install over the old version. Your settings are kept.

## 3. First run

Open **JellyHome** from Your Apps & Channels. Settings opens automatically:

1. **Server address**: e.g. `192.168.1.20:8096`. `http://` and the default `:8096` port are added if you leave them off.
2. **Username / password**: your Jellyfin login.
3. **Choose apps for the home screen**: OK adds or removes an app; ◀ ▶ moves it earlier or later.
4. Optional: **Trailer sound** in the background (the big view always has sound), and **how many seconds before trailers start**. If you set it to Off, trailers still load in the background; press ▲ Up or ▶❚❚ to watch one.

Settings are always available from the **Home settings** tile at the end of the apps row.

## 4. Playing videos

Play/Resume sends the video to your TV's video player. **Install VLC** from the Fire TV Appstore. Resume positions are passed to VLC, so it picks up where you left off.

External players don't report progress back to Jellyfin, so Continue Watching won't update from those plays. If you want watch progress tracked, use **Open the Jellyfin app**. You can also use **Mark as watched**.

## 5. Trailers

JellyHome uses, in order:

1. **Local trailers**: trailer files stored next to your movies (`Movie Name-trailer.mp4` or a `trailers/` folder). These load fastest and look best in the big view.
2. **YouTube trailers**: the trailer links Jellyfin pulls from TMDb metadata. Most movies and shows already have these. If some don't, refresh their metadata in Jellyfin.

While you rest on a title, JellyHome also looks up the trailers for the titles on either side, so moving left or right starts the next one quickly.

## 6. Making it the real Home button target

The honest limitation: **Fire OS ignores the "default home app" setting**. Pressing Home goes to Amazon's screen no matter what. Options, easiest first:

- **Leave it as an app**: put JellyHome first in your Fire TV app row. It's one click from Home.
- **Remap a button**: a remapper app (e.g. *Button Mapper* or *TvQuickActions*) can make Home, or a spare button, open JellyHome. You may see Amazon's screen flash first. Amazon has blocked some remappers on some software versions.
- **Full replacement**: possible only with exploits Amazon keeps patching. They often mean turning off system updates, so I wouldn't recommend it.

## Preview in a browser

Open `app/src/main/assets/index.html?demo` in Chrome and use the arrow keys, Enter and Esc. Without `?demo`, Settings offers **Try a demo without a server**. Demo mode has no trailers.

## How it works

- `app/src/main/assets/index.html` contains the whole UI (HTML/CSS/JS). Edit it, re-upload it to GitHub, and a new APK is built.
- `MainActivity.kt` hosts the page in a full-screen WebView. It adds a small bridge for listing and launching apps, handing videos to a player, and passing the remote's Back button to the page.
- `app/jellyhome.keystore` is the signing key. Keeping the same key lets updates install over the old version.
- It works with Jellyfin 10.8 and newer. It tries newer API routes first and falls back to the older ones.
