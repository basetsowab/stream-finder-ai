# StreamFinder AI — Fire TV MVP

Native Java Android / Fire OS prototype for discovering real movies and TV shows and up to three US subscription streaming providers. Uses TMDB and JustWatch-derived provider data returned by TMDB. No fake titles, ratings, or availability.

## Features
- Real TMDB trending titles and movie/TV search
- Real TMDB vote averages
- Real US subscription streaming providers (up to three), or clear no-results message
- D-pad-focusable native Android buttons and details dialog
- No API key committed to source control

## Build on GitHub without VS Code
1. Create a GitHub repository and upload this folder's contents, preserving paths, including `.github/workflows/build.yml`.
2. Create a free TMDB API key at https://www.themoviedb.org/settings/api.
3. In GitHub: Settings → Secrets and variables → Actions → New repository secret. Name it `TMDB_API_KEY` and paste your key.
4. Go to Actions → Build Fire TV APK → Run workflow. Download the APK from the successful run's Artifacts.
5. Sideload the APK onto a Fire TV device or compatible emulator using `adb install app-debug.apk`. Test D-pad, search, detail view and internet access.

## Notes
- The key is injected at build time and remains extractable from the APK. Use a backend proxy for a production release. Never commit the key to GitHub.
- TMDB does not provide Netflix or other subscription login state or direct playback deep links.
- Provider results can change and may be incomplete; the first three returned are not personalized or ranked by price.
- IMDb, Rotten Tomatoes and Letterboxd scores are **not** included: no misleading or fabricated ratings.
- Add TMDB's required logo and full attribution before public release; follow https://developer.themoviedb.org/docs/faq.
- This is an untested source project until GitHub Actions builds successfully and a Fire TV/emulator demo is recorded. A successful APK alone does not prove hackathon eligibility.
