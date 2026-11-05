# Open-source research log

No third-party code has been copied into the initial scaffold.

Before adopting a project, record:
- canonical repository URL and exact commit/tag;
- license and attribution obligations;
- last meaningful maintenance activity;
- minimum Android/API level and dependency requirements;
- whether code relies on privileged APIs, root, Accessibility, overlays or hidden APIs;
- exact files/components reused and modifications made.

## Initial research leads
- Android platform API history and API 19 reference documentation should be treated as the source of truth for platform capabilities.
- Search for lightweight Java launchers, preference screens, color/theme editors and overlay edge-launcher examples that explicitly support API 19.
- Do not adopt a repository solely because its README says "Android"; verify Gradle version, compile/min SDK, license and code path compatibility.

Research remains open; no external component is currently a dependency.
