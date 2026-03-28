# Vidora v3.0 - The Ultra-Light Evolution

We are excited to announce Vidora v3.0, a major milestone in performance, stability, and focus. This version has been completely re-engineered for a smoother, faster, and more streamlined experience.

## Key Highlights

### Ultra-Smooth Performance (60 FPS)
- **Lazy Recycling**: Implemented advanced contentType recycling for the Home Screen lists, ensuring fluid scrolling even on entry-level devices.
- **Stable Architecture**: All media models are now marked as @Immutable, reducing unnecessary UI re-draws for a more responsive feel.
- **Native Efficiency**: Removed performance-heavy shimmer animations and replaced them with static, lightweight placeholders.

### Focused Content & Global Filter
- **Anime-Free Experience**: Implemented a global genre filter across all data providers to exclude animation from search, trending, and recommendations.
- **Provider Purge**: Completely removed AnimePahe, AniList, and all related infrastructure to reduce APK size and background overhead.

### Robust Infrastructure (IMDbAPI Integration)
- **Primary Metadata**: Integrated IMDbAPI (api.imdbapi.dev) as a high-reliability metadata provider.
- **Seamless Failover**: If TMDB is blocked in your region, Vidora now automatically falls back to IMDb for titles, posters, and details.
- **Streaming Reliability**: Consolidated all streaming to the reliable VidNest platform.

### Light & Efficient
- **Adaptive Image Scaling**: Reduced poster resolution to w185 for thumbnails, significantly decreasing memory and bandwidth usage without sacrificing clarity.
- **Smart Caching**: Optimized Disk Cache (50MB) with a 24-hour freshness policy to ensure near-instant app resumes.

## Maintenance & Fixes
- Fixed "Continue Watching" 404 errors for items with complex titles.
- Resolved "Unknown" title issues in watch history.
- Removed legacy Subtitle and Rating services to slim down the codebase.
- Purged all AniList-specific logic from the repository and network modules.

---
*Thank you for being part of the Vidora journey. Enjoy the smoothest streaming experience yet!*
