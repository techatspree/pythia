/// <reference types="vite/client" />

// This file declares NO `ImportMetaEnv` members of its own. It used to type a
// `VITE_AUTH_PROVIDER` / `VITE_ENTRA_*` set, which task-162 removed: Vite
// inlines `VITE_*` at BUILD time, which made the frontend image *be* the
// configuration — a stage needed its own build and an image could not be
// promoted. The SPA's coordinates now arrive at runtime as `/config.json`
// (`$lib/config/runtimeConfig.ts`), served from the `frontend-config` ConfigMap
// in a cluster and from the `predev`-generated `static/config.json` locally.
//
// Do not reintroduce a `VITE_*` auth variable here. The reference above still
// supplies Vite's own built-ins (`import.meta.env.DEV`, used by `$lib/log.ts`).
